package de.applicatus.app.data.export

import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.model.SlotType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für die Export/Import DTOs und JSON-Serialisierung.
 */
class CharacterExportDtoTest {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Test
    fun `CharacterDto serializes correctly`() {
        val characterDto = CharacterDto(
            id = 1,
            name = "Test Charakter",
            mu = 12,
            kl = 14,
            inValue = 13,
            ch = 11,
            ff = 10,
            ge = 12,
            ko = 11,
            kk = 10,
            hasApplicatus = true,
            applicatusZfw = 10,
            applicatusModifier = -2
        )
        
        val jsonString = json.encodeToString(characterDto)
        
        assertTrue(jsonString.contains("Test Charakter"))
        assertTrue(jsonString.contains("\"mu\": 12"))
        assertTrue(jsonString.contains("\"hasApplicatus\": true"))
        assertTrue(jsonString.contains("\"applicatusZfw\": 10"))
        
        val decoded = json.decodeFromString<CharacterDto>(jsonString)
        assertEquals(characterDto, decoded)
    }
    
    @Test
    fun `CharacterDto deserializes correctly`() {
        val jsonString = """
            {
                "id": 1,
                "name": "Test",
                "mu": 8,
                "kl": 8,
                "inValue": 8,
                "ch": 8,
                "ff": 8,
                "ge": 8,
                "ko": 8,
                "kk": 8,
                "hasApplicatus": false,
                "applicatusZfw": 0,
                "applicatusModifier": 0
            }
        """.trimIndent()
        
        val dto = json.decodeFromString<CharacterDto>(jsonString)
        
        assertEquals("Test", dto.name)
        assertEquals(8, dto.mu)
        assertFalse(dto.hasApplicatus)
    }
    
    @Test
    fun `SpellSlotDto serializes with spell name`() {
        val slotDto = SpellSlotDto(
            slotNumber = 0,
            slotType = "APPLICATUS",
            volumePoints = 0,
            spellId = 1,
            spellName = "Fulminictus",
            zfw = 10,
            modifier = -2,
            variant = "Verstärkt",
            isFilled = true,
            zfpStar = 8,
            lastRollResult = "Erfolg! [12, 14, 8] ZfP*: 8",
            applicatusRollResult = "Erfolg! [10, 11, 9] ZfP*: 10"
        )
        
        val jsonString = json.encodeToString(slotDto)
        
        assertTrue(jsonString.contains("Fulminictus"))
        assertTrue(jsonString.contains("APPLICATUS"))
        assertTrue(jsonString.contains("\"zfw\": 10"))
        assertTrue(jsonString.contains("Verstärkt"))
        
        val decoded = json.decodeFromString<SpellSlotDto>(jsonString)
        assertEquals(slotDto, decoded)
    }
    
