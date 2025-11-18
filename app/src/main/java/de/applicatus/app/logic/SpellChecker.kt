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
    val isTripleTwenty: Boolean = false,
    val aspCost: Int = 0  // Tatsächliche AsP-Kosten des Zaubers
)

data class ApplicatusCheckResult(
    val spellResult: SpellCheckResult,
    val applicatusResult: SpellCheckResult?,
    val overallSuccess: Boolean,
    val totalAspCost: Int = 0  // Gesamte AsP-Kosten (Applicatus + Zauber)
)

/**
 * Zauberproben-Logik für Applicatus
 * 
 * Verwendet die zentrale ProbeChecker-Klasse für die eigentliche Proben-Durchführung.
 */
object SpellChecker {
    /**
     * Fuehrt eine Zauberprobe durch (ohne Applicatus)
     * @param zfw Zauberfertigkeit
     * @param modifier Modifikator
     * @param attribute1 Eigenschaftswert 1
     * @param attribute2 Eigenschaftswert 2
     * @param attribute3 Eigenschaftswert 3
     * @param aspCostBase Basis-AsP-Kosten (fester Wert)
     * @param aspCostFormula AsP-Kosten-Formel (z.B. 16-ZfP/2)
     * @param useHexenRepresentation Wird in hexischer Repraesentation gesprochen?
     * @param hasKraftkontrolle Hat der Charakter Kraftkontrolle?
     * @param hasKraftfokus Hat der Charakter einen Kraftfokus?
     * @param applicKraftfokus Kraftfokus anwendbar? (false bei Zauberspeicher)
     * @return SpellCheckResult mit allen Details inkl. AsP-Kosten
     */
    fun performSpellCheck(
        zfw: Int,
        modifier: Int,
        attribute1: Int,
        attribute2: Int,
        attribute3: Int,
        aspCost: String = "",
        useHexenRepresentation: Boolean = false,
        hasKraftkontrolle: Boolean = false,
        hasKraftfokus: Boolean = false,
        applicKraftfokus: Boolean = true,
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
        
        // Berechne AsP-Kosten
        val calculatedAspCost = ProbeChecker.calculateAspCost(
            costFormula = aspCost,
            zfpStar = probeResult.qualityPoints,
            success = probeResult.success,
            useHexenRepresentation = useHexenRepresentation,
            hasKraftkontrolle = hasKraftkontrolle,
            hasKraftfokus = hasKraftfokus,
            applicKraftfokus = applicKraftfokus
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
            isTripleTwenty = probeResult.isTripleTwenty,
            aspCost = calculatedAspCost
        )
    }
    
