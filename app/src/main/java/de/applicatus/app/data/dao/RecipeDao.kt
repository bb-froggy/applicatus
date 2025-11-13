package de.applicatus.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.applicatus.app.data.model.potion.Recipe
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<Recipe>>
    
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: Long): Recipe?
    
    @Insert
    suspend fun insertRecipe(recipe: Recipe): Long
    
    @Insert
    suspend fun insertRecipes(recipes: List<Recipe>)
    
    @Delete
    suspend fun deleteRecipe(recipe: Recipe)
    
    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int
    
    @Query("SELECT name FROM recipes")
    suspend fun getAllRecipeNames(): List<String>
}
