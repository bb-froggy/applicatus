package de.applicatus.app.data.nearby

import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.export.CharacterDto
import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.SpellSlotDto
import de.applicatus.app.data.model.spell.SlotType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests für die Nearby-Verbindung mit simulierten Services.
 * Testet die Datenübertragung zwischen zwei Geräten ohne echte Nearby API.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyConnectionsTest {
    
    private lateinit var senderService: FakeNearbyConnectionsService
    private lateinit var receiverService: FakeNearbyConnectionsService
    
    @Before
    fun setup() {
        FakeNearbyConnectionsService.clearRegistry()
        senderService = FakeNearbyConnectionsService()
        receiverService = FakeNearbyConnectionsService()
    }
    
    @After
    fun tearDown() {
        senderService.stopAllConnections()
        receiverService.stopAllConnections()
        FakeNearbyConnectionsService.clearRegistry()
    }
    
    @Test
    fun `test advertising and discovery`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Empfänger startet Advertising
        val advertisingJob = launch(dispatcher) {
            receiverService.startAdvertising("Receiver Device")
        }
        advanceUntilIdle()
        
        // Prüfe, dass Empfänger im Advertising-Modus ist
        val receiverState = receiverService.connectionStateFlow.first()
        assertTrue(receiverState is NearbyConnectionsInterface.ConnectionState.Advertising)
        
        // Sender startet Discovery
        val discoveredDevices = mutableListOf<Pair<String, String>>()
        val discoveryJob = launch(dispatcher) {
            senderService.startDiscovery { endpointId, deviceName ->
                discoveredDevices.add(endpointId to deviceName)
            }
        }
        advanceUntilIdle()
        
        // Prüfe, dass Sender das Empfänger-Gerät gefunden hat
        assertEquals(1, discoveredDevices.size)
        assertEquals("Receiver Device", discoveredDevices[0].first)
        assertEquals("Receiver Device", discoveredDevices[0].second)
        
        advertisingJob.cancel()
        discoveryJob.cancel()
    }
    
    @Test
    fun `test successful connection between two devices`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Empfänger startet Advertising
        launch(dispatcher) {
            receiverService.startAdvertising("Receiver Device")
        }
        advanceUntilIdle()
        
        // Sender verbindet sich mit Empfänger
        launch(dispatcher) {
            senderService.connectToEndpoint("Receiver Device", "Sender Device")
        }
        advanceUntilIdle()
        
        // Prüfe, dass beide verbunden sind
        val senderState = senderService.connectionStateFlow.first()
        val receiverState = receiverService.connectionStateFlow.first()
        
        assertTrue(senderState is NearbyConnectionsInterface.ConnectionState.Connected)
        assertTrue(receiverState is NearbyConnectionsInterface.ConnectionState.Connected)
        
        if (senderState is NearbyConnectionsInterface.ConnectionState.Connected) {
            assertEquals("Receiver Device", senderState.endpointId)
        }
    }
    
    @Test
    fun `test character data transfer from sender to receiver`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Testdaten erstellen
        val characterData = createTestCharacterData()
        
        // Setup: Verbindung herstellen
        launch(dispatcher) {
            receiverService.startAdvertising("Receiver Device")
        }
        advanceUntilIdle()
        
        launch(dispatcher) {
            senderService.connectToEndpoint("Receiver Device", "Sender Device")
        }
        advanceUntilIdle()
        
        // Empfänger bereit zum Empfangen
        var receivedData: CharacterExportDto? = null
        var receivedError: String? = null
        
        receiverService.receiveCharacterData(
            onDataReceived = { data ->
                receivedData = data
            },
            onError = { error ->
                receivedError = error
            }
        )
        
        // Sender sendet Daten
        var sendSuccess = false
        var sendError: String? = null
        
        senderService.sendCharacterData(
            endpointId = "Receiver Device",
            characterData = characterData,
            onSuccess = {
                sendSuccess = true
            },
            onFailure = { error ->
                sendError = error
            }
        )
        advanceUntilIdle()
        
        // Assertions
        assertTrue("Send should succeed", sendSuccess)
        assertNull("Send error should be null", sendError)
        assertNotNull("Should have received data", receivedData)
        assertNull("Receive error should be null", receivedError)
        
        // Prüfe, dass die empfangenen Daten korrekt sind
        receivedData?.let { data ->
            assertEquals(characterData.version, data.version)
            assertEquals(characterData.character.name, data.character.name)
            assertEquals(characterData.character.mu, data.character.mu)
            assertEquals(characterData.spellSlots.size, data.spellSlots.size)
        }
    }
    
    @Test
    fun `test bidirectional data transfer`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Zwei verschiedene Charaktere
        val character1 = createTestCharacterData("Character 1")
        val character2 = createTestCharacterData("Character 2")
        
        // Verbindung herstellen
        launch(dispatcher) {
            receiverService.startAdvertising("Device A")
        }
        advanceUntilIdle()
        
        launch(dispatcher) {
            senderService.connectToEndpoint("Device A", "Device B")
        }
        advanceUntilIdle()
        
        // Device A empfängt
        var dataReceivedByA: CharacterExportDto? = null
        receiverService.receiveCharacterData(
            onDataReceived = { data -> dataReceivedByA = data },
            onError = { }
        )
        
        // Device B empfängt
        var dataReceivedByB: CharacterExportDto? = null
        senderService.receiveCharacterData(
            onDataReceived = { data -> dataReceivedByB = data },
            onError = { }
        )
        
        // Device B sendet an Device A
        var sendByBSuccess = false
        senderService.sendCharacterData(
            endpointId = "Device A",
            characterData = character1,
            onSuccess = { sendByBSuccess = true },
            onFailure = { }
        )
        advanceUntilIdle()
        
        assertTrue("Device B send should succeed", sendByBSuccess)
        assertNotNull("Device A should have received data", dataReceivedByA)
        assertEquals("Character 1", dataReceivedByA?.character?.name)
        
        // Device A sendet an Device B (umgekehrte Richtung)
        var sendByASuccess = false
        receiverService.sendCharacterData(
            endpointId = "Device B",
            characterData = character2,
            onSuccess = { sendByASuccess = true },
            onFailure = { }
        )
        advanceUntilIdle()
        
        assertTrue("Device A send should succeed", sendByASuccess)
        assertNotNull("Device B should have received data", dataReceivedByB)
        assertEquals("Character 2", dataReceivedByB?.character?.name)
    }
    
    @Test
    fun `test connection failure when device not found`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Versuche zu verbinden ohne dass ein Gerät advertised
        launch(dispatcher) {
            senderService.connectToEndpoint("Nonexistent Device", "Sender")
        }
        advanceUntilIdle()
        
        val state = senderService.connectionStateFlow.first()
        assertTrue(state is NearbyConnectionsInterface.ConnectionState.Error)
        
        if (state is NearbyConnectionsInterface.ConnectionState.Error) {
            assertEquals("Gerät nicht gefunden", state.message)
        }
    }
    
    @Test
    fun `test send failure when not connected`() = runTest {
        val characterData = createTestCharacterData()
        
        var sendSuccess = false
        var sendError: String? = null
        
        senderService.sendCharacterData(
            endpointId = "Some Device",
            characterData = characterData,
            onSuccess = { sendSuccess = true },
            onFailure = { error -> sendError = error }
        )
        
        assertFalse("Send should fail", sendSuccess)
        assertNotNull("Should have error message", sendError)
        assertEquals("Keine Verbindung", sendError)
    }
    
    @Test
    fun `test disconnect functionality`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Verbindung herstellen
        launch(dispatcher) {
            receiverService.startAdvertising("Receiver")
        }
        advanceUntilIdle()
        
        launch(dispatcher) {
            senderService.connectToEndpoint("Receiver", "Sender")
        }
        advanceUntilIdle()
        
        // Prüfe Verbindung
        assertTrue(senderService.connectionStateFlow.first() is NearbyConnectionsInterface.ConnectionState.Connected)
        
        // Trenne Verbindung
        senderService.stopAllConnections()
        advanceUntilIdle()
        
        // Prüfe, dass beide getrennt sind
        val senderState = senderService.connectionStateFlow.first()
        val receiverState = receiverService.connectionStateFlow.first()
        
        assertTrue(senderState is NearbyConnectionsInterface.ConnectionState.Idle)
        assertTrue(receiverState is NearbyConnectionsInterface.ConnectionState.Disconnected)
    }
    
    @Test
    fun `test simulated disconnect`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Verbindung herstellen
        launch(dispatcher) {
            receiverService.startAdvertising("Receiver")
        }
        advanceUntilIdle()
        
        launch(dispatcher) {
            senderService.connectToEndpoint("Receiver", "Sender")
        }
        advanceUntilIdle()
        
        // Simuliere Verbindungsabbruch
        senderService.simulateDisconnect()
        advanceUntilIdle()
        
        val senderState = senderService.connectionStateFlow.first()
        assertTrue(senderState is NearbyConnectionsInterface.ConnectionState.Disconnected)
    }
    
    @Test
    fun `test version compatibility check on send`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Verbindung herstellen
        launch(dispatcher) {
            receiverService.startAdvertising("Receiver")
        }
        advanceUntilIdle()
        
        launch(dispatcher) {
            senderService.connectToEndpoint("Receiver", "Sender")
        }
        advanceUntilIdle()
        
        // Erstelle Daten mit zukünftiger Version (inkompatibel)
        val futureVersionData = createTestCharacterData().copy(
            version = DataModelVersion.CURRENT_VERSION + 1
        )
        
        var sendSuccess = false
        var sendError: String? = null
        
        senderService.sendCharacterData(
            endpointId = "Receiver",
            characterData = futureVersionData,
            onSuccess = { sendSuccess = true },
            onFailure = { error -> sendError = error }
        )
        advanceUntilIdle()
        
        assertFalse("Send should fail for incompatible version", sendSuccess)
        assertNotNull("Should have error message", sendError)
        assertTrue("Error should mention version", sendError!!.contains("Version") || sendError!!.contains("kompatibel"))
    }
    
    @Test
    fun `test multiple devices in registry`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        
        // Erstelle mehrere Empfänger
        val receiver1 = FakeNearbyConnectionsService()
        val receiver2 = FakeNearbyConnectionsService()
        val receiver3 = FakeNearbyConnectionsService()
        
        launch(dispatcher) {
            receiver1.startAdvertising("Device 1")
            receiver2.startAdvertising("Device 2")
            receiver3.startAdvertising("Device 3")
        }
        advanceUntilIdle()
        
        // Sender startet Discovery
        val discoveredDevices = mutableListOf<Pair<String, String>>()
        launch(dispatcher) {
            senderService.startDiscovery { endpointId, deviceName ->
                discoveredDevices.add(endpointId to deviceName)
            }
        }
        advanceUntilIdle()
        
        // Prüfe, dass alle 3 Geräte gefunden wurden
        assertEquals(3, discoveredDevices.size)
        assertTrue(discoveredDevices.any { it.first == "Device 1" })
        assertTrue(discoveredDevices.any { it.first == "Device 2" })
        assertTrue(discoveredDevices.any { it.first == "Device 3" })
    }
    
    // Hilfsfunktion zum Erstellen von Testdaten
    private fun createTestCharacterData(name: String = "Test Character"): CharacterExportDto {
        val character = CharacterDto(
            id = 1,
            guid = "test-guid-123",
            name = name,
            mu = 12,
            kl = 14,
            inValue = 13,
            ch = 11,
            ff = 10,
            ge = 12,
            ko = 11,
            kk = 10,
            hasApplicatus = true,
            applicatusZfw = 10,
            applicatusModifier = -2,
            hasAlchemy = false,
            alchemySkill = 0,
            hasCookingPotions = false,
            cookingPotionsSkill = 0,
            hasOdem = false,
            odemZfw = 0,
            hasAnalys = false,
            analysZfw = 0,
            currentLe = 28,
            maxLe = 28,
            currentAe = 15,
            maxAe = 15,
            currentKe = 25,
            maxKe = 25
        )
        
        val slots = listOf(
            SpellSlotDto(
                slotNumber = 0,
                spellId = 1,
                spellName = "Blitz dich find",
                zfw = 10,
                modifier = 0,
                variant = "",
                isFilled = false,
                zfpStar = null,
                lastRollResult = null,
                slotType = SlotType.APPLICATUS.name,
                volumePoints = 0,
                applicatusRollResult = null
            )
        )
        
        return CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = character,
            spellSlots = slots,
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            exportTimestamp = System.currentTimeMillis()
        )
    }
}
