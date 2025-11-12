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
        assertNull(ProbeChecker.rollDice("W6"))
        assertNull(ProbeChecker.rollDice("3D6"))
        assertNull(ProbeChecker.rollDice(""))
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
}
