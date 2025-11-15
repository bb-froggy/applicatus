package de.applicatus.app.inventory

import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionQuality
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for countable items functionality
 */
class CountableItemsTest {
    
    @Test
    fun `test Item quantity defaults to 1`() {
        val item = Item(
            characterId = 1,
            locationId = 1,
            name = "Schwert",
            weight = Weight(1, 0)
        )
        
        assertEquals(1, item.quantity)
        assertFalse(item.isCountable)
    }
    
    @Test
    fun `test Item can be countable with custom quantity`() {
        val item = Item(
            characterId = 1,
            locationId = 1,
            name = "Wurfmesser",
            weight = Weight(0, 4),
            isCountable = true,
            quantity = 5
        )
        
        assertEquals(5, item.quantity)
        assertTrue(item.isCountable)
    }
    
    @Test
    fun `test ItemWithLocation totalWeight calculation`() {
        val item = ItemWithLocation(
            id = 1,
            characterId = 1,
            locationId = 1,
            name = "Wurfmesser",
            stone = 0,
            ounces = 4,
            sortOrder = 0,
            locationName = "Gürtel",
            isCountable = true,
            quantity = 5
        )
        
        assertEquals(Weight(0, 4), item.weight)
        assertEquals(Weight(0, 20), item.totalWeight)
        assertEquals(20, item.totalWeight.toOunces())
    }
    
    @Test
    fun `test Weight multiplication operator`() {
        val weight = Weight(stone = 1, ounces = 4) // 1 Stein 4 Unzen = 44 Unzen (1 Stein = 40 Unzen)
        val doubled = weight * 2
        
        assertEquals(88, doubled.toOunces())
        assertEquals(Weight(2, 8), doubled)
    }
    
    @Test
    fun `test Weight multiplication with zero`() {
        val weight = Weight(stone = 1, ounces = 4)
        val zero = weight * 0
        
        assertEquals(0, zero.toOunces())
        assertEquals(Weight.ZERO, zero)
    }
    
    @Test
    fun `test Weight multiplication preserves total ounces`() {
        val weight = Weight(stone = 0, ounces = 7)
        val tripled = weight * 3
        
        assertEquals(21, tripled.toOunces())
        assertEquals(Weight(0, 21), tripled) // 21 Unzen bleibt 21 Unzen (unter 40)
    }
    
    @Test
    fun `test Potion quantity defaults to 1`() {
        val potion = Potion(
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            appearance = "golden",
            createdDate = "1 Praios 1040 BF",
            expiryDate = "1 Boron 1040 BF"
        )
        
        assertEquals(1, potion.quantity)
    }
    
    @Test
    fun `test Potion can have multiple quantity`() {
        val potion = Potion(
            characterId = 1,
            recipeId = 1,
            actualQuality = PotionQuality.C,
            appearance = "golden",
            createdDate = "1 Praios 1040 BF",
            expiryDate = "1 Boron 1040 BF",
            quantity = 12
        )
        
        assertEquals(12, potion.quantity)
    }
    
    @Test
    fun `test ItemWithLocation quantity field is separate per location`() {
        val itemA = ItemWithLocation(
            id = 1,
            characterId = 1,
            locationId = 1,
            name = "Wurfmesser",
            stone = 0,
            ounces = 4,
            sortOrder = 0,
            locationName = "Ort A",
            isCountable = true,
            quantity = 5
        )
        
        val itemB = ItemWithLocation(
            id = 2,
            characterId = 1,
            locationId = 2,
            name = "Wurfmesser",
            stone = 0,
            ounces = 4,
            sortOrder = 0,
            locationName = "Ort B",
            isCountable = true,
            quantity = 3
        )
        
        // Items with same name but different locations have independent quantities
        assertEquals(5, itemA.quantity)
        assertEquals(3, itemB.quantity)
        assertNotEquals(itemA.id, itemB.id)
        assertNotEquals(itemA.locationId, itemB.locationId)
    }
    
    @Test
    fun `test purses cannot be countable`() {
        // Purses should always have quantity=1 and isCountable=false
        val purse = Item(
            characterId = 1,
            locationId = 1,
            name = "Geldbeutel",
            weight = Weight(0, 1),
            isPurse = true,
            isCountable = false,
            quantity = 1
        )
        
        assertTrue(purse.isPurse)
        assertFalse(purse.isCountable)
        assertEquals(1, purse.quantity)
    }
    
    @Test
    fun `test split quantity validation - source quantity should be correct`() {
        // Simulates the split dialog scenario
        val itemInLocationB = ItemWithLocation(
            id = 2,
            characterId = 1,
            locationId = 2, // Location B
            name = "Wurfmesser",
            stone = 0,
            ounces = 4,
            sortOrder = 0,
            locationName = "Ort B",
            isCountable = true,
            quantity = 3 // Only 3 in location B
        )
        
        // The split dialog should show max quantity of 3, not 8 (total across all locations)
        val maxQuantityForSplit = itemInLocationB.quantity
        assertEquals(3, maxQuantityForSplit)
        assertTrue(maxQuantityForSplit in 1..3)
    }
    
    @Test
    fun `test total weight for non-countable items equals single weight`() {
        val sword = ItemWithLocation(
            id = 1,
            characterId = 1,
            locationId = 1,
            name = "Schwert",
            stone = 2,
            ounces = 0,
            sortOrder = 0,
            locationName = "Waffengurt",
            isCountable = false,
            quantity = 1
        )
        
        assertEquals(sword.weight, sword.totalWeight)
        assertEquals(Weight(2, 0), sword.totalWeight)
    }
}
