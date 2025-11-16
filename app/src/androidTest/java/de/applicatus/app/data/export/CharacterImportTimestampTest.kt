package de.applicatus.app.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AndroidTest für das korrekte Verhalten von lastModifiedDate beim Import.
 * 
 * Anforderung: Beim Import soll lastModifiedDate auf den exportTimestamp gesetzt werden,
 * nicht auf die aktuelle Zeit. Dies gilt sowohl für neue als auch für existierende Charaktere.
 */
@RunWith(AndroidJUnit4::class)
class CharacterImportTimestampTest {
    
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    private lateinit var exportManager: CharacterExportManager
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ApplicatusDatabase::class.java
        ).build()
        repository = ApplicatusRepository(database)
        exportManager = CharacterExportManager(repository)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun importNewCharacter_preservesExportTimestamp() = runBlocking {
        // Zeitstempel: Export um 17:00 Uhr, Import um 17:10 Uhr (simuliert durch Test-Ausführung)
        val exportTime = 1700000000000L // 17:00 Uhr
        
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "new-char-timestamp-test",
                name = "NewCharacter",
                mu = 10, kl = 10, inValue = 10, ch = 10,
                ff = 10, ge = 10, ko = 10, kk = 10,
                lastModifiedDate = exportTime
            ),
            spellSlots = listOf(),
            exportTimestamp = exportTime,
            locations = listOf(),
            items = listOf(),
            potions = listOf(),
            recipeKnowledge = listOf()
        )
        
        val jsonString = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.encodeToString(kotlinx.serialization.serializer(), exportDto)
        
        // Import durchführen
        val result = exportManager.importCharacter(jsonString, targetCharacterId = null)
        
        assertTrue("Import should succeed", result.isSuccess)
        val (characterId, _) = result.getOrThrow()
        
        // Charakter aus DB laden
        val importedCharacter = repository.getCharacterById(characterId)
        assertNotNull("Character should exist", importedCharacter)
        
        // WICHTIG: lastModifiedDate sollte exportTime sein, NICHT die aktuelle Zeit
        assertEquals(
            "lastModifiedDate should be exportTimestamp (17:00), not current time",
            exportTime,
            importedCharacter!!.lastModifiedDate
        )
    }
    
    @Test
    fun importExistingCharacter_preservesExportTimestamp() = runBlocking {
        // Erstelle existierenden Charakter
        val initialTime = 1700000000000L // Ursprüngliche Zeit
        val exportTime = 1700000600000L  // Export um 17:10 Uhr (10 Minuten später)
        
        val character = Character(
            guid = "existing-char-timestamp-test",
            name = "ExistingCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10,
            lastModifiedDate = initialTime
        )
        val characterId = database.characterDao().insertCharacter(character)
        
        // Export DTO mit späterem Zeitstempel erstellen
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "existing-char-timestamp-test",
                name = "UpdatedCharacter",
                mu = 12, kl = 12, inValue = 12, ch = 12,
                ff = 12, ge = 12, ko = 12, kk = 12,
                lastModifiedDate = exportTime
            ),
            spellSlots = listOf(),
            exportTimestamp = exportTime,
            locations = listOf(),
            items = listOf(),
            potions = listOf(),
            recipeKnowledge = listOf()
        )
        
        val jsonString = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.encodeToString(kotlinx.serialization.serializer(), exportDto)
        
        // Import durchführen (überschreibt existierenden Charakter)
        val result = exportManager.importCharacter(jsonString, targetCharacterId = null)
        
        assertTrue("Import should succeed", result.isSuccess)
        
        // Charakter aus DB laden
        val updatedCharacter = repository.getCharacterById(characterId)
        assertNotNull("Character should still exist", updatedCharacter)
        
        // Eigenschaften sollten aktualisiert sein
        assertEquals("UpdatedCharacter", updatedCharacter!!.name)
        assertEquals(12, updatedCharacter.mu)
        
        // WICHTIG: lastModifiedDate sollte exportTime sein, NICHT die aktuelle Zeit
        assertEquals(
            "lastModifiedDate should be exportTimestamp (17:10), not current time",
            exportTime,
            updatedCharacter.lastModifiedDate
        )
    }
    
    @Test
    fun importWithSlots_preservesExportTimestamp() = runBlocking {
        // Test mit Zauber-Slots, um sicherzustellen dass insertSlots nicht touchCharacter aufruft
        val exportTime = 1700000000000L
        
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "char-with-slots-test",
                name = "CharWithSlots",
                mu = 10, kl = 10, inValue = 10, ch = 10,
                ff = 10, ge = 10, ko = 10, kk = 10,
                lastModifiedDate = exportTime
            ),
            spellSlots = listOf(
                SpellSlotDto(
                    spellName = "TestSpell",
                    slotType = "SPELL",
                    currentZfp = 0,
                    zfpBonus = 0
                )
            ),
            exportTimestamp = exportTime,
            locations = listOf(),
            items = listOf(),
            potions = listOf(),
            recipeKnowledge = listOf()
        )
        
        val jsonString = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.encodeToString(kotlinx.serialization.serializer(), exportDto)
        
        // Import durchführen
        val result = exportManager.importCharacter(jsonString, targetCharacterId = null)
        
        assertTrue("Import should succeed", result.isSuccess)
        val (characterId, _) = result.getOrThrow()
        
        // Charakter aus DB laden
        val importedCharacter = repository.getCharacterById(characterId)
        assertNotNull("Character should exist", importedCharacter)
        
        // WICHTIG: lastModifiedDate sollte exportTime sein, auch nach insertSlots
        assertEquals(
            "lastModifiedDate should be exportTimestamp even after inserting slots",
            exportTime,
            importedCharacter!!.lastModifiedDate
        )
    }
}
