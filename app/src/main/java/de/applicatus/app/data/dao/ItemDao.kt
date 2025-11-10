package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE characterId = :characterId ORDER BY locationId, sortOrder, name")
    fun getItemsForCharacter(characterId: Long): Flow<List<Item>>
    
    @Query("SELECT * FROM items WHERE locationId = :locationId ORDER BY sortOrder, name")
    fun getItemsForLocation(locationId: Long): Flow<List<Item>>
    
    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): Item?
    
    @Transaction
    @Query("""
        SELECT items.id, items.characterId, items.locationId, items.name, 
               items.stone, items.ounces, items.sortOrder,
               locations.name as locationName 
        FROM items 
        LEFT JOIN locations ON items.locationId = locations.id 
        WHERE items.characterId = :characterId 
        ORDER BY locations.sortOrder, locations.name, items.sortOrder, items.name
    """)
    fun getItemsWithLocationForCharacter(characterId: Long): Flow<List<ItemWithLocation>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long
    
    @Update
    suspend fun update(item: Item)
    
    @Delete
    suspend fun delete(item: Item)
    
    @Query("DELETE FROM items WHERE characterId = :characterId")
    suspend fun deleteAllForCharacter(characterId: Long)
}
