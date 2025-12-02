package de.applicatus.app.inventory

import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.model.magicsign.MagicSignEffect
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für Gewichtsreduktion durch Sigille des Unsichtbaren Trägers
 */
class WeightReductionTest {

    /**
     * Test: Ein Item von 15 Stein wird auf 1 Stein reduziert (nicht auf 1 Unze)
     * Reduktion: RkP* = 7 → 7 × 2 Stein = 14 Stein
     * Original: 15 Stein = 600 oz
     * Nach Reduktion: 1 Stein = 40 oz (nicht 1 oz!)
     */
    @Test
    fun `test weight reduction to minimum of 1 stone not 1 ounce`() {
        val originalWeight = Weight.fromOunces(15 * 40) // 15 Stein = 600 oz
        val reductionRkpStar = 7
        val reductionOunces = reductionRkpStar * 2 * 40 // 7 × 2 Stein = 560 oz
        
        // Berechnung wie im ViewModel
        val originalOunces = originalWeight.toOunces()
        val minimumOunces = if (originalOunces >= 40) 40 else originalOunces
        val reducedOunces = maxOf(originalOunces - reductionOunces, minimumOunces)
        
        val expectedOunces = 40 // 1 Stein
        assertEquals("Reduced weight should be 40 ounces (1 stone), not 1 ounce", 
            expectedOunces, reducedOunces)
        
        val reducedWeight = Weight.fromOunces(reducedOunces)
        assertEquals("Reduced weight should be 1 stone", 1, reducedWeight.stone)
        assertEquals("Reduced weight should have 0 remaining ounces", 0, reducedWeight.ounces)
    }

    /**
     * Test: Ein Item unter 1 Stein wird nicht schwerer gemacht
     * Original: 20 oz
     * Reduktion: 14 Stein = 560 oz
     * Nach Reduktion: 20 oz (nicht 40 oz - nie schwerer werden!)
     */
    @Test
    fun `test weight reduction does not make light items heavier`() {
        val originalWeight = Weight.fromOunces(20) // Unter 1 Stein
        val reductionRkpStar = 7
        val reductionOunces = reductionRkpStar * 2 * 40 // 560 oz
        
        // Berechnung wie im ViewModel
        val originalOunces = originalWeight.toOunces()
        val minimumOunces = if (originalOunces >= 40) 40 else originalOunces
        val reducedOunces = maxOf(originalOunces - reductionOunces, minimumOunces)
        
        assertEquals("Light items should never become heavier", 
            originalOunces, reducedOunces)
    }

    /**
     * Test: Location-Gewicht wird korrekt berechnet mit Item-Reduktion
     * Location mit einem Item: 15 Stein (600 oz)
     * Item hat Sigille mit RkP* = 7 → Reduktion um 14 Stein (560 oz)
     * Erwartetes Location-Gewicht: 1 Stein (40 oz)
     */
    @Test
    fun `test location weight calculation with item reduction`() {
        // Item-Gewicht
        val itemWeight = 600 // 15 Stein in oz
        
        // Item-Reduktion durch Zauberzeichen
        val itemReduction = 7 * 2 * 40 // RkP* 7 = 14 Stein = 560 oz
        
        // Location-Gewicht berechnen (wie im ViewModel)
        var calculatedItemWeight = itemWeight
        if (itemReduction > 0) {
            val minimumOunces = if (itemWeight >= 40) 40 else itemWeight
            calculatedItemWeight = maxOf(itemWeight - itemReduction, minimumOunces)
        }
        
        // Location-Gesamtgewicht
        val locationWeight = calculatedItemWeight
        
        assertEquals("Location weight should be 40 ounces (1 stone)", 
            40, locationWeight)
        
        val weight = Weight.fromOunces(locationWeight)
        assertEquals("Location should show 1 stone", 1, weight.stone)
        assertEquals("Location should show 0 remaining ounces", 0, weight.ounces)
    }

    /**
     * Test: Eigenobjekt-Reduktion wird auf ganzen Ort angewendet
     * Location mit mehreren Items: 100 oz + 200 oz = 300 oz
     * Eigenobjekt hat Sigille mit RkP* = 3 → Reduktion um 6 Stein (240 oz)
     * Erwartetes Location-Gewicht: 300 - 240 = 60 oz (nicht unter 40 oz)
     */
    @Test
    fun `test location weight with self-item reduction`() {
        // Items im Ort
        val item1Weight = 100
        val item2Weight = 200
        val totalOriginalWeight = item1Weight + item2Weight // 300 oz
        
        // Location-Reduktion durch Eigenobjekt
        val locationReduction = 3 * 2 * 40 // RkP* 3 = 6 Stein = 240 oz
        
        // Location-Gewicht berechnen
        val locationWeight = maxOf(totalOriginalWeight - locationReduction, 40)
        
        assertEquals("Location weight should be 60 ounces", 
            60, locationWeight)
    }

    /**
     * Test: Location-Reduktion darf nicht unter 1 Stein fallen
     * Location mit Items: 50 oz
     * Eigenobjekt hat Sigille mit RkP* = 10 → Reduktion um 20 Stein (800 oz)
     * Erwartetes Location-Gewicht: 40 oz (1 Stein minimum)
     */
    @Test
    fun `test location weight minimum is 1 stone`() {
        val totalOriginalWeight = 50 // oz
        val locationReduction = 10 * 2 * 40 // 800 oz - viel mehr als das Gewicht
        
        val locationWeight = maxOf(totalOriginalWeight - locationReduction, 40)
        
        assertEquals("Location weight should not go below 1 stone (40 ounces)", 
            40, locationWeight)
    }

    /**
     * Test: Kombination von Item-Reduktion und Location-Reduktion
     * - Item 1: 600 oz mit Sigille (RkP* 7) → reduziert auf 40 oz
     * - Item 2: 200 oz ohne Sigille → bleibt 200 oz
     * - Gesamt vor Location-Reduktion: 240 oz
     * - Eigenobjekt mit Sigille (RkP* 2) → Reduktion um 4 Stein (160 oz)
     * - Erwartetes Location-Gewicht: 240 - 160 = 80 oz
     */
    @Test
    fun `test combined item and location reductions`() {
        // Item 1 mit eigener Reduktion
        var item1Weight = 600 // 15 Stein
        val item1Reduction = 7 * 2 * 40 // 560 oz
        val minItem1 = if (item1Weight >= 40) 40 else item1Weight
        item1Weight = maxOf(item1Weight - item1Reduction, minItem1) // = 40 oz
        
        // Item 2 ohne Reduktion
        val item2Weight = 200 // oz
        
        // Location-Gewicht vor Location-Reduktion
        var locationWeight = item1Weight + item2Weight // 40 + 200 = 240 oz
        
        // Location-Reduktion durch Eigenobjekt
        val locationReduction = 2 * 2 * 40 // RkP* 2 = 4 Stein = 160 oz
        locationWeight = maxOf(locationWeight - locationReduction, 40) // 240 - 160 = 80 oz
        
        assertEquals("Location weight with combined reductions should be 80 ounces", 
            80, locationWeight)
    }
}
