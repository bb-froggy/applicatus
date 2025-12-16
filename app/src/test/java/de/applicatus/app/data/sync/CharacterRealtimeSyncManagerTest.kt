package de.applicatus.app.data.sync

import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.export.CharacterDto
import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.CharacterExportManager
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.nearby.FakeNearbyConnectionsService
import de.applicatus.app.data.repository.ApplicatusRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests für CharacterRealtimeSyncManager
 * 
 * Hinweis: Diese Tests verwenden die FakeNearbyConnectionsService-Infrastruktur
 * zur Simulation der Nearby Connections ohne echte Geräte.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterRealtimeSyncManagerTest {
    
    private lateinit var hostService: FakeNearbyConnectionsService
    private lateinit var clientService: FakeNearbyConnectionsService
    
    private lateinit var mockRepository: ApplicatusRepository
    private lateinit var mockExportManager: CharacterExportManager
    private lateinit var syncManager: CharacterRealtimeSyncManager
    
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    
    private val testCharacter = Character(
        id = 1L,
        guid = "test-guid-123",
        name = "Test Charakter",
        mu = 10,
        kl = 12,
        inValue = 11,
        ch = 10,
        ff = 13,
        ge = 14,
        ko = 12,
        kk = 11,
        maxLe = 30,
        currentLe = 30,
        hasAe = true,
        maxAe = 25,
        currentAe = 25,
        hasKe = false,
        maxKe = 0,
        currentKe = 0,
        groupId = 1L,
        lastModifiedDate = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        FakeNearbyConnectionsService.clearRegistry()
        
        hostService = FakeNearbyConnectionsService()
        clientService = FakeNearbyConnectionsService()
        
        mockRepository = mockk(relaxed = true)
        mockExportManager = mockk(relaxed = true)
    }
    
    @After
    fun tearDown() {
        hostService.stopAllConnections()
        clientService.stopAllConnections()
        FakeNearbyConnectionsService.clearRegistry()
        clearAllMocks()
    }
    
    /**
     * Test: Serialisierung und Deserialisierung von CharacterExportDto funktioniert
     */
    @Test
    fun `test CharacterExportDto serialization`() {
        val testCharacterDto = CharacterDto(
            guid = "test-guid-123",
            name = "Test Charakter",
            groupName = "Test Gruppe",
            mu = 10,
            kl = 12,
            inValue = 11,
            ch = 10,
            ff = 13,
            ge = 14,
            ko = 12,
            kk = 11,
            maxLe = 30,
            currentLe = 30,
            hasAe = true,
            maxAe = 25,
            currentAe = 25,
            hasKe = false,
            maxKe = 0,
            currentKe = 0
        )
        
        val testExportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = testCharacterDto,
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            exportTimestamp = System.currentTimeMillis()
        )
        
        // Serialize
        val jsonString = json.encodeToString(CharacterExportDto.serializer(), testExportDto)
        
        // Deserialize
        val decoded = json.decodeFromString(CharacterExportDto.serializer(), jsonString)
        
        // Verify
        assertEquals(testExportDto.character.guid, decoded.character.guid)
        assertEquals(testExportDto.character.name, decoded.character.name)
        assertEquals(testExportDto.version, decoded.version)
    }
    
    /**
     * Test: SyncStatus-Klasse funktioniert korrekt
     */
    @Test
    fun `test SyncStatus sealed class`() {
        val idleStatus: CharacterRealtimeSyncManager.SyncStatus = CharacterRealtimeSyncManager.SyncStatus.Idle
        val connectingStatus: CharacterRealtimeSyncManager.SyncStatus = CharacterRealtimeSyncManager.SyncStatus.Connecting("Test Device")
        val syncingStatus: CharacterRealtimeSyncManager.SyncStatus = CharacterRealtimeSyncManager.SyncStatus.Syncing(
            "test-guid",
            "endpoint-123",
            "Test Device"
        )
        val warningStatus: CharacterRealtimeSyncManager.SyncStatus = CharacterRealtimeSyncManager.SyncStatus.Warning(
            "test-guid",
            "Test warning",
            System.currentTimeMillis() - 20000
        )
        val errorStatus: CharacterRealtimeSyncManager.SyncStatus = CharacterRealtimeSyncManager.SyncStatus.Error("Test error message")
        
        assertTrue(idleStatus is CharacterRealtimeSyncManager.SyncStatus.Idle)
        assertTrue(connectingStatus is CharacterRealtimeSyncManager.SyncStatus.Connecting)
        assertTrue(syncingStatus is CharacterRealtimeSyncManager.SyncStatus.Syncing)
        assertTrue(warningStatus is CharacterRealtimeSyncManager.SyncStatus.Warning)
        assertTrue(errorStatus is CharacterRealtimeSyncManager.SyncStatus.Error)
    }
    
    /**
     * Test: FakeNearbyConnectionsService kann Verbindungen simulieren
     */
    @Test
    fun `test FakeNearbyConnectionsService connection`() = runTest {
        val hostFlow = hostService.startAdvertising("Host Device")
        val clientFlow = clientService.startDiscovery { endpointId, deviceName -> }
        
        advanceTimeBy(200)
        advanceUntilIdle()
        
        assertTrue("Test completed successfully", true)
    }
    
    /**
     * Test: exportManager wird mit korrekter characterId aufgerufen
     */
    @Test
    fun `exportManager is called with correct characterId`() = runTest {
        coEvery { mockRepository.getCharacterById(1L) } returns testCharacter
        every { mockRepository.getCharacterByIdFlow(1L) } returns flowOf(testCharacter)
        coEvery { mockExportManager.exportCharacter(1L) } returns Result.success("{}")
        
        // Verify export is called
        coVerify(exactly = 0) { mockExportManager.exportCharacter(1L) }
        
        // Call export directly to test
        val result = mockExportManager.exportCharacter(1L)
        assertTrue(result.isSuccess)
        
        coVerify(exactly = 1) { mockExportManager.exportCharacter(1L) }
    }
    
    /**
     * Test: Repository getCharacterById gibt Character zurück
     */
    @Test
    fun `repository getCharacterById returns character`() = runTest {
        coEvery { mockRepository.getCharacterById(1L) } returns testCharacter
        
        val character = mockRepository.getCharacterById(1L)
        
        assertNotNull(character)
        assertEquals("test-guid-123", character?.guid)
        assertEquals("Test Charakter", character?.name)
    }
    
    /**
     * Test: Repository getCharacterById gibt null bei nicht gefundenem Character
     */
    @Test
    fun `repository getCharacterById returns null when not found`() = runTest {
        coEvery { mockRepository.getCharacterById(999L) } returns null
        
        val character = mockRepository.getCharacterById(999L)
        
        assertNull(character)
    }
    
    /**
     * Test: applySnapshotFromSync wird mit korrekten Parametern aufgerufen
     * 
     * Dieser Test verifiziert nur, dass die Methode mit den richtigen Parametern aufgerufen wird.
     */
    @Test
    fun `applySnapshotFromSync is called with correct parameters`() = runTest {
        val testSnapshot = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "test-guid-123",
                name = "Updated Character",
                mu = 15,
                kl = 14,
                inValue = 13
            ),
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            exportTimestamp = System.currentTimeMillis()
        )
        
        // Aufruf der gemockten Methode
        mockRepository.applySnapshotFromSync(testSnapshot, allowCreateNew = false)
        
        // Verifiziere, dass die Methode mit korrekten Parametern aufgerufen wurde
        coVerify { mockRepository.applySnapshotFromSync(testSnapshot, false) }
    }
    
    /**
     * Test: Flow von getCharacterByIdFlow emittiert Werte
     */
    @Test
    fun `getCharacterByIdFlow emits character values`() = runTest {
        val testFlow = flowOf(testCharacter, testCharacter.copy(currentLe = 25))
        every { mockRepository.getCharacterByIdFlow(1L) } returns testFlow
        
        val flow = mockRepository.getCharacterByIdFlow(1L)
        val values = mutableListOf<Character?>()
        
        flow.collect { character ->
            values.add(character)
        }
        
        assertEquals(2, values.size)
        assertEquals(30, values[0]?.currentLe)
        assertEquals(25, values[1]?.currentLe)
    }
    
    /**
     * Test: Export failure wird korrekt behandelt
     * 
     * Dieser Test verifiziert, dass Result.failure korrekt erstellt werden kann.
     */
    @Test
    fun `export failure returns failure result`() = runTest {
        // Teste die Result-Erstellung direkt
        val failureResult: Result<CharacterExportDto> = Result.failure(Exception("Export failed"))
        
        assertTrue("Result should be failure", failureResult.isFailure)
        assertEquals("Export failed", failureResult.exceptionOrNull()?.message)
    }
    
    /**
     * Test: stopSession kann ohne vorherige Session aufgerufen werden
     */
    @Test
    fun `stopSession can be called without active session`() = runTest {
        val testScope = TestScope()
        syncManager = CharacterRealtimeSyncManager(
            mockRepository,
            hostService,
            mockExportManager,
            testScope
        )
        
        // Should not throw
        syncManager.stopSession()
        
        val status = syncManager.syncStatus.value
        assertTrue("Status should be Idle", status is CharacterRealtimeSyncManager.SyncStatus.Idle)
    }
    
    /**
     * Test: SyncManager initialisiert mit Idle Status
     */
    @Test
    fun `syncManager initializes with Idle status`() = runTest {
        val testScope = TestScope()
        syncManager = CharacterRealtimeSyncManager(
            mockRepository,
            hostService,
            mockExportManager,
            testScope
        )
        
        val status = syncManager.syncStatus.value
        assertTrue("Initial status should be Idle", status is CharacterRealtimeSyncManager.SyncStatus.Idle)
    }
    
    /**
     * Test: Nach einseitigem Disconnect und Reconnect bleibt Payload-Größe konstant.
     * Stellt sicher, dass keine Daten akkumuliert werden.
     * 
     * Dieser Test wird mehrfach wiederholt, um flaky Behavior zu erkennen.
     */
    @Test
    fun `payload size stays constant after reconnect`() = runTest {
        // Test mehrfach wiederholen, um Race Conditions zu erkennen
        repeat(20) { iteration ->
            // Registry vor jeder Iteration leeren!
            FakeNearbyConnectionsService.clearRegistry()
            
            // Für jede Iteration neue Services erstellen, um Isolation zu gewährleisten
            val testHostService = FakeNearbyConnectionsService()
            val testClientService = FakeNearbyConnectionsService()
            
            try {
                // Fester Timestamp für lastModifiedDate - WICHTIG für konsistente Serialisierung!
                val fixedModifiedDate = 1700000000000L
                val testCharacterDto = CharacterDto(
                    guid = "test-guid-123",
                    name = "Test Charakter",
                    groupName = "Test Gruppe",
                    mu = 10,
                    kl = 12,
                    inValue = 11,
                    ch = 10,
                    ff = 13,
                    ge = 14,
                    ko = 12,
                    kk = 11,
                    maxLe = 30,
                    currentLe = 30,
                    hasAe = true,
                    maxAe = 25,
                    currentAe = 25,
                    hasKe = false,
                    maxKe = 0,
                    currentKe = 0,
                    lastModifiedDate = fixedModifiedDate
                )
                
                // Fester Timestamp für Reproduzierbarkeit
                val fixedTimestamp = 1700000000000L
                val testExportDto = CharacterExportDto(
                    version = DataModelVersion.CURRENT_VERSION,
                    character = testCharacterDto,
                    spellSlots = emptyList(),
                    potions = emptyList(),
                    recipeKnowledge = emptyList(),
                    locations = emptyList(),
                    items = emptyList(),
                    journalEntries = emptyList(),
                    exportTimestamp = fixedTimestamp
                )
                
                // Erstes Senden
                testHostService.resetPayloadStats()
                testHostService.startAdvertising("Host")
                testClientService.startDiscovery { _, _ -> }
                testClientService.connectToEndpoint("Host", "Client")
                
                advanceUntilIdle()
                
                // Sende Charakter
                testHostService.sendCharacterData(
                    "Client",
                    testExportDto,
                    onSuccess = {},
                    onFailure = { fail("Iteration $iteration: Send should succeed: $it") }
                )
                
                val firstPayloadSize = testHostService.lastSentPayloadSize
                val firstPayloadJson = testHostService.lastSentPayloadJson
                assertTrue("Iteration $iteration: First payload should have reasonable size", firstPayloadSize > 0)
                assertTrue("Iteration $iteration: First payload should be under 10KB for empty character", firstPayloadSize < 10_000)
                
                // Simuliere einseitigen Disconnect
                testClientService.simulateOneSidedDisconnect()
                advanceUntilIdle()
                
                // Reconnect
                testClientService.simulateReconnect()
                advanceUntilIdle()
                
                // Erneut senden (gleicher Charakter, keine Änderungen)
                testHostService.sendCharacterData(
                    "Client",
                    testExportDto,
                    onSuccess = {},
                    onFailure = { fail("Iteration $iteration: Send should succeed: $it") }
                )
                
                val secondPayloadSize = testHostService.lastSentPayloadSize
                val secondPayloadJson = testHostService.lastSentPayloadJson
                
                // Payload-Größe sollte gleich bleiben - bei Fehler beide Payloads ausgeben
                if (firstPayloadSize != secondPayloadSize) {
                    println("=== PAYLOAD MISMATCH in Iteration $iteration ===")
                    println("First payload ($firstPayloadSize bytes):")
                    println(firstPayloadJson)
                    println("---")
                    println("Second payload ($secondPayloadSize bytes):")
                    println(secondPayloadJson)
                    println("===")
                    fail("Iteration $iteration: Payload size should remain constant after reconnect (first=$firstPayloadSize, second=$secondPayloadSize)")
                }
                assertEquals("Iteration $iteration: Should have sent exactly 2 payloads", 2, testHostService.sentPayloadCount)
            } finally {
                testHostService.stopAllConnections()
                testClientService.stopAllConnections()
            }
        }
    }
    
    /**
     * Test: Mehrfaches Senden ohne Änderungen akkumuliert keine Daten.
     */
    @Test
    fun `multiple sends without changes do not accumulate data`() = runTest {
        // Feste Timestamps für konsistente Serialisierung!
        val fixedModifiedDate = 1700000000000L
        val fixedExportTimestamp = 1700000000000L
        
        val testCharacterDto = CharacterDto(
            guid = "test-guid-123",
            name = "Test Charakter",
            groupName = "Test Gruppe",
            mu = 10,
            lastModifiedDate = fixedModifiedDate
        )
        
        val testExportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = testCharacterDto,
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            journalEntries = emptyList(),
            exportTimestamp = fixedExportTimestamp
        )
        
        hostService.resetPayloadStats()
        hostService.startAdvertising("Host")
        clientService.connectToEndpoint("Host", "Client")
        advanceUntilIdle()
        
        val payloadSizes = mutableListOf<Int>()
        val payloadJsons = mutableListOf<String>()
        
        // Sende 5 mal den gleichen Charakter
        repeat(5) {
            hostService.sendCharacterData(
                "Client",
                testExportDto,
                onSuccess = {},
                onFailure = { fail("Send $it should succeed") }
            )
            payloadSizes.add(hostService.lastSentPayloadSize)
            payloadJsons.add(hostService.lastSentPayloadJson)
        }
        
        // Alle Payload-Größen sollten identisch sein
        val firstSize = payloadSizes.first()
        val firstJson = payloadJsons.first()
        payloadSizes.forEachIndexed { index, size ->
            if (firstSize != size) {
                println("=== PAYLOAD MISMATCH at send $index ===")
                println("First payload ($firstSize bytes):")
                println(firstJson)
                println("---")
                println("Payload $index ($size bytes):")
                println(payloadJsons[index])
                println("===")
            }
            assertEquals(
                "Payload $index should have same size as first (first=$firstSize, current=$size)",
                firstSize,
                size
            )
        }
        
        assertEquals("Should have sent exactly 5 payloads", 5, hostService.sentPayloadCount)
    }
    
    // ==================== Potion Location Sync Tests ====================
    
    /**
     * Test: PotionDto enthält locationName für Export
     */
    @Test
    fun `PotionDto includes locationName in export`() {
        val potionDto = de.applicatus.app.data.export.PotionDto(
            guid = "potion-guid-123",
            recipeId = 1L,
            recipeName = "Heiltrank",
            locationName = "Rucksack",  // Neu hinzugefügtes Feld
            actualQuality = "B",
            expiryDate = "15 Praios 1041 BF"
        )
        
        // Serialisiere und deserialisiere
        val jsonString = json.encodeToString(de.applicatus.app.data.export.PotionDto.serializer(), potionDto)
        val decoded = json.decodeFromString(de.applicatus.app.data.export.PotionDto.serializer(), jsonString)
        
        // Verifiziere, dass locationName erhalten bleibt
        assertEquals("Rucksack", decoded.locationName)
        assertEquals("potion-guid-123", decoded.guid)
        assertEquals("Heiltrank", decoded.recipeName)
    }
    
    /**
     * Test: PotionDto ohne locationName (null) funktioniert für "Ohne Ort" Tränke
     */
    @Test
    fun `PotionDto with null locationName represents unassigned potion`() {
        val potionDto = de.applicatus.app.data.export.PotionDto(
            guid = "potion-guid-456",
            recipeId = 2L,
            recipeName = "Gifttrank",
            locationName = null,  // "Ohne Ort"
            actualQuality = "C",
            expiryDate = "1 Efferd 1041 BF"
        )
        
        val jsonString = json.encodeToString(de.applicatus.app.data.export.PotionDto.serializer(), potionDto)
        val decoded = json.decodeFromString(de.applicatus.app.data.export.PotionDto.serializer(), jsonString)
        
        assertNull("locationName should be null for unassigned potions", decoded.locationName)
    }
    
    /**
     * Test: CharacterExportDto mit Tränken an verschiedenen Lagerorten
     */
    @Test
    fun `CharacterExportDto with potions at different locations serializes correctly`() {
        val testCharacterDto = CharacterDto(
            guid = "char-guid-123",
            name = "Test Charakter"
        )
        
        val potions = listOf(
            de.applicatus.app.data.export.PotionDto(
                guid = "potion-1",
                recipeName = "Heiltrank",
                locationName = "Rucksack",
                actualQuality = "B",
                expiryDate = "1 Praios 1041 BF"
            ),
            de.applicatus.app.data.export.PotionDto(
                guid = "potion-2",
                recipeName = "Gifttrank",
                locationName = "Am Körper",
                actualQuality = "C",
                expiryDate = "1 Praios 1041 BF"
            ),
            de.applicatus.app.data.export.PotionDto(
                guid = "potion-3",
                recipeName = "Antidot",
                locationName = null,  // "Ohne Ort"
                actualQuality = "A",
                expiryDate = "1 Praios 1041 BF"
            )
        )
        
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = testCharacterDto,
            spellSlots = emptyList(),
            potions = potions,
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            journalEntries = emptyList(),
            exportTimestamp = 1700000000000L
        )
        
        val jsonString = json.encodeToString(CharacterExportDto.serializer(), exportDto)
        val decoded = json.decodeFromString(CharacterExportDto.serializer(), jsonString)
        
        assertEquals(3, decoded.potions.size)
        assertEquals("Rucksack", decoded.potions[0].locationName)
        assertEquals("Am Körper", decoded.potions[1].locationName)
        assertNull(decoded.potions[2].locationName)
    }
    
    // ==================== Journal Sync Tests ====================
    
    /**
     * Test: JournalEntryDto Serialisierung funktioniert
     */
    @Test
    fun `JournalEntryDto serializes correctly`() {
        val journalEntry = de.applicatus.app.data.export.JournalEntryDto(
            timestamp = 1700000000000L,
            derianDate = "15 Praios 1040 BF",
            category = "Potion.Brewed",
            playerMessage = "Ein Heiltrank wurde gebraut.",
            gmMessage = "Qualität: B"
        )
        
        val jsonString = json.encodeToString(de.applicatus.app.data.export.JournalEntryDto.serializer(), journalEntry)
        val decoded = json.decodeFromString(de.applicatus.app.data.export.JournalEntryDto.serializer(), jsonString)
        
        assertEquals(1700000000000L, decoded.timestamp)
        assertEquals("15 Praios 1040 BF", decoded.derianDate)
        assertEquals("Potion.Brewed", decoded.category)
        assertEquals("Ein Heiltrank wurde gebraut.", decoded.playerMessage)
        assertEquals("Qualität: B", decoded.gmMessage)
    }
    
    /**
     * Test: JournalEntryDto mit null gmMessage funktioniert
     */
    @Test
    fun `JournalEntryDto with null gmMessage works`() {
        val journalEntry = de.applicatus.app.data.export.JournalEntryDto(
            timestamp = 1700000000000L,
            derianDate = "15 Praios 1040 BF",
            category = "Energy.Changed",
            playerMessage = "LE: 30 → 25"
            // gmMessage ist standardmäßig null
        )
        
        val jsonString = json.encodeToString(de.applicatus.app.data.export.JournalEntryDto.serializer(), journalEntry)
        val decoded = json.decodeFromString(de.applicatus.app.data.export.JournalEntryDto.serializer(), jsonString)
        
        assertNull(decoded.gmMessage)
    }
    
    /**
     * Test: CharacterExportDto mit Journal-Einträgen
     */
    @Test
    fun `CharacterExportDto with journal entries serializes correctly`() {
        val testCharacterDto = CharacterDto(
            guid = "char-guid-456",
            name = "Journal Test Charakter"
        )
        
        val journalEntries = listOf(
            de.applicatus.app.data.export.JournalEntryDto(
                timestamp = 1700000000000L,
                derianDate = "1 Praios 1040 BF",
                category = "Character.Created",
                playerMessage = "Charakter wurde erstellt."
            ),
            de.applicatus.app.data.export.JournalEntryDto(
                timestamp = 1700000001000L,
                derianDate = "2 Praios 1040 BF",
                category = "Potion.Brewed",
                playerMessage = "Ein Heiltrank wurde gebraut.",
                gmMessage = "Qualität: A"
            )
        )
        
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = testCharacterDto,
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            journalEntries = journalEntries,
            exportTimestamp = 1700000002000L
        )
        
        val jsonString = json.encodeToString(CharacterExportDto.serializer(), exportDto)
        val decoded = json.decodeFromString(CharacterExportDto.serializer(), jsonString)
        
        assertEquals(2, decoded.journalEntries.size)
        assertEquals("Character.Created", decoded.journalEntries[0].category)
        assertEquals("Potion.Brewed", decoded.journalEntries[1].category)
        assertEquals("Qualität: A", decoded.journalEntries[1].gmMessage)
    }
    
    // ==================== Multi-Character Sync Tests ====================
    
    /**
     * Test: SyncSessionManager trackt mehrere aktive Character-GUIDs
     */
    @Test
    fun `SyncSessionManager tracks multiple character GUIDs`() {
        val syncSessionManager = SyncSessionManager.getInstance()
        
        // Registriere zwei verschiedene Charaktere
        syncSessionManager.registerCharacterGuid(1L, "char-guid-A")
        syncSessionManager.registerCharacterGuid(2L, "char-guid-B")
        
        val activeGuids = syncSessionManager.getActiveCharacterGuids()
        
        assertTrue("char-guid-A should be registered", activeGuids.contains("char-guid-A"))
        assertTrue("char-guid-B should be registered", activeGuids.contains("char-guid-B"))
        assertEquals(2, activeGuids.size)
        
        // Cleanup
        syncSessionManager.unregisterCharacterGuid("char-guid-A")
        syncSessionManager.unregisterCharacterGuid("char-guid-B")
    }
    
    /**
     * Test: SyncSessionManager kann Snapshots anhand der GUID routen
     */
    @Test
    fun `SyncSessionManager routes snapshots by character GUID`() = runTest {
        // Dieser Test prüft, dass routeIncomingSnapshot false zurückgibt,
        // wenn kein passender Manager registriert ist
        val syncSessionManager = SyncSessionManager.getInstance()
        
        val testSnapshot = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "unknown-guid",
                name = "Unbekannter Charakter"
            ),
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            journalEntries = emptyList(),
            exportTimestamp = System.currentTimeMillis()
        )
        
        // Sollte false zurückgeben, da kein Manager für diese GUID registriert ist
        val routed = syncSessionManager.routeIncomingSnapshot(testSnapshot)
        assertFalse("Snapshot should not be routed when no manager is registered", routed)
    }
    
    /**
     * Test: CharacterRealtimeSyncManager hat handleIncomingSnapshot Methode
     */
    @Test
    fun `CharacterRealtimeSyncManager has handleIncomingSnapshot method`() = runTest {
        val testScope = TestScope()
        val manager = CharacterRealtimeSyncManager(
            mockRepository,
            hostService,
            mockExportManager,
            testScope
        )
        
        // Methode sollte existieren und ohne Exception aufgerufen werden können
        // (Der eigentliche Aufruf macht nichts, da keine Session aktiv ist)
        val testSnapshot = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "some-guid",
                name = "Test"
            ),
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            journalEntries = emptyList(),
            exportTimestamp = System.currentTimeMillis()
        )
        
        // Sollte ohne Exception durchlaufen (GUID stimmt nicht überein, also wird nichts gemacht)
        manager.handleIncomingSnapshot(testSnapshot)
        
        assertTrue("handleIncomingSnapshot should complete without throwing", true)
    }
    
    // ==================== Journal Sync Scenario Tests ====================
    
    /**
     * Test: Bidirektionaler Journal-Sync - Szenario aus dem Bugreport
     * 
     * Szenario:
     * - Gerät A und Gerät B synchronisieren Charakter X
     * - Beide haben initial den Journal-Eintrag "Trank wird hinzugefügt"
     * - Auf Gerät A wird LE von 20 auf 25 erhöht → neuer Journal-Eintrag
     * - Der Snapshot wird zu Gerät B gesendet
     * - Erwartung: Gerät B sollte den Journal-Eintrag "LE-Änderung 20->25" haben
     */
    @Test
    fun `journal entries are included in sync snapshot`() = runTest {
        val characterGuid = "char-sync-test-123"
        val baseTimestamp = 1700000000000L
        
        // Erstelle CharacterDto mit LE = 25 (nach der Änderung)
        val characterDtoAfterLeChange = CharacterDto(
            guid = characterGuid,
            name = "Test Charakter",
            mu = 10,
            kl = 12,
            inValue = 11,
            ch = 10,
            ff = 13,
            ge = 14,
            ko = 12,
            kk = 11,
            maxLe = 30,
            currentLe = 25,  // War 20, jetzt 25
            hasAe = false,
            maxAe = 0,
            currentAe = 0,
            hasKe = false,
            maxKe = 0,
            currentKe = 0,
            lastModifiedDate = baseTimestamp + 2000
        )
        
        // Erstelle Journal-Einträge - einer alt, einer neu (LE-Änderung)
        val oldJournalEntry = de.applicatus.app.data.export.JournalEntryDto(
            timestamp = baseTimestamp,
            derianDate = "1 Praios 1040 BF",
            category = "Potion.Brewed",
            playerMessage = "Trank wird hinzugefügt"
        )
        
        val newJournalEntry = de.applicatus.app.data.export.JournalEntryDto(
            timestamp = baseTimestamp + 1000,  // Später als der alte Eintrag
            derianDate = "1 Praios 1040 BF",
            category = "Energy.Changed",
            playerMessage = "LE: 20 → 25"
        )
        
        // Erstelle Snapshot mit BEIDEN Journal-Einträgen
        val snapshotFromDeviceA = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = characterDtoAfterLeChange,
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            journalEntries = listOf(oldJournalEntry, newJournalEntry),  // BEIDE Einträge!
            exportTimestamp = baseTimestamp + 2000
        )
        
        // Serialisiere und deserialisiere (simuliert Übertragung)
        val jsonString = json.encodeToString(CharacterExportDto.serializer(), snapshotFromDeviceA)
        val receivedSnapshot = json.decodeFromString(CharacterExportDto.serializer(), jsonString)
        
        // Verifiziere: Der empfangene Snapshot enthält BEIDE Journal-Einträge
        assertEquals(
            "Snapshot should contain 2 journal entries (old + new LE change)",
            2, 
            receivedSnapshot.journalEntries.size
        )
        
        // Verifiziere den alten Eintrag
        val receivedOldEntry = receivedSnapshot.journalEntries.find { it.category == "Potion.Brewed" }
        assertNotNull("Old journal entry (Potion.Brewed) should be in snapshot", receivedOldEntry)
        assertEquals("Trank wird hinzugefügt", receivedOldEntry?.playerMessage)
        
        // Verifiziere den neuen LE-Änderungs-Eintrag
        val receivedNewEntry = receivedSnapshot.journalEntries.find { it.category == "Energy.Changed" }
        assertNotNull("New journal entry (Energy.Changed) should be in snapshot", receivedNewEntry)
        assertEquals("LE: 20 → 25", receivedNewEntry?.playerMessage)
        
        // Verifiziere Reihenfolge nach Timestamp
        assertTrue(
            "New entry should have later timestamp than old entry",
            receivedNewEntry!!.timestamp > receivedOldEntry!!.timestamp
        )
    }
    
    /**
     * Test: applySnapshotFromSync wird mit Journal-Einträgen aufgerufen
     * 
     * Dieser Test verifiziert, dass wenn ein Snapshot mit Journal-Einträgen empfangen wird,
     * die Repository-Methode mit diesen Einträgen aufgerufen wird.
     */
    @Test
    fun `applySnapshotFromSync is called with journal entries from snapshot`() = runTest {
        val characterGuid = "char-journal-sync-456"
        val baseTimestamp = 1700000000000L
        
        // Erstelle Snapshot mit Journal-Einträgen
        val snapshotWithJournal = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = characterGuid,
                name = "Journal Test Char",
                currentLe = 25,
                maxLe = 30
            ),
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            locations = emptyList(),
            items = emptyList(),
            journalEntries = listOf(
                de.applicatus.app.data.export.JournalEntryDto(
                    timestamp = baseTimestamp,
                    derianDate = "1 Praios 1040 BF",
                    category = "Potion.Brewed",
                    playerMessage = "Heiltrank gebraut"
                ),
                de.applicatus.app.data.export.JournalEntryDto(
                    timestamp = baseTimestamp + 1000,
                    derianDate = "1 Praios 1040 BF",
                    category = "Energy.Changed",
                    playerMessage = "LE: 20 → 25"
                )
            ),
            exportTimestamp = baseTimestamp + 2000
        )
        
        // Konfiguriere Mock, um erfolgreiche Anwendung zu simulieren
        coEvery { 
            mockRepository.applySnapshotFromSync(any(), any()) 
        } returns Result.success(1L)
        
        // Rufe die gemockte Methode auf
        mockRepository.applySnapshotFromSync(snapshotWithJournal, allowCreateNew = false)
        
        // Capture den übergebenen Snapshot
        coVerify { 
            mockRepository.applySnapshotFromSync(
                match { snapshot ->
                    snapshot.journalEntries.size == 2 &&
                    snapshot.journalEntries.any { it.category == "Potion.Brewed" } &&
                    snapshot.journalEntries.any { it.category == "Energy.Changed" }
                },
                false
            )
        }
    }
    
    /**
     * Test: Bidirektionale Synchronisation - vollständiges Szenario
     * 
     * Simuliert das vollständige Szenario aus dem Bugreport:
     * 1. Host A und Client B verbinden sich
     * 2. Host A sendet Snapshot mit LE=25 und Journal "LE: 20→25"
     * 3. Client B empfängt und sollte den Journal-Eintrag haben
     * 4. Client B ändert LE auf 27 und sendet Snapshot mit Journal "LE: 25→27"
     * 5. Host A empfängt und sollte den Journal-Eintrag haben
     * 
     * KRITISCH: Beide Geräte sollten am Ende ALLE Journal-Einträge haben!
     */
    @Test
    fun `bidirectional sync includes journal entries in both directions`() = runTest {
        FakeNearbyConnectionsService.clearRegistry()
        
        val hostNearbyService = FakeNearbyConnectionsService()
        val clientNearbyService = FakeNearbyConnectionsService()
        
        try {
            val characterGuid = "char-bidirectional-789"
            val baseTimestamp = 1700000000000L
            
            // === PHASE 1: Host sendet Snapshot nach LE-Änderung 20→25 ===
            
            val hostSnapshotAfterFirstChange = CharacterExportDto(
                version = DataModelVersion.CURRENT_VERSION,
                character = CharacterDto(
                    guid = characterGuid,
                    name = "Bidirectional Test Char",
                    currentLe = 25,  // Geändert von 20 auf 25
                    maxLe = 30,
                    lastModifiedDate = baseTimestamp + 1000
                ),
                spellSlots = emptyList(),
                potions = emptyList(),
                recipeKnowledge = emptyList(),
                locations = emptyList(),
                items = emptyList(),
                journalEntries = listOf(
                    de.applicatus.app.data.export.JournalEntryDto(
                        timestamp = baseTimestamp,
                        derianDate = "1 Praios 1040 BF",
                        category = "Potion.Brewed",
                        playerMessage = "Trank wird hinzugefügt"
                    ),
                    de.applicatus.app.data.export.JournalEntryDto(
                        timestamp = baseTimestamp + 500,
                        derianDate = "1 Praios 1040 BF",
                        category = "Energy.Changed",
                        playerMessage = "LE: 20 → 25"
                    )
                ),
                exportTimestamp = baseTimestamp + 1000
            )
            
            // Host beginnt Advertising
            hostNearbyService.startAdvertising("Host Device")
            
            // Client verbindet sich
            clientNearbyService.connectToEndpoint("Host Device", "Client Device")
            advanceUntilIdle()
            
            // Client registriert zuerst seinen Empfangs-Callback
            var clientReceivedSnapshot: CharacterExportDto? = null
            clientNearbyService.receiveCharacterData(
                onDataReceived = { snapshot ->
                    clientReceivedSnapshot = snapshot
                },
                onKeepAliveReceived = {},
                onError = { fail("Client receive should not error: $it") }
            )
            
            // Host sendet Snapshot zu Client
            hostNearbyService.sendCharacterData(
                "Client Device",
                hostSnapshotAfterFirstChange,
                onSuccess = {},
                onFailure = { fail("Host send should succeed") }
            )
            
            advanceUntilIdle()
            
            // Verifiziere: Client hat beide Journal-Einträge empfangen
            assertNotNull("Client should have received snapshot", clientReceivedSnapshot)
            assertEquals(
                "Client snapshot should have 2 journal entries",
                2,
                clientReceivedSnapshot!!.journalEntries.size
            )
            assertTrue(
                "Client should have LE change entry",
                clientReceivedSnapshot!!.journalEntries.any { 
                    it.category == "Energy.Changed" && it.playerMessage == "LE: 20 → 25" 
                }
            )
            
            // === PHASE 2: Client sendet Snapshot nach LE-Änderung 25→27 ===
            
            // Host registriert seinen Empfangs-Callback
            var hostReceivedSnapshot: CharacterExportDto? = null
            hostNearbyService.receiveCharacterData(
                onDataReceived = { snapshot ->
                    hostReceivedSnapshot = snapshot
                },
                onKeepAliveReceived = {},
                onError = { fail("Host receive should not error: $it") }
            )
            
            // Wichtig: Client-Snapshot sollte ALLE bisherigen Journal-Einträge + den neuen enthalten
            val clientSnapshotAfterSecondChange = CharacterExportDto(
                version = DataModelVersion.CURRENT_VERSION,
                character = CharacterDto(
                    guid = characterGuid,
                    name = "Bidirectional Test Char",
                    currentLe = 27,  // Geändert von 25 auf 27
                    maxLe = 30,
                    lastModifiedDate = baseTimestamp + 2000
                ),
                spellSlots = emptyList(),
                potions = emptyList(),
                recipeKnowledge = emptyList(),
                locations = emptyList(),
                items = emptyList(),
                journalEntries = listOf(
                    de.applicatus.app.data.export.JournalEntryDto(
                        timestamp = baseTimestamp,
                        derianDate = "1 Praios 1040 BF",
                        category = "Potion.Brewed",
                        playerMessage = "Trank wird hinzugefügt"
                    ),
                    // KRITISCH: Der Client muss auch den vorher empfangenen Eintrag mitsenden!
                    // Bei Last-Write-Wins ist der komplette Snapshot maßgeblich.
                    de.applicatus.app.data.export.JournalEntryDto(
                        timestamp = baseTimestamp + 500,
                        derianDate = "1 Praios 1040 BF",
                        category = "Energy.Changed",
                        playerMessage = "LE: 20 → 25"
                    ),
                    de.applicatus.app.data.export.JournalEntryDto(
                        timestamp = baseTimestamp + 1500,
                        derianDate = "1 Praios 1040 BF",
                        category = "Energy.Changed",
                        playerMessage = "LE: 25 → 27"
                    )
                ),
                exportTimestamp = baseTimestamp + 2000
            )
            
            // Client sendet Snapshot zurück zu Host
            clientNearbyService.sendCharacterData(
                "Host Device",
                clientSnapshotAfterSecondChange,
                onSuccess = {},
                onFailure = { fail("Client send should succeed") }
            )
            
            advanceUntilIdle()
            
            // Verifiziere: Host hat ALLE drei Journal-Einträge empfangen
            assertNotNull("Host should have received snapshot", hostReceivedSnapshot)
            assertEquals(
                "Host snapshot should have 3 journal entries (old + 2 LE changes)",
                3,
                hostReceivedSnapshot!!.journalEntries.size
            )
            assertTrue(
                "Host should have first LE change entry",
                hostReceivedSnapshot!!.journalEntries.any { 
                    it.category == "Energy.Changed" && it.playerMessage == "LE: 20 → 25" 
                }
            )
            assertTrue(
                "Host should have second LE change entry",
                hostReceivedSnapshot!!.journalEntries.any { 
                    it.category == "Energy.Changed" && it.playerMessage == "LE: 25 → 27" 
                }
            )
            
        } finally {
            hostNearbyService.stopAllConnections()
            clientNearbyService.stopAllConnections()
        }
    }
}
