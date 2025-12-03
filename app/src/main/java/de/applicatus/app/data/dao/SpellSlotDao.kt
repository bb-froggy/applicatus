package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.spell.SpellSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface SpellSlotDao {
    @Query("SELECT * FROM spell_slots WHERE characterId = :characterId ORDER BY slotNumber ASC")
    fun getSlotsByCharacter(characterId: Long): Flow<List<SpellSlot>>
    
    @Query("SELECT * FROM spell_slots WHERE characterId = :characterId ORDER BY slotNumber ASC")
    suspend fun getSlotsByCharacterOnce(characterId: Long): List<SpellSlot>
    
    /** Nur eigene Slots: creatorGuid == characterGuid oder creatorGuid ist null (alte Daten) */
    @Query("""
        SELECT * FROM spell_slots 
        WHERE characterId = :characterId 
        AND (creatorGuid = :characterGuid OR creatorGuid IS NULL)
        ORDER BY slotNumber ASC
    """)
    fun getOwnSlotsByCharacter(characterId: Long, characterGuid: String): Flow<List<SpellSlot>>
    
    /** Fremde Slots: creatorGuid != characterGuid und creatorGuid ist nicht null */
    @Query("""
        SELECT * FROM spell_slots 
        WHERE characterId = :characterId 
        AND creatorGuid IS NOT NULL 
        AND creatorGuid != :characterGuid
        ORDER BY slotNumber ASC
    """)
    fun getForeignSlotsByCharacter(characterId: Long, characterGuid: String): Flow<List<SpellSlot>>
    
    @Query("SELECT * FROM spell_slots WHERE id = :id")
    suspend fun getSlotById(id: Long): SpellSlot?
    
    @Query("SELECT * FROM spell_slots WHERE itemId = :itemId ORDER BY slotNumber ASC")
    fun getSlotsByItem(itemId: Long): Flow<List<SpellSlot>>
    
    @Query("SELECT * FROM spell_slots WHERE itemId = :itemId ORDER BY slotNumber ASC")
    suspend fun getSlotsByItemOnce(itemId: Long): List<SpellSlot>
    
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
