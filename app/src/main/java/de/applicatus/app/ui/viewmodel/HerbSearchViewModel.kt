package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.InitialHerbs
import de.applicatus.app.data.InitialRegions
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.herb.Herb
import de.applicatus.app.data.model.herb.Landscape
import de.applicatus.app.data.model.herb.Region
import de.applicatus.app.data.repository.ApplicatusRepository
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
    
    private val _selectedRegion = MutableStateFlow<Region?>(null)
    val selectedRegion: StateFlow<Region?> = _selectedRegion.asStateFlow()
    
    private val _selectedLandscape = MutableStateFlow<Landscape?>(null)
    val selectedLandscape: StateFlow<Landscape?> = _selectedLandscape.asStateFlow()
    
    private val _selectedMonth = MutableStateFlow<DerianDateCalculator.DerianMonth>(DerianDateCalculator.DerianMonth.FULL_YEAR)
    val selectedMonth: StateFlow<DerianDateCalculator.DerianMonth> = _selectedMonth.asStateFlow()
    
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
                _character.value = char
            }
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
    
    fun selectMonth(month: DerianDateCalculator.DerianMonth) {
        _selectedMonth.value = month
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
        val month = _selectedMonth.value
        
        if (region == null || landscape == null) {
            _availableHerbs.value = emptyList()
            _selectedHerb.value = null
            return
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
        
        val result = calculator.performHerbSearch(
            character = char,
            herb = herb,
            landscape = landscape,
            hasOrtskenntnis = _hasOrtskenntnis.value,
            hasDoubledSearchTime = _hasDoubledSearchTime.value
        )
        
        _searchResult.value = result
    }
    
    fun resetSearch() {
        _searchResult.value = null
    }
    
    fun getAllRegions(): List<Region> = InitialRegions.ALL_REGIONS
    
    fun getAllMonths(): List<DerianDateCalculator.DerianMonth> = DerianDateCalculator.DerianMonth.values().toList()
    
    fun getHerbSearchTaW(): Int? {
        val char = _character.value ?: return null
        return calculator.calculateHerbSearchTaW(char)
    }
    
    fun getSearchDifficulty(): Int? {
        val char = _character.value ?: return null
        val herb = _selectedHerb.value ?: return null
        val landscape = _selectedLandscape.value ?: return null
        
        return calculator.calculateSearchDifficulty(
            herb = herb,
            landscape = landscape,
            hasOrtskenntnis = _hasOrtskenntnis.value,
            hasDoubledSearchTime = _hasDoubledSearchTime.value,
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
