package de.applicatus.app.logic

import de.applicatus.app.data.InitialHerbs
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.herb.Landscape
import de.applicatus.app.logic.DerianDateCalculator.DerianMonth
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit-Tests für HerbSearchCalculator
 */
class HerbSearchCalculatorTest {
    
    private lateinit var calculator: HerbSearchCalculator
    private lateinit var testCharacter: Character
    
    @Before
    fun setup() {
        calculator = HerbSearchCalculator()
        
        // Test-Charakter mit durchschnittlichen Werten
        testCharacter = Character(
            id = 1,
            name = "Test Kräutersucher",
            mu = 12,
            inValue = 14,
            ff = 13,
            sensoryAcuitySkill = 8,
            wildernessSkill = 10,
            herbalLoreSkill = 7,
            gelaendekunde = emptyList()
        )
    }
    
    // ==================== TaW-Berechnung Tests ====================
    
    @Test
    fun testCalculateHerbSearchTaW_average() {
        // (8 + 10 + 7) / 3 = 25 / 3 = 8.33 → 8 (aufgerundet: (25+1)/3 = 26/3 = 8)
        val taw = calculator.calculateHerbSearchTaW(testCharacter)
        assertEquals(8, taw)
    }
    
    @Test
    fun testCalculateHerbSearchTaW_limitedBySmallestTalent() {
        // Charakter mit 15, 15, 5 -> Durchschnitt = 12, aber max 2*5 = 10
        val char = testCharacter.copy(
            sensoryAcuitySkill = 15,
            wildernessSkill = 15,
            herbalLoreSkill = 5
        )
        val taw = calculator.calculateHerbSearchTaW(char)
        assertEquals(10, taw) // Begrenzt auf 2 * 5
    }
    
    @Test
    fun testCalculateHerbSearchTaW_allEqual() {
        // Alle Talente gleich: 10, 10, 10 -> TaW = 10
        val char = testCharacter.copy(
            sensoryAcuitySkill = 10,
            wildernessSkill = 10,
            herbalLoreSkill = 10
        )
        val taw = calculator.calculateHerbSearchTaW(char)
        assertEquals(10, taw)
    }
    
    @Test
    fun testCalculateHerbSearchTaW_roundingUp() {
        // (7 + 8 + 9) / 3 = 24 / 3 = 8.0 → 8
        // (7 + 8 + 10) / 3 = 25 / 3 = 8.33 → 9 (mit kaufmännischer Rundung: (25+1)/3 = 8)
        val char1 = testCharacter.copy(
            sensoryAcuitySkill = 7,
            wildernessSkill = 8,
            herbalLoreSkill = 9
        )
        assertEquals(8, calculator.calculateHerbSearchTaW(char1))
        
        val char2 = testCharacter.copy(
            sensoryAcuitySkill = 7,
            wildernessSkill = 8,
            herbalLoreSkill = 10
        )
        // (7+8+10+1)/3 = 26/3 = 8
        assertEquals(8, calculator.calculateHerbSearchTaW(char2))
    }
    
    // ==================== Erschwernis-Berechnung Tests ====================
    
    @Test
    fun testCalculateSearchDifficulty_basicHerb() {
        val dornrose = InitialHerbs.findHerbByName("Dornrose")!!
        
        // Dornrose: Bestimmung 3, in Wald gelegentlich (+4) = 7
        val difficulty = calculator.calculateSearchDifficulty(
            herb = dornrose,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = false,
            hasDoubledSearchTime = false,
            character = testCharacter
        )
        assertEquals(7, difficulty)
    }
    
    @Test
    fun testCalculateSearchDifficulty_withGelaendekunde() {
        val dornrose = InitialHerbs.findHerbByName("Dornrose")!!
        val charWithGelaende = testCharacter.copy(gelaendekunde = listOf("Wald"))
        
        // Dornrose: Bestimmung 3, Wald gelegentlich (+4) = 7, Geländekunde (-3) = 4
        val difficulty = calculator.calculateSearchDifficulty(
            herb = dornrose,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = false,
            hasDoubledSearchTime = false,
            character = charWithGelaende
        )
        assertEquals(4, difficulty)
    }
    
    @Test
    fun testCalculateSearchDifficulty_withOrtskenntnis() {
        val dornrose = InitialHerbs.findHerbByName("Dornrose")!!
        
        // Dornrose: Bestimmung 3, Wald gelegentlich (+4) = 7, Ortskenntnis (-7) = 0
        val difficulty = calculator.calculateSearchDifficulty(
            herb = dornrose,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = true,
            hasDoubledSearchTime = false,
            character = testCharacter
        )
        assertEquals(0, difficulty)
    }
    
    @Test
    fun testCalculateSearchDifficulty_withDoubledSearchTime() {
        val dornrose = InitialHerbs.findHerbByName("Dornrose")!!
        
        // Dornrose: Bestimmung 3, Wald gelegentlich (+4) = 7, Verdopplung (-2) = 5
        val difficulty = calculator.calculateSearchDifficulty(
            herb = dornrose,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = false,
            hasDoubledSearchTime = true,
            character = testCharacter
        )
        assertEquals(5, difficulty)
    }
    
    @Test
    fun testCalculateSearchDifficulty_allBonuses() {
        val dornrose = InitialHerbs.findHerbByName("Dornrose")!!
        val charWithGelaende = testCharacter.copy(gelaendekunde = listOf("Wald"))
        
        // Dornrose: 3 + 4 - 3 (Geländekunde) - 7 (Ortskenntnis) - 2 (Verdopplung) = -5
        val difficulty = calculator.calculateSearchDifficulty(
            herb = dornrose,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = true,
            hasDoubledSearchTime = true,
            character = charWithGelaende
        )
        assertEquals(-5, difficulty)
    }
    
