package de.applicatus.app.data.model.character

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.applicatus.app.data.model.potion.Laboratory
import java.util.UUID

@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class Character(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val guid: String = UUID.randomUUID().toString(),  // Eindeutige GUID für Import/Export
    val name: String,
    val mu: Int = 8,  // Mut
    val kl: Int = 8,  // Klugheit
    val inValue: Int = 8,  // Intuition (kann nicht "in" heißen wegen Kotlin-Keyword)
    val ch: Int = 8,  // Charisma
    val ff: Int = 8,  // Fingerfertigkeit
    val ge: Int = 8,  // Gewandtheit
    val ko: Int = 8,  // Konstitution
    val kk: Int = 8,  // Körperkraft
    val hasApplicatus: Boolean = false,  // Hat der Charakter Applicatus?
    val applicatusZfw: Int = 0,          // Applicatus-Zauberfertigkeit
    val applicatusModifier: Int = 0,     // Applicatus-Modifikator
    // Talente für Alchimie
    val hasAlchemy: Boolean = false,     // Beherrscht der Charakter Alchimie?
    val alchemySkill: Int = 0,           // Alchimie-Talentwert (0-18)
    val alchemyIsMagicalMastery: Boolean = false, // Ist Alchimie ein Magisches Meisterhandwerk?
    val hasCookingPotions: Boolean = false, // Beherrscht der Charakter Kochen (Tränke)?
    val cookingPotionsSkill: Int = 0,    // Kochen (Tränke)-Talentwert (0-18)
    val cookingPotionsIsMagicalMastery: Boolean = false, // Ist Kochen (Tränke) ein Magisches Meisterhandwerk?
    val selfControlSkill: Int = 0,       // Selbstbeherrschung-Talentwert (0-18, für Strukturanalyse-Serie)
    val sensoryAcuitySkill: Int = 0,     // Sinnenschärfe-Talentwert (0-18, für Analyse nach Augenschein)
    val magicalLoreSkill: Int = 0,       // Magiekunde-Talentwert (0-18, für Analys und Laboranalyse)
    val herbalLoreSkill: Int = 0,        // Pflanzenkunde-Talentwert (0-18, alternativ zu Magiekunde bei Laboranalyse)
    // Zauber für Alchimie (nur mit AE)
    val hasOdem: Boolean = false,        // Beherrscht der Charakter ODEM ARCANUM?
    val odemZfw: Int = 0,                // ODEM ARCANUM-Zauberfertigkeit (0-18)
    val hasAnalys: Boolean = false,      // Beherrscht der Charakter ANALYS ARKANSTRUKTUR?
    val analysZfw: Int = 0,              // ANALYS ARKANSTRUKTUR-Zauberfertigkeit (0-18)
    // Labor für Alchimie
    val defaultLaboratory: Laboratory? = null,  // Standard-Labor für Brauproben (null = kein Labor verfügbar)
    // Lebensenergie (alle Charaktere haben LE)
    val currentLe: Int = 30,             // Aktuelle Lebensenergie
    val maxLe: Int = 30,                 // Maximale Lebensenergie
    val leRegenBonus: Int = 0,           // Zusätzlicher LE-Regenerationsbonus (-3 bis +3)
    // Astralenergie (nur für Zauberer)
    val hasAe: Boolean = false,          // Hat der Charakter AE?
    val currentAe: Int = 0,              // Aktuelle Astralenergie
    val maxAe: Int = 0,                  // Maximale Astralenergie
    val aeRegenBonus: Int = 0,           // Zusätzlicher AE-Regenerationsbonus (-3 bis +3)
    val hasMasteryRegeneration: Boolean = false,  // Hat Meisterliche Regeneration?
    // Karmaenergie (nur für Geweihte)
    val hasKe: Boolean = false,          // Hat der Charakter KE?
    val currentKe: Int = 0,              // Aktuelle Karmaenergie
    val maxKe: Int = 0,                  // Maximale Karmaenergie
    // Spieler/Spielleiter-Modus
    val isGameMaster: Boolean = false,   // Ist der Nutzer Spielleiter? (zeigt alle Infos)
    // Gruppe
    val groupId: Long? = null,           // Foreign Key zur Gruppe (null = Standard-Gruppe wird verwendet)
    // Metadata
    val lastModifiedDate: Long = System.currentTimeMillis()  // Letzte Änderung des Charakters (Unix timestamp)
)