    /**
     * Fuehrt eine Applicatus-Zauberprobe durch (mit Applicatus-Probe)
     * Applicatus verwendet immer KL/FF/FF und wird IMMER in gildenmagischer Repraesentation gesprochen
     */
    fun performApplicatusCheck(
        spellZfw: Int,
        spellModifier: Int,
        spellAttribute1: Int,
        spellAttribute2: Int,
        spellAttribute3: Int,
        spellAspCost: String,
        spellUseHexenRepresentation: Boolean,
        applicatusZfw: Int,
        applicatusModifier: Int,
        applicatusDuration: de.applicatus.app.data.model.spell.ApplicatusDuration,
        applicatusDurationModifier: Int = 0, // Schwierigkeitsmodifikator durch Wirkungsdauer
        applicatusAspSavingPercent: Int = 0, // Kostenersparnis beim Applicatus
        characterKl: Int,
        characterFf: Int,
        hasKraftkontrolle: Boolean,
        hasKraftfokus: Boolean,
        diceRoll: () -> Int = { Random.nextInt(1, 21) },
        diceRollD6: (Int) -> Int = { diceSize -> Random.nextInt(1, diceSize + 1) }
    ): ApplicatusCheckResult {
        // Berechne Applicatus-AsP-Kosten
        val (applicatusAspCost, applicatusBaseCost, applicatusRollText) = 
            ProbeChecker.calculateApplicatusAspCost(
                duration = applicatusDuration,
                savingPercent = applicatusAspSavingPercent,
                hasKraftkontrolle = hasKraftkontrolle,
                hasKraftfokus = hasKraftfokus,
                diceRoll = diceRollD6
            )
        
        // Berechne Erschwernis durch Kostenersparnis: 1,5 pro 10%, aufgerundet
        val savingPenalty = if (applicatusAspSavingPercent > 0) {
            // 1,5 pro 10% = 15 pro 100%, also (percent * 15 + 50) / 100 für kaufmännische Rundung
            // Aber wir wollen aufrunden: (percent * 15 + 99) / 100
            (applicatusAspSavingPercent * 15 + 99) / 100
        } else {
            0
        }
        
        // Erst Applicatus-Probe (mit Wirkungsdauer-Modifikator und Kostenersparnis-Erschwernis)
        // Applicatus wird IMMER in gildenmagischer Repräsentation gesprochen (kein useHexenRepresentation)
        val totalApplicatusModifier = applicatusModifier + applicatusDurationModifier + savingPenalty
        val applicatusProbe = ProbeChecker.performThreeAttributeProbe(
            fertigkeitswert = applicatusZfw,
            difficulty = totalApplicatusModifier,
            attribute1 = characterKl,
            attribute2 = characterFf,
            attribute3 = characterFf,
            diceRoll = diceRoll,
            qualityPointName = "ZfP*"
        )
        
        // Bei Applicatus-Fehlschlag: Halbierung der AsP-Kosten, KEIN Zauber
        if (!applicatusProbe.success) {
            val reducedApplicatusCost = (applicatusAspCost + 1) / 2
            
            val applicatusResult = SpellCheckResult(
                success = false,
                zfpStar = applicatusProbe.qualityPoints,
                rolls = applicatusProbe.rolls,
                message = "Applicatus fehlgeschlagen! $applicatusRollText = $applicatusBaseCost AsP → $reducedApplicatusCost AsP (halbiert)",
                isDoubleOne = applicatusProbe.isDoubleOne,
                isTripleOne = applicatusProbe.isTripleOne,
                isDoubleTwenty = applicatusProbe.isDoubleTwenty,
                isTripleTwenty = applicatusProbe.isTripleTwenty,
                aspCost = reducedApplicatusCost
            )
            
            return ApplicatusCheckResult(
                spellResult = SpellCheckResult(
                    success = false,
                    zfpStar = 0,
                    rolls = emptyList(),
                    message = "Zauber nicht gesprochen (Applicatus fehlgeschlagen)",
                    aspCost = 0
                ),
                applicatusResult = applicatusResult,
                overallSuccess = false,
                totalAspCost = reducedApplicatusCost
            )
        }
        
        // Applicatus erfolgreich → eigentliche Zauberprobe
        // Kraftfokus ist bei Applicatus-Zaubern NICHT anwendbar für den eigentlichen Zauber
        val spellResult = performSpellCheck(
            zfw = spellZfw,
            modifier = spellModifier,
            attribute1 = spellAttribute1,
            attribute2 = spellAttribute2,
            attribute3 = spellAttribute3,
            aspCost = spellAspCost,
            useHexenRepresentation = spellUseHexenRepresentation,
            hasKraftkontrolle = hasKraftkontrolle,
            hasKraftfokus = hasKraftfokus,
            applicKraftfokus = false, // Kraftfokus NICHT bei Applicatus-Zauber
            diceRoll = diceRoll
        )
        
        val applicatusResult = SpellCheckResult(
            success = true,
            zfpStar = applicatusProbe.qualityPoints,
            rolls = applicatusProbe.rolls,
            message = "Applicatus erfolgreich! $applicatusRollText = $applicatusBaseCost AsP → $applicatusAspCost AsP",
            isDoubleOne = applicatusProbe.isDoubleOne,
            isTripleOne = applicatusProbe.isTripleOne,
            isDoubleTwenty = applicatusProbe.isDoubleTwenty,
            isTripleTwenty = applicatusProbe.isTripleTwenty,
            aspCost = applicatusAspCost
        )
        
        return ApplicatusCheckResult(
            spellResult = spellResult,
            applicatusResult = applicatusResult,
            overallSuccess = spellResult.success,
            totalAspCost = applicatusAspCost + spellResult.aspCost
        )
    }
    
}