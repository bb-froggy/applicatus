package de.applicatus.app.data.export

import android.content.Context
import android.net.Uri
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedWriter
import java.io.OutputStreamWriter

/**
 * Manager für den vollständigen Datenbank-Backup Export und Import.
 * 
 * Exportiert/Importiert:
 * - Alle Zauber und Rezepte (Libraries)
 * - Globale Einstellungen
 * - Alle Gruppen
 * - Alle Charaktere mit ihren kompletten Daten
 */
class DatabaseBackupManager(private val repository: ApplicatusRepository) {

    companion object {
        private const val BACKUP_VERSION = 1
        
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    /**
     * Fortschritt beim Import/Export.
     */
    data class BackupProgress(
        val stage: String,
        val current: Int,
        val total: Int
    ) {
        val percentage: Int
            get() = if (total > 0) (current * 100 / total) else 0
    }

    /**
     * Exportiert die komplette Datenbank als JSON.
     * 
     * @return Flow mit Fortschritts-Updates
     */
    suspend fun exportFullBackup(context: Context, uri: Uri): Flow<BackupProgress> = flow {
        emit(BackupProgress("Lade Zauber...", 0, 8))
        
        // 1. Zauber laden
        val spells = repository.allSpells.first().map { SpellDto.fromSpell(it) }
        emit(BackupProgress("Lade Rezepte...", 1, 8))
        
        // 2. Rezepte laden
        val recipes = repository.allRecipes.first().map { RecipeDto.fromRecipe(it) }
        emit(BackupProgress("Lade Einstellungen...", 2, 8))
        
        // 3. Globale Einstellungen laden
        val globalSettings = repository.getGlobalSettingsOnce()?.let { 
            GlobalSettingsDto.fromGlobalSettings(it) 
        }
        emit(BackupProgress("Lade Gruppen...", 3, 8))
        
        // 4. Gruppen laden
        val groups = repository.allGroups.first().map { GroupDto.fromGroup(it) }
        emit(BackupProgress("Lade Charaktere...", 4, 8))
        
        // 5. Charaktere mit allen Daten laden
        val allCharacters = repository.allCharacters.first()
        val characterExports = mutableListOf<CharacterExportDto>()
        
        var processed = 0
        for (character in allCharacters) {
            emit(BackupProgress("Exportiere ${character.name}...", 5 + processed, 5 + allCharacters.size))
            
            // Gruppennamen ermitteln
            val groupName = character.groupId?.let { groupId ->
                groups.find { it.id == groupId }?.name
            }
            
            // Locations laden (vor Items, da LocationName für Items gebraucht wird)
            val locations = repository.getLocationsForCharacter(character.id).first()
            val locationDtos = locations.map { LocationDto.fromLocation(it) }
            
            // Items laden (vor Slots, da itemGuid für Slots gebraucht wird)
            val items = repository.getItemsForCharacter(character.id).first()
            val itemsById = items.associateBy { it.id }
            val itemDtos = items.map { item ->
                val locationName = item.locationId?.let { locId ->
                    locations.find { it.id == locId }?.name
                }
                ItemDto.fromItem(item, locationName)
            }
            
            // Zauber-Slots laden
            val slots = repository.getSlotsByCharacter(character.id).first()
            val slotDtos = slots.map { slot ->
                val spellName = slot.spellId?.let { spellId ->
                    repository.getSpellById(spellId)?.name
                }
                val itemGuid = slot.itemId?.let { itemId ->
                    itemsById[itemId]?.guid
                }
                SpellSlotDto.fromSpellSlot(slot, spellName, itemGuid)
            }
            
            // MagicSigns laden (seit v6)
            val magicSigns = repository.getMagicSignsWithItemsForCharacter(character.id).first()
            val magicSignDtos = magicSigns.mapNotNull { signWithItem ->
                val itemGuid = itemsById[signWithItem.magicSign.itemId]?.guid ?: return@mapNotNull null
                MagicSignDto.fromMagicSign(signWithItem.magicSign, itemGuid)
            }
            
            // Tränke laden
            val potions = repository.getPotionsForCharacter(character.id).first()
            val potionDtos = potions.map { potionWithRecipe ->
                val potion = potionWithRecipe.potion
                val recipeName = potionWithRecipe.recipe?.name
                PotionDto.fromPotion(potion, recipeName)
            }
            
            // Rezeptwissen laden
            val recipeKnowledge = repository.getRecipeKnowledgeForCharacter(character.id).first()
            val knowledgeDtos = recipeKnowledge.map { knowledge ->
                val recipeName = repository.getRecipeById(knowledge.recipeId)?.name
                RecipeKnowledgeDto.fromModel(knowledge, recipeName)
            }
            
            characterExports.add(
                CharacterExportDto(
                    version = DataModelVersion.CURRENT_VERSION,
                    character = CharacterDto.fromCharacter(character, groupName),
                    spellSlots = slotDtos,
                    potions = potionDtos,
                    recipeKnowledge = knowledgeDtos,
                    locations = locationDtos,
                    items = itemDtos,
                    magicSigns = magicSignDtos,
                    exportTimestamp = System.currentTimeMillis()
                )
            )
            processed++
        }
        
        emit(BackupProgress("Erstelle Backup-Datei...", 5 + allCharacters.size, 5 + allCharacters.size + 1))
        
        // 6. Backup-DTO erstellen und exportieren
        val backup = DatabaseBackupDto(
            backupVersion = BACKUP_VERSION,
            backupTimestamp = System.currentTimeMillis(),
            spells = spells,
            recipes = recipes,
            globalSettings = globalSettings,
            groups = groups,
            characters = characterExports
        )
        
        // 7. Als JSON speichern
        val jsonString = json.encodeToString(backup)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                writer.write(jsonString)
            }
        }
        
