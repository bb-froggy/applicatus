package de.applicatus.app.logic

import de.applicatus.app.data.model.Character
import kotlin.random.Random

/**
 * Ergebnis einer Energie-Regeneration
 */
data class RegenerationResult(
    val leGain: Int = 0,
    val aeGain: Int = 0,
    val keGain: Int = 0,
    val leDetails: String = "",
    val aeDetails: String = "",
    val keDetails: String = ""
) {
    fun getFormattedResult(): String {
        val parts = mutableListOf<String>()
        // Zeige immer die Berechnung an, auch wenn der Gain 0 ist
        if (leDetails.isNotEmpty()) {
            parts.add("LE +$leGain ($leDetails)")
        }
        if (aeDetails.isNotEmpty()) {
            parts.add("AE +$aeGain ($aeDetails)")
        }
        if (keDetails.isNotEmpty()) {
            parts.add("KE +$keGain ($keDetails)")
        }
        return if (parts.isEmpty()) "Keine Regeneration" else parts.joinToString(", ")
    }
}

/**
 * Berechnet die Regeneration für einen Charakter
 */
object RegenerationCalculator {
    
    /**
     * Führt eine vollständige Regeneration durch
     * @param character Der Charakter
     * @param modifier Globaler Regenerations-Modifikator (-6 bis +2)
     * @return Das Regenerations-Ergebnis
     */
    fun performRegeneration(character: Character, modifier: Int): RegenerationResult {
        val clampedModifier = modifier.coerceIn(-6, 2)
        
        var leGain = 0
        var aeGain = 0
        var keGain = 0
        val leDetailsParts = mutableListOf<String>()
        val aeDetailsParts = mutableListOf<String>()
        val keDetailsParts = mutableListOf<String>()
        
        // LE-Regeneration
        val leDice = rollD6()
        leDetailsParts.add("W6=$leDice")
        leGain += leDice
        
        // LE-Modifikator
        if (clampedModifier != 0) {
            leGain += clampedModifier
            leDetailsParts.add("Mod=$clampedModifier")
        }
        
        // LE-Bonus
        if (character.leRegenBonus != 0) {
            leGain += character.leRegenBonus
            leDetailsParts.add("Bonus=${character.leRegenBonus}")
        }

        leGain = leGain.coerceAtLeast(0)
        
        // KO-Probe für LE
        val koRoll = rollD20()
        if (koRoll <= character.ko) {
            leGain += 1
            leDetailsParts.add("KO-Probe: $koRoll≤${character.ko} ✓")
        } else {
            leDetailsParts.add("KO-Probe: $koRoll>${character.ko} ✗")
        }
        
        // AE-Regeneration (wenn vorhanden)
        if (character.hasAe) {
            if (character.hasMasteryRegeneration) {
                // Meisterliche Regeneration: 1/3 von max(KL, IN) + 3
                var baseValue = maxOf(character.kl, character.inValue)
                baseValue += 1  // Damit im nächsten Schritt richtig gerundet wird
                val masteryRegen = (baseValue / 3.0).toInt() + 3
                aeGain += masteryRegen
                aeDetailsParts.add("Meisterlich=${masteryRegen}")
            } else {
                // Normale Regeneration: 1W6
                val aeDice = rollD6()
                aeDetailsParts.add("W6=$aeDice")
                aeGain += aeDice
                
                // AE-Modifikator
                if (clampedModifier != 0) {
                    aeGain += clampedModifier
                    aeDetailsParts.add("Mod=$clampedModifier")
                }
            }
            
            // AE-Bonus
            if (character.aeRegenBonus != 0) {
                aeGain += character.aeRegenBonus
                aeDetailsParts.add("Bonus=${character.aeRegenBonus}")
            }

            aeGain = aeGain.coerceAtLeast(0)
            
            // IN-Probe für AE
            val inRoll = rollD20()
            if (inRoll <= character.inValue) {
                aeGain += 1
                aeDetailsParts.add("IN-Probe: $inRoll≤${character.inValue} ✓")
            } else {
                aeDetailsParts.add("IN-Probe: $inRoll>${character.inValue} ✗")
            }
        }
        
        // KE-Regeneration (wenn vorhanden, fix 1 Punkt, kein Modifikator)
        if (character.hasKe) {
            keGain = 1
            keDetailsParts.add("Fix=1")
        }
        
        // Sicherstellen, dass keine Werte negativ werden
        leGain = leGain.coerceAtLeast(0)
        aeGain = aeGain.coerceAtLeast(0)
        keGain = keGain.coerceAtLeast(0)
        
        return RegenerationResult(
            leGain = leGain,
            aeGain = aeGain,
            keGain = keGain,
            leDetails = leDetailsParts.joinToString(", "),
            aeDetails = if (aeDetailsParts.isEmpty()) "" else aeDetailsParts.joinToString(", "),
            keDetails = if (keDetailsParts.isEmpty()) "" else keDetailsParts.joinToString(", ")
        )
    }
    
    private fun rollD6(): Int = Random.nextInt(1, 7)
    private fun rollD20(): Int = Random.nextInt(1, 21)
}
