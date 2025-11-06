package de.applicatus.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rezept für alchimistische Produkte
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String  // Name des Rezepts (z.B. "Heiltrank", "Zaubertrank")
)
