package de.applicatus.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.InitialHerbs
import de.applicatus.app.data.InitialRegions
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.model.herb.Herb
import de.applicatus.app.data.model.herb.Landscape
import de.applicatus.app.data.model.herb.Region
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.BaseQuantityParser
import de.applicatus.app.logic.DerianDateCalculator
import de.applicatus.app.logic.HerbSearchCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HerbSearchViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    private val calculator = HerbSearchCalculator()
    
    private val _character = MutableStateFlow<Character?>(null)
    val character: StateFlow<Character?> = _character.asStateFlow()
    
    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()
    
    private val _currentDate = MutableStateFlow<String?>(null)
    val currentDate: StateFlow<String?> = _currentDate.asStateFlow()
    
    private val _selectedRegion = MutableStateFlow<Region?>(null)
    val selectedRegion: StateFlow<Region?> = _selectedRegion.asStateFlow()
    
    private val _selectedLandscape = MutableStateFlow<Landscape?>(null)
    val selectedLandscape: StateFlow<Landscape?> = _selectedLandscape.asStateFlow()
    
    private val _selectedHerb = MutableStateFlow<Herb?>(null)
    val selectedHerb: StateFlow<Herb?> = _selectedHerb.asStateFlow()
    
    private val _hasOrtskenntnis = MutableStateFlow(false)
    val hasOrtskenntnis: StateFlow<Boolean> = _hasOrtskenntnis.asStateFlow()
    
    private val _hasDoubledSearchTime = MutableStateFlow(false)
    val hasDoubledSearchTime: StateFlow<Boolean> = _hasDoubledSearchTime.asStateFlow()
    
    private val _searchResult = MutableStateFlow<HerbSearchCalculator.HerbSearchResult?>(null)
    val searchResult: StateFlow<HerbSearchCalculator.HerbSearchResult?> = _searchResult.asStateFlow()
    
    // Computed lists
    private val _availableLandscapes = MutableStateFlow<List<Landscape>>(emptyList())
    val availableLandscapes: StateFlow<List<Landscape>> = _availableLandscapes.asStateFlow()
    
    private val _availableHerbs = MutableStateFlow<List<Herb>>(emptyList())
    val availableHerbs: StateFlow<List<Herb>> = _availableHerbs.asStateFlow()
    
    init {
        loadCharacter()
    }
    
    private fun loadCharacter() {
        viewModelScope.launch {
            repository.getCharacterByIdFlow(characterId).collect { char ->
                Log.d("HerbSearchVM", "Character geladen: ${char?.name}, lastRegion=${char?.lastHerbSearchRegion}, lastLandscape=${char?.lastHerbSearchLandscape}")
                _character.value = char
                
                // Lade Gruppe und Datum
                char?.groupId?.let { groupId ->
                    repository.getGroupById(groupId).collect { group ->
                        _group.value = group
                        _currentDate.value = group?.currentDerianDate
                    }
                }
                
                // Initialisierung wird jetzt über initializeLastSearch() gemacht
            }
        }
    }
    
    /**
     * Initialisiert Region und Landschaft basierend auf den letzten gespeicherten Werten
     * Wird bei jedem Besuch der Screen aufgerufen
     */
    fun initializeLastSearch() {
        val char = _character.value ?: return
        
        Log.d("HerbSearchVM", "initializeLastSearch: lastRegion=${char.lastHerbSearchRegion}, lastLandscape=${char.lastHerbSearchLandscape}")
        
        // Initialisiere Region mit lastHerbSearchRegion
        if (!char.lastHerbSearchRegion.isNullOrBlank()) {
            Log.d("HerbSearchVM", "Versuche Region zu setzen: ${char.lastHerbSearchRegion}")
            val region = getAllRegions().find { it.name == char.lastHerbSearchRegion }
            if (region != null && _selectedRegion.value?.name != region.name) {
                Log.d("HerbSearchVM", "Setze Region: ${region.name}")
                selectRegion(region)
                
                // Initialisiere auch Landschaft mit lastHerbSearchLandscape
                if (!char.lastHerbSearchLandscape.isNullOrBlank()) {
                    Log.d("HerbSearchVM", "Versuche Landschaft zu setzen: ${char.lastHerbSearchLandscape}")
                    val landscape = _availableLandscapes.value.find { 
                        it.displayName == char.lastHerbSearchLandscape 
                    }
                    if (landscape != null) {
                        Log.d("HerbSearchVM", "Setze Landschaft: ${landscape.displayName}")
                        selectLandscape(landscape)
                    } else {
                        Log.w("HerbSearchVM", "Landschaft nicht gefunden: ${char.lastHerbSearchLandscape}")
                    }
                }
            } else {
                Log.d("HerbSearchVM", "Region nicht gesetzt: region=$region, current=${_selectedRegion.value?.name}")
            }
        } else {
            Log.d("HerbSearchVM", "Keine lastHerbSearchRegion gespeichert")
        }
    }
    
    fun selectRegion(region: Region?) {
        _selectedRegion.value = region
        _availableLandscapes.value = region?.landscapes ?: emptyList()
        
        // Reset dependent selections
        if (region == null || !_availableLandscapes.value.contains(_selectedLandscape.value)) {
            _selectedLandscape.value = null
        }
        updateAvailableHerbs()
    }
    
    fun selectLandscape(landscape: Landscape?) {
        _selectedLandscape.value = landscape
        updateAvailableHerbs()
    }
    
    fun selectHerb(herb: Herb?) {
        _selectedHerb.value = herb
    }
    
    fun setOrtskenntnis(hasIt: Boolean) {
        _hasOrtskenntnis.value = hasIt
    }
    
    fun setDoubledSearchTime(hasIt: Boolean) {
        _hasDoubledSearchTime.value = hasIt
    }
    
    private fun updateAvailableHerbs() {
        val region = _selectedRegion.value
        val landscape = _selectedLandscape.value
        val currentDateStr = _currentDate.value
        
        if (region == null || landscape == null) {
            _availableHerbs.value = emptyList()
            _selectedHerb.value = null
            return
        }
        
        // Extrahiere Monat aus aktuellem Gruppendatum
        val month = if (currentDateStr != null) {
            DerianDateCalculator.getMonthFromDate(currentDateStr)
        } else {
            DerianDateCalculator.DerianMonth.FULL_YEAR
        }
        
        val herbs = calculator.getAvailableHerbs(
            regionHerbs = region.herbs,
            landscape = landscape,
            month = month
        )
        
        _availableHerbs.value = herbs
        
        // Reset herb selection if it's no longer available
        if (_selectedHerb.value !in herbs) {
            _selectedHerb.value = null
        }
    }
    
    fun performSearch() {
        val char = _character.value ?: return
        val herb = _selectedHerb.value ?: return
        val landscape = _selectedLandscape.value ?: return
        val region = _selectedRegion.value ?: return
        val currentDateStr = _currentDate.value ?: return
        
        val result = calculator.performHerbSearch(
            character = char,
            herb = herb,
            landscape = landscape,
            hasOrtskenntnis = _hasOrtskenntnis.value,
            hasDoubledSearchTime = _hasDoubledSearchTime.value
        )
        
        viewModelScope.launch {
            // Würfel die Mengen aus (für Anzeige und Inventar)
            val harvestedItems = if (result.success && result.foundQuantity != null) {
                // Würfel jede Portion einzeln
                val allItems = mutableListOf<BaseQuantityParser.HerbHarvestItem>()
                repeat(result.portionCount) {
                    val portionItems = BaseQuantityParser.rollQuantity(result.foundQuantity, result.qualityPoints)
                    allItems.addAll(portionItems)
                }
                
                // Summiere Items mit gleichem Produktnamen
                allItems.groupBy { it.productName }
                    .map { (productName, items) ->
                        val totalQuantity = items.sumOf { it.quantity.toIntOrNull() ?: 0 }
                        val firstDiceRoll = items.firstOrNull()?.diceRoll
                        // Sammle alle Einzelwürfe für detaillierte Anzeige
                        val allRolls = items.mapNotNull { it.quantity.toIntOrNull() }
                        BaseQuantityParser.HerbHarvestItem(
                            productName = productName,
                            quantity = totalQuantity.toString(),
                            rolled = true,
                            diceRoll = firstDiceRoll,
                            individualRolls = allRolls
                        )
                    }
            } else {
                emptyList()
            }
            
            // Aktualisiere Result mit harvestedItems
            val resultWithItems = result.copy(harvestedItems = harvestedItems)
            _searchResult.value = resultWithItems
            
            // Speichere lastHerbSearchRegion UND lastHerbSearchLandscape
            Log.d("HerbSearchVM", "Speichere Region=${region.name}, Landschaft=${landscape.displayName}")
            repository.updateCharacter(char.copy(
                lastHerbSearchRegion = region.name,
                lastHerbSearchLandscape = landscape.displayName
            ))
            Log.d("HerbSearchVM", "Character aktualisiert")
            
            // Bei Erfolg: Kräuter ins Inventar legen
            if (result.success && result.foundQuantity != null) {
                addHerbsToInventory(herb, harvestedItems, currentDateStr)
            }
        }
    }
    
    private suspend fun addHerbsToInventory(herb: Herb, harvestedItems: List<BaseQuantityParser.HerbHarvestItem>, currentDate: String) {
        // Items wurden bereits gewürfelt
        
        // Finde Kräutertasche
        val herbPouch = repository.getHerbPouchLocationForCharacter(characterId)
        val locationId = herbPouch?.id
        
        // Standard-Haltbarkeit für Kräuter: 1 Jahr
        // TODO: shelfLife-Feld zu Herb-Modell hinzufügen und aus Herb-Daten verwenden
        val shelfLife = "1 Jahr"
        val expiryDate = DerianDateCalculator.calculateExpiryDate(currentDate, shelfLife)
        
        // Erstelle Items
        harvestedItems.forEach { harvestItem ->
            val item = Item(
                characterId = characterId,
                name = "${herb.name}: ${harvestItem.productName}",
                quantity = harvestItem.quantity.toIntOrNull() ?: 1,
                locationId = locationId,
                isHerb = true,
                expiryDate = expiryDate,
                isCountable = true  // Kräuter sind immer zählbar
            )
            repository.insertItem(item)
        }
    }
    
    fun resetSearch() {
        _searchResult.value = null
    }
    
    fun getAllRegions(): List<Region> = InitialRegions.ALL_REGIONS
    
    fun getHerbSearchTaW(): Int? {
        val char = _character.value ?: return null
        return calculator.calculateHerbSearchTaW(char)
    }
    
    /**
     * Gibt den effektiven TaW zurück (mit 1.5x bei doppelter Suchdauer)
     */
    fun getEffectiveTaW(): Int? {
        val baseTaw = getHerbSearchTaW() ?: return null
        return if (_hasDoubledSearchTime.value) {
            kotlin.math.ceil(baseTaw * 1.5).toInt()
        } else {
            baseTaw
        }
    }
    
    fun getSearchDifficulty(): Int? {
        val char = _character.value ?: return null
        val herb = _selectedHerb.value ?: return null
        val landscape = _selectedLandscape.value ?: return null
        
        return calculator.calculateSearchDifficulty(
            herb = herb,
            landscape = landscape,
            hasOrtskenntnis = _hasOrtskenntnis.value,
            character = char
        )
    }
}

class HerbSearchViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HerbSearchViewModel::class.java)) {
            return HerbSearchViewModel(repository, characterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
