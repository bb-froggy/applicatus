package de.applicatus.app.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.model.character.Character
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
 * AndroidTest für SelfItem (Eigenobjekt) Duplizierungsprobleme beim Sync.
 * 
 * Problem: Beim Real-Time-Sync werden Eigenobjekte der Locations verdoppelt,
 * weil isSelfItem und selfItemForLocationId nicht im Export/Import enthalten sind.
 * 
 * Jedes Mal wenn ein Snapshot importiert wird:
 * 1. Alte Locations werden gelöscht
 * 2. Alte Items werden gelöscht  
 * 3. Neue Locations werden importiert (ohne SelfItems zu erstellen)
 * 4. Neue Items werden importiert (SelfItems werden als normale Items importiert)
 * 
 * Das Problem: Die SelfItem-Felder (isSelfItem, selfItemForLocationId) gehen verloren!
 */
@RunWith(AndroidJUnit4::class)
class SelfItemDuplicationTest {
    
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    private lateinit var exportManager: CharacterExportManager
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ApplicatusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ApplicatusRepository(database)
        exportManager = CharacterExportManager(repository)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    /**
     * Test: Nach Export und Import sollten SelfItems korrekt erhalten bleiben.
     * 
     * Erwartet: Das Eigenobjekt einer Location sollte nach Import weiterhin:
     * - isSelfItem = true haben
     * - selfItemForLocationId auf die korrekte (neue) Location-ID verweisen
     */
    @Test
    fun selfItemsShouldBePreservedAfterExportImport() = runBlocking {
        // Erstelle Charakter
        val character = Character(
            guid = "test-selfitem-char",
            name = "TestCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = database.characterDao().insertCharacter(character)
        
        // Erstelle Standard-Locations (inklusive Eigenobjekte)
        repository.createDefaultLocationsForCharacter(characterId)
        
        // Prüfe initiale Items
        val itemsBefore = repository.getItemsForCharacter(characterId).first()
        val selfItemsBefore = itemsBefore.filter { it.isSelfItem }
        val normalItemsBefore = itemsBefore.filter { !it.isSelfItem }
        
        assertTrue("Sollte Eigenobjekte haben", selfItemsBefore.isNotEmpty())
        assertEquals("Sollte keine normalen Items haben", 0, normalItemsBefore.size)
        
        // Verifiziere dass alle Eigenobjekte korrekt gesetzt sind
        selfItemsBefore.forEach { selfItem ->
            assertTrue("Item sollte isSelfItem=true haben", selfItem.isSelfItem)
            assertNotNull("Item sollte selfItemForLocationId haben", selfItem.selfItemForLocationId)
        }
        
        val selfItemCountBefore = selfItemsBefore.size
        
        // Export erstellen
        val jsonResult = exportManager.exportCharacter(characterId)
        assertTrue("Export sollte erfolgreich sein", jsonResult.isSuccess)
        val jsonString = jsonResult.getOrThrow()
        
        // Import durchführen (überschreibt den existierenden Charakter)
        val importResult = exportManager.importCharacter(jsonString, targetCharacterId = null)
        assertTrue("Import sollte erfolgreich sein", importResult.isSuccess)
        
        // Prüfe Items nach Import
        val itemsAfter = repository.getItemsForCharacter(characterId).first()
        val selfItemsAfter = itemsAfter.filter { it.isSelfItem }
        val normalItemsAfter = itemsAfter.filter { !it.isSelfItem }
        
        // KRITISCHER TEST: Anzahl der SelfItems sollte gleich bleiben
        assertEquals(
            "Anzahl der Eigenobjekte sollte gleich bleiben (war $selfItemCountBefore, ist jetzt ${selfItemsAfter.size})",
            selfItemCountBefore,
            selfItemsAfter.size
        )
        
        // Keine zusätzlichen "normalen" Items sollten entstehen
        assertEquals(
            "Keine normalen Items sollten entstehen (sind jetzt ${normalItemsAfter.size})",
            0,
            normalItemsAfter.size
        )
        
        // Alle Eigenobjekte sollten korrekte Felder haben
        selfItemsAfter.forEach { selfItem ->
            assertTrue("Item sollte isSelfItem=true haben: ${selfItem.name}", selfItem.isSelfItem)
            assertNotNull("Item sollte selfItemForLocationId haben: ${selfItem.name}", selfItem.selfItemForLocationId)
        }
    }
    
    /**
     * Test: Mehrfacher Sync sollte keine Duplizierung verursachen.
     * 
     * Simuliert einen Real-Time-Sync-Zyklus: Export -> Import -> Export -> Import
     */
    @Test
    fun multipleSyncCyclesShouldNotDuplicateSelfItems() = runBlocking {
        // Erstelle Charakter mit Locations und Items
        val character = Character(
            guid = "test-multisync-char",
            name = "MultiSyncCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = database.characterDao().insertCharacter(character)
        
        // Erstelle Standard-Locations
        repository.createDefaultLocationsForCharacter(characterId)
        
        // Füge ein normales Item hinzu
        val locations = repository.getLocationsForCharacter(characterId).first()
        val rucksack = locations.find { it.name == "Rucksack" }!!
        val normalItem = Item(
            characterId = characterId,
            locationId = rucksack.id,
            name = "TestSchwert",
            weight = Weight.fromOunces(40),
            isPurse = false,
            kreuzerAmount = 0,
            isCountable = false,
            quantity = 1
        )
        repository.insertItem(normalItem)
        
        // Zähle initiale Items
        val itemsBefore = repository.getItemsForCharacter(characterId).first()
        val selfItemsBefore = itemsBefore.filter { it.isSelfItem }
        val normalItemsBefore = itemsBefore.filter { !it.isSelfItem }
        
        val selfItemCountBefore = selfItemsBefore.size
        val normalItemCountBefore = normalItemsBefore.size
        
        // Simuliere 3 Sync-Zyklen
        repeat(3) { cycle ->
            // Export
            val jsonResult = exportManager.exportCharacter(characterId)
            assertTrue("Export sollte erfolgreich sein (Zyklus $cycle)", jsonResult.isSuccess)
            val jsonString = jsonResult.getOrThrow()
            
            // Import (simuliert applySnapshotFromSync)
            val snapshot = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString(CharacterExportDto.serializer(), jsonString)
            val result = repository.applySnapshotFromSync(snapshot, allowCreateNew = false)
            assertTrue("Import sollte erfolgreich sein (Zyklus $cycle)", result.isSuccess)
        }
        
        // Prüfe Items nach 3 Sync-Zyklen
        val itemsAfter = repository.getItemsForCharacter(characterId).first()
        val selfItemsAfter = itemsAfter.filter { it.isSelfItem }
        val normalItemsAfter = itemsAfter.filter { !it.isSelfItem }
        
        // KRITISCHER TEST: Keine Duplizierung
        assertEquals(
            "Anzahl Eigenobjekte sollte nach 3 Sync-Zyklen gleich sein " +
            "(war $selfItemCountBefore, ist jetzt ${selfItemsAfter.size})",
            selfItemCountBefore,
            selfItemsAfter.size
        )
        
        assertEquals(
            "Anzahl normale Items sollte nach 3 Sync-Zyklen gleich sein " +
            "(war $normalItemCountBefore, ist jetzt ${normalItemsAfter.size})",
            normalItemCountBefore,
            normalItemsAfter.size
        )
        
        // Gesamtzahl Items
        assertEquals(
            "Gesamtzahl Items sollte gleich sein",
            itemsBefore.size,
            itemsAfter.size
        )
    }
    
    /**
     * Test: SelfItems sollten beim Import zu ihrer Location zurück verlinkt werden.
     */
    @Test
    fun selfItemsShouldBeLinkedToCorrectLocation() = runBlocking {
        // Erstelle Charakter
        val character = Character(
            guid = "test-link-char",
            name = "LinkTestCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = database.characterDao().insertCharacter(character)
        
        // Erstelle Standard-Locations
        repository.createDefaultLocationsForCharacter(characterId)
        
        // Hole Locations VOR Export
        val locationsBefore = repository.getLocationsForCharacter(characterId).first()
        val locationNamesBefore = locationsBefore.map { it.name }.toSet()
        
        // Export
        val jsonResult = exportManager.exportCharacter(characterId)
        assertTrue("Export sollte erfolgreich sein", jsonResult.isSuccess)
        val jsonString = jsonResult.getOrThrow()
        
        // Import (löst alte Locations und Items, erstellt neue)
        val snapshot = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(CharacterExportDto.serializer(), jsonString)
        val result = repository.applySnapshotFromSync(snapshot, allowCreateNew = false)
        assertTrue("Import sollte erfolgreich sein", result.isSuccess)
        
        // Hole Locations NACH Import
        val locationsAfter = repository.getLocationsForCharacter(characterId).first()
        val locationNamesAfter = locationsAfter.map { it.name }.toSet()
        
        // Location-Namen sollten gleich sein
        assertEquals("Location-Namen sollten gleich sein", locationNamesBefore, locationNamesAfter)
        
        // Für jede Location: Prüfe ob Eigenobjekt korrekt verlinkt ist
        locationsAfter.forEach { location ->
            val items = repository.getItemsForCharacter(characterId).first()
            val selfItemForLocation = items.find { 
                it.isSelfItem && it.selfItemForLocationId == location.id 
            }
            
            assertNotNull(
                "Location '${location.name}' sollte ein Eigenobjekt haben",
                selfItemForLocation
            )
            
            assertEquals(
                "Eigenobjekt sollte in der richtigen Location sein",
                location.id,
                selfItemForLocation!!.locationId
            )
        }
    }
    
    /**
     * Test: Tränke sollten bei mehrfachem Sync nicht dupliziert werden.
     */
    @Test
    fun potionsShouldNotBeDuplicatedAfterMultipleSyncs() = runBlocking {
        // Rezepte synchronisieren
        repository.syncMissingRecipes()
        
        // Erstelle Charakter
        val character = Character(
            guid = "test-potion-sync-char",
            name = "PotionSyncCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = database.characterDao().insertCharacter(character)
        
        // Erstelle Standard-Locations
        repository.createDefaultLocationsForCharacter(characterId)
        
        // Ein Rezept finden
        val recipes = database.recipeDao().getAllRecipes().first()
        assertTrue("Sollte Rezepte haben", recipes.isNotEmpty())
        val testRecipe = recipes.first()
        
        // Potion erstellen
        val potion = de.applicatus.app.data.model.potion.Potion(
            characterId = characterId,
            recipeId = testRecipe.id,
            actualQuality = de.applicatus.app.data.model.potion.PotionQuality.C,
            createdDate = "1 Praios 1040 BF",
            expiryDate = "30 Praios 1040 BF"
        )
        repository.insertPotion(potion)
        
        // Zähle Tränke vor Sync
        val potionsBefore = repository.getPotionsForCharacter(characterId).first()
        val potionCountBefore = potionsBefore.size
        assertEquals("Sollte 1 Trank haben", 1, potionCountBefore)
        
        // Simuliere 3 Sync-Zyklen
        repeat(3) { cycle ->
            // Export
            val jsonResult = exportManager.exportCharacter(characterId)
            assertTrue("Export sollte erfolgreich sein (Zyklus $cycle)", jsonResult.isSuccess)
            val jsonString = jsonResult.getOrThrow()
            
            // Import (simuliert applySnapshotFromSync)
            val snapshot = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString(CharacterExportDto.serializer(), jsonString)
            val result = repository.applySnapshotFromSync(snapshot, allowCreateNew = false)
            assertTrue("Import sollte erfolgreich sein (Zyklus $cycle)", result.isSuccess)
        }
        
        // Prüfe Tränke nach 3 Sync-Zyklen
        val potionsAfter = repository.getPotionsForCharacter(characterId).first()
        
        // KRITISCHER TEST: Keine Duplizierung
        assertEquals(
            "Anzahl Tränke sollte nach 3 Sync-Zyklen gleich sein " +
            "(war $potionCountBefore, ist jetzt ${potionsAfter.size})",
            potionCountBefore,
            potionsAfter.size
        )
    }
    
    /**
     * Test: Gelöschte Tränke sollten beim Sync entfernt werden.
     */
    @Test
    fun deletedPotionsShouldBeRemovedAfterSync() = runBlocking {
        // Rezepte synchronisieren
        repository.syncMissingRecipes()
        
        // Erstelle Charakter
        val character = Character(
            guid = "test-potion-delete-char",
            name = "PotionDeleteCharacter",
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        )
        val characterId = database.characterDao().insertCharacter(character)
        
        // Erstelle Standard-Locations
        repository.createDefaultLocationsForCharacter(characterId)
        
        // Ein Rezept finden
        val recipes = database.recipeDao().getAllRecipes().first()
        val testRecipe = recipes.first()
        
        // 2 Potions erstellen
        val potion1 = de.applicatus.app.data.model.potion.Potion(
            guid = "potion-1",
            characterId = characterId,
            recipeId = testRecipe.id,
            actualQuality = de.applicatus.app.data.model.potion.PotionQuality.C,
            createdDate = "1 Praios 1040 BF",
            expiryDate = "30 Praios 1040 BF"
        )
        val potion2 = de.applicatus.app.data.model.potion.Potion(
            guid = "potion-2",
            characterId = characterId,
            recipeId = testRecipe.id,
            actualQuality = de.applicatus.app.data.model.potion.PotionQuality.D,
            createdDate = "1 Praios 1040 BF",
            expiryDate = "30 Praios 1040 BF"
        )
        repository.insertPotion(potion1)
        repository.insertPotion(potion2)
        
        // Verifiziere dass 2 Tränke existieren
        val potionsBefore = repository.getPotionsForCharacter(characterId).first()
        assertEquals("Sollte 2 Tränke haben", 2, potionsBefore.size)
        
        // Exportiere nur mit 1 Trank (simuliert Löschung von potion2)
        val exportResult = exportManager.exportCharacter(characterId)
        val snapshot = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(CharacterExportDto.serializer(), exportResult.getOrThrow())
        
        // Modifiziere Snapshot: Entferne potion2
        val modifiedSnapshot = snapshot.copy(
            potions = snapshot.potions.filter { it.guid != "potion-2" }
        )
        
        // Importiere modifizierten Snapshot
        val result = repository.applySnapshotFromSync(modifiedSnapshot, allowCreateNew = false)
        assertTrue("Import sollte erfolgreich sein", result.isSuccess)
        
        // Prüfe Tränke nach Import
        val potionsAfter = repository.getPotionsForCharacter(characterId).first()
        
        // KRITISCHER TEST: Trank 2 sollte gelöscht worden sein
        assertEquals("Sollte nur 1 Trank haben", 1, potionsAfter.size)
        assertEquals("Sollte potion-1 sein", "potion-1", potionsAfter.first().potion.guid)
    }
}
