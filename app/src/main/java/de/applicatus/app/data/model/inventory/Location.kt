package de.applicatus.app.data.model.inventory

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lagerort für Gegenstände (z.B. "Am Körper", "Rucksack", "Packtier")
 */
@Entity(tableName = "locations")
data class Location(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** ID des Charakters, dem dieser Ort gehört */
    val characterId: Long,
    
    /** Name des Ortes (z.B. "Am Körper", "Rucksack", "Pferd") */
    val name: String,
    
    /** Ist dieser Ort ein Standard-Ort, der nicht gelöscht werden kann? */
    val isDefault: Boolean = false,
    
    /** Sortierreihenfolge */
    val sortOrder: Int = 0
)
