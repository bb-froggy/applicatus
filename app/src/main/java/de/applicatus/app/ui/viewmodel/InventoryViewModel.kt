package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.ItemWithMagic
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.MagicIndicator
import de.applicatus.app.data.model.inventory.MagicIndicatorType
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.model.magicsign.MagicSignEffect
import de.applicatus.app.data.model.potion.PotionWithRecipe
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.model.spell.SlotType
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.DerianDateCalculator
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
    
    // Zauberzeichen mit Item-Informationen (für Gewichtsreduktion)
    private val magicSignsWithItems = repository.getMagicSignsWithItemsForCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Alle SpellSlots des Charakters (für Magic-Indikatoren auf Items)
    private val allSpellSlots = repository.getSlotsByCharacter(characterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Alle Zauber (für Slot-Namen)
    private val allSpells = repository.allSpells
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Aktuelles derisches Datum der Gruppe
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentDerianDate: StateFlow<String> = character
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
     * Items mit Magic-Indikatoren und Gewichtsreduktion, gruppiert nach Location.
     * Enthält Zauberzeichen-Informationen und SpellSlot-Informationen für jedes Item.
     * 
     * Hinweis: Das Eigenobjekt von "Rüstung/Kleidung" wird ausgeblendet, da
     * Kleidung/Rüstung kein eigenes Gewicht hat (wird direkt am Körper getragen).
     */
    val itemsWithMagicByLocation: StateFlow<Map<Location?, List<ItemWithMagic>>> =
        combine(itemsByLocation, magicSignsWithItems, allSpellSlots, allSpells, currentDerianDate) { itemsMap, signs, slots, spells, currentDate ->
            val currentDays = DerianDateCalculator.parseDateToDays(currentDate) ?: 0
            
            itemsMap.mapValues { (location, items) ->
                // Filtere das Eigenobjekt von "Rüstung/Kleidung" aus
                val filteredItems = if (location?.name == "Rüstung/Kleidung") {
                    items.filter { !it.isSelfItem }
                } else {
                    items
                }
                
                filteredItems.map { item ->
                    // Finde Zauberzeichen für dieses Item
                    val itemSigns = signs.filter { it.magicSign.itemId == item.id }
                    
                    // Finde SpellSlots, die an dieses Item gebunden sind
                    val itemSlots = slots.filter { it.itemId == item.id }
                    
                    // Erstelle Magic-Indikatoren für Zauberzeichen
                    val signIndicators = itemSigns.map { signWithItem ->
                        val sign = signWithItem.magicSign
                        val isExpired = sign.expiryDate?.let { expiryDate ->
                            DerianDateCalculator.parseDateToDays(expiryDate)?.let { expiryDays ->
                                expiryDays < currentDays
                            }
                        } ?: false
                        
                        MagicIndicator(
                            type = MagicIndicatorType.MAGIC_SIGN,
                            name = sign.name,
                            description = sign.effectDescription,
                            isActive = sign.isActivated && !isExpired,
                            isBotched = sign.isBotched,
                            expiryDate = sign.expiryDate,
                            effectPoints = sign.activationRkpStar,
                            sourceId = sign.id,
                            magicSignEffect = sign.effect,
                            activationModifier = sign.activationModifier
                        )
                    }
                    
                    // Erstelle Magic-Indikatoren für SpellSlots (Applicatus und langwährende Zauber)
                    val slotIndicators = itemSlots.mapNotNull { slot ->
                        val spell = spells.find { it.id == slot.spellId }
                        val spellName = spell?.name ?: "Unbekannter Zauber"
                        
                        val isExpired = slot.expiryDate?.let { expiryDate ->
                            DerianDateCalculator.parseDateToDays(expiryDate)?.let { expiryDays ->
                                expiryDays < currentDays
                            }
                        } ?: false
                        
                        val indicatorType = when (slot.slotType) {
                            SlotType.APPLICATUS -> MagicIndicatorType.APPLICATUS
                            SlotType.LONG_DURATION -> MagicIndicatorType.LONG_DURATION_SPELL
                            else -> return@mapNotNull null // Zauberspeicher werden nicht angezeigt
                        }
                        
                        MagicIndicator(
                            type = indicatorType,
                            name = spellName,
                            description = if (slot.variant.isNotBlank()) slot.variant else "",
                            isActive = slot.isFilled && !isExpired,
                            isBotched = slot.isBotched,
                            expiryDate = slot.expiryDate,
                            effectPoints = slot.zfpStar,
                            sourceId = slot.id
                        )
                    }
                    
                    val indicators = signIndicators + slotIndicators
                    
                    // Berechne Gewichtsreduktion für dieses Item (nur für normale Items, nicht Eigenobjekte)
                    var originalWeight = item.weight
                    var reducedWeight = item.weight
                    
                    // Nur normale Items (nicht Eigenobjekte) bekommen individuelle Gewichtsreduktion
                    // Eigenobjekte reduzieren das ganze Location-Gewicht stattdessen
                    if (!item.isSelfItem) {
                        val activeWeightReductionSigns = itemSigns.filter { signWithItem ->
                            val sign = signWithItem.magicSign
                            sign.isActivated &&
                            !sign.isBotched &&
                            sign.effect == MagicSignEffect.WEIGHT_REDUCTION &&
                            sign.expiryDate?.let { expiryDate ->
                                DerianDateCalculator.parseDateToDays(expiryDate)?.let { expiryDays ->
                                    expiryDays >= currentDays
                                }
                            } ?: false
                        }
                        
                        if (activeWeightReductionSigns.isNotEmpty()) {
                            // Summiere alle Gewichtsreduktionen
                            var totalReductionOunces = 0
                            activeWeightReductionSigns.forEach { signWithItem ->
                                val rkpStar = signWithItem.magicSign.activationRkpStar ?: 0
                                // RkP* × 2 Stein Reduktion
                                totalReductionOunces += rkpStar * 2 * 40
                            }
                            
                            // Reduziere Gewicht: Mindestens 1 Stein (40 Unzen) bleibt, aber nie schwerer als Original
                            val originalOunces = originalWeight.toOunces()
                            val minimumOunces = if (originalOunces >= 40) 40 else originalOunces
                            val reducedOunces = maxOf(originalOunces - totalReductionOunces, minimumOunces)
                            reducedWeight = Weight.fromOunces(reducedOunces)
                        }
                    }
                    
                    ItemWithMagic(
                        item = item,
                        magicIndicators = indicators,
                        isSelfItem = item.isSelfItem,
                        originalWeight = if (reducedWeight != originalWeight) originalWeight else null,
                        reducedWeight = if (reducedWeight != originalWeight) reducedWeight else null
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /**
     * Berechnet das Gesamtgewicht pro Location
     * Berücksichtigt aktive Zauberzeichen mit WEIGHT_REDUCTION-Effekt:
     * - Auf Eigenobjekt: Reduziert das gesamte Location-Gewicht
     * - Auf normales Item: Reduziert nur das Item-Gewicht
     */
    val weightByLocation: StateFlow<Map<Long?, Weight>> =
        combine(itemsWithLocation, potions, magicSignsWithItems, currentDerianDate) { items, pots, signs, currentDate ->
            val currentDays = DerianDateCalculator.parseDateToDays(currentDate) ?: 0
            
            // Finde aktive Gewichtsreduktions-Zauberzeichen
            val activeWeightReductionSigns = signs.filter { signWithItem ->
                val sign = signWithItem.magicSign
                sign.isActivated && 
                !sign.isBotched && 
                sign.effect == MagicSignEffect.WEIGHT_REDUCTION &&
                sign.expiryDate?.let { 
                    DerianDateCalculator.parseDateToDays(it)?.let { expiryDays -> expiryDays >= currentDays } 
                } ?: false
            }
            
            // Map: ItemId -> Reduktion in Unzen (für normale Items)
            val itemReductions = mutableMapOf<Long, Int>()
            // Map: LocationId -> Reduktion in Unzen (für Eigenobjekte, wirkt auf ganze Location)
            val locationReductions = mutableMapOf<Long, Int>()
            
            activeWeightReductionSigns.forEach { signWithItem ->
                val sign = signWithItem.magicSign
                val rkpStar = sign.activationRkpStar ?: 0
                val reductionOunces = rkpStar * 2 * 40 // RkP* × 2 Stein
                
                // Finde das Item mit dem Zauberzeichen
                val itemWithSign = items.find { it.id == sign.itemId }
                
                if (itemWithSign != null) {
                    if (itemWithSign.isSelfItem && itemWithSign.selfItemForLocationId != null) {
                        // Eigenobjekt: Reduziere ganze Location
                        val currentReduction = locationReductions[itemWithSign.selfItemForLocationId] ?: 0
                        locationReductions[itemWithSign.selfItemForLocationId] = currentReduction + reductionOunces
                    } else {
                        // Normales Item: Reduziere nur dieses Item
                        val currentReduction = itemReductions[itemWithSign.id] ?: 0
                        itemReductions[itemWithSign.id] = currentReduction + reductionOunces
                    }
                }
            }
            
            val weights = mutableMapOf<Long?, Int>() // Location ID -> Gewicht in Unzen
            
            // Items (mit individueller Reduktion falls vorhanden)
            items.forEach { item ->
                val currentWeight = weights[item.locationId] ?: 0
                var itemWeight = if (item.isCountable) {
                    item.totalWeight.toOunces()
                } else {
                    item.weight.toOunces()
                }
                
                // Wende individuelle Item-Reduktion an
                val itemReduction = itemReductions[item.id] ?: 0
                if (itemReduction > 0) {
                    // Reduziere Item-Gewicht: Mindestens 1 Stein (40 Unzen) bleibt, aber nie schwerer als Original
                    val minimumOunces = if (itemWeight >= 40) 40 else itemWeight
                    itemWeight = maxOf(itemWeight - itemReduction, minimumOunces)
                }
                
                weights[item.locationId] = currentWeight + itemWeight
            }
            
            // Tränke (je 4 Unzen * Menge)
            pots.forEach { potionWithRecipe ->
                val currentWeight = weights[potionWithRecipe.potion.locationId] ?: 0
                val potionTotalWeight = Weight.POTION.toOunces() * potionWithRecipe.potion.quantity
                weights[potionWithRecipe.potion.locationId] = currentWeight + potionTotalWeight
            }
            
            // Wende Location-weite Reduktionen an (von Eigenobjekten)
            locationReductions.forEach { (locationId, reductionOunces) ->
                if (weights.containsKey(locationId)) {
                    val currentWeight = weights[locationId] ?: 0
                    // Reduziere Location-Gewicht, aber mindestens 1 Stein (40 Unzen) bleibt
                    weights[locationId] = maxOf(currentWeight - reductionOunces, 40)
                }
            }
            
            // Konvertiere zu Weight-Objekten
            weights.mapValues { Weight.fromOunces(it.value) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /**
     * Original-Gewicht pro Location (ohne Gewichtsreduktion durch Zauberzeichen)
     */
    val originalWeightByLocation: StateFlow<Map<Long?, Weight>> =
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
            
            // Journal-Eintrag für hinzugefügtes Item
            val location = locationId?.let { id ->
                repository.getLocationById(id)
            }
            val locationText = location?.name ?: "Ohne Ort"
            
            val itemText = if (isCountable && quantity > 1) {
                "$quantity× $name"
            } else {
                name
            }
            
            repository.logCharacterEvent(
                characterId = characterId,
                category = de.applicatus.app.data.model.character.JournalCategory.INVENTORY_ITEM_ACQUIRED,
                playerMessage = "Gegenstand hinzugefügt: $itemText",
                gmMessage = "Ort: $locationText, Gewicht: ${weight.stone} Stein ${weight.ounces} Unzen"
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
     * Datenklasse für magische Eigenschaften eines Items
     */
    data class ItemMagicProperties(
        val hasMagicSigns: Boolean = false,
        val magicSignCount: Int = 0,
        val hasSpellSlots: Boolean = false,
        val spellSlotCount: Int = 0,
        val slotTypes: List<SlotType> = emptyList()
    ) {
        val hasMagic: Boolean get() = hasMagicSigns || hasSpellSlots
        
        fun getWarningMessage(): String {
            val parts = mutableListOf<String>()
            if (hasMagicSigns) {
                parts.add("$magicSignCount Zauberzeichen")
            }
            if (hasSpellSlots) {
                val slotDescriptions = slotTypes.groupBy { it }.map { (type, slots) ->
                    val typeName = when (type) {
                        SlotType.APPLICATUS -> "Applicatus"
                        SlotType.SPELL_STORAGE -> "Zauberspeicher"
                        SlotType.LONG_DURATION -> "Langwirkender Zauber"
                    }
                    "${slots.size}x $typeName"
                }
                parts.add(slotDescriptions.joinToString(", "))
            }
            return "Dieser Gegenstand hat folgende magische Eigenschaften, die beim Löschen verloren gehen:\n\n${parts.joinToString("\n")}"
        }
    }
    
    /**
     * Prüft, ob ein Item magische Eigenschaften hat (Zauberzeichen oder SpellSlots)
     */
    suspend fun getItemMagicProperties(itemId: Long): ItemMagicProperties {
        val magicSigns = repository.getMagicSignsListForItem(itemId)
        val spellSlots = repository.getSlotsListForItem(itemId)
        
        return ItemMagicProperties(
            hasMagicSigns = magicSigns.isNotEmpty(),
            magicSignCount = magicSigns.size,
            hasSpellSlots = spellSlots.isNotEmpty(),
            spellSlotCount = spellSlots.size,
            slotTypes = spellSlots.map { it.slotType }
        )
    }
    
    /**
     * Löscht ein Item
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            // Hole Location-Name für Journal
            val locationName = item.locationId?.let { locId ->
                repository.getLocationById(locId)?.name
            } ?: "Kein Ort"
            
            repository.deleteItem(item)
            
            // Journal-Eintrag
            repository.logCharacterEvent(
                characterId = characterId,
                category = de.applicatus.app.data.model.character.JournalCategory.INVENTORY_ITEM_REMOVED,
                playerMessage = "Gegenstand gelöscht: ${item.name}",
                gmMessage = "Ort: $locationName, Gewicht: ${item.weight.toDisplayString()}"
            )
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
            // Hole Location und Charakter-Namen für Journal
            val location = repository.getLocationById(locationId)
            val sourceChar = repository.getCharacterById(characterId)
            val targetChar = repository.getCharacterById(targetCharacterId)
            
            if (location != null && sourceChar != null && targetChar != null) {
                // Journal-Eintrag beim Geber
                repository.logCharacterEvent(
                    characterId = characterId,
                    category = de.applicatus.app.data.model.character.JournalCategory.INVENTORY_LOCATION_CREATED,
                    playerMessage = "Ort übergeben: ${location.name} → ${targetChar.name}",
                    gmMessage = ""
                )
                
                // Journal-Eintrag beim Empfänger
                repository.logCharacterEvent(
                    characterId = targetCharacterId,
                    category = de.applicatus.app.data.model.character.JournalCategory.INVENTORY_LOCATION_CREATED,
                    playerMessage = "Ort erhalten: ${location.name} von ${sourceChar.name}",
                    gmMessage = ""
                )
            }
            
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
