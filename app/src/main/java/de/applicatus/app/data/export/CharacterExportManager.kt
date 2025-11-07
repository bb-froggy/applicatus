package de.applicatus.app.data.export

import android.content.Context
import android.net.Uri
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
                
                // Bestehenden Charakter aktualisieren (ID und GUID beibehalten)
                val updatedCharacter = exportDto.character.toCharacter().copy(
                    id = existingCharacter.id,
                    guid = existingCharacter.guid
                )
                repository.updateCharacter(updatedCharacter)
                
                // Alte Slots löschen
                val oldSlots = repository.getSlotsByCharacter(existingCharacter.id).first()
                oldSlots.forEach { repository.deleteSlot(it) }
                
                existingCharacter.id
            } else {
                // Neuen Charakter erstellen (mit GUID aus Import)
                repository.insertCharacter(exportDto.character.toCharacter())
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
            
            // Bestehende Tränke und Wissenseinträge zurücksetzen
            repository.deletePotionsForCharacter(characterId)
            repository.deleteRecipeKnowledgeForCharacter(characterId)
            
            exportDto.potions.forEach { potionDto ->
                val resolvedRecipeId = potionDto.recipeId?.let { recipesById[it]?.id }
                    ?: potionDto.recipeName?.let { recipesByName[it]?.id }
                if (resolvedRecipeId == null) {
                    val identifier = potionDto.recipeName ?: potionDto.recipeId?.toString() ?: "Unbekanntes Rezept"
                    additionalWarnings += "Trank für Rezept '$identifier' konnte nicht importiert werden, da das Rezept nicht gefunden wurde."
                } else {
                    repository.insertPotion(potionDto.toPotion(characterId, resolvedRecipeId))
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
