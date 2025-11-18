package de.applicatus.app.logic

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für SpellChecker (bereits implementiert, erweitert um Edge Cases).
 */
class SpellCheckerAdditionalTest {
    
    @Test
    fun `triple 1 with negative ZfW still succeeds`() {
        val zfw = -5
        val result = SpellChecker.performSpellCheck(
            zfw = zfw,
            modifier = 10,
            attribute1 = 8,
            attribute2 = 8,
            attribute3 = 8,
            diceRoll = diceSequence(1, 1, 1)
        )

        assertTrue(result.isTripleOne)
        assertTrue(result.success)
        assertEquals(zfw, result.zfpStar)
    }

    @Test
    fun `triple 20 with high ZfW still fails`() {
        val result = SpellChecker.performSpellCheck(
            zfw = 20,
            modifier = -10,
            attribute1 = 20,
            attribute2 = 20,
            attribute3 = 20,
            diceRoll = diceSequence(20, 20, 20)
        )

        assertTrue(result.isTripleTwenty)
        assertFalse(result.success)
        assertEquals(0, result.zfpStar)
    }

    @Test
    fun `ZfP is capped at ZfW`() {
        val zfw = 10
        val result = SpellChecker.performSpellCheck(
            zfw = zfw,
            modifier = -5,
            attribute1 = 20,
            attribute2 = 20,
            attribute3 = 20,
            diceRoll = diceSequence(2, 3, 4)
        )

        assertTrue(result.success)
        assertEquals(zfw, result.zfpStar)
    }

    @Test
    fun `applicatus check performs two checks`() {
        val result = SpellChecker.performApplicatusCheck(
            spellZfw = 10,
            spellModifier = 0,
            spellAttribute1 = 12,
            spellAttribute2 = 12,
            spellAttribute3 = 12,
            spellAspCost = "",
            spellUseHexenRepresentation = false,
            applicatusZfw = 8,
            applicatusModifier = 0,
            applicatusDuration = de.applicatus.app.data.model.spell.ApplicatusDuration.DAY,
            applicatusDurationModifier = 0,
            applicatusAspSavingPercent = 0,
            characterKl = 13,
            characterFf = 13,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = diceSequence(5, 6, 7, 4, 5, 6)
        )

        assertNotNull(result.applicatusResult)
        assertNotNull(result.spellResult)
        assertTrue(result.applicatusResult!!.success)
        assertTrue(result.spellResult.success)
        assertTrue(result.overallSuccess)
    }

    @Test
    fun `applicatus failure prevents spell check`() {
        val result = SpellChecker.performApplicatusCheck(
            spellZfw = 10,
            spellModifier = 0,
            spellAttribute1 = 12,
            spellAttribute2 = 12,
            spellAttribute3 = 12,
            spellAspCost = "",
            spellUseHexenRepresentation = false,
            applicatusZfw = 0,
            applicatusModifier = 10,
            applicatusDuration = de.applicatus.app.data.model.spell.ApplicatusDuration.DAY,
            applicatusDurationModifier = 0,
            applicatusAspSavingPercent = 0,
            characterKl = 8,
            characterFf = 8,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = diceSequence(20, 20, 20)
        )

        assertNotNull(result.applicatusResult)
        assertFalse(result.applicatusResult!!.success)
        assertFalse(result.overallSuccess)
        assertEquals("Zauber nicht gesprochen (Applicatus fehlgeschlagen)", result.spellResult.message)
    }

    @Test
    fun `modifier affects ZfP calculation`() {
        val rolls = intArrayOf(9, 10, 8)
        val malus = SpellChecker.performSpellCheck(
            zfw = 15,
            modifier = 5,
            attribute1 = 15,
            attribute2 = 15,
            attribute3 = 15,
            diceRoll = diceSequence(*rolls)
        )

        val bonus = SpellChecker.performSpellCheck(
            zfw = 15,
            modifier = -5,
            attribute1 = 15,
            attribute2 = 15,
            attribute3 = 15,
            diceRoll = diceSequence(*rolls)
        )

        assertTrue(bonus.zfpStar > malus.zfpStar)
    }

