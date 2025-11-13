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
import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.DerianDateCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CharacterListViewModel(
    private val repository: ApplicatusRepository
) : ViewModel() {
    
    private val exportManager = CharacterExportManager(repository)
    
    // Import State
    var importState by mutableStateOf<ImportState>(ImportState.Idle)
        private set
    
    // Export State
    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set
    
    // Spell Sync State
    var spellSyncState by mutableStateOf<SpellSyncState>(SpellSyncState.Idle)
        private set
    
    // Recipe Sync State
    var recipeSyncState by mutableStateOf<RecipeSyncState>(RecipeSyncState.Idle)
        private set
    
    // Edit Mode für derisches Datum
    var isDateEditMode by mutableStateOf(false)
        private set
    
    // Ausgewählte Gruppe für Datumsanzeige
    var selectedGroupId by mutableStateOf<Long?>(null)
        private set
    
    sealed class SpellSyncState {
        object Idle : SpellSyncState()
        object Syncing : SpellSyncState()
        data class Success(val count: Int) : SpellSyncState()
        data class Error(val message: String) : SpellSyncState()
    }
    
    sealed class RecipeSyncState {
        object Idle : RecipeSyncState()
        object Syncing : RecipeSyncState()
        data class Success(val count: Int) : RecipeSyncState()
        data class Error(val message: String) : RecipeSyncState()
    }
    
    sealed class ImportState {
        object Idle : ImportState()
        object Importing : ImportState()
        data class Success(val message: String, val characterId: Long) : ImportState()
        data class Error(val message: String) : ImportState()
    }
    
    sealed class ExportState {
        object Idle : ExportState()
        object Exporting : ExportState()
        data class Success(val message: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }
    
    val characters: StateFlow<List<Character>> = repository.allCharacters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val groups: StateFlow<List<Group>> = repository.allGroups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Aktuelle Gruppe basierend auf selectedGroupId
    val currentGroup: StateFlow<Group?> = groups.map { groupList ->
        if (selectedGroupId != null) {
            groupList.find { it.id == selectedGroupId }
        } else {
            groupList.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    init {
        // Setze Standard-Gruppe als ausgewählt und korrigiere ungültige Daten
        viewModelScope.launch {
            groups.collect { groupList ->
                if (selectedGroupId == null && groupList.isNotEmpty()) {
                    selectedGroupId = groupList.first().id
                }
                
                // Korrigiere ungültige Daten (z.B. negative Jahre oder Jahr 0)
                groupList.forEach { group ->
                    val date = group.currentDerianDate
                    val parts = date.split(" ")
                    
                    if (parts.size >= 3) {
                        val yearPart = if (parts.size >= 5 && parts[1] == "Namenlose" && parts[2] == "Tage") {
                            parts[3]
                        } else {
                            parts[2]
                        }
                        
                        val year = yearPart.toIntOrNull()
                        if (year != null && year < 1) {
                            // Korrigiere auf Standarddatum
                            repository.updateGroupDerianDate(group.id, "1 Praios 1 BF")
                        }
                    }
                }
            }
        }
    }
    
    fun selectGroup(groupId: Long) {
        selectedGroupId = groupId
    }
    
    fun addCharacter(
        name: String,
        groupId: Long? = null,
        group: String = "Meine Gruppe",
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
            // Falls keine groupId übergeben wurde, verwende selectedGroupId oder erstelle neue Gruppe
            val finalGroupId = groupId ?: selectedGroupId ?: run {
                // Erstelle neue Gruppe mit dem Namen aus dem group-Parameter
                repository.insertGroup(Group(name = group))
            }
            
            val character = Character(
                name = name,
                groupId = finalGroupId,
                group = group,  // Deprecated, aber behalten für Kompatibilität
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
            
            // Erstelle Standard-Locations (Rüstung/Kleidung, Rucksack)
            repository.createDefaultLocationsForCharacter(characterId)
        }
    }
    
    fun deleteCharacter(character: Character) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
        }
    }
    
    fun addGroup(name: String) {
        viewModelScope.launch {
            val groupId = repository.insertGroup(Group(name = name))
            selectedGroupId = groupId
        }
    }
    
    fun updateGroup(group: Group) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }
    
    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            repository.deleteGroup(groupId)
            // Wähle erste verfügbare Gruppe nach Löschung
            groups.value.firstOrNull { it.id != groupId }?.let {
                selectedGroupId = it.id
            }
        }
    }
    
    fun moveCharacterToGroup(characterId: Long, targetGroupId: Long) {
        viewModelScope.launch {
            repository.moveCharacterToGroup(characterId, targetGroupId)
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
     * Exportiert einen Charakter in eine JSON-Datei.
     */
    fun exportCharacterToFile(context: Context, characterId: Long, uri: Uri) {
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
    
    fun resetExportState() {
        exportState = ExportState.Idle
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
    
    /**
     * Synchronisiert fehlende Rezepte aus InitialRecipes in die Datenbank.
     */
    fun syncMissingRecipes() {
        viewModelScope.launch {
            recipeSyncState = RecipeSyncState.Syncing
            try {
                val addedCount = repository.syncMissingRecipes()
                recipeSyncState = RecipeSyncState.Success(addedCount)
            } catch (e: Exception) {
                recipeSyncState = RecipeSyncState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
    
    fun resetRecipeSyncState() {
        recipeSyncState = RecipeSyncState.Idle
    }
    
    // Derisches Datum verwalten (pro Gruppe)
    fun toggleDateEditMode() {
        isDateEditMode = !isDateEditMode
    }
    
    fun updateDerianDate(newDate: String) {
        viewModelScope.launch {
            val groupId = selectedGroupId ?: return@launch
            repository.updateGroupDerianDate(groupId, newDate)
        }
    }
    
    fun incrementDerianDate() {
        viewModelScope.launch {
            val group = currentGroup.value ?: return@launch
            val currentDate = group.currentDerianDate
            val newDate = DerianDateCalculator.calculateExpiryDate(currentDate, "1 Tag")
            repository.updateGroupDerianDate(group.id, newDate)
        }
    }
    
    fun decrementDerianDate() {
        viewModelScope.launch {
            val group = currentGroup.value ?: return@launch
            val currentDate = group.currentDerianDate
            
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
                    
                    // Verhindere negative Jahre (minimum 1 BF)
                    if (currentYear < 1) {
                        currentYear = 1
                        monthIndex = 0 // Praios
                        day = 1
                    }
                }
                
                // Letzter Tag des neuen Monats (nur wenn Jahr nicht auf Minimum gesetzt wurde)
                if (currentYear > 1 || monthIndex > 0 || day > 1) {
                    day = if (monthIndex == 12) 5 else 30
                }
            }
            
            val newMonthName = months[monthIndex]
            val newDate = "$day $newMonthName $currentYear $era"
            repository.updateGroupDerianDate(group.id, newDate)
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
