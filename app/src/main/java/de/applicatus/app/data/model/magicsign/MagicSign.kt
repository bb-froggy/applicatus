package de.applicatus.app.data.model.magicsign

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.inventory.Item
import java.util.UUID

/**
 * Zauberzeichen, das auf einem Inventar-Gegenstand angebracht ist.
 * Die Erstellung erfolgt außerhalb der App, hier wird nur die Aktivierung verwaltet.
 */
@Entity(
    tableName = "magic_signs",
    foreignKeys = [
        ForeignKey(
            entity = Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("characterId"),
        Index("itemId")
    ]
)
data class MagicSign(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Eindeutige GUID für Import/Export */
    val guid: String = UUID.randomUUID().toString(),
    
    /** ID des Charakters, dem dieses Zauberzeichen gehört */
    val characterId: Long,
    
    /** ID des Items, auf dem das Zauberzeichen angebracht ist */
    val itemId: Long,
    
    /** GUID des Charakters, der das Zauberzeichen ursprünglich angelegt hat */
    val creatorGuid: String? = null,
    
    /** Name/Beschreibung des Zauberzeichens (Freitext) */
    val name: String,
    
    /** Detaillierte Beschreibung der Wirkung (Freitext) */
    val effectDescription: String = "",
    
    /** Spezieller hardcodierter Effekt (z.B. Gewichtsreduktion) */
    val effect: MagicSignEffect = MagicSignEffect.NONE,
    
    /** Erleichterung auf die Aktivierungsprobe (hängt von der Qualität der Erstellung ab) */
    val activationModifier: Int = 0,
    
    /** Gewählte Wirkdauer des Zauberzeichens */
    val duration: MagicSignDuration = MagicSignDuration.HALF_RKW_DAYS,
    
    /** Ist das Zauberzeichen aktiviert? */
    val isActivated: Boolean = false,
    
    /** Ist das Zauberzeichen durch einen Patzer verdorben? (nur für Spielleiter sichtbar) */
    val isBotched: Boolean = false,
    
    /** Ablaufdatum im Format "Tag Monat Jahr BF" (derisches Datum) */
    val expiryDate: String? = null,
    
    /** RkP* aus der Aktivierungsprobe (für Effektberechnung wie Gewichtsreduktion) */
    val activationRkpStar: Int? = null,
    
    /** Letztes Würfelergebnis der Aktivierungsprobe (formatierter String) */
    val lastRollResult: String? = null
)
