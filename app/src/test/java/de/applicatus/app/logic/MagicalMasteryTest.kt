package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.talent.Talent
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests für Magisches Meisterhandwerk-Funktionalität
 * 
 * Regeln:
 * - Bei AE-Ausgabe: +2 TaW pro AE
 * - Maximum: 2x des ursprünglichen TaW
 * - Nur verfügbar wenn Charakter AE hat und Talent als Magisches Meisterhandwerk besitzt
 */
class MagicalMasteryTest {
    
    /**
     * Erstellt einen Test-Charakter mit Alchimie als Magisches Meisterhandwerk
     */
    private fun createTestCharacter(
        alchemySkill: Int = 10,
        alchemyIsMagicalMastery: Boolean = true,
        currentAe: Int = 20,
        maxAe: Int = 30
    ) = Character(
        name = "Test Magier",
        mu = 12,
        kl = 14,
        inValue = 13,
        ch = 10,
        ff = 11,
        ge = 12,
        ko = 10,
        kk = 9,
        hasAlchemy = true,
        alchemySkill = alchemySkill,
        alchemyIsMagicalMastery = alchemyIsMagicalMastery,
        hasAe = true,
        currentAe = currentAe,
        maxAe = maxAe
    )
    
    @Test
    fun `Ohne AE-Ausgabe - normaler TaW wird verwendet`() {
        val character = createTestCharacter(alchemySkill = 10)
        
        // Feste Würfe: Alle 10 (gerade erfolgreich bei TaW 10 und Eigenschaften 12/14/11)
        val fixedRolls = listOf(10, 10, 10)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 0,
            astralEnergyCost = 0,  // Keine AE ausgeben
            diceRoll = diceRoll
        )
        
