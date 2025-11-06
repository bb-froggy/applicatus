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
    val name: String,                   // Name des Rezepts (z.B. "Heiltrank", "Zaubertrank")
    val brewingDifficulty: Int = 0,     // Brauschwierigkeit (+0 bis +18)
    val analysisDifficulty: Int = 0,    // Analyse-Schwierigkeit (+0 bis +18)
    val appearance: String = "",        // Aussehen des Tranks (z.B. "goldgelb, glitzernd")
    val shelfLife: String = "1 Mond"    // Haltbarkeit (z.B. "3 Monde", "1 Jahr")
)
