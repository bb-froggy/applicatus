package de.applicatus.app.logic

import de.applicatus.app.data.model.spell.ApplicatusDuration
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit-Tests fuer die AsP-Kostenberechnung in ProbeChecker
 */
class AspCostCalculationTest {
    
    // ==================== calculateAspCost Tests ====================
    
    @Test
    fun testCalculateAspCost_onlyBase() {
        // Nur Basis-Kosten ohne Formel oder Modifikatoren
        val result = ProbeChecker.calculateAspCost(
            costFormula = "8",
            zfpStar = 5,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = false,
            hasKraftfokus = false
        )
        assertEquals(8, result)
    }
    
    @Test
    fun testCalculateAspCost_withKraftkontrolle() {
        // Basis + Kraftkontrolle (-1 AsP)
        val result = ProbeChecker.calculateAspCost(
            costFormula = "8",
            zfpStar = 5,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = true,
            hasKraftfokus = false
        )
        assertEquals(7, result)
    }
    
    @Test
    fun testCalculateAspCost_withKraftfokus() {
        // Basis + Kraftfokus (-1 AsP)
        val result = ProbeChecker.calculateAspCost(
            costFormula = "8",
            zfpStar = 5,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = false,
            hasKraftfokus = true
        )
        assertEquals(7, result)
    }
    
    @Test
    fun testCalculateAspCost_withBothKraftAbilities() {
        // Basis + Kraftkontrolle + Kraftfokus (-2 AsP)
        val result = ProbeChecker.calculateAspCost(
            costFormula = "10",
            zfpStar = 5,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = true,
            hasKraftfokus = true
        )
        assertEquals(8, result)
    }
    
    @Test
    fun testCalculateAspCost_minimumZero() {
        // Kosten koennen nicht unter 1 fallen
        val result = ProbeChecker.calculateAspCost(
            costFormula = "2",
            zfpStar = 5,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = true,
            hasKraftfokus = true
        )
        assertEquals(1, result)
    }
    
    @Test
    fun testCalculateAspCost_failureWithGuildMagic() {
        // Bei Fehlschlag und Gildenmagie: Halbe Kosten (aufgerundet)
        val result = ProbeChecker.calculateAspCost(
            costFormula = "9",
            zfpStar = 0,
            success = false,
            useHexenRepresentation = false,
            hasKraftkontrolle = false,
            hasKraftfokus = false
        )
        // 9 / 2 = 4.5 -> aufgerundet = 5
        assertEquals(5, result)
    }
    
    @Test
    fun testCalculateAspCost_failureWithHexenRepresentation() {
        // Bei Fehlschlag und Hexenrepraesentation: Ein Drittel Kosten (kaufmaennisch gerundet)
        val result = ProbeChecker.calculateAspCost(
            costFormula = "10",
            zfpStar = 0,
            success = false,
            useHexenRepresentation = true,
            hasKraftkontrolle = false,
            hasKraftfokus = false
        )
        // 10 / 3 = 3.333... -> kaufmaennisch gerundet = 3
        assertEquals(3, result)
    }
    
    @Test
    fun testCalculateAspCost_failureWithHexenRepresentationRoundUp() {
        // Bei Fehlschlag und Hexenrepraesentation: Ein Drittel Kosten (kaufmaennisch gerundet)
        val result = ProbeChecker.calculateAspCost(
            costFormula = "11",
            zfpStar = 0,
            success = false,
            useHexenRepresentation = true,
            hasKraftkontrolle = false,
            hasKraftfokus = false
        )
        // 11 / 3 = 3.666... -> kaufmaennisch gerundet = 4
        assertEquals(4, result)
    }
    
    @Test
    fun testCalculateAspCost_withSimpleFormula() {
        // Formel "8" sollte 8 AsP kosten
        val result = ProbeChecker.calculateAspCost(
            costFormula = "8",
            zfpStar = 5,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = false,
            hasKraftfokus = false
        )
        assertEquals(8, result)
    }
    
    @Test
    fun testCalculateAspCost_formulaWithZfP() {
        // Formel "ZfP*2" mit ZfP* = 5 sollte 10 AsP kosten
        val result = ProbeChecker.calculateAspCost(
            costFormula = "ZfP*2",
            zfpStar = 5,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = false,
            hasKraftfokus = false
        )
        assertEquals(10, result)
    }
    
