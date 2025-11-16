package de.applicatus.app.data.export

import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.model.potion.AnalysisStatus
import de.applicatus.app.data.model.potion.IntensityQuality
import de.applicatus.app.data.model.potion.KnownQualityLevel
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.model.spell.SlotType
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
        val timestamp = System.currentTimeMillis()
        val characterDto = CharacterDto(
            id = 1,
            guid = "test-guid-abc",
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
            applicatusModifier = -2,
            hasAlchemy = true,
            alchemySkill = 7,
            hasCookingPotions = true,
            cookingPotionsSkill = 6,
            hasOdem = true,
            odemZfw = 8,
            hasAnalys = true,
            analysZfw = 9,
            currentLe = 28,
            maxLe = 35,
            leRegenBonus = 2,
            hasAe = true,
            currentAe = 25,
            maxAe = 35,
            aeRegenBonus = 1,
            hasMasteryRegeneration = true,
            hasKe = true,
            currentKe = 5,
            maxKe = 10,
            lastModifiedDate = timestamp
        )
        
        val jsonString = json.encodeToString(characterDto)
        
        assertTrue(jsonString.contains("Test Charakter"))
        assertTrue(jsonString.contains("test-guid-abc"))
        assertTrue(jsonString.contains("\"mu\": 12"))
        assertTrue(jsonString.contains("\"hasApplicatus\": true"))
        assertTrue(jsonString.contains("\"applicatusZfw\": 10"))
        assertTrue(jsonString.contains("\"alchemySkill\": 7"))
        assertTrue(jsonString.contains("\"currentAe\": 25"))
        
        val decoded = json.decodeFromString<CharacterDto>(jsonString)
        assertEquals(characterDto, decoded)
    }
    
    @Test
    fun `CharacterDto deserializes correctly`() {
        val jsonString = """
            {
                "id": 1,
                "guid": "test-guid-def",
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
        assertEquals("test-guid-def", dto.guid)
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
                guid = "test-guid-123",
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
        assertTrue("JSON should contain guid", jsonString.contains("test-guid-123"))
        assertTrue("JSON should contain character name", jsonString.contains("Test"))
        assertTrue("JSON should contain export timestamp", jsonString.contains("exportTimestamp"))
        
        val decoded = json.decodeFromString<CharacterExportDto>(jsonString)
        assertEquals(DataModelVersion.CURRENT_VERSION, decoded.version)
        assertEquals("test-guid-123", decoded.character.guid)
        assertEquals("Test", decoded.character.name)
        assertEquals(timestamp, decoded.exportTimestamp)
    }
    
    @Test
    fun `CharacterExportDto with slots serializes correctly`() {
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "gandalf-guid-456",
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
            potions = listOf(
                PotionDto(
                    guid = "test-guid-1",
                    recipeId = 42,
                    recipeName = "Heiltrank",
                    actualQuality = PotionQuality.B.name,
                    appearance = "klar",
                    expiryDate = "1 Efferd 1041 BF",
                    categoryKnown = true,
                    knownQualityLevel = KnownQualityLevel.EXACT.name,
                    knownExactQuality = PotionQuality.B.name
                )
            ),
            recipeKnowledge = listOf(
                RecipeKnowledgeDto(
                    recipeId = 42,
                    recipeName = "Heiltrank",
                    knowledgeLevel = RecipeKnowledgeLevel.UNDERSTOOD.name
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
        assertTrue(jsonString.contains("Heiltrank"))
        assertTrue(jsonString.contains(PotionQuality.B.name))
        
        val decoded = json.decodeFromString<CharacterExportDto>(jsonString)
        assertEquals(2, decoded.spellSlots.size)
        assertEquals("Gandalf", decoded.character.name)
        assertEquals("Ignifaxius", decoded.spellSlots[0].spellName)
        assertEquals("Fulminictus", decoded.spellSlots[1].spellName)
        assertEquals(50, decoded.spellSlots[1].volumePoints)
        assertEquals(1, decoded.potions.size)
        assertEquals("Heiltrank", decoded.potions.first().recipeName)
        assertEquals(1, decoded.recipeKnowledge.size)
        assertEquals(RecipeKnowledgeLevel.UNDERSTOOD.name, decoded.recipeKnowledge.first().knowledgeLevel)
    }
    
    @Test
    fun `ignores unknown JSON fields`() {
        val jsonWithExtraFields = """
            {
                "version": 2,
                "character": {
                    "id": 1,
                    "guid": "test-guid-789",
                    "name": "Test",
                    "mu": 8, "kl": 8, "inValue": 8, "ch": 8,
                    "ff": 8, "ge": 8, "ko": 8, "kk": 8,
                    "hasApplicatus": false,
                    "applicatusZfw": 0,
                    "applicatusModifier": 0,
                    "unknownField": "should be ignored"
                },
                "spellSlots": [],
                "potions": [],
                "recipeKnowledge": [],
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
    fun `CharacterDto contains GUID field`() {
        val guid = "unique-guid-12345"
        val characterDto = CharacterDto(
            guid = guid,
            name = "TestChar",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        
        val jsonString = json.encodeToString(characterDto)
        
        assertTrue("JSON should contain guid field", jsonString.contains("\"guid\""))
        assertTrue("JSON should contain guid value", jsonString.contains(guid))
        
        val decoded = json.decodeFromString<CharacterDto>(jsonString)
        assertEquals(guid, decoded.guid)
    }
    
    @Test
    fun `GUID is preserved in round-trip serialization`() {
        val originalGuid = "preserved-guid-999"
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = originalGuid,
                name = "RoundTrip",
                mu = 8, kl = 8, inValue = 8, ch = 8,
                ff = 8, ge = 8, ko = 8, kk = 8
            ),
            spellSlots = listOf(),
            exportTimestamp = System.currentTimeMillis()
        )
        
        val jsonString = json.encodeToString(exportDto)
        val decoded = json.decodeFromString<CharacterExportDto>(jsonString)
        
        assertEquals(originalGuid, decoded.character.guid)
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

    @Test
    fun `PotionDto converts to Potion model`() {
        val dto = PotionDto(
            guid = "test-guid-2",
            recipeId = 7,
            recipeName = "Zaubertrank",
            actualQuality = PotionQuality.A.name,
            appearance = "funkelnd",
            expiryDate = "1 Rondra 1042 BF",
            categoryKnown = true,
            knownQualityLevel = KnownQualityLevel.WEAK_OR_STRONG.name,
            intensityQuality = IntensityQuality.STRONG.name
        )

        val model = dto.toPotion(characterId = 3, resolvedRecipeId = 9)

        assertEquals(3, model.characterId)
        assertEquals(9, model.recipeId)
        assertEquals(PotionQuality.A, model.actualQuality)
        assertEquals(KnownQualityLevel.WEAK_OR_STRONG, model.knownQualityLevel)
        assertEquals(IntensityQuality.STRONG, model.intensityQuality)
        assertEquals("funkelnd", model.appearance)
    }

    @Test
    fun `RecipeKnowledgeDto converts to model`() {
        val dto = RecipeKnowledgeDto(
            recipeId = 11,
            recipeName = "Heiltrank",
            knowledgeLevel = RecipeKnowledgeLevel.UNDERSTOOD.name
        )

        val model = dto.toModel(characterId = 5, resolvedRecipeId = 11)

        assertEquals(5, model.characterId)
        assertEquals(11, model.recipeId)
        assertEquals(RecipeKnowledgeLevel.UNDERSTOOD, model.knowledgeLevel)
    }
    
    @Test
    fun `CharacterDto includes magical mastery fields`() {
        val characterDto = CharacterDto(
            guid = "mastery-guid",
            name = "Magier",
            mu = 12, kl = 14, inValue = 13, ch = 11,
            ff = 10, ge = 12, ko = 11, kk = 10,
            hasAlchemy = true,
            alchemySkill = 10,
            alchemyIsMagicalMastery = true,
            hasCookingPotions = true,
            cookingPotionsSkill = 8,
            cookingPotionsIsMagicalMastery = true,
            selfControlSkill = 7,
            sensoryAcuitySkill = 9,
            magicalLoreSkill = 12,
            herbalLoreSkill = 6
        )
        
        val jsonString = json.encodeToString(characterDto)
        
        assertTrue(jsonString.contains("\"alchemyIsMagicalMastery\": true"))
        assertTrue(jsonString.contains("\"cookingPotionsIsMagicalMastery\": true"))
        assertTrue(jsonString.contains("\"selfControlSkill\": 7"))
        assertTrue(jsonString.contains("\"sensoryAcuitySkill\": 9"))
        assertTrue(jsonString.contains("\"magicalLoreSkill\": 12"))
        assertTrue(jsonString.contains("\"herbalLoreSkill\": 6"))
        
        val decoded = json.decodeFromString<CharacterDto>(jsonString)
        assertTrue(decoded.alchemyIsMagicalMastery)
        assertTrue(decoded.cookingPotionsIsMagicalMastery)
        assertEquals(7, decoded.selfControlSkill)
        assertEquals(9, decoded.sensoryAcuitySkill)
        assertEquals(12, decoded.magicalLoreSkill)
        assertEquals(6, decoded.herbalLoreSkill)
    }
    
    @Test
    fun `CharacterDto includes laboratory and group fields`() {
        val characterDto = CharacterDto(
            guid = "lab-guid",
            name = "Alchemist",
            mu = 10, kl = 12, inValue = 11, ch = 10,
            ff = 11, ge = 10, ko = 10, kk = 9,
            defaultLaboratory = "LABORATORY",
            groupName = "TestGroup"
        )
        
        val jsonString = json.encodeToString(characterDto)
        
        assertTrue(jsonString.contains("\"defaultLaboratory\": \"LABORATORY\""))
        assertTrue(jsonString.contains("\"groupName\": \"TestGroup\""))
        
        val decoded = json.decodeFromString<CharacterDto>(jsonString)
        assertEquals("LABORATORY", decoded.defaultLaboratory)
        assertEquals("TestGroup", decoded.groupName)
    }
    
    @Test
    fun `LocationDto serializes correctly`() {
        val locationDto = LocationDto(
            name = "Rucksack",
            isDefault = true,
            isCarried = true,
            sortOrder = 1
        )
        
        val jsonString = json.encodeToString(locationDto)
        
        assertTrue(jsonString.contains("\"name\": \"Rucksack\""))
        assertTrue(jsonString.contains("\"isDefault\": true"))
        assertTrue(jsonString.contains("\"isCarried\": true"))
        assertTrue(jsonString.contains("\"sortOrder\": 1"))
        
        val decoded = json.decodeFromString<LocationDto>(jsonString)
        assertEquals(locationDto, decoded)
    }
    
    @Test
    fun `ItemDto serializes correctly`() {
        val itemDto = ItemDto(
            locationName = "Rucksack",
            name = "Schwert",
            weightStone = 2,
            weightOunces = 10,
            sortOrder = 0,
            isPurse = false,
            kreuzerAmount = 0,
            isCountable = false,
            quantity = 1
        )
        
        val jsonString = json.encodeToString(itemDto)
        
        assertTrue(jsonString.contains("\"locationName\": \"Rucksack\""))
        assertTrue(jsonString.contains("\"name\": \"Schwert\""))
        assertTrue(jsonString.contains("\"weightStone\": 2"))
        assertTrue(jsonString.contains("\"weightOunces\": 10"))
        
        val decoded = json.decodeFromString<ItemDto>(jsonString)
        assertEquals(itemDto, decoded)
    }
    
    @Test
    fun `ItemDto with purse serializes correctly`() {
        val purseDto = ItemDto(
            locationName = "Gürtel",
            name = "Geldbeutel",
            weightStone = 0,
            weightOunces = 5,
            sortOrder = 0,
            isPurse = true,
            kreuzerAmount = 1250,
            isCountable = false,
            quantity = 1
        )
        
        val jsonString = json.encodeToString(purseDto)
        
        assertTrue(jsonString.contains("\"isPurse\": true"))
        assertTrue(jsonString.contains("\"kreuzerAmount\": 1250"))
        
        val decoded = json.decodeFromString<ItemDto>(jsonString)
        assertTrue(decoded.isPurse)
        assertEquals(1250, decoded.kreuzerAmount)
    }
    
    @Test
    fun `ItemDto with countable items serializes correctly`() {
        val countableDto = ItemDto(
            locationName = "Köcher",
            name = "Pfeile",
            weightStone = 0,
            weightOunces = 2,
            sortOrder = 0,
            isPurse = false,
            kreuzerAmount = 0,
            isCountable = true,
            quantity = 20
        )
        
        val jsonString = json.encodeToString(countableDto)
        
        assertTrue(jsonString.contains("\"isCountable\": true"))
        assertTrue(jsonString.contains("\"quantity\": 20"))
        
        val decoded = json.decodeFromString<ItemDto>(jsonString)
        assertTrue(decoded.isCountable)
        assertEquals(20, decoded.quantity)
    }
    
    @Test
    fun `CharacterExportDto with inventory serializes correctly`() {
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "inventory-guid-123",
                name = "Abenteurer",
                mu = 10, kl = 10, inValue = 10, ch = 10,
                ff = 10, ge = 10, ko = 10, kk = 12
            ),
            spellSlots = listOf(),
            potions = listOf(),
            recipeKnowledge = listOf(),
            locations = listOf(
                LocationDto(
                    name = "Rucksack",
                    isDefault = true,
                    isCarried = true,
                    sortOrder = 1
                ),
                LocationDto(
                    name = "Pferd",
                    isDefault = false,
                    isCarried = false,
                    sortOrder = 2
                )
            ),
            items = listOf(
                ItemDto(
                    locationName = "Rucksack",
                    name = "Heiltrank",
                    weightStone = 0,
                    weightOunces = 4,
                    sortOrder = 0
                ),
                ItemDto(
                    locationName = "Pferd",
                    name = "Zelt",
                    weightStone = 5,
                    weightOunces = 0,
                    sortOrder = 0
                )
            ),
            exportTimestamp = System.currentTimeMillis()
        )
        
        val jsonString = json.encodeToString(exportDto)
        
        assertTrue(jsonString.contains("\"locations\""))
        assertTrue(jsonString.contains("\"items\""))
        assertTrue(jsonString.contains("Rucksack"))
        assertTrue(jsonString.contains("Pferd"))
        assertTrue(jsonString.contains("Heiltrank"))
        assertTrue(jsonString.contains("Zelt"))
        
        val decoded = json.decodeFromString<CharacterExportDto>(jsonString)
        assertEquals(2, decoded.locations.size)
        assertEquals(2, decoded.items.size)
        assertEquals("Rucksack", decoded.locations[0].name)
        assertEquals("Pferd", decoded.locations[1].name)
        assertEquals("Heiltrank", decoded.items[0].name)
        assertEquals("Zelt", decoded.items[1].name)
        assertEquals("Rucksack", decoded.items[0].locationName)
        assertEquals("Pferd", decoded.items[1].locationName)
    }
    
    @Test
    fun `CharacterDto includes lastModifiedDate`() {
        val timestamp = 1700000000000L // Fixed timestamp for testing
        val characterDto = CharacterDto(
            guid = "timestamp-guid",
            name = "TimeTest",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10,
            lastModifiedDate = timestamp
        )
        
        val jsonString = json.encodeToString(characterDto)
        println("Generated JSON: $jsonString")
        
        assertTrue("JSON should contain lastModifiedDate field", 
            jsonString.contains("\"lastModifiedDate\""))
        
        val decoded = json.decodeFromString<CharacterDto>(jsonString)
        // Timestamps should match exactly as we're using the same value
        assertEquals(timestamp, decoded.lastModifiedDate)
    }
    
    @Test
    fun `CharacterExportDto includes lastModifiedDate in round trip`() {
        val modifiedTime = 1700000000000L
        val exportTime = 1700100000000L
        
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "modified-guid-999",
                name = "ModifiedCharacter",
                mu = 10, kl = 10, inValue = 10, ch = 10,
                ff = 10, ge = 10, ko = 10, kk = 10,
                lastModifiedDate = modifiedTime
            ),
            spellSlots = listOf(),
            exportTimestamp = exportTime
        )
        
        val jsonString = json.encodeToString(exportDto)
        val decoded = json.decodeFromString<CharacterExportDto>(jsonString)
        
        assertEquals(modifiedTime, decoded.character.lastModifiedDate)
        assertEquals(exportTime, decoded.exportTimestamp)
    }
    
    @Test
    fun `toCharacter uses lastModifiedDate from DTO not current time`() {
        val exportTime = 1700000000000L // Fixed timestamp from export
        
        val characterDto = CharacterDto(
            guid = "test-guid",
            name = "Test",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10,
            lastModifiedDate = exportTime
        )
        
        val character = characterDto.toCharacter()
        
        assertEquals("lastModifiedDate should be from DTO, not current time", 
            exportTime, character.lastModifiedDate)
    }
    
    @Test
    fun `exportTimestamp should be preserved during import workflow`() {
        // Dies dokumentiert die Anforderung:
        // JSON um 17:00 Uhr exportiert -> Import um 17:10 Uhr -> lastModifiedDate = 17:00 Uhr
        val exportTime = 1700000000000L // 17:00 Uhr
        
        // Schritt 1: Export DTO erstellen
        val exportDto = CharacterExportDto(
            version = DataModelVersion.CURRENT_VERSION,
            character = CharacterDto(
                guid = "workflow-test",
                name = "WorkflowTest",
                mu = 10, kl = 10, inValue = 10, ch = 10,
                ff = 10, ge = 10, ko = 10, kk = 10,
                lastModifiedDate = exportTime
            ),
            spellSlots = listOf(),
            exportTimestamp = exportTime
        )
        
        // Schritt 2: Serialisierung
        val jsonString = json.encodeToString(exportDto)
        
        // Schritt 3: Deserialisierung
        val decoded = json.decodeFromString<CharacterExportDto>(jsonString)
        
        // Schritt 4: Zu Character konvertieren
        val character = decoded.character.toCharacter()
        
        // Verifizierung: exportTimestamp sollte erhalten bleiben
        assertEquals("exportTimestamp should be preserved", exportTime, decoded.exportTimestamp)
        assertEquals("lastModifiedDate should equal exportTimestamp", 
            decoded.exportTimestamp, character.lastModifiedDate)
    }
    
    @Test
    fun `ItemDto with unknown location name handles gracefully`() {
        // Test dass Items mit unbekannten Location-Namen korrekt gehandhabt werden
        val itemDto = ItemDto(
            locationName = "NonExistentLocation",
            name = "TestItem",
            weightStone = 1,
            weightOunces = 0,
            isPurse = false,
            kreuzerAmount = 0,
            isCountable = false,
            quantity = 1
        )
        
        // toItem mit null locationId sollte funktionieren
        val item = itemDto.toItem(characterId = 1, resolvedLocationId = null)
        
        assertEquals(1L, item.characterId)
        assertNull("locationId should be null when location not found", item.locationId)
        assertEquals("TestItem", item.name)
    }
}




