package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.spell.SpellSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface SpellSlotDao {
    @Query("SELECT * FROM spell_slots WHERE characterId = :characterId ORDER BY slotNumber ASC")
    fun getSlotsByCharacter(characterId: Long): Flow<List<SpellSlot>>
    
    @Query("SELECT * FROM spell_slots WHERE id = :id")
    suspend fun getSlotById(id: Long): SpellSlot?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: SpellSlot): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<SpellSlot>)
    
    @Update
    suspend fun updateSlot(slot: SpellSlot)
    
    @Delete
    suspend fun deleteSlot(slot: SpellSlot)
    
    @Query("DELETE FROM spell_slots WHERE characterId = :characterId")
    suspend fun deleteSlotsByCharacter(characterId: Long)
}
