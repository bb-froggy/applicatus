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
import de.applicatus.app.data.model.character.JournalCategory
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.RegenerationCalculator
import de.applicatus.app.logic.RegenerationResult
import de.applicatus.app.logic.ProbeChecker
import de.applicatus.app.data.sync.CharacterRealtimeSyncManager
import de.applicatus.app.data.sync.SyncSessionManager
import de.applicatus.app.data.nearby.NearbyConnectionsInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


class CharacterHomeViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long,
    private val nearbyService: NearbyConnectionsInterface,
    private val syncSessionManager: SyncSessionManager
) : ViewModel() {
    
    private val exportManager = CharacterExportManager(repository)
    
    // Real-Time Sync Manager - aus SyncSessionManager holen, NICHT im ViewModel erstellen
    // Dadurch überlebt die Session die Navigation zwischen Screens
    private val realtimeSyncManager: CharacterRealtimeSyncManager
        get() = syncSessionManager.getOrCreateSyncManager(characterId)
    
    // Expose Sync Status - direkt aus dem SyncManager
    val syncStatus: StateFlow<CharacterRealtimeSyncManager.SyncStatus>
        get() = realtimeSyncManager.syncStatus
    
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
    private var journalDebounceJob: Job? = null
    private var initialEnergyValues = mutableMapOf<String, Int>() // Track initial values for journal
    
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
            val oldCharacter = _character.value
            repository.updateCharacter(character)
            
            // Journal-Einträge für Eigenschaften- und Talent-Änderungen
            if (oldCharacter != null) {
                val changes = mutableListOf<String>()
                
                // Eigenschaften
                if (oldCharacter.mu != character.mu) changes.add("MU: ${oldCharacter.mu} → ${character.mu}")
                if (oldCharacter.kl != character.kl) changes.add("KL: ${oldCharacter.kl} → ${character.kl}")
                if (oldCharacter.inValue != character.inValue) changes.add("IN: ${oldCharacter.inValue} → ${character.inValue}")
                if (oldCharacter.ch != character.ch) changes.add("CH: ${oldCharacter.ch} → ${character.ch}")
                if (oldCharacter.ff != character.ff) changes.add("FF: ${oldCharacter.ff} → ${character.ff}")
                if (oldCharacter.ge != character.ge) changes.add("GE: ${oldCharacter.ge} → ${character.ge}")
                if (oldCharacter.ko != character.ko) changes.add("KO: ${oldCharacter.ko} → ${character.ko}")
                if (oldCharacter.kk != character.kk) changes.add("KK: ${oldCharacter.kk} → ${character.kk}")
                
                // Max LE
                if (oldCharacter.maxLe != character.maxLe) {
                    changes.add("Max LE: ${oldCharacter.maxLe} → ${character.maxLe}")
                }
                
                // Talente
                if (oldCharacter.alchemySkill != character.alchemySkill) {
                    changes.add("Alchimie: ${oldCharacter.alchemySkill} → ${character.alchemySkill}")
                }
                if (oldCharacter.cookingPotionsSkill != character.cookingPotionsSkill) {
                    changes.add("Trankkochen: ${oldCharacter.cookingPotionsSkill} → ${character.cookingPotionsSkill}")
                }
                if (oldCharacter.selfControlSkill != character.selfControlSkill) {
                    changes.add("Selbstbeherrschung: ${oldCharacter.selfControlSkill} → ${character.selfControlSkill}")
                }
                if (oldCharacter.sensoryAcuitySkill != character.sensoryAcuitySkill) {
                    changes.add("Sinnenschärfe: ${oldCharacter.sensoryAcuitySkill} → ${character.sensoryAcuitySkill}")
                }
                if (oldCharacter.magicalLoreSkill != character.magicalLoreSkill) {
                    changes.add("Magiekunde: ${oldCharacter.magicalLoreSkill} → ${character.magicalLoreSkill}")
                }
                if (oldCharacter.herbalLoreSkill != character.herbalLoreSkill) {
                    changes.add("Pflanzenkunde: ${oldCharacter.herbalLoreSkill} → ${character.herbalLoreSkill}")
                }
                if (oldCharacter.ritualKnowledgeValue != character.ritualKnowledgeValue) {
                    changes.add("Ritualkenntnis: ${oldCharacter.ritualKnowledgeValue} → ${character.ritualKnowledgeValue}")
                }
                
                // Journal-Eintrag wenn es Änderungen gibt
                if (changes.isNotEmpty()) {
                    repository.logCharacterEvent(
                        characterId = character.id,
                        category = JournalCategory.CHARACTER_MODIFIED,
                        playerMessage = changes.joinToString(", "),
                        gmMessage = ""
                    )
                }
            }
        }
    }
    
    fun updateCurrentLe(value: Int) {
        _character.value?.let { char ->
            val oldValue = char.currentLe
            val newValue = value.coerceIn(0, char.maxLe)
            if (oldValue != newValue) {
                updateCharacter(char.copy(currentLe = newValue))
                trackManualEnergyChange("LE", oldValue, newValue)
            }
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
            val oldValue = char.currentAe
            val newValue = value.coerceIn(0, char.maxAe)
            if (oldValue != newValue) {
                updateCharacter(char.copy(currentAe = newValue))
                trackManualEnergyChange("AE", oldValue, newValue)
            }
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
            val oldValue = char.currentKe
            val newValue = value.coerceIn(0, char.maxKe)
            if (oldValue != newValue) {
                updateCharacter(char.copy(currentKe = newValue))
                trackManualEnergyChange("KE", oldValue, newValue)
            }
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
    
    /**
     * Tracks manual energy changes for journal entries with 5-second debouncing.
     * All changes within 5 seconds are combined into a single journal entry.
     */
    private fun trackManualEnergyChange(energyType: String, oldValue: Int, newValue: Int) {
        // Cancel existing debounce job
        journalDebounceJob?.cancel()
        
        // Store initial value if this is the first change in the window
        if (!initialEnergyValues.containsKey(energyType)) {
            initialEnergyValues[energyType] = oldValue
        }
        
        // Start new debounce job that writes journal entry after 5 seconds of inactivity
        journalDebounceJob = viewModelScope.launch {
            delay(windowDurationMs)
            
            // Write journal entry with accumulated changes
            val changes = mutableMapOf<String, Pair<Int, Int>>()
            _character.value?.let { char ->
                if (initialEnergyValues.containsKey("LE")) {
                    changes["LE"] = initialEnergyValues["LE"]!! to char.currentLe
                }
                if (initialEnergyValues.containsKey("AE")) {
                    changes["AE"] = initialEnergyValues["AE"]!! to char.currentAe
                }
                if (initialEnergyValues.containsKey("KE")) {
                    changes["KE"] = initialEnergyValues["KE"]!! to char.currentKe
                }
                
                if (changes.isNotEmpty()) {
                    val playerMessage = changes.entries.joinToString(", ") { (type, values) ->
                        val (old, new) = values
                        val delta = new - old
                        val sign = if (delta >= 0) "+" else ""
                        "$type: $old → $new ($sign$delta)"
                    }
                    
                    repository.logCharacterEvent(
                        characterId = characterId,
                        category = JournalCategory.MANUAL_POINTS_CHANGE,
                        playerMessage = "Manuelle Änderung: $playerMessage",
                        gmMessage = ""
                    )
                }
            }
            
            // Clear initial values
            initialEnergyValues.clear()
        }
    }
    
    fun clearEnergyChanges() {
        _energyChanges.value = emptyMap()
        changeWindowStartTime = 0L
        journalDebounceJob?.cancel()
        initialEnergyValues.clear()
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
            
            // Journal-Eintrag für Regeneration
            viewModelScope.launch {
                val parts = mutableListOf<String>()
                if (result.leGain > 0) parts.add("LE: ${char.currentLe} → ${newChar.currentLe} (+${result.leGain})")
                if (result.aeGain > 0) parts.add("AE: ${char.currentAe} → ${newChar.currentAe} (+${result.aeGain})")
                if (result.keGain > 0) parts.add("KE: ${char.currentKe} → ${newChar.currentKe} (+${result.keGain})")
                
                if (parts.isNotEmpty()) {
                    val details = mutableListOf<String>()
                    if (result.leDetails.isNotEmpty()) details.add("LE: ${result.leDetails}")
                    if (result.aeDetails.isNotEmpty()) details.add("AE: ${result.aeDetails}")
                    if (result.keDetails.isNotEmpty()) details.add("KE: ${result.keDetails}")
                    
                    repository.logCharacterEvent(
                        characterId = char.id,
                        category = JournalCategory.ENERGY_REGENERATION,
                        playerMessage = "Regeneriert: ${parts.joinToString(", ")}",
                        gmMessage = details.joinToString(", ")
                    )
                }
            }
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
                
                // Journal-Eintrag
                viewModelScope.launch {
                    repository.logCharacterEvent(
                        characterId = char.id,
                        category = JournalCategory.ASTRAL_MEDITATION,
                        playerMessage = "Astrale Meditation: ${leToConvert} LE → ${leToConvert} AE (LE: ${char.currentLe} → ${newChar.currentLe}, AE: ${char.currentAe} → ${newChar.currentAe})",
                        gmMessage = "Probe: ${probeResult.rolls.joinToString(", ")}, TaP*: ${probeResult.qualityPoints}, Zusatz-LE-Kosten: ${additionalLeCost}"
                    )
                }
                
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
                // Fehlschlag - auch ins Journal
                viewModelScope.launch {
                    repository.logCharacterEvent(
                        characterId = char.id,
                        category = JournalCategory.ASTRAL_MEDITATION,
                        playerMessage = "Astrale Meditation fehlgeschlagen",
                        gmMessage = "Probe: ${probeResult.rolls.joinToString(", ")}, TaP*: ${probeResult.qualityPoints}"
                    )
                }
                
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
        nearbyService.stopDiscovery()
        _discoveredEndpoints.value = emptyMap()
    }
    
    /**
     * Starts a client session to connect to a host.
     * Connects to the specified endpoint.
     * 
     * Important: Discovery must be stopped before connecting to avoid 
     * STATUS_OUT_OF_ORDER_API_CALL (8009) from Nearby Connections API.
     */
    fun startClientSession(endpointId: String, endpointName: String) {
        viewModelScope.launch {
            // Stop discovery first - required by Nearby Connections API
            nearbyService.stopDiscovery()
            
            // Small delay to ensure the API has processed the stop call
            // This is necessary because stopDiscovery() is asynchronous internally
            delay(100)
            
            // Clear discovered endpoints
            _discoveredEndpoints.value = emptyMap()
            
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
    
    // KEIN onCleared() - wir wollen die Sync-Session NICHT beenden wenn das ViewModel
    // zerstört wird. Die Session wird vom SyncSessionManager verwaltet und überlebt
    // die Navigation zwischen Screens.
}

class CharacterHomeViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long,
    private val nearbyService: NearbyConnectionsInterface,
    private val syncSessionManager: SyncSessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterHomeViewModel::class.java)) {
            return CharacterHomeViewModel(repository, characterId, nearbyService, syncSessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
