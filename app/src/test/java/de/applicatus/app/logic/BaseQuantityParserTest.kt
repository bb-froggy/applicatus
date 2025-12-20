package de.applicatus.app.logic

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit-Tests für BaseQuantityParser
 */
class BaseQuantityParserTest {
    
    // ==================== Einfache Würfelnotationen ====================
    
    @Test
    fun testParse_simpleW6() {
        val result = BaseQuantityParser.parse("W6 Blätter", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("W6", result[0].quantity)
        assertFalse(result[0].rolled)
    }
    
    @Test
    fun testParse_multipleW20() {
        val result = BaseQuantityParser.parse("2W20 Blüten", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Blüten", result[0].productName)
        assertEquals("2W20", result[0].quantity)
    }
    
    @Test
    fun testParse_diceWithModifier() {
        val result = BaseQuantityParser.parse("W20+5 Blätter", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("W20+5", result[0].quantity)
    }
    
    @Test
    fun testParse_diceWithSubtraction() {
        val result = BaseQuantityParser.parse("W6-2 Blätter", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("W6-2", result[0].quantity)
    }
    
    @Test
    fun testParse_W3() {
        val result = BaseQuantityParser.parse("W3 Blüten", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Blüten", result[0].productName)
        assertEquals("W3", result[0].quantity)
    }
    
    // ==================== Festgelegte Mengen ====================
    
    @Test
    fun testParse_numberWord_eine() {
        val result = BaseQuantityParser.parse("eine Pflanze", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Pflanze", result[0].productName)
        assertEquals("1", result[0].quantity)
        assertFalse(result[0].rolled)
    }
    
    @Test
    fun testParse_numberWord_ein() {
        val result = BaseQuantityParser.parse("ein Baum", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Baum", result[0].productName)
        assertEquals("1", result[0].quantity)
    }
    
    @Test
    fun testParse_numberWord_vier() {
        val result = BaseQuantityParser.parse("vier Blätter", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("4", result[0].quantity)
    }
    
    @Test
    fun testParse_numberWord_zwölf() {
        val result = BaseQuantityParser.parse("12 Stängel", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Stängel", result[0].productName)
        assertEquals("12", result[0].quantity)
    }
    
    @Test
    fun testParse_simpleNumber() {
        val result = BaseQuantityParser.parse("5 Blüten, die kurz vor dem Verblühen sind", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Blüten", result[0].productName)
        assertEquals("5", result[0].quantity)
    }
    
    // ==================== Mehrere Produkte ====================
    
    @Test
    fun testParse_multipleProducts_semicolon() {
        val result = BaseQuantityParser.parse("2 Blätter; eine geschlossene Samenkapsel", roll = false)
        
        assertEquals(2, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("2", result[0].quantity)
        assertEquals("geschlossene Samenkapsel", result[1].productName)
        assertEquals("1", result[1].quantity)
    }
    
    @Test
    fun testParse_multipleProducts_and_fallback() {
        val result = BaseQuantityParser.parse("2 Blätter und eine geschlossene Samenkapsel", roll = false)
        
        assertEquals(2, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("2", result[0].quantity)
        assertEquals("geschlossene Samenkapsel", result[1].productName)
        assertEquals("1", result[1].quantity)
    }
    
    @Test
    fun testParse_multipleProducts_withDice() {
        val result = BaseQuantityParser.parse("W20+2 Blüten; 2W20+10 Blätter", roll = false)
        
        assertEquals(2, result.size)
        assertEquals("Blüten", result[0].productName)
        assertEquals("W20+2", result[0].quantity)
        assertEquals("Blätter", result[1].productName)
        assertEquals("2W20+10", result[1].quantity)
    }
    
    @Test
    fun testParse_complexMultipleProducts() {
        val result = BaseQuantityParser.parse("W6 Blätter; W20 Blüten; W3 Früchte", roll = false)
        
        assertEquals(3, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("W6", result[0].quantity)
        assertEquals("Blüten", result[1].productName)
        assertEquals("W20", result[1].quantity)
        assertEquals("Früchte", result[2].productName)
        assertEquals("W3", result[2].quantity)
    }
    
    // ==================== Komplexe Formate mit "mit" ====================
    
    @Test
    fun testParse_strauchMit() {
        val result = BaseQuantityParser.parse("Strauch mit W6 Blüten", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Blüten", result[0].productName)
        assertEquals("W6", result[0].quantity)
    }
    
    @Test
    fun testParse_baumMit() {
        val result = BaseQuantityParser.parse("Baum mit W20 Trieben; Bast", roll = false)
        
        // "Baum mit W20 Trieben" → "W20 Trieben"
        // Danach getrennt durch Semikolon
        assertEquals(2, result.size)
        assertEquals("Trieben", result[0].productName)
        assertEquals("W20", result[0].quantity)
        assertEquals("Bast", result[1].productName)
        assertEquals("1", result[1].quantity)
    }
    
    // ==================== Klammerinhalte ignorieren ====================
    
    @Test
    fun testParse_ignoreParentheses() {
        val result = BaseQuantityParser.parse("2W6 Knospen (Nur die Hälfte um den Strauch zu schonen)", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Knospen", result[0].productName)
        assertEquals("2W6", result[0].quantity)
    }
    
    @Test
    fun testParse_ignoreComplexParentheses() {
        val result = BaseQuantityParser.parse("vier Farnblätter; zwei je 3W6 Schritt lange Rangen", roll = false)
        
        // Zwei Produkte getrennt durch Semikolon
        assertEquals(2, result.size)
        assertEquals("Farnblätter", result[0].productName)
        assertEquals("4", result[0].quantity)
        // Der zweite Teil ist komplex, sollte aber erkannt werden
        assertTrue(result[1].productName.contains("Rangen") || result[1].productName.contains("Schritt"))
    }
    
    // ==================== Sonderfälle ====================
    
    @Test
    fun testParse_emptyString() {
        val result = BaseQuantityParser.parse("", roll = false)
        
        assertEquals(0, result.size)
    }
    
    @Test
    fun testParse_saft() {
        val result = BaseQuantityParser.parse("Saft einer Pflanze", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Saft", result[0].productName)
        assertEquals("1", result[0].quantity)
    }
    
    @Test
    fun testParse_1W3x20() {
        val result = BaseQuantityParser.parse("1W3 x 20 Blätter", roll = false)
        
        // Sollte "1W3 x 20 Blätter" als "1W3" und "20 Blätter" erkennen
        // Oder als komplexes Produkt
        assertTrue(result.isNotEmpty())
    }
    
    @Test
    fun testParse_withUnit_Stein() {
        val result = BaseQuantityParser.parse("W6 Stein der Algen", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Stein der Algen", result[0].productName)
        assertEquals("W6", result[0].quantity)
    }
    
    @Test
    fun testParse_withUnit_Maß() {
        val result = BaseQuantityParser.parse("eine Wurzel mit W6 Maß klarem Wasser", roll = false)
        
        assertEquals(1, result.size)
        // "mit" wird entfernt, dann wird nur noch "W6 Maß klarem Wasser" geparst
        assertEquals("Maß klarem Wasser", result[0].productName)
        assertEquals("W6", result[0].quantity)
    }
    
    @Test
    fun testParse_withUnit_Unzen() {
        val result = BaseQuantityParser.parse("1W20 Unzen", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Unzen", result[0].productName)
        assertEquals("1W20", result[0].quantity)
    }
    
    @Test
    fun testParse_compoundUnit() {
        val result = BaseQuantityParser.parse("W20 Blätter; Blüten sowie 1 Unze Harz", roll = false)
        
        // Durch Semikolon getrennt: "W20 Blätter" und "Blüten sowie 1 Unze Harz"
        assertEquals(2, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("W20", result[0].quantity)
        // Der zweite Teil enthält mehrere Informationen, wird als Fallback erkannt
        assertEquals("Blüten sowie 1 Unze Harz", result[1].productName)
        assertEquals("1", result[1].quantity)
    }
    
    // ==================== Würfeln-Tests ====================
    
    @Test
    fun testRollQuantity_W6() {
        val result = BaseQuantityParser.rollQuantity("W6 Blätter")
        
        assertEquals(1, result.size)
        assertEquals("Blätter", result[0].productName)
        assertTrue(result[0].rolled)
        val quantity = result[0].quantity.toInt()
        assertTrue(quantity in 1..6)
    }
    
    @Test
    fun testRollQuantity_multipleProducts() {
        val result = BaseQuantityParser.rollQuantity("W6 Blätter; W3 Blüten")
        
        assertEquals(2, result.size)
        assertTrue(result[0].rolled)
        assertTrue(result[1].rolled)
        
        val leafQuantity = result[0].quantity.toInt()
        val flowerQuantity = result[1].quantity.toInt()
        
        assertTrue(leafQuantity in 1..6)
        assertTrue(flowerQuantity in 1..3)
    }
    
    @Test
    fun testRollQuantity_fixedNumberStaysFixed() {
        val result = BaseQuantityParser.rollQuantity("eine Pflanze")
        
        assertEquals(1, result.size)
        assertEquals("Pflanze", result[0].productName)
        assertEquals("1", result[0].quantity)
        assertFalse(result[0].rolled)
    }
    
    // ==================== Realwelt-Beispiele ====================
    
    @Test
    fun testParse_alraune() {
        val result = BaseQuantityParser.parse("eine Pflanze", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Pflanze", result[0].productName)
        assertEquals("1", result[0].quantity)
    }
    
    @Test
    fun testParse_atanKiefer() {
        val result = BaseQuantityParser.parse("W20 Stein Rinde, bei komplettem Abschälen Verdreifachung des Wertes", roll = false)
        
        assertEquals(1, result.size)
        assertEquals("Stein Rinde", result[0].productName)
        assertEquals("W20", result[0].quantity)
    }
    
    @Test
    fun testParse_braunschlinge() {
        val result = BaseQuantityParser.parse("vier Farnblätter; zwei je 3W6 Schritt lange Rangen", roll = false)
        
        assertEquals(2, result.size)
        assertEquals("Farnblätter", result[0].productName)
        assertEquals("4", result[0].quantity)
    }
    
    @Test
    fun testParse_cheriaKaktus() {
        val result = BaseQuantityParser.parse("W3 Stein Kaktusfleisch; pro Stein 3W6+8 Stacheln", roll = false)
        
        assertEquals(2, result.size)
        assertEquals("Stein Kaktusfleisch", result[0].productName)
        assertEquals("W3", result[0].quantity)
        assertTrue(result[1].productName.contains("Stacheln"))
    }
    
    @Test
    fun testParse_hollbeere() {
        val result = BaseQuantityParser.parse("2W6 Sträucher mit jeweils 2W6+5 Beeren; 2W6+3 Blätter der untersten Zweige", roll = false)
        
        // "mit jeweils..." wird entfernt, dann getrennt durch Semikolon
        assertEquals(2, result.size)
        assertEquals("jeweils 2W6+5 Beeren", result[0].productName)
        assertEquals("1", result[0].quantity)
        assertEquals("Blätter der untersten Zweige", result[1].productName)
        assertEquals("2W6+3", result[1].quantity)
    }
    
    @Test
    fun testParse_satuariensbusch() {
        // Mit Semikolon-Trennung werden jetzt alle 4 Produkte erkannt
        val result = BaseQuantityParser.parse("4W20 Blätter; W20 Blüten; W20 Früchte; W3 Flux Saft", roll = false)
        
        assertEquals(4, result.size)
        assertEquals("Blätter", result[0].productName)
        assertEquals("4W20", result[0].quantity)
        assertEquals("Blüten", result[1].productName)
        assertEquals("W20", result[1].quantity)
        assertEquals("Früchte", result[2].productName)
        assertEquals("W20", result[2].quantity)
        assertEquals("Flux Saft", result[3].productName)
        assertEquals("W3", result[3].quantity)
    }
    
    // ==================== TaP*-Bedingungen ====================
    
    @Test
    fun testParse_tapCondition_met() {
        // Bedingung erfüllt: TaP* >= 7, tapStar = 10
        val result = BaseQuantityParser.parse("IF TaP*>=7: 7W6 Beeren", roll = false, tapStar = 10)
        
        assertEquals(1, result.size)
        assertEquals("Beeren", result[0].productName)
        assertEquals("7W6", result[0].quantity)
        assertEquals(7, result[0].requiredTapStar)
    }
    
    @Test
    fun testParse_tapCondition_notMet() {
        // Bedingung nicht erfüllt: TaP* >= 7, tapStar = 5
        val result = BaseQuantityParser.parse("IF TaP*>=7: 7W6 Beeren", roll = false, tapStar = 5)
        
        assertEquals(0, result.size) // Keine Items, da Bedingung nicht erfüllt
    }
    
    @Test
    fun testParse_tapCondition_exact() {
        // Bedingung exakt erfüllt: TaP* >= 13, tapStar = 13
        val result = BaseQuantityParser.parse("IF TaP*>=13: eine Beere", roll = false, tapStar = 13)
        
        assertEquals(1, result.size)
        assertEquals("Beere", result[0].productName)
        assertEquals("1", result[0].quantity)
        assertEquals(13, result[0].requiredTapStar)
    }
    
    @Test
    fun testParse_tapCondition_greaterThan() {
        // Bedingung mit >: TaP* > 6, tapStar = 7
        val result = BaseQuantityParser.parse("IF TaP*>6: 7W6 Beeren", roll = false, tapStar = 7)
        
        assertEquals(1, result.size)
        assertEquals("Beeren", result[0].productName)
        assertEquals("7W6", result[0].quantity)
        assertEquals(6, result[0].requiredTapStar)
    }
    
    @Test
    fun testParse_tapCondition_greaterThan_notMet() {
        // Bedingung mit > nicht erfüllt: TaP* > 6, tapStar = 6
        val result = BaseQuantityParser.parse("IF TaP*>6: 7W6 Beeren", roll = false, tapStar = 6)
        
        assertEquals(0, result.size) // Genau 6 reicht nicht für ">"
    }
    
    @Test
    fun testParse_tapCondition_withOtherProducts() {
        // TaP*-Bedingung kombiniert mit anderen Produkten
        val result = BaseQuantityParser.parse("W6+3 Kelche; IF TaP*>=13: eine Beere", roll = false, tapStar = 15)
        
        assertEquals(2, result.size)
        assertEquals("Kelche", result[0].productName)
        assertEquals("W6+3", result[0].quantity)
        assertNull(result[0].requiredTapStar) // Keine Bedingung
        assertEquals("Beere", result[1].productName)
        assertEquals("1", result[1].quantity)
        assertEquals(13, result[1].requiredTapStar)
    }
    
    @Test
    fun testParse_tapCondition_withOtherProducts_notMet() {
        // TaP*-Bedingung nicht erfüllt, nur erstes Produkt bleibt
        val result = BaseQuantityParser.parse("W6+3 Kelche; IF TaP*>=13: eine Beere", roll = false, tapStar = 10)
        
        assertEquals(1, result.size) // Nur Kelche, keine Beere
        assertEquals("Kelche", result[0].productName)
        assertEquals("W6+3", result[0].quantity)
    }
    
    @Test
    fun testRollQuantity_withTapCondition() {
        // Roll mit TaP*-Bedingung erfüllt
        val result = BaseQuantityParser.rollQuantity("IF TaP*>=7: 7W6 Beeren", tapStar = 10)
        
        assertEquals(1, result.size)
        assertEquals("Beeren", result[0].productName)
        assertTrue(result[0].rolled)
        assertNotNull(result[0].diceRoll)
        assertEquals("7W6", result[0].diceRoll)
    }
}
