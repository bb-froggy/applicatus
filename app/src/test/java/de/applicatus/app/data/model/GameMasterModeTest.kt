package de.applicatus.app.data.model

import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.model.spell.SlotType
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests für die Spieler/Spielleiter-Funktionalität
 * 
 * Hinweis: Der Spielleitermodus ist jetzt auf Gruppen-Ebene (Group.isGameMasterGroup),
 * nicht mehr auf Charakter-Ebene.
 */
class GameMasterModeTest {
    
    @Test
    fun `Group hat isGameMasterGroup Standardwert false`() {
        val group = Group(
            id = 1,
            name = "Test Gruppe"
        )
        
        assertFalse("isGameMasterGroup sollte standardmäßig false sein", group.isGameMasterGroup)
    }
    
    @Test
    fun `Group kann als Spielleiter-Gruppe markiert werden`() {
        val group = Group(
            id = 1,
            name = "Test Gruppe",
            isGameMasterGroup = true
        )
        
        assertTrue("isGameMasterGroup sollte true sein", group.isGameMasterGroup)
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
    fun `Group kann von Spieler zu Spielleiter geändert werden`() {
        var group = Group(
            id = 1,
            name = "Test Gruppe",
            isGameMasterGroup = false
        )
        
        assertFalse(group.isGameMasterGroup)
        
        group = group.copy(isGameMasterGroup = true)
        
        assertTrue("Group sollte jetzt Spielleiter-Gruppe sein", group.isGameMasterGroup)
    }
}