    @Test
    fun `CharacterExportDto contains version`() {
        val timestamp = System.currentTimeMillis()
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                name = "Test",
                mu = 8, kl = 8, inValue = 8, ch = 8,
                ff = 8, ge = 8, ko = 8, kk = 8
            ),
            spellSlots = listOf(),
            exportTimestamp = timestamp  // Explizit angeben
        )
        
        val jsonString = json.encodeToString(exportDto)
        println("Generated JSON: $jsonString")
        
        // Prüfe, dass alle Felder vorhanden sind
        assertTrue("JSON should contain version field: $jsonString", 
            jsonString.contains("\"version\""))
        assertTrue("JSON should contain version value: $jsonString",
            jsonString.contains(DataModelVersion.CURRENT_VERSION.toString()))
        assertTrue("JSON should contain character name", jsonString.contains("Test"))
        assertTrue("JSON should contain export timestamp", jsonString.contains("exportTimestamp"))
        
        val decoded = json.decodeFromString<CharacterExportDto>(jsonString)
        assertEquals(DataModelVersion.CURRENT_VERSION, decoded.version)
        assertEquals("Test", decoded.character.name)
        assertEquals(timestamp, decoded.exportTimestamp)
    }
    
    @Test
    fun `CharacterExportDto with slots serializes correctly`() {
        val exportDto = CharacterExportDto(
            version = 2,
            character = CharacterDto(
                name = "Gandalf",
                mu = 14, kl = 15, inValue = 14, ch = 13,
                ff = 10, ge = 11, ko = 12, kk = 9,
                hasApplicatus = true,
                applicatusZfw = 12,
                applicatusModifier = 0
            ),
            spellSlots = listOf(
                SpellSlotDto(
                    slotNumber = 0,
                    slotType = "APPLICATUS",
                    volumePoints = 0,
                    spellId = 1,
                    spellName = "Ignifaxius",
                    zfw = 10,
                    modifier = 0,
                    variant = "",
                    isFilled = false,
                    zfpStar = null,
                    lastRollResult = null,
                    applicatusRollResult = null
                ),
                SpellSlotDto(
                    slotNumber = 1,
                    slotType = "SPELL_STORAGE",
                    volumePoints = 50,
                    spellId = 2,
                    spellName = "Fulminictus",
                    zfw = 12,
                    modifier = -1,
                    variant = "Stark",
                    isFilled = true,
                    zfpStar = 10,
                    lastRollResult = "Erfolg! [10, 12, 11] ZfP*: 10",
                    applicatusRollResult = null
                )
            ),
            exportTimestamp = System.currentTimeMillis()
        )
        
        val jsonString = json.encodeToString(exportDto)
        
        assertTrue(jsonString.contains("Gandalf"))
        assertTrue(jsonString.contains("Ignifaxius"))
        assertTrue(jsonString.contains("Fulminictus"))
        assertTrue(jsonString.contains("APPLICATUS"))
        assertTrue(jsonString.contains("SPELL_STORAGE"))
        assertTrue(jsonString.contains("\"volumePoints\": 50"))
        
        val decoded = json.decodeFromString<CharacterExportDto>(jsonString)
        assertEquals(2, decoded.spellSlots.size)
        assertEquals("Gandalf", decoded.character.name)
        assertEquals("Ignifaxius", decoded.spellSlots[0].spellName)
        assertEquals("Fulminictus", decoded.spellSlots[1].spellName)
        assertEquals(50, decoded.spellSlots[1].volumePoints)
    }
    
    @Test
    fun `ignores unknown JSON fields`() {
        val jsonWithExtraFields = """
            {
                "version": 2,
                "character": {
                    "id": 1,
                    "name": "Test",
                    "mu": 8, "kl": 8, "inValue": 8, "ch": 8,
                    "ff": 8, "ge": 8, "ko": 8, "kk": 8,
                    "hasApplicatus": false,
                    "applicatusZfw": 0,
                    "applicatusModifier": 0,
                    "unknownField": "should be ignored"
                },
                "spellSlots": [],
                "exportTimestamp": 1234567890,
                "futureField": "from version 3"
            }
        """.trimIndent()
        
        // Sollte nicht werfen, wegen ignoreUnknownKeys
        val decoded = json.decodeFromString<CharacterExportDto>(jsonWithExtraFields)
        assertEquals("Test", decoded.character.name)
        assertEquals(2, decoded.version)
    }
    
    @Test
    fun `SpellSlotDto converts SlotType correctly`() {
        val applicatusSlot = SpellSlotDto(
            slotNumber = 0,
            slotType = "APPLICATUS",
            volumePoints = 0,
            spellId = null,
            spellName = null,
            zfw = 0,
            modifier = 0,
            variant = ""
        )
        
        val storageSlot = SpellSlotDto(
            slotNumber = 1,
            slotType = "SPELL_STORAGE",
            volumePoints = 25,
            spellId = null,
            spellName = null,
            zfw = 0,
            modifier = 0,
            variant = ""
        )
        
        val applicatusModel = applicatusSlot.toSpellSlot(characterId = 1, resolvedSpellId = null)
        val storageModel = storageSlot.toSpellSlot(characterId = 1, resolvedSpellId = null)
        
        assertEquals(SlotType.APPLICATUS, applicatusModel.slotType)
        assertEquals(SlotType.SPELL_STORAGE, storageModel.slotType)
        assertEquals(25, storageModel.volumePoints)
    }
    
    @Test
    fun `handles invalid SlotType gracefully`() {
        val invalidSlot = SpellSlotDto(
            slotNumber = 0,
            slotType = "INVALID_TYPE",
            volumePoints = 0,
            spellId = null,
            spellName = null,
            zfw = 0,
            modifier = 0,
            variant = ""
        )
        
        val model = invalidSlot.toSpellSlot(characterId = 1, resolvedSpellId = null)
        
        // Sollte auf APPLICATUS zurückfallen
        assertEquals(SlotType.APPLICATUS, model.slotType)
    }
}
