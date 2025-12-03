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
    
    /** Nur eigene Zauberzeichen: creatorGuid == characterGuid oder creatorGuid ist null (alte Daten) */
    @Query("""
        SELECT * FROM magic_signs 
        WHERE characterId = :characterId 
        AND (creatorGuid = :characterGuid OR creatorGuid IS NULL)
        ORDER BY name
    """)
    fun getOwnMagicSignsForCharacter(characterId: Long, characterGuid: String): Flow<List<MagicSign>>
    
    /** Fremde Zauberzeichen: creatorGuid != characterGuid und creatorGuid ist nicht null */
    @Query("""
        SELECT * FROM magic_signs 
        WHERE characterId = :characterId 
        AND creatorGuid IS NOT NULL 
        AND creatorGuid != :characterGuid
        ORDER BY name
    """)
    fun getForeignMagicSignsForCharacter(characterId: Long, characterGuid: String): Flow<List<MagicSign>>
    
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
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(magicSigns: List<MagicSign>)
    
    @Update
    suspend fun update(magicSign: MagicSign)
    
    @Delete
    suspend fun delete(magicSign: MagicSign)
    
    @Query("DELETE FROM magic_signs WHERE characterId = :characterId")
    suspend fun deleteAllForCharacter(characterId: Long)
    
    @Query("DELETE FROM magic_signs WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: Long)
}
