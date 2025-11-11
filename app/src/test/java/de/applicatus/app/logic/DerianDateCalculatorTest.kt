package de.applicatus.app.logic

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit-Tests für DerianDateCalculator
 * 
 * Testet insbesondere die Würfelnotations-Unterstützung für Haltbarkeitsdaten
 */
class DerianDateCalculatorTest {
    
    // ==================== Würfel-Tests ====================
    
    @Test
    fun testRollDice_simpleNotation() {
        // 1W6 sollte zwischen 1 und 6 liegen
        val result = DerianDateCalculator.rollDice("1W6")
        assertNotNull(result)
        assertTrue(result!! in 1..6)
    }
    
    @Test
    fun testRollDice_multipleRolls() {
        // 3W6 sollte zwischen 3 und 18 liegen
        val result = DerianDateCalculator.rollDice("3W6")
        assertNotNull(result)
        assertTrue(result!! in 3..18)
    }
    
    @Test
    fun testRollDice_withPositiveModifier() {
        // 2W6+5 sollte zwischen 7 und 17 liegen (2-12 + 5)
        val result = DerianDateCalculator.rollDice("2W6+5")
        assertNotNull(result)
        assertTrue(result!! in 7..17)
    }
    
    @Test
    fun testRollDice_withNegativeModifier() {
        // 3W6-2 sollte zwischen 1 und 16 liegen (3-18 - 2)
        val result = DerianDateCalculator.rollDice("3W6-2")
        assertNotNull(result)
        assertTrue(result!! in 1..16)
    }
    
    @Test
    fun testRollDice_W20() {
        // 1W20 sollte zwischen 1 und 20 liegen
        val result = DerianDateCalculator.rollDice("1W20")
        assertNotNull(result)
        assertTrue(result!! in 1..20)
    }
    
    @Test
    fun testRollDice_caseInsensitive() {
        // Groß-/Kleinschreibung sollte keine Rolle spielen
        val result1 = DerianDateCalculator.rollDice("2w6+3")
        val result2 = DerianDateCalculator.rollDice("2W6+3")
        assertNotNull(result1)
        assertNotNull(result2)
    }
    
    @Test
    fun testRollDice_invalidNotation() {
        // Ungültige Notationen sollten null zurückgeben
        assertNull(DerianDateCalculator.rollDice("abc"))
        assertNull(DerianDateCalculator.rollDice("W6"))
        assertNull(DerianDateCalculator.rollDice("3D6"))
        assertNull(DerianDateCalculator.rollDice(""))
    }
    
    @Test
    fun testRollDice_negativeValues() {
        // Negative Würfel oder Würfelgrößen sollten null ergeben
        assertNull(DerianDateCalculator.rollDice("-1W6"))
        assertNull(DerianDateCalculator.rollDice("1W-6"))
    }
    
    // ==================== parseShelfLifeAmount-Tests ====================
    
    @Test
    fun testParseShelfLifeAmount_fixedNumber() {
        // Feste Zahlen sollten direkt zurückgegeben werden
        assertEquals(3, DerianDateCalculator.parseShelfLifeAmount("3 Monde"))
        assertEquals(12, DerianDateCalculator.parseShelfLifeAmount("12 Wochen"))
        assertEquals(1, DerianDateCalculator.parseShelfLifeAmount("1 Jahr"))
    }
    
    @Test
    fun testParseShelfLifeAmount_diceNotation() {
        // Würfelnotationen sollten einen Wert im gültigen Bereich liefern
        val result1 = DerianDateCalculator.parseShelfLifeAmount("3W6+2 Wochen")
        assertNotNull(result1)
        assertTrue(result1!! in 5..20) // 3-18 + 2
        
        val result2 = DerianDateCalculator.parseShelfLifeAmount("2W6-1 Tage")
        assertNotNull(result2)
        assertTrue(result2!! in 1..11) // 2-12 - 1
        
        val result3 = DerianDateCalculator.parseShelfLifeAmount("1W6 Monate")
        assertNotNull(result3)
        assertTrue(result3!! in 1..6)
    }
    
