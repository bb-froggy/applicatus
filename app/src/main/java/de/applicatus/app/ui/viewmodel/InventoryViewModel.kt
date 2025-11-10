package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.model.potion.PotionWithRecipe
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel für die Inventar-Verwaltung (Packesel)
 */
class InventoryViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    // Character-Daten
    val character = repository.getCharacterByIdFlow(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Locations
    val locations = repository.getLocationsForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Items mit Location-Namen
    val itemsWithLocation = repository.getItemsWithLocationForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Tränke des Charakters
    val potions = repository.getPotionsForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Gruppiert Items und Tränke nach Location
     */
    val itemsByLocation: StateFlow<Map<Location?, List<ItemWithLocation>>> = 
        combine(locations, itemsWithLocation, potions) { locs, items, pots ->
            // Erstelle eine Map: Location -> Items
            val groupedItems = items.groupBy { item ->
                locs.find { it.id == item.locationId }
            }.toMutableMap()
            
            // Füge Tränke als Items hinzu (virtuell)
            pots.forEach { potionWithRecipe ->
                val location = locs.find { it.id == potionWithRecipe.potion.locationId }
                val virtualItem = ItemWithLocation(
                    id = -potionWithRecipe.potion.id, // Negative ID für Tränke
                    characterId = characterId,
                    locationId = potionWithRecipe.potion.locationId,
                    name = if (potionWithRecipe.potion.nameKnown) potionWithRecipe.recipe.name else "Unbekannter Trank",
                    stone = Weight.POTION.stone,
                    ounces = Weight.POTION.ounces,
                    sortOrder = 0,
                    locationName = location?.name
                )
                
                val currentList = groupedItems[location] ?: emptyList()
                groupedItems[location] = currentList + virtualItem
            }
            
            groupedItems
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /**
     * Berechnet das Gesamtgewicht pro Location
     */
    val weightByLocation: StateFlow<Map<Long?, Weight>> =
        combine(itemsWithLocation, potions) { items, pots ->
            val weights = mutableMapOf<Long?, Int>() // Location ID -> Gewicht in Unzen
            
            // Items
            items.forEach { item ->
                val currentWeight = weights[item.locationId] ?: 0
                weights[item.locationId] = currentWeight + item.weight.toOunces()
            }
            
            // Tränke (je 4 Unzen)
            pots.forEach { potionWithRecipe ->
                val currentWeight = weights[potionWithRecipe.potion.locationId] ?: 0
                weights[potionWithRecipe.potion.locationId] = currentWeight + Weight.POTION.toOunces()
            }
            
            // Konvertiere zu Weight-Objekten
            weights.mapValues { Weight.fromOunces(it.value) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /**
     * Fügt eine neue Location hinzu
     */
    fun addLocation(name: String) {
        viewModelScope.launch {
            val sortOrder = locations.value.size
            repository.insertLocation(
                Location(
                    characterId = characterId,
                    name = name,
                    isDefault = false,
                    sortOrder = sortOrder
                )
            )
        }
    }
    
    /**
     * Aktualisiert eine Location
     */
    fun updateLocation(location: Location) {
        viewModelScope.launch {
            repository.updateLocation(location)
        }
    }
    
    /**
     * Löscht eine Location (nur nicht-Standard-Locations)
     */
    fun deleteLocation(location: Location) {
        if (!location.isDefault) {
            viewModelScope.launch {
                repository.deleteLocation(location)
            }
        }
    }
    
    /**
     * Fügt ein neues Item hinzu
     */
    fun addItem(name: String, weight: Weight, locationId: Long?) {
        viewModelScope.launch {
            repository.insertItem(
                Item(
                    characterId = characterId,
                    locationId = locationId,
                    name = name,
                    weight = weight
                )
            )
        }
    }
    
    /**
     * Aktualisiert ein Item
     */
    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }
    
    /**
     * Löscht ein Item
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }
    
    /**
     * Verschiebt ein Item zu einer anderen Location
     */
    fun moveItemToLocation(itemId: Long, newLocationId: Long?) {
        viewModelScope.launch {
            val item = repository.getItemById(itemId)
            item?.let {
                repository.updateItem(it.copy(locationId = newLocationId))
            }
        }
    }
    
    /**
     * Verschiebt einen Trank zu einer anderen Location
     */
    fun movePotionToLocation(potionId: Long, newLocationId: Long?) {
        viewModelScope.launch {
            val potion = repository.getPotionById(potionId)
            potion?.let {
                repository.updatePotion(it.copy(locationId = newLocationId))
            }
        }
    }
}

/**
 * Factory für das InventoryViewModel
 */
class InventoryViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository, characterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
