package de.applicatus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionWithRecipe
import kotlinx.coroutines.flow.Flow

@Dao
interface PotionDao {
    @Transaction
    @Query("SELECT * FROM potions WHERE characterId = :characterId ORDER BY expiryDate ASC")
    fun getPotionsForCharacter(characterId: Long): Flow<List<PotionWithRecipe>>
    
    @Transaction
    @Query("SELECT * FROM potions WHERE locationId = :locationId ORDER BY expiryDate ASC")
    fun getPotionsForLocation(locationId: Long): Flow<List<PotionWithRecipe>>
    
    @Query("SELECT * FROM potions WHERE id = :id")
    suspend fun getPotionById(id: Long): Potion?
    
    @Query("SELECT * FROM potions WHERE characterId = :characterId")
    suspend fun getPotionsListForCharacter(characterId: Long): List<Potion>
    
    @Insert
    suspend fun insertPotion(potion: Potion): Long
    
    @Update
    suspend fun updatePotion(potion: Potion)
    
    @Delete
    suspend fun deletePotion(potion: Potion)
    
    @Query("DELETE FROM potions WHERE characterId = :characterId")
    suspend fun deletePotionsForCharacter(characterId: Long)
}