    @Test
    fun testParseShelfLifeAmount_invalidFormat() {
        // Ungültige Formate sollten null zurückgeben
        assertNull(DerianDateCalculator.parseShelfLifeAmount(""))
        assertNull(DerianDateCalculator.parseShelfLifeAmount("Wochen"))
        assertNull(DerianDateCalculator.parseShelfLifeAmount("abc Wochen"))
    }
    
    // ==================== calculateExpiryDate mit Würfeln ====================
    
    @Test
    fun testCalculateExpiryDate_fixedAmount() {
        // Feste Zeiträume sollten wie bisher funktionieren
        val currentDate = "15 Praios 1040 BF"
        
        // 3 Monde = 90 Tage
        val result1 = DerianDateCalculator.calculateExpiryDate(currentDate, "3 Monde")
        assertEquals("15 Travia 1040 BF", result1)
        
        // 1 Jahr = 365 Tage
        val result2 = DerianDateCalculator.calculateExpiryDate(currentDate, "1 Jahr")
        assertEquals("15 Praios 1041 BF", result2)
        
        // 2 Wochen = 14 Tage
        val result3 = DerianDateCalculator.calculateExpiryDate(currentDate, "2 Wochen")
        assertEquals("29 Praios 1040 BF", result3)
    }
    
    @Test
    fun testCalculateExpiryDate_withDiceNotation() {
        val currentDate = "1 Praios 1040 BF"
        
        // 1W6 Wochen = 1-6 Wochen = 7-42 Tage
        val result1 = DerianDateCalculator.calculateExpiryDate(currentDate, "1W6 Wochen")
        assertNotEquals(currentDate, result1)
        
        // Prüfe, ob das Datum in der Zukunft liegt
        val result1Days = DerianDateCalculator.parseDateToDays(result1)
        val currentDays = DerianDateCalculator.parseDateToDays(currentDate)
        assertNotNull(result1Days)
        assertNotNull(currentDays)
        assertTrue(result1Days!! > currentDays!!)
        
        // Die Differenz sollte zwischen 7 und 42 Tagen liegen
        val diff = result1Days - currentDays
        assertTrue(diff in 7..42)
    }
    
    @Test
    fun testCalculateExpiryDate_withDiceAndModifier() {
        val currentDate = "10 Rondra 1040 BF"
        
        // 3W6+2 Wochen = 5-20 Wochen = 35-140 Tage
        val result = DerianDateCalculator.calculateExpiryDate(currentDate, "3W6+2 Wochen")
        assertNotEquals(currentDate, result)
        
        val resultDays = DerianDateCalculator.parseDateToDays(result)
        val currentDays = DerianDateCalculator.parseDateToDays(currentDate)
        assertNotNull(resultDays)
        assertNotNull(currentDays)
        
        val diff = resultDays!! - currentDays!!
        assertTrue("Differenz $diff sollte zwischen 35 und 140 liegen", diff in 35..140)
    }
    
    @Test
    fun testCalculateExpiryDate_diceWithMonde() {
        val currentDate = "1 Praios 1040 BF"
        
        // 2W6 Monde = 2-12 Monde = 60-360 Tage
        val result = DerianDateCalculator.calculateExpiryDate(currentDate, "2W6 Monde")
        assertNotEquals(currentDate, result)
        
        val resultDays = DerianDateCalculator.parseDateToDays(result)
        val currentDays = DerianDateCalculator.parseDateToDays(currentDate)
        assertNotNull(resultDays)
        assertNotNull(currentDays)
        
        val diff = resultDays!! - currentDays!!
        assertTrue("Differenz $diff sollte zwischen 60 und 360 liegen", diff in 60..360)
    }
    
    @Test
    fun testCalculateExpiryDate_diceWithTage() {
        val currentDate = "20 Efferd 1040 BF"
        
        // 1W6+3 Tage = 4-9 Tage
        val result = DerianDateCalculator.calculateExpiryDate(currentDate, "1W6+3 Tage")
        assertNotEquals(currentDate, result)
        
        val resultDays = DerianDateCalculator.parseDateToDays(result)
        val currentDays = DerianDateCalculator.parseDateToDays(currentDate)
        assertNotNull(resultDays)
        assertNotNull(currentDays)
        
        val diff = resultDays!! - currentDays!!
        assertTrue("Differenz $diff sollte zwischen 4 und 9 liegen", diff in 4..9)
    }
    
