package de.applicatus.app.data.sync

import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.CharacterExportManager
import de.applicatus.app.data.nearby.NearbyConnectionsInterface
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Verwaltet Echtzeit-Synchronisation von Charakterdaten über Nearby Connections.
 * 
 * Funktionsweise:
 * - Full-Snapshot-basiert: Bei jeder Änderung wird der komplette Charakter als CharacterExportDto gesendet
 * - Last-Write-Wins: Jeder empfangene Snapshot überschreibt den lokalen Stand
 * - Bidirektional: Beide Seiten (Host/Client) können senden und empfangen
 * - Keine Hintergrund-Synchronisation: Funktioniert nur, solange App aktiv ist
 * 
 * @param repository Repository für Charakterdaten
 * @param nearbyService Nearby Connections Service
 * @param exportManager Manager für Export/Import von Charakteren
 * @param scope CoroutineScope für async Operationen
 */
@OptIn(FlowPreview::class)
class CharacterRealtimeSyncManager(
    private val repository: ApplicatusRepository,
    private val nearbyService: NearbyConnectionsInterface,
    private val exportManager: CharacterExportManager,
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private enum class SessionRole { HOST, CLIENT }
    
    /**
     * Status einer Sync-Session
     */
    sealed class SyncStatus {
        /** Keine aktive Synchronisation */
        object Idle : SyncStatus()
        
        /** Session wird aufgebaut */
        data class Connecting(val deviceName: String) : SyncStatus()
        
        /** Aktive Synchronisation läuft */
        data class Syncing(
            val characterGuid: String,
            val endpointId: String,
            val endpointName: String
        ) : SyncStatus()
        
        /** Warnung: Keine erfolgreiche Übertragung seit > 15 Sekunden */
        data class Warning(
            val characterGuid: String,
            val message: String,
            val staleSince: Long
        ) : SyncStatus()
        
        /** Fehler aufgetreten */
        data class Error(val message: String) : SyncStatus()
    }
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private var observeJob: Job? = null
    private var sendJob: Job? = null
    private var receiveJob: Job? = null
    private var watchdogJob: Job? = null
    
    private var currentCharacterGuid: String? = null
    private var currentEndpointId: String? = null
    private var currentEndpointName: String? = null
    
    private var lastSuccessfulSendTime: Long = 0L
    private var lastSuccessfulReceiveTime: Long = 0L
    private var sessionRole: SessionRole? = null
    
    companion object {
        private const val DEBOUNCE_MS = 500L // Debounce für Änderungen
        private const val WATCHDOG_INTERVAL_MS = 2000L // Watchdog-Check alle 2 Sekunden
        private const val STALE_THRESHOLD_MS = 15000L // 15 Sekunden Warnschwelle
    }
    
    /**
     * Startet eine Host-Session (für einen einzelnen Charakter).
     * Der Host akzeptiert eingehende Verbindungen und synchronisiert den Charakter bidirektional.
     * 
     * @param characterId ID des zu synchronisierenden Charakters
     * @param deviceName Name dieses Geräts für Advertising
     */
    suspend fun startHostSession(characterId: Long, deviceName: String) {
        // Bei Host: Alte Session komplett beenden (inkl. stopAllConnections)
        stopSession()
        sessionRole = SessionRole.HOST
        
        // Charakter laden und GUID ermitteln
        val character = repository.getCharacterById(characterId)
            ?: throw IllegalArgumentException("Charakter mit ID $characterId nicht gefunden")
        
        currentCharacterGuid = character.guid
        _syncStatus.value = SyncStatus.Connecting(deviceName)
        
        // Advertising starten und auf Verbindung warten
        receiveJob = scope.launch {
            nearbyService.startAdvertising(deviceName).collectLatest { connectionState ->
                when (connectionState) {
                    is NearbyConnectionsInterface.ConnectionState.Connected -> {
                        currentEndpointId = connectionState.endpointId
                        currentEndpointName = connectionState.endpointName
                        lastSuccessfulSendTime = System.currentTimeMillis()
                        lastSuccessfulReceiveTime = System.currentTimeMillis()
                        
                        _syncStatus.value = SyncStatus.Syncing(
                            characterGuid = character.guid,
                            endpointId = connectionState.endpointId,
                            endpointName = connectionState.endpointName
                        )
                        
                        // Initialen Full-Sync senden
                        sendSnapshot(characterId, connectionState.endpointId)
                        
                        // Starte Beobachtung + Empfang + Watchdog
                        startCharacterObservation(characterId)
                        startReceiving()
                        startWatchdog()
                    }
                    is NearbyConnectionsInterface.ConnectionState.Error -> {
                        _syncStatus.value = SyncStatus.Error(connectionState.message)
                    }
                    is NearbyConnectionsInterface.ConnectionState.Disconnected -> {
                        handleHostDisconnect(deviceName)
                    }
                    else -> { /* Advertising, Discovering - warten */ }
                }
            }
        }
    }
    
    /**
     * Startet eine Client-Session (verbindet sich mit Host).
     * Der Client synchronisiert seinen Charakter bidirektional mit dem Host.
     * 
     * Wichtig: Der Aufrufer muss vorher die Discovery stoppen (nearbyService.stopDiscovery()),
     * um STATUS_OUT_OF_ORDER_API_CALL (8009) zu vermeiden.
     * 
     * @param characterId ID des zu synchronisierenden Charakters
     * @param hostEndpointId Endpoint-ID des Hosts
     * @param deviceName Name dieses Geräts
     */
    suspend fun startClientSession(characterId: Long, hostEndpointId: String, deviceName: String) {
        stopSession()
        sessionRole = SessionRole.CLIENT
        
        // Charakter laden und GUID ermitteln
        val character = repository.getCharacterById(characterId)
            ?: throw IllegalArgumentException("Charakter mit ID $characterId nicht gefunden")
        
        currentCharacterGuid = character.guid
        _syncStatus.value = SyncStatus.Connecting(deviceName)
        
        // Mit Host verbinden
        receiveJob = scope.launch {
            nearbyService.connectToEndpoint(hostEndpointId, deviceName).collectLatest { connectionState ->
                when (connectionState) {
                    is NearbyConnectionsInterface.ConnectionState.Connected -> {
                        currentEndpointId = connectionState.endpointId
                        currentEndpointName = connectionState.endpointName
                        lastSuccessfulSendTime = System.currentTimeMillis()
                        lastSuccessfulReceiveTime = System.currentTimeMillis()
                        
                        _syncStatus.value = SyncStatus.Syncing(
                            characterGuid = character.guid,
                            endpointId = connectionState.endpointId,
                            endpointName = connectionState.endpointName
                        )
                        
                        // Initialen Full-Sync senden
                        sendSnapshot(characterId, connectionState.endpointId)
                        
                        // Starte Beobachtung + Empfang + Watchdog
                        startCharacterObservation(characterId)
                        startReceiving()
                        startWatchdog()
                    }
                    is NearbyConnectionsInterface.ConnectionState.Error -> {
                        _syncStatus.value = SyncStatus.Error(connectionState.message)
                    }
                    is NearbyConnectionsInterface.ConnectionState.Disconnected -> {
                        handleClientDisconnect()
                    }
                    else -> { /* Connecting - warten */ }
                }
            }
        }
    }
    
    /**
     * Interne Methode zum Aufräumen der Jobs ohne Nearby-Verbindungen zu trennen.
     * Wird beim Start einer neuen Session verwendet.
     */
    private fun clearActiveDataPipelines(preserveCharacterGuid: Boolean) {
        observeJob?.cancel()
        observeJob = null
        sendJob?.cancel()
        sendJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        
        currentEndpointId = null
        currentEndpointName = null
        lastSuccessfulSendTime = 0L
        lastSuccessfulReceiveTime = 0L
        if (!preserveCharacterGuid) {
            currentCharacterGuid = null
        }
    }
    
    /**
     * Stoppt die aktuelle Sync-Session und trennt alle Verbindungen.
     */
    fun stopSession() {
        clearActiveDataPipelines(false)
        receiveJob?.cancel()
        receiveJob = null
        sessionRole = null
        nearbyService.stopAllConnections()
        _syncStatus.value = SyncStatus.Idle
    }

    private fun handleHostDisconnect(deviceName: String) {
        if (sessionRole != SessionRole.HOST) {
            stopSession()
            return
        }
        clearActiveDataPipelines(true)
        _syncStatus.value = SyncStatus.Connecting(deviceName)
    }

    private fun handleClientDisconnect() {
        if (sessionRole != SessionRole.CLIENT) {
            stopSession()
            return
        }
        clearActiveDataPipelines(false)
        receiveJob?.cancel()
        receiveJob = null
        sessionRole = null
        nearbyService.stopAllConnections()
        _syncStatus.value = SyncStatus.Error("Verbindung zum Host verloren. Bitte erneut verbinden.")
    }
    
    /**
     * Startet die Beobachtung des Charakters und sendet bei Änderungen Snapshots
     */
    private fun startCharacterObservation(characterId: Long) {
        observeJob?.cancel()
        observeJob = scope.launch {
            repository.getCharacterByIdFlow(characterId)
                .debounce(DEBOUNCE_MS)
                .collectLatest { character ->
                    if (character != null) {
                        currentEndpointId?.let { endpointId ->
                            sendSnapshot(characterId, endpointId)
                        }
                    }
                }
        }
    }
    
    /**
     * Sendet einen Full-Snapshot des Charakters
     */
    private suspend fun sendSnapshot(characterId: Long, endpointId: String) {
        try {
            val jsonResult = exportManager.exportCharacter(characterId)
            if (jsonResult.isFailure) {
                _syncStatus.value = SyncStatus.Error("Export fehlgeschlagen: ${jsonResult.exceptionOrNull()?.message}")
                return
            }
            
            val jsonString = jsonResult.getOrThrow()
            val snapshot = json.decodeFromString<CharacterExportDto>(jsonString)
            
            nearbyService.sendCharacterData(
                endpointId = endpointId,
                characterData = snapshot,
                onSuccess = {
                    lastSuccessfulSendTime = System.currentTimeMillis()
                },
                onFailure = { error ->
                    _syncStatus.value = SyncStatus.Error("Senden fehlgeschlagen: $error")
                }
            )
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("Snapshot-Erstellung fehlgeschlagen: ${e.message}")
        }
    }
    
    /**
     * Startet den Empfang von Snapshots
     */
    private fun startReceiving() {
        sendJob?.cancel()
        sendJob = scope.launch {
            nearbyService.receiveCharacterData(
                onDataReceived = { snapshot ->
                    scope.launch {
                        // Snapshot anwenden (nur wenn GUID übereinstimmt)
                        if (snapshot.character.guid == currentCharacterGuid) {
                            val result = repository.applySnapshotFromSync(snapshot, allowCreateNew = false)
                            if (result.isSuccess) {
                                lastSuccessfulReceiveTime = System.currentTimeMillis()
                            } else {
                                _syncStatus.value = SyncStatus.Error(
                                    "Snapshot-Import fehlgeschlagen: ${result.exceptionOrNull()?.message}"
                                )
                            }
                        }
                    }
                },
                onError = { error ->
                    _syncStatus.value = SyncStatus.Error("Empfang fehlgeschlagen: $error")
                }
            )
        }
    }
    
    /**
     * Startet den Watchdog, der prüft, ob seit > 15 Sekunden keine Übertragung erfolgt ist
     */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (true) {
                delay(WATCHDOG_INTERVAL_MS)
                
                val now = System.currentTimeMillis()
                val timeSinceLastSend = now - lastSuccessfulSendTime
                val timeSinceLastReceive = now - lastSuccessfulReceiveTime
                val timeSinceLastActivity = minOf(timeSinceLastSend, timeSinceLastReceive)
                
                if (timeSinceLastActivity > STALE_THRESHOLD_MS) {
                    val currentGuid = currentCharacterGuid
                    if (currentGuid != null && _syncStatus.value is SyncStatus.Syncing) {
                        _syncStatus.value = SyncStatus.Warning(
                            characterGuid = currentGuid,
                            message = "Keine erfolgreiche Übertragung seit ${timeSinceLastActivity / 1000} Sekunden",
                            staleSince = now - STALE_THRESHOLD_MS
                        )
                    }
                } else {
                    // Wenn wieder Aktivität, zurück zu Syncing
                    val currentGuid = currentCharacterGuid
                    val endpointId = currentEndpointId
                    val endpointName = currentEndpointName
                    if (currentGuid != null && endpointId != null && endpointName != null 
                        && _syncStatus.value is SyncStatus.Warning) {
                        _syncStatus.value = SyncStatus.Syncing(currentGuid, endpointId, endpointName)
                    }
                }
            }
        }
    }
}