    @Test
    fun testCalculateSearchDifficulty_rareHerb() {
        val zwolfblatt = InitialHerbs.findHerbByName("Zwölfblatt")!!
        
        // Zwölfblatt: Bestimmung 5, in Wald gelegentlich (+4) = 9
        val difficulty = calculator.calculateSearchDifficulty(
            herb = zwolfblatt,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = false,
            hasDoubledSearchTime = false,
            character = testCharacter
        )
        assertEquals(9, difficulty)
    }
    
    // ==================== Verfügbarkeits-Tests ====================
    
    @Test
    fun testGetAvailableHerbs_filtersByLandscape() {
        val regionHerbs = listOf("Dornrose", "Donf", "Zwölfblatt")
        
        // Donf wächst nur in Sumpf/Fluss, nicht im Wald
        val available = calculator.getAvailableHerbs(
            regionHerbs = regionHerbs,
            landscape = Landscape.FOREST,
            month = DerianMonth.FULL_YEAR
        )
        
        assertTrue(available.any { it.name == "Dornrose" })
        assertFalse(available.any { it.name == "Donf" })
        assertTrue(available.any { it.name == "Zwölfblatt" })
    }
    
    @Test
    fun testGetAvailableHerbs_filtersByMonth() {
        val regionHerbs = listOf("Donf", "Quinja")
        
        // Donf: ALL_MONTHS (auch in Praios verfügbar in RIVER_LAKE/SWAMP)
        // Quinja: PRAIOS, RONDRA, EFFERD, TRAVIA, INGERIMM, RAHJA, NAMELESS_DAYS (NICHT Phex)
        val availableInPraios = calculator.getAvailableHerbs(
            regionHerbs = regionHerbs,
            landscape = Landscape.FOREST,
            month = DerianMonth.PRAIOS
        )
        
        assertFalse(availableInPraios.any { it.name == "Donf" }) // Donf nicht im Wald
        assertTrue(availableInPraios.any { it.name == "Quinja" })
        
        val availableInPhex = calculator.getAvailableHerbs(
            regionHerbs = regionHerbs,
            landscape = Landscape.FOREST,
            month = DerianMonth.PHEX
        )
        
        assertFalse(availableInPhex.any { it.name == "Donf" }) // Donf nicht im Wald
        assertFalse(availableInPhex.any { it.name == "Quinja" }) // Quinja nicht im Phex
    }
    
    @Test
    fun testGetAvailableHerbs_fullYearShowsAll() {
        val regionHerbs = listOf("Donf", "Quinja")
        
        val available = calculator.getAvailableHerbs(
            regionHerbs = regionHerbs,
            landscape = Landscape.FOREST,
            month = DerianMonth.FULL_YEAR
        )
        
        // Mit FULL_YEAR sollte Quinja verfügbar sein (Donf nicht, da nur Grasland)
        assertTrue(available.any { it.name == "Quinja" })
    }
    
    // ==================== Kräutersuche-Tests ====================
    
    @Test
    fun testPerformHerbSearch_successfulSearch() {
        val dornrose = InitialHerbs.findHerbByName("Dornrose")!!
        
        // Erfolgreiche Probe: Alle Würfe unter den Eigenschaften
        val result = calculator.performHerbSearch(
            character = testCharacter,
            herb = dornrose,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = false,
            hasDoubledSearchTime = false,
            providedRolls = listOf(10, 12, 11) // MU 12, IN 14, FF 13
        )
        
        assertTrue(result.success)
        assertTrue(result.qualityPoints >= 0)
        assertEquals("Strauch mit W6 Blüten", result.foundQuantity)
        assertFalse(result.isSpectacular)
        assertFalse(result.isCatastrophic)
    }
    
    @Test
    fun testPerformHerbSearch_failedSearch() {
        val zwolfblatt = InitialHerbs.findHerbByName("Zwölfblatt")!!
        
        // Fehlgeschlagene Probe: Erschwernis 9, aber Würfe zu hoch
        val result = calculator.performHerbSearch(
            character = testCharacter,
            herb = zwolfblatt,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = false,
            hasDoubledSearchTime = false,
            providedRolls = listOf(15, 16, 17)
        )
        
        assertFalse(result.success)
        assertEquals(0, result.qualityPoints)  // Bei Fehlschlag: TaP* = 0
        assertNull(result.foundQuantity)
    }
    
    @Test
    fun testPerformHerbSearch_spectacularSuccess() {
        val dornrose = InitialHerbs.findHerbByName("Dornrose")!!
        
        // Doppel-1 = Spektakulärer Erfolg
        val result = calculator.performHerbSearch(
            character = testCharacter,
            herb = dornrose,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = false,
            hasDoubledSearchTime = false,
            providedRolls = listOf(1, 1, 10)
        )
        
        assertTrue(result.success)
        assertTrue(result.isSpectacular)
        assertFalse(result.isCatastrophic)
        assertNotNull(result.foundQuantity)
    }
    
    @Test
    fun testPerformHerbSearch_catastrophicFailure() {
        val dornrose = InitialHerbs.findHerbByName("Dornrose")!!
        
        // Doppel-20 = Katastrophaler Patzer
        val result = calculator.performHerbSearch(
            character = testCharacter,
            herb = dornrose,
            landscape = Landscape.FOREST,
            hasOrtskenntnis = false,
            hasDoubledSearchTime = false,
            providedRolls = listOf(20, 20, 10)
        )
        
        assertFalse(result.success)
        assertTrue(result.isCatastrophic)
        assertFalse(result.isSpectacular)
        assertNull(result.foundQuantity)
    }
}
