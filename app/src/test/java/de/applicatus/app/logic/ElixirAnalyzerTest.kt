package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit-Tests für ElixirAnalyzer
 * 
 * Testet die Trank-Analyse-Logik, insbesondere:
 * - Intensitätsbestimmung
 * - Strukturanalyse
 * - Kombination beider in verschiedenen Reihenfolgen
 */
class ElixirAnalyzerTest {
    
    private lateinit var testCharacter: Character
    private lateinit var testRecipe: Recipe
    
    @Before
    fun setUp() {
        testCharacter = Character(
            name = "Test-Alchimist",
            kl = 14,
            mu = 12,
            inValue = 13,
            ff = 10,
            hasAlchemy = true,
            alchemySkill = 12,
            hasOdem = true,
            odemZfw = 10,
            hasAnalys = true,
            analysZfw = 10,
            magicalLoreSkill = 10,
            sensoryAcuitySkill = 9,
            hasAe = true,
            currentAe = 50,
            maxAe = 50
        )
        
        testRecipe = Recipe(
            id = 1,
            name = "Test-Heiltrank",
            analysisDifficulty = 3
        )
    }
    
    // ===== Intensitätsbestimmung Tests =====
    
    @Test
    fun testIntensityDetermination_WeakPotion() {
        // Trank mit Qualität B (schwach)
        val result = ElixirAnalyzer.determineIntensity(
            character = testCharacter,
            recipe = testRecipe,
            actualQuality = PotionQuality.B
        )
        
        // Bei erfolgreicher Bestimmung sollte die Intensität WEAK sein
        if (result.success && result.zfp >= 3) {
            assertEquals(IntensityQuality.WEAK, result.intensityQuality)
        }
    }
    
    @Test
    fun testIntensityDetermination_StrongPotion() {
        // Trank mit Qualität E (stark)
        val result = ElixirAnalyzer.determineIntensity(
            character = testCharacter,
            recipe = testRecipe,
            actualQuality = PotionQuality.E
        )
        
        // Bei erfolgreicher Bestimmung sollte die Intensität STRONG sein
        if (result.success && result.zfp >= 3) {
            assertEquals(IntensityQuality.STRONG, result.intensityQuality)
        }
    }
    
    // ===== Strukturanalyse ohne vorherige Intensitätsbestimmung =====
    
    @Test
    fun testStructureAnalysis_WithoutIntensity_WeakPotion() {
        // Strukturanalyse OHNE vorherige Intensitätsbestimmung
        // Trank mit Qualität B (schwach)
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 5,  // 4+ TaP* für Qualitätsbestimmung
            actualQuality = PotionQuality.B,
            currentIntensity = IntensityQuality.UNKNOWN,  // Keine vorherige Intensitätsbestimmung
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        // Sollte WEAK_OR_STRONG sein (grobe Qualität bekannt)
        assertEquals(KnownQualityLevel.WEAK_OR_STRONG, finalResult.knownQualityLevel)
        
        // Intensität sollte aus der Strukturanalyse abgeleitet werden (ABC → WEAK)
        assertEquals(IntensityQuality.WEAK, finalResult.intensityQuality)
        
        assertTrue(finalResult.message.contains("schwach (A, B oder C)"))
    }
    
    @Test
    fun testStructureAnalysis_WithoutIntensity_StrongPotion() {
        // Strukturanalyse OHNE vorherige Intensitätsbestimmung
        // Trank mit Qualität E (stark)
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 6,  // 4+ TaP* für Qualitätsbestimmung
            actualQuality = PotionQuality.E,
            currentIntensity = IntensityQuality.UNKNOWN,  // Keine vorherige Intensitätsbestimmung
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        // Sollte WEAK_OR_STRONG sein (grobe Qualität bekannt)
        assertEquals(KnownQualityLevel.WEAK_OR_STRONG, finalResult.knownQualityLevel)
        
        // Intensität sollte aus der Strukturanalyse abgeleitet werden (DEF → STRONG)
        assertEquals(IntensityQuality.STRONG, finalResult.intensityQuality)
        
        assertTrue(finalResult.message.contains("stark (D, E oder F)"))
    }
    
