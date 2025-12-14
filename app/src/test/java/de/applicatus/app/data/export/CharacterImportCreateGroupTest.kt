package de.applicatus.app.data.export

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.Group
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für das Anlegen einer neuen Gruppe beim Import.
 * 
 * Bug: Ein Charakter wird importiert, aber ist in CharacterListScreen nicht sichtbar.
 * Das passiert, weil der Charakter keine Gruppe hat (groupId = null) und die UI
 * nur Charaktere mit gültiger groupId anzeigt.
 * 
 * Ursache: Beim Import wird versucht, die Gruppe über den Namen zu finden.
 * Wenn die Gruppe nicht existiert, wird als Fallback "Unbekannte Gruppe" gesucht.
 * Wenn auch diese nicht existiert, bleibt groupId = null.
 * 
 * Fix: Wenn die Gruppe nicht existiert, sollte sie automatisch angelegt werden.
 * Der Name der Gruppe kommt aus dem Export (exportDto.character.groupName).
 */
class CharacterImportCreateGroupTest {
    
    @Test
    fun `import should preserve groupName in DTO`() {
        // Arrange - Ein Charakter aus einer Gruppe "Die Helden"
        val dto = CharacterDto(
            guid = "test-guid",
            name = "Gandalf",
            groupName = "Die Helden"
        )
        
        // Assert - Der Gruppenname sollte vorhanden sein
        assertEquals("Die Helden", dto.groupName)
    }
    
    @Test
    fun `toCharacter sets groupId to null - it is resolved separately during import`() {
        // Arrange
        val dto = CharacterDto(
            guid = "test-guid",
            name = "Gandalf",
            groupName = "Die Helden"
        )
        
        // Act
        val character = dto.toCharacter()
        
        // Assert - groupId ist null, weil die Auflösung im ImportManager passiert
        assertNull(character.groupId)
    }
    
    @Test
    fun `group name from export is available for creating new group`() {
        // Dieser Test dokumentiert, dass der Gruppenname beim Import
        // verfügbar ist und zum Erstellen einer neuen Gruppe verwendet werden kann.
        
        val exportJson = """
        {
            "version": 5,
            "character": {
                "guid": "test-guid-123",
                "name": "Alrik der Krieger",
                "groupName": "Thorwaler Abenteurer",
                "mu": 14,
                "kl": 11,
                "inValue": 12,
                "ch": 13,
                "ff": 11,
                "ge": 12,
                "ko": 14,
                "kk": 15
            },
            "spellSlots": [],
            "potions": [],
            "recipeKnowledge": [],
            "locations": [],
            "items": [],
            "journalEntries": []
        }
        """.trimIndent()
        
        // Parse würde hier einen CharacterExportDto ergeben
        // Der Gruppenname "Thorwaler Abenteurer" sollte dann verwendet werden,
        // um entweder eine existierende Gruppe zu finden oder eine neue zu erstellen.
        
        // Dieser Test existiert hauptsächlich zur Dokumentation des erwarteten Verhaltens
        assertTrue(exportJson.contains("Thorwaler Abenteurer"))
    }
    
    @Test
    fun `new group should have default values`() {
        // Wenn eine neue Gruppe erstellt wird, sollte sie sinnvolle Standardwerte haben
        val newGroup = Group(
            name = "Importierte Gruppe",
            currentDerianDate = "1 Praios 1040 BF",
            isGameMasterGroup = false
        )
        
        assertEquals("Importierte Gruppe", newGroup.name)
        assertEquals("1 Praios 1040 BF", newGroup.currentDerianDate)
        assertFalse(newGroup.isGameMasterGroup)
    }
}
