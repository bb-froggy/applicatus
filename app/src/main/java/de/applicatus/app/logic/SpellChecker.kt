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
        // Würfle 3x W20
        val rolls = List(3) { diceRoll() }
        
        // Prüfe auf besondere Fälle
        val countOnes = rolls.count { it == 1 }
        val countTwenties = rolls.count { it == 20 }
        
        // Dreifach-1
        if (countOnes >= 3) {
            return SpellCheckResult(
                success = true,
                zfpStar = zfw, // Maximum
                rolls = rolls,
                message = "Dreifach-1! Legendär!",
                isTripleOne = true
            )
        }
        
        // Doppel-1
        if (countOnes >= 2) {
            return SpellCheckResult(
                success = true,
                zfpStar = zfw, // Maximum
                rolls = rolls,
                message = "Doppel-1! Meisterwerk!",
                isDoubleOne = true
            )
        }
        
        // Dreifach-20
        if (countTwenties >= 3) {
            return SpellCheckResult(
                success = false,
                zfpStar = 0,
                rolls = rolls,
                message = "Dreifach-20! Katastrophaler Patzer!",
                isTripleTwenty = true
            )
        }
        
        // Doppel-20
        if (countTwenties >= 2) {
            return SpellCheckResult(
                success = false,
                zfpStar = 0,
                rolls = rolls,
                message = "Doppel-20! Patzer!",
                isDoubleTwenty = true
            )
        }
        
        // ZfP* = ZfW - Modifikator
        var zfpStar = zfw - modifier
        
        val attributes = listOf(attribute1, attribute2, attribute3)
        var success : Boolean
        if (zfw > modifier) {
            // Normale Probe
            
            // Prüfe jeden Würfelwurf gegen die Eigenschaft
            rolls.forEachIndexed { index, roll ->
                val attribute = attributes[index]
                if (roll > attribute) {
                    // Überwurf: Differenz von ZfP* abziehen
                    val difference = roll - attribute
                    zfpStar -= difference
                }
            }
            
            // Prüfe Erfolg/Misserfolg
            success = zfpStar >= 0
            
            // ZfP* wird auf ZfW gedeckelt (kann nicht höher sein als ZfW)
            if (success) {
                zfpStar = minOf(zfpStar, zfw)
            } else {
                zfpStar = 0
            }
        } else {
            // Erschwerte Probe: Jede Eigenschaft muss um die Erschwernis unterwürfelt werden
            var difficulty = modifier - zfw
            zfpStar = 0 // Besser kann es nicht werden
            success = true  // Grundannahme ... kann sich aber gleich noch ändern

            // Prüfe jeden Würfelwurf gegen die Eigenschaft
            rolls.forEachIndexed { index, roll ->
                val attribute = attributes[index]
                if (roll + difficulty > attribute) {
                    success = false
                }
            }
        }

        if (success && zfpStar == 0) {
            zfpStar = 1 // Ein Erfolg hat immer mindestens 1 ZfP*
        }
        
        return SpellCheckResult(
            success = success,
            zfpStar = zfpStar,
            rolls = rolls,
            message = if (success) "Zauber erfolgreich!" else "Zauber fehlgeschlagen!"
        )
    }
    
    /**
     * Führt eine Applicatus-Zauberprobe durch (mit Applicatus-Probe)
     * Applicatus verwendet immer KL/IN/CH
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
        characterIn: Int,
        characterCh: Int,
        diceRoll: () -> Int = { Random.nextInt(1, 21) }
    ): ApplicatusCheckResult {
        // Erst Applicatus-Probe
        val applicatusResult = performSpellCheck(
            zfw = applicatusZfw,
            modifier = applicatusModifier,
            attribute1 = characterKl,
            attribute2 = characterIn,
            attribute3 = characterCh,
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
