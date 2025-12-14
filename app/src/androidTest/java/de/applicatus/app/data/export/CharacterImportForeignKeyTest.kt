package de.applicatus.app.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AndroidTest für Foreign Key-Probleme beim Import auf einem anderen Gerät.
 * 
 * Problem: Wenn ein Charakter mit Items exportiert wird, die auf Locations verweisen,
 * und dann auf einem anderen Gerät importiert wird, schlägt der Import mit
 * "FOREIGN KEY constraint failed" fehl.
 */
@RunWith(AndroidJUnit4::class)
class CharacterImportForeignKeyTest {
    
    private lateinit var sourceDatabase: ApplicatusDatabase
    private lateinit var sourceRepository: ApplicatusRepository
    private lateinit var sourceExportManager: CharacterExportManager
    
    private lateinit var targetDatabase: ApplicatusDatabase
    private lateinit var targetRepository: ApplicatusRepository
    private lateinit var targetExportManager: CharacterExportManager
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // "Quell-Gerät" mit Daten
        sourceDatabase = Room.inMemoryDatabaseBuilder(
            context,
            ApplicatusDatabase::class.java
        ).build()
        sourceRepository = ApplicatusRepository(sourceDatabase)
        sourceExportManager = CharacterExportManager(sourceRepository)
        
