package de.applicatus.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "characters")
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
    val maxKe: Int = 0                   // Maximale Karmaenergie
)