        emit(BackupProgress("Backup abgeschlossen", 5 + allCharacters.size + 1, 5 + allCharacters.size + 1))
    }

    /**
     * Importiert eine komplette Datenbank aus einem JSON-Backup.
     * 
     * Merge-Strategie:
     * - Spells/Recipes: Nach Name matchen, bei Konflikt überspringen
     * - Groups: Nach Name matchen, bei Konflikt vorhandene verwenden
     * - Characters: Nach GUID matchen, bei Konflikt aktualisieren
     * - Potions: Nach GUID matchen, bei Konflikt intelligentes Merge
     * 
     * @return Flow mit Fortschritts-Updates
     */
    suspend fun importFullBackup(context: Context, uri: Uri): Flow<Result<BackupProgress>> = flow {
        try {
            emit(Result.success(BackupProgress("Lese Backup-Datei...", 0, 10)))
            
            // 1. JSON einlesen
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: throw Exception("Konnte Backup-Datei nicht öffnen")
            
            emit(Result.success(BackupProgress("Parse Backup-Daten...", 1, 10)))
            
            val backup = json.decodeFromString<DatabaseBackupDto>(jsonString)
            
            // 2. Versions-Check
            if (backup.backupVersion > BACKUP_VERSION) {
                throw Exception("Backup wurde mit einer neueren App-Version erstellt (Backup v${backup.backupVersion}, App v$BACKUP_VERSION). Bitte aktualisiere die App.")
            }
            
            emit(Result.success(BackupProgress("Importiere Zauber...", 2, 10)))
            
            // 3. Zauber importieren (merge by name)
            val existingSpells = repository.allSpells.first()
            val existingSpellNames = existingSpells.map { it.name }.toSet()
            var newSpellsCount = 0
            
            backup.spells.forEach { spellDto ->
                if (!existingSpellNames.contains(spellDto.name)) {
                    repository.insertSpell(spellDto.toSpell())
                    newSpellsCount++
                }
            }
            
            emit(Result.success(BackupProgress("Importiere Rezepte...", 3, 10)))
            
            // 4. Rezepte importieren (merge by name)
            val existingRecipes = repository.allRecipes.first()
            val existingRecipeNames = existingRecipes.map { it.name }.toSet()
            var newRecipesCount = 0
            
            backup.recipes.forEach { recipeDto ->
                if (!existingRecipeNames.contains(recipeDto.name)) {
                    repository.insertRecipe(recipeDto.toRecipe())
                    newRecipesCount++
                }
            }
            
            emit(Result.success(BackupProgress("Importiere Einstellungen...", 4, 10)))
            
            // 5. Globale Einstellungen importieren (nur wenn keine vorhanden)
            backup.globalSettings?.let { settingsDto ->
                if (repository.getGlobalSettingsOnce() == null) {
                    repository.updateGlobalSettings(settingsDto.toGlobalSettings())
                }
            }
            
            emit(Result.success(BackupProgress("Importiere Gruppen...", 5, 10)))
            
            // 6. Gruppen importieren (merge by name)
            val groupNameToId = mutableMapOf<String, Long>()
            backup.groups.forEach { groupDto ->
                val existingGroups = repository.allGroups.first()
                val existingGroup = existingGroups.find { it.name == groupDto.name }
                if (existingGroup != null) {
                    // Gruppe existiert bereits, verwende vorhandene ID
                    groupNameToId[groupDto.name] = existingGroup.id
                    // Optional: Datum aktualisieren wenn neuer
                    // repository.updateGroup(existingGroup.copy(currentDerianDate = groupDto.currentDerianDate))
                } else {
                    // Neue Gruppe erstellen
                    val newGroupId = repository.insertGroup(groupDto.toGroup())
                    groupNameToId[groupDto.name] = newGroupId
                }
            }
            
            emit(Result.success(BackupProgress("Importiere Charaktere...", 6, 10)))
            
            // 7. Charaktere importieren
            var importedCharactersCount = 0
            var updatedCharactersCount = 0
            val totalCharacters = backup.characters.size
            
            backup.characters.forEachIndexed { index, characterExport ->
                emit(Result.success(BackupProgress(
                    "Importiere ${characterExport.character.name}...", 
                    6 + index, 
                    6 + totalCharacters
                )))
                
                // CharacterExportManager für einzelnen Charakter-Import verwenden
                val exportManager = CharacterExportManager(repository)
                
                // JSON-String aus CharacterExportDto erstellen
                val characterJson = json.encodeToString(characterExport)
                
                // Import durchführen
                val importResult = exportManager.importCharacter(characterJson, null)
                
                importResult.fold(
                    onSuccess = { (characterId, warning) ->
                        val existingChar = repository.getCharacterByGuid(characterExport.character.guid)
                        if (existingChar != null && existingChar.id != characterId) {
                            updatedCharactersCount++
                        } else {
                            importedCharactersCount++
                        }
                    },
                    onFailure = { error ->
                        // Fehler loggen aber weitermachen mit nächstem Charakter
                        emit(Result.failure(Exception("Fehler beim Import von ${characterExport.character.name}: ${error.message}")))
                    }
                )
            }
            
            emit(Result.success(BackupProgress(
                "Import abgeschlossen", 
                6 + totalCharacters, 
                6 + totalCharacters
            )))
            
            // Erfolgs-Zusammenfassung
            val summary = buildString {
                append("Backup erfolgreich importiert!\n\n")
                if (newSpellsCount > 0) append("• $newSpellsCount neue Zauber\n")
                if (newRecipesCount > 0) append("• $newRecipesCount neue Rezepte\n")
                if (backup.groups.isNotEmpty()) append("• ${backup.groups.size} Gruppen\n")
                if (importedCharactersCount > 0) append("• $importedCharactersCount neue Charaktere\n")
                if (updatedCharactersCount > 0) append("• $updatedCharactersCount aktualisierte Charaktere")
            }
            
            emit(Result.success(BackupProgress(summary, 100, 100)))
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Validiert ein Backup ohne es zu importieren.
     * Gibt Warnungen zurück falls vorhanden.
     */
    suspend fun validateBackup(context: Context, uri: Uri): Result<BackupValidation> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return Result.failure(Exception("Konnte Backup-Datei nicht öffnen"))
            
            val backup = json.decodeFromString<DatabaseBackupDto>(jsonString)
            
            val warnings = mutableListOf<String>()
            
            // Version prüfen
            if (backup.backupVersion > BACKUP_VERSION) {
                return Result.failure(Exception("Backup wurde mit neuerer App-Version erstellt"))
            }
            if (backup.backupVersion < BACKUP_VERSION) {
                warnings.add("Backup ist von einer älteren App-Version")
            }
            
            // Datenbank nicht leer?
            val hasExistingData = repository.allCharacters.first().isNotEmpty()
            if (hasExistingData) {
                warnings.add("Die Datenbank enthält bereits Daten. Diese werden mit den importierten Daten zusammengeführt.")
            }
            
            // Gruppen-Konflikte?
            val existingGroupNames = repository.allGroups.first().map { it.name }.toSet()
            val conflictingGroups = backup.groups.count { it.name in existingGroupNames }
            if (conflictingGroups > 0) {
                warnings.add("$conflictingGroups Gruppe(n) existieren bereits und werden nicht überschrieben")
            }
            
            // Charakter-Konflikte?
            val existingCharacterGuids = repository.allCharacters.first().map { it.guid }.toSet()
            val conflictingCharacters = backup.characters.count { it.character.guid in existingCharacterGuids }
            if (conflictingCharacters > 0) {
                warnings.add("$conflictingCharacters Charakter(e) existieren bereits und werden aktualisiert")
            }
            
            Result.success(BackupValidation(
                backupVersion = backup.backupVersion,
                characterCount = backup.characters.size,
                groupCount = backup.groups.size,
                spellCount = backup.spells.size,
                recipeCount = backup.recipes.size,
                warnings = warnings
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Validierungs-Ergebnis für ein Backup.
 */
data class BackupValidation(
    val backupVersion: Int,
    val characterCount: Int,
    val groupCount: Int,
    val spellCount: Int,
    val recipeCount: Int,
    val warnings: List<String>
)
