package de.applicatus.app.logic

import kotlin.random.Random

/**
 * Ergebnis einer Drei-Eigenschafts-Probe (z.B. Talent, Zauber)
 */
data class ProbeResult(
    val success: Boolean,              // War die Probe erfolgreich?
    val qualityPoints: Int,            // Qualitätspunkte (TaP*/ZfP*) übrig
    val rolls: List<Int>,              // Die drei Würfelwürfe
    val message: String,               // Beschreibung des Ergebnisses
    val isDoubleOne: Boolean = false,  // Doppel-1 gewürfelt
    val isTripleOne: Boolean = false,  // Dreifach-1 gewürfelt
    val isDoubleTwenty: Boolean = false,  // Doppel-20 gewürfelt
    val isTripleTwenty: Boolean = false   // Dreifach-20 gewürfelt
)

/**
 * Ergebnis einer einfachen Attributsprobe (z.B. KO-Probe, IN-Probe)
 */
data class AttributeProbeResult(
    val success: Boolean,
    val roll: Int,
    val attribute: Int
)

/**
 * Zentrale Klasse für die Durchführung von DSA-Proben
 * 
 * Diese Klasse kapselt die Logik für:
 * - Drei-Eigenschafts-Proben (Talente, Zauber)
 * - Einfache Attributsproben
 * - Würfelwürfe (W20, W6)
 * 
 * Alle anderen Klassen (SpellChecker, ElixirAnalyzer, PotionAnalyzer, etc.)
 * sollten diese zentrale Implementierung verwenden.
 */
object ProbeChecker {
    