    @Test
    fun testCalculateAspCost_armatrutzFormula() {
        // Armatrutz-Formel "16-ZfP/2" mit ZfP* = 6 sollte 16 - (6/2) = 13 AsP kosten
        val result = ProbeChecker.calculateAspCost(
            costFormula = "16-ZfP/2",
            zfpStar = 6,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = false,
            hasKraftfokus = false
        )
        assertEquals(13, result)
    }
    
    @Test
    fun testCalculateAspCost_baseAndFormulaCombined() {
        // Basis 5 + Formel "ZfP" mit ZfP* = 3 = 8 AsP
        val result = ProbeChecker.calculateAspCost(
            costFormula = "5+ZfP",
            zfpStar = 3,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = false,
            hasKraftfokus = false
        )
        assertEquals(8, result)
    }
    
    @Test
    fun testCalculateAspCost_complexFormulaWithKraftAbilities() {
        // Formel "16-ZfP/2" mit ZfP* = 4, plus beide Kraft-Faehigkeiten
        // 16 - (4/2) = 14, dann -1 -1 = 12
        val result = ProbeChecker.calculateAspCost(
            costFormula = "16-ZfP/2",
            zfpStar = 4,
            success = true,
            useHexenRepresentation = false,
            hasKraftkontrolle = true,
            hasKraftfokus = true
        )
        assertEquals(12, result)
    }
    
    // ==================== evaluateAspCostFormula Tests ====================
    
