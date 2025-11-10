package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.inventory.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE characterId = :characterId ORDER BY sortOrder, name")
    fun getLocationsForCharacter(characterId: Long): Flow<List<Location>>
    
    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: Long): Location?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: Location): Long
    
    @Update
    suspend fun update(location: Location)
    
    @Delete
    suspend fun delete(location: Location)
    
    @Query("DELETE FROM locations WHERE characterId = :characterId AND isDefault = 0")
    suspend fun deleteNonDefaultLocations(characterId: Long)
    
    /**
     * Erstellt die Standard-Orte für einen Charakter (Am Körper, Rucksack)
     */
    suspend fun createDefaultLocations(characterId: Long) {
        insert(Location(
            characterId = characterId,
            name = "Am Körper",
            isDefault = true,
            sortOrder = 0
        ))
        insert(Location(
            characterId = characterId,
            name = "Rucksack",
            isDefault = true,
            sortOrder = 1
        ))
    }
}
