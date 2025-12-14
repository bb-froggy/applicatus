package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für den Regenerations-Modifikator.
 * 
 * Der Regenerations-Modifikator (-6 bis +2) wird sowohl bei LE als auch bei AE angewendet,
 * unabhängig davon ob normale oder Meisterliche Regeneration verwendet wird.
 * 
 * - LE-Modifikator: Wird immer zum W6-Wurf addiert
 * - AE-Modifikator: Wird sowohl bei normaler (W6) als auch bei Meisterlicher Regeneration addiert
 * 
 * Diese Tests verifizieren, dass der Modifikator korrekt verwendet wird.
 */
class RegenerationCalculatorModifierTest {
    
    private val testCharacter = Character(
        id = 1,
        name = "Test",
        mu = 12, kl = 14, inValue = 13, ch = 10, ff = 12, ge = 11, ko = 12, kk = 11,
        hasAe = true,
        maxAe = 30,
        currentAe = 30,
        maxLe = 30,
        currentLe = 20,
        hasMasteryRegeneration = false
    )
    
    private val testCharacterWithMasteryRegen = testCharacter.copy(
        hasMasteryRegeneration = true
    )
    
    @Test
    fun `regeneration with positive modifier includes modifier in LE details`() {
        // Führe mehrere Regenerationen durch und prüfe, dass der Modifikator in den Details erscheint
        val result = RegenerationCalculator.performRegeneration(testCharacter, 2)
        
        // Der Modifikator sollte in den Details erscheinen
        assertTrue(
            "LE Details sollten 'Mod=2' enthalten, aber waren: ${result.leDetails}",
            result.leDetails.contains("Mod=2")
        )
    }
    
    @Test
    fun `regeneration with positive modifier includes modifier in AE details for normal regen`() {
        // Charakter OHNE Meisterliche Regeneration
        val result = RegenerationCalculator.performRegeneration(testCharacter, 2)
        
        // AE Details sollten den Modifikator enthalten
        assertTrue(
            "AE Details sollten 'Mod=2' enthalten bei normaler Regeneration, aber waren: ${result.aeDetails}",
            result.aeDetails.contains("Mod=2")
        )
    }
    
    @Test
    fun `regeneration with positive modifier DOES include modifier in AE details for mastery regen`() {
        // Charakter MIT Meisterlicher Regeneration
        val result = RegenerationCalculator.performRegeneration(testCharacterWithMasteryRegen, 2)
        
        // AE Details sollten den Modifikator auch bei Meisterlicher Regeneration enthalten
        assertTrue(
            "AE Details sollten 'Mod=2' auch bei Meisterlicher Regeneration enthalten, aber waren: ${result.aeDetails}",
            result.aeDetails.contains("Mod=2")
        )
        
        // Außerdem sollte "Meisterlich" enthalten sein
        assertTrue(
            "AE Details sollten 'Meisterlich' enthalten",
            result.aeDetails.contains("Meisterlich")
        )
    }
    
    @Test
    fun `regeneration with negative modifier includes modifier in details`() {
        val result = RegenerationCalculator.performRegeneration(testCharacter, -2)
        
        assertTrue(
            "Details sollten 'Mod=-2' enthalten",
            result.leDetails.contains("Mod=-2")
        )
    }
    
    @Test
    fun `regeneration gain cannot be negative`() {
        // Mit -6 Modifikator und niedrigem Würfel könnte das Ergebnis theoretisch negativ werden
        // aber es sollte auf 0 begrenzt werden
        val result = RegenerationCalculator.performRegeneration(testCharacter, -6)
        
        assertTrue("LE Gain darf nicht negativ sein", result.leGain >= 0)
        assertTrue("AE Gain darf nicht negativ sein", result.aeGain >= 0)
    }
    
    @Test
    fun `modifier is clamped between -6 and +2`() {
        // Test mit +10 (sollte auf +2 begrenzt werden)
        val resultHigh = RegenerationCalculator.performRegeneration(testCharacter, 10)
        assertTrue(
            "Modifikator sollte auf +2 begrenzt werden",
            resultHigh.leDetails.contains("Mod=2")
        )
        
        // Test mit -10 (sollte auf -6 begrenzt werden)
        val resultLow = RegenerationCalculator.performRegeneration(testCharacter, -10)
        assertTrue(
            "Modifikator sollte auf -6 begrenzt werden",
            resultLow.leDetails.contains("Mod=-6")
        )
    }
    
    @Test
    fun `zero modifier is not shown in details`() {
        val result = RegenerationCalculator.performRegeneration(testCharacter, 0)
        
        assertFalse(
            "Bei Modifikator 0 sollte kein Mod= in den Details erscheinen",
            result.leDetails.contains("Mod=")
        )
    }
    
    @Test
    fun `regeneration result formatted output includes modifier`() {
        val result = RegenerationCalculator.performRegeneration(testCharacter, 2)
        val formatted = result.getFormattedResult()
        
        assertTrue(
            "Formatierte Ausgabe sollte den Modifikator enthalten",
            formatted.contains("Mod=2")
        )
    }
}

