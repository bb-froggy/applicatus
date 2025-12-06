package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.inventory.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE characterId = :characterId ORDER BY isDefault DESC, sortOrder, name")
    fun getLocationsForCharacter(characterId: Long): Flow<List<Location>>
    
    @Query("SELECT * FROM locations WHERE characterId = :characterId ORDER BY isDefault DESC, sortOrder, name")
    suspend fun getLocationsForCharacterOnce(characterId: Long): List<Location>
    
    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: Long): Location?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: Location): Long
    
    @Update
    suspend fun update(location: Location)
    
    @Query("UPDATE locations SET isCarried = :isCarried WHERE id = :locationId")
    suspend fun updateIsCarried(locationId: Long, isCarried: Boolean)
    
    @Delete
    suspend fun delete(location: Location)
    
    @Query("DELETE FROM locations WHERE characterId = :characterId AND isDefault = 0")
    suspend fun deleteNonDefaultLocations(characterId: Long)
    
    /**
     * Erstellt die Standard-Orte für einen Charakter (Rüstung/Kleidung, Rucksack)
     */
    suspend fun createDefaultLocations(characterId: Long) {
        insert(Location(
            characterId = characterId,
            name = "Rüstung/Kleidung",
            isDefault = true,
            isCarried = true,
            sortOrder = 0
        ))
        insert(Location(
            characterId = characterId,
            name = "Rucksack",
            isDefault = true,
            isCarried = true,
            sortOrder = 1
        ))
    }
}
