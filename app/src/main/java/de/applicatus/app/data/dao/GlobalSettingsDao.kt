package de.applicatus.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.applicatus.app.data.model.character.GlobalSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface GlobalSettingsDao {
    
    @Query("SELECT * FROM global_settings WHERE id = 1")
    fun getSettings(): Flow<GlobalSettings?>
    
    @Query("SELECT * FROM global_settings WHERE id = 1")
    suspend fun getSettingsOnce(): GlobalSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: GlobalSettings)
    
    @Update
    suspend fun updateSettings(settings: GlobalSettings)
    
    @Query("UPDATE global_settings SET currentDerianDate = :date WHERE id = 1")
    suspend fun updateCurrentDate(date: String)
}
