package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.character.Character
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY name ASC")
    fun getAllCharacters(): Flow<List<Character>>
    
    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: Long): Character?
    
    @Query("SELECT * FROM characters WHERE id = :id")
    fun getCharacterByIdFlow(id: Long): Flow<Character?>
    
    @Query("SELECT * FROM characters WHERE groupId = :groupId ORDER BY name ASC")
    fun getCharactersByGroupId(groupId: Long): Flow<List<Character>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: Character): Long
    
    @Update
    suspend fun updateCharacter(character: Character)
    
    @Delete
    suspend fun deleteCharacter(character: Character)
}
