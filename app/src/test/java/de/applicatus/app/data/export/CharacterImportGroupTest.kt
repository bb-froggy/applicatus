package de.applicatus.app.data.export

import de.applicatus.app.data.model.character.Character
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für Group-Export/Import beim Character-Import.
 * 
 * Problem (alt): IDs sind gerätespezifisch und führten zu FK-Constraint-Fehlern.
 * 
 * Lösung (neu): Gruppennamen werden exportiert/importiert statt IDs.
 * - Beim Export: groupName wird aus Group geladen
 * - Beim Import: Group wird über Namen gefunden
 * - Fallback: "Unbekannte Gruppe" wenn Name nicht existiert
 * - Überschreiben: groupId wird NICHT geändert
 */
class CharacterImportGroupTest {
    
    @Test
    fun `toCharacter should set groupId to null`() {
        // Arrange
        val dto = CharacterDto(
            guid = "test-guid",
            name = "Test Character",
            groupName = "TestGroup"
        )
        
        // Act
        val character = dto.toCharacter()
        
        // Assert
        assertNull("GroupId should be null - wird beim Import separat aufgelöst", character.groupId)
    }
    
    @Test
    fun `toCharacter should handle null groupName`() {
        // Arrange
        val dto = CharacterDto(
            guid = "test-guid",
            name = "Test Character",
            groupName = null
        )
        
        // Act
        val character = dto.toCharacter()
        
        // Assert
        assertNull("GroupId should be null", character.groupId)
    }
}
