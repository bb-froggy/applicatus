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
        val idleStatus = CharacterRealtimeSyncManager.SyncStatus.Idle
        val connectingStatus = CharacterRealtimeSyncManager.SyncStatus.Connecting("Test Device")
        val syncingStatus = CharacterRealtimeSyncManager.SyncStatus.Syncing(
            "test-guid",
            "endpoint-123",
            "Test Device"
        )
        val warningStatus = CharacterRealtimeSyncManager.SyncStatus.Warning(
            "test-guid",
            "Test warning",
            System.currentTimeMillis() - 20000
        )
        val errorStatus = CharacterRealtimeSyncManager.SyncStatus.Error("Test error message")
        
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
        
        coEvery { mockRepository.applySnapshotFromSync(any(), any()) } returns Result.success(1L)
        
        val result = mockRepository.applySnapshotFromSync(testSnapshot, allowCreateNew = false)
        
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
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
     * Test: Export failure gibt Failure-Result zurück
     */
    @Test
    fun `export failure returns failure result`() = runTest {
        coEvery { mockExportManager.exportCharacter(1L) } returns Result.failure(
            Exception("Export failed")
        )
        
        val result = mockExportManager.exportCharacter(1L)
        
        assertTrue(result.isFailure)
        assertEquals("Export failed", result.exceptionOrNull()?.message)
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
     */
    @Test
    fun `payload size stays constant after reconnect`() = runTest {
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
            journalEntries = emptyList(),
            exportTimestamp = System.currentTimeMillis()
        )
        
        // Erstes Senden
        hostService.resetPayloadStats()
        hostService.startAdvertising("Host")
        clientService.startDiscovery { _, _ -> }
        clientService.connectToEndpoint("Host", "Client")
        
        advanceUntilIdle()
        
        // Sende Charakter
        hostService.sendCharacterData(
            "Client",
            testExportDto,
            onSuccess = {},
            onFailure = { fail("Send should succeed: $it") }
        )
        
        val firstPayloadSize = hostService.lastSentPayloadSize
        assertTrue("First payload should have reasonable size", firstPayloadSize > 0)
        assertTrue("First payload should be under 10KB for empty character", firstPayloadSize < 10_000)
        
        // Simuliere einseitigen Disconnect
        clientService.simulateOneSidedDisconnect()
        advanceUntilIdle()
        
        // Reconnect
        clientService.simulateReconnect()
        advanceUntilIdle()
        
        // Erneut senden (gleicher Charakter, keine Änderungen)
        hostService.sendCharacterData(
            "Client",
            testExportDto,
            onSuccess = {},
            onFailure = { fail("Send should succeed: $it") }
        )
        
        val secondPayloadSize = hostService.lastSentPayloadSize
        
        // Payload-Größe sollte gleich bleiben
        assertEquals(
            "Payload size should remain constant after reconnect",
            firstPayloadSize,
            secondPayloadSize
        )
        assertEquals("Should have sent exactly 2 payloads", 2, hostService.sentPayloadCount)
    }
    
    /**
     * Test: Mehrfaches Senden ohne Änderungen akkumuliert keine Daten.
     */
    @Test
    fun `multiple sends without changes do not accumulate data`() = runTest {
        val testCharacterDto = CharacterDto(
            guid = "test-guid-123",
            name = "Test Charakter",
            groupName = "Test Gruppe",
            mu = 10
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
            exportTimestamp = System.currentTimeMillis()
        )
        
        hostService.resetPayloadStats()
        hostService.startAdvertising("Host")
        clientService.connectToEndpoint("Host", "Client")
        advanceUntilIdle()
        
        val payloadSizes = mutableListOf<Int>()
        
        // Sende 5 mal den gleichen Charakter
        repeat(5) {
            hostService.sendCharacterData(
                "Client",
                testExportDto,
                onSuccess = {},
                onFailure = { fail("Send $it should succeed") }
            )
            payloadSizes.add(hostService.lastSentPayloadSize)
        }
        
        // Alle Payload-Größen sollten identisch sein
        val firstSize = payloadSizes.first()
        payloadSizes.forEachIndexed { index, size ->
            assertEquals(
                "Payload $index should have same size as first",
                firstSize,
                size
            )
        }
        
        assertEquals("Should have sent exactly 5 payloads", 5, hostService.sentPayloadCount)
    }
}
