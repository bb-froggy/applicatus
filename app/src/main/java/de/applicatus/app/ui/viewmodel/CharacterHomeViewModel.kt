package de.applicatus.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.CharacterExportManager
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.RegenerationCalculator
import de.applicatus.app.logic.RegenerationResult
import de.applicatus.app.logic.ProbeChecker
import de.applicatus.app.data.sync.CharacterRealtimeSyncManager
import de.applicatus.app.data.nearby.NearbyConnectionsInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CharacterHomeViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long,
    private val nearbyService: NearbyConnectionsInterface
) : ViewModel() {
    
    private val exportManager = CharacterExportManager(repository)
    
    // Real-Time Sync Manager
    private val realtimeSyncManager = CharacterRealtimeSyncManager(
        repository = repository,
        nearbyService = nearbyService,
        exportManager = exportManager,
        scope = viewModelScope
    )
    
    // Expose Sync Status
    val syncStatus: StateFlow<CharacterRealtimeSyncManager.SyncStatus> = realtimeSyncManager.syncStatus
    
    // Discovered Endpoints (for client mode)
    private val _discoveredEndpoints = MutableStateFlow<Map<String, String>>(emptyMap())
    val discoveredEndpoints: StateFlow<Map<String, String>> = _discoveredEndpoints.asStateFlow()
    
    // Export State
    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set
    
    sealed class ExportState {
        object Idle : ExportState()
        object Exporting : ExportState()
        data class Success(val message: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }
    
    // Import State
    var importState by mutableStateOf<ImportState>(ImportState.Idle)
        private set
    
    sealed class ImportState {
        object Idle : ImportState()
        object Importing : ImportState()
        data class ConfirmationRequired(
            val warning: String, 
            val context: Context, 
            val uri: Uri,
            val targetCharacterId: Long
        ) : ImportState()
        data class Success(val message: String) : ImportState()
        data class Error(val message: String) : ImportState()
    }
    
    private val _character = MutableStateFlow<Character?>(null)
    val character: StateFlow<Character?> = _character.asStateFlow()
    
    private val _lastRegenerationResult = MutableStateFlow<RegenerationResult?>(null)
    val lastRegenerationResult: StateFlow<RegenerationResult?> = _lastRegenerationResult.asStateFlow()
    
    // Energy Change Tracking (5-second sliding window)
    private val _energyChanges = MutableStateFlow<Map<String, Int>>(emptyMap())
    val energyChanges: StateFlow<Map<String, Int>> = _energyChanges.asStateFlow()
    
    private var changeWindowStartTime = 0L
    private val windowDurationMs = 5000L // 5 seconds
    
    init {
        loadCharacter()
    }
    
    private fun loadCharacter() {
        viewModelScope.launch {
            repository.getCharacterByIdFlow(characterId).collect { char ->
                _character.value = char
            }
        }
    }
    
    fun updateCharacter(character: Character) {
        viewModelScope.launch {
            repository.updateCharacter(character)
        }
    }
    
    fun updateCurrentLe(value: Int) {
        _character.value?.let { char ->
            val newValue = value.coerceIn(0, char.maxLe)
            updateCharacter(char.copy(currentLe = newValue))
        }
    }
    
    fun adjustCurrentLe(delta: Int) {
        _character.value?.let { char ->
            updateCurrentLe(char.currentLe + delta)
            trackEnergyChange("LE", delta)
        }
    }
    
    fun updateCurrentAe(value: Int) {
        _character.value?.let { char ->
            val newValue = value.coerceIn(0, char.maxAe)
            updateCharacter(char.copy(currentAe = newValue))
        }
    }
    
    fun adjustCurrentAe(delta: Int) {
        _character.value?.let { char ->
            updateCurrentAe(char.currentAe + delta)
            trackEnergyChange("AE", delta)
        }
    }
    
    fun updateCurrentKe(value: Int) {
        _character.value?.let { char ->
            val newValue = value.coerceIn(0, char.maxKe)
            updateCharacter(char.copy(currentKe = newValue))
        }
    }
    
    fun adjustCurrentKe(delta: Int) {
        _character.value?.let { char ->
            updateCurrentKe(char.currentKe + delta)
            trackEnergyChange("KE", delta)
        }
    }
    
    /**
     * Tracks cumulative energy changes within a 5-second sliding window.
     * The window extends with each new change (resets the 5-second timer).
     */
    private fun trackEnergyChange(energyType: String, delta: Int) {
        val currentTime = System.currentTimeMillis()
        
        // Reset window if expired (no activity for 5 seconds)
        if (currentTime - changeWindowStartTime > windowDurationMs) {
            _energyChanges.value = mapOf(energyType to delta)
        } else {
            // Accumulate within window
            _energyChanges.value = _energyChanges.value.toMutableMap().apply {
                this[energyType] = (this[energyType] ?: 0) + delta
            }
        }
        // Reset change window
        changeWindowStartTime = currentTime
    }
    
    fun clearEnergyChanges() {
        _energyChanges.value = emptyMap()
        changeWindowStartTime = 0L
    }
    
    fun performRegeneration(modifier: Int) {
        _character.value?.let { char ->
            val result = RegenerationCalculator.performRegeneration(char, modifier)
            _lastRegenerationResult.value = result
            
            // Energien aktualisieren
            val newChar = char.copy(
                currentLe = (char.currentLe + result.leGain).coerceAtMost(char.maxLe),
                currentAe = (char.currentAe + result.aeGain).coerceAtMost(char.maxAe),
                currentKe = (char.currentKe + result.keGain).coerceAtMost(char.maxKe)
            )
            updateCharacter(newChar)
        }
    }
    
    fun clearRegenerationResult() {
        _lastRegenerationResult.value = null
    }
    
    // Astrale Meditation State
    private val _lastAstralMeditationResult = MutableStateFlow<String?>(null)
    val lastAstralMeditationResult: StateFlow<String?> = _lastAstralMeditationResult.asStateFlow()
    
    fun performAstralMeditation(leToConvert: Int) {
        _character.value?.let { char ->
            // Prüfe Voraussetzungen
            if (!char.hasAe) {
                _lastAstralMeditationResult.value = "❌ Fehler: Charakter hat keine Astralenergie!"
                return
            }
            
            if (leToConvert <= 0) {
                _lastAstralMeditationResult.value = "❌ Fehler: Ungültige LE-Anzahl!"
                return
            }
            
            // Führe Probe durch
            val result = ProbeChecker.performAstralMeditation(char, leToConvert)
            val probeResult = result.first
            val additionalLeCost = result.second
            
            if (probeResult.success) {
                // Berechne Gesamt-LE-Kosten: leToConvert + 1W3-1
                val totalLeCost = leToConvert + additionalLeCost
                
                // Prüfe ob genug LE vorhanden
                if (char.currentLe < totalLeCost) {
                    _lastAstralMeditationResult.value = "❌ Fehler: Nicht genug LE! Benötigt: $totalLeCost (${leToConvert} + ${additionalLeCost}), Verfügbar: ${char.currentLe}"
                    return
                }
                
                // Prüfe ob genug AsP vorhanden (1 AsP Kosten)
                if (char.currentAe < 1) {
                    _lastAstralMeditationResult.value = "❌ Fehler: Nicht genug AsP! Benötigt: 1, Verfügbar: ${char.currentAe}"
                    return
                }
                
                // Prüfe ob AE-Maximum erreicht wäre
                if (char.currentAe + leToConvert - 1 > char.maxAe) {
                    val maxConvertible = char.maxAe - char.currentAe + 1
                    _lastAstralMeditationResult.value = "❌ Fehler: AE-Maximum würde überschritten! Maximum konvertierbar: $maxConvertible LE"
                    return
                }
                
                // Umwandlung durchführen
                val newChar = char.copy(
                    currentLe = char.currentLe - totalLeCost,
                    currentAe = (char.currentAe - 1 + leToConvert).coerceAtMost(char.maxAe)
                )
                updateCharacter(newChar)
                
                // Erfolgsmeldung
                _lastAstralMeditationResult.value = buildString {
                    append("✅ Astrale Meditation erfolgreich!\n\n")
                    append("Probe: IN/CH/KO\n")
                    append("Würfe: ${probeResult.rolls.joinToString(", ")}\n")
                    append("${probeResult.message}\n\n")
                    append("Umwandlung: $leToConvert LE → $leToConvert AE\n")
                    append("Kosten: 1 AsP + $totalLeCost LE (${leToConvert} + ${additionalLeCost})\n\n")
                    append("Neue Werte:\n")
                    append("LE: ${char.currentLe} → ${newChar.currentLe}\n")
                    append("AE: ${char.currentAe} → ${newChar.currentAe}")
                }
            } else {
                // Fehlschlag
                _lastAstralMeditationResult.value = buildString {
                    append("❌ Astrale Meditation fehlgeschlagen!\n\n")
                    append("Probe: IN/CH/KO\n")
                    append("Würfe: ${probeResult.rolls.joinToString(", ")}\n")
                    append(probeResult.message)
                }
            }
        }
    }
    
    fun clearAstralMeditationResult() {
        _lastAstralMeditationResult.value = null
    }
    
    // Export/Import Funktionen
    fun exportCharacterToFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            exportState = ExportState.Exporting
            val result = exportManager.saveCharacterToFile(context, characterId, uri)
            exportState = if (result.isSuccess) {
                ExportState.Success("Charakter erfolgreich exportiert")
            } else {
                ExportState.Error("Export fehlgeschlagen: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    /**
     * Importiert einen Charakter aus einer Datei und überschreibt den aktuellen Charakter.
     * GUID-Validierung stellt sicher, dass nur der richtige Charakter überschrieben wird.
     * Prüft zunächst auf Warnungen und zeigt ggf. einen Bestätigungsdialog an.
     */
    fun importCharacterFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            importState = ImportState.Importing
            
            // Zuerst JSON-String laden
            val jsonString = try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: run {
                    importState = ImportState.Error("Konnte Datei nicht lesen")
                    return@launch
                }
            } catch (e: Exception) {
                importState = ImportState.Error("Fehler beim Lesen der Datei: ${e.message}")
                return@launch
            }
            
            // Prüfe auf Warnungen
            val warningResult = exportManager.checkImportWarnings(jsonString, targetCharacterId = characterId)
            if (warningResult.isFailure) {
                importState = ImportState.Error("Import fehlgeschlagen: ${warningResult.exceptionOrNull()?.message}")
                return@launch
            }
            
            val (guid, warning) = warningResult.getOrNull()!!
            
            // Falls Warnung vorhanden, Bestätigung anfordern
            if (warning != null) {
                importState = ImportState.ConfirmationRequired(warning, context, uri, characterId)
                return@launch
            }
            
            // Keine Warnung -> Direkt importieren
            performImport(jsonString, characterId)
        }
    }
    
    /**
     * Führt den eigentlichen Import durch (nach Bestätigung oder wenn keine Warnung vorhanden).
     */
    fun confirmImport(context: Context, uri: Uri, targetCharacterId: Long) {
        viewModelScope.launch {
            importState = ImportState.Importing
            
            // JSON-String erneut laden
            val jsonString = try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: run {
                    importState = ImportState.Error("Konnte Datei nicht lesen")
                    return@launch
                }
            } catch (e: Exception) {
                importState = ImportState.Error("Fehler beim Lesen der Datei: ${e.message}")
                return@launch
            }
            
            performImport(jsonString, targetCharacterId)
        }
    }
    
    /**
     * Interner Helper zum Ausführen des Imports.
     */
    private suspend fun performImport(jsonString: String, targetCharacterId: Long) {
        val result = exportManager.importCharacter(jsonString, targetCharacterId)
        importState = if (result.isSuccess) {
            val (_, warning) = result.getOrNull()!!
            val message = if (warning != null) {
                "Import erfolgreich mit Warnung:\n$warning"
            } else {
                "Charakter erfolgreich überschrieben"
            }
            ImportState.Success(message)
        } else {
            ImportState.Error("Import fehlgeschlagen: ${result.exceptionOrNull()?.message}")
        }
    }
    
    fun resetImportState() {
        importState = ImportState.Idle
    }
    
    suspend fun exportCharacterForNearby(): Result<CharacterExportDto> {
        return try {
            val jsonResult = exportManager.exportCharacter(characterId)
            if (jsonResult.isSuccess) {
                val json = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                }
                val dto = json.decodeFromString<CharacterExportDto>(jsonResult.getOrNull()!!)
                Result.success(dto)
            } else {
                Result.failure(jsonResult.exceptionOrNull() ?: Exception("Export failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun resetExportState() {
        exportState = ExportState.Idle
    }
    
    // ===== Real-Time Sync Methods =====
    
    /**
     * Starts a host session for real-time character synchronization.
     * The character will be advertised and synced bidirectionally with connected clients.
     */
    fun startHostSession(deviceName: String) {
        viewModelScope.launch {
            realtimeSyncManager.startHostSession(characterId, deviceName)
        }
    }
    
    /**
     * Starts discovery to find available hosts.
     * Discovered devices will be added to discoveredEndpoints.
     */
    fun startDiscovery() {
        viewModelScope.launch {
            nearbyService.startDiscovery { endpointId, endpointName ->
                _discoveredEndpoints.value = _discoveredEndpoints.value + (endpointId to endpointName)
            }.collect { connectionState ->
                when (connectionState) {
                    is NearbyConnectionsInterface.ConnectionState.Error -> {
                        // Handle discovery errors if needed
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Stops discovery and clears discovered endpoints.
     */
    fun stopDiscovery() {
        nearbyService.stopAllConnections()
        _discoveredEndpoints.value = emptyMap()
    }
    
    /**
     * Starts a client session to connect to a host.
     * Connects to the specified endpoint.
     */
    fun startClientSession(endpointId: String, endpointName: String) {
        viewModelScope.launch {
            // Stop discovery first
            stopDiscovery()
            
            // Start client session with the selected host
            realtimeSyncManager.startClientSession(characterId, endpointId, endpointName)
        }
    }
    
    /**
     * Stops the current sync session (host or client).
     */
    fun stopSyncSession() {
        viewModelScope.launch {
            realtimeSyncManager.stopSession()
            _discoveredEndpoints.value = emptyMap()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up sync session when ViewModel is destroyed
        viewModelScope.launch {
            realtimeSyncManager.stopSession()
        }
    }
}

class CharacterHomeViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long,
    private val nearbyService: NearbyConnectionsInterface
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterHomeViewModel::class.java)) {
            return CharacterHomeViewModel(repository, characterId, nearbyService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