    // ===== Strukturanalyse MIT vorheriger Intensitätsbestimmung =====
    
    @Test
    fun testStructureAnalysis_WithIntensity_Refined() {
        // Strukturanalyse MIT vorheriger Intensitätsbestimmung
        // Sollte zu verfeinerten Qualitäten führen (AB / CD / EF)
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 5,  // 4+ TaP* für Qualitätsbestimmung
            actualQuality = PotionQuality.E,
            currentIntensity = IntensityQuality.STRONG,  // Vorherige Intensitätsbestimmung
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        // Sollte VERY_WEAK_MEDIUM_OR_VERY_STRONG sein (verfeinerte Qualität)
        assertEquals(KnownQualityLevel.VERY_WEAK_MEDIUM_OR_VERY_STRONG, finalResult.knownQualityLevel)
        
        // Sollte verfeinerte Qualität sein (E → VERY_STRONG)
        assertEquals(RefinedQuality.VERY_STRONG, finalResult.refinedQuality)
        
        assertTrue(finalResult.message.contains("Verfeinerte Qualität erkannt"))
    }
    
    // ===== Sequenz-Tests: Strukturanalyse → Intensitätsbestimmung → Strukturanalyse =====
    
    @Test
    fun testSequence_StructureFirst_ThenIntensity_ThenStructure() {
        // Szenario: Erst Strukturanalyse, dann Intensitätsbestimmung, dann nochmal Strukturanalyse
        // Dies sollte zu verfeinerten Qualitäten führen
        
        val actualQuality = PotionQuality.E
        
        // Schritt 1: Strukturanalyse ohne Intensitätsbestimmung
        val firstStructure = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 5,
            actualQuality = actualQuality,
            currentIntensity = IntensityQuality.UNKNOWN,
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        // Nach erster Strukturanalyse: Grobe Qualität bekannt (stark = DEF)
        assertEquals(KnownQualityLevel.WEAK_OR_STRONG, firstStructure.knownQualityLevel)
        assertEquals(IntensityQuality.STRONG, firstStructure.intensityQuality)
        
        // Schritt 2: Intensitätsbestimmung (sollte keine neue Info bringen, da schon bekannt)
        // Die Intensität ist bereits durch die Strukturanalyse bekannt
        val intensityResult = ElixirAnalyzer.determineIntensity(
            character = testCharacter,
            recipe = testRecipe,
            actualQuality = actualQuality
        )
        // Intensität sollte STRONG sein (wenn erfolgreich)
        if (intensityResult.success && intensityResult.zfp >= 3) {
            assertEquals(IntensityQuality.STRONG, intensityResult.intensityQuality)
        }
        
        // Schritt 3: Zweite Strukturanalyse mit bekannter Intensität
        val secondStructure = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 6,
            actualQuality = actualQuality,
            currentIntensity = firstStructure.intensityQuality,  // Verwende Intensität aus erster Analyse
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        // Nach zweiter Strukturanalyse: Verfeinerte Qualität (E/F)
        assertEquals(KnownQualityLevel.VERY_WEAK_MEDIUM_OR_VERY_STRONG, secondStructure.knownQualityLevel)
        assertEquals(RefinedQuality.VERY_STRONG, secondStructure.refinedQuality)
    }
    
    // ===== Sequenz-Tests: Intensitätsbestimmung → Strukturanalyse =====
    
    @Test
    fun testSequence_IntensityFirst_ThenStructure() {
        // Szenario: Erst Intensitätsbestimmung, dann Strukturanalyse
        // Dies sollte DIREKT zu verfeinerten Qualitäten führen
        
        val actualQuality = PotionQuality.D
        
        // Schritt 1: Intensitätsbestimmung
        val intensityResult = ElixirAnalyzer.determineIntensity(
            character = testCharacter,
            recipe = testRecipe,
            actualQuality = actualQuality
        )
        
        // Annahme: Intensitätsbestimmung war erfolgreich
        if (intensityResult.success && intensityResult.zfp >= 3) {
            assertEquals(IntensityQuality.STRONG, intensityResult.intensityQuality)
            
            // Schritt 2: Strukturanalyse mit bekannter Intensität
            val structureResult = ElixirAnalyzer.calculateStructureAnalysisResult(
                totalTap = 5,
                actualQuality = actualQuality,
                currentIntensity = intensityResult.intensityQuality,
                isRecipeKnown = false,
                method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
                acceptHarderProbe = true
            )
            
            // Sollte DIREKT verfeinerte Qualität sein (C/D)
            assertEquals(KnownQualityLevel.VERY_WEAK_MEDIUM_OR_VERY_STRONG, structureResult.knownQualityLevel)
            assertEquals(RefinedQuality.MEDIUM, structureResult.refinedQuality)
        }
    }
    
