package de.applicatus.app.data.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.export.CharacterExportDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Service für die Nearby Connections API zur Synchronisation von Charakteren.
 */
class NearbyConnectionsService(private val context: Context) {
    
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val json = Json { 
        prettyPrint = false
        ignoreUnknownKeys = true
    }
    
    companion object {
        private const val SERVICE_ID = "de.applicatus.app.nearby"
        private val STRATEGY = Strategy.P2P_STAR
    }
    
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
    fun startAdvertising(deviceName: String): Flow<ConnectionState> = callbackFlow {
        trySend(ConnectionState.Advertising)
        
        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                // Automatisch akzeptieren (in Produktions-App sollte der Nutzer bestätigen)
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            }
            
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        trySend(ConnectionState.Connected(endpointId, "Remote Device"))
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        trySend(ConnectionState.Error("Verbindung abgelehnt"))
                    }
                    else -> {
                        trySend(ConnectionState.Error("Verbindung fehlgeschlagen"))
                    }
                }
            }
            
            override fun onDisconnected(endpointId: String) {
                trySend(ConnectionState.Disconnected("Verbindung getrennt"))
            }
        }
        
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()
        
        connectionsClient.startAdvertising(
            deviceName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            // Erfolgreich gestartet
        }.addOnFailureListener { e ->
            trySend(ConnectionState.Error("Advertising fehlgeschlagen: ${e.message}"))
            close()
        }
        
        awaitClose {
            connectionsClient.stopAdvertising()
        }
    }
    
    /**
     * Startet die Suche (Discovery) nach anderen Geräten.
     */
    fun startDiscovery(onDeviceFound: (String, String) -> Unit): Flow<ConnectionState> = callbackFlow {
        trySend(ConnectionState.Discovering)
        
        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                onDeviceFound(endpointId, info.endpointName)
            }
            
            override fun onEndpointLost(endpointId: String) {
                // Gerät verloren
            }
        }
        
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()
        
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            // Erfolgreich gestartet
        }.addOnFailureListener { e ->
            trySend(ConnectionState.Error("Discovery fehlgeschlagen: ${e.message}"))
            close()
        }
        
        awaitClose {
            connectionsClient.stopDiscovery()
        }
    }
    
    /**
     * Stellt eine Verbindung zu einem entdeckten Gerät her.
     */
    fun connectToEndpoint(
        endpointId: String,
        deviceName: String
    ): Flow<ConnectionState> = callbackFlow {
        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            }
            
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        trySend(ConnectionState.Connected(endpointId, deviceName))
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        trySend(ConnectionState.Error("Verbindung abgelehnt"))
                    }
                    else -> {
                        trySend(ConnectionState.Error("Verbindung fehlgeschlagen"))
                    }
                }
            }
            
            override fun onDisconnected(endpointId: String) {
                trySend(ConnectionState.Disconnected("Verbindung getrennt"))
            }
        }
        
        connectionsClient.requestConnection(
            deviceName,
            endpointId,
            connectionLifecycleCallback
        ).addOnFailureListener { e ->
            trySend(ConnectionState.Error("Verbindungsanfrage fehlgeschlagen: ${e.message}"))
            close()
        }
        
        awaitClose {
            connectionsClient.disconnectFromEndpoint(endpointId)
        }
    }
    
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes()
                if (data != null) {
                    onDataReceived?.invoke(data)
                }
            }
        }
        
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                onTransferSuccess?.invoke()
            } else if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                onTransferFailure?.invoke("Transfer fehlgeschlagen")
            }
        }
    }
    
    private var onDataReceived: ((ByteArray) -> Unit)? = null
    private var onTransferSuccess: (() -> Unit)? = null
    private var onTransferFailure: ((String) -> Unit)? = null
    
    /**
     * Sendet Charakterdaten an ein verbundenes Gerät.
     */
    fun sendCharacterData(
        endpointId: String,
        characterData: CharacterExportDto,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val jsonString = json.encodeToString(characterData)
            val payload = Payload.fromBytes(jsonString.toByteArray())
            
            onTransferSuccess = onSuccess
            onTransferFailure = onFailure
            
            connectionsClient.sendPayload(endpointId, payload)
                .addOnFailureListener { e ->
                    onFailure("Senden fehlgeschlagen: ${e.message}")
                }
        } catch (e: Exception) {
            onFailure("Fehler beim Vorbereiten der Daten: ${e.message}")
        }
    }
    
    /**
     * Empfängt Charakterdaten von einem verbundenen Gerät.
     */
    fun receiveCharacterData(
        onDataReceived: (CharacterExportDto) -> Unit,
        onError: (String) -> Unit
    ) {
        this.onDataReceived = fun(data: ByteArray) {
            try {
                val jsonString = String(data)
                val characterData = json.decodeFromString<CharacterExportDto>(jsonString)
                
                // Versionscheck
                val (isCompatible, warning) = DataModelVersion.checkCompatibility(characterData.version)
                if (!isCompatible) {
                    onError(warning ?: "Inkompatible Version")
                    return
                }
                
                onDataReceived(characterData)
            } catch (e: Exception) {
                onError("Fehler beim Verarbeiten der Daten: ${e.message}")
            }
        }
    }
    
    /**
     * Trennt alle Verbindungen.
     */
    fun stopAllConnections() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }
}
