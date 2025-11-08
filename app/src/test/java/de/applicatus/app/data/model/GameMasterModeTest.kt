package de.applicatus.app.data.model

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.model.spell.SlotType
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests für die Spieler/Spielleiter-Funktionalität
 */
class GameMasterModeTest {
    
    @Test
    fun `Character hat isGameMaster Standardwert false`() {
        val character = Character(
            id = 1,
            name = "Test Charakter"
        )
        
        assertFalse("isGameMaster sollte standardmäßig false sein", character.isGameMaster)
    }
    
    @Test
    fun `Character kann als Spielleiter markiert werden`() {
        val character = Character(
            id = 1,
            name = "Test Charakter",
            isGameMaster = true
        )
        
        assertTrue("isGameMaster sollte true sein", character.isGameMaster)
    }
    
    @Test
    fun `SpellSlot hat isBotched Standardwert false`() {
        val slot = SpellSlot(
            characterId = 1,
            slotNumber = 0,
            slotType = SlotType.SPELL_STORAGE,
            spellId = null
        )
        
        assertFalse("isBotched sollte standardmäßig false sein", slot.isBotched)
    }
    
    @Test
    fun `SpellSlot kann als verpatzt markiert werden`() {
        val slot = SpellSlot(
            characterId = 1,
            slotNumber = 0,
            slotType = SlotType.SPELL_STORAGE,
            spellId = 1,
            isFilled = true,
            isBotched = true,
            zfpStar = null
        )
        
        assertTrue("Slot sollte gefüllt sein (auch bei Patzer)", slot.isFilled)
        assertTrue("Slot sollte als verpatzt markiert sein", slot.isBotched)
        assertNull("Bei Patzer sollte keine ZfP* gespeichert werden", slot.zfpStar)
    }
    
    @Test
    fun `Erfolgreicher Zauber hat kein isBotched-Flag`() {
        val slot = SpellSlot(
            characterId = 1,
            slotNumber = 0,
            slotType = SlotType.SPELL_STORAGE,
            spellId = 1,
            isFilled = true,
            isBotched = false,
            zfpStar = 10
        )
        
        assertTrue("Slot sollte gefüllt sein", slot.isFilled)
        assertFalse("Slot sollte nicht als verpatzt markiert sein", slot.isBotched)
        assertNotNull("Bei Erfolg sollte ZfP* gespeichert werden", slot.zfpStar)
        assertEquals("ZfP* sollte korrekt sein", 10, slot.zfpStar)
    }
    
    @Test
    fun `Normal fehlgeschlagener Zauber belegt Slot nicht`() {
        val slot = SpellSlot(
            characterId = 1,
            slotNumber = 0,
            slotType = SlotType.SPELL_STORAGE,
            spellId = 1,
            isFilled = false,  // Slot bleibt leer bei normalem Fehlschlag
            isBotched = false,
            zfpStar = null
        )
        
        assertFalse("Slot sollte bei normalem Fehlschlag leer bleiben", slot.isFilled)
        assertFalse("Kein Patzer-Flag bei normalem Fehlschlag", slot.isBotched)
        assertNull("Keine ZfP* bei Fehlschlag", slot.zfpStar)
    }
    
    @Test
    fun `Character kann von Spieler zu Spielleiter geändert werden`() {
        var character = Character(
            id = 1,
            name = "Test Charakter",
            isGameMaster = false
        )
        
        assertFalse(character.isGameMaster)
        
        character = character.copy(isGameMaster = true)
        
        assertTrue("Character sollte jetzt Spielleiter sein", character.isGameMaster)
    }
}
