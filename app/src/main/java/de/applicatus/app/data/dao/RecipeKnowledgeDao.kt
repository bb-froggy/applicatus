package de.applicatus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.applicatus.app.data.model.RecipeKnowledge
import de.applicatus.app.data.model.RecipeKnowledgeLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeKnowledgeDao {
    
    @Query("SELECT * FROM recipe_knowledge WHERE characterId = :characterId")
    fun getKnowledgeForCharacter(characterId: Long): Flow<List<RecipeKnowledge>>
    
    @Query("SELECT * FROM recipe_knowledge WHERE characterId = :characterId AND recipeId = :recipeId")
    suspend fun getKnowledge(characterId: Long, recipeId: Long): RecipeKnowledge?
    
    @Query("SELECT * FROM recipe_knowledge WHERE characterId = :characterId AND knowledgeLevel = :level")
    fun getKnowledgeByLevel(characterId: Long, level: RecipeKnowledgeLevel): Flow<List<RecipeKnowledge>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: RecipeKnowledge)
    
    @Update
    suspend fun updateKnowledge(knowledge: RecipeKnowledge)
    
    @Delete
    suspend fun deleteKnowledge(knowledge: RecipeKnowledge)
    
    @Query("DELETE FROM recipe_knowledge WHERE characterId = :characterId")
    suspend fun deleteKnowledgeForCharacter(characterId: Long)
    
    @Query("UPDATE recipe_knowledge SET knowledgeLevel = :level WHERE characterId = :characterId AND recipeId = :recipeId")
    suspend fun updateKnowledgeLevel(characterId: Long, recipeId: Long, level: RecipeKnowledgeLevel)
}
