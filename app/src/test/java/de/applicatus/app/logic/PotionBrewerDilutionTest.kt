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
        createdDate = "1. Praios 1040 BF",
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
        // Tränke können jetzt über A hinaus verdünnt werden (→ X)
        val potionQualityA = testPotion.copy(actualQuality = PotionQuality.A)
        
        // A kann um 1 Stufe verdünnt werden → X (wirkungslos)
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = potionQualityA,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 5
        )
        
        assertNotNull(result)
        assertEquals(2, result.numberOfPotions)
        
        if (result.success) {
            assertEquals(PotionQuality.X, result.newQuality)
        }
    }
    
    @Test
    fun testDilutionQualityF() {
        // F (Index 6) kann um bis zu 6 Stufen verdünnt werden (bis X)
        val potionQualityF = testPotion.copy(actualQuality = PotionQuality.F)
        
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = potionQualityF,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 6,
            facilitationFromAnalysis = 5
        )
        
        assertNotNull(result)
        assertEquals(7, result.numberOfPotions)  // Original + 6 neue = 7 Tränke
        
        if (result.success) {
            assertEquals(PotionQuality.X, result.newQuality)
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
        // Misslungene Tränke (M) können verdünnt werden (Spieler weiß ja nicht, dass es M ist)
        // Aber das Ergebnis ist immer M
        val misslungaPotion = testPotion.copy(actualQuality = PotionQuality.M)
        
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = misslungaPotion,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 0
        )
        
        // Ergebnis ist immer M
        assertEquals(PotionQuality.M, result.newQuality)
        assertFalse(result.success)
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
        // Verdünnungsstufen müssen zwischen 1 und 10 liegen
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
                dilutionSteps = 11,
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
        // Spieler sieht nur minimale Informationen
        assertTrue(formatted.contains("Verdünnung abgeschlossen"))
        assertTrue(formatted.contains("Anzahl Tränke:"))
        
        // Spieler sieht KEINE Probe-Details
        assertFalse(formatted.contains("Verdünnungsprobe:"))
        assertFalse(formatted.contains("Erfolg"))
        assertFalse(formatted.contains("Misserfolg"))
        assertFalse(formatted.contains("TaP*"))
        assertFalse(formatted.contains("Würfe:"))
        assertFalse(formatted.contains("Qualität"))
    }
    
    @Test
    fun testDilutionBeyondA() {
        // A kann um mehr als 1 Stufe verdünnt werden → X
        val potionQualityA = testPotion.copy(actualQuality = PotionQuality.A)
        
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = potionQualityA,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 3,
            facilitationFromAnalysis = 5
        )
        
        assertNotNull(result)
        assertEquals(4, result.numberOfPotions)
        
        if (result.success) {
            assertEquals(PotionQuality.X, result.newQuality)
        }
    }
    
    @Test
    fun testDilutionQualityX() {
        // X (wirkungslos) kann nicht weiter verdünnt werden
        val potionQualityX = testPotion.copy(actualQuality = PotionQuality.X)
        
        // X ist bei Index 0, Verdünnung würde negativen Index ergeben
        val result = PotionBrewer.dilutePotion(
            character = testCharacter,
            potion = potionQualityX,
            recipe = testRecipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 5
        )
        
        assertNotNull(result)
        
        if (result.success) {
            assertEquals(PotionQuality.X, result.newQuality)
        }
    }
}
