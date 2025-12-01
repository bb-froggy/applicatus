package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.magicsign.MagicSign
import de.applicatus.app.data.model.magicsign.MagicSignDuration

/**
 * Ergebnis einer Zauberzeichen-Aktivierungsprobe
 */
data class MagicSignActivationResult(
    val success: Boolean,
    val rkpStar: Int,             // RkP* (Ritualkenntnispunkte übrig)
    val rolls: List<Int>,
    val message: String,
    val formattedRollResult: String,
    val isBotched: Boolean = false,
    val isDoubleOne: Boolean = false,
    val isTripleOne: Boolean = false,
    val isDoubleTwenty: Boolean = false,
    val isTripleTwenty: Boolean = false,
    val calculatedExpiryDate: String? = null
)

/**
 * Klasse für die Durchführung von Zauberzeichen-Aktivierungsproben
 * 
 * Die Aktivierungsprobe wird immer auf KL/IN/FF gewürfelt mit dem RkW als Fertigkeitswert.
 * Der Aktivierungsmodifikator des Zauberzeichens wird als Erleichterung angewandt.
 */
object MagicSignChecker {
    
    /**
     * Führt eine Zauberzeichen-Aktivierungsprobe durch
     * 
     * @param character Der Charakter, der das Zeichen aktiviert
     * @param magicSign Das zu aktivierende Zauberzeichen
     * @param currentDerianDate Das aktuelle derische Datum (für Ablaufberechnung)
     * @return MagicSignActivationResult mit allen Details
     */
    fun performActivationProbe(
        character: Character,
        magicSign: MagicSign,
        currentDerianDate: String
    ): MagicSignActivationResult {
        // Zauberzeichen-Aktivierung: KL/IN/FF mit RkW
        // Der Aktivierungsmodifikator ist eine Erleichterung (negativer difficulty-Wert)
        val difficulty = -magicSign.activationModifier
        
        val probeResult = ProbeChecker.performThreeAttributeProbe(
            fertigkeitswert = character.ritualKnowledgeValue,
            difficulty = difficulty,
            attribute1 = character.kl,
            attribute2 = character.inValue,
            attribute3 = character.ff,
            qualityPointName = "RkP*"
        )
        
        // Bei Patzer ist das Zeichen verdorben
        val isBotched = probeResult.isDoubleTwenty || probeResult.isTripleTwenty
        
        // Berechne Ablaufdatum wenn erfolgreich
        val expiryDate = if (probeResult.success && !isBotched) {
            calculateExpiryDate(magicSign.duration, character.ritualKnowledgeValue, currentDerianDate)
        } else null
        
        // Formatiere das Würfelergebnis
        val formattedRollResult = formatRollResult(
            rolls = probeResult.rolls,
            attributes = listOf(character.kl, character.inValue, character.ff),
            rkw = character.ritualKnowledgeValue,
            modifier = magicSign.activationModifier,
            rkpStar = probeResult.qualityPoints,
            success = probeResult.success,
            probeResult = probeResult
        )
        
        return MagicSignActivationResult(
            success = probeResult.success,
            rkpStar = probeResult.qualityPoints,
            rolls = probeResult.rolls,
            message = probeResult.message,
            formattedRollResult = formattedRollResult,
            isBotched = isBotched,
            isDoubleOne = probeResult.isDoubleOne,
            isTripleOne = probeResult.isTripleOne,
            isDoubleTwenty = probeResult.isDoubleTwenty,
            isTripleTwenty = probeResult.isTripleTwenty,
            calculatedExpiryDate = expiryDate
        )
    }
    
