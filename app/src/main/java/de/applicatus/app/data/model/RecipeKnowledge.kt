package de.applicatus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Wissen eines Charakters über ein Rezept
 */
@Entity(
    tableName = "recipe_knowledge",
    foreignKeys = [
        ForeignKey(
            entity = Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterId"), Index("recipeId"), Index(value = ["characterId", "recipeId"], unique = true)]
)
data class RecipeKnowledge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val characterId: Long,
    val recipeId: Long,
    val knowledgeLevel: RecipeKnowledgeLevel = RecipeKnowledgeLevel.UNKNOWN
)
