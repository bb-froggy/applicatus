package de.applicatus.app.data.repository

import de.applicatus.app.data.InitialSpells
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.dao.CharacterDao
import de.applicatus.app.data.dao.GlobalSettingsDao
import de.applicatus.app.data.dao.GroupDao
import de.applicatus.app.data.dao.ItemDao
import de.applicatus.app.data.dao.LocationDao
import de.applicatus.app.data.dao.PotionDao
import de.applicatus.app.data.dao.RecipeDao
import de.applicatus.app.data.dao.RecipeKnowledgeDao
import de.applicatus.app.data.dao.SpellDao
import de.applicatus.app.data.dao.SpellSlotDao
import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.mergePotion
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.GlobalSettings
import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionWithRecipe
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.RecipeKnowledge
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.model.spell.SpellSlotWithSpell
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class ApplicatusRepository(
    private val spellDao: SpellDao,
    private val characterDao: CharacterDao,
    private val spellSlotDao: SpellSlotDao,
    private val recipeDao: RecipeDao,
    private val potionDao: PotionDao,
    private val globalSettingsDao: GlobalSettingsDao,
    private val recipeKnowledgeDao: RecipeKnowledgeDao,
    private val groupDao: GroupDao,
    private val itemDao: ItemDao,
    private val locationDao: LocationDao
) {
    // Spells
    val allSpells: Flow<List<Spell>> = spellDao.getAllSpells()
    
    suspend fun insertSpell(spell: Spell) = spellDao.insertSpell(spell)
    suspend fun insertSpells(spells: List<Spell>) = spellDao.insertSpells(spells)
    suspend fun deleteSpell(spell: Spell) = spellDao.deleteSpell(spell)
    suspend fun deleteAllSpells() = spellDao.deleteAllSpells()
    suspend fun getSpellById(id: Long) = spellDao.getSpellById(id)
    suspend fun getSpellCount() = spellDao.getSpellCount()
    
    /**
     * Synchronisiert fehlende Zauber aus InitialSpells in die Datenbank.
     * Vergleicht die vorhandenen Zauber mit den Initial-Zaubern und fügt fehlende hinzu.
     * @return Anzahl der neu hinzugefügten Zauber
     */
    suspend fun syncMissingSpells(): Int {
        val existingSpellNames = spellDao.getAllSpellNames().toSet()
        val initialSpells = InitialSpells.getDefaultSpells()
        
        val missingSpells = initialSpells.filter { spell ->
            spell.name !in existingSpellNames
        }
        
        if (missingSpells.isNotEmpty()) {
            insertSpells(missingSpells)
        }
        
        return missingSpells.size
    }
    
    /**
     * Synchronisiert fehlende Rezepte aus InitialRecipes in die Datenbank.
     * Aktualisiert auch quantityProduced-Werte für existierende Rezepte.
     * @return Anzahl der neu hinzugefügten Rezepte
     */
    suspend fun syncMissingRecipes(): Int {
        val initialRecipes = de.applicatus.app.data.InitialRecipes.getDefaultRecipes()
        
        var addedCount = 0
        var updatedCount = 0
        
        initialRecipes.forEach { initialRecipe ->
            val existingRecipe = recipeDao.getRecipeByName(initialRecipe.name)
            
            if (existingRecipe == null) {
                // Rezept existiert nicht -> neu hinzufügen
                recipeDao.insertRecipe(initialRecipe)
                addedCount++
            } else {
                // Rezept existiert -> quantityProduced aktualisieren falls unterschiedlich
                if (existingRecipe.quantityProduced != initialRecipe.quantityProduced) {
                    recipeDao.updateRecipe(existingRecipe.copy(
                        quantityProduced = initialRecipe.quantityProduced
                    ))
                    updatedCount++
                }
            }
        }
        
        // Gib die Anzahl neu hinzugefügter und aktualisierter Rezepte zurück
        return addedCount + updatedCount
    }
    
    // Characters
    val allCharacters: Flow<List<Character>> = characterDao.getAllCharacters()
    
    suspend fun insertCharacter(character: Character): Long = characterDao.insertCharacter(character)
    suspend fun updateCharacter(character: Character) = characterDao.updateCharacter(character)
    suspend fun deleteCharacter(character: Character) = characterDao.deleteCharacter(character)
    suspend fun getCharacterById(id: Long) = characterDao.getCharacterById(id)
    suspend fun getCharacterByGuid(guid: String) = characterDao.getCharacterByGuid(guid)
    fun getCharacterByIdFlow(id: Long) = characterDao.getCharacterByIdFlow(id)
    
    /**
     * Aktualisiert das lastModifiedDate eines Charakters auf die aktuelle Zeit
     */
    private suspend fun touchCharacter(characterId: Long) {
        val character = getCharacterById(characterId)
        character?.let {
            updateCharacter(it.copy(lastModifiedDate = System.currentTimeMillis()))
        }
    }
    
    // Spell Slots
    fun getSlotsByCharacter(characterId: Long): Flow<List<SpellSlot>> = 
        spellSlotDao.getSlotsByCharacter(characterId)
    
    fun getSlotsWithSpellsByCharacter(characterId: Long): Flow<List<SpellSlotWithSpell>> {
        return combine(
            spellSlotDao.getSlotsByCharacter(characterId),
            allSpells
        ) { slots, spells ->
            slots.map { slot ->
                val spell = spells.find { it.id == slot.spellId }
                SpellSlotWithSpell(slot, spell)
            }
        }
    }
    
    suspend fun insertSlot(slot: SpellSlot) {
        spellSlotDao.insertSlot(slot)
        touchCharacter(slot.characterId)
    }
    suspend fun insertSlots(slots: List<SpellSlot>) {
        spellSlotDao.insertSlots(slots)
        slots.firstOrNull()?.characterId?.let { touchCharacter(it) }
    }
    suspend fun updateSlot(slot: SpellSlot) {
        spellSlotDao.updateSlot(slot)
        touchCharacter(slot.characterId)
    }
    suspend fun deleteSlot(slot: SpellSlot) {
        spellSlotDao.deleteSlot(slot)
        touchCharacter(slot.characterId)
    }
    suspend fun getSlotById(id: Long) = spellSlotDao.getSlotById(id)
    
    // Initialize slots for a new character (10 slots)
    suspend fun initializeSlotsForCharacter(characterId: Long) {
        val slots = (0..9).map { slotNumber ->
            SpellSlot(
                characterId = characterId,
                slotNumber = slotNumber,
                spellId = null
            )
        }
        insertSlots(slots)
    }
    
    // Recipes
    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()
    
    suspend fun insertRecipe(recipe: Recipe): Long = recipeDao.insertRecipe(recipe)
    suspend fun insertRecipes(recipes: List<Recipe>) = recipeDao.insertRecipes(recipes)
    suspend fun deleteRecipe(recipe: Recipe) = recipeDao.deleteRecipe(recipe)
    suspend fun getRecipeById(id: Long) = recipeDao.getRecipeById(id)
    suspend fun getRecipeCount() = recipeDao.getRecipeCount()
    
    // Potions
    fun getPotionsForCharacter(characterId: Long): Flow<List<PotionWithRecipe>> =
        potionDao.getPotionsForCharacter(characterId)
    
    suspend fun insertPotion(potion: Potion): Long {
        val id = potionDao.insertPotion(potion)
        touchCharacter(potion.characterId)
        return id
    }
    suspend fun updatePotion(potion: Potion) {
        potionDao.updatePotion(potion)
        touchCharacter(potion.characterId)
    }
    suspend fun deletePotion(potion: Potion) {
        potionDao.deletePotion(potion)
        touchCharacter(potion.characterId)
    }
    suspend fun getPotionById(id: Long) = potionDao.getPotionById(id)
    suspend fun deletePotionsForCharacter(characterId: Long) = potionDao.deletePotionsForCharacter(characterId)
    
    // Global Settings
    val globalSettings: Flow<GlobalSettings?> = globalSettingsDao.getSettings()
    
    suspend fun getGlobalSettingsOnce(): GlobalSettings? = globalSettingsDao.getSettingsOnce()
    
    suspend fun updateGlobalSettings(settings: GlobalSettings) = globalSettingsDao.updateSettings(settings)
    
    suspend fun updateCurrentDerianDate(date: String) = globalSettingsDao.updateCurrentDate(date)
    
    /**
     * Stellt sicher, dass GlobalSettings existieren.
     * Falls nicht, wird ein Standard-Eintrag erstellt.
     */
    suspend fun ensureGlobalSettingsExist() {
        if (getGlobalSettingsOnce() == null) {
            globalSettingsDao.insertSettings(GlobalSettings())
        }
    }
    
    // Recipe Knowledge
    fun getRecipeKnowledgeForCharacter(characterId: Long): Flow<List<RecipeKnowledge>> =
        recipeKnowledgeDao.getKnowledgeForCharacter(characterId)
    
    suspend fun getRecipeKnowledge(characterId: Long, recipeId: Long): RecipeKnowledge? =
        recipeKnowledgeDao.getKnowledge(characterId, recipeId)
    
    fun getRecipeKnowledgeByLevel(characterId: Long, level: RecipeKnowledgeLevel): Flow<List<RecipeKnowledge>> =
        recipeKnowledgeDao.getKnowledgeByLevel(characterId, level)
    
    suspend fun insertRecipeKnowledge(knowledge: RecipeKnowledge) =
        recipeKnowledgeDao.insertKnowledge(knowledge)
    
    suspend fun updateRecipeKnowledge(knowledge: RecipeKnowledge) =
        recipeKnowledgeDao.updateKnowledge(knowledge)
    
    suspend fun deleteRecipeKnowledge(knowledge: RecipeKnowledge) =
        recipeKnowledgeDao.deleteKnowledge(knowledge)
    
    suspend fun updateRecipeKnowledgeLevel(characterId: Long, recipeId: Long, level: RecipeKnowledgeLevel) =
        recipeKnowledgeDao.updateKnowledgeLevel(characterId, recipeId, level)

    suspend fun deleteRecipeKnowledgeForCharacter(characterId: Long) =
        recipeKnowledgeDao.deleteKnowledgeForCharacter(characterId)
    
    // Groups
    val allGroups: Flow<List<Group>> = groupDao.getAllGroups()
    
    fun getGroupById(groupId: Long): Flow<Group?> = groupDao.getGroupById(groupId)
    
    suspend fun getGroupByIdOnce(groupId: Long): Group? = groupDao.getGroupByIdOnce(groupId)
    
    /**
     * Ermittelt die Gruppe eines Charakters.
     * Gibt die Gruppe zurück, zu der der Charakter gehört, oder die Standard-Gruppe.
     */
    fun getGroupForCharacter(characterId: Long): Flow<Group?> = flow {
        val character = characterDao.getCharacterById(characterId)
        if (character != null) {
            val groupId = character.groupId ?: ensureDefaultGroupExists()
            groupDao.getGroupById(groupId).collect { group ->
                emit(group)
            }
        } else {
            emit(null)
        }
    }
    
    suspend fun insertGroup(group: Group): Long = groupDao.insertGroup(group)
    
    suspend fun updateGroup(group: Group) = groupDao.updateGroup(group)
    
    suspend fun deleteGroup(groupId: Long) = groupDao.deleteGroup(groupId)
    
    suspend fun updateGroupDerianDate(groupId: Long, date: String) = groupDao.updateCurrentDate(groupId, date)
    
    suspend fun moveCharacterToGroup(characterId: Long, targetGroupId: Long) {
        val character = characterDao.getCharacterById(characterId)
        character?.let {
            characterDao.updateCharacter(it.copy(groupId = targetGroupId))
        }
    }
    
    /**
     * Stellt sicher, dass eine Standard-Gruppe existiert.
     * Falls keine Gruppe vorhanden ist, wird "Meine Gruppe" erstellt.
     * @return ID der Standard-Gruppe
     */
    suspend fun ensureDefaultGroupExists(): Long {
        val groups = groupDao.getAllGroups()
        // Prüfe ob bereits Gruppen existieren
        var defaultGroupId: Long? = null
        groups.collect { list ->
            if (list.isEmpty()) {
                // Erstelle Standard-Gruppe
                defaultGroupId = groupDao.insertGroup(Group(name = "Meine Gruppe"))
            } else {
                // Verwende erste Gruppe als Standard
                defaultGroupId = list.first().id
            }
        }
        return defaultGroupId ?: 1L
    }
    
    // Locations
    fun getLocationsForCharacter(characterId: Long): Flow<List<Location>> =
        locationDao.getLocationsForCharacter(characterId)
    
    suspend fun getLocationById(locationId: Long): Location? =
        locationDao.getLocationById(locationId)
    
    suspend fun insertLocation(location: Location): Long =
        locationDao.insert(location)
    
    suspend fun updateLocation(location: Location) =
        locationDao.update(location)
    
    suspend fun updateLocationIsCarried(locationId: Long, isCarried: Boolean) =
        locationDao.updateIsCarried(locationId, isCarried)
    
    suspend fun deleteLocation(location: Location) =
        locationDao.delete(location)
    
    suspend fun createDefaultLocationsForCharacter(characterId: Long) =
        locationDao.createDefaultLocations(characterId)
    
    // Items
    fun getItemsForCharacter(characterId: Long): Flow<List<Item>> =
        itemDao.getItemsForCharacter(characterId)
    
    fun getItemsWithLocationForCharacter(characterId: Long): Flow<List<ItemWithLocation>> =
        itemDao.getItemsWithLocationForCharacter(characterId)
    
    fun getItemsForLocation(locationId: Long): Flow<List<Item>> =
        itemDao.getItemsForLocation(locationId)
    
    suspend fun getItemById(itemId: Long): Item? =
        itemDao.getItemById(itemId)
    
    suspend fun insertItem(item: Item): Long {
        val id = itemDao.insert(item)
        touchCharacter(item.characterId)
        return id
    }
    
    suspend fun updateItem(item: Item) {
        itemDao.update(item)
        touchCharacter(item.characterId)
    }
    
    suspend fun updatePurseAmount(itemId: Long, kreuzerAmount: Int) {
        val item = itemDao.getItemById(itemId)
        if (item != null && item.isPurse) {
            // Berechne automatisch das Gewicht basierend auf den Münzen
            val currency = de.applicatus.app.data.model.inventory.Currency.fromKreuzer(kreuzerAmount)
            val weight = currency.toWeight()
            itemDao.update(item.copy(kreuzerAmount = kreuzerAmount, weight = weight))
            touchCharacter(item.characterId)
        }
    }
    
    suspend fun updateItemQuantity(itemId: Long, quantity: Int) {
        val item = itemDao.getItemById(itemId)
        if (item != null && item.isCountable && quantity >= 1) {
            itemDao.update(item.copy(quantity = quantity))
            touchCharacter(item.characterId)
        }
    }
    
    /**
     * Teilt einen zählbaren Gegenstand und verschiebt einen Teil zu einer anderen Location.
     * Wenn in der Ziel-Location bereits ein identischer Gegenstand existiert, werden sie zusammengeführt.
     * @param itemId Die ID des zu teilenden Gegenstands
     * @param quantityToMove Die Anzahl, die verschoben werden soll
     * @param targetLocationId Die Ziel-Location
     */
    suspend fun splitAndMoveItem(itemId: Long, quantityToMove: Int, targetLocationId: Long?) {
        val sourceItem = itemDao.getItemById(itemId) ?: return
        
        // Validierung
        if (!sourceItem.isCountable || quantityToMove <= 0 || quantityToMove >= sourceItem.quantity) {
            // Wenn alle Gegenstände verschoben werden, einfach die Location ändern
            if (quantityToMove == sourceItem.quantity) {
                itemDao.update(sourceItem.copy(locationId = targetLocationId))
                mergeIdenticalItemsAtLocation(targetLocationId, sourceItem.characterId)
            }
            return
        }
        
        // Reduziere die Quell-Menge
        val newSourceQuantity = sourceItem.quantity - quantityToMove
        itemDao.update(sourceItem.copy(quantity = newSourceQuantity))
        
        // Prüfe, ob in der Ziel-Location bereits ein identischer Gegenstand existiert
        val targetItems = itemDao.getItemsListForCharacter(sourceItem.characterId)
        val identicalItem = targetItems.find { 
            it.locationId == targetLocationId &&
            it.name == sourceItem.name &&
            it.isCountable &&
            !it.isPurse &&
            it.id != sourceItem.id &&
            it.weight == sourceItem.weight
        }
        
        if (identicalItem != null) {
            // Füge zur bestehenden Menge hinzu
            val newTargetQuantity = identicalItem.quantity + quantityToMove
            itemDao.update(identicalItem.copy(quantity = newTargetQuantity))
        } else {
            // Erstelle einen neuen Gegenstand in der Ziel-Location
            val newItem = sourceItem.copy(
                id = 0, // Neue ID wird automatisch vergeben
                locationId = targetLocationId,
                quantity = quantityToMove
            )
            itemDao.insert(newItem)
        }
    }
    
    /**
     * Führt identische zählbare Gegenstände an einer Location zusammen
     */
    private suspend fun mergeIdenticalItemsAtLocation(locationId: Long?, characterId: Long) {
        val items = itemDao.getItemsListForCharacter(characterId)
            .filter { it.locationId == locationId && it.isCountable && !it.isPurse }
            .groupBy { it.name }
        
        items.forEach { (_, itemGroup) ->
            if (itemGroup.size > 1) {
                // Verwende das erste Item als Basis
                val baseItem = itemGroup.first()
                val totalQuantity = itemGroup.sumOf { it.quantity }
                
                // Prüfe, ob alle Items das gleiche Gewicht haben
                val sameWeight = itemGroup.all { it.weight == baseItem.weight }
                
                if (sameWeight) {
                    // Update base item with total quantity
                    itemDao.update(baseItem.copy(quantity = totalQuantity))
                    
                    // Lösche die anderen Items
                    itemGroup.drop(1).forEach { itemDao.delete(it) }
                }
            }
        }
    }
    
    /**
     * Teilt einen Trank und verschiebt einen Teil zu einer anderen Location.
     * Wenn in der Ziel-Location bereits ein identischer Trank existiert, werden sie zusammengeführt.
     * Tränke sind identisch wenn sie: gleichen recipeId, actualQuality, expiryDate, appearance haben
     * @param potionId Die ID des zu teilenden Tranks
     * @param quantityToMove Die Anzahl, die verschoben werden soll
     * @param targetLocationId Die Ziel-Location
     */
    suspend fun splitAndMovePotion(potionId: Long, quantityToMove: Int, targetLocationId: Long?) {
        val sourcePotion = potionDao.getPotionById(potionId) ?: return
        
        // Validierung
        if (quantityToMove <= 0 || quantityToMove >= sourcePotion.quantity) {
            // Wenn alle Tränke verschoben werden, einfach die Location ändern
            if (quantityToMove == sourcePotion.quantity) {
                potionDao.updatePotion(sourcePotion.copy(locationId = targetLocationId))
                mergeIdenticalPotionsAtLocation(targetLocationId, sourcePotion.characterId)
            }
            return
        }
        
        // Reduziere die Quell-Menge
        val newSourceQuantity = sourcePotion.quantity - quantityToMove
        potionDao.updatePotion(sourcePotion.copy(quantity = newSourceQuantity))
        
        // Prüfe, ob in der Ziel-Location bereits ein identischer Trank existiert
        val targetPotions = potionDao.getPotionsListForCharacter(sourcePotion.characterId)
        val identicalPotion = targetPotions.find { 
            it.locationId == targetLocationId &&
            it.recipeId == sourcePotion.recipeId &&
            it.actualQuality == sourcePotion.actualQuality &&
            it.expiryDate == sourcePotion.expiryDate &&
            it.appearance == sourcePotion.appearance &&
            it.nameKnown == sourcePotion.nameKnown &&
            it.categoryKnown == sourcePotion.categoryKnown &&
            it.shelfLifeKnown == sourcePotion.shelfLifeKnown &&
            it.id != sourcePotion.id
        }
        
        if (identicalPotion != null) {
            // Füge zur bestehenden Menge hinzu
            val newTargetQuantity = identicalPotion.quantity + quantityToMove
            potionDao.updatePotion(identicalPotion.copy(quantity = newTargetQuantity))
        } else {
            // Erstelle einen neuen Trank in der Ziel-Location
            val newPotion = sourcePotion.copy(
                id = 0, // Neue ID wird automatisch vergeben
                locationId = targetLocationId,
                quantity = quantityToMove
            )
            potionDao.insertPotion(newPotion)
        }
    }
    
    /**
     * Führt identische Tränke an einer Location zusammen
     */
    private suspend fun mergeIdenticalPotionsAtLocation(locationId: Long?, characterId: Long) {
        val potions = potionDao.getPotionsListForCharacter(characterId)
            .filter { it.locationId == locationId }
            .groupBy { 
                // Group by all properties that make potions identical
                Triple(it.recipeId, it.actualQuality, Triple(it.expiryDate, it.appearance, Triple(it.nameKnown, it.categoryKnown, it.shelfLifeKnown)))
            }
        
        potions.forEach { (_, potionGroup) ->
            if (potionGroup.size > 1) {
                // Verwende den ersten Trank als Basis
                val basePotion = potionGroup.first()
                val totalQuantity = potionGroup.sumOf { it.quantity }
                
                // Update base potion with total quantity
                potionDao.updatePotion(basePotion.copy(quantity = totalQuantity))
                
                // Lösche die anderen Tränke
                potionGroup.drop(1).forEach { potionDao.deletePotion(it) }
            }
        }
    }
    
    suspend fun deleteItem(item: Item) {
        itemDao.delete(item)
        touchCharacter(item.characterId)
    }
    
    // Character by Group
    fun getCharactersByGroupId(groupId: Long): Flow<List<Character>> =
        characterDao.getCharactersByGroupId(groupId)
    
    suspend fun getCharactersByGroupOnce(groupId: Long): List<Character> =
        characterDao.getCharactersByGroupIdOnce(groupId)
    
    suspend fun getSlotsWithSpellsByCharacterOnce(characterId: Long): List<de.applicatus.app.data.model.spell.SpellSlotWithSpell> {
        val slots = spellSlotDao.getSlotsByCharacterOnce(characterId)
        val spells = spellDao.getAllSpellsOnce()
        return slots.map { slot ->
            val spell = spells.find { it.id == slot.spellId }
            de.applicatus.app.data.model.spell.SpellSlotWithSpell(slot, spell)
        }
    }
    
    /**
     * Überträgt eine Location mit allen Items und Tränken zu einem anderen Charakter.
     * Die Location wird beim Zielcharakter neu erstellt (nicht verschoben).
     * @param locationId ID der zu übertragenden Location
     * @param targetCharacterId ID des Zielcharakters
     */
    suspend fun transferLocationToCharacter(locationId: Long, targetCharacterId: Long) {
        val location = getLocationById(locationId) ?: return
        
        // Erstelle neue Location beim Zielcharakter
        val newLocationId = insertLocation(
            location.copy(
                id = 0, // Neue ID generieren lassen
                characterId = targetCharacterId,
                isDefault = false, // Übertragene Locations sind nie Default
                sortOrder = 0 // Wird automatisch ans Ende sortiert
            )
        )
        
        // Hole alle Items und Tränke SYNCHRON mit .first()
        val items = itemDao.getItemsForLocation(locationId).first()
        val potions = potionDao.getPotionsForLocation(locationId).first()
        
        // Übertrage alle Items
        items.forEach { item ->
            // Erstelle Kopie des Items beim Zielcharakter
            insertItem(
                item.copy(
                    id = 0, // Neue ID generieren lassen
                    characterId = targetCharacterId,
                    locationId = newLocationId
                )
            )
            // Lösche Original-Item
            deleteItem(item)
        }
        
        // Übertrage alle Tränke
        potions.forEach { potionWithRecipe ->
            // Erstelle Kopie des Tranks beim Zielcharakter
            insertPotion(
                potionWithRecipe.potion.copy(
                    id = 0, // Neue ID generieren lassen
                    characterId = targetCharacterId,
                    locationId = newLocationId
                )
            )
            // Lösche Original-Trank
            deletePotion(potionWithRecipe.potion)
        }
        
        // Lösche die Original-Location
        deleteLocation(location)
    }
    
    /**
     * Wendet einen Charakter-Snapshot aus einer Sync-Session an.
     * Diese Funktion ist speziell für Live-Sync gedacht und überschreibt ohne UI-Warnungen.
     * 
     * Verhaltensweisen:
     * - Sucht Charakter per GUID
     * - Wenn vorhanden: Überschreibt komplett (Charakter, Slots, Tränke, Items, Locations)
     * - Wenn nicht vorhanden: Optional anlegen (nur wenn allowCreateNew = true)
     * - lastModifiedDate wird auf exportTimestamp gesetzt
     * - Gruppe bleibt beim Überschreiben erhalten
     * 
     * @param snapshot Der importierte Snapshot (CharacterExportDto)
     * @param allowCreateNew Ob neue Charaktere angelegt werden dürfen (default: false)
     * @return Result mit Character-ID oder Fehler
     */
    suspend fun applySnapshotFromSync(
        snapshot: CharacterExportDto,
        allowCreateNew: Boolean = false
    ): Result<Long> = kotlin.runCatching {
        // Versionscheck
        val compatibilityResult = DataModelVersion.checkCompatibility(snapshot.version)
        if (!compatibilityResult.first) {
            throw IllegalStateException(compatibilityResult.second ?: "Inkompatible Version")
        }
        
        val existingCharacter = getCharacterByGuid(snapshot.character.guid)
        
        val characterId = if (existingCharacter != null) {
            // Bestehenden Charakter überschreiben
            val updatedCharacter = snapshot.character.toCharacter().copy(
                id = existingCharacter.id,
                guid = existingCharacter.guid,
                groupId = existingCharacter.groupId,  // Gruppe wird NICHT überschrieben
                lastModifiedDate = snapshot.exportTimestamp
            )
            updateCharacter(updatedCharacter)
            
            // Alte Slots, Items und nicht-Standard-Locations löschen
            val oldSlots = getSlotsByCharacter(existingCharacter.id).first()
            oldSlots.forEach { deleteSlot(it) }
            
            val oldItems = getItemsForCharacter(existingCharacter.id).first()
            oldItems.forEach { deleteItem(it) }
            
            val oldLocations = getLocationsForCharacter(existingCharacter.id).first()
            oldLocations.filter { !it.isDefault }.forEach { deleteLocation(it) }
            
            existingCharacter.id
        } else if (allowCreateNew) {
            // Neuen Charakter erstellen
            // GroupId über Gruppennamen auflösen, Fallback auf "Unbekannte Gruppe"
            val allGroups = this.allGroups.first()
            val resolvedGroupId = snapshot.character.groupName?.let { groupName ->
                allGroups.find { it.name == groupName }?.id
                    ?: allGroups.find { it.name == "Unbekannte Gruppe" }?.id
            }
            
            val newCharacter = snapshot.character.toCharacter().copy(
                groupId = resolvedGroupId,
                lastModifiedDate = snapshot.exportTimestamp
            )
            val newCharacterId = insertCharacter(newCharacter)
            createDefaultLocationsForCharacter(newCharacterId)
            newCharacterId
        } else {
            throw IllegalStateException("Charakter mit GUID ${snapshot.character.guid} existiert nicht und allowCreateNew=false")
        }
        
        // Slots einfügen
        val allSpells = allSpells.first()
        val newSlots = snapshot.spellSlots.map { slotDto ->
            val resolvedSpellId = slotDto.spellName?.let { spellName ->
                allSpells.find { it.name == spellName }?.id
            }
            slotDto.toSpellSlot(characterId, resolvedSpellId)
        }
        insertSlots(newSlots)
        
        // Tränke importieren
        val allRecipes = allRecipes.first()
        val recipesById = allRecipes.associateBy { it.id }
        val recipesByName = allRecipes.associateBy { it.name }
        
        val existingPotions = getPotionsForCharacter(characterId).first()
        val existingPotionsByGuid = existingPotions.associate { it.potion.guid to it.potion }
        
        // Rezeptwissen zurücksetzen
        deleteRecipeKnowledgeForCharacter(characterId)
        
        snapshot.potions.forEach { potionDto ->
            val resolvedRecipeId = potionDto.recipeId?.let { recipesById[it]?.id }
                ?: potionDto.recipeName?.let { recipesByName[it]?.id }
            if (resolvedRecipeId != null) {
                val importedPotion = potionDto.toPotion(characterId, resolvedRecipeId)
                val existingPotion = existingPotionsByGuid[potionDto.guid]
                
                if (existingPotion != null) {
                    // Merge mit besserem Analyse-Ergebnis
                    val mergedPotion = mergePotion(existingPotion, importedPotion)
                    updatePotion(mergedPotion)
                } else {
                    insertPotion(importedPotion)
                }
            }
        }
        
        // Rezeptwissen importieren
        snapshot.recipeKnowledge.forEach { knowledgeDto ->
            val resolvedRecipeId = knowledgeDto.recipeId?.let { recipesById[it]?.id }
                ?: knowledgeDto.recipeName?.let { recipesByName[it]?.id }
            if (resolvedRecipeId != null) {
                val recipeKnowledge = knowledgeDto.toModel(characterId, resolvedRecipeId)
                insertRecipeKnowledge(recipeKnowledge)
            }
        }
        
        // Locations importieren
        snapshot.locations.forEach { locationDto ->
            val location = locationDto.toLocation(characterId)
            insertLocation(location)
        }
        
        // Items importieren - auflöse locationName zu locationId
        val locationsByName = getLocationsForCharacter(characterId).first().associateBy { it.name }
        snapshot.items.forEach { itemDto ->
            val resolvedLocationId = itemDto.locationName?.let { locationsByName[it]?.id }
            if (resolvedLocationId != null) {
                val item = Item(
                    id = 0,
                    characterId = characterId,
                    name = itemDto.name,
                    weight = Weight(itemDto.weightStone, itemDto.weightOunces),
                    locationId = resolvedLocationId,
                    sortOrder = itemDto.sortOrder,
                    isPurse = itemDto.isPurse,
                    kreuzerAmount = itemDto.kreuzerAmount,
                    isCountable = itemDto.isCountable,
                    quantity = itemDto.quantity
                )
                insertItem(item)
            }
        }
        
        characterId
    }
}
