package de.applicatus.app.data.model.potion

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rezept für alchimistische Produkte
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                       // Name des Rezepts (z.B. "Heiltrank", "Zaubertrank")
    val gruppe: String = "",                // Art des Tranks (z.B. "Spagyrik", "Venenik", "keine")
    val lab: Laboratory? = null,            // Benötigtes Labor (Archaisch, Hexenküche, Labor)
    val preis: Int? = null,                 // Preis des fertigen Tranks in Dukaten (null = unverkäuflich)
    val zutatenPreis: Int? = null,          // Preis der Zutaten in Dukaten
    val zutatenVerbreitung: Int = 0,        // Verbreitung der Zutaten (1-15, je höher desto häufiger)
    val verbreitung: Int = 0,               // Verbreitung des fertigen Tranks (1-15)
    val brewingDifficulty: Int = 0,         // Brauschwierigkeit (Erschwernis der Brauprobe, +0 bis +18)
    val analysisDifficulty: Int = 0,        // Analyse-Schwierigkeit (Erschwernis der Analyseprobe, +0 bis +18)
    val appearance: String = "",            // Aussehen des Tranks (z.B. "goldgelb, glitzernd")
    val shelfLife: String = "1 Mond"        // Haltbarkeit (z.B. "3 Monde", "1 Jahr")
)
