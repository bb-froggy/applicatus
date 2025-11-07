package de.applicatus.app.data.model.spell

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spells")
data class Spell(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val attribute1: String, // z.B. "MU"
    val attribute2: String, // z.B. "KL"
    val attribute3: String  // z.B. "IN"
)
