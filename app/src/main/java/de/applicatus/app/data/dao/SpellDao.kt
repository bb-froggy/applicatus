package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.spell.Spell
import kotlinx.coroutines.flow.Flow

@Dao
interface SpellDao {
    @Query("SELECT * FROM spells ORDER BY name ASC")
    fun getAllSpells(): Flow<List<Spell>>
    
    @Query("SELECT * FROM spells ORDER BY name ASC")
    suspend fun getAllSpellsOnce(): List<Spell>
    
    @Query("SELECT * FROM spells WHERE id = :id")
    suspend fun getSpellById(id: Long): Spell?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpell(spell: Spell): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpells(spells: List<Spell>)
    
    @Delete
    suspend fun deleteSpell(spell: Spell)
    
    @Query("DELETE FROM spells")
    suspend fun deleteAllSpells()
    
    @Query("SELECT COUNT(*) FROM spells")
    suspend fun getSpellCount(): Int
    
    @Query("SELECT name FROM spells")
    suspend fun getAllSpellNames(): List<String>
}
