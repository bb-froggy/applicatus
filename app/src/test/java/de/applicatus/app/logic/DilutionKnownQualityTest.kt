package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.KnownQualityLevel
import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.talent.Talent
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für die Verdünnung mit Qualitätsanzeige.
 * 
 * Bug: Nach einer 1:2 Verdünnung eines F-Tranks wird immer noch F angezeigt,
 * obwohl die Qualität E sein müsste.
 * 
 * Ursache: Bei der Verdünnung wird zwar `actualQuality` aktualisiert,
 * aber `knownExactQuality` bleibt auf dem alten Wert. Die UI zeigt
 * die bekannte Qualität an, nicht die tatsächliche.
 * 
 * Fix: Bei erfolgreicher Verdünnung muss auch `knownExactQuality` 
 * auf die neue Qualität gesetzt werden (wenn sie vorher bekannt war).
 */
class DilutionKnownQualityTest {
    
    private val testCharacter = Character(
        id = 1,
        name = "Test Alchemist",
        mu = 12, kl = 14, inValue = 13, ch = 10, ff = 12, ge = 11, ko = 12, kk = 11,
        hasAe = true,
        maxAe = 30,
        currentAe = 30,
        alchemySkill = 10,
        alchemyIsMagicalMastery = false
    )
    
    private val testRecipe = Recipe(
        id = 1,
        name = "Heiltrank",
        brewingDifficulty = 5,
        analysisDifficulty = 3,
        lab = Laboratory.WITCHES_KITCHEN,
        shelfLife = "1 Jahr",
        appearance = "goldgelb, glitzernd"
    )
    
    @Test
    fun `dilution of quality F should result in quality E on one step`() {
        val potionF = Potion(
            id = 1,
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.F,  // Schlechteste Qualität
            knownExactQuality = PotionQuality.F,  // Spieler kennt die genaue Qualität
            knownQualityLevel = KnownQualityLevel.EXACT,
            appearance = "goldgelb, glitzernd",
            expiryDate = "1. Praios 1041 BF",
            createdDate = "1. Praios 1040 BF"
        )
        
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = potionF,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,  // 1:2 Verdünnung = 1 Stufe besser
            facilitationFromAnalysis = 0,
            magicalMasteryAsp = 0
        )
        
        if (result.success) {
            assertEquals(
                "Bei erfolgreicher Verdünnung sollte F zu E werden",
                PotionQuality.E,
                result.newQuality
            )
        } else {
            assertEquals(
                "Bei Misserfolg sollte die Qualität M (verdorben) sein",
                PotionQuality.M,
                result.newQuality
            )
        }
    }
    
    @Test
    fun `dilution should update knownExactQuality when previously known`() {
        // Dieser Test dokumentiert das erwartete Verhalten:
        // Wenn der Spieler die Qualität kannte (knownExactQuality != null),
        // sollte nach einer erfolgreichen Verdünnung auch die bekannte Qualität
        // auf die neue Qualität gesetzt werden.
        
        val potionF = Potion(
            id = 1,
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.F,
            knownExactQuality = PotionQuality.F,  // Spieler kennt die Qualität
            knownQualityLevel = KnownQualityLevel.EXACT,
            appearance = "goldgelb",
            expiryDate = "1. Praios 1041 BF",
            createdDate = "1. Praios 1040 BF"
        )
        
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = potionF,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 0,
            magicalMasteryAsp = 0
        )
        
        // Das DilutionResult gibt nur die neue Qualität zurück.
        // Der Fix muss im ViewModel/Repository erfolgen, wo der Trank aktualisiert wird.
        // Dieser Test dokumentiert das erwartete Verhalten.
        
        if (result.success) {
            // Die neue Qualität sollte E sein
            assertEquals(PotionQuality.E, result.newQuality)
            
            // WICHTIG: Wenn die Qualität vorher bekannt war, sollte sie auch nach
            // der Verdünnung bekannt sein. Das ViewModel muss sicherstellen, dass
            // knownExactQuality ebenfalls aktualisiert wird.
        }
    }
    
    @Test
    fun `quality ordinal values are correct for dilution calculation`() {
        // Verifiziere, dass die Qualitätsstufen in der richtigen Reihenfolge sind
        // X=0, A=1, B=2, C=3, D=4, E=5, F=6, M=7
        assertEquals(0, PotionQuality.X.ordinal)
        assertEquals(1, PotionQuality.A.ordinal)
        assertEquals(2, PotionQuality.B.ordinal)
        assertEquals(3, PotionQuality.C.ordinal)
        assertEquals(4, PotionQuality.D.ordinal)
        assertEquals(5, PotionQuality.E.ordinal)
        assertEquals(6, PotionQuality.F.ordinal)
        assertEquals(7, PotionQuality.M.ordinal)
        
        // F - 1 Stufe = E
        val fOrdinal = PotionQuality.F.ordinal  // 6
        val expectedE = PotionQuality.values()[fOrdinal - 1]  // Index 5 = E
        assertEquals(PotionQuality.E, expectedE)
    }
}
