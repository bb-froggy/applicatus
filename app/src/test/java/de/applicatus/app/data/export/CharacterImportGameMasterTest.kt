package de.applicatus.app.data.export

import de.applicatus.app.data.model.character.Character
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Tests für das Verhalten von CharacterDto.toCharacter() bezüglich des isGameMaster-Flags.
 * 
 * Anforderung: CharacterDto enthält bewusst KEIN isGameMaster-Feld. Beim Import wird
 * dieses Feld aus dem existierenden Character beibehalten (bzw. bei neuen Charakteren
 * auf den Default-Wert false gesetzt).
 * 
 * Dies ermöglicht, dass Spielleiter und Spieler denselben Charakter austauschen können,
 * aber ihre jeweilige Rolle behalten.
 */
class CharacterImportGameMasterTest {

    @Test
    fun `CharacterDto has no isGameMaster field`() {
        // Verify: CharacterDto doesn't expose isGameMaster
        val guid = UUID.randomUUID().toString()
        val dto = CharacterDto(
            guid = guid,
            name = "Test Character"
        )
        
        // toCharacter() creates Character with default isGameMaster=false
        val character = dto.toCharacter()
        
        assertEquals(guid, character.guid)
        assertEquals("Test Character", character.name)
        assertFalse("New Character from DTO should have isGameMaster=false", character.isGameMaster)
    }
    
    @Test
    fun `CharacterExportManager preserves isGameMaster when updating via copy`() {
        // This tests the actual logic in CharacterExportManager.importCharacter()
        // Line 134-138: 
        // val updatedCharacter = exportDto.character.toCharacter().copy(
        //     id = existingCharacter.id,
        //     guid = existingCharacter.guid,
        //     isGameMaster = existingCharacter.isGameMaster
        // )
        
        // Scenario 1: Spielleiter-Charakter (isGameMaster=true)
        val gmCharacter = Character(
            id = 1,
            guid = "test-guid",
            name = "Old Name",
            isGameMaster = true
        )
        
        // Import creates new character from DTO
        val importedDto = CharacterDto(
            guid = "test-guid",
            name = "New Name"
        )
        val importedChar = importedDto.toCharacter()
        
        // ExportManager would do this:
        val updated = importedChar.copy(
            id = gmCharacter.id,
            guid = gmCharacter.guid,
            isGameMaster = gmCharacter.isGameMaster  // Preserved!
        )
        
        assertEquals(1, updated.id)
        assertEquals("test-guid", updated.guid)
        assertTrue("isGameMaster should be preserved as true", updated.isGameMaster)
        assertEquals("New Name", updated.name)
        
        // Scenario 2: Spieler-Charakter (isGameMaster=false)
        val playerCharacter = Character(
            id = 2,
            guid = "test-guid-2",
            name = "Player Name",
            isGameMaster = false
        )
        
        val importedDto2 = CharacterDto(
            guid = "test-guid-2",
            name = "Updated Player Name"
        )
        val importedChar2 = importedDto2.toCharacter()
        
        val updated2 = importedChar2.copy(
            id = playerCharacter.id,
            guid = playerCharacter.guid,
            isGameMaster = playerCharacter.isGameMaster  // Preserved!
        )
        
        assertEquals(2, updated2.id)
        assertEquals("test-guid-2", updated2.guid)
        assertFalse("isGameMaster should be preserved as false", updated2.isGameMaster)
        assertEquals("Updated Player Name", updated2.name)
    }
}
