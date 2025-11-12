package de.applicatus.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.Weight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test für die Location-Übertragung zwischen Charakteren.
 * 
 * Reproduziert Bug: Beim Transfer eines Rucksacks von Charakter A zu B:
 * - A behält den leeren Rucksack
 * - B bekommt einen neuen Rucksack mit duplizierten Items
 */
@RunWith(AndroidJUnit4::class)
class LocationTransferTest {
    
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ApplicatusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        repository = ApplicatusRepository(
            spellDao = database.spellDao(),
            characterDao = database.characterDao(),
            spellSlotDao = database.spellSlotDao(),
            recipeDao = database.recipeDao(),
            potionDao = database.potionDao(),
            globalSettingsDao = database.globalSettingsDao(),
            recipeKnowledgeDao = database.recipeKnowledgeDao(),
            groupDao = database.groupDao(),
            itemDao = database.itemDao(),
            locationDao = database.locationDao()
        )
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun testTransferLocationBetweenCharacters() = runBlocking {
        // Erstelle Gruppe
        val groupId = repository.insertGroup(Group(
            id = 0,
            name = "Test Gruppe",
            currentDerianDate = "1. Praios 1040 BF"
        ))
        
        // Erstelle zwei Charaktere
        val charAId = repository.insertCharacter(Character(
            id = 0,
            name = "Charakter A",
            groupId = groupId,
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        ))
        
        val charBId = repository.insertCharacter(Character(
            id = 0,
            name = "Charakter B",
            groupId = groupId,
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        ))
        
        // Erstelle Standard-Locations für beide Charaktere
        repository.createDefaultLocationsForCharacter(charAId)
        repository.createDefaultLocationsForCharacter(charBId)
        
        // Hole die Standard-Locations (Am Körper, Rucksack) von Charakter A
        val locationsA = repository.getLocationsForCharacter(charAId).first()
        val rucksackA = locationsA.find { it.name == "Rucksack" }
        assertNotNull("Rucksack von A sollte existieren", rucksackA)
        
        // Erstelle ein Item im Rucksack von A
        val itemId = repository.insertItem(Item(
            id = 0,
            characterId = charAId,
            locationId = rucksackA!!.id,
            name = "Flasche",
            weight = Weight.fromOunces(4)
        ))
        
        // Verifiziere Ausgangszustand
        val itemsBeforeA = repository.getItemsWithLocationForCharacter(charAId).first()
        val itemsBeforeB = repository.getItemsWithLocationForCharacter(charBId).first()
        val locationsBeforeB = repository.getLocationsForCharacter(charBId).first()
        
        assertEquals("A sollte 1 Item haben", 1, itemsBeforeA.size)
        assertEquals("Item sollte 'Flasche' heißen", "Flasche", itemsBeforeA[0].name)
        assertEquals("Item sollte in 'Rucksack' sein", "Rucksack", itemsBeforeA[0].locationName)
        assertEquals("B sollte 0 Items haben", 0, itemsBeforeB.size)
        assertEquals("B sollte 2 Locations haben (Rüstung/Kleidung, Rucksack)", 2, locationsBeforeB.size)
        
        // Übertrage Rucksack von A zu B
        repository.transferLocationToCharacter(rucksackA.id, charBId)
        
        // Warte kurz, damit die Transaktion abgeschlossen ist
        kotlinx.coroutines.delay(100)
        
        // Verifiziere Endzustand
        val itemsAfterA = repository.getItemsWithLocationForCharacter(charAId).first()
        val itemsAfterB = repository.getItemsWithLocationForCharacter(charBId).first()
        val locationsAfterA = repository.getLocationsForCharacter(charAId).first()
        val locationsAfterB = repository.getLocationsForCharacter(charBId).first()
        
        // ERWARTETES VERHALTEN:
        // - A sollte den Rucksack nicht mehr haben (gelöscht)
        // - A sollte 0 Items haben
        // - B sollte 3 Locations haben (Rüstung/Kleidung, Rucksack (original), Rucksack (übertragen))
        // - B sollte 1 Item haben (die Flasche)
        
        // AKTUELLES (FEHLERHAFTES) VERHALTEN:
        // - A behält leeren Rucksack
        // - B bekommt duplizierte Items
        
        println("=== Nach Transfer ===")
        println("Locations A: ${locationsAfterA.map { it.name }}")
        println("Items A: ${itemsAfterA.map { "${it.name} in ${it.locationName}" }}")
        println("Locations B: ${locationsAfterB.map { it.name }}")
        println("Items B: ${itemsAfterB.map { "${it.name} in ${it.locationName}" }}")
        
        // A sollte den Rucksack nicht mehr haben
        assertFalse(
            "A sollte keinen Rucksack mehr haben",
            locationsAfterA.any { it.name == "Rucksack" }
        )
        
        // A sollte keine Items mehr haben
        assertEquals("A sollte 0 Items haben nach Transfer", 0, itemsAfterA.size)
        
        // B sollte genau 1 Flasche haben (nicht 2!)
        assertEquals("B sollte genau 1 Item haben", 1, itemsAfterB.size)
        assertEquals("Item sollte 'Flasche' heißen", "Flasche", itemsAfterB[0].name)
        
        // B sollte 3 Locations haben (2 Standard + 1 übertragen)
        assertEquals("B sollte 3 Locations haben", 3, locationsAfterB.size)
        assertTrue(
            "B sollte 'Rüstung/Kleidung' haben",
            locationsAfterB.any { it.name == "Rüstung/Kleidung" }
        )
        assertTrue(
            "B sollte ursprünglichen 'Rucksack' haben",
            locationsAfterB.any { it.name == "Rucksack" && it.isDefault }
        )
        assertTrue(
            "B sollte übertragenen 'Rucksack' haben",
            locationsAfterB.any { it.name == "Rucksack" && !it.isDefault }
        )
    }
}
