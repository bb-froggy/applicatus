package de.applicatus.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.applicatus.app.data.model.character.Group
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    
    @Query("SELECT * FROM groups")
    fun getAllGroups(): Flow<List<Group>>
    
    @Query("SELECT * FROM groups WHERE id = :groupId")
    fun getGroupById(groupId: Long): Flow<Group?>
    
    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupByIdOnce(groupId: Long): Group?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long
    
    @Update
    suspend fun updateGroup(group: Group)
    
    @Query("UPDATE groups SET currentDerianDate = :date WHERE id = :groupId")
    suspend fun updateCurrentDate(groupId: Long, date: String)
    
    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)
}
