package de.applicatus.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Alchimistisches Produkt (Elixier, Gift, etc.)
 */
@Entity(
    tableName = "potions",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId"), Index("characterId")]
)
data class Potion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val characterId: Long,          // Zugehöriger Charakter
    val recipeId: Long,             // Zugehöriges Rezept
    val quality: PotionQuality,     // Qualität (A-F, M)
    val expiryDate: String          // Haltbarkeit (derisches Datum als String)
)