    /**
     * Führt eine Drei-Eigenschafts-Probe durch (z.B. Talent- oder Zauberprobe)
     * 
     * DSA-Regelwerk:
     * - 3x W20 würfeln
     * - Jeder Wurf wird gegen eine Eigenschaft geprüft
     * - Bei Überwurf: Differenz von FW abziehen
     * - Erfolg bei FP >= 0
     * - Doppel-1/Dreifach-1: Automatischer Erfolg mit max FP
     * - Doppel-20/Dreifach-20: Automatischer Patzer
     * 
     * @param fertigkeitswert Fertigkeitswert (FW): Talentwert oder Zauberfertigkeit
     * @param difficulty Erschwernis (positiv) oder Erleichterung (negativ)
     * @param attribute1 Erster Eigenschaftswert
     * @param attribute2 Zweiter Eigenschaftswert
     * @param attribute3 Dritter Eigenschaftswert
     * @param diceRoll Lambda für Würfelwürfe (Standard: W20, überschreibbar für Tests)
     * @param qualityPointName Name der Qualitätspunkte (z.B. "TaP*", "ZfP*")
     * @return ProbeResult mit allen Details
     */
    fun performThreeAttributeProbe(
        fertigkeitswert: Int,
        difficulty: Int,
        attribute1: Int,
        attribute2: Int,
        attribute3: Int,
        diceRoll: () -> Int = { rollD20() },
        qualityPointName: String = "FP*"
    ): ProbeResult {
        // Würfle 3x W20
        val rolls = List(3) { diceRoll() }
        
        // Prüfe auf besondere Fälle
        val countOnes = rolls.count { it == 1 }
        val countTwenties = rolls.count { it == 20 }
        
        // Dreifach-1: Automatischer Erfolg mit Maximum
        if (countOnes >= 3) {
            return ProbeResult(
                success = true,
                qualityPoints = fertigkeitswert,
                rolls = rolls,
                message = "Dreifach-1! Legendär!",
                isTripleOne = true
            )
        }
        
        // Doppel-1: Automatischer Erfolg mit Maximum
        if (countOnes >= 2) {
            return ProbeResult(
                success = true,
                qualityPoints = fertigkeitswert,
                rolls = rolls,
                message = "Doppel-1! Meisterwerk!",
                isDoubleOne = true
            )
        }
        
        // Dreifach-20: Automatischer Patzer
        if (countTwenties >= 3) {
            return ProbeResult(
                success = false,
                qualityPoints = 0,
                rolls = rolls,
                message = "Dreifach-20! Katastrophaler Patzer!",
                isTripleTwenty = true
            )
        }
        
        // Doppel-20: Automatischer Patzer
        if (countTwenties >= 2) {
            return ProbeResult(
                success = false,
                qualityPoints = 0,
                rolls = rolls,
                message = "Doppel-20! Patzer!",
                isDoubleTwenty = true
            )
        }
        
        // Berechne Fertigkeitspunkte (FP* = FW - Erschwernis)
        var qualityPoints = fertigkeitswert - difficulty
        
        val attributes = listOf(attribute1, attribute2, attribute3)
        val success: Boolean
        
        if (fertigkeitswert > difficulty) {
            // Normale Probe: FW > Erschwernis
            // Prüfe jeden Würfelwurf gegen die Eigenschaft
            rolls.forEachIndexed { index, roll ->
                val attribute = attributes[index]
                if (roll > attribute) {
                    // Überwurf: Differenz von FP* abziehen
                    val difference = roll - attribute
                    qualityPoints -= difference
                }
            }
            
            // Erfolg wenn FP* >= 0
            success = qualityPoints >= 0
            
            // FP* werden auf FW gedeckelt (kann nicht höher sein als FW)
            if (success) {
                qualityPoints = minOf(qualityPoints, fertigkeitswert)
            } else {
                qualityPoints = 0
            }
        } else {
            // Erschwerte Probe: FW <= Erschwernis
            // Jede Eigenschaft muss um die Erschwernis unterwürfelt werden
            val effectiveDifficulty = difficulty - fertigkeitswert
            qualityPoints = 0 // Besser kann es nicht werden
            success = rolls.zip(attributes).all { (roll, attribute) ->
                roll + effectiveDifficulty <= attribute
            }
        }
        
        // Ein Erfolg hat immer mindestens 1 FP*
        if (success && qualityPoints == 0) {
            qualityPoints = 1
        }
        
        val message = if (success) {
            "Probe erfolgreich mit $qualityPoints $qualityPointName!"
        } else {
            "Probe fehlgeschlagen!"
        }
        
        return ProbeResult(
            success = success,
            qualityPoints = qualityPoints,
            rolls = rolls,
            message = message
        )
    }
    
    /**
     * Führt eine einfache Attributsprobe durch (z.B. KO-Probe)
     * 
     * @param attributeValue Wert der Eigenschaft
     * @param difficulty Erschwernis (Standard 0)
     * @param diceRoll Lambda für Würfelwurf (Standard: W20)
     * @return AttributeProbeResult mit Erfolg, Wurf und Eigenschaftswert
     */
    fun performAttributeProbe(
        attributeValue: Int,
        difficulty: Int = 0,
        diceRoll: () -> Int = { rollD20() }
    ): AttributeProbeResult {
        val roll = diceRoll()
        val success = roll + difficulty <= attributeValue
        return AttributeProbeResult(
            success = success,
            roll = roll,
            attribute = attributeValue
        )
    }
    
    /**
     * Würfelt einen W20 (1-20)
     */
    fun rollD20(): Int = Random.nextInt(1, 21)
    
    /**
     * Würfelt einen W6 (1-6)
     */
    fun rollD6(): Int = Random.nextInt(1, 7)
    
    /**
     * Würfelt N W20 und gibt eine Liste zurück
     */
    fun rollMultipleD20(count: Int): List<Int> = List(count) { rollD20() }
    
    /**
     * Zählt Einsen in einer Würfelliste
     */
    fun countOnes(rolls: List<Int>): Int = rolls.count { it == 1 }
    
    /**
     * Zählt Zwanziger in einer Würfelliste
     */
    fun countTwenties(rolls: List<Int>): Int = rolls.count { it == 20 }
}
