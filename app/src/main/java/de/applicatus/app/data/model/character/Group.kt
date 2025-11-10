package de.applicatus.app.data.model.character

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Spielgruppe mit eigenem derischen Datum.
 * Charaktere gehören zu einer Gruppe und teilen sich deren Datum.
 */
@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,  // Name der Gruppe (z.B. "Die Helden von Gareth")
    val currentDerianDate: String = "1 Praios 1040 BF"  // Aktuelles derisches Datum der Gruppe
)
