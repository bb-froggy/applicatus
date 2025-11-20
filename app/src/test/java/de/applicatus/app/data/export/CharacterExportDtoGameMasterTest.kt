package de.applicatus.app.data.export

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Tests für CharacterExportDto und Import-Logik.
 * 
 * Hinweis: Der Spielleitermodus wurde von Character.isGameMaster zu Group.isGameMasterGroup
 * verschoben. Diese Tests prüfen die grundlegende Serialisierung von CharacterDto.
 */
class CharacterExportDtoGameMasterTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `CharacterDto serializes basic character fields`() {
        // Given: Ein CharacterDto mit grundlegenden Eigenschaften
        val guid = UUID.randomUUID().toString()
        val characterDto = CharacterDto(
            guid = guid,
            name = "Test Character",
            mu = 12,
            kl = 14,
            inValue = 13,
            ch = 11,
            ff = 10,
            ge = 12,
            ko = 11,
            kk = 10
        )

        val exportDto = CharacterExportDto(
            version = 11,
            character = characterDto,
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            exportTimestamp = System.currentTimeMillis()
        )

        // When: Serialisierung zu JSON
        val jsonString = json.encodeToString(
            kotlinx.serialization.serializer<CharacterExportDto>(),
            exportDto
        )

        // Then: JSON enthält die wichtigen Felder
        assertTrue("JSON sollte 'name' enthalten", jsonString.contains("\"name\""))
        assertTrue("JSON sollte 'guid' enthalten", jsonString.contains("\"guid\""))
        assertTrue("JSON sollte 'mu' enthalten", jsonString.contains("\"mu\""))
    }

    @Test
    fun `CharacterDto toCharacter creates character with correct attributes`() {
        // Given: Ein CharacterDto mit Eigenschaften
        val guid = UUID.randomUUID().toString()
        val characterDto = CharacterDto(
            guid = guid,
            name = "New Character",
            mu = 10,
            kl = 11,
            inValue = 12,
            ch = 13,
            ff = 14,
            ge = 15,
            ko = 16,
            kk = 17
        )

        // When: Konvertierung zu Character
        val character = characterDto.toCharacter()

        // Then: Character hat die korrekten Werte
        assertEquals("Neue Charaktere sollten korrekten Namen haben", "New Character", character.name)
        assertEquals("MU sollte korrekt sein", 10, character.mu)
        assertEquals("KL sollte korrekt sein", 11, character.kl)
        assertEquals("IN sollte korrekt sein", 12, character.inValue)
    }

    @Test
    fun `JSON deserializes correctly to CharacterDto`() {
        // Given: Ein JSON-String mit Charakter-Daten
        val guid = UUID.randomUUID().toString()
        val jsonString = """
        {
            "version": 11,
            "character": {
                "guid": "$guid",
                "name": "Imported Character",
                "mu": 14,
                "kl": 15,
                "inValue": 13,
                "ch": 12,
                "ff": 11,
                "ge": 13,
                "ko": 12,
                "kk": 11
            },
            "spellSlots": [],
            "potions": [],
            "recipeKnowledge": [],
            "exportTimestamp": ${System.currentTimeMillis()}
        }
        """.trimIndent()

        // When: Deserialisierung
        val exportDto = json.decodeFromString<CharacterExportDto>(jsonString)

        // Then: DTO wurde korrekt erstellt
        assertEquals(guid, exportDto.character.guid)
        assertEquals("Imported Character", exportDto.character.name)
        assertEquals(14, exportDto.character.mu)
        assertEquals(15, exportDto.character.kl)

        // When: Konvertierung zu Character
        val character = exportDto.character.toCharacter()

        // Then: Character hat korrekte Eigenschaften
        assertEquals("Importierter Character sollte korrekten Namen haben", "Imported Character", character.name)
        assertEquals("MU sollte korrekt importiert werden", 14, character.mu)
    }

    @Test
    fun `export and reimport cycle preserves character attributes`() {
        // This test simulates a complete export->import cycle
        
        // 1. Create original DTO
        val guid = UUID.randomUUID().toString()
        val originalDto = CharacterExportDto(
            version = 11,
            character = CharacterDto(
                guid = guid,
                name = "Cycle Test",
                mu = 12,
                kl = 13,
                inValue = 14,
                ch = 15,
                ff = 16,
                ge = 17,
                ko = 18,
                kk = 19
            ),
            spellSlots = emptyList(),
            potions = emptyList(),
            recipeKnowledge = emptyList(),
            exportTimestamp = System.currentTimeMillis()
        )

        // 2. Export to JSON
        val jsonString = json.encodeToString(
            kotlinx.serialization.serializer<CharacterExportDto>(),
            originalDto
        )

        // 3. Import from JSON
        val importedDto = json.decodeFromString<CharacterExportDto>(jsonString)

        // 4. Convert to Character
        val character = importedDto.character.toCharacter()

        // 5. Verify: Character has correct attributes
        assertEquals("Nach Export->Import-Zyklus sollte Name erhalten bleiben", "Cycle Test", character.name)
        assertEquals("MU sollte erhalten bleiben", 12, character.mu)
        assertEquals("KL sollte erhalten bleiben", 13, character.kl)
        assertEquals("KK sollte erhalten bleiben", 19, character.kk)
    }
}
