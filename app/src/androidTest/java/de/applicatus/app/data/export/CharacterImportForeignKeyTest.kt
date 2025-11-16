package de.applicatus.app.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.repository.ApplicatusRepository
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
        
        // Item erstellen
        val item = de.applicatus.app.data.model.item.Item(
            characterId = characterId,
            locationId = rucksackLocation!!.id,
            name = "TestSchwert",
            weightStone = 1,
            weightUnzen = 0,
            isPurse = false,
            kreuzerAmount = 0,
            isCountable = false,
            quantity = 1
        )
        sourceDatabase.itemDao().insertItem(item)
        
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
        // Auf Quell-Gerät: Spell-Datenbank initialisieren
        sourceRepository.initializeSpellsFromAssets()
        val spells = sourceRepository.allSpells.first()
        assertTrue("Should have spells in source database", spells.isNotEmpty())
        
        // Charakter mit Spell-Slot erstellen
        val character = de.applicatus.app.data.model.character.Character(
            guid = "test-fk-spell-char",
            name = "SpellCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = sourceDatabase.characterDao().insertCharacter(character)
        
        // Spell-Slot erstellen
        val testSpell = spells.first()
        val spellSlot = de.applicatus.app.data.model.spell.SpellSlot(
            characterId = characterId,
            spellId = testSpell.id,
            slotType = de.applicatus.app.data.model.spell.SlotType.SPELL,
            currentZfp = 0,
            zfpBonus = 0
        )
        sourceDatabase.spellSlotDao().insertSlot(spellSlot)
        
        // Export erstellen
        val jsonResult = sourceExportManager.exportCharacter(characterId)
        assertTrue("Export should succeed", jsonResult.isSuccess)
        val jsonString = jsonResult.getOrThrow()
        
        // Auf Ziel-Gerät: Spell-Datenbank NICHT initialisieren (simuliert fehlendes Asset)
        // oder nur teilweise initialisiert
        
        // Import sollte trotzdem funktionieren (Spell-Slot wird übersprungen, wenn Spell nicht gefunden)
        val importResult = targetExportManager.importCharacter(jsonString, targetCharacterId = null)
        
        // Import sollte nicht komplett fehlschlagen
        assertTrue("Import should not fail completely even if spell is missing", importResult.isSuccess)
        val (importedCharacterId, warning) = importResult.getOrThrow()
        
        // Warnung sollte vorhanden sein
        assertNotNull("Should have warning about missing spell", warning)
        
        // Charakter sollte trotzdem existieren
        val importedCharacter = targetRepository.getCharacterById(importedCharacterId)
        assertNotNull("Character should exist", importedCharacter)
        assertEquals("SpellCharacter", importedCharacter!!.name)
    }
    
    @Test
    fun importCharacterWithPotions_onEmptyDevice_shouldSucceed() = runBlocking {
        // Auf Quell-Gerät: Recipe-Datenbank initialisieren
        sourceRepository.initializeRecipesFromAssets()
        val recipes = sourceRepository.allRecipes.first()
        assertTrue("Should have recipes in source database", recipes.isNotEmpty())
        
        // Charakter mit Potion erstellen
        val character = de.applicatus.app.data.model.character.Character(
            guid = "test-fk-potion-char",
            name = "PotionCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = sourceDatabase.characterDao().insertCharacter(character)
        
        // Potion erstellen
        val testRecipe = recipes.first()
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = characterId,
            recipeId = testRecipe.id,
            guid = "test-potion-guid",
            quality = de.applicatus.app.data.model.potion.PotionQuality.NORMAL,
            currentDurability = 10,
            maxDurability = 10
        )
        sourceDatabase.potionDao().insertPotion(potion)
        
        // Export erstellen
        val jsonResult = sourceExportManager.exportCharacter(characterId)
        assertTrue("Export should succeed", jsonResult.isSuccess)
        val jsonString = jsonResult.getOrThrow()
        
        // Auf Ziel-Gerät: Recipe-Datenbank NICHT initialisieren
        
        // Import sollte mit Warnung funktionieren
        val importResult = targetExportManager.importCharacter(jsonString, targetCharacterId = null)
        
        assertTrue("Import should not fail completely even if recipe is missing", importResult.isSuccess)
        val (importedCharacterId, warning) = importResult.getOrThrow()
        
        // Warnung sollte vorhanden sein
        assertNotNull("Should have warning about missing recipe", warning)
        
        // Charakter sollte trotzdem existieren
        val importedCharacter = targetRepository.getCharacterById(importedCharacterId)
        assertNotNull("Character should exist", importedCharacter)
        assertEquals("PotionCharacter", importedCharacter!!.name)
    }
}
