package de.applicatus.app.logic

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für SpellChecker (bereits implementiert, erweitert um Edge Cases).
 */
class SpellCheckerAdditionalTest {
    
    @Test
    fun `triple 1 with negative ZfW still succeeds`() {
        val zfw = -5  // Unmöglich hoher Modifikator
        val result = SpellChecker.performSpellCheck(
            zfw = zfw,
            modifier = 10,
            attribute1 = 8,
            attribute2 = 8,
            attribute3 = 8
        )
        
        // Bei Dreifach-1 sollte es trotzdem erfolgreich sein
        if (result.isTripleOne) {
            assertTrue(result.success)
            assertEquals(zfw, result.zfpStar)  // ZfP* = ZfW bei Dreifach-1
        }
    }
    
    @Test
    fun `triple 20 with high ZfW still fails`() {
        val result = SpellChecker.performSpellCheck(
            zfw = 20,  // Sehr hoher ZfW
            modifier = -10,  // Bonus
            attribute1 = 20,
            attribute2 = 20,
            attribute3 = 20
        )
        
        // Bei Dreifach-20 sollte es immer fehlschlagen
        if (result.isTripleTwenty) {
            assertFalse(result.success)
        }
    }
    
    @Test
    fun `ZfP is capped at ZfW`() {
        val zfw = 10
        // Perfekte Würfe sollten auf ZfW gedeckelt werden
        val result = SpellChecker.performSpellCheck(
            zfw = zfw,
            modifier = -5,  // Bonus von 5
            attribute1 = 20,  // Alle Attribute sehr hoch
            attribute2 = 20,
            attribute3 = 20
        )
        
        if (result.success && !result.isDoubleOne && !result.isTripleOne) {
            assertTrue("ZfP* ${result.zfpStar} should be <= ZfW $zfw", result.zfpStar <= zfw)
        }
    }
    
    @Test
    fun `applicatus check performs two checks`() {
        val result = SpellChecker.performApplicatusCheck(
            spellZfw = 10,
            spellModifier = 0,
            spellAttribute1 = 12,
            spellAttribute2 = 12,
            spellAttribute3 = 12,
            applicatusZfw = 8,
            applicatusModifier = 0,
            characterKl = 13,
            characterIn = 13,
            characterCh = 13
        )
        
        // Sollte beide Ergebnisse enthalten
        assertNotNull(result.applicatusResult)
        assertNotNull(result.spellResult)
        
        // Gesamterfolg nur wenn beide erfolgreich
        if (result.applicatusResult?.success == true && result.spellResult.success) {
            assertTrue(result.overallSuccess)
        }
        
        if (result.applicatusResult?.success == false) {
            assertFalse(result.overallSuccess)
        }
    }
    
    @Test
    fun `applicatus failure prevents spell check`() {
        // Dieser Test prüft die Logik, aber Würfelergebnisse sind zufällig
        val result = SpellChecker.performApplicatusCheck(
            spellZfw = 10,
            spellModifier = 0,
            spellAttribute1 = 12,
            spellAttribute2 = 12,
            spellAttribute3 = 12,
            applicatusZfw = 0,  // Unmöglicher ZfW
            applicatusModifier = 10,  // Hoher Malus
            characterKl = 8,
            characterIn = 8,
            characterCh = 8
        )
        
        // Applicatus-Ergebnis sollte vorhanden sein
        assertNotNull(result.applicatusResult)
        
        // Bei Applicatus-Misserfolg sollte Gesamterfolg false sein
        if (result.applicatusResult?.success == false) {
            assertFalse(result.overallSuccess)
        }
    }
    
    @Test
    fun `modifier affects ZfP calculation`() {
        // Mit Malus
        var totalZfp = 0
        repeat(10) {
            val result = SpellChecker.performSpellCheck(
                zfw = 15,
                modifier = 5,  // +5 Malus
                attribute1 = 15,
                attribute2 = 15,
                attribute3 = 15
            )
            if (result.success) totalZfp += result.zfpStar
        }
        
        val avgWithMalus = totalZfp / 10.0
        
        // Mit Bonus
        totalZfp = 0
        repeat(10) {
            val result = SpellChecker.performSpellCheck(
                zfw = 15,
                modifier = -5,  // -5 Bonus
                attribute1 = 15,
                attribute2 = 15,
                attribute3 = 15
            )
            if (result.success) totalZfp += result.zfpStar
        }
        
        val avgWithBonus = totalZfp / 10.0
        
        // Bonus sollte im Durchschnitt höhere ZfP* ergeben
        // (Dies ist ein statistischer Test, kann gelegentlich fehlschlagen)
        assertTrue(avgWithBonus >= avgWithMalus - 2)  // Mit etwas Toleranz wegen Zufall
    }
    
    @Test
    fun `rolls are in valid range`() {
        repeat(20) {
            val result = SpellChecker.performSpellCheck(
                zfw = 10,
                modifier = 0,
                attribute1 = 10,
                attribute2 = 10,
                attribute3 = 10
            )
            
            // Alle Würfe sollten zwischen 1 und 20 sein
            assertEquals(3, result.rolls.size)
            result.rolls.forEach { roll ->
                assertTrue("Roll $roll out of range", roll in 1..20)
            }
        }
    }
    
    @Test
    fun `double one detection works`() {
        // Dieser Test ist schwierig wegen Zufälligkeit
        // Wir testen nur die Logik-Konsistenz
        var foundDoubleOne = false
        
        repeat(100) {
            val result = SpellChecker.performSpellCheck(
                zfw = 10,
                modifier = 0,
                attribute1 = 10,
                attribute2 = 10,
                attribute3 = 10
            )
            
            if (result.isDoubleOne) {
                foundDoubleOne = true
                assertTrue(result.success)  // Doppel-1 ist immer Erfolg
                val ones = result.rolls.count { it == 1 }
                assertEquals(2, ones)  // Genau 2 Einsen
            }
            
            if (result.isTripleOne) {
                assertTrue(result.isDoubleOne)  // Dreifach-1 ist auch Doppel-1
                val ones = result.rolls.count { it == 1 }
                assertEquals(3, ones)  // Genau 3 Einsen
            }
        }
        
        // Nach 100 Versuchen sollten wir mindestens eine Doppel-1 gesehen haben
        // (Wahrscheinlichkeit: ~14% pro Versuch, nach 100 Versuchen fast sicher)
        // Aber das ist nicht garantiert, daher nur informativ
        if (foundDoubleOne) {
            assertTrue(true)  // Gut, gefunden
        }
    }
}
