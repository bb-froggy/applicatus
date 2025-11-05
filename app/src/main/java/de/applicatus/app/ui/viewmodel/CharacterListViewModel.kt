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
import de.applicatus.app.data.model.Character
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CharacterListViewModel(
    private val repository: ApplicatusRepository
) : ViewModel() {
    
    private val exportManager = CharacterExportManager(repository)
    
    // Import State
    var importState by mutableStateOf<ImportState>(ImportState.Idle)
        private set
    
    sealed class ImportState {
        object Idle : ImportState()
        object Importing : ImportState()
        data class Success(val message: String, val characterId: Long) : ImportState()
        data class Error(val message: String) : ImportState()
    }
    
    val characters: StateFlow<List<Character>> = repository.allCharacters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun addCharacter(
        name: String,
        mu: Int = 8,
        kl: Int = 8,
        inValue: Int = 8,
        ch: Int = 8,
        ff: Int = 8,
        ge: Int = 8,
        ko: Int = 8,
        kk: Int = 8,
        hasApplicatus: Boolean = false,
        applicatusZfw: Int = 0,
        applicatusModifier: Int = 0
    ) {
        viewModelScope.launch {
            val character = Character(
                name = name,
                mu = mu,
                kl = kl,
                inValue = inValue,
                ch = ch,
                ff = ff,
                ge = ge,
                ko = ko,
                kk = kk,
                hasApplicatus = hasApplicatus,
                applicatusZfw = applicatusZfw,
                applicatusModifier = applicatusModifier
            )
            val characterId = repository.insertCharacter(character)
            // Keine automatische Initialisierung von Slots mehr
            // Benutzer fügt Slots selbst im Bearbeitungsmodus hinzu
        }
    }
    
    fun deleteCharacter(character: Character) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
        }
    }
    
    /**
     * Importiert einen Charakter aus einer JSON-Datei und legt einen neuen Charakter an.
     */
    fun importCharacterFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            importState = ImportState.Importing
            val result = exportManager.loadCharacterFromFile(context, uri, targetCharacterId = null)
            importState = if (result.isSuccess) {
                val (characterId, warning) = result.getOrNull()!!
                val message = if (warning != null) {
                    "Import erfolgreich mit Warnung:\n$warning"
                } else {
                    "Charakter erfolgreich importiert"
                }
                ImportState.Success(message, characterId)
            } else {
                ImportState.Error("Import fehlgeschlagen: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    /**
     * Importiert einen Charakter von Nearby und legt einen neuen Charakter an.
     */
    suspend fun importCharacterFromNearby(dto: CharacterExportDto): Result<Pair<Long, String?>> {
        return try {
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }
            val jsonString = json.encodeToString(kotlinx.serialization.serializer(), dto)
            exportManager.importCharacter(jsonString, targetCharacterId = null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun resetImportState() {
        importState = ImportState.Idle
    }
}

class CharacterListViewModelFactory(
    private val repository: ApplicatusRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CharacterListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
