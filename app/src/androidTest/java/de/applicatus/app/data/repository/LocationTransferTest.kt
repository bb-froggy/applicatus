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
        
        repository = ApplicatusRepository(database)
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
    
    /**
     * Test: Eigengewicht (SelfItem) darf beim Transfer nicht dupliziert werden.
     * 
     * Szenario:
     * 1. Charakter A hat eine Location "Tasche" mit Eigengewicht
     * 2. Location wird zu Charakter B übertragen
     * 3. Erwartung: B hat genau 1 Eigengewicht für die neue Location
     * 4. Bug: B hat 2 Eigengewichte (das übertragene + ein neu erstelltes)
     */
    @Test
    fun testTransferLocationDoesNotDuplicateSelfItem() = runBlocking {
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
        
        // Erstelle eine zusätzliche Location "Tasche" für A mit Eigengewicht
        val tascheId = repository.insertLocation(Location(
            id = 0,
            characterId = charAId,
            name = "Tasche",
            isDefault = false,
            isCarried = true,
            sortOrder = 2
        ))
        
        // Erstelle SelfItem (Eigengewicht) für die Tasche
        val tasche = repository.getLocationById(tascheId)!!
        repository.createSelfItemForLocation(tasche, Weight.fromOunces(8)) // 2 Unzen Eigengewicht
        
        // Erstelle ein normales Item in der Tasche
        repository.insertItem(Item(
            id = 0,
            characterId = charAId,
            locationId = tascheId,
            name = "Schlüssel",
            weight = Weight.fromOunces(1)
        ))
        
        // Verifiziere Ausgangszustand
        val itemsBeforeA = repository.getItemsWithLocationForCharacter(charAId).first()
        val selfItemsBeforeA = itemsBeforeA.filter { it.isSelfItem }
        val normalItemsBeforeA = itemsBeforeA.filter { !it.isSelfItem }
        
        println("=== Vor Transfer ===")
        println("A alle Items: ${itemsBeforeA.map { "${it.name} (isSelfItem=${it.isSelfItem})" }}")
        
        // A sollte mehrere SelfItems haben (für jede Location eines)
        assertTrue("A sollte SelfItems haben", selfItemsBeforeA.isNotEmpty())
        assertEquals("A sollte 1 normales Item haben (Schlüssel)", 1, normalItemsBeforeA.size)
        
        // Finde das SelfItem für die Tasche
        val taschenSelfItemBefore = itemsBeforeA.find { 
            it.isSelfItem && it.locationName == "Tasche" 
        }
        assertNotNull("Tasche sollte ein SelfItem haben", taschenSelfItemBefore)
        
        // Übertrage Tasche von A zu B
        repository.transferLocationToCharacter(tascheId, charBId)
        
        // Warte kurz, damit die Transaktion abgeschlossen ist
        kotlinx.coroutines.delay(100)
        
        // Verifiziere Endzustand
        val itemsAfterA = repository.getItemsWithLocationForCharacter(charAId).first()
        val itemsAfterB = repository.getItemsWithLocationForCharacter(charBId).first()
        val locationsAfterA = repository.getLocationsForCharacter(charAId).first()
        val locationsAfterB = repository.getLocationsForCharacter(charBId).first()
        
        println("=== Nach Transfer ===")
        println("A Items: ${itemsAfterA.map { "${it.name} (isSelfItem=${it.isSelfItem})" }}")
        println("B Items: ${itemsAfterB.map { "${it.name} (isSelfItem=${it.isSelfItem})" }}")
        println("B Locations: ${locationsAfterB.map { it.name }}")
        
        // A sollte die Tasche nicht mehr haben
        assertFalse(
            "A sollte keine 'Tasche' mehr haben",
            locationsAfterA.any { it.name == "Tasche" }
        )
        
        // A sollte kein Tasche-SelfItem mehr haben
        assertFalse(
            "A sollte kein Tasche-SelfItem mehr haben",
            itemsAfterA.any { it.isSelfItem && it.name.contains("Tasche") }
        )
        
        // B sollte die Tasche haben
        val tascheB = locationsAfterB.find { it.name == "Tasche" }
        assertNotNull("B sollte die 'Tasche' haben", tascheB)
        
        // B sollte genau 1 normales Item haben (Schlüssel)
        val normalItemsB = itemsAfterB.filter { !it.isSelfItem }
        assertEquals("B sollte 1 normales Item haben", 1, normalItemsB.size)
        assertEquals("Das Item sollte 'Schlüssel' heißen", "Schlüssel", normalItemsB[0].name)
        
        // KRITISCHER TEST: B sollte genau 1 SelfItem für die Tasche haben (nicht 2!)
        val tascheSelfItemsB = itemsAfterB.filter { 
            it.isSelfItem && it.locationName == "Tasche" 
        }
        
        println("=== Tasche SelfItems bei B ===")
        tascheSelfItemsB.forEach { 
            println("  - ${it.name} (id=${it.id}, locationId=${it.locationId})") 
        }
        
        assertEquals(
            "B sollte genau 1 SelfItem für die Tasche haben (nicht dupliziert!)",
            1,
            tascheSelfItemsB.size
        )
        
        // Das SelfItem sollte das korrekte Gewicht haben
        val selfItem = tascheSelfItemsB[0]
        assertEquals(
            "SelfItem sollte 8 Unzen (2 Unzen Eigengewicht) haben",
            Weight.fromOunces(8),
            Weight(selfItem.stone, selfItem.ounces)
        )
    }
    
    /**
     * Test: Nach dem Bearbeiten eines Eigenobjekts bleiben isSelfItem und selfItemForLocationId erhalten.
     * 
     * Bug-Reproduktion: Wenn das Item.copy() beim Bearbeiten nicht alle Felder kopiert,
     * verliert das Eigenobjekt seine speziellen Eigenschaften.
     */
    @Test
    fun testEditSelfItemPreservesProperties() = runBlocking {
        // Erstelle Gruppe
        val groupId = repository.insertGroup(Group(
            id = 0,
            name = "Test Gruppe",
            currentDerianDate = "1. Praios 1040 BF"
        ))
        
        // Erstelle Charakter
        val charId = repository.insertCharacter(Character(
            id = 0,
            name = "Test Charakter",
            groupId = groupId,
            mu = 10, kl = 10, inValue = 10, ch = 10,
            ff = 10, ge = 10, ko = 10, kk = 10
        ))
        
        // Erstelle Standard-Locations
        repository.createDefaultLocationsForCharacter(charId)
        
        // Erstelle eine zusätzliche Location "Tasche"
        val tascheId = repository.insertLocation(Location(
            id = 0,
            characterId = charId,
            name = "Tasche",
            isDefault = false,
            isCarried = true,
            sortOrder = 2
        ))
        
        // Hole das automatisch erstellte SelfItem
        val itemsBefore = repository.getItemsWithLocationForCharacter(charId).first()
        val selfItemBefore = itemsBefore.find { it.isSelfItem && it.selfItemForLocationId == tascheId }
        assertNotNull("SelfItem sollte existieren", selfItemBefore)
        assertTrue("Item sollte isSelfItem=true haben", selfItemBefore!!.isSelfItem)
        assertEquals("Item sollte selfItemForLocationId=tascheId haben", tascheId, selfItemBefore.selfItemForLocationId)
        
        // Simuliere das Bearbeiten des Items (wie in InventoryScreen)
        // WICHTIG: Hier müssen alle Felder kopiert werden!
        val editedItem = Item(
            id = selfItemBefore.id,
            guid = selfItemBefore.guid,
            characterId = selfItemBefore.characterId,
            locationId = selfItemBefore.locationId,
            name = "Tasche (Eigenobjekt) - bearbeitet", // Geänderter Name
            weight = Weight.fromOunces(16), // Geändertes Gewicht
            sortOrder = selfItemBefore.sortOrder,
            isPurse = selfItemBefore.isPurse,
            kreuzerAmount = selfItemBefore.kreuzerAmount,
            isCountable = selfItemBefore.isCountable,
            quantity = selfItemBefore.quantity,
            isSelfItem = selfItemBefore.isSelfItem, // MUSS erhalten bleiben!
            selfItemForLocationId = selfItemBefore.selfItemForLocationId // MUSS erhalten bleiben!
        )
        
        // Speichere das bearbeitete Item
        repository.updateItem(editedItem)
        
        // Prüfe, dass die Eigenschaften erhalten blieben
        val itemsAfter = repository.getItemsWithLocationForCharacter(charId).first()
        val selfItemAfter = itemsAfter.find { it.id == selfItemBefore.id }
        
        assertNotNull("Item sollte noch existieren", selfItemAfter)
        assertTrue("Item sollte weiterhin isSelfItem=true haben", selfItemAfter!!.isSelfItem)
        assertEquals("Item sollte weiterhin selfItemForLocationId haben", tascheId, selfItemAfter.selfItemForLocationId)
        assertEquals("Name sollte aktualisiert sein", "Tasche (Eigenobjekt) - bearbeitet", selfItemAfter.name)
        assertEquals("Gewicht sollte aktualisiert sein", Weight.fromOunces(16), Weight(selfItemAfter.stone, selfItemAfter.ounces))
    }
}
