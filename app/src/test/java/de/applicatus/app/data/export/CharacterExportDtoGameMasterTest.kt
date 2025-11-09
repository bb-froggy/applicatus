package de.applicatus.app.data.export

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Tests für CharacterExportDto und Import-Logik bezüglich isGameMaster.
 * Diese Tests prüfen, dass das isGameMaster-Feld NICHT im Export enthalten ist.
 */
class CharacterExportDtoGameMasterTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `CharacterDto does not serialize isGameMaster field`() {
        // Given: Ein CharacterDto (ohne isGameMaster-Feld im DTO)
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

        // Then: JSON enthält KEIN isGameMaster-Feld
        assertFalse(
            "JSON sollte kein 'isGameMaster'-Feld enthalten",
            jsonString.contains("isGameMaster")
        )

        // Aber enthält die anderen Felder
        assertTrue("JSON sollte 'name' enthalten", jsonString.contains("\"name\""))
        assertTrue("JSON sollte 'guid' enthalten", jsonString.contains("\"guid\""))
        assertTrue("JSON sollte 'mu' enthalten", jsonString.contains("\"mu\""))
    }

    @Test
    fun `CharacterDto toCharacter creates character with default isGameMaster false`() {
        // Given: Ein CharacterDto ohne isGameMaster-Feld
        val guid = UUID.randomUUID().toString()
        val characterDto = CharacterDto(
            guid = guid,
            name = "New Character",
            mu = 10,
            kl = 10,
            inValue = 10,
            ch = 10,
            ff = 10,
            ge = 10,
            ko = 10,
            kk = 10
        )

        // When: Konvertierung zu Character
        val character = characterDto.toCharacter()

        // Then: Character hat isGameMaster=false (Default-Wert aus Character-Klasse)
        assertFalse(
            "Neu erstellter Character sollte isGameMaster=false haben",
            character.isGameMaster
        )
    }

    @Test
    fun `JSON without isGameMaster field deserializes correctly`() {
        // Given: Ein JSON-String OHNE isGameMaster-Feld
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

        // Then: Character hat Default-Wert für isGameMaster (false)
        assertFalse(
            "Importierter Character sollte isGameMaster=false haben (Default)",
            character.isGameMaster
        )
    }

    @Test
    fun `JSON with extra isGameMaster field is ignored during import`() {
        // Given: Ein JSON mit isGameMaster-Feld (sollte ignoriert werden)
        val guid = UUID.randomUUID().toString()
        val jsonString = """
        {
            "version": 11,
            "character": {
                "guid": "$guid",
                "name": "Character with GM field",
                "mu": 10,
                "kl": 10,
                "inValue": 10,
                "ch": 10,
                "ff": 10,
                "ge": 10,
                "ko": 10,
                "kk": 10,
                "isGameMaster": true
            },
            "spellSlots": [],
            "potions": [],
            "recipeKnowledge": [],
            "exportTimestamp": ${System.currentTimeMillis()}
        }
        """.trimIndent()

        // When: Deserialisierung (ignoreUnknownKeys = true)
        val exportDto = json.decodeFromString<CharacterExportDto>(jsonString)

        // Then: Deserialisierung erfolgreich (Feld wurde ignoriert)
        assertEquals(guid, exportDto.character.guid)
        assertEquals("Character with GM field", exportDto.character.name)

        // When: Konvertierung zu Character
        val character = exportDto.character.toCharacter()

        // Then: Character hat Default-Wert (false), nicht den Wert aus JSON
        assertFalse(
            "isGameMaster-Feld aus JSON sollte ignoriert werden",
            character.isGameMaster
        )
    }

    @Test
    fun `export and reimport cycle does not transfer isGameMaster`() {
        // This test simulates a complete export->import cycle
        
        // 1. Create original DTO
        val guid = UUID.randomUUID().toString()
        val originalDto = CharacterExportDto(
            version = 11,
            character = CharacterDto(
                guid = guid,
                name = "Cycle Test",
                mu = 12,
                kl = 12,
                inValue = 12,
                ch = 12,
                ff = 12,
                ge = 12,
                ko = 12,
                kk = 12
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

        // 3. Verify: No isGameMaster in JSON
        assertFalse(jsonString.contains("isGameMaster"))

        // 4. Import from JSON
        val importedDto = json.decodeFromString<CharacterExportDto>(jsonString)

        // 5. Convert to Character
        val character = importedDto.character.toCharacter()

        // 6. Verify: Character has default isGameMaster=false
        assertFalse(
            "Nach Export->Import-Zyklus sollte isGameMaster=false sein",
            character.isGameMaster
        )
    }
}