        // Bei TaW 10, keine Erschwernisse, Eigenschaften 12/14/11:
        // Würfe: 10, 10, 10
        // Überwürfe: 0, 0, 0 (10 <= 12, 10 <= 14, 10 <= 11)
        // FP* = 10 - 0 = 10
        assertTrue("Probe sollte erfolgreich sein", result.success)
        assertEquals("QP sollte 10 sein", 10, result.qualityPoints)
    }
    
    @Test
    fun `Mit 1 AE - TaW erhöht sich um 2`() {
        val character = createTestCharacter(alchemySkill = 10)
        
        // Feste Würfe: Alle 11 (würden bei TaW 10 scheitern, bei TaW 12 erfolgreich)
        val fixedRolls = listOf(11, 11, 11)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 0,
            astralEnergyCost = 1,  // 1 AE ausgeben = +2 TaW
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: 10 + (1 * 2) = 12
        // Bei TaW 12, Eigenschaften 12/14/11:
        // Würfe: 11, 11, 11
        // Überwürfe: 0 (11 <= 12), 0 (11 <= 14), 0 (11 <= 11)
        // FP* = 12 - 0 = 12
        assertTrue("Probe sollte erfolgreich sein mit erhöhtem TaW", result.success)
        assertEquals("QP sollte 12 sein (erhöhter TaW)", 12, result.qualityPoints)
    }
    
    @Test
    fun `Mit 3 AE - TaW erhöht sich um 6`() {
        val character = createTestCharacter(alchemySkill = 10)
        
        // Feste Würfe: Alle 12
        val fixedRolls = listOf(12, 12, 12)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 0,
            astralEnergyCost = 3,  // 3 AE ausgeben = +6 TaW
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: 10 + (3 * 2) = 16
        // Bei TaW 16, Eigenschaften 12/14/11:
        // Würfe: 12, 12, 12
        // Überwürfe: 0 (12 <= 12), 0 (12 <= 14), 1 (12 > 11)
        // FP* = 16 - 1 = 15
        assertTrue("Probe sollte erfolgreich sein", result.success)
        assertEquals("QP sollte 15 sein", 15, result.qualityPoints)
    }
    
    @Test
    fun `Deckelung auf 2x TaW - mit 10 AE bei TaW 10 max auf 20`() {
        val character = createTestCharacter(alchemySkill = 10, currentAe = 30)
        
        // Feste Würfe: Alle 1 (automatischer Erfolg)
        val fixedRolls = listOf(1, 1, 1)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        // Bei TaW 10: Maximal sinnvoll sind ⌈10/2⌉ = 5 AE (für +10 → TaW 20)
        // Mit 10 AE würde man +20 bekommen, aber Deckelung auf 20
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 0,
            astralEnergyCost = 10,  // 10 AE ausgeben = +20 TaW, aber max 20
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: min(10 + 20, 20) = 20 (Deckelung!)
        // Dreifach-1: Automatischer Erfolg mit max FP*
        assertTrue("Probe sollte erfolgreich sein (Dreifach-1)", result.success)
        assertTrue("Sollte Dreifach-1 erkennen", result.isTripleOne)
        assertEquals("QP sollte 20 sein (gedeckelt auf 2x TaW)", 20, result.qualityPoints)
        
        // Hinweis: Optimal wären nur 5 AE (⌈10/2⌉ = 5), der Rest ist verschwendet
    }
    
    @Test
    fun `Optimal AE-Ausgabe - halber TaW aufgerundet reicht für Deckelung`() {
        val character = createTestCharacter(alchemySkill = 10)
        
        // Feste Würfe: Alle 12
        val fixedRolls = listOf(12, 12, 12)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        // Bei TaW 10: Optimal sind ⌈10/2⌉ = 5 AE für maximalen Effekt
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 0,
            astralEnergyCost = 5,  // 5 AE = +10, erreicht die Deckelung von 20
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: min(10 + 10, 20) = 20
        // Bei TaW 20, Eigenschaften 12/14/11:
        // Würfe: 12, 12, 12
        // Überwürfe: 0, 0, 1 (12 > 11)
        // FP* = 20 - 1 = 19
        assertTrue("Probe sollte erfolgreich sein", result.success)
        assertEquals("QP sollte 19 sein", 19, result.qualityPoints)
    }
    
    @Test
    fun `Optimal AE-Ausgabe mit ungeradem TaW - aufrunden`() {
        val character = createTestCharacter(alchemySkill = 9)
        
        // Feste Würfe: Alle 12
        val fixedRolls = listOf(12, 12, 12)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        // Bei TaW 9: Optimal sind ⌈9/2⌉ = 5 AE für maximalen Effekt (9 + 10 = 19, max 18)
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 9,
            difficulty = 0,
            astralEnergyCost = 5,  // 5 AE = +10, aber gedeckelt auf 18
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: min(9 + 10, 18) = 18
        // Bei TaW 18, Eigenschaften 12/14/11:
        // Würfe: 12, 12, 12
        // Überwürfe: 0, 0, 1 (12 > 11)
        // FP* = 18 - 1 = 17
        assertTrue("Probe sollte erfolgreich sein", result.success)
        assertEquals("QP sollte 17 sein", 17, result.qualityPoints)
    }
    
    @Test
    fun `Deckelung auf 2x TaW - mit 5 AE bei TaW 8 nur bis 16`() {
        val character = createTestCharacter(alchemySkill = 8)
        
        // Feste Würfe: Alle 10
        val fixedRolls = listOf(10, 10, 10)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 8,
            difficulty = 0,
            astralEnergyCost = 5,  // 5 AE = +10, wäre 18, aber max 16
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: min(8 + 10, 16) = 16 (Deckelung!)
        // Bei TaW 16, Eigenschaften 12/14/11:
        // Würfe: 10, 10, 10
        // Überwürfe: 0, 0, 0
        // FP* = 16
        assertTrue("Probe sollte erfolgreich sein", result.success)
        assertEquals("QP sollte 16 sein (gedeckelt auf 2x TaW)", 16, result.qualityPoints)
    }
    
    @Test
    fun `Mit AE-Ausgabe und Erschwernis - TaW-Bonus hilft gegen Erschwernis`() {
        val character = createTestCharacter(alchemySkill = 10)
        
        // Feste Würfe: Alle 12
        val fixedRolls = listOf(12, 12, 12)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 5,  // +5 Erschwernis
            astralEnergyCost = 3,  // 3 AE = +6 TaW
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: 10 + 6 = 16
        // FP* = 16 - 5 = 11
        // Bei TaW 11 (nach Erschwernis), Eigenschaften 12/14/11:
        // Würfe: 12, 12, 12
        // Überwürfe: 0, 0, 1 (12 > 11)
        // FP* = 11 - 1 = 10
        assertTrue("Probe sollte erfolgreich sein trotz Erschwernis", result.success)
        assertEquals("QP sollte 10 sein", 10, result.qualityPoints)
    }
    
    @Test
    fun `Ohne AE-Ausgabe mit Erschwernis - würde scheitern`() {
        val character = createTestCharacter(alchemySkill = 10)
        
        // Feste Würfe: Alle 12
        val fixedRolls = listOf(12, 12, 12)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 5,  // +5 Erschwernis
            astralEnergyCost = 0,  // Keine AE
            diceRoll = diceRoll
        )
        
        // Bei TaW 10, +5 Erschwernis:
        // FP* = 10 - 5 = 5
        // Bei TaW 5, Eigenschaften 12/14/11:
        // Würfe: 12, 12, 12
        // Überwürfe: 0 (12 <= 12), 0 (12 <= 14), 1 (12 > 11)
        // FP* = 5 - 1 = 4
        assertTrue("Probe sollte erfolgreich sein", result.success)
        assertEquals("QP sollte 4 sein", 4, result.qualityPoints)
    }
    
    @Test
    fun `Niedriger TaW mit viel AE - kann auf Doppeltes erhöht werden`() {
        val character = createTestCharacter(alchemySkill = 5, currentAe = 20)
        
        // Feste Würfe: Alle 9
        val fixedRolls = listOf(9, 9, 9)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 5,
            difficulty = 0,
            astralEnergyCost = 3,  // 3 AE = +6, wäre 11, aber max 10
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: min(5 + 6, 10) = 10
        // Bei TaW 10, Eigenschaften 12/14/11:
        // Würfe: 9, 9, 9
        // Überwürfe: 0, 0, 0
        // FP* = 10
        assertTrue("Probe sollte erfolgreich sein", result.success)
        assertEquals("QP sollte 10 sein", 10, result.qualityPoints)
    }
    
    @Test
    fun `Doppel-20 mit AE-Ausgabe - bleibt ein Patzer`() {
        val character = createTestCharacter(alchemySkill = 10)
        
        // Feste Würfe: Zwei 20er und eine 1
        val fixedRolls = listOf(20, 20, 1)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 0,
            astralEnergyCost = 5,  // 5 AE ausgeben
            diceRoll = diceRoll
        )
        
        // Doppel-20 ist immer ein Patzer, egal wie hoch der TaW
        assertFalse("Probe sollte fehlschlagen (Doppel-20)", result.success)
        assertTrue("Sollte Doppel-20 erkennen", result.isDoubleTwenty)
        assertEquals("QP sollte 0 sein bei Patzer", 0, result.qualityPoints)
    }
    
    @Test
    fun `Doppel-1 mit AE-Ausgabe - automatischer Erfolg mit erhöhtem TaW`() {
        val character = createTestCharacter(alchemySkill = 10)
        
        // Feste Würfe: Zwei 1er und eine 10
        val fixedRolls = listOf(1, 1, 10)
        var rollIndex = 0
        val diceRoll = { fixedRolls[rollIndex++ % fixedRolls.size] }
        
        val result = ProbeChecker.performTalentProbe(
            talent = Talent.ALCHEMY,
            character = character,
            talentwert = 10,
            difficulty = 0,
            astralEnergyCost = 3,  // 3 AE = +6 TaW
            diceRoll = diceRoll
        )
        
        // Effektiver TaW: 10 + 6 = 16
        // Doppel-1: Automatischer Erfolg mit max FP* = effektiver TaW
        assertTrue("Probe sollte erfolgreich sein (Doppel-1)", result.success)
        assertTrue("Sollte Doppel-1 erkennen", result.isDoubleOne)
        assertEquals("QP sollte 16 sein (erhöhter TaW)", 16, result.qualityPoints)
    }
}
