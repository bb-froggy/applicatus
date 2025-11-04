package de.applicatus.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class Character(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mu: Int = 8,  // Mut
    val kl: Int = 8,  // Klugheit
    val inValue: Int = 8,  // Intuition (kann nicht "in" heißen wegen Kotlin-Keyword)
    val ch: Int = 8,  // Charisma
    val ff: Int = 8,  // Fingerfertigkeit
    val ge: Int = 8,  // Gewandtheit
    val ko: Int = 8,  // Konstitution
    val kk: Int = 8   // Körperkraft
)
