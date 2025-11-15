package de.applicatus.app.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.KnownQualityLevel
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Manager für Export und Import von Charakterdaten als JSON.
 */
class CharacterExportManager(
    private val repository: ApplicatusRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // Wichtig für Abwärtskompatibilität
    }
    
    /**
     * Mergt zwei Tränke mit derselben GUID und übernimmt das bessere Analyse-Ergebnis.
     */
    private fun mergePotion(existing: Potion, imported: Potion): Potion {
        // Vergleiche Analyse-Qualitätslevel (höher ist besser)
        val existingLevel = existing.knownQualityLevel.ordinal
        val importedLevel = imported.knownQualityLevel.ordinal
        
        return if (importedLevel > existingLevel) {
            // Importierter Trank hat besseres Analyse-Ergebnis
            imported.copy(id = existing.id, characterId = existing.characterId)
        } else if (importedLevel == existingLevel) {
            // Gleicher Level -> Nehme höhere Erleichterung
            if (imported.bestStructureAnalysisFacilitation > existing.bestStructureAnalysisFacilitation) {
                imported.copy(id = existing.id, characterId = existing.characterId)
            } else {
                existing
            }
        } else {
            // Existierender Trank ist besser
            existing
        }
    }
    
    /**
     * Exportiert einen Charakter als JSON-String.
     */
    suspend fun exportCharacter(characterId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val character = repository.getCharacterById(characterId)
                ?: return@withContext Result.failure(Exception("Charakter nicht gefunden"))
            
            // Sammle Slots und zugehörige Zauber
            val slotsWithSpells = repository.getSlotsWithSpellsByCharacter(characterId).first()
            
            val slots = slotsWithSpells.map { slotWithSpell ->
                SpellSlotDto.fromSpellSlot(slotWithSpell.slot, slotWithSpell.spell?.name)
            }
            
            // Sammle Tränke und Rezepte
            val potionsWithRecipes = repository.getPotionsForCharacter(characterId).first()
            val potions = potionsWithRecipes.map { potionWithRecipe ->
                PotionDto.fromPotion(potionWithRecipe.potion, potionWithRecipe.recipe.name)
            }
            
            // Sammle Rezeptwissen
            val allRecipes = repository.allRecipes.first()
            val recipeNamesById = allRecipes.associateBy { it.id }
            val recipeKnowledge = repository.getRecipeKnowledgeForCharacter(characterId).first().map { knowledge ->
                RecipeKnowledgeDto.fromModel(knowledge, recipeNamesById[knowledge.recipeId]?.name)
            }
            
            val exportDto = CharacterExportDto(
                version = DataModelVersion.CURRENT_VERSION,
                character = CharacterDto.fromCharacter(character),
                spellSlots = slots,
                potions = potions,
                recipeKnowledge = recipeKnowledge,
                exportTimestamp = System.currentTimeMillis()  // Explizit setzen
            )
            
            val jsonString = json.encodeToString(exportDto)
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Speichert einen Charakter als JSON-Datei.
     */
    suspend fun saveCharacterToFile(
        context: Context,
        characterId: Long,
        uri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonResult = exportCharacter(characterId)
            if (jsonResult.isFailure) {
                return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Export fehlgeschlagen"))
            }
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonResult.getOrNull()?.toByteArray())
            } ?: return@withContext Result.failure(IOException("Konnte Datei nicht öffnen"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Erstellt ein Share-Intent für einen Charakter, ohne die Datei im Dateisystem zu speichern.
     * Nutzt einen temporären Cache-File und FileProvider.
     */
    suspend fun createShareIntent(
        context: Context,
        characterId: Long
    ): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val character = repository.getCharacterById(characterId)
                ?: return@withContext Result.failure(Exception("Charakter nicht gefunden"))
            
            val jsonResult = exportCharacter(characterId)
            if (jsonResult.isFailure) {
                return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Export fehlgeschlagen"))
            }
            
            // Erstelle temporäre Datei im Cache-Verzeichnis
            val cacheDir = File(context.cacheDir, "shared_characters")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val fileName = "${character.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.json"
            val tempFile = File(cacheDir, fileName)
            tempFile.writeText(jsonResult.getOrNull()!!)
            
            // Erstelle URI mit FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            
            // Erstelle Share-Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "DSA Charakter: ${character.name}")
                putExtra(Intent.EXTRA_TEXT, "Charakterdaten für ${character.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Result.success(Intent.createChooser(shareIntent, "Charakter teilen"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Importiert einen Charakter aus einem JSON-String.
     * @param jsonString Der JSON-String mit den Charakterdaten
     * @param targetCharacterId Optional: ID eines bestehenden Charakters, der überschrieben werden soll
     *                          Wenn null, wird ein neuer Charakter angelegt
     * @return Result mit Pair<Character-ID, Warnung?>
     */
    suspend fun importCharacter(
        jsonString: String,
        targetCharacterId: Long? = null
    ): Result<Pair<Long, String?>> = withContext(Dispatchers.IO) {
        try {
            val exportDto = json.decodeFromString<CharacterExportDto>(jsonString)
            
            // Versionscheck
            val (isCompatible, warning) = DataModelVersion.checkCompatibility(exportDto.version)
            if (!isCompatible) {
                return@withContext Result.failure(Exception(warning ?: "Inkompatible Version"))
            }
            
            val overwriteWarning = DataModelVersion.checkOverwriteWarning(
                DataModelVersion.CURRENT_VERSION, 
                exportDto.version
            )
            
            val characterId = if (targetCharacterId != null) {
                // Überschreiben eines bestehenden Charakters
                val existingCharacter = repository.getCharacterById(targetCharacterId)
                    ?: return@withContext Result.failure(Exception("Ziel-Charakter nicht gefunden"))
                
                // GUID-Validierung: Nur überschreiben, wenn GUIDs übereinstimmen
                if (existingCharacter.guid != exportDto.character.guid) {
                    return@withContext Result.failure(Exception(
                        "GUID-Mismatch: Der ausgewählte Charakter stimmt nicht mit dem importierten Charakter überein. " +
                        "Bitte importiere von der Charakterauswahl aus, um einen neuen Charakter anzulegen."
                    ))
                }
                
                // Bestehenden Charakter aktualisieren (ID, GUID und isGameMaster beibehalten)
                // isGameMaster wird bewusst NICHT überschrieben, damit Spieler und Spielleiter
                // den gleichen Charakter teilen können, aber unterschiedliche Rechte behalten
                val updatedCharacter = exportDto.character.toCharacter().copy(
                    id = existingCharacter.id,
                    guid = existingCharacter.guid,
                    isGameMaster = existingCharacter.isGameMaster
                )
                repository.updateCharacter(updatedCharacter)
                
                // Alte Slots löschen
                val oldSlots = repository.getSlotsByCharacter(existingCharacter.id).first()
                oldSlots.forEach { repository.deleteSlot(it) }
                
                existingCharacter.id
            } else {
                // Neuen Charakter erstellen (mit GUID aus Import)
                val newCharacterId = repository.insertCharacter(exportDto.character.toCharacter())
                
                // Erstelle Standard-Locations (Rüstung/Kleidung, Rucksack)
                repository.createDefaultLocationsForCharacter(newCharacterId)
                
                newCharacterId
            }
            
            // Zauber-IDs auflösen (nach Namen matchen)
            val allSpells = repository.allSpells.first()
            
            val newSlots = exportDto.spellSlots.map { slotDto ->
                val resolvedSpellId = slotDto.spellName?.let { spellName ->
                    allSpells.find { it.name == spellName }?.id
                }
                slotDto.toSpellSlot(characterId, resolvedSpellId)
            }
            
            repository.insertSlots(newSlots)
            
            // Rezepte für spätere Zuordnung cachen
            val allRecipes = repository.allRecipes.first()
            val recipesById = allRecipes.associateBy { it.id }
            val recipesByName = allRecipes.associateBy { it.name }
            val additionalWarnings = mutableListOf<String>()
            
            // Bestehende Tränke laden (für GUID-basierten Merge)
            val existingPotions = repository.getPotionsForCharacter(characterId).first()
            val existingPotionsByGuid = existingPotions.associate { it.potion.guid to it.potion }
            
            // Bestehende Wissenseinträge zurücksetzen (Tränke werden gemerged, nicht gelöscht!)
            repository.deleteRecipeKnowledgeForCharacter(characterId)
            
            // Tränke importieren mit intelligentem Merge
            exportDto.potions.forEach { potionDto ->
                val resolvedRecipeId = potionDto.recipeId?.let { recipesById[it]?.id }
                    ?: potionDto.recipeName?.let { recipesByName[it]?.id }
                if (resolvedRecipeId == null) {
                    val identifier = potionDto.recipeName ?: potionDto.recipeId?.toString() ?: "Unbekanntes Rezept"
                    additionalWarnings += "Trank für Rezept '$identifier' konnte nicht importiert werden, da das Rezept nicht gefunden wurde."
                } else {
                    val importedPotion = potionDto.toPotion(characterId, resolvedRecipeId)
                    val existingPotion = existingPotionsByGuid[potionDto.guid]
                    
                    if (existingPotion != null) {
                        // Trank existiert bereits -> Merge mit besserem Analyse-Ergebnis
                        val mergedPotion = mergePotion(existingPotion, importedPotion)
                        repository.updatePotion(mergedPotion)
                    } else {
                        // Neuer Trank -> Einfügen
                        repository.insertPotion(importedPotion)
                    }
                }
            }
            
            exportDto.recipeKnowledge.forEach { knowledgeDto ->
                val resolvedRecipeId = knowledgeDto.recipeId?.let { recipesById[it]?.id }
                    ?: knowledgeDto.recipeName?.let { recipesByName[it]?.id }
                if (resolvedRecipeId == null) {
                    val identifier = knowledgeDto.recipeName ?: knowledgeDto.recipeId?.toString() ?: "Unbekanntes Rezept"
                    additionalWarnings += "Rezeptwissen für '$identifier' konnte nicht importiert werden, da das Rezept nicht gefunden wurde."
                } else {
                    repository.insertRecipeKnowledge(knowledgeDto.toModel(characterId, resolvedRecipeId))
                }
            }
            
            val finalWarning = listOfNotNull(
                warning,
                overwriteWarning,
                additionalWarnings.takeIf { it.isNotEmpty() }?.joinToString("\n")
            ).joinToString("\n\n")
            Result.success(Pair(characterId, finalWarning.ifEmpty { null }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Lädt einen Charakter aus einer JSON-Datei.
     * @param context Android Context
     * @param uri URI der zu lesenden Datei
     * @param targetCharacterId Optional: ID eines bestehenden Charakters, der überschrieben werden soll
     */
    suspend fun loadCharacterFromFile(
        context: Context,
        uri: Uri,
        targetCharacterId: Long? = null
    ): Result<Pair<Long, String?>> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return@withContext Result.failure(IOException("Konnte Datei nicht lesen"))
            
            importCharacter(jsonString, targetCharacterId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
