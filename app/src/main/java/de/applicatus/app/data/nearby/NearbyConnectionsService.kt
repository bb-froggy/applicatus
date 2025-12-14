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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Service für die Nearby Connections API zur Synchronisation von Charakteren.
 */
class NearbyConnectionsService(private val context: Context) : NearbyConnectionsInterface {
    
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val json = Json { 
        prettyPrint = false
        ignoreUnknownKeys = true
    }
    
    companion object {
        private const val SERVICE_ID = "de.applicatus.app.nearby"
        private val STRATEGY = Strategy.P2P_STAR
        private const val MAX_PAYLOAD_BYTES = 1_047_552 // Nearby Connections limit (~1 MB)
    }
    
    /**
     * Startet die Werbung (Advertising) für eingehende Verbindungen.
     */
    override fun startAdvertising(deviceName: String): Flow<NearbyConnectionsInterface.ConnectionState> = callbackFlow {
        trySend(NearbyConnectionsInterface.ConnectionState.Advertising)
        
        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                // Automatisch akzeptieren (in Produktions-App sollte der Nutzer bestätigen)
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            }
            
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        trySend(NearbyConnectionsInterface.ConnectionState.Connected(endpointId, "Remote Device"))
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        trySend(NearbyConnectionsInterface.ConnectionState.Error("Verbindung abgelehnt"))
                    }
                    else -> {
                        trySend(NearbyConnectionsInterface.ConnectionState.Error("Verbindung fehlgeschlagen"))
                    }
                }
            }
            
            override fun onDisconnected(endpointId: String) {
                trySend(NearbyConnectionsInterface.ConnectionState.Disconnected("Verbindung getrennt"))
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
            trySend(NearbyConnectionsInterface.ConnectionState.Error("Advertising fehlgeschlagen: ${e.message}"))
            close()
        }
        
        awaitClose {
            connectionsClient.stopAdvertising()
        }
    }
    
    /**
     * Startet die Suche (Discovery) nach anderen Geräten.
     */
    override fun startDiscovery(onDeviceFound: (String, String) -> Unit): Flow<NearbyConnectionsInterface.ConnectionState> = callbackFlow {
        trySend(NearbyConnectionsInterface.ConnectionState.Discovering)
        
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
            trySend(NearbyConnectionsInterface.ConnectionState.Error("Discovery fehlgeschlagen: ${e.message}"))
            close()
        }
        
        awaitClose {
            connectionsClient.stopDiscovery()
        }
    }
    
    /**
     * Stellt eine Verbindung zu einem entdeckten Gerät her.
     */
    override fun connectToEndpoint(
        endpointId: String,
        deviceName: String
    ): Flow<NearbyConnectionsInterface.ConnectionState> = callbackFlow {
        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            }
            
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        trySend(NearbyConnectionsInterface.ConnectionState.Connected(endpointId, deviceName))
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        trySend(NearbyConnectionsInterface.ConnectionState.Error("Verbindung abgelehnt"))
                    }
                    else -> {
                        trySend(NearbyConnectionsInterface.ConnectionState.Error("Verbindung fehlgeschlagen"))
                    }
                }
            }
            
            override fun onDisconnected(endpointId: String) {
                trySend(NearbyConnectionsInterface.ConnectionState.Disconnected("Verbindung getrennt"))
            }
        }
        
        connectionsClient.requestConnection(
            deviceName,
            endpointId,
            connectionLifecycleCallback
        ).addOnFailureListener { e ->
            trySend(NearbyConnectionsInterface.ConnectionState.Error("Verbindungsanfrage fehlgeschlagen: ${e.message}"))
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
    override fun sendCharacterData(
        endpointId: String,
        characterData: CharacterExportDto,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val jsonString = json.encodeToString(characterData)
            val rawBytes = jsonString.toByteArray()
            val compressedBytes = compressBytes(rawBytes)
            
            android.util.Log.d("NearbySync", "Payload size: raw=${rawBytes.size} bytes, compressed=${compressedBytes.size} bytes")
            android.util.Log.d("NearbySync", "Character: ${characterData.character.name}, spellSlots=${characterData.spellSlots.size}, potions=${characterData.potions.size}, items=${characterData.items.size}, journal=${characterData.journalEntries.size}")
            
            if (compressedBytes.size > MAX_PAYLOAD_BYTES) {
                onFailure("Snapshot zu groß (${compressedBytes.size / 1024} KB, unkomprimiert ${rawBytes.size / 1024} KB). Kürze Journal oder Inventar und versuche es erneut.")
                return
            }
            val payload = Payload.fromBytes(compressedBytes)
            
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
    override fun receiveCharacterData(
        onDataReceived: (CharacterExportDto) -> Unit,
        onError: (String) -> Unit
    ) {
        this.onDataReceived = fun(data: ByteArray) {
            try {
                val decodedBytes = if (isGzipCompressed(data)) {
                    try {
                        decompressBytes(data)
                    } catch (e: Exception) {
                        onError("Komprimierte Daten konnten nicht entpackt werden: ${e.message}")
                        return
                    }
                } else {
                    data
                }
                val jsonString = String(decodedBytes)
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
     * Stoppt nur die Discovery.
     */
    override fun stopDiscovery() {
        connectionsClient.stopDiscovery()
    }
    
    /**
     * Trennt alle Verbindungen.
     */
    override fun stopAllConnections() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }

    private fun compressBytes(input: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { gzip ->
            gzip.write(input)
        }
        return output.toByteArray()
    }

    private fun decompressBytes(input: ByteArray): ByteArray {
        return GZIPInputStream(ByteArrayInputStream(input)).use { gzip ->
            gzip.readBytes()
        }
    }

    private fun isGzipCompressed(data: ByteArray): Boolean {
        return data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()
    }
}
