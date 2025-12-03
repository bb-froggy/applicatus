package de.applicatus.app.data.model.spell

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.inventory.Item

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
        ),
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["characterId"]),
        Index(value = ["spellId"]),
        Index(value = ["itemId"])
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
    val applicatusRollResult: String? = null, // Applicatus-Würfelergebnis (falls relevant)
    val isBotched: Boolean = false, // Ist der Zauber verpatzt? (nur für Spieler-Ansicht relevant)
    val expiryDate: String? = null,  // Ablaufdatum im derischen Format (z.B. "1 Praios 1040 BF")
    val longDurationFormula: String = "", // Formel für langwirkende Zauber (z.B. "ZfP* Wochen")
    val aspCost: String = "",           // AsP-Kosten: Zahl ("8") oder Formel ("16-ZfP/2")
    val useHexenRepresentation: Boolean = false, // Wird in hexischer Repräsentation gesprochen? (1/3 AsP bei Fehlschlag statt 1/2)
    
    /** ID des Items, an das dieser Slot gebunden ist (Pflicht für Applicatus, optional für langwährende Zauber) */
    val itemId: Long? = null,
    
    /** GUID des Charakters, der diesen Zauber ursprünglich eingespeichert hat */
    val creatorGuid: String? = null
)