    /**
     * Berechnet das Ablaufdatum basierend auf der Wirkdauer
     */
    fun calculateExpiryDate(
        duration: MagicSignDuration,
        ritualKnowledgeValue: Int,
        currentDerianDate: String
    ): String {
        return when (duration) {
            MagicSignDuration.HALF_RKW_DAYS -> {
                // RkW/2 Tage (aufgerundet nach DSA-Regel)
                val days = (ritualKnowledgeValue + 1) / 2
                DerianDateCalculator.calculateExpiryDate(currentDerianDate, "$days Tage")
            }
            MagicSignDuration.ONE_MONTH -> {
                // Ein Monat = 30 Tage
                DerianDateCalculator.calculateExpiryDate(currentDerianDate, "30 Tage")
            }
            MagicSignDuration.ONE_QUARTER -> {
                // Ein Quartal = 3 Monate = 90 Tage
                DerianDateCalculator.calculateExpiryDate(currentDerianDate, "90 Tage")
            }
            MagicSignDuration.UNTIL_WINTER_SOLSTICE -> {
                // Bis zur nächsten Wintersonnenwende (1. Firun)
                DerianDateCalculator.getNextWinterSolstice(currentDerianDate)
            }
        }
    }
    
    /**
     * Formatiert das Würfelergebnis für die Anzeige
     */
    private fun formatRollResult(
        rolls: List<Int>,
        attributes: List<Int>,
        rkw: Int,
        modifier: Int,
        rkpStar: Int,
        success: Boolean,
        probeResult: ProbeResult
    ): String {
        val sb = StringBuilder()
        
        // Eigenschaftsnamen
        val attributeNames = listOf("KL", "IN", "FF")
        
        // Kopfzeile
        sb.appendLine("Zauberzeichen-Aktivierung (KL/IN/FF)")
        sb.appendLine("RkW: $rkw" + if (modifier != 0) " ${if (modifier > 0) "+" else ""}$modifier Erleichterung" else "")
        sb.appendLine()
        
        // Würfelergebnisse
        for (i in 0..2) {
            val roll = rolls[i]
            val attr = attributes[i]
            val attrName = attributeNames[i]
            val diff = if (roll > attr) roll - attr else 0
            val marker = when {
                roll == 1 -> " ⭐"
                roll == 20 -> " 💀"
                roll > attr -> " (-$diff)"
                else -> " ✓"
            }
            sb.appendLine("$attrName $attr: $roll$marker")
        }
        
        sb.appendLine()
        
        // Ergebnis
        when {
            probeResult.isTripleOne -> sb.appendLine("🌟 DREIFACH-1! Legendäre Aktivierung!")
            probeResult.isDoubleOne -> sb.appendLine("⭐ DOPPEL-1! Meisterhafte Aktivierung!")
            probeResult.isTripleTwenty -> sb.appendLine("💀💀💀 DREIFACH-20! Katastrophaler Patzer - Zeichen verdorben!")
            probeResult.isDoubleTwenty -> sb.appendLine("💀💀 DOPPEL-20! Patzer - Zeichen verdorben!")
            success -> sb.appendLine("✅ Erfolg! RkP*: $rkpStar")
            else -> sb.appendLine("❌ Misserfolg (RkP*: $rkpStar)")
        }
        
        return sb.toString()
    }
    
    /**
     * Prüft, ob ein Charakter Zauberzeichen verwenden kann
     */
    fun canUseZauberzeichen(character: Character): Boolean {
        return character.hasZauberzeichen && character.ritualKnowledgeValue > 0
    }
    
    /**
     * Berechnet die Gewichtsreduktion durch die Sigille des Unsichtbaren Trägers
     * 
     * @param rkpStar Die RkP* aus der Aktivierungsprobe
     * @param originalWeightInStone Das ursprüngliche Gewicht in Stein
     * @return Das reduzierte Gewicht in Stein (mindestens 1, wenn original >= 1)
     */
    fun calculateWeightReduction(rkpStar: Int, originalWeightInStone: Int): Int {
        if (originalWeightInStone <= 0) return 0
        
        val reduction = rkpStar * 2
        val reducedWeight = originalWeightInStone - reduction
        
        // Mindestens 1 Stein, wenn das Original mindestens 1 Stein war
        return if (originalWeightInStone >= 1) {
            maxOf(1, reducedWeight)
        } else {
            maxOf(0, reducedWeight)
        }
    }
}
