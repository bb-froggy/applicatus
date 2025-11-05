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
            
            val exportDto = CharacterExportDto(
                version = DataModelVersion.CURRENT_VERSION,
                character = CharacterDto.fromCharacter(character),
                spellSlots = slots,
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
     * @return Result mit Pair<Character-ID, Warnung?>
     */
    suspend fun importCharacter(
        jsonString: String,
        overwriteExisting: Boolean = false
    ): Result<Pair<Long, String?>> = withContext(Dispatchers.IO) {
        try {
            val exportDto = json.decodeFromString<CharacterExportDto>(jsonString)
            
            // Versionscheck
            val (isCompatible, warning) = DataModelVersion.checkCompatibility(exportDto.version)
            if (!isCompatible) {
                return@withContext Result.failure(Exception(warning ?: "Inkompatible Version"))
            }
            
            // Prüfen, ob Charakter bereits existiert (nach Name)
            val characters = repository.allCharacters.first()
            val existingCharacter = characters.find { it.name == exportDto.character.name }
            
            val overwriteWarning = if (existingCharacter != null && overwriteExisting) {
                DataModelVersion.checkOverwriteWarning(
                    DataModelVersion.CURRENT_VERSION, 
                    exportDto.version
                )
            } else null
            
            val characterId = if (existingCharacter != null && overwriteExisting) {
                // Bestehenden Charakter aktualisieren
                val updatedCharacter = exportDto.character.toCharacter().copy(id = existingCharacter.id)
                repository.updateCharacter(updatedCharacter)
                
                // Alte Slots löschen
                val oldSlots = repository.getSlotsByCharacter(existingCharacter.id).first()
                oldSlots.forEach { repository.deleteSlot(it) }
                
                existingCharacter.id
            } else {
                // Neuen Charakter erstellen
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
            
            val finalWarning = listOfNotNull(warning, overwriteWarning).joinToString("\n\n")
            Result.success(Pair(characterId, finalWarning.ifEmpty { null }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Lädt einen Charakter aus einer JSON-Datei.
     */
    suspend fun loadCharacterFromFile(
        context: Context,
        uri: Uri,
        overwriteExisting: Boolean = false
    ): Result<Pair<Long, String?>> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return@withContext Result.failure(IOException("Konnte Datei nicht lesen"))
            
            importCharacter(jsonString, overwriteExisting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
