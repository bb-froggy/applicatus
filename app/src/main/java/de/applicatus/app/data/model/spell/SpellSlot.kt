package de.applicatus.app.data.model.spell

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.applicatus.app.data.model.character.Character

@Entity(
    tableName = "spell_slots",
    foreignKeys = [
        ForeignKey(
            entity = Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Spell::class,
            parentColumns = ["id"],
            childColumns = ["spellId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["characterId"]),
        Index(value = ["spellId"])
    ]
)
data class SpellSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val characterId: Long,
    val slotNumber: Int,          // Slot-Nummer (0-9 oder mehr)
    val slotType: SlotType = SlotType.APPLICATUS, // Slot-Typ
    val volumePoints: Int = 0,    // Volumenpunkte (nur für SPELL_STORAGE relevant)
    val spellId: Long?,           // Welcher Zauber (null wenn kein Zauber ausgewählt)
    val zfw: Int = 0,             // Zauberfertigkeit
    val modifier: Int = 0,        // Modifikator
    val variant: String = "",     // Variante/Notiz
    val isFilled: Boolean = false, // Ist der Slot gefüllt?
    val zfpStar: Int? = null,     // ZfP* (nur wenn gefüllt)
    val lastRollResult: String? = null, // Letztes Würfelergebnis (für Anzeige)
    val applicatusRollResult: String? = null // Applicatus-Würfelergebnis (falls relevant)
)
