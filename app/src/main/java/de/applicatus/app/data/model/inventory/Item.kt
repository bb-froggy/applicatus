package de.applicatus.app.data.model.inventory

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Gegenstand im Inventar eines Charakters
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = de.applicatus.app.data.model.character.Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Location::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("characterId"),
        Index("locationId")
    ]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** ID des Charakters, dem dieser Gegenstand gehört */
    val characterId: Long,
    
    /** ID des Ortes, an dem der Gegenstand verstaut ist (null = kein Ort zugewiesen) */
    val locationId: Long?,
    
    /** Name des Gegenstands */
    val name: String,
    
    /** Gewicht des Gegenstands */
    @Embedded
    val weight: Weight = Weight.ZERO,
    
    /** Sortierreihenfolge innerhalb eines Ortes */
    val sortOrder: Int = 0,
    
    /** Ist dies ein Geldbeutel? */
    val isPurse: Boolean = false,
    
    /** Geldmenge in Kreuzern (nur für Geldbeutel) */
    val kreuzerAmount: Int = 0,
    
    /** Ist dieser Gegenstand zählbar (mehrere gleichartige Exemplare)? */
    val isCountable: Boolean = false,
    
    /** Anzahl der Gegenstände (nur für zählbare Gegenstände, sonst 1) */
    val quantity: Int = 1
)