        // "Ziel-Gerät" - leere Datenbank (simuliert anderes Gerät)
        targetDatabase = Room.inMemoryDatabaseBuilder(
            context,
            ApplicatusDatabase::class.java
        ).build()
        targetRepository = ApplicatusRepository(targetDatabase)
        targetExportManager = CharacterExportManager(targetRepository)
    }
    
    @After
    fun tearDown() {
        sourceDatabase.close()
        targetDatabase.close()
    }
    
    @Test
    fun importCharacterWithItems_onEmptyDevice_shouldSucceed() = runBlocking {
        // Auf Quell-Gerät: Charakter mit Inventar erstellen
        val character = de.applicatus.app.data.model.character.Character(
            guid = "test-fk-char",
            name = "TestCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = sourceDatabase.characterDao().insertCharacter(character)
        
        // Standard-Locations erstellen
        sourceRepository.createDefaultLocationsForCharacter(characterId)
        val locations = sourceRepository.getLocationsForCharacter(characterId).first()
        val rucksackLocation = locations.find { it.name == "Rucksack" }
        assertNotNull("Rucksack should exist", rucksackLocation)
        
        // Item erstellen (mit korrekter Item-API)
        val item = Item(
            characterId = characterId,
            locationId = rucksackLocation!!.id,
            name = "TestSchwert",
            weight = Weight.fromOunces(40), // 1 Stein = 40 Unzen
            isPurse = false,
            kreuzerAmount = 0,
            isCountable = false,
            quantity = 1
        )
        sourceRepository.insertItem(item)
        
        // Export erstellen
        val jsonResult = sourceExportManager.exportCharacter(characterId)
        assertTrue("Export should succeed", jsonResult.isSuccess)
        val jsonString = jsonResult.getOrThrow()
        
        // Auf Ziel-Gerät importieren (leere Datenbank!)
        val importResult = targetExportManager.importCharacter(jsonString, targetCharacterId = null)
        
        // Dies sollte NICHT mit Foreign Key-Fehler fehlschlagen
        if (importResult.isFailure) {
            val exception = importResult.exceptionOrNull()
            fail("Import should succeed but failed with: ${exception?.message}")
        }
        
        assertTrue("Import should succeed on empty device", importResult.isSuccess)
        val (importedCharacterId, _) = importResult.getOrThrow()
        
        // Verifizieren: Charakter existiert
        val importedCharacter = targetRepository.getCharacterById(importedCharacterId)
        assertNotNull("Character should exist", importedCharacter)
        assertEquals("TestCharacter", importedCharacter!!.name)
        
        // Verifizieren: Locations wurden erstellt
        val importedLocations = targetRepository.getLocationsForCharacter(importedCharacterId).first()
        assertTrue("Should have locations", importedLocations.isNotEmpty())
        val importedRucksack = importedLocations.find { it.name == "Rucksack" }
        assertNotNull("Rucksack should exist", importedRucksack)
        
        // Verifizieren: Items wurden erstellt
        val importedItems = targetRepository.getItemsForCharacter(importedCharacterId).first()
        assertEquals("Should have 1 item", 1, importedItems.size)
        assertEquals("TestSchwert", importedItems[0].name)
    }
    
    @Test
    fun importCharacterWithSpells_onEmptyDevice_shouldSucceed() = runBlocking {
        // Auf Quell-Gerät: Zauber synchronisieren und Charakter mit SpellSlots erstellen
        sourceRepository.syncMissingSpells()
        
        val character = de.applicatus.app.data.model.character.Character(
            guid = "test-fk-spell-char",
            name = "SpellCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = sourceDatabase.characterDao().insertCharacter(character)
        
        // Standard-Locations erstellen (benötigt für Applicatus-Items)
        sourceRepository.createDefaultLocationsForCharacter(characterId)
        val locations = sourceRepository.getLocationsForCharacter(characterId).first()
        val rucksackLocation = locations.find { it.name == "Rucksack" }
        
        // Item für Applicatus erstellen
        val applicatusItem = Item(
            characterId = characterId,
            locationId = rucksackLocation!!.id,
            name = "Test-Applicatus",
            weight = Weight.fromOunces(10),
            isPurse = false,
            kreuzerAmount = 0,
            isCountable = false,
            quantity = 1
        )
        val applicatusItemId = sourceRepository.insertItem(applicatusItem)
        
        // Einen Zauber finden
        val spells = sourceDatabase.spellDao().getAllSpellsOnce()
        assertTrue("Should have spells", spells.isNotEmpty())
        val testSpell = spells.first()
        
        // SpellSlot erstellen
        val spellSlot = de.applicatus.app.data.model.spell.SpellSlot(
            characterId = characterId,
            slotNumber = 0,
            slotType = de.applicatus.app.data.model.spell.SlotType.APPLICATUS,
            spellId = testSpell.id,
            zfw = 10,
            isFilled = true,
            zfpStar = 5,
            itemId = applicatusItemId,
            creatorGuid = character.guid
        )
        sourceDatabase.spellSlotDao().insertSlot(spellSlot)
        
        // Export erstellen
        val jsonResult = sourceExportManager.exportCharacter(characterId)
        assertTrue("Export should succeed", jsonResult.isSuccess)
        val jsonString = jsonResult.getOrThrow()
        
        // Auf Ziel-Gerät: Zauber synchronisieren, dann importieren
        targetRepository.syncMissingSpells()
        
        val importResult = targetExportManager.importCharacter(jsonString, targetCharacterId = null)
        
        if (importResult.isFailure) {
            val exception = importResult.exceptionOrNull()
            fail("Import should succeed but failed with: ${exception?.message}")
        }
        
        assertTrue("Import should succeed on empty device", importResult.isSuccess)
        val (importedCharacterId, _) = importResult.getOrThrow()
        
        // Verifizieren: Charakter existiert
        val importedCharacter = targetRepository.getCharacterById(importedCharacterId)
        assertNotNull("Character should exist", importedCharacter)
        assertEquals("SpellCharacter", importedCharacter!!.name)
        
        // Verifizieren: SpellSlots wurden erstellt
        val importedSlots = targetDatabase.spellSlotDao().getSlotsByCharacter(importedCharacterId).first()
        assertEquals("Should have 1 spell slot", 1, importedSlots.size)
        assertEquals(10, importedSlots[0].zfw)
        assertTrue("Slot should be filled", importedSlots[0].isFilled)
    }
    
    @Test
    fun importCharacterWithPotions_onEmptyDevice_shouldSucceed() = runBlocking {
        // Auf Quell-Gerät: Rezepte synchronisieren und Charakter mit Potions erstellen
        sourceRepository.syncMissingRecipes()
        
        val character = de.applicatus.app.data.model.character.Character(
            guid = "test-fk-potion-char",
            name = "PotionCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = sourceDatabase.characterDao().insertCharacter(character)
        
        // Ein Rezept finden
        val recipes = sourceDatabase.recipeDao().getAllRecipes().first()
        assertTrue("Should have recipes", recipes.isNotEmpty())
        val testRecipe = recipes.first()
        
        // Potion erstellen (mit aktuellem Datenmodell)
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = characterId,
            recipeId = testRecipe.id,
            actualQuality = de.applicatus.app.data.model.potion.PotionQuality.C,
            createdDate = "1 Praios 1040 BF",
            expiryDate = "30 Praios 1040 BF"
        )
        sourceRepository.insertPotion(potion)
        
        // Export erstellen
        val jsonResult = sourceExportManager.exportCharacter(characterId)
        assertTrue("Export should succeed", jsonResult.isSuccess)
        val jsonString = jsonResult.getOrThrow()
        
        // Auf Ziel-Gerät: Rezepte synchronisieren, dann importieren
        targetRepository.syncMissingRecipes()
        
        val importResult = targetExportManager.importCharacter(jsonString, targetCharacterId = null)
        
        if (importResult.isFailure) {
            val exception = importResult.exceptionOrNull()
            fail("Import should succeed but failed with: ${exception?.message}")
        }
        
        assertTrue("Import should succeed on empty device", importResult.isSuccess)
        val (importedCharacterId, _) = importResult.getOrThrow()
        
        // Verifizieren: Charakter existiert
        val importedCharacter = targetRepository.getCharacterById(importedCharacterId)
        assertNotNull("Character should exist", importedCharacter)
        assertEquals("PotionCharacter", importedCharacter!!.name)
        
        // Verifizieren: Potions wurden erstellt
        val importedPotions = targetRepository.getPotionsForCharacter(importedCharacterId).first()
        assertEquals("Should have 1 potion", 1, importedPotions.size)
        assertEquals(de.applicatus.app.data.model.potion.PotionQuality.C, importedPotions[0].potion.actualQuality)
    }
}
