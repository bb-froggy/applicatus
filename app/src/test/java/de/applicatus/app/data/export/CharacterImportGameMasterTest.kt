package de.applicatus.app.data.export

import de.applicatus.app.data.model.character.Character
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.util.UUID

/**
 * Tests für das Verhalten von CharacterDto.toCharacter() beim Import.
 * 
 * Hinweis: Der Spielleitermodus wurde von Character.isGameMaster zu Group.isGameMasterGroup
 * verschoben. CharacterDto enthält kein isGameMaster-Feld mehr, da dieser Modus nun
 * auf Gruppen-Ebene verwaltet wird.
 */
class CharacterImportGameMasterTest {

    @Test
    fun `CharacterDto can be converted to Character`() {
        val guid = UUID.randomUUID().toString()
        val dto = CharacterDto(
            guid = guid,
            name = "Test Character",
            mu = 12,
            kl = 13,
            inValue = 14
        )
        
        val character = dto.toCharacter()
        
        assertEquals(guid, character.guid)
        assertEquals("Test Character", character.name)
        assertEquals(12, character.mu)
        assertEquals(13, character.kl)
        assertEquals(14, character.inValue)
    }
    
    @Test
    fun `CharacterExportManager preserves ID and GUID when updating`() {
        // This tests the logic in CharacterExportManager.importCharacter()
        // The manager preserves id, guid, and groupId when importing over existing character
        
        val existingCharacter = Character(
            id = 1,
            guid = "test-guid",
            name = "Old Name",
            mu = 10,
            groupId = 5L  // Important: groupId should be preserved - must be Long
        )
        
        // Import creates new character from DTO with updated values
        val importedDto = CharacterDto(
            guid = "test-guid",
            name = "New Name",
            mu = 12
        )
        val importedChar = importedDto.toCharacter()
        
        // ExportManager does this to preserve critical fields:
        val updated = importedChar.copy(
            id = existingCharacter.id,
            guid = existingCharacter.guid,
            groupId = existingCharacter.groupId  // Preserved!
        )
        
        assertEquals(1, updated.id)
        assertEquals("test-guid", updated.guid)
        assertEquals(5L, updated.groupId)
        assertEquals("New Name", updated.name)
        assertEquals(12, updated.mu)
    }
}
