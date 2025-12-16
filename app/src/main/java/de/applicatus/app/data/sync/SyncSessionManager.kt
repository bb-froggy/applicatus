package de.applicatus.app.data.sync

import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.CharacterExportManager
import de.applicatus.app.data.nearby.NearbyConnectionsInterface
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton-Manager für Echtzeit-Sync-Sessions.
 * 
 * Ermöglicht es dem Spielleiter, mehrere Charaktere gleichzeitig im Sync zu haben,
 * auch wenn er zwischen den Charakteren navigiert. Die Sessions überleben die
 * Navigation zwischen Screens.
 * 
 * Jeder Charakter kann maximal eine aktive Session haben.
 * 
 * MULTI-CHARACTER-SYNC:
 * Wenn über eine physische Nearby-Verbindung mehrere Charaktere synchronisiert werden,
 * werden eingehende Snapshots automatisch an den passenden CharacterRealtimeSyncManager
 * weitergeleitet (basierend auf der Character-GUID).
 */
class SyncSessionManager private constructor() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Alle aktiven Sync-Sessions (characterId -> SyncManager)
    private val activeSessions = mutableMapOf<Long, CharacterRealtimeSyncManager>()
    
    // Mapping von Character-GUID zu Character-ID für schnelles Lookup
    private val guidToCharacterId = mutableMapOf<String, Long>()
    
    // Combined Status aller Sessions für UI-Anzeige
    private val _activeSyncStatuses = MutableStateFlow<Map<Long, CharacterRealtimeSyncManager.SyncStatus>>(emptyMap())
    val activeSyncStatuses: StateFlow<Map<Long, CharacterRealtimeSyncManager.SyncStatus>> = _activeSyncStatuses.asStateFlow()
    
    // Abhängigkeiten (müssen vor Verwendung gesetzt werden)
    private var repository: ApplicatusRepository? = null
    private var nearbyService: NearbyConnectionsInterface? = null
    
    // Zentraler Receiver für eingehende Daten (wird von allen Managern geteilt)
    private var centralReceiverJob: Job? = null
    
    /**
     * Initialisiert den Manager mit den benötigten Abhängigkeiten.
     * Muss aufgerufen werden bevor Sessions gestartet werden können.
     */
    fun initialize(repository: ApplicatusRepository, nearbyService: NearbyConnectionsInterface) {
        this.repository = repository
        this.nearbyService = nearbyService
    }
    
    /**
     * Gibt den SyncManager für einen bestimmten Charakter zurück.
     * Erstellt einen neuen Manager wenn noch keiner existiert.
     */
    fun getOrCreateSyncManager(characterId: Long): CharacterRealtimeSyncManager {
        val repo = repository ?: throw IllegalStateException("SyncSessionManager nicht initialisiert. Rufe initialize() auf.")
        val nearby = nearbyService ?: throw IllegalStateException("SyncSessionManager nicht initialisiert. Rufe initialize() auf.")
        
        return activeSessions.getOrPut(characterId) {
            val exportManager = CharacterExportManager(repo)
            CharacterRealtimeSyncManager(
                repository = repo,
                nearbyService = nearby,
                exportManager = exportManager,
                scope = scope
            ).also { manager ->
                // Status-Änderungen beobachten und in die Combined-Map eintragen
                scope.launch {
                    manager.syncStatus.collect { status ->
                        updateSessionStatus(characterId, status)
                        
                        // GUID-Mapping aktualisieren wenn Syncing
                        if (status is CharacterRealtimeSyncManager.SyncStatus.Syncing) {
                            guidToCharacterId[status.characterGuid] = characterId
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Registriert eine Character-GUID für das Routing von eingehenden Snapshots.
     * Wird vom CharacterRealtimeSyncManager aufgerufen wenn eine Session gestartet wird.
     */
    fun registerCharacterGuid(characterId: Long, guid: String) {
        guidToCharacterId[guid] = characterId
    }
    
    /**
     * Entfernt die Registrierung einer Character-GUID.
     */
    fun unregisterCharacterGuid(guid: String) {
        guidToCharacterId.remove(guid)
    }
    
    /**
     * Verarbeitet einen eingehenden Snapshot und leitet ihn an den passenden Manager weiter.
     * Wenn kein passender Manager gefunden wird (d.h. der Charakter wird auf diesem Gerät
     * nicht synchronisiert), wird der Snapshot ignoriert.
     * 
     * @return true wenn der Snapshot verarbeitet wurde, false wenn kein passender Manager gefunden wurde
     */
    suspend fun routeIncomingSnapshot(snapshot: CharacterExportDto): Boolean {
        val characterGuid = snapshot.character.guid
        val characterId = guidToCharacterId[characterGuid] ?: return false
        val manager = activeSessions[characterId] ?: return false
        
        // Der Manager verarbeitet den Snapshot
        manager.handleIncomingSnapshot(snapshot)
        return true
    }
    
    /**
     * Gibt den SyncManager für einen Charakter zurück, falls vorhanden.
     */
    fun getSyncManager(characterId: Long): CharacterRealtimeSyncManager? {
        return activeSessions[characterId]
    }
    
    /**
     * Entfernt eine Session und stoppt den Sync.
     */
    fun removeSession(characterId: Long) {
        val manager = activeSessions.remove(characterId)
        if (manager != null) {
            // GUID-Mapping entfernen
            val status = manager.syncStatus.value
            if (status is CharacterRealtimeSyncManager.SyncStatus.Syncing) {
                guidToCharacterId.remove(status.characterGuid)
            }
            manager.stopSession()
        }
        updateSessionStatus(characterId, CharacterRealtimeSyncManager.SyncStatus.Idle)
    }
    
    /**
     * Stoppt alle aktiven Sessions.
     */
    fun stopAllSessions() {
        activeSessions.values.forEach { it.stopSession() }
        activeSessions.clear()
        guidToCharacterId.clear()
        _activeSyncStatuses.value = emptyMap()
    }
    
    /**
     * Prüft ob ein Charakter eine aktive Sync-Session hat.
     */
    fun hasActiveSession(characterId: Long): Boolean {
        val status = activeSessions[characterId]?.syncStatus?.value
        return status != null && status !is CharacterRealtimeSyncManager.SyncStatus.Idle
    }
    
    /**
     * Prüft ob überhaupt eine Sync-Session aktiv ist.
     */
    fun hasAnySyncSession(): Boolean {
        return activeSessions.values.any { manager ->
            val status = manager.syncStatus.value
            status !is CharacterRealtimeSyncManager.SyncStatus.Idle
        }
    }
    
    /**
     * Gibt die Anzahl aktiver Sessions zurück.
     */
    fun getActiveSyncCount(): Int {
        return activeSessions.values.count { manager ->
            val status = manager.syncStatus.value
            status is CharacterRealtimeSyncManager.SyncStatus.Syncing ||
            status is CharacterRealtimeSyncManager.SyncStatus.Warning ||
            status is CharacterRealtimeSyncManager.SyncStatus.Connecting
        }
    }
    
    /**
     * Gibt alle aktiven Character-GUIDs zurück.
     */
    fun getActiveCharacterGuids(): Set<String> {
        return guidToCharacterId.keys.toSet()
    }
    
    private fun updateSessionStatus(characterId: Long, status: CharacterRealtimeSyncManager.SyncStatus) {
        val currentStatuses = _activeSyncStatuses.value.toMutableMap()
        if (status is CharacterRealtimeSyncManager.SyncStatus.Idle) {
            currentStatuses.remove(characterId)
        } else {
            currentStatuses[characterId] = status
        }
        _activeSyncStatuses.value = currentStatuses
    }
    
    companion object {
        @Volatile
        private var instance: SyncSessionManager? = null
        
        fun getInstance(): SyncSessionManager {
            return instance ?: synchronized(this) {
                instance ?: SyncSessionManager().also { instance = it }
            }
        }
    }
}
