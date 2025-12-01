package de.applicatus.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.JournalCategory
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.magicsign.MagicSign
import de.applicatus.app.data.model.magicsign.MagicSignDuration
import de.applicatus.app.data.model.magicsign.MagicSignEffect
import de.applicatus.app.data.model.magicsign.MagicSignWithItem
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.DerianDateCalculator
import de.applicatus.app.logic.MagicSignActivationResult
import de.applicatus.app.logic.MagicSignChecker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel für die Zauberzeichen-Verwaltung
 */
class MagicSignViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    // Character-Daten
    val character = repository.getCharacterByIdFlow(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Game Master Mode (auf Gruppen-Ebene)
    @OptIn(ExperimentalCoroutinesApi::class)
    val isGameMasterGroup: StateFlow<Boolean> = character
        .mapLatest { char ->
            char?.groupId?.let { groupId ->
                repository.getGroupByIdOnce(groupId)?.isGameMasterGroup
            } ?: false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Aktuelles derisches Datum der Gruppe
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDerianDate: StateFlow<String> = character
        .mapLatest { char ->
            char?.groupId?.let { groupId ->
                repository.getGroupByIdOnce(groupId)?.currentDerianDate
            } ?: "1 Praios 1040 BF"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "1 Praios 1040 BF"
        )
    
    // Zauberzeichen mit Item-Informationen
    val magicSignsWithItems = repository.getMagicSignsWithItemsForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Alle Items für die Zielauswahl (inkl. Eigenobjekte)
    val items = repository.getItemsForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Locations für die Zielauswahl (Eigenobjekte als Ziel)
    val locations = repository.getLocationsForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Items die als Ziel für Zauberzeichen geeignet sind (normale Items + Eigenobjekte)
    val availableTargets: StateFlow<List<Item>> = combine(items, locations) { itemList, locationList ->
        // Alle Items außer Tränke (negative IDs sind virtuelle Tränke-Items)
        itemList.filter { it.id > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Letzte Aktivierungsergebnis
    var lastActivationResult by mutableStateOf<MagicSignActivationResult?>(null)
        private set
    
    // Anzeige-Dialog für Aktivierungsergebnis
    var showActivationResult by mutableStateOf(false)
        private set
    
    /**
     * Erstellt ein neues Zauberzeichen
     */
    fun createMagicSign(
        name: String,
        effectDescription: String,
        effect: MagicSignEffect,
        activationModifier: Int,
        duration: MagicSignDuration,
        targetItemId: Long
    ) {
        viewModelScope.launch {
            val magicSign = MagicSign(
                characterId = characterId,
                itemId = targetItemId,
                name = name,
                effectDescription = effectDescription,
                effect = effect,
                activationModifier = activationModifier,
                duration = duration,
                isActivated = false,
                isBotched = false
            )
            repository.insertMagicSign(magicSign)
            
            // Journal-Eintrag
            val targetItem = repository.getItemById(targetItemId)
            repository.logCharacterEvent(
                characterId = characterId,
                category = JournalCategory.MAGIC_SIGN_CREATED,
                playerMessage = "Zauberzeichen '$name' auf ${targetItem?.name ?: "unbekannt"} angebracht."
            )
        }
    }
    
    /**
     * Aktiviert ein Zauberzeichen (führt die Probe durch)
     */
    fun activateMagicSign(magicSign: MagicSign) {
        viewModelScope.launch {
            val char = character.value ?: return@launch
            val date = currentDerianDate.value
            
            // Führe die Aktivierungsprobe durch
            val result = MagicSignChecker.performActivationProbe(char, magicSign, date)
            lastActivationResult = result
            showActivationResult = true
            
            // Aktualisiere das Zauberzeichen
            val updatedSign = magicSign.copy(
                isActivated = result.success && !result.isBotched,
                isBotched = result.isBotched,
                expiryDate = result.calculatedExpiryDate,
                activationRkpStar = if (result.success) result.rkpStar else null,
                lastRollResult = result.formattedRollResult
            )
            repository.updateMagicSign(updatedSign)
            
            // Journal-Eintrag
            val targetItem = repository.getItemById(magicSign.itemId)
            val gmMessage = if (result.isBotched) {
                "⚠️ PATZER! Das Zauberzeichen ist verdorben und wirkungslos."
            } else null
            
            repository.logCharacterEvent(
                characterId = characterId,
                category = if (result.success && !result.isBotched) 
                    JournalCategory.MAGIC_SIGN_ACTIVATED 
                else 
                    JournalCategory.MAGIC_SIGN_ACTIVATION_FAILED,
                playerMessage = if (result.success && !result.isBotched) {
                    "Zauberzeichen '${magicSign.name}' auf ${targetItem?.name ?: "unbekannt"} aktiviert. " +
                    "RkP*: ${result.rkpStar}, wirkt bis ${result.calculatedExpiryDate}."
                } else {
                    "Aktivierung von '${magicSign.name}' fehlgeschlagen."
                },
                gmMessage = gmMessage
            )
        }
    }
    
    /**
     * Entfernt ein Zauberzeichen
     */
    fun deleteMagicSign(magicSign: MagicSign) {
        viewModelScope.launch {
            repository.deleteMagicSign(magicSign)
            
            repository.logCharacterEvent(
                characterId = characterId,
                category = JournalCategory.MAGIC_SIGN_REMOVED,
                playerMessage = "Zauberzeichen '${magicSign.name}' entfernt."
            )
        }
    }
    
    /**
     * Schließt den Aktivierungsergebnis-Dialog
     */
    fun dismissActivationResult() {
        showActivationResult = false
    }
    
    /**
     * Prüft, ob ein Zauberzeichen abgelaufen ist
     */
    fun isExpired(magicSign: MagicSign): Boolean {
        val expiryDate = magicSign.expiryDate ?: return false
        val currentDate = currentDerianDate.value
        return DerianDateCalculator.isExpired(expiryDate, currentDate)
    }
    
    /**
     * Prüft, ob der Charakter Zauberzeichen verwenden kann
     */
    fun canUseZauberzeichen(): Boolean {
        return character.value?.let { MagicSignChecker.canUseZauberzeichen(it) } ?: false
    }
    
    /**
     * Erstellt ein Eigenobjekt für eine Location (falls noch nicht vorhanden)
     */
    fun createSelfItemForLocation(location: Location) {
        viewModelScope.launch {
            repository.getOrCreateSelfItemForLocation(location)
        }
    }
    
    /**
     * Findet alle ablaufenden Zauberzeichen (innerhalb von 7 Tagen)
     */
    fun getExpiringSigns(): List<MagicSignWithItem> {
        val currentDate = currentDerianDate.value
        val currentDays = DerianDateCalculator.parseDateToDays(currentDate) ?: return emptyList()
        
        return magicSignsWithItems.value.filter { signWithItem ->
            val sign = signWithItem.magicSign
            if (!sign.isActivated || sign.isBotched) return@filter false
            
            val expiryDays = sign.expiryDate?.let { DerianDateCalculator.parseDateToDays(it) } ?: return@filter false
            val daysUntilExpiry = expiryDays - currentDays
            daysUntilExpiry in 0..7
        }
    }
    
    class Factory(
        private val repository: ApplicatusRepository,
        private val characterId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MagicSignViewModel::class.java)) {
                return MagicSignViewModel(repository, characterId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
