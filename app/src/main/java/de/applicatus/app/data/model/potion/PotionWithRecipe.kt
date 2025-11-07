package de.applicatus.app.data.model.potion

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Join-Objekt für Potion mit Recipe
 */
data class PotionWithRecipe(
    @Embedded val potion: Potion,
    @Relation(
        parentColumn = "recipeId",
        entityColumn = "id"
    )
    val recipe: Recipe
)
