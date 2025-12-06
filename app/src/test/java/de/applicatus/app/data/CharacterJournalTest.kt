package de.applicatus.app.data

import de.applicatus.app.data.model.character.JournalCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für das Character Journal System.
 */
class CharacterJournalTest {

    @Test
    fun testJournalCategories_AreWellFormed() {
        // Test that all category constants follow the pattern Domain.Action or Domain.SubDomain.Action
        // Exception: "Other" is allowed as a catch-all category
        val categoryFields = JournalCategory::class.java.declaredFields
        
        var categoryCount = 0
        categoryFields.forEach { field ->
            if (field.type == String::class.java && java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                val value = field.get(null) as String
                categoryCount++
                
                // Special case: "Other" is allowed without a dot
                if (value == "Other") {
                    return@forEach
                }
                
                assertTrue(
                    "Category '$value' should contain at least one dot separator",
                    value.contains(".")
                )
                
                val parts = value.split(".")
                assertTrue(
                    "Category '$value' should have at least 2 parts (Domain.Action)",
                    parts.size >= 2
                )
                
                assertTrue(
                    "Domain part should not be empty",
                    parts[0].isNotEmpty()
                )
                assertTrue(
                    "Action part should not be empty",
                    parts.last().isNotEmpty()
                )
            }
        }
        
        assertTrue("Should have at least some categories defined", categoryCount > 0)
        println("✅ Validated $categoryCount journal categories")
    }

    @Test
    fun testJournalCategories_HaveExpectedValues() {
        // Test specific important categories exist and have correct format
        assertEquals("Potion.Brewed", JournalCategory.POTION_BREWED)
        assertEquals("Potion.Consumed", JournalCategory.POTION_CONSUMED)
        assertEquals("Potion.Analysis.Intensity", JournalCategory.POTION_ANALYSIS_INTENSITY)
        assertEquals("Spell.Cast", JournalCategory.SPELL_CAST)
        assertEquals("Spell.Stored", JournalCategory.SPELL_STORED)
        assertEquals("Energy.Regeneration", JournalCategory.ENERGY_REGENERATION)
        assertEquals("Energy.LE.Changed", JournalCategory.ENERGY_LE_CHANGED)
        assertEquals("Energy.AE.Changed", JournalCategory.ENERGY_AE_CHANGED)
        assertEquals("Character.Created", JournalCategory.CHARACTER_CREATED)
        
        println("✅ All expected categories have correct values")
    }

    @Test
    fun testJournalCategories_NoDuplicates() {
        // Collect all category values
        val categoryValues = mutableSetOf<String>()
        val categoryFields = JournalCategory::class.java.declaredFields
        
        categoryFields.forEach { field ->
            if (field.type == String::class.java && java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                val value = field.get(null) as String
                assertFalse(
                    "Category value '$value' should not be duplicated",
                    categoryValues.contains(value)
                )
                categoryValues.add(value)
            }
        }
        
        assertTrue("Should have multiple unique categories", categoryValues.size > 5)
        println("✅ All ${categoryValues.size} categories are unique")
    }

    @Test
    fun testJournalCategories_ValidDomains() {
        // Define expected domains
        val expectedDomains = setOf(
            "Potion", "Spell", "Energy", "Character", 
            "Inventory", "Group", "Talent", "Recipe", "Combat", "MagicSign", "Other"
        )
        
        val categoryFields = JournalCategory::class.java.declaredFields
        val foundDomains = mutableSetOf<String>()
        
        categoryFields.forEach { field ->
            if (field.type == String::class.java && java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                val value = field.get(null) as String
                val domain = value.split(".")[0]
                foundDomains.add(domain)
                
                assertTrue(
                    "Domain '$domain' should be one of the expected domains: $expectedDomains",
                    expectedDomains.contains(domain)
                )
            }
        }
        
        println("✅ All categories use valid domains: $foundDomains")
    }
}
