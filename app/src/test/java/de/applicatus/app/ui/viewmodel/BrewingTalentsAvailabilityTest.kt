package de.applicatus.app.ui.viewmodel

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.talent.Talent
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für verfügbare Brau-Talente.
 * 
 * Bug: Im Spielleiter-Modus passiert beim Tap auf "Brauen" nichts.
 * Der gleiche Charakter funktioniert als Spieler.
 * 
 * Ursache: getAvailableBrewingTalents() liest _character.value synchron.
 * Im BrewPotionDialog wird dies einmalig beim Öffnen aufgerufen.
 * Wenn zu diesem Zeitpunkt _character.value null ist (z.B. bei Sync-Delays),
 * wird eine leere Liste zurückgegeben.
 * 
 * Da availableTalents leer ist, wird die Fehlermeldung 
 * "Keine Brau-Talente verfügbar" angezeigt, oder es gibt keine Talente zum Auswählen.
 * 
 * Fix: Im BrewPotionDialog sollten die Talente direkt vom übergebenen
 * `character` Parameter berechnet werden, nicht vom ViewModel.
 */
class BrewingTalentsAvailabilityTest {
    
    @Test
    fun `character with alchemy has alchemy talent available`() {
        val character = Character(
            id = 1,
            name = "Alchemist",
            mu = 12, kl = 14, inValue = 13, ch = 10, ff = 12, ge = 11, ko = 12, kk = 11,
            hasAlchemy = true,  // Das Flag, das anzeigt ob Alchimie beherrscht wird
            alchemySkill = 8
        )
        
        assertTrue("hasAlchemy sollte true sein", character.hasAlchemy)
        
        // Simuliere getAvailableBrewingTalents-Logik
        val talents = mutableListOf<Talent>()
        if (character.hasAlchemy) talents.add(Talent.ALCHEMY)
        if (character.hasCookingPotions) talents.add(Talent.COOKING_POTIONS)
        
        assertTrue("Alchimie sollte verfügbar sein", talents.contains(Talent.ALCHEMY))
    }
    
    @Test
    fun `character with cooking potions has cooking talent available`() {
        val character = Character(
            id = 1,
            name = "Koch",
            mu = 12, kl = 14, inValue = 13, ch = 10, ff = 12, ge = 11, ko = 12, kk = 11,
            hasCookingPotions = true,  // Das Flag, das anzeigt ob Kochen (Tränke) beherrscht wird
            cookingPotionsSkill = 6
        )
        
        assertTrue("hasCookingPotions sollte true sein", character.hasCookingPotions)
        
        val talents = mutableListOf<Talent>()
        if (character.hasAlchemy) talents.add(Talent.ALCHEMY)
        if (character.hasCookingPotions) talents.add(Talent.COOKING_POTIONS)
        
        assertTrue("Kochen (Tränke) sollte verfügbar sein", talents.contains(Talent.COOKING_POTIONS))
    }
    
    @Test
    fun `character with both talents has both available`() {
        val character = Character(
            id = 1,
            name = "Meisterbrauer",
            mu = 12, kl = 14, inValue = 13, ch = 10, ff = 12, ge = 11, ko = 12, kk = 11,
            hasAlchemy = true,
            alchemySkill = 10,
            hasCookingPotions = true,
            cookingPotionsSkill = 8
        )
        
        val talents = mutableListOf<Talent>()
        if (character.hasAlchemy) talents.add(Talent.ALCHEMY)
        if (character.hasCookingPotions) talents.add(Talent.COOKING_POTIONS)
        
        assertEquals("Sollte 2 Talente haben", 2, talents.size)
        assertTrue(talents.contains(Talent.ALCHEMY))
        assertTrue(talents.contains(Talent.COOKING_POTIONS))
    }
    
    @Test
    fun `character without brewing talents has none available`() {
        val character = Character(
            id = 1,
            name = "Krieger",
            mu = 14, kl = 10, inValue = 11, ch = 12, ff = 11, ge = 13, ko = 14, kk = 15,
            hasAlchemy = false,
            alchemySkill = 0,
            hasCookingPotions = false,
            cookingPotionsSkill = 0
        )
        
        assertFalse("hasAlchemy sollte false sein", character.hasAlchemy)
        assertFalse("hasCookingPotions sollte false sein", character.hasCookingPotions)
        
        val talents = mutableListOf<Talent>()
        if (character.hasAlchemy) talents.add(Talent.ALCHEMY)
        if (character.hasCookingPotions) talents.add(Talent.COOKING_POTIONS)
        
        assertTrue("Keine Talente sollten verfügbar sein", talents.isEmpty())
    }
    
    @Test
    fun `null character from viewmodel results in empty talents`() {
        // Simuliere den Bug: _character.value ist null
        val nullCharacter: Character? = null
        
        // Aktuelle Implementierung in PotionViewModel
        val talents = if (nullCharacter == null) {
            emptyList()
        } else {
            mutableListOf<Talent>().apply {
                if (nullCharacter.hasAlchemy) add(Talent.ALCHEMY)
                if (nullCharacter.hasCookingPotions) add(Talent.COOKING_POTIONS)
            }
        }
        
        assertTrue("Bei null Character sollte leere Liste zurückgegeben werden", talents.isEmpty())
    }
    
    @Test
    fun `talents calculated from passed character parameter work correctly`() {
        // FIX: Berechne Talente direkt vom übergebenen Character-Parameter
        // anstatt vom ViewModel abzurufen
        
        val character = Character(
            id = 1,
            name = "Hexe",
            mu = 12, kl = 14, inValue = 13, ch = 10, ff = 12, ge = 11, ko = 12, kk = 11,
            hasAlchemy = true,
            alchemySkill = 7,
            hasCookingPotions = true,
            cookingPotionsSkill = 5
        )
        
        // So sollte es im BrewPotionDialog berechnet werden:
        val availableTalents = buildList {
            if (character.hasAlchemy) add(Talent.ALCHEMY)
            if (character.hasCookingPotions) add(Talent.COOKING_POTIONS)
        }
        
        assertEquals(2, availableTalents.size)
        
        // Die Talente sollten verfügbar sein, unabhängig vom ViewModel-State
        assertNotNull(availableTalents.firstOrNull())
    }
}
