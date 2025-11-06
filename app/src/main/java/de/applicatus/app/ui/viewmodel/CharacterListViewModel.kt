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
import de.applicatus.app.data.model.GlobalSettings
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.DerianDateCalculator
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
    
    // Spell Sync State
    var spellSyncState by mutableStateOf<SpellSyncState>(SpellSyncState.Idle)
        private set
    
    // Edit Mode für derisches Datum
    var isDateEditMode by mutableStateOf(false)
        private set
    
    sealed class SpellSyncState {
        object Idle : SpellSyncState()
        object Syncing : SpellSyncState()
        data class Success(val count: Int) : SpellSyncState()
        data class Error(val message: String) : SpellSyncState()
    }
    
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
    
    val globalSettings: StateFlow<GlobalSettings?> = repository.globalSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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
    
    /**
     * Synchronisiert fehlende Zauber aus InitialSpells in die Datenbank.
     */
    fun syncMissingSpells() {
        viewModelScope.launch {
            spellSyncState = SpellSyncState.Syncing
            try {
                val addedCount = repository.syncMissingSpells()
                spellSyncState = SpellSyncState.Success(addedCount)
            } catch (e: Exception) {
                spellSyncState = SpellSyncState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
    
    fun resetSpellSyncState() {
        spellSyncState = SpellSyncState.Idle
    }
    
    // Derisches Datum verwalten
    fun toggleDateEditMode() {
        isDateEditMode = !isDateEditMode
    }
    
    fun updateDerianDate(newDate: String) {
        viewModelScope.launch {
            repository.updateCurrentDerianDate(newDate)
        }
    }
    
    fun incrementDerianDate() {
        viewModelScope.launch {
            val settings = globalSettings.value ?: return@launch
            val currentDate = settings.currentDerianDate
            val newDate = DerianDateCalculator.calculateExpiryDate(currentDate, "1 Tag")
            repository.updateCurrentDerianDate(newDate)
        }
    }
    
    fun decrementDerianDate() {
        viewModelScope.launch {
            val settings = globalSettings.value ?: return@launch
            val currentDate = settings.currentDerianDate
            
            // Parse aktuelles Datum
            val parts = currentDate.split(" ")
            if (parts.size < 3) return@launch
            
            var day = parts[0].toIntOrNull() ?: 1
            
            // Behandle "Namenlose Tage" als zwei Wörter
            val (monthName, year, era) = if (parts.size >= 5 && parts[1] == "Namenlose" && parts[2] == "Tage") {
                Triple("Namenlose Tage", parts[3].toIntOrNull() ?: 1040, parts.getOrNull(4) ?: "BF")
            } else {
                Triple(parts[1], parts[2].toIntOrNull() ?: 1040, parts.getOrNull(3) ?: "BF")
            }
            
            val months = DerianDateCalculator.getMonths()
            var monthIndex = months.indexOf(monthName)
            if (monthIndex == -1) return@launch
            
            var currentYear = year
            
            // Tag decrementieren
            day--
            if (day < 1) {
                // Zum vorherigen Monat wechseln
                monthIndex--
                if (monthIndex < 0) {
                    // Zum letzten Monat (Namenlose Tage) des vorherigen Jahres
                    monthIndex = 12 // Namenlose Tage
                    currentYear--
                }
                
                // Letzter Tag des neuen Monats
                day = if (monthIndex == 12) 5 else 30
            }
            
            val newMonthName = months[monthIndex]
            val newDate = "$day $newMonthName $currentYear $era"
            repository.updateCurrentDerianDate(newDate)
        }
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
