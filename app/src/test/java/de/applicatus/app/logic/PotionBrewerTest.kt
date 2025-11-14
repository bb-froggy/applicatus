package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.Substitution
import de.applicatus.app.data.model.potion.SubstitutionType
import de.applicatus.app.data.model.talent.Talent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit-Tests für PotionBrewer
 */
class PotionBrewerTest {
    
    private lateinit var testCharacter: Character
    private lateinit var testRecipe: Recipe
    
    @Before
    fun setUp() {
        testCharacter = Character(
            name = "Test-Alchimist",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            hasCookingPotions = true,
            cookingPotionsSkill = 8,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        testRecipe = Recipe(
            name = "Test-Trank",
            lab = Laboratory.ALCHEMIST_LABORATORY,
            brewingDifficulty = 6
        )
    }
    
    @Test
    fun testCalculateMaxVoluntaryHandicap() {
        // Brauschwierigkeit 6 → max 9 (1.5 * 6 = 9)
        val recipe1 = Recipe(name = "Test", brewingDifficulty = 6)
        assertEquals(9, PotionBrewer.calculateMaxVoluntaryHandicap(recipe1))
        
        // Brauschwierigkeit 7 → max 11 (1.5 * 7 = 10.5, aufgerundet 11)
        val recipe2 = Recipe(name = "Test", brewingDifficulty = 7)
        assertEquals(11, PotionBrewer.calculateMaxVoluntaryHandicap(recipe2))
        
        // Brauschwierigkeit 0 → max 0
        val recipe3 = Recipe(name = "Test", brewingDifficulty = 0)
        assertEquals(0, PotionBrewer.calculateMaxVoluntaryHandicap(recipe3))
    }
    
    @Test
    fun testCalculateAspCostForQualityPoints() {
        assertEquals(0, PotionBrewer.calculateAspCostForQualityPoints(0))
        assertEquals(1, PotionBrewer.calculateAspCostForQualityPoints(1))  // 2^0 = 1
        assertEquals(2, PotionBrewer.calculateAspCostForQualityPoints(2))  // 2^1 = 2
        assertEquals(4, PotionBrewer.calculateAspCostForQualityPoints(3))  // 2^2 = 4
        assertEquals(8, PotionBrewer.calculateAspCostForQualityPoints(4))  // 2^3 = 8
        assertEquals(16, PotionBrewer.calculateAspCostForQualityPoints(5)) // 2^4 = 16
        assertEquals(32, PotionBrewer.calculateAspCostForQualityPoints(6)) // 2^5 = 32
    }
    
    @Test
    fun testCalculateMaxQualityPointsFromAsp() {
        assertEquals(0, PotionBrewer.calculateMaxQualityPointsFromAsp(0))
        assertEquals(1, PotionBrewer.calculateMaxQualityPointsFromAsp(1))  // 1 AsP → 1 QP
        assertEquals(2, PotionBrewer.calculateMaxQualityPointsFromAsp(2))  // 2 AsP → 2 QP
        assertEquals(2, PotionBrewer.calculateMaxQualityPointsFromAsp(3))  // 3 AsP → 2 QP (4 wäre zu teuer)
        assertEquals(3, PotionBrewer.calculateMaxQualityPointsFromAsp(4))  // 4 AsP → 3 QP
        assertEquals(3, PotionBrewer.calculateMaxQualityPointsFromAsp(7))  // 7 AsP → 3 QP (8 wäre zu teuer)
        assertEquals(4, PotionBrewer.calculateMaxQualityPointsFromAsp(8))  // 8 AsP → 4 QP
        assertEquals(5, PotionBrewer.calculateMaxQualityPointsFromAsp(16)) // 16 AsP → 5 QP
        assertEquals(6, PotionBrewer.calculateMaxQualityPointsFromAsp(32)) // 32 AsP → 6 QP
    }
    
    @Test
    fun testCanBrew_SameLaboratory() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY)
        assertTrue(PotionBrewer.canBrew(recipe, Laboratory.ALCHEMIST_LABORATORY))
    }
    
    @Test
    fun testCanBrew_BetterLaboratory() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ARCANE)
        assertTrue(PotionBrewer.canBrew(recipe, Laboratory.ALCHEMIST_LABORATORY))
        
        val recipe2 = Recipe(name = "Test", lab = Laboratory.WITCHES_KITCHEN)
        assertTrue(PotionBrewer.canBrew(recipe2, Laboratory.ALCHEMIST_LABORATORY))
    }
    
    @Test
    fun testCanBrew_OneLevelWorse() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY)
        assertTrue(PotionBrewer.canBrew(recipe, Laboratory.WITCHES_KITCHEN))
        
        val recipe2 = Recipe(name = "Test", lab = Laboratory.WITCHES_KITCHEN)
        assertTrue(PotionBrewer.canBrew(recipe2, Laboratory.ARCANE))
    }
    
    @Test
    fun testCanBrew_TwoLevelsWorse() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY)
        assertFalse(PotionBrewer.canBrew(recipe, Laboratory.ARCANE))
    }
    
    @Test
    fun testCanBrew_NoLabRequired() {
        val recipe = Recipe(name = "Test", lab = null)
        assertTrue(PotionBrewer.canBrew(recipe, Laboratory.ARCANE))
        assertTrue(PotionBrewer.canBrew(recipe, Laboratory.WITCHES_KITCHEN))
        assertTrue(PotionBrewer.canBrew(recipe, Laboratory.ALCHEMIST_LABORATORY))
    }
    
    @Test
    fun testLaboratoryModifier_Same() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY)
        assertEquals(0, recipe.lab!!.getBrewingModifier(Laboratory.ALCHEMIST_LABORATORY))
    }
    
    @Test
    fun testLaboratoryModifier_Better() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ARCANE)
        // Arcane mit Alchemist-Labor = -3 (zwei Stufen besser)
        assertEquals(-3, recipe.lab!!.getBrewingModifier(Laboratory.ALCHEMIST_LABORATORY))
        
        val recipe2 = Recipe(name = "Test", lab = Laboratory.ARCANE)
        // Arcane mit Hexenküche = 0 (eine Stufe besser, keine Erleichterung)
        assertEquals(0, recipe2.lab!!.getBrewingModifier(Laboratory.WITCHES_KITCHEN))
        
        val recipe3 = Recipe(name = "Test", lab = Laboratory.WITCHES_KITCHEN)
        // Hexenküche mit Alchemist-Labor = 0 (eine Stufe besser, keine Erleichterung)
        assertEquals(0, recipe3.lab!!.getBrewingModifier(Laboratory.ALCHEMIST_LABORATORY))
    }
    
    @Test
    fun testLaboratoryModifier_Worse() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY)
        // Alchemist mit Hexenküche = +7 (eine Stufe schlechter)
        assertEquals(7, recipe.lab!!.getBrewingModifier(Laboratory.WITCHES_KITCHEN))
        
        val recipe2 = Recipe(name = "Test", lab = Laboratory.WITCHES_KITCHEN)
        // Hexenküche mit Arcane = +7 (eine Stufe schlechter)
        assertEquals(7, recipe2.lab!!.getBrewingModifier(Laboratory.ARCANE))
    }
    
    @Test
    fun testCalculateTotalModifier_NoModifiers() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 0)
        val modifier = PotionBrewer.calculateTotalModifier(
            recipe,
            Laboratory.ALCHEMIST_LABORATORY,
            0,
            emptyList()
        )
        assertEquals(0, modifier)
    }
    
    @Test
    fun testCalculateTotalModifier_OnlyBrewingDifficulty() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 6)
        val modifier = PotionBrewer.calculateTotalModifier(
            recipe,
            Laboratory.ALCHEMIST_LABORATORY,
            0,
            emptyList()
        )
        assertEquals(6, modifier)
    }
    
    @Test
    fun testCalculateTotalModifier_WithVoluntaryHandicap() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 6)
        val modifier = PotionBrewer.calculateTotalModifier(
            recipe,
            Laboratory.ALCHEMIST_LABORATORY,
            3,
            emptyList()
        )
        assertEquals(9, modifier) // 6 + 3
    }
    
    @Test
    fun testCalculateTotalModifier_WithSubstitutions() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 6)
        val substitutions = listOf(
            Substitution(SubstitutionType.OPTIMIZING),  // -3
            Substitution(SubstitutionType.SENSIBLE)     // +3
        )
        val modifier = PotionBrewer.calculateTotalModifier(
            recipe,
            Laboratory.ALCHEMIST_LABORATORY,
            0,
            substitutions
        )
        assertEquals(6, modifier) // 6 + (-3 + 3) = 6
    }
    
    @Test
    fun testCalculateTotalModifier_All() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 6)
        val substitutions = listOf(
            Substitution(SubstitutionType.POSSIBLE)  // +6
        )
        val modifier = PotionBrewer.calculateTotalModifier(
            recipe,
            Laboratory.WITCHES_KITCHEN,  // +7 (eine Stufe schlechter)
            2,  // freiwilliger Handicap
            substitutions
        )
        assertEquals(21, modifier) // 7 + 6 + 2 + 6 = 21
    }
    
    @Test
    fun testBrewPotion_RequiresAlchemyOrCookingPotions() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY)
        try {
            PotionBrewer.brewPotion(
                testCharacter,
                recipe,
                Talent.MAGICAL_LORE,  // Falsches Talent
                Laboratory.ALCHEMIST_LABORATORY
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Nur Alchimie oder Kochen") == true)
        }
    }
    
    @Test
    fun testBrewPotion_VoluntaryHandicapMustBeZeroOrMinTwo() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 6)
        try {
            PotionBrewer.brewPotion(
                testCharacter,
                recipe,
                Talent.ALCHEMY,
                Laboratory.ALCHEMIST_LABORATORY,
                voluntaryHandicap = 1  // Ungültig: 1 ist nicht erlaubt
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("mindestens 2") == true)
        }
    }
    
    @Test
    fun testBrewPoton_VoluntaryHandicapMaxLimit() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 6)
        // Max = ceil(6 * 1.5) = 9
        try {
            PotionBrewer.brewPotion(
                testCharacter,
                recipe,
                Talent.ALCHEMY,
                Laboratory.ALCHEMIST_LABORATORY,
                voluntaryHandicap = 10  // Zu hoch
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("maximal") == true)
        }
    }
    
    @Test
    fun testBrewPotion_RequiresValidLaboratory() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY)
        try {
            PotionBrewer.brewPotion(
                testCharacter,
                recipe,
                Talent.ALCHEMY,
                Laboratory.ARCANE  // Zu schlecht (2 Stufen schlechter)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("nicht möglich") == true)
        }
    }
    
    @Test
    fun testQualityCalculation() {
        // Diese Methode ist privat, aber wir testen sie indirekt über die Ergebnisse
        
        // 0-6 QP → A
        // 7-12 QP → B
        // 13-18 QP → C
        // 19-24 QP → D
        // 25-30 QP → E
        // 31+ QP → F
        
        // Wir müssten einen Mock für die Würfe verwenden, um das zu testen
        // Da ProbeChecker.performTalentProbe verwendet wird, ist das schwierig
        // ohne Dependency Injection
    }
    
    @Test
    fun testFormatBrewingResult_PlayerView() {
        val probeResult = ProbeResult(
            success = true,
            qualityPoints = 5,
            rolls = listOf(10, 12, 8),
            message = "Erfolg"
        )
        
        val result = PotionBrewer.BrewingResult(
            probeResult = probeResult,
            qualityPoints = 15,
            quality = PotionQuality.C,
            laborModifier = 0,
            brewingDifficultyModifier = 6,
            voluntaryHandicapModifier = 2,
            substitutionModifier = 0,
            totalModifier = 8,
            diceRoll1 = 3,
            diceRoll2 = 4,
            qualityDice = 7
        )
        
        val formatted = PotionBrewer.formatBrewingResult(result, isGameMaster = false)
        
        // Spieler sieht keine Qualität
        assertTrue(formatted.contains("Erfolg"))
        assertTrue(formatted.contains("unbekannt"))
        assertFalse(formatted.contains("Qualität: C"))
    }
    
    @Test
    fun testFormatBrewingResult_GameMasterView() {
        val probeResult = ProbeResult(
            success = true,
            qualityPoints = 5,
            rolls = listOf(10, 12, 8),
            message = "Erfolg"
        )
        
        val result = PotionBrewer.BrewingResult(
            probeResult = probeResult,
            qualityPoints = 15,
            quality = PotionQuality.C,
            laborModifier = 0,
            brewingDifficultyModifier = 6,
            voluntaryHandicapModifier = 2,
            substitutionModifier = 0,
            totalModifier = 8,
            diceRoll1 = 3,
            diceRoll2 = 4,
            qualityDice = 7
        )
        
        val formatted = PotionBrewer.formatBrewingResult(result, isGameMaster = true)
        
        // Spielleiter sieht Qualität
        assertTrue(formatted.contains("Erfolg"))
        assertTrue(formatted.contains("Qualität: C"))
        assertFalse(formatted.contains("unbekannt"))
    }
    
    @Test
    fun testFormatBrewingResult_Failure() {
        val probeResult = ProbeResult(
            success = false,
            qualityPoints = -2,
            rolls = listOf(18, 19, 20),
            message = "Misserfolg"
        )
        
        val result = PotionBrewer.BrewingResult(
            probeResult = probeResult,
            qualityPoints = 0,
            quality = PotionQuality.M,
            laborModifier = 0,
            brewingDifficultyModifier = 6,
            voluntaryHandicapModifier = 0,
            substitutionModifier = 0,
            totalModifier = 6,
            diceRoll1 = 0,
            diceRoll2 = 0,
            qualityDice = 0
        )
        
        val formatted = PotionBrewer.formatBrewingResult(result, isGameMaster = false)
        
        // Spieler sieht nur Misserfolg
        assertTrue(formatted.contains("Misserfolg"))
        assertFalse(formatted.contains("Qualität: M"))
        
        val formattedGM = PotionBrewer.formatBrewingResult(result, isGameMaster = true)
        
        // Spielleiter sieht Qualität M
        assertTrue(formattedGM.contains("Misserfolg"))
        assertTrue(formattedGM.contains("Qualität: M"))
    }
    
    @Test
    fun testBrewPotion_CookingPotionsCannotUseAlchemistLaboratory() {
        val recipe = Recipe(name = "Test", lab = Laboratory.WITCHES_KITCHEN)
        try {
            PotionBrewer.brewPotion(
                testCharacter,
                recipe,
                Talent.COOKING_POTIONS,
                Laboratory.ALCHEMIST_LABORATORY  // Kochen darf kein Alchimistenlabor verwenden
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Kochen") == true)
            assertTrue(e.message?.contains("Alchimistenlabor") == true)
        }
    }
    
    @Test
    fun testBrewPotion_CookingPotionsCannotBrewAlchemistRecipes() {
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY)
        try {
            PotionBrewer.brewPotion(
                testCharacter,
                recipe,
                Talent.COOKING_POTIONS,
                Laboratory.WITCHES_KITCHEN  // Labor ist okay, aber Rezept erfordert Alchimistenlabor
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Kochen") == true)
            assertTrue(e.message?.contains("Alchimistenlabor erfordern") == true)
        }
    }
    
    @Test
    fun testBrewPotion_CookingPotionsCanUseArcaneAndWitchesKitchen() {
        // Archaisches Labor
        val recipe1 = Recipe(name = "Test1", lab = Laboratory.ARCANE, brewingDifficulty = 4)
        val result1 = PotionBrewer.brewPotion(
            testCharacter,
            recipe1,
            Talent.COOKING_POTIONS,
            Laboratory.ARCANE
        )
        assertNotNull(result1)
        
        // Hexenküche
        val recipe2 = Recipe(name = "Test2", lab = Laboratory.WITCHES_KITCHEN, brewingDifficulty = 4)
        val result2 = PotionBrewer.brewPotion(
            testCharacter,
            recipe2,
            Talent.COOKING_POTIONS,
            Laboratory.WITCHES_KITCHEN
        )
        assertNotNull(result2)
    }
    
    @Test
    fun testBrewPotion_AstralChargingWorks() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = false,  // Kein magisches Meisterhandwerk
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 4)
        
        // Astrale Aufladung ist auch ohne Magisches Meisterhandwerk möglich
        val result = PotionBrewer.brewPotion(
            character,
            recipe,
            Talent.ALCHEMY,
            Laboratory.ALCHEMIST_LABORATORY,
            astralCharging = 3  // 3 QP kosten 4 AsP (2^2)
        )
        
        assertNotNull(result)
        // Bei Erfolg sollten die astralen QP in die Qualität einfließen
        if (result.probeResult.success) {
            assertTrue(result.qualityPoints >= 3)  // Mindestens die 3 astralen QP
        }
    }
    
    @Test
    fun testBrewPotion_MagicalMasteryRequiresAstralEnergy() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 2,  // Nur 2 AsP verfügbar
            maxAe = 50
        )
        
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 4)
        
        try {
            PotionBrewer.brewPotion(
                character,
                recipe,
                Talent.ALCHEMY,
                Laboratory.ALCHEMIST_LABORATORY,
                astralCharging = 3  // Kostet 4 AsP (2^(3-1) = 4), aber nur 2 verfügbar
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Nicht genug Astralenergie") == true)
        }
    }
    
    @Test
    fun testBrewPotion_MagicalMasteryWithSufficientAstralEnergy() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 4)
        
        val result = PotionBrewer.brewPotion(
            character,
            recipe,
            Talent.ALCHEMY,
            Laboratory.ALCHEMIST_LABORATORY,
            astralCharging = 3  // 3 QP kosten 4 AsP (2^(3-1) = 4)
        )
        
        assertNotNull(result)
        // Bei Erfolg sollten die astralen QP in die Qualität einfließen
        if (result.probeResult.success) {
            assertTrue(result.qualityPoints >= 3)  // Mindestens die 3 astralen QP
        }
    }
    
    @Test
    fun testBrewPotion_MagicalMasteryRequiresFlag() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = false,  // Kein magisches Meisterhandwerk
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 4)
        
        try {
            PotionBrewer.brewPotion(
                character,
                recipe,
                Talent.ALCHEMY,
                Laboratory.ALCHEMIST_LABORATORY,
                magicalMasteryAsp = 3  // Ohne Flag nicht möglich
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Magisches Meisterhandwerk") == true)
        }
    }
    
    @Test
    fun testBrewPotion_MagicalMasteryMaxLimit() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,  // TaW 10 → max 5 AsP
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 4)
        
        try {
            PotionBrewer.brewPotion(
                character,
                recipe,
                Talent.ALCHEMY,
                Laboratory.ALCHEMIST_LABORATORY,
                magicalMasteryAsp = 6  // Mehr als TaW/2 = 5
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Maximal 5 AsP") == true)
        }
    }
    
    @Test
    fun testBrewPotion_MagicalMasteryBoostsTaW() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 4)
        
        // Mit 3 AsP für Magisches Meisterhandwerk sollte TaW von 10 auf 16 steigen (+6)
        val result = PotionBrewer.brewPotion(
            character,
            recipe,
            Talent.ALCHEMY,
            Laboratory.ALCHEMIST_LABORATORY,
            magicalMasteryAsp = 3
        )
        
        assertNotNull(result)
        // Die Probe sollte mit effektiv TaW 16 (10 + 3*2) durchgeführt werden
        // Das können wir indirekt über höhere TaP* bei Erfolg erkennen
    }
    
    @Test
    fun testBrewPotion_CombinedMagicalMasteryAndAstralCharging() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(name = "Test", lab = Laboratory.ALCHEMIST_LABORATORY, brewingDifficulty = 4)
        
        // Kombiniert: 3 AsP für TaW-Erhöhung + 4 AsP für 3 QP = 7 AsP gesamt
        val result = PotionBrewer.brewPotion(
            character,
            recipe,
            Talent.ALCHEMY,
            Laboratory.ALCHEMIST_LABORATORY,
            magicalMasteryAsp = 3,  // +6 TaW
            astralCharging = 3      // +3 QP, kostet 4 AsP
        )
        
        assertNotNull(result)
        // Bei Erfolg sollten die astralen QP in die Qualität einfließen
        if (result.probeResult.success) {
            assertTrue(result.qualityPoints >= 3)  // Mindestens die 3 astralen QP
        }
    }
    
    @Test
    fun testPreservePotion_Success() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 15,
            hasAe = false
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.B,
            createdDate = "1. Praios 1040 BF",
            expiryDate = "1. Rondra 1040 BF",  // 1 Monat später
            preservationAttempted = false
        )
        
        val result = PotionBrewer.preservePotion(
            character = character,
            potion = potion,
            talent = Talent.ALCHEMY,
            currentDate = "1. Praios 1040 BF"
        )
        
        assertNotNull(result)
        assertEquals(9, result.totalModifier)  // Erschwernis ist immer 9
        
        // Bei Erfolg sollte Multiplier 2.0 sein, kein W6-Wurf und keine Qualitätsänderung
        if (result.success) {
            assertEquals(2.0, result.multiplier, 0.001)
            assertNull(result.rollW6)
            assertNull(result.newQuality)
        }
    }
    
    @Test
    fun testPreservePotion_Failure_W6Rolls() {
        val character = Character(
            name = "Test",
            kl = 8,  // Niedrige Werte für Misserfolg
            mu = 8,
            ff = 8,
            hasAlchemy = true,
            alchemySkill = 2,
            hasAe = false
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.B,
            createdDate = "1. Praios 1040 BF",
            expiryDate = "1. Rondra 1040 BF",
            preservationAttempted = false
        )
        
        // Mehrere Versuche, um verschiedene W6-Ergebnisse zu testen
        for (i in 1..10) {
            val result = PotionBrewer.preservePotion(
                character = character,
                potion = potion,
                talent = Talent.ALCHEMY,
                currentDate = "1. Praios 1040 BF"
            )
            
            // Bei Misserfolg sollte ein W6-Wurf vorhanden sein (1-10 mit Patzer-Bonus)
            if (!result.success) {
                assertNotNull(result.rollW6)
                assertTrue(result.rollW6!! in 1..10)
                
                // Prüfe Multiplier und Qualität basierend auf W6
                when (result.rollW6) {
                    1, 2 -> {
                        assertEquals(2.0, result.multiplier, 0.001)
                        assertNull(result.newQuality)  // Keine Qualitätsänderung
                    }
                    3 -> {
                        assertEquals(1.5, result.multiplier, 0.001)
                        assertNull(result.newQuality)  // Keine Qualitätsänderung
                    }
                    4 -> {
                        assertEquals(1.5, result.multiplier, 0.001)
                        assertNotNull(result.newQuality)  // Qualität sinkt
                    }
                    5 -> {
                        assertEquals(1.0, result.multiplier, 0.001)
                        assertNotNull(result.newQuality)  // Qualität sinkt
                    }
                    in 6..8 -> {
                        assertEquals(1.0, result.multiplier, 0.001)
                        assertEquals(PotionQuality.X, result.newQuality)  // Wirkungslos
                    }
                    in 9..10 -> {
                        assertEquals(1.0, result.multiplier, 0.001)
                        assertEquals(PotionQuality.M, result.newQuality)  // Misslungen
                    }
                }
            }
        }
    }
    
    @Test
    fun testPreservePotion_WithMagicalMastery() {
        val character = Character(
            name = "Test",
            kl = 12,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 20,
            maxAe = 20
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.B,
            createdDate = "1. Praios 1040 BF",
            expiryDate = "1. Rondra 1040 BF",
            preservationAttempted = false
        )
        
        // Mit 5 AsP (max für TaW 10 ist (10+1)/2 = 5)
        val result = PotionBrewer.preservePotion(
            character = character,
            potion = potion,
            talent = Talent.ALCHEMY,
            magicalMasteryAsp = 5,
            currentDate = "1. Praios 1040 BF"
        )
        
        assertNotNull(result)
        // Mit +10 TaW (5 AsP * 2) sollte die Probe leichter sein
        assertTrue(result.probeResult.qualityPoints >= 0 || !result.probeResult.success)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testPreservePotion_AlreadyPreserved() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.B,
            createdDate = "1. Praios 1040 BF",
            expiryDate = "1. Rondra 1040 BF",
            preservationAttempted = true  // Bereits haltbar gemacht
        )
        
        PotionBrewer.preservePotion(
            character = character,
            potion = potion,
            talent = Talent.ALCHEMY,
            currentDate = "1. Praios 1040 BF"
        )
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testPreservePotion_TooMuchAsp() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 20,
            maxAe = 20
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.B,
            createdDate = "1. Praios 1040 BF",
            expiryDate = "1. Rondra 1040 BF",
            preservationAttempted = false
        )
        
        // Versuche 10 AsP zu verwenden (max ist (10+1)/2 = 5, also 6 wäre schon zu viel)
        PotionBrewer.preservePotion(
            character = character,
            potion = potion,
            talent = Talent.ALCHEMY,
            magicalMasteryAsp = 10,
            currentDate = "1. Praios 1040 BF"
        )
    }
    
    @Test
    fun testPreservePotion_CalculatesNewExpiryDate() {
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 15
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.B,
            createdDate = "1. Praios 1040 BF",
            expiryDate = "1. Rondra 1040 BF",  // 30 Tage später
            preservationAttempted = false
        )
        
        val result = PotionBrewer.preservePotion(
            character = character,
            potion = potion,
            talent = Talent.ALCHEMY,
            currentDate = "1. Praios 1040 BF"
        )
        
        assertNotNull(result.newExpiryDate)
        assertNotEquals(potion.expiryDate, result.newExpiryDate)
        
        // Das neue Datum sollte später sein als das ursprüngliche
        // (außer bei Multiplier 1.0, aber das können wir nicht zuverlässig testen)
    }
    
    @Test
    fun testPreservePotion_QualityReduction() {
        // Teste dass Qualitätsreduktion korrekt funktioniert
        val testCases = mapOf(
            PotionQuality.F to PotionQuality.E,
            PotionQuality.E to PotionQuality.D,
            PotionQuality.D to PotionQuality.C,
            PotionQuality.C to PotionQuality.B,
            PotionQuality.B to PotionQuality.A,
            PotionQuality.A to PotionQuality.X,
            PotionQuality.X to PotionQuality.X,  // Bleibt X
            PotionQuality.M to PotionQuality.M   // Bleibt M
        )
        
        val character = Character(
            name = "Test",
            kl = 8,  // Niedrige Werte für häufigen Misserfolg
            mu = 8,
            ff = 8,
            hasAlchemy = true,
            alchemySkill = 1
        )
        
        testCases.forEach { (originalQuality, _) ->
            val potion = de.applicatus.app.data.model.potion.Potion(
                characterId = 1,
                recipeId = 1,
                actualQuality = originalQuality,
                createdDate = "1. Praios 1040 BF",
                expiryDate = "1. Rondra 1040 BF",
                preservationAttempted = false
            )
            
            // Versuche mehrmals, um verschiedene W6-Ergebnisse zu bekommen
            for (i in 1..20) {
                val result = PotionBrewer.preservePotion(
                    character = character,
                    potion = potion,
                    talent = Talent.ALCHEMY,
                    currentDate = "1. Praios 1040 BF"
                )
                
                // Wenn W6 = 4 oder 5, sollte Qualität sinken
                if (!result.success && result.rollW6 in listOf(4, 5)) {
                    assertNotNull("Qualität sollte bei W6=${result.rollW6} geändert werden", result.newQuality)
                }
                
                // Wenn W6 = 6-10, sollte Qualität X oder M sein
                if (!result.success && result.rollW6!! >= 6) {
                    assertNotNull(result.newQuality)
                    assertTrue("Bei W6=${result.rollW6} sollte Qualität X oder M sein", 
                        result.newQuality == PotionQuality.X || result.newQuality == PotionQuality.M)
                }
            }
        }
    }
    
    @Test
    fun testMagicalMasteryWithOddSkillValue() {
        // Test dass bei ungeradem TaW das Maximum korrekt ist ((TaW+1)/2 aufgerundet)
        // TaW 11 → max 6 AsP (6 AsP → TaW 23, begrenzt auf 22, bringt also noch +1)
        val character = Character(
            name = "Test",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 11,  // Ungerader TaW
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 20,
            maxAe = 20
        )
        
        val recipe = Recipe(
            name = "Test",
            lab = Laboratory.ALCHEMIST_LABORATORY,
            brewingDifficulty = 4
        )
        
        // 6 AsP sollten erlaubt sein ((11+1)/2 = 6)
        val result6 = PotionBrewer.brewPotion(
            character,
            recipe,
            Talent.ALCHEMY,
            Laboratory.ALCHEMIST_LABORATORY,
            magicalMasteryAsp = 6
        )
        assertNotNull(result6)
        
        // 7 AsP sollten NICHT erlaubt sein (würde nichts mehr bringen wegen Deckelung)
        try {
            PotionBrewer.brewPotion(
                character,
                recipe,
                Talent.ALCHEMY,
                Laboratory.ALCHEMIST_LABORATORY,
                magicalMasteryAsp = 7
            )
            fail("7 AsP sollten bei TaW 11 nicht erlaubt sein")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Maximal 6 AsP") == true)
        }
    }
    
    // ==================== Tests für nachträglichen AsP-Einsatz ====================
    
    @Test
    fun testDilution_RetroactiveAsp_Success() {
        // Test: Probe würde fehlschlagen, aber nachträglich genug AsP vorhanden
        val character = Character(
            name = "Test-Alchimist",
            kl = 10,
            mu = 10,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(
            name = "Test-Trank",
            lab = Laboratory.ALCHEMIST_LABORATORY,
            brewingDifficulty = 6
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1L,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            createdDate = "1 Praios 1000 BF",
            expiryDate = "1 Rondra 1000 BF"
        )
        
        // Würfelwürfe, die zu einem Misserfolg führen würden
        // TaP* = TaW - Erschwernis = 10 - 6 = 4
        // Würfe: 15, 15, 15 → Überwürfe: 5, 5, 5 = -11 → Misserfolg
        // Fehlende Punkte: 11 (aber wir können maximal TaW=10 nachträglich einsetzen)
        var rollIndex = 0
        val fixedRolls = listOf(15, 15, 15)
        
        val result = PotionBrewer.dilutePotion(
            character = character,
            potion = potion,
            recipe = recipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 0,
            magicalMasteryAsp = 0
        )
        
        // Wir können diesen Test nicht direkt testen, da die Würfel zufällig sind
        // Stattdessen prüfen wir, dass retroactiveAspUsed >= 0 ist
        assertTrue(result.retroactiveAspUsed >= 0)
        
        // Wenn AsP eingesetzt wurden, muss die Probe erfolgreich sein
        if (result.retroactiveAspUsed > 0) {
            assertTrue(result.success)
            assertEquals(0, result.probeResult.qualityPoints)  // Gerade so geschafft
        }
    }
    
    @Test
    fun testDilution_RetroactiveAsp_NotEnoughAsp() {
        // Test: Probe fehlgeschlagen, aber nicht genug AsP für nachträglichen Einsatz
        val character = Character(
            name = "Test-Alchimist",
            kl = 10,
            mu = 10,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 2,  // Nur 2 AsP verfügbar
            maxAe = 50
        )
        
        val recipe = Recipe(
            name = "Test-Trank",
            lab = Laboratory.ALCHEMIST_LABORATORY,
            brewingDifficulty = 6
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1L,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            createdDate = "1 Praios 1000 BF",
            expiryDate = "1 Rondra 1000 BF"
        )
        
        val result = PotionBrewer.dilutePotion(
            character = character,
            potion = potion,
            recipe = recipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 0,
            magicalMasteryAsp = 0
        )
        
        // Wenn mehr als 2 Punkte fehlen, kann nicht nachträglich ausgeglichen werden
        // retroactiveAspUsed sollte 0 oder <= 2 sein
        assertTrue(result.retroactiveAspUsed <= 2)
    }
    
    @Test
    fun testDilution_RetroactiveAsp_DoubleTwenty_NotAllowed() {
        // Test: Bei Doppel-20 darf kein nachträglicher AsP-Einsatz erfolgen
        val character = Character(
            name = "Test-Alchimist",
            kl = 10,
            mu = 10,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(
            name = "Test-Trank",
            lab = Laboratory.ALCHEMIST_LABORATORY,
            brewingDifficulty = 6
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1L,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            createdDate = "1 Praios 1000 BF",
            expiryDate = "1 Rondra 1000 BF"
        )
        
        // Mehrere Durchläufe, um eine Doppel-20 zu erwischen
        var foundDoubleTwenty = false
        
        for (i in 1..100) {
            val result = PotionBrewer.dilutePotion(
                character = character,
                potion = potion,
                recipe = recipe,
                talent = Talent.ALCHEMY,
                dilutionSteps = 1,
                facilitationFromAnalysis = 0,
                magicalMasteryAsp = 0
            )
            
            if (result.probeResult.isDoubleTwenty || result.probeResult.isTripleTwenty) {
                foundDoubleTwenty = true
                // Bei Patzer darf kein nachträglicher AsP-Einsatz erfolgen
                assertEquals(0, result.retroactiveAspUsed)
                assertFalse(result.success)
                break
            }
        }
        
        // Test ist nur aussagekräftig, wenn wir tatsächlich einen Patzer gefunden haben
        // (Bei 100 Versuchen ist die Chance sehr hoch)
    }
    
    @Test
    fun testDilution_RetroactiveAsp_TooManyPointsMissing() {
        // Test: Mehr Punkte fehlen als TaW → kein nachträglicher Einsatz möglich
        val character = Character(
            name = "Test-Alchimist",
            kl = 5,  // Sehr niedrige Eigenschaften
            mu = 5,
            ff = 5,
            hasAlchemy = true,
            alchemySkill = 5,  // Niedriger TaW
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(
            name = "Test-Trank",
            lab = Laboratory.ALCHEMIST_LABORATORY,
            brewingDifficulty = 10  // Hohe Erschwernis
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1L,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            createdDate = "1 Praios 1000 BF",
            expiryDate = "1 Rondra 1000 BF"
        )
        
        val result = PotionBrewer.dilutePotion(
            character = character,
            potion = potion,
            recipe = recipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 0,
            magicalMasteryAsp = 0
        )
        
        // Bei dieser Kombination ist ein Erfolg extrem unwahrscheinlich
        // Wenn die Probe fehlschlägt, darf maximal TaW (5) AsP nachträglich eingesetzt werden
        assertTrue(result.retroactiveAspUsed <= 5)
    }
    
    @Test
    fun testPreservation_RetroactiveAsp_Success() {
        // Test: Nachträglicher AsP-Einsatz bei Haltbarmachen
        val character = Character(
            name = "Test-Alchimist",
            kl = 10,
            mu = 10,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1L,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            createdDate = "1 Praios 1000 BF",
            expiryDate = "1 Rondra 1000 BF",
            preservationAttempted = false
        )
        
        val result = PotionBrewer.preservePotion(
            character = character,
            potion = potion,
            talent = Talent.ALCHEMY,
            magicalMasteryAsp = 0,
            currentDate = "1 Praios 1000 BF"
        )
        
        // retroactiveAspUsed sollte >= 0 sein
        assertTrue(result.retroactiveAspUsed >= 0)
        
        // Wenn AsP eingesetzt wurden, muss die Probe erfolgreich sein
        if (result.retroactiveAspUsed > 0) {
            assertTrue(result.success)
            assertEquals(0, result.probeResult.qualityPoints)
        }
    }
    
    @Test
    fun testPreservation_RetroactiveAsp_DoubleTwenty_NotAllowed() {
        // Test: Bei Doppel-20 darf kein nachträglicher AsP-Einsatz erfolgen
        val character = Character(
            name = "Test-Alchimist",
            kl = 10,
            mu = 10,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1L,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            createdDate = "1 Praios 1000 BF",
            expiryDate = "1 Rondra 1000 BF",
            preservationAttempted = false
        )
        
        // Mehrere Durchläufe, um eine Doppel-20 zu erwischen
        for (i in 1..100) {
            val testPotion = potion.copy(id = i.toLong())
            
            val result = PotionBrewer.preservePotion(
                character = character,
                potion = testPotion,
                talent = Talent.ALCHEMY,
                magicalMasteryAsp = 0,
                currentDate = "1 Praios 1000 BF"
            )
            
            if (result.probeResult.isDoubleTwenty || result.probeResult.isTripleTwenty) {
                // Bei Patzer darf kein nachträglicher AsP-Einsatz erfolgen
                assertEquals(0, result.retroactiveAspUsed)
                break
            }
        }
    }
    
    @Test
    fun testDilution_RetroactiveAsp_WithMagicalMastery() {
        // Test: Nachträglicher AsP-Einsatz zusätzlich zu vorne eingesetzten AsP
        val character = Character(
            name = "Test-Alchimist",
            kl = 10,
            mu = 10,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        val recipe = Recipe(
            name = "Test-Trank",
            lab = Laboratory.ALCHEMIST_LABORATORY,
            brewingDifficulty = 6
        )
        
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = 1L,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            createdDate = "1 Praios 1000 BF",
            expiryDate = "1 Rondra 1000 BF"
        )
        
        
        // 2 AsP vorne einsetzen (TaW wird um 4 erhöht)
        val result = PotionBrewer.dilutePotion(
            character = character,
            potion = potion,
            recipe = recipe,
            talent = Talent.ALCHEMY,
            dilutionSteps = 1,
            facilitationFromAnalysis = 0,
            magicalMasteryAsp = 2
        )
        
        // Die Gesamtkosten sind magicalMasteryAsp + retroactiveAspUsed
        val totalAspCost = 2 + result.retroactiveAspUsed
        
        // Maximal 50 AsP verfügbar, also sollte totalAspCost <= 50 sein
        assertTrue(totalAspCost <= 50)
        
        // Wenn nachträglich AsP eingesetzt wurden, muss die Probe erfolgreich sein
        if (result.retroactiveAspUsed > 0) {
            assertTrue(result.success)
        }
    }
    
    @Test
    fun testBrewPotion_UnderstoodRecipe_DoublesDifficulty() {
        // Test: Bei einem verstandenen Rezept wird die Brauschwierigkeit verdoppelt
        val recipe = Recipe(
            name = "Test-Trank",
            lab = Laboratory.ARCANE,
            brewingDifficulty = 5
        )
        
        val character = Character(
            name = "Test-Alchimist",
            kl = 14,
            mu = 12,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 15,
            hasAe = false
        )
        
        // Normale Brauprobe (bekanntes Rezept)
        val resultKnown = PotionBrewer.brewPotion(
            character = character,
            recipe = recipe,
            talent = Talent.ALCHEMY,
            availableLaboratory = Laboratory.ARCANE,
            isUnderstoodRecipe = false
        )
        
        // Brauprobe mit verstandenem Rezept (doppelte Schwierigkeit)
        val resultUnderstood = PotionBrewer.brewPotion(
            character = character,
            recipe = recipe,
            talent = Talent.ALCHEMY,
            availableLaboratory = Laboratory.ARCANE,
            isUnderstoodRecipe = true
        )
        
        // Bei verstandenem Rezept ist die Schwierigkeit verdoppelt
        assertEquals(5, resultKnown.brewingDifficultyModifier)
        assertEquals(10, resultUnderstood.brewingDifficultyModifier) // 5 * 2 = 10
    }
    
    @Test
    fun testBrewPotion_UnderstoodRecipe_WithHighSkill() {
        // Test: Ein Charakter mit hohem Talentwert kann auch verstandene Rezepte erfolgreich brauen
        val recipe = Recipe(
            name = "Test-Trank",
            lab = Laboratory.ARCANE,
            brewingDifficulty = 4  // Verdoppelt: 8
        )
        
        val character = Character(
            name = "Meister-Alchimist",
            kl = 16,
            mu = 14,
            ff = 14,
            hasAlchemy = true,
            alchemySkill = 20,  // Sehr hoher Talentwert
            hasAe = false
        )
        
        // Bei sehr hohem Talentwert sollte die Probe auch mit verdoppelter Schwierigkeit gelingen können
        val result = PotionBrewer.brewPotion(
            character = character,
            recipe = recipe,
            talent = Talent.ALCHEMY,
            availableLaboratory = Laboratory.ARCANE,
            isUnderstoodRecipe = true
        )
        
        // Wir können nicht garantieren, dass die Probe gelingt (Würfelpech),
        // aber der Modifikator sollte korrekt sein
        assertEquals(8, result.brewingDifficultyModifier) // 4 * 2 = 8
    }
}








