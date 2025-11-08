package de.applicatus.app.data.nearby

import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.export.CharacterExportDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake-Implementierung von NearbyConnectionsInterface für Tests.
 * Ermöglicht direkte Verbindung zwischen zwei Instanzen ohne echte Nearby-API.
 */
class FakeNearbyConnectionsService : NearbyConnectionsInterface {
    
    companion object {
        // Zentrale Verwaltung aller Fake-Services für simulierte Verbindungen
        private val registry = mutableMapOf<String, FakeNearbyConnectionsService>()
        
        /**
         * Registriert einen Service unter einem Namen.
         */
        fun register(deviceName: String, service: FakeNearbyConnectionsService) {
            registry[deviceName] = service
        }
        
        /**
         * Entfernt alle registrierten Services (für Test-Cleanup).
         */
        fun clearRegistry() {
            registry.clear()
        }
        
        /**
         * Findet einen registrierten Service nach Namen.
         */
        fun findService(deviceName: String): FakeNearbyConnectionsService? {
            return registry[deviceName]
        }
    }
    
    private val _connectionState = MutableStateFlow<NearbyConnectionsInterface.ConnectionState>(
        NearbyConnectionsInterface.ConnectionState.Idle
    )
    val connectionStateFlow: Flow<NearbyConnectionsInterface.ConnectionState> = _connectionState.asStateFlow()
    
    private var connectedEndpoint: String? = null
    private var connectedPeer: FakeNearbyConnectionsService? = null
    
    private var dataReceivedCallback: ((CharacterExportDto) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    
    var deviceName: String = "Test Device"
        private set
    
    override fun startAdvertising(deviceName: String): Flow<NearbyConnectionsInterface.ConnectionState> {
        this.deviceName = deviceName
        register(deviceName, this)
        _connectionState.value = NearbyConnectionsInterface.ConnectionState.Advertising
        return connectionStateFlow
    }
    
    override fun startDiscovery(onDeviceFound: (String, String) -> Unit): Flow<NearbyConnectionsInterface.ConnectionState> {
        _connectionState.value = NearbyConnectionsInterface.ConnectionState.Discovering
        
        // Simuliere Discovery: Finde alle anderen registrierten Geräte
        registry.forEach { (name, service) ->
            if (service != this && service._connectionState.value is NearbyConnectionsInterface.ConnectionState.Advertising) {
                // Rufe Callback mit gefundenem Gerät auf
                onDeviceFound(name, name) // endpointId = deviceName für Tests
            }
        }
        
        return connectionStateFlow
    }
    
    override fun connectToEndpoint(
        endpointId: String,
        deviceName: String
    ): Flow<NearbyConnectionsInterface.ConnectionState> {
        val targetService = findService(endpointId)
        
        if (targetService == null) {
            _connectionState.value = NearbyConnectionsInterface.ConnectionState.Error("Gerät nicht gefunden")
            return connectionStateFlow
        }
        
        // Simuliere erfolgreiche Verbindung
        connectedEndpoint = endpointId
        connectedPeer = targetService
        
        // Bidirektionale Verbindung herstellen
        targetService.connectedEndpoint = this.deviceName
        targetService.connectedPeer = this
        
        _connectionState.value = NearbyConnectionsInterface.ConnectionState.Connected(endpointId, deviceName)
        targetService._connectionState.value = NearbyConnectionsInterface.ConnectionState.Connected(
            this.deviceName, 
            this.deviceName
        )
        
        return connectionStateFlow
    }
    
    override fun sendCharacterData(
        endpointId: String,
        characterData: CharacterExportDto,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (connectedPeer == null) {
            onFailure("Keine Verbindung")
            return
        }
        
        // Versionscheck vor dem Senden
        val (isCompatible, warning) = DataModelVersion.checkCompatibility(characterData.version)
        if (!isCompatible) {
            onFailure(warning ?: "Inkompatible Version")
            return
        }
        
        try {
            // Direkte Übertragung zum verbundenen Peer
            connectedPeer?.receiveData(characterData)
            onSuccess()
        } catch (e: Exception) {
            onFailure("Fehler beim Senden: ${e.message}")
        }
    }
    
    override fun receiveCharacterData(
        onDataReceived: (CharacterExportDto) -> Unit,
        onError: (String) -> Unit
    ) {
        this.dataReceivedCallback = onDataReceived
        this.errorCallback = onError
    }
    
    /**
     * Interne Methode zum Empfangen von Daten (wird vom Sender aufgerufen).
     */
    private fun receiveData(characterData: CharacterExportDto) {
        // Versionscheck
        val (isCompatible, warning) = DataModelVersion.checkCompatibility(characterData.version)
        if (!isCompatible) {
            errorCallback?.invoke(warning ?: "Inkompatible Version")
            return
        }
        
        dataReceivedCallback?.invoke(characterData)
    }
    
    override fun stopAllConnections() {
        connectedEndpoint?.let { endpoint ->
            connectedPeer?._connectionState?.value = 
                NearbyConnectionsInterface.ConnectionState.Disconnected("Verbindung getrennt")
        }
        
        connectedEndpoint = null
        connectedPeer = null
        _connectionState.value = NearbyConnectionsInterface.ConnectionState.Idle
    }
    
    /**
     * Hilfsmethode für Tests: Simuliert einen Verbindungsabbruch.
     */
    fun simulateDisconnect() {
        connectedPeer?._connectionState?.value = 
            NearbyConnectionsInterface.ConnectionState.Disconnected("Verbindung verloren")
        _connectionState.value = NearbyConnectionsInterface.ConnectionState.Disconnected("Verbindung verloren")
        connectedEndpoint = null
        connectedPeer = null
    }
    
    /**
     * Hilfsmethode für Tests: Simuliert einen Fehler.
     */
    fun simulateError(message: String) {
        _connectionState.value = NearbyConnectionsInterface.ConnectionState.Error(message)
    }
}
