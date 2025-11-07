package de.applicatus.app.data.model.character

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Globale App-Einstellungen (charakterübergreifend)
 * Singleton-Pattern mit fester ID = 1
 */
@Entity(tableName = "global_settings")
data class GlobalSettings(
    @PrimaryKey
    val id: Long = 1,  // Immer 1, da Singleton
    val currentDerianDate: String = "1 Praios 1040 BF"  // Aktuelles derisches Datum
)
