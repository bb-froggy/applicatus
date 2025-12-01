package de.applicatus.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.export.BackupValidation
import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.CharacterExportManager
import de.applicatus.app.data.export.DatabaseBackupManager
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
    private val backupManager = DatabaseBackupManager(repository)
    
    // Import State
    var importState by mutableStateOf<ImportState>(ImportState.Idle)
        private set
    
    // Export State
    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set
    
    // Backup Import State
    var backupImportState by mutableStateOf<BackupImportState>(BackupImportState.Idle)
        private set
    
    // Backup Export State
    var backupExportState by mutableStateOf<BackupExportState>(BackupExportState.Idle)
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
    
    // Expiry warning state
    var expiryWarningMessage by mutableStateOf<String?>(null)
        private set
    
    fun dismissExpiryWarning() {
        expiryWarningMessage = null
    }
    
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
    
    sealed class BackupImportState {
        object Idle : BackupImportState()
        data class Validating(val context: Context, val uri: Uri) : BackupImportState()
        data class ConfirmationRequired(
            val validation: BackupValidation,
            val context: Context,
            val uri: Uri
        ) : BackupImportState()
        data class Importing(val progress: String, val percentage: Int) : BackupImportState()
        data class Success(val summary: String) : BackupImportState()
        data class Error(val message: String) : BackupImportState()
    }
    
    sealed class BackupExportState {
        object Idle : BackupExportState()
        data class Exporting(val progress: String, val percentage: Int) : BackupExportState()
        data class Success(val message: String) : BackupExportState()
        data class Error(val message: String) : BackupExportState()
    }
    
    sealed class ImportState {
        object Idle : ImportState()
        object Importing : ImportState()
        data class ConfirmationRequired(
            val warning: String, 
            val context: Context, 
            val uri: Uri,
            val targetCharacterId: Long? = null
        ) : ImportState()
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
            // Falls keine groupId übergeben wurde, verwende selectedGroupId
            val finalGroupId = groupId ?: selectedGroupId
            
            val character = Character(
                name = name,
                groupId = finalGroupId,
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
    
    fun updateGroupGameMasterMode(groupId: Long, isGameMaster: Boolean) {
        viewModelScope.launch {
            val group = groups.value.find { it.id == groupId } ?: return@launch
            repository.updateGroup(group.copy(isGameMasterGroup = isGameMaster))
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
            val warningResult = exportManager.checkImportWarnings(jsonString, targetCharacterId = null)
            if (warningResult.isFailure) {
                importState = ImportState.Error("Import fehlgeschlagen: ${warningResult.exceptionOrNull()?.message}")
                return@launch
            }
            
            val (guid, warning) = warningResult.getOrNull()!!
            
            // Falls Warnung vorhanden, Bestätigung anfordern
            if (warning != null) {
                importState = ImportState.ConfirmationRequired(warning, context, uri, targetCharacterId = null)
                return@launch
            }
            
            // Keine Warnung -> Direkt importieren
            performImport(jsonString, targetCharacterId = null)
        }
    }
    
    /**
     * Führt den eigentlichen Import durch (nach Bestätigung oder wenn keine Warnung vorhanden).
     */
    fun confirmImport(context: Context, uri: Uri, targetCharacterId: Long? = null) {
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
    private suspend fun performImport(jsonString: String, targetCharacterId: Long?) {
        val result = exportManager.importCharacter(jsonString, targetCharacterId)
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
    
    /**
     * Erstellt ein Share-Intent für einen Charakter.
     */
    suspend fun shareCharacter(context: Context, characterId: Long): Result<android.content.Intent> {
        return exportManager.createShareIntent(context, characterId)
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
    
    fun incrementDerianDate() {
        viewModelScope.launch {
            val group = currentGroup.value ?: return@launch
            val currentDate = group.currentDerianDate
            val newDate = DerianDateCalculator.calculateExpiryDate(currentDate, "1 Tag")
            
            // Prüfe auf ablaufende Zauber
            checkExpiringSpells(group.id, newDate)
            
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
    
    fun updateDerianDate(newDate: String) {
        viewModelScope.launch {
            val group = currentGroup.value ?: return@launch
            
            // Prüfe auf ablaufende Zauber
            checkExpiringSpells(group.id, newDate)
            
            repository.updateGroupDerianDate(group.id, newDate)
            isDateEditMode = false
        }
    }
    
    /**
     * Prüft, ob beim Aktualisieren des Datums Zauber oder Zauberzeichen ablaufen
     */
    private suspend fun checkExpiringSpells(groupId: Long, newDate: String) {
        // Hole alle Charaktere der Gruppe
        val characters = repository.getCharactersByGroupOnce(groupId)
        
        val expiredSpells = mutableListOf<Pair<String, String>>() // (Character name, Spell name)
        val expiredMagicSigns = mutableListOf<Pair<String, String>>() // (Character name, Sign name)
        
        for (character in characters) {
            // Hole alle gefüllten Slots des Charakters
            val slots = repository.getSlotsWithSpellsByCharacterOnce(character.id)
            
            for (slotWithSpell in slots) {
                val slot = slotWithSpell.slot
                val spell = slotWithSpell.spell
                
                // Prüfe, ob Slot gefüllt ist und ein Ablaufdatum hat
                if (slot.isFilled && slot.expiryDate != null && spell != null) {
                    if (DerianDateCalculator.isSpellExpired(slot.expiryDate, newDate)) {
                        expiredSpells.add(character.name to spell.name)
                    }
                }
            }
            
            // Prüfe auf ablaufende Zauberzeichen
            val magicSigns = repository.getActiveMagicSignsListForCharacter(character.id)
            for (sign in magicSigns) {
                if (sign.expiryDate != null && !sign.isBotched) {
                    if (DerianDateCalculator.isSpellExpired(sign.expiryDate, newDate)) {
                        expiredMagicSigns.add(character.name to sign.name)
                    }
                }
            }
        }
        
        // Zeige Warnung, wenn Zauber oder Zauberzeichen ablaufen
        if (expiredSpells.isNotEmpty() || expiredMagicSigns.isNotEmpty()) {
            val message = buildString {
                if (expiredSpells.isNotEmpty()) {
                    appendLine("⚠️ Folgende Zauber laufen am $newDate ab:")
                    appendLine()
                    expiredSpells.groupBy { it.first }.forEach { (charName, spells) ->
                        appendLine("$charName:")
                        spells.forEach { (_, spellName) ->
                            appendLine("  • $spellName")
                        }
                    }
                }
                
                if (expiredMagicSigns.isNotEmpty()) {
                    if (expiredSpells.isNotEmpty()) appendLine()
                    appendLine("⚠️ Folgende Zauberzeichen laufen am $newDate ab:")
                    appendLine()
                    expiredMagicSigns.groupBy { it.first }.forEach { (charName, signs) ->
                        appendLine("$charName:")
                        signs.forEach { (_, signName) ->
                            appendLine("  • $signName")
                        }
                    }
                }
            }
            expiryWarningMessage = message
        }
    }
    
    // ========== Vollständiges Datenbank-Backup ==========
    
    /**
     * Validiert ein Backup und zeigt Bestätigungs-Dialog bei Warnungen.
     */
    fun validateAndPrepareBackupImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            backupImportState = BackupImportState.Validating(context, uri)
            
            val validationResult = backupManager.validateBackup(context, uri)
            
            validationResult.fold(
                onSuccess = { validation ->
                    if (validation.warnings.isEmpty()) {
                        // Keine Warnungen, direkt importieren
                        importFullBackup(context, uri)
                    } else {
                        // Warnungen vorhanden, Bestätigung erforderlich
                        backupImportState = BackupImportState.ConfirmationRequired(
                            validation = validation,
                            context = context,
                            uri = uri
                        )
                    }
                },
                onFailure = { error ->
                    backupImportState = BackupImportState.Error(
                        error.message ?: "Fehler beim Validieren des Backups"
                    )
                }
            )
        }
    }
    
    /**
     * Importiert ein vollständiges Datenbank-Backup.
     */
    fun importFullBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            backupImportState = BackupImportState.Importing("Starte Import...", 0)
            
            backupManager.importFullBackup(context, uri).collect { result ->
                result.fold(
                    onSuccess = { progress ->
                        if (progress.percentage == 100) {
                            // Import erfolgreich abgeschlossen
                            backupImportState = BackupImportState.Success(progress.stage)
                        } else {
                            // Import läuft
                            backupImportState = BackupImportState.Importing(
                                progress = progress.stage,
                                percentage = progress.percentage
                            )
                        }
                    },
                    onFailure = { error ->
                        backupImportState = BackupImportState.Error(
                            error.message ?: "Fehler beim Import"
                        )
                    }
                )
            }
        }
    }
    
    /**
     * Exportiert die komplette Datenbank als Backup.
     */
    fun exportFullBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            backupExportState = BackupExportState.Exporting("Starte Export...", 0)
            
            try {
                backupManager.exportFullBackup(context, uri).collect { progress ->
                    if (progress.percentage == 100) {
                        // Export erfolgreich abgeschlossen
                        backupExportState = BackupExportState.Success(
                            "Vollständiges Backup erfolgreich exportiert!"
                        )
                    } else {
                        // Export läuft
                        backupExportState = BackupExportState.Exporting(
                            progress = progress.stage,
                            percentage = progress.percentage
                        )
                    }
                }
            } catch (e: Exception) {
                backupExportState = BackupExportState.Error(
                    e.message ?: "Fehler beim Export"
                )
            }
        }
    }
    
    /**
     * Setzt den Backup-Import-State zurück.
     */
    fun resetBackupImportState() {
        backupImportState = BackupImportState.Idle
    }
    
    /**
     * Setzt den Backup-Export-State zurück.
     */
    fun resetBackupExportState() {
        backupExportState = BackupExportState.Idle
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
