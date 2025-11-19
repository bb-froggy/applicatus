package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.model.potion.PotionWithRecipe
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    
    // Locations
    val locations = repository.getLocationsForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Items mit Location-Namen
    val itemsWithLocation = repository.getItemsWithLocationForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Tränke des Charakters
    val potions = repository.getPotionsForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Rezeptwissen des Charakters
    private val recipeKnowledge = repository.getRecipeKnowledgeForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Gruppenmitglieder (alle Charaktere der gleichen Gruppe, außer dem aktuellen)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val groupMembers: StateFlow<List<Character>> = character.flatMapLatest { char ->
        if (char?.groupId != null) {
            repository.getCharactersByGroupId(char.groupId).map { characters ->
                characters.filter { it.id != characterId }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Gruppiert Items und Tränke nach Location
     */
    val itemsByLocation: StateFlow<Map<Location?, List<ItemWithLocation>>> = 
        combine(locations, itemsWithLocation, potions, recipeKnowledge) { locs, items, pots, recipeKnow ->
            // Erstelle eine Map: Location -> Items
            val groupedItems = items.groupBy { item ->
                locs.find { it.id == item.locationId }
            }.toMutableMap()
            
            // Füge Tränke als Items hinzu (virtuell)
            pots.forEach { potionWithRecipe ->
                val location = locs.find { it.id == potionWithRecipe.potion.locationId }
                
                // Prüfe, ob das Rezept bekannt/verstanden ist
                val recipeKnowledgeItem = recipeKnow.find { it.recipeId == potionWithRecipe.recipe.id }
                val isRecipeKnown = recipeKnowledgeItem?.knowledgeLevel == RecipeKnowledgeLevel.KNOWN || 
                                   recipeKnowledgeItem?.knowledgeLevel == RecipeKnowledgeLevel.UNDERSTOOD
                
                // Name ist bekannt wenn:
                // 1. Selbst gebraut (nameKnown = true) ODER
                // 2. Strukturanalyse durchgeführt (categoryKnown = true) UND Rezept bekannt/verstanden
                val isNameKnown = potionWithRecipe.potion.nameKnown || 
                                 (potionWithRecipe.potion.categoryKnown && isRecipeKnown)
                
                val displayName = if (isNameKnown) {
                    potionWithRecipe.recipe.name
                } else {
                    "Unbekannter Trank"
                }
                
                val virtualItem = ItemWithLocation(
                    id = -potionWithRecipe.potion.id, // Negative ID für Tränke
                    characterId = characterId,
                    locationId = potionWithRecipe.potion.locationId,
                    name = displayName,
                    stone = Weight.POTION.stone,
                    ounces = Weight.POTION.ounces,
                    sortOrder = 0,
                    locationName = location?.name,
                    isPurse = false,
                    kreuzerAmount = 0,
                    appearance = potionWithRecipe.potion.appearance.takeIf { it.isNotBlank() },
                    isCountable = true,
                    quantity = potionWithRecipe.potion.quantity
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
                val itemWeight = if (item.isCountable) {
                    item.totalWeight.toOunces()
                } else {
                    item.weight.toOunces()
                }
                weights[item.locationId] = currentWeight + itemWeight
            }
            
            // Tränke (je 4 Unzen * Menge)
            pots.forEach { potionWithRecipe ->
                val currentWeight = weights[potionWithRecipe.potion.locationId] ?: 0
                val potionTotalWeight = Weight.POTION.toOunces() * potionWithRecipe.potion.quantity
                weights[potionWithRecipe.potion.locationId] = currentWeight + potionTotalWeight
            }
            
            // Konvertiere zu Weight-Objekten
            weights.mapValues { Weight.fromOunces(it.value) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /**
     * Berechnet das getragene Gesamtgewicht
     * Rüstung/Kleidung zählt nur halb
     */
    val carriedWeight: StateFlow<Weight> =
        combine(locations, weightByLocation) { locs, weights ->
            var totalOunces = 0
            locs.filter { it.isCarried }.forEach { location ->
                val locationWeight = weights[location.id]?.toOunces() ?: 0
                // Rüstung/Kleidung zählt nur halb
                if (location.name == "Rüstung/Kleidung" && location.isDefault) {
                    totalOunces += locationWeight / 2
                } else {
                    totalOunces += locationWeight
                }
            }
            Weight.fromOunces(totalOunces)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Weight.ZERO)
    
    /**
     * Tragfähigkeit: Körperkraft in Stein
     */
    val carryingCapacity: StateFlow<Weight> =
        character.map { char ->
            val kk = char?.kk ?: 10
            Weight(stone = kk, ounces = 0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Weight.ZERO)
    
    /**
     * Last-BE: Für jedes angefangene halbe Tragfähigkeitsgewicht über der Tragfähigkeit
     */
    val encumbrancePenalty: StateFlow<Int> =
        combine(carriedWeight, carryingCapacity) { carried, capacity ->
            val carriedOunces = carried.toOunces()
            val capacityOunces = capacity.toOunces()
            
            if (carriedOunces <= capacityOunces) {
                0
            } else {
                val overweight = carriedOunces - capacityOunces
                val halfCapacity = capacityOunces / 2
                // Für jedes angefangene halbe Tragfähigkeitsgewicht +1 BE
                ((overweight + halfCapacity - 1) / halfCapacity)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
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
     * Aktualisiert den "Getragen"-Status einer Location
     */
    fun updateLocationIsCarried(locationId: Long, isCarried: Boolean) {
        viewModelScope.launch {
            repository.updateLocationIsCarried(locationId, isCarried)
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
     * Tauscht die Sortierreihenfolge von zwei Locations
     */
    fun swapLocationOrder(location1Id: Long, location2Id: Long) {
        viewModelScope.launch {
            val allLocations = locations.value
            val loc1 = allLocations.find { it.id == location1Id }
            val loc2 = allLocations.find { it.id == location2Id }
            
            if (loc1 != null && loc2 != null) {
                // Tausche sortOrder
                repository.updateLocation(loc1.copy(sortOrder = loc2.sortOrder))
                repository.updateLocation(loc2.copy(sortOrder = loc1.sortOrder))
            }
        }
    }
    
    /**
     * Aktualisiert die Sortierreihenfolge aller Locations basierend auf der aktuellen Liste
     */
    fun updateLocationOrder(orderedLocationIds: List<Long>) {
        viewModelScope.launch {
            val allLocations = locations.value
            orderedLocationIds.forEachIndexed { index, locationId ->
                val location = allLocations.find { it.id == locationId }
                location?.let {
                    repository.updateLocation(it.copy(sortOrder = index))
                }
            }
        }
    }
    
    /**
     * Fügt ein neues Item hinzu
     */
    fun addItem(
        name: String, 
        weight: Weight, 
        locationId: Long?, 
        isPurse: Boolean = false,
        isCountable: Boolean = false,
        quantity: Int = 1
    ) {
        viewModelScope.launch {
            repository.insertItem(
                Item(
                    characterId = characterId,
                    locationId = locationId,
                    name = name,
                    weight = weight,
                    isPurse = isPurse,
                    kreuzerAmount = if (isPurse) 0 else 0,
                    isCountable = isCountable,
                    quantity = quantity
                )
            )
        }
    }
    
    /**
     * Aktualisiert den Geldbetrag in einem Geldbeutel
     */
    fun updatePurseAmount(itemId: Long, kreuzerAmount: Int) {
        viewModelScope.launch {
            repository.updatePurseAmount(itemId, kreuzerAmount)
        }
    }
    
    /**
     * Aktualisiert die Menge eines zählbaren Gegenstands
     */
    fun updateItemQuantity(itemId: Long, quantity: Int) {
        viewModelScope.launch {
            repository.updateItemQuantity(itemId, quantity)
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
    
    /**
     * Teilt einen zählbaren Gegenstand und verschiebt einen Teil zu einer anderen Location
     * @param itemId Die ID des zu teilenden Gegenstands
     * @param quantityToMove Die Anzahl, die verschoben werden soll
     * @param targetLocationId Die Ziel-Location
     */
    fun splitAndMoveItem(itemId: Long, quantityToMove: Int, targetLocationId: Long?) {
        viewModelScope.launch {
            repository.splitAndMoveItem(itemId, quantityToMove, targetLocationId)
        }
    }
    
    /**
     * Teilt einen Trank und verschiebt einen Teil zu einer anderen Location
     * @param potionId Die ID des zu teilenden Tranks
     * @param quantityToMove Die Anzahl, die verschoben werden soll
     * @param targetLocationId Die Ziel-Location
     */
    fun splitAndMovePotion(potionId: Long, quantityToMove: Int, targetLocationId: Long?) {
        viewModelScope.launch {
            repository.splitAndMovePotion(potionId, quantityToMove, targetLocationId)
        }
    }
    
    /**
     * Überträgt eine Location mit allen Items und Tränken zu einem anderen Charakter
     */
    fun transferLocationToCharacter(locationId: Long, targetCharacterId: Long) {
        viewModelScope.launch {
            repository.transferLocationToCharacter(locationId, targetCharacterId)
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