    // ===== Grenzfälle =====
    
    @Test
    fun testStructureAnalysis_ExactQuality() {
        // Bei 13+ TaP* sollte die genaue Qualität bekannt sein
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 15,
            actualQuality = PotionQuality.C,
            currentIntensity = IntensityQuality.UNKNOWN,
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        assertEquals(KnownQualityLevel.EXACT, finalResult.knownQualityLevel)
        assertEquals(PotionQuality.C, finalResult.knownExactQuality)
    }
    
    @Test
    fun testStructureAnalysis_LowTap_NoQuality() {
        // Bei weniger als 4 TaP* sollte keine Qualität bekannt sein
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 2,
            actualQuality = PotionQuality.D,
            currentIntensity = IntensityQuality.UNKNOWN,
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        assertEquals(KnownQualityLevel.UNKNOWN, finalResult.knownQualityLevel)
    }
    
    @Test
    fun testStructureAnalysis_ShelfLifeKnown() {
        // Bei 8+ TaP* sollte die Haltbarkeit bekannt sein
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 9,
            actualQuality = PotionQuality.B,
            currentIntensity = IntensityQuality.WEAK,
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        assertTrue(finalResult.shelfLifeKnown)
    }
    
    @Test
    fun testStructureAnalysis_RecipeKnown() {
        // Bei 19+ TaP* sollte das Rezept verstanden werden
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 20,
            actualQuality = PotionQuality.F,
            currentIntensity = IntensityQuality.STRONG,
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        assertTrue(finalResult.recipeKnown)
    }
    
    @Test
    fun testStructureAnalysis_PotionConsumed() {
        // Bei Laboranalyse ohne acceptHarderProbe sollte der Trank verbraucht werden
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 5,
            actualQuality = PotionQuality.C,
            currentIntensity = IntensityQuality.WEAK,
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = false  // Trank wird verbraucht
        )
        
        assertTrue(finalResult.potionConsumed)
    }
    
    @Test
    fun testStructureAnalysis_PotionNotConsumed() {
        // Bei Laboranalyse mit acceptHarderProbe sollte der Trank NICHT verbraucht werden
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 5,
            actualQuality = PotionQuality.C,
            currentIntensity = IntensityQuality.WEAK,
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true  // Trank wird NICHT verbraucht
        )
        
        assertFalse(finalResult.potionConsumed)
    }
    
    // ===== Qualitäts-M (Misslungen) Tests =====
    
    @Test
    fun testStructureAnalysis_QualityM_WithoutIntensity() {
        // Qualität M ist speziell - sollte zufällig WEAK oder STRONG sein
        val finalResult = ElixirAnalyzer.calculateStructureAnalysisResult(
            totalTap = 5,
            actualQuality = PotionQuality.M,
            currentIntensity = IntensityQuality.UNKNOWN,
            isRecipeKnown = false,
            method = StructureAnalysisMethod.LABORATORY_ALCHEMY,
            acceptHarderProbe = true
        )
        
        // Sollte WEAK_OR_STRONG sein
        assertEquals(KnownQualityLevel.WEAK_OR_STRONG, finalResult.knownQualityLevel)
        
        // Intensität sollte entweder WEAK oder STRONG sein (nicht UNKNOWN)
        assertTrue(
            finalResult.intensityQuality == IntensityQuality.WEAK || 
            finalResult.intensityQuality == IntensityQuality.STRONG
        )
    }
}
