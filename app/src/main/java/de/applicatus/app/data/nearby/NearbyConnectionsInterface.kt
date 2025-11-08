package de.applicatus.app.data.nearby

import de.applicatus.app.data.export.CharacterExportDto
import kotlinx.coroutines.flow.Flow

/**
 * Interface für Nearby Connections, um die Implementierung testbar zu machen.
 */
interface NearbyConnectionsInterface {
    
    sealed class ConnectionState {
        object Idle : ConnectionState()
        object Advertising : ConnectionState()
        object Discovering : ConnectionState()
        data class Connected(val endpointId: String, val endpointName: String) : ConnectionState()
        data class Disconnected(val reason: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
    
    sealed class TransferState {
        object Idle : TransferState()
        data class Sending(val progress: Int) : TransferState()
        data class Receiving(val progress: Int) : TransferState()
        data class Success(val data: CharacterExportDto) : TransferState()
        data class Failed(val error: String) : TransferState()
    }
    
    /**
     * Startet die Werbung (Advertising) für eingehende Verbindungen.
     */
    fun startAdvertising(deviceName: String): Flow<ConnectionState>
    
    /**
     * Startet die Suche (Discovery) nach anderen Geräten.
     */
    fun startDiscovery(onDeviceFound: (String, String) -> Unit): Flow<ConnectionState>
    
    /**
     * Stellt eine Verbindung zu einem entdeckten Gerät her.
     */
    fun connectToEndpoint(endpointId: String, deviceName: String): Flow<ConnectionState>
    
    /**
     * Sendet Charakterdaten an ein verbundenes Gerät.
     */
    fun sendCharacterData(
        endpointId: String,
        characterData: CharacterExportDto,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )
    
    /**
     * Empfängt Charakterdaten von einem verbundenen Gerät.
     */
    fun receiveCharacterData(
        onDataReceived: (CharacterExportDto) -> Unit,
        onError: (String) -> Unit
    )
    
    /**
     * Trennt alle Verbindungen.
     */
    fun stopAllConnections()
}
