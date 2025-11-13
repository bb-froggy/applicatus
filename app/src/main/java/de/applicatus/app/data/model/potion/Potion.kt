package de.applicatus.app.data.model.potion

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.applicatus.app.data.model.character.Character
import java.util.UUID

/**
 * Alchimistisches Produkt (Elixier, Gift, etc.)
 */
@Entity(
    tableName = "potions",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId"), Index("characterId"), Index("locationId")]
)
data class Potion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val guid: String = UUID.randomUUID().toString(),    // Eindeutige GUID für Import/Export und Trank-Übergabe
    val characterId: Long,                              // Zugehöriger Charakter
    val recipeId: Long,                                 // Zugehöriges Rezept
    val locationId: Long? = null,                       // Lagerort (null = nicht zugeordnet)
    
    // Tatsächliche Eigenschaften (vom Spielleiter/Brauprozess bestimmt)
    val actualQuality: PotionQuality,                   // Tatsächliche Qualität (A-F, M)
    val appearance: String = "",                        // Aussehen (vom Rezept übernommen, aber änderbar)
    val createdDate: String,                            // Erstellungsdatum (derisches Datum als String)
    val expiryDate: String,                             // Haltbarkeit (derisches Datum als String)
    val preservationAttempted: Boolean = false,         // Wurde bereits versucht, den Trank haltbar zu machen?
    
    // Wissen des Charakters über das Elixier (durch Analyse gewonnen)
    val nameKnown: Boolean = false,                     // Ist der Name bekannt? (beim Brauen oder durch Rezeptwissen)
    val categoryKnown: Boolean = false,                 // Ist die Kategorie bekannt? (ab Strukturanalyse Erfolg)
    val knownQualityLevel: KnownQualityLevel = KnownQualityLevel.UNKNOWN,  // Wie genau ist die Qualität bekannt?
    val intensityQuality: IntensityQuality = IntensityQuality.UNKNOWN,     // Ergebnis der Intensitätsbestimmung (schwach/stark)
    val refinedQuality: RefinedQuality = RefinedQuality.UNKNOWN,           // Verfeinerte Qualität aus Strukturanalyse mit Intensität
    val knownExactQuality: PotionQuality? = null,       // Bekannte genaue Qualität (ab 13 TaP* Strukturanalyse)
    val shelfLifeKnown: Boolean = false,                // Ist die Haltbarkeit bekannt? (ab 8 TaP* Strukturanalyse)
    
    // Analysedaten
    val intensityDeterminationZfp: Int = 0,             // ZfP* der Intensitätsbestimmung (für Strukturanalyse-Erleichterung)
    val bestStructureAnalysisFacilitation: Int = 0      // Beste Erleichterung aus vorherigen Analysen (halbe ZfP*/TaP* der besten Analyse)
)
