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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CharacterHomeViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    private val exportManager = CharacterExportManager(repository)
    
    // Export/Import State
    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set
    
    sealed class ExportState {
        object Idle : ExportState()
        object Exporting : ExportState()
        data class Success(val message: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }
    
    private val _character = MutableStateFlow<Character?>(null)
    val character: StateFlow<Character?> = _character.asStateFlow()
    
    private val _lastRegenerationResult = MutableStateFlow<RegenerationResult?>(null)
    val lastRegenerationResult: StateFlow<RegenerationResult?> = _lastRegenerationResult.asStateFlow()
    
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
        }
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
     */
    fun importCharacterFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            exportState = ExportState.Exporting
            val result = exportManager.loadCharacterFromFile(context, uri, characterId)
            exportState = if (result.isSuccess) {
                val (_, warning) = result.getOrNull()!!
                val message = if (warning != null) {
                    "Import erfolgreich mit Warnung:\n$warning"
                } else {
                    "Charakter erfolgreich überschrieben"
                }
                ExportState.Success(message)
            } else {
                ExportState.Error("Import fehlgeschlagen: ${result.exceptionOrNull()?.message}")
            }
        }
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
}

class CharacterHomeViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterHomeViewModel::class.java)) {
            return CharacterHomeViewModel(repository, characterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
