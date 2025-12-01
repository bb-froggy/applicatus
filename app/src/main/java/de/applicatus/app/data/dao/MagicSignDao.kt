package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.magicsign.MagicSign
import kotlinx.coroutines.flow.Flow

@Dao
interface MagicSignDao {
    
    @Query("SELECT * FROM magic_signs WHERE characterId = :characterId ORDER BY name")
    fun getMagicSignsForCharacter(characterId: Long): Flow<List<MagicSign>>
    
    @Query("SELECT * FROM magic_signs WHERE characterId = :characterId ORDER BY name")
    suspend fun getMagicSignsListForCharacter(characterId: Long): List<MagicSign>
    
    @Query("SELECT * FROM magic_signs WHERE itemId = :itemId ORDER BY name")
    fun getMagicSignsForItem(itemId: Long): Flow<List<MagicSign>>
    
    @Query("SELECT * FROM magic_signs WHERE itemId = :itemId ORDER BY name")
    suspend fun getMagicSignsListForItem(itemId: Long): List<MagicSign>
    
    @Query("SELECT * FROM magic_signs WHERE id = :signId")
    suspend fun getMagicSignById(signId: Long): MagicSign?
    
    @Query("SELECT * FROM magic_signs WHERE guid = :guid")
    suspend fun getMagicSignByGuid(guid: String): MagicSign?
    
    @Query("""
        SELECT * FROM magic_signs 
        WHERE characterId = :characterId 
        AND isActivated = 1 
        AND isBotched = 0
        ORDER BY name
    """)
    fun getActiveMagicSignsForCharacter(characterId: Long): Flow<List<MagicSign>>
    
    @Query("""
        SELECT * FROM magic_signs 
        WHERE characterId = :characterId 
        AND isActivated = 1 
        AND isBotched = 0
        ORDER BY name
    """)
    suspend fun getActiveMagicSignsListForCharacter(characterId: Long): List<MagicSign>
    
    @Query("""
        SELECT * FROM magic_signs 
        WHERE itemId = :itemId 
        AND isActivated = 1 
        AND isBotched = 0
        ORDER BY name
    """)
    suspend fun getActiveMagicSignsForItem(itemId: Long): List<MagicSign>
    
    @Query("""
        SELECT ms.* FROM magic_signs ms
        INNER JOIN items i ON ms.itemId = i.id
        WHERE i.locationId = :locationId 
        AND ms.isActivated = 1 
        AND ms.isBotched = 0
        ORDER BY ms.name
    """)
    suspend fun getActiveMagicSignsForLocation(locationId: Long): List<MagicSign>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(magicSign: MagicSign): Long
    
    @Update
    suspend fun update(magicSign: MagicSign)
    
    @Delete
    suspend fun delete(magicSign: MagicSign)
    
    @Query("DELETE FROM magic_signs WHERE characterId = :characterId")
    suspend fun deleteAllForCharacter(characterId: Long)
    
    @Query("DELETE FROM magic_signs WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: Long)
}
