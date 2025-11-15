package de.applicatus.app.repository

import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.Recipe
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for recipe synchronization with quantityProduced updates
 */
class RecipeSyncTest {
    
    @Test
    fun `test recipe quantityProduced values from InitialRecipes`() {
        // Verify that specific recipes from InitialRecipes have correct quantityProduced
        val initialRecipes = de.applicatus.app.data.InitialRecipes.getDefaultRecipes()
        
        // Test recipes that should produce multiple items
        val zauberkreide = initialRecipes.find { it.name == "Zauberkreide" }
        assertNotNull("Zauberkreide recipe should exist", zauberkreide)
        assertEquals(12, zauberkreide?.quantityProduced)
        
        val beschwörungskerzen = initialRecipes.find { it.name == "Beschörungskerzen" }
        assertNotNull("Beschörungskerzen recipe should exist", beschwörungskerzen)
        assertEquals(7, beschwörungskerzen?.quantityProduced)
        
        val hauchDerWeissagung = initialRecipes.find { it.name == "Hauch der Weissagung" }
        assertNotNull("Hauch der Weissagung recipe should exist", hauchDerWeissagung)
        assertEquals(7, hauchDerWeissagung?.quantityProduced)
        
        val regenbogenstaub = initialRecipes.find { it.name == "Regenbogenstaub" }
        assertNotNull("Regenbogenstaub recipe should exist", regenbogenstaub)
        assertEquals(5, regenbogenstaub?.quantityProduced)
        
        val waffenbalsam = initialRecipes.find { it.name == "Waffenbalsam" }
        assertNotNull("Waffenbalsam recipe should exist", waffenbalsam)
        assertEquals(5, waffenbalsam?.quantityProduced)
        
        val liebestrunk = initialRecipes.find { it.name == "Liebestrunk" }
        assertNotNull("Liebestrunk recipe should exist", liebestrunk)
        assertEquals(5, liebestrunk?.quantityProduced)
        
        val pastillenGgErschöpfung = initialRecipes.find { it.name == "Pastillen gg. Erschöpfung" }
        assertNotNull("Pastillen gg. Erschöpfung recipe should exist", pastillenGgErschöpfung)
        assertEquals(3, pastillenGgErschöpfung?.quantityProduced)
        
        val bannpulverGgUnsichtbares = initialRecipes.find { it.name == "Bannpulver gg. Unsichtbares" }
        assertNotNull("Bannpulver gg. Unsichtbares recipe should exist", bannpulverGgUnsichtbares)
        assertEquals(3, bannpulverGgUnsichtbares?.quantityProduced)
        
        val purpurwasser = initialRecipes.find { it.name == "Purpurwasser" }
        assertNotNull("Purpurwasser recipe should exist", purpurwasser)
        assertEquals(3, purpurwasser?.quantityProduced)
        
        val mengbillerBannbalöl = initialRecipes.find { it.name == "Mengbiller Bannbalöl" }
        assertNotNull("Mengbiller Bannbalöl recipe should exist", mengbillerBannbalöl)
        assertEquals(8, mengbillerBannbalöl?.quantityProduced)
        
        val schlafgift = initialRecipes.find { it.name == "Schlafgift" }
        assertNotNull("Schlafgift recipe should exist", schlafgift)
        assertEquals(2, schlafgift?.quantityProduced)
    }
    
    @Test
    fun `test recipes with single quantity default`() {
        // Verify that recipes without explicit quantityProduced have default value of 1
        val initialRecipes = de.applicatus.app.data.InitialRecipes.getDefaultRecipes()
        
        val heiltrank = initialRecipes.find { it.name == "Heiltrank" }
        assertNotNull("Heiltrank recipe should exist", heiltrank)
        assertEquals(1, heiltrank?.quantityProduced)
        
        val zaubertrank = initialRecipes.find { it.name == "Zaubertrank" }
        assertNotNull("Zaubertrank recipe should exist", zaubertrank)
        assertEquals(1, zaubertrank?.quantityProduced)
        
        val antidot = initialRecipes.find { it.name == "Antidot" }
        assertNotNull("Antidot recipe should exist", antidot)
        assertEquals(1, antidot?.quantityProduced)
    }
    
    @Test
    fun `test recipe update preserves other fields`() {
        // Simulate updating a recipe's quantityProduced while keeping other fields
        val originalRecipe = Recipe(
            id = 1,
            name = "Zauberkreide",
            gruppe = "Magika",
            lab = Laboratory.ARCANE,
            preis = 5,
            zutatenPreis = 3,
            zutatenVerbreitung = 13,
            verbreitung = 7,
            brewingDifficulty = 2,
            analysisDifficulty = 1,
            shelfLife = "nahezu unbegrenzt",
            quantityProduced = 1 // Old value
        )
        
        val updatedRecipe = originalRecipe.copy(quantityProduced = 12)
        
        // Verify all other fields remain the same
        assertEquals(originalRecipe.id, updatedRecipe.id)
        assertEquals(originalRecipe.name, updatedRecipe.name)
        assertEquals(originalRecipe.gruppe, updatedRecipe.gruppe)
        assertEquals(originalRecipe.lab, updatedRecipe.lab)
        assertEquals(originalRecipe.preis, updatedRecipe.preis)
        assertEquals(originalRecipe.brewingDifficulty, updatedRecipe.brewingDifficulty)
        assertEquals(originalRecipe.shelfLife, updatedRecipe.shelfLife)
        
        // Verify only quantityProduced changed
        assertNotEquals(originalRecipe.quantityProduced, updatedRecipe.quantityProduced)
        assertEquals(12, updatedRecipe.quantityProduced)
    }
    
    @Test
    fun `test all InitialRecipes have valid quantityProduced`() {
        val initialRecipes = de.applicatus.app.data.InitialRecipes.getDefaultRecipes()
        
        // Verify all recipes have quantityProduced >= 1
        initialRecipes.forEach { recipe ->
            assertTrue(
                "Recipe '${recipe.name}' should have quantityProduced >= 1",
                recipe.quantityProduced >= 1
            )
        }
    }
    
    @Test
    fun `test sync identifies recipes needing quantityProduced update`() {
        // Simulate a scenario where existing recipe has old quantityProduced value
        val existingRecipe = Recipe(
            id = 1,
            name = "Zauberkreide",
            gruppe = "Magika",
            lab = Laboratory.ARCANE,
            preis = 5,
            zutatenPreis = 3,
            zutatenVerbreitung = 13,
            verbreitung = 7,
            brewingDifficulty = 2,
            analysisDifficulty = 1,
            shelfLife = "nahezu unbegrenzt",
            quantityProduced = 1 // Old value before migration
        )
        
        val initialRecipes = de.applicatus.app.data.InitialRecipes.getDefaultRecipes()
        val newRecipe = initialRecipes.find { it.name == "Zauberkreide" }
        
        assertNotNull(newRecipe)
        assertNotEquals(
            "Existing recipe should have different quantityProduced than InitialRecipes",
            existingRecipe.quantityProduced,
            newRecipe?.quantityProduced
        )
        assertEquals(12, newRecipe?.quantityProduced)
        
        // After update, they should match
        val shouldUpdate = existingRecipe.quantityProduced != newRecipe?.quantityProduced
        assertTrue("Recipe should be flagged for update", shouldUpdate)
    }
}
