package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.talent.Talent
import org.junit.Assert.*
import org.junit.Test

class PotionBrewerDilutionTest {
    
    private val testCharacter = Character(
        id = 1,
        name = "Test Alchemist",
        mu = 12,
        kl = 14,
        inValue = 13,
        ch = 10,
        ff = 12,
        ge = 11,
        ko = 12,
        kk = 11,
        hasAe = true,
        maxAe = 30,
        currentAe = 30,
        alchemySkill = 10,
        alchemyIsMagicalMastery = true,
        cookingPotionsSkill = 8,
        cookingPotionsIsMagicalMastery = false
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
    
    private val testPotion = Potion(
        id = 1,
        characterId = 1,
        recipeId = 1,
        actualQuality = PotionQuality.D,  // Durchschnittlich
        appearance = "goldgelb, glitzernd",
        expiryDate = "1. Praios 1041 BF",
        bestStructureAnalysisFacilitation = 10  // 10 TaP* aus vorheriger Analyse → 5 Erleichterung
    )
    
    @Test
    fun testSuccessfulDilution() {
        // Bei erfolgreichem Verdünnen wird die Qualität um die Anzahl der Stufen reduziert
        // D (Index 3) - 2 Stufen = B (Index 1)
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = testPotion,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 2,
            facilitationFromAnalysis = 5,  // Halbe TaP* aufgerundet: (10 + 1) / 2 = 5
            magicalMasteryAsp = 0
        )
        
        // Ergebnis prüfen
        assertNotNull(result)
        assertEquals(3, result.numberOfPotions)  // Original + 2 neue = 3 Tränke
        
        if (result.success) {
            assertEquals(PotionQuality.B, result.newQuality)
        } else {
            assertEquals(PotionQuality.M, result.newQuality)
        }
    }
    
    @Test
    fun testDilutionWithMagicalMastery() {
        // Mit Magischem Meisterhandwerk: 3 AsP → +6 TaW
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = testPotion,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 5,
            magicalMasteryAsp = 3
        )
        
        assertNotNull(result)
        assertEquals(2, result.numberOfPotions)
        
