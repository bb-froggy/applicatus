package de.applicatus.app.logic

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit-Tests für ProbeChecker
 */
class ProbeCheckerTest {
    
    // ==================== rollDice Tests ====================
    
    @Test
    fun testRollDice_simpleNotation() {
        // 1W6 sollte zwischen 1 und 6 liegen
        val result = ProbeChecker.rollDice("1W6")
        assertNotNull(result)
        assertTrue(result!! in 1..6)
    }
    
    @Test
    fun testRollDice_multipleRolls() {
        // 3W6 sollte zwischen 3 und 18 liegen
        val result = ProbeChecker.rollDice("3W6")
        assertNotNull(result)
        assertTrue(result!! in 3..18)
    }
    
    @Test
    fun testRollDice_withPositiveModifier() {
        // 2W6+5 sollte zwischen 7 und 17 liegen (2-12 + 5)
        val result = ProbeChecker.rollDice("2W6+5")
        assertNotNull(result)
        assertTrue(result!! in 7..17)
    }
    
    @Test
    fun testRollDice_withNegativeModifier() {
        // 3W6-2 sollte zwischen 1 und 16 liegen (3-18 - 2)
        val result = ProbeChecker.rollDice("3W6-2")
        assertNotNull(result)
        assertTrue(result!! in 1..16)
    }
    
    @Test
    fun testRollDice_W20() {
        // 1W20 sollte zwischen 1 und 20 liegen
        val result = ProbeChecker.rollDice("1W20")
        assertNotNull(result)
        assertTrue(result!! in 1..20)
    }
    
    @Test
    fun testRollDice_caseInsensitive() {
        // Groß-/Kleinschreibung sollte keine Rolle spielen
        val result1 = ProbeChecker.rollDice("2w6+3")
        val result2 = ProbeChecker.rollDice("2W6+3")
        assertNotNull(result1)
        assertNotNull(result2)
    }
    
    @Test
    fun testRollDice_invalidNotation() {
        // Ungültige Notationen sollten null zurückgeben
        assertNull(ProbeChecker.rollDice("abc"))
        assertNull(ProbeChecker.rollDice("3D6"))
        assertNull(ProbeChecker.rollDice(""))
    }
    
    @Test
    fun testRollDice_shorthandNotation_W3() {
        // "W3" ist DSA-Kurzschreibweise für "1W3" und sollte funktionieren
        val result = ProbeChecker.rollDice("W3")
        assertNotNull("W3 should be valid DSA notation for 1W3", result)
        assertTrue("W3 should give result between 1 and 3", result!! in 1..3)
    }
    
    @Test
    fun testRollDice_shorthandNotation_W6() {
        // "W6" ist DSA-Kurzschreibweise für "1W6"
        val result = ProbeChecker.rollDice("W6")
        assertNotNull("W6 should be valid DSA notation for 1W6", result)
        assertTrue("W6 should give result between 1 and 6", result!! in 1..6)
    }
    
    @Test
    fun testRollDice_shorthandNotation_W20() {
        // "W20" ist DSA-Kurzschreibweise für "1W20"
        val result = ProbeChecker.rollDice("W20")
        assertNotNull("W20 should be valid DSA notation for 1W20", result)
        assertTrue("W20 should give result between 1 and 20", result!! in 1..20)
    }
    
    @Test
    fun testRollDice_shorthandNotation_withModifier() {
        // "W6+2" sollte als "1W6+2" interpretiert werden
        val fixedRoll: (Int) -> Int = { 4 }
        val result = ProbeChecker.rollDice("W6+2", fixedRoll)
        assertNotNull("W6+2 should be valid", result)
        assertEquals("W6+2 with roll of 4 should be 6", 6, result)
    }
    
    @Test
    fun testRollDice_shorthandNotation_withNegativeModifier() {
        // "W20-1" sollte als "1W20-1" interpretiert werden  
        val fixedRoll: (Int) -> Int = { 10 }
        val result = ProbeChecker.rollDice("W20-1", fixedRoll)
        assertNotNull("W20-1 should be valid", result)
        assertEquals("W20-1 with roll of 10 should be 9", 9, result)
    }
    
    @Test
    fun testRollDice_negativeValues() {
        // Negative Würfel oder Würfelgrößen sollten null ergeben
        assertNull(ProbeChecker.rollDice("-1W6"))
        assertNull(ProbeChecker.rollDice("1W-6"))
    }
    
    @Test
    fun testRollDice_withCustomDiceRoll() {
        // Test mit benutzerdefinierter Würfel-Lambda (für Tests)
        val fixedRoll: (Int) -> Int = { 3 } // Gibt immer 3 zurück
        
        // 2W6+1 mit fixedRoll sollte 2*3+1 = 7 ergeben
        val result = ProbeChecker.rollDice("2W6+1", fixedRoll)
        assertEquals(7, result)
    }
    
    @Test
    fun testRollDice_withZeroModifier() {
        // 2W6+0 sollte korrekt funktionieren
        val result = ProbeChecker.rollDice("2W6+0")
        assertNotNull(result)
        assertTrue(result!! in 2..12)
    }

    // ==================== Duration Specification Tests ====================

    @Test
    fun testEvaluateDurationSpecification_fixedNumber() {
        val result = ProbeChecker.evaluateDurationSpecification("2 Monate", 5)
        assertNotNull(result)
        assertEquals(2, result!!.amount)
        assertEquals("Monate", result.unit)
    }

    @Test
    fun testEvaluateDurationSpecification_usesZfpStar() {
        val result = ProbeChecker.evaluateDurationSpecification("ZfP* Wochen", 4)
        assertNotNull(result)
        assertEquals(4, result!!.amount)
        assertEquals("Wochen", result.unit)
    }

    @Test
    fun testEvaluateDurationSpecification_withMultiplicationAndAddition() {
        val result = ProbeChecker.evaluateDurationSpecification("3*ZfP*+2 Tage", 3)
        assertNotNull(result)
        assertEquals(11, result!!.amount)
        assertEquals("Tage", result.unit)
    }

    @Test
    fun testEvaluateDurationSpecification_withDiceExpression() {
        val fixedRoll: (Int) -> Int = { 4 }
        val result = ProbeChecker.evaluateDurationSpecification("2W6+3 Tage", 1, fixedRoll)
        assertNotNull(result)
        // 2 Würfe à 4 + 3 = 11
        assertEquals(11, result!!.amount)
        assertEquals("Tage", result.unit)
    }

    @Test
    fun testEvaluateDurationSpecification_invalidUnitReturnsNull() {
        val result = ProbeChecker.evaluateDurationSpecification("2 Zyklen", 3)
        assertNull(result)
    }

    @Test
    fun testEvaluateDurationSpecification_negativeResultReturnsNull() {
        val result = ProbeChecker.evaluateDurationSpecification("-1 Tage", 3)
        assertNull(result)
    }
}
