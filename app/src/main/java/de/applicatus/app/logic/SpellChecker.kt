package de.applicatus.app.logic

import kotlin.random.Random

data class SpellCheckResult(
    val success: Boolean,
    val zfpStar: Int,
    val rolls: List<Int>,
    val message: String,
    val isDoubleOne: Boolean = false,
    val isTripleOne: Boolean = false,
    val isDoubleTwenty: Boolean = false,
    val isTripleTwenty: Boolean = false
)

data class ApplicatusCheckResult(
    val spellResult: SpellCheckResult,
    val applicatusResult: SpellCheckResult?,
    val overallSuccess: Boolean
)

/**
 * Zauberproben-Logik für Applicatus
 * 
 * Verwendet die zentrale ProbeChecker-Klasse für die eigentliche Proben-Durchführung.
 */
object SpellChecker {
    /**
     * Führt eine Zauberprobe durch (ohne Applicatus)
     * @param zfw Zauberfertigkeit
     * @param modifier Modifikator
     * @param attribute1 Eigenschaftswert 1
     * @param attribute2 Eigenschaftswert 2
     * @param attribute3 Eigenschaftswert 3
     * @return SpellCheckResult mit allen Details
     */
    fun performSpellCheck(
        zfw: Int,
        modifier: Int,
        attribute1: Int,
        attribute2: Int,
        attribute3: Int,
        diceRoll: () -> Int = { Random.nextInt(1, 21) }
    ): SpellCheckResult {
        // Nutze zentrale ProbeChecker-Logik
        val probeResult = ProbeChecker.performThreeAttributeProbe(
            fertigkeitswert = zfw,
            difficulty = modifier,
            attribute1 = attribute1,
            attribute2 = attribute2,
            attribute3 = attribute3,
            diceRoll = diceRoll,
            qualityPointName = "ZfP*"
        )
        
        // Konvertiere zu SpellCheckResult
        return SpellCheckResult(
            success = probeResult.success,
            zfpStar = probeResult.qualityPoints,
            rolls = probeResult.rolls,
            message = if (probeResult.success) "Zauber erfolgreich!" else "Zauber fehlgeschlagen!",
            isDoubleOne = probeResult.isDoubleOne,
            isTripleOne = probeResult.isTripleOne,
            isDoubleTwenty = probeResult.isDoubleTwenty,
            isTripleTwenty = probeResult.isTripleTwenty
        )
    }
    
    /**
     * Führt eine Applicatus-Zauberprobe durch (mit Applicatus-Probe)
     * Applicatus verwendet immer KL/FF/FF
     */
    fun performApplicatusCheck(
        spellZfw: Int,
        spellModifier: Int,
        spellAttribute1: Int,
        spellAttribute2: Int,
        spellAttribute3: Int,
        applicatusZfw: Int,
        applicatusModifier: Int,
        characterKl: Int,
        characterFf: Int,
        diceRoll: () -> Int = { Random.nextInt(1, 21) }
    ): ApplicatusCheckResult {
        // Erst Applicatus-Probe
        val applicatusResult = performSpellCheck(
            zfw = applicatusZfw,
            modifier = applicatusModifier,
            attribute1 = characterKl,
            attribute2 = characterFf,
            attribute3 = characterFf,
            diceRoll = diceRoll
        )
        
        // Applicatus fehlgeschlagen → Gesamtergebnis ist Misserfolg
        if (!applicatusResult.success) {
            return ApplicatusCheckResult(
                spellResult = SpellCheckResult(
                    success = false,
                    zfpStar = 0,
                    rolls = emptyList(),
                    message = "Applicatus fehlgeschlagen!"
                ),
                applicatusResult = applicatusResult,
                overallSuccess = false
            )
        }
        
        // Applicatus erfolgreich → eigentliche Zauberprobe
        val spellResult = performSpellCheck(
            zfw = spellZfw,
            modifier = spellModifier,
            attribute1 = spellAttribute1,
            attribute2 = spellAttribute2,
            attribute3 = spellAttribute3,
            diceRoll = diceRoll
        )
        
        return ApplicatusCheckResult(
            spellResult = spellResult,
            applicatusResult = applicatusResult,
            overallSuccess = spellResult.success
        )
    }
    
}