    @Test
    fun testEvaluateAspCostFormula_emptyString() {
        val result = ProbeChecker.evaluateAspCostFormula("", 5)
        assertNull(result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_simpleNumber() {
        val result = ProbeChecker.evaluateAspCostFormula("42", 5)
        assertEquals(42, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_zfpVariable() {
        val result = ProbeChecker.evaluateAspCostFormula("ZfP", 7)
        assertEquals(7, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_addition() {
        val result = ProbeChecker.evaluateAspCostFormula("5+3", 0)
        assertEquals(8, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_subtraction() {
        val result = ProbeChecker.evaluateAspCostFormula("10-4", 0)
        assertEquals(6, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_multiplication() {
        val result = ProbeChecker.evaluateAspCostFormula("6*3", 0)
        assertEquals(18, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_division() {
        val result = ProbeChecker.evaluateAspCostFormula("20/5", 0)
        assertEquals(4, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_divisionRounding() {
        // Division durch 2 wird aufgerundet
        val result = ProbeChecker.evaluateAspCostFormula("7/2", 0)
        assertEquals(4, result) // 3.5 aufgerundet
    }
    
    @Test
    fun testEvaluateAspCostFormula_parentheses() {
        val result = ProbeChecker.evaluateAspCostFormula("(3+5)*2", 0)
        assertEquals(16, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_operatorPrecedence() {
        val result = ProbeChecker.evaluateAspCostFormula("2+3*4", 0)
        assertEquals(14, result) // Nicht 20
    }
    
    @Test
    fun testEvaluateAspCostFormula_complexExpression() {
        val result = ProbeChecker.evaluateAspCostFormula("16-ZfP/2", 6)
        // 16 - (6/2) = 16 - 3 = 13
        assertEquals(13, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_zfpMultiplication() {
        val result = ProbeChecker.evaluateAspCostFormula("ZfP*3+2", 4)
        // 4*3+2 = 14
        assertEquals(14, result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_invalidFormula() {
        // Bei ungültiger Formel sollte null zurückgegeben werden
        val result = ProbeChecker.evaluateAspCostFormula("abc", 5)
        assertNull(result)
    }
    
    @Test
    fun testEvaluateAspCostFormula_whitespace() {
        val result = ProbeChecker.evaluateAspCostFormula("  10  +  5  ", 0)
        assertEquals(15, result)
    }
    
    // ==================== calculateApplicatusAspCost Tests ====================
    
    @Test
    fun testCalculateApplicatusAspCost_dayDuration() {
        // Tag: 2W6 = zwischen 2 und 12, ohne Ersparnis
        val fixedRoll: (Int) -> Int = { 4 } // Gibt immer 4 zurueck
        
        val (finalCost, baseCost, rollText) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.DAY,
            savingPercent = 0,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = fixedRoll
        )
        
        // 2W6 mit fixedRoll(4) = 2*4 = 8
        assertEquals(8, baseCost)
        assertEquals(8, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_moonDuration() {
        // Mond: 3W6
        val fixedRoll: (Int) -> Int = { 3 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.MOON,
            savingPercent = 0,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = fixedRoll
        )
        
        // 3W6 mit fixedRoll(3) = 3*3 = 9
        assertEquals(9, baseCost)
        assertEquals(9, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_quarterDuration() {
        // Quartal: 3W6+2
        val fixedRoll: (Int) -> Int = { 5 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.QUARTER,
            savingPercent = 0,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = fixedRoll
        )
        
        // 3W6+2 mit fixedRoll(5) = 3*5+2 = 17
        assertEquals(17, baseCost)
        assertEquals(17, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_winterSolsticeDuration() {
        // Wintersonnenwende: 4W6
        val fixedRoll: (Int) -> Int = { 4 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.WINTER_SOLSTICE,
            savingPercent = 0,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = fixedRoll
        )
        
        // 4W6 mit fixedRoll(4) = 4*4 = 16
        assertEquals(16, baseCost)
        assertEquals(16, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_with10PercentSaving() {
        // 10% Ersparnis
        val fixedRoll: (Int) -> Int = { 5 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.DAY,
            savingPercent = 10,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = fixedRoll
        )
        
        // 2W6 = 10, dann 10% Ersparnis = 10 - 1 = 9
        assertEquals(10, baseCost)
        assertEquals(9, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_with20PercentSaving() {
        // 20% Ersparnis
        val fixedRoll: (Int) -> Int = { 5 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.DAY,
            savingPercent = 20,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = fixedRoll
        )
        
        // 2W6 = 10, dann 20% Ersparnis = 10 - 2 = 8
        assertEquals(10, baseCost)
        assertEquals(8, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_with50PercentSaving() {
        // 50% Ersparnis
        val fixedRoll: (Int) -> Int = { 4 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.DAY,
            savingPercent = 50,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = fixedRoll
        )
        
        // 2W6 = 8, dann 50% Ersparnis = 8 - 4 = 4
        assertEquals(8, baseCost)
        assertEquals(4, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_withKraftkontrolle() {
        // Kraftkontrolle reduziert um 1
        val fixedRoll: (Int) -> Int = { 3 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.DAY,
            savingPercent = 0,
            hasKraftkontrolle = true,
            hasKraftfokus = false,
            diceRoll = fixedRoll
        )
        
        // 2W6 = 6, dann -1 = 5
        assertEquals(6, baseCost)
        assertEquals(5, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_withKraftfokus() {
        // Kraftfokus reduziert um 1
        val fixedRoll: (Int) -> Int = { 4 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.DAY,
            savingPercent = 0,
            hasKraftkontrolle = false,
            hasKraftfokus = true,
            diceRoll = fixedRoll
        )
        
        // 2W6 = 8, dann -1 = 7
        assertEquals(8, baseCost)
        assertEquals(7, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_combinedModifiers() {
        // Kombination: 20% Ersparnis + Kraftkontrolle + Kraftfokus
        val fixedRoll: (Int) -> Int = { 5 }
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.DAY,
            savingPercent = 20,
            hasKraftkontrolle = true,
            hasKraftfokus = true,
            diceRoll = fixedRoll
        )
        
        // 2W6 = 10
        // 20% Ersparnis = -2 -> 8
        // Kraftkontrolle = -1 -> 7
        // Kraftfokus = -1 -> 6
        assertEquals(10, baseCost)
        assertEquals(6, finalCost)
    }
    
    @Test
    fun testCalculateApplicatusAspCost_minimumZero() {
        // Kosten koennen nicht unter 0 fallen
        val fixedRoll: (Int) -> Int = { 1 } // Minimaler Wurf
        
        val (finalCost, baseCost, _) = ProbeChecker.calculateApplicatusAspCost(
            duration = ApplicatusDuration.DAY,
            savingPercent = 50,
            hasKraftkontrolle = true,
            hasKraftfokus = true,
            diceRoll = fixedRoll
        )
        
        // 2W6 = 2
        // 50% Ersparnis = -1 -> 1
        // Kraftkontrolle = -1 -> 0
        // Kraftfokus = -1 -> 0 (bleibt bei 0)
        assertEquals(2, baseCost)
        assertEquals(0, finalCost)
    }
}
