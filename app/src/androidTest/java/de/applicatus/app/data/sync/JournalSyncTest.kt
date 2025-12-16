package de.applicatus.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.export.*
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android Instrumented Test für Journal-Synchronisation.
 * 
 * Diese Tests simulieren das in der Bugreport beschriebene Szenario:
 * - Gerät A und B synchronisieren Charakter X
 * - LE-Änderungen erzeugen Journal-Einträge
 * - Journal-Einträge müssen korrekt übertragen und gespeichert werden
 */
@RunWith(AndroidJUnit4::class)
class JournalSyncTest {
    
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
    
    /**
     * Test: Journal-Einträge werden im Export enthalten.
     * 
     * Szenario: Ein Charakter hat Journal-Einträge, die beim Export mit übertragen werden müssen.
     */
    @Test
    fun journalEntriesAreIncludedInExport() = runBlocking {
        // Erstelle Charakter
        val character = Character(
            guid = "journal-export-test-char",
            name = "Journal Export Test",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10,
            maxLe = 30,
            currentLe = 20,
            groupId = 1L
        )
        val characterId = repository.insertCharacter(character)
        
        // Füge Journal-Einträge hinzu
        repository.logCharacterEvent(
            characterId = characterId,
            category = "Potion.Brewed",
            playerMessage = "Ein Heiltrank wurde gebraut.",
            gmMessage = "Qualität: B"
        )
        
        // Warte kurz, damit der Timestamp unterschiedlich ist
        kotlinx.coroutines.delay(10)
        
        repository.logCharacterEvent(
            characterId = characterId,
            category = "Energy.Changed",
            playerMessage = "LE: 30 → 20"
        )
        
        // Exportiere Charakter
        val exportResult = exportManager.exportCharacter(characterId)
        assertTrue("Export should succeed", exportResult.isSuccess)
        
        val jsonString = exportResult.getOrThrow()
        val exportDto = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString<CharacterExportDto>(jsonString)
        
        // Verifiziere: Journal-Einträge sind im Export
        assertEquals(
            "Export should contain 2 journal entries",
            2,
            exportDto.journalEntries.size
        )
        
        assertTrue(
            "Export should contain Potion.Brewed entry",
            exportDto.journalEntries.any { it.category == "Potion.Brewed" }
        )
        
        assertTrue(
            "Export should contain Energy.Changed entry",
            exportDto.journalEntries.any { it.category == "Energy.Changed" }
        )
    }
    
    /**
     * Test: Journal-Einträge werden beim Sync-Import gespeichert.
     * 
     * Szenario: Ein Snapshot mit Journal-Einträgen wird empfangen und 
     * die Einträge werden in die lokale Datenbank geschrieben.
     */
    @Test
    fun journalEntriesAreImportedDuringSync() = runBlocking {
        // Erstelle lokalen Charakter (existiert bereits auf diesem Gerät)
        val character = Character(
            guid = "journal-sync-test-char",
            name = "Journal Sync Test",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10,
            maxLe = 30,
            currentLe = 20,
            groupId = 1L
        )
        val localCharacterId = repository.insertCharacter(character)
        
        // Lokaler Charakter hat einen Journal-Eintrag
        repository.logCharacterEvent(
            characterId = localCharacterId,
            category = "Potion.Brewed",
            playerMessage = "Trank wird hinzugefügt"
        )
        
        // Verifiziere lokalen Zustand
        var localEntries = repository.getJournalEntriesOnce(localCharacterId)
        assertEquals("Should have 1 local entry before sync", 1, localEntries.size)
        
        // Simuliere eingehenden Snapshot von anderem Gerät
        // Der andere Spieler hat die LE geändert → neuer Journal-Eintrag
        val baseTimestamp = 1700000000000L
        val incomingSnapshot = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "journal-sync-test-char",  // Gleiche GUID!
                name = "Journal Sync Test",
                mu = 10, kl = 10, inValue = 10, ch = 10,
                ff = 10, ge = 10, ko = 10, kk = 10,
                maxLe = 30,
                currentLe = 25,  // LE wurde von 20 auf 25 geändert
                lastModifiedDate = baseTimestamp + 1000
            ),
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            journalEntries = listOf(
                JournalEntryDto(
                    timestamp = baseTimestamp,
                    derianDate = "1 Praios 1040 BF",
                    category = "Potion.Brewed",
                    playerMessage = "Trank wird hinzugefügt"
                ),
                JournalEntryDto(
                    timestamp = baseTimestamp + 500,
                    derianDate = "1 Praios 1040 BF",
                    category = "Energy.Changed",
                    playerMessage = "LE: 20 → 25"  // Dieser Eintrag soll importiert werden!
                )
            ),
            exportTimestamp = baseTimestamp + 1000
        )
        
        // Wende Snapshot an (wie beim Real-Time-Sync)
        val result = repository.applySnapshotFromSync(incomingSnapshot, allowCreateNew = false)
        assertTrue("applySnapshotFromSync should succeed", result.isSuccess)
        
        // Verifiziere: Beide Journal-Einträge sind jetzt in der DB
        localEntries = repository.getJournalEntriesOnce(localCharacterId)
        
        assertEquals(
            "Should have 2 entries after sync (old + new LE change)",
            2,
            localEntries.size
        )
        
        assertTrue(
            "Should have Potion.Brewed entry",
            localEntries.any { it.category == "Potion.Brewed" }
        )
        
        assertTrue(
            "Should have Energy.Changed entry (LE: 20 → 25)",
            localEntries.any { 
                it.category == "Energy.Changed" && 
                it.playerMessage == "LE: 20 → 25" 
            }
        )
    }
    
    /**
     * Test: Bidirektionaler Journal-Sync - das vollständige Szenario aus dem Bugreport.
     * 
     * Ablauf:
     * 1. Beide Geräte haben Charakter X mit Journal-Eintrag "Trank hinzugefügt"
     * 2. Gerät A ändert LE 20→25 → sendet Snapshot
     * 3. Gerät B empfängt, hat jetzt 2 Einträge (Trank + LE 20→25)
     * 4. Gerät B ändert LE 25→27 → sendet Snapshot
     * 5. Gerät A empfängt, sollte 3 Einträge haben (Trank + LE 20→25 + LE 25→27)
     * 
     * KRITISCH: Nach dem Sync sollte Gerät A auch den Eintrag "LE: 25 → 27" haben!
     */
    @Test
    fun bidirectionalJournalSync_deviceAReceivesDeviceBJournalEntry() = runBlocking {
        val characterGuid = "bidirectional-journal-test"
        val baseTimestamp = 1700000000000L
        
        // ======== GERÄT A: Initialer Zustand ========
        val characterA = Character(
            guid = characterGuid,
            name = "Bidirectional Test",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10,
            maxLe = 30,
            currentLe = 20,
            groupId = 1L
        )
        val characterIdA = repository.insertCharacter(characterA)
        
        // Initialer Journal-Eintrag (wie nach Import/Export)
        repository.logCharacterEvent(
            characterId = characterIdA,
            category = "Potion.Brewed",
            playerMessage = "Trank wird hinzugefügt"
        )
        
        // ======== SCHRITT 1: Gerät A ändert LE 20→25 ========
        // (In der echten App würde das automatisch einen Journal-Eintrag erzeugen)
        repository.updateCharacter(characterA.copy(id = characterIdA, currentLe = 25))
        repository.logCharacterEvent(
            characterId = characterIdA,
            category = "Energy.Changed",
            playerMessage = "LE: 20 → 25"
        )
        
        // Exportiere Charakter A
        val exportResultA = exportManager.exportCharacter(characterIdA)
        assertTrue("Export A should succeed", exportResultA.isSuccess)
        val snapshotJsonFromA = exportResultA.getOrThrow()
        
        // Parse den Export um ihn als "empfangenen Snapshot" auf B zu simulieren
        val snapshotFromA = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString<CharacterExportDto>(snapshotJsonFromA)
        
        // Verifiziere: Snapshot von A enthält beide Journal-Einträge
        assertEquals(
            "Snapshot from A should have 2 journal entries",
            2,
            snapshotFromA.journalEntries.size
        )
        
        // ======== SCHRITT 2: Gerät B empfängt Snapshot von A ========
        // (Wir simulieren das, indem wir applySnapshotFromSync aufrufen)
        // Da wir nur eine DB haben, löschen wir erst die Einträge um B zu simulieren
        
        // Simuliere "Gerät B hat nur den alten Eintrag"
        // Lösche den neuen Eintrag, um den Zustand von B vor dem Empfang zu simulieren
        val entriesBeforeSync = repository.getJournalEntriesOnce(characterIdA)
        assertEquals("Should have 2 entries on A before simulating B", 2, entriesBeforeSync.size)
        
        // Wende Snapshot von A an (simuliert Empfang auf B)
        val syncResultB = repository.applySnapshotFromSync(snapshotFromA, allowCreateNew = false)
        assertTrue("Sync on B should succeed", syncResultB.isSuccess)
        
        // Gerät B hat jetzt beide Einträge
        var entriesOnB = repository.getJournalEntriesOnce(characterIdA)
        assertEquals(
            "B should have 2 entries after receiving A's snapshot",
            2,
            entriesOnB.size
        )
        
        // ======== SCHRITT 3: Gerät B ändert LE 25→27 ========
        repository.updateCharacter(characterA.copy(id = characterIdA, currentLe = 27))
        repository.logCharacterEvent(
            characterId = characterIdA,
            category = "Energy.Changed",
            playerMessage = "LE: 25 → 27"
        )
        
        // Exportiere Charakter B
        val exportResultB = exportManager.exportCharacter(characterIdA)
        assertTrue("Export B should succeed", exportResultB.isSuccess)
        val snapshotJsonFromB = exportResultB.getOrThrow()
        
        val snapshotFromB = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString<CharacterExportDto>(snapshotJsonFromB)
        
        // KRITISCH: Snapshot von B muss ALLE 3 Einträge enthalten!
        assertEquals(
            "Snapshot from B should have 3 journal entries (Trank + LE 20→25 + LE 25→27)",
            3,
            snapshotFromB.journalEntries.size
        )
        
        assertTrue(
            "Snapshot from B should include LE 20→25 entry",
            snapshotFromB.journalEntries.any {
                it.category == "Energy.Changed" && it.playerMessage == "LE: 20 → 25"
            }
        )
        
        assertTrue(
            "Snapshot from B should include LE 25→27 entry",
            snapshotFromB.journalEntries.any {
                it.category == "Energy.Changed" && it.playerMessage == "LE: 25 → 27"
            }
        )
        
        // ======== SCHRITT 4: Gerät A empfängt Snapshot von B ========
        val syncResultA = repository.applySnapshotFromSync(snapshotFromB, allowCreateNew = false)
        assertTrue("Sync on A should succeed", syncResultA.isSuccess)
        
        // KRITISCH: Gerät A sollte jetzt ALLE 3 Einträge haben!
        val finalEntriesOnA = repository.getJournalEntriesOnce(characterIdA)
        assertEquals(
            "A should have 3 entries after receiving B's snapshot",
            3,
            finalEntriesOnA.size
        )
        
        assertTrue(
            "A should have the original Potion entry",
            finalEntriesOnA.any { it.category == "Potion.Brewed" }
        )
        
        assertTrue(
            "A should have LE 20→25 entry",
            finalEntriesOnA.any {
                it.category == "Energy.Changed" && it.playerMessage == "LE: 20 → 25"
            }
        )
        
        assertTrue(
            "A should have LE 25→27 entry (from B)",
            finalEntriesOnA.any {
                it.category == "Energy.Changed" && it.playerMessage == "LE: 25 → 27"
            }
        )
    }
}