        // Mit höherem effektiven TaW sollte die Erfolgswahrscheinlichkeit steigen
        assertTrue(result.probeResult.qualityPoints >= 0 || !result.probeResult.success)
    }
    
    @Test
    fun testDilutionMaxSteps() {
        // Maximale Verdünnung: A (Index 0) kann um 5 Stufen verdünnt werden
        val potionQualityA = testPotion.copy(actualQuality = PotionQuality.A)
        
        // Sollte nicht funktionieren - A kann nicht verdünnt werden (würde über F gehen)
        assertThrows(IllegalArgumentException::class.java) {
            PotionBrewer.dilutePotion(
                character = testCharacter,
                potion = potionQualityA,
                recipe = testRecipe,
                talent = Talent.ALCHEMY,
                dilutionSteps = 1,
                facilitationFromAnalysis = 0
            )
        }
    }
    
    @Test
    fun testDilutionQualityF() {
        // F (Index 5) kann um bis zu 5 Stufen verdünnt werden (bis A)
        val potionQualityF = testPotion.copy(actualQuality = PotionQuality.F)
        
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = potionQualityF,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 5,
            facilitationFromAnalysis = 5
        )
        
        assertNotNull(result)
        assertEquals(6, result.numberOfPotions)  // Original + 5 neue = 6 Tränke
        
        if (result.success) {
            assertEquals(PotionQuality.A, result.newQuality)
        }
    }
    
    @Test
    fun testDilutionWithCookingPotions() {
        // Kochen (Tränke) ohne Magisches Meisterhandwerk
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = testPotion,
            recipe = testRecipe,
            talent = Talent.COOKING_POTIONS,
            dilutionSteps = 1,
            facilitationFromAnalysis = 5
        )
        
        assertNotNull(result)
        assertEquals(2, result.numberOfPotions)
    }
    
    @Test
    fun testCannotDiluteMislunga() {
        // Misslungene Tränke (M) können nicht verdünnt werden
        val misslungaPotion = testPotion.copy(actualQuality = PotionQuality.M)
        
        assertThrows(IllegalArgumentException::class.java) {
            PotionBrewer.dilutePotion(
                character = testCharacter,
                potion = misslungaPotion,
                recipe = testRecipe,
                talent = Talent.ALCHEMY,
                dilutionSteps = 1,
                facilitationFromAnalysis = 0
            )
        }
    }
    
    @Test
    fun testDilutionModifierCalculation() {
        // Gesamtmodifikator = Brauschwierigkeit - Erleichterung
        // 5 - 5 = 0
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = testPotion,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 5
        )
        
        assertEquals(0, result.totalModifier)
    }
    
    @Test
    fun testDilutionWithoutFacilitation() {
        // Ohne Erleichterung aus Analyse
        val potionNoAnalysis = testPotion.copy(bestStructureAnalysisFacilitation = 0)
        
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = potionNoAnalysis,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 0
        )
        
        assertEquals(5, result.totalModifier)  // Nur Brauschwierigkeit
    }
    
    @Test
    fun testInvalidTalent() {
        // Andere Talente als ALCHEMY oder COOKING_POTIONS sind nicht erlaubt
        assertThrows(IllegalArgumentException::class.java) {
            PotionBrewer.dilutePotion(
                character = testCharacter,
                potion = testPotion,
                recipe = testRecipe,
                talent = Talent.SELF_CONTROL,
                dilutionSteps = 1,
                facilitationFromAnalysis = 0
            )
        }
    }
    
    @Test
    fun testInvalidDilutionSteps() {
        // Verdünnungsstufen müssen zwischen 1 und 6 liegen
        assertThrows(IllegalArgumentException::class.java) {
            PotionBrewer.dilutePotion(
                character = testCharacter,
                potion = testPotion,
                recipe = testRecipe,
                talent = Talent.ALCHEMY,
                dilutionSteps = 0,
                facilitationFromAnalysis = 0
            )
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            PotionBrewer.dilutePotion(
                character = testCharacter,
                potion = testPotion,
                recipe = testRecipe,
                talent = Talent.ALCHEMY,
                dilutionSteps = 7,
                facilitationFromAnalysis = 0
            )
        }
    }
    
    @Test
    fun testMagicalMasteryAspLimit() {
        // Max AsP für Magisches Meisterhandwerk = TaW (1:2 Verhältnis, max 2x TaW)
        assertThrows(IllegalArgumentException::class.java) {
            PotionBrewer.dilutePotion(
                character = testCharacter,
                potion = testPotion,
                recipe = testRecipe,
                talent = Talent.ALCHEMY,
                dilutionSteps = 1,
                facilitationFromAnalysis = 0,
                magicalMasteryAsp = 11  // Mehr als TaW (10)
            )
        }
    }
    
    @Test
    fun testMagicalMasteryNotAvailable() {
        // Kochen (Tränke) hat kein Magisches Meisterhandwerk
        assertThrows(IllegalArgumentException::class.java) {
            PotionBrewer.dilutePotion(
                character = testCharacter,
                potion = testPotion,
                recipe = testRecipe,
                talent = Talent.COOKING_POTIONS,
                dilutionSteps = 1,
                facilitationFromAnalysis = 0,
                magicalMasteryAsp = 3
            )
        }
    }
    
    @Test
    fun testFormatDilutionResult() {
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = testPotion,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 2,
            facilitationFromAnalysis = 5
        )
        
        val formatted = PotionBrewer.formatDilutionResult(result, isGameMaster = true)
        
        assertNotNull(formatted)
        assertTrue(formatted.contains("Verdünnungsprobe:"))
        assertTrue(formatted.contains("Anzahl Tränke: 3"))
        
        if (result.success) {
            assertTrue(formatted.contains("Erfolg"))
            assertTrue(formatted.contains("Neue Qualität:"))
        } else {
            assertTrue(formatted.contains("Misserfolg"))
        }
    }
    
    @Test
    fun testFormatDilutionResultForPlayer() {
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = testPotion,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 5
        )
        
        val formatted = PotionBrewer.formatDilutionResult(result, isGameMaster = false)
        
        assertNotNull(formatted)
        assertTrue(formatted.contains("Verdünnungsprobe:"))
        
        if (result.success) {
            assertTrue(formatted.contains("(Qualität unbekannt - nur Spielleiter sichtbar)"))
        }
    }
}