    @Test
    fun testCalculateExpiryDate_invalidDiceNotation() {
        val currentDate = "15 Praios 1040 BF"
        
        // Ungültige Würfelnotationen sollten das aktuelle Datum zurückgeben
        assertEquals(currentDate, DerianDateCalculator.calculateExpiryDate(currentDate, "abc Wochen"))
        assertEquals(currentDate, DerianDateCalculator.calculateExpiryDate(currentDate, "W6 Wochen"))
        assertEquals(currentDate, DerianDateCalculator.calculateExpiryDate(currentDate, ""))
    }
    
    // ==================== Integrationstests ====================
    
    @Test
    fun testCalculateExpiryDate_multipleRollsGiveDifferentResults() {
        val currentDate = "1 Praios 1040 BF"
        val shelfLife = "2W6 Wochen"
        
        // Führe mehrere Berechnungen durch
        val results = mutableSetOf<String>()
        repeat(20) {
            val result = DerianDateCalculator.calculateExpiryDate(currentDate, shelfLife)
            results.add(result)
        }
        
        // Es sollten (wahrscheinlich) mehrere unterschiedliche Ergebnisse geben
        // (außer bei extremem Pech haben wir mindestens 2 verschiedene)
        // Hinweis: Dieser Test kann theoretisch fehlschlagen, ist aber sehr unwahrscheinlich
        assertTrue("Es sollten mehrere unterschiedliche Ergebnisse geben", results.size >= 2)
    }
    
    @Test
    fun testCalculateExpiryDate_crossMonthBoundary() {
        val currentDate = "25 Praios 1040 BF"
        
        // 1W6 Wochen ab 25. Praios sollte in Rondra oder später enden
        val result = DerianDateCalculator.calculateExpiryDate(currentDate, "1W6 Wochen")
        assertNotEquals(currentDate, result)
        
        // Prüfe, dass das Ergebnis gültig ist
        val resultDays = DerianDateCalculator.parseDateToDays(result)
        assertNotNull(resultDays)
    }
    
    @Test
    fun testCalculateExpiryDate_crossYearBoundary() {
        val currentDate = "25 Rahja 1040 BF"
        
        // 3W6 Wochen ab Ende Rahja sollte ins nächste Jahr gehen können
        val result = DerianDateCalculator.calculateExpiryDate(currentDate, "3W6 Wochen")
        assertNotEquals(currentDate, result)
        
        // Prüfe, dass das Ergebnis gültig ist
        val resultDays = DerianDateCalculator.parseDateToDays(result)
        assertNotNull(resultDays)
    }
    
    @Test
    fun testCalculateExpiryDate_withNamelessDays() {
        val currentDate = "3 Namenlose Tage 1040 BF"
        
        // 1W6 Tage ab Namenlosen Tagen
        val result = DerianDateCalculator.calculateExpiryDate(currentDate, "1W6 Tage")
        assertNotEquals(currentDate, result)
        
        // Prüfe, dass das Ergebnis gültig ist
        val resultDays = DerianDateCalculator.parseDateToDays(result)
        assertNotNull(resultDays)
    }
    
    // ==================== Regressionstests (alte Funktionalität) ====================
    
    @Test
    fun testParseDateToDays_basic() {
        // Prüfe, dass die Datumsberechnung weiterhin funktioniert
        val date1 = "1 Praios 1040 BF"
        val date2 = "1 Rondra 1040 BF"
        
        val days1 = DerianDateCalculator.parseDateToDays(date1)
        val days2 = DerianDateCalculator.parseDateToDays(date2)
        
        assertNotNull(days1)
        assertNotNull(days2)
        assertEquals(30, days2!! - days1!!) // 1 Monat = 30 Tage
    }
    
    @Test
    fun testIsExpired() {
        val currentDate = "15 Praios 1040 BF"
        val futureDate = "20 Praios 1040 BF"
        val pastDate = "10 Praios 1040 BF"
        
        // Zukünftiges Datum ist nicht abgelaufen
        assertFalse(DerianDateCalculator.isExpired(futureDate, currentDate))
        
        // Vergangenes Datum ist abgelaufen
        assertTrue(DerianDateCalculator.isExpired(pastDate, currentDate))
    }
}
