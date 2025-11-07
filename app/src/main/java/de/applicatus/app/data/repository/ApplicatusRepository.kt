package de.applicatus.app.data.repository

import de.applicatus.app.data.InitialSpells
import de.applicatus.app.data.dao.CharacterDao
import de.applicatus.app.data.dao.GlobalSettingsDao
import de.applicatus.app.data.dao.PotionDao
import de.applicatus.app.data.dao.RecipeDao
import de.applicatus.app.data.dao.RecipeKnowledgeDao
import de.applicatus.app.data.dao.SpellDao
import de.applicatus.app.data.dao.SpellSlotDao
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.GlobalSettings
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

class ApplicatusRepository(
    private val spellDao: SpellDao,
    private val characterDao: CharacterDao,
    private val spellSlotDao: SpellSlotDao,
    private val recipeDao: RecipeDao,
    private val potionDao: PotionDao,
    private val globalSettingsDao: GlobalSettingsDao,
    private val recipeKnowledgeDao: RecipeKnowledgeDao
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
    
    // Characters
    val allCharacters: Flow<List<Character>> = characterDao.getAllCharacters()
    
    suspend fun insertCharacter(character: Character): Long = characterDao.insertCharacter(character)
    suspend fun updateCharacter(character: Character) = characterDao.updateCharacter(character)
    suspend fun deleteCharacter(character: Character) = characterDao.deleteCharacter(character)
    suspend fun getCharacterById(id: Long) = characterDao.getCharacterById(id)
    fun getCharacterByIdFlow(id: Long) = characterDao.getCharacterByIdFlow(id)
    
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
    
    suspend fun insertSlot(slot: SpellSlot) = spellSlotDao.insertSlot(slot)
    suspend fun insertSlots(slots: List<SpellSlot>) = spellSlotDao.insertSlots(slots)
    suspend fun updateSlot(slot: SpellSlot) = spellSlotDao.updateSlot(slot)
    suspend fun deleteSlot(slot: SpellSlot) = spellSlotDao.deleteSlot(slot)
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
    
    suspend fun insertPotion(potion: Potion): Long = potionDao.insertPotion(potion)
    suspend fun updatePotion(potion: Potion) = potionDao.updatePotion(potion)
    suspend fun deletePotion(potion: Potion) = potionDao.deletePotion(potion)
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
}
