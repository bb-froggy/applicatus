package de.applicatus.app.data.repository

import de.applicatus.app.data.dao.CharacterDao
import de.applicatus.app.data.dao.SpellDao
import de.applicatus.app.data.dao.SpellSlotDao
import de.applicatus.app.data.model.Character
import de.applicatus.app.data.model.Spell
import de.applicatus.app.data.model.SpellSlot
import de.applicatus.app.data.model.SpellSlotWithSpell
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ApplicatusRepository(
    private val spellDao: SpellDao,
    private val characterDao: CharacterDao,
    private val spellSlotDao: SpellSlotDao
) {
    // Spells
    val allSpells: Flow<List<Spell>> = spellDao.getAllSpells()
    
    suspend fun insertSpell(spell: Spell) = spellDao.insertSpell(spell)
    suspend fun insertSpells(spells: List<Spell>) = spellDao.insertSpells(spells)
    suspend fun deleteSpell(spell: Spell) = spellDao.deleteSpell(spell)
    suspend fun deleteAllSpells() = spellDao.deleteAllSpells()
    suspend fun getSpellById(id: Long) = spellDao.getSpellById(id)
    suspend fun getSpellCount() = spellDao.getSpellCount()
    
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
}