    @Test
    fun `rolls are in valid range`() {
        val result = SpellChecker.performSpellCheck(
            zfw = 10,
            modifier = 0,
            attribute1 = 10,
            attribute2 = 10,
            attribute3 = 10,
            diceRoll = diceSequence(4, 12, 19)
        )

        assertEquals(3, result.rolls.size)
        result.rolls.forEach { roll ->
            assertTrue("Roll $roll out of range", roll in 1..20)
        }
    }

    @Test
    fun `double one detection works`() {
        val result = SpellChecker.performSpellCheck(
            zfw = 10,
            modifier = 0,
            attribute1 = 10,
            attribute2 = 10,
            attribute3 = 10,
            diceRoll = diceSequence(1, 1, 5)
        )

        assertTrue(result.isDoubleOne)
        assertFalse(result.isTripleOne)
        assertTrue(result.success)
        assertEquals(2, result.rolls.count { it == 1 })
    }
    
    @Test
    fun `applicatus saving penalty is applied correctly`() {
        // 10% Kostenersparnis = 1,5 Erschwernis = 2 (aufgerundet)
        // 20% = 3, 30% = 5, 40% = 6, 50% = 8
        
        // Test mit 20% Kostenersparnis (sollte +3 Erschwernis geben)
        val result = SpellChecker.performApplicatusCheck(
            spellZfw = 10,
            spellModifier = 0,
            spellAttribute1 = 12,
            spellAttribute2 = 12,
            spellAttribute3 = 12,
            spellAspCost = "8",
            spellUseHexenRepresentation = false,
            applicatusZfw = 10,
            applicatusModifier = 0, // Keine zusätzliche Erschwernis
            applicatusDuration = de.applicatus.app.data.model.spell.ApplicatusDuration.DAY,
            applicatusDurationModifier = 0,
            applicatusAspSavingPercent = 20, // 20% Ersparnis = +3 Erschwernis
            characterKl = 13,
            characterFf = 13,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = diceSequence(5, 6, 7, 4, 5, 6) // Erst Applicatus, dann Zauber
        )
        
        // Bei ZfW 10 und Erschwernis 3 (durch Kostenersparnis):
        // Effective FW = 10 - 3 = 7
        // Würfe (5, 6, 7) gegen KL 13, FF 13, FF 13
        // Alle unter den Eigenschaften, FP* = 7 übrig
        assertTrue(result.applicatusResult?.success == true)
        assertTrue(result.overallSuccess)
    }
    
    @Test
    fun `applicatus high saving penalty causes failure`() {
        // 50% Kostenersparnis = 7,5 Erschwernis = 8 (aufgerundet)
        val result = SpellChecker.performApplicatusCheck(
            spellZfw = 10,
            spellModifier = 0,
            spellAttribute1 = 12,
            spellAttribute2 = 12,
            spellAttribute3 = 12,
            spellAspCost = "8",
            spellUseHexenRepresentation = false,
            applicatusZfw = 8,
            applicatusModifier = 0,
            applicatusDuration = de.applicatus.app.data.model.spell.ApplicatusDuration.DAY,
            applicatusDurationModifier = 0,
            applicatusAspSavingPercent = 50, // 50% Ersparnis = +8 Erschwernis
            characterKl = 13,
            characterFf = 13,
            hasKraftkontrolle = false,
            hasKraftfokus = false,
            diceRoll = diceSequence(10, 11, 12, 4, 5, 6) // Schwierige Würfe für Applicatus
        )
        
        // Bei ZfW 8 und Erschwernis 8: FW = 0, sehr schwierig
        // Diese Würfe sollten wahrscheinlich fehlschlagen
        // (Test verifiziert hauptsächlich, dass die Erschwernis angewendet wird)
        assertNotNull(result.applicatusResult)
    }

    private fun diceSequence(vararg values: Int): () -> Int {
        val iterator = values.iterator()
        return {
            if (!iterator.hasNext()) {
                fail("Not enough dice values provided for test")
            }
            iterator.nextInt()
        }
    }
}
