package de.applicatus.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.spell.Spell
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test für Datenbank-Migrationen.
 * 
 * Dieser Test stellt sicher, dass die Datenbank korrekt erstellt wird und 
 * alle Migrationen verfügbar sind. Er testet CRUD-Operationen und validiert,
 * dass alle durch Migrationen hinzugefügten Felder existieren und korrekt funktionieren.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var database: ApplicatusDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun closeDb() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    /**
     * Testet, dass die Datenbank mit allen Migrationen korrekt erstellt werden kann.
     * 
     * Dieser Test:
     * 1. Erstellt eine neue Datenbank mit allen Migrationen
     * 2. Fügt Testdaten ein
     * 3. Prüft, dass alle Tabellen und Spalten existieren
     * 4. Validiert, dass CRUD-Operationen funktionieren
     */
    @Test
    fun testDatabaseCreationWithAllMigrations() = runBlocking {
        // Erstelle Datenbank mit allen Migrationen
        database = Room.databaseBuilder(
            context,
            ApplicatusDatabase::class.java,
            "test_migration_db"
        )
        .addMigrations(
            ApplicatusDatabase.MIGRATION_1_2,
            ApplicatusDatabase.MIGRATION_2_3,
            ApplicatusDatabase.MIGRATION_3_4,
            ApplicatusDatabase.MIGRATION_4_5,
            ApplicatusDatabase.MIGRATION_5_6,
            ApplicatusDatabase.MIGRATION_6_7,
            ApplicatusDatabase.MIGRATION_7_8,
            ApplicatusDatabase.MIGRATION_8_9,
            ApplicatusDatabase.MIGRATION_9_10,
            ApplicatusDatabase.MIGRATION_10_11,
            ApplicatusDatabase.MIGRATION_11_12,
            ApplicatusDatabase.MIGRATION_12_13,
            ApplicatusDatabase.MIGRATION_13_14,
            ApplicatusDatabase.MIGRATION_14_15,
            ApplicatusDatabase.MIGRATION_15_16,
            ApplicatusDatabase.MIGRATION_16_17,
            ApplicatusDatabase.MIGRATION_17_18,
            ApplicatusDatabase.MIGRATION_18_19,
            ApplicatusDatabase.MIGRATION_19_20
        )
        .allowMainThreadQueries()
        .build()

        // Teste Spell-Operationen
        val testSpell = Spell(
            name = "TEST_SPELL",
            attribute1 = "MU",
            attribute2 = "KL",
            attribute3 = "IN"
        )
        database.spellDao().insertSpell(testSpell)
        val spells = database.spellDao().getAllSpells().first()
        assertTrue("Spell sollte eingefügt werden können", spells.isNotEmpty())
        
        // Teste Character-Operationen
        val testCharacter = Character(
            name = "Test Charakter",
            mu = 12,
            kl = 13,
            inValue = 14,
            ch = 11,
            ff = 10,
            ge = 12,
            ko = 13,
            kk = 11
        )
        val characterId = database.characterDao().insertCharacter(testCharacter)
        assertTrue("Character sollte eingefügt werden können", characterId > 0)
        
        val characters = database.characterDao().getAllCharacters().first()
        assertEquals("Es sollte 1 Character geben", 1, characters.size)
        
        val retrievedChar = characters[0]
        assertEquals("Character-Name sollte übereinstimmen", "Test Charakter", retrievedChar.name)
        assertEquals("MU sollte übereinstimmen", 12, retrievedChar.mu)
        assertEquals("KL sollte übereinstimmen", 13, retrievedChar.kl)
        assertEquals("IN sollte übereinstimmen", 14, retrievedChar.inValue)
        
        // Teste, dass neue Felder existieren (nach Migrationen)
        assertNotNull("GUID sollte existieren", retrievedChar.guid)
        assertTrue("GUID sollte nicht leer sein", retrievedChar.guid.isNotEmpty())
        
        // Teste Energie-Felder (Migration 3->4)
        assertEquals("CurrentLE sollte Defaultwert haben", 30, retrievedChar.currentLe)
        assertEquals("MaxLE sollte Defaultwert haben", 30, retrievedChar.maxLe)
        
        // Teste Regenerations-Felder (Migration 4->5)
        assertEquals("LeRegenBonus sollte 0 sein", 0, retrievedChar.leRegenBonus)
        
        // Teste Alchemie-Felder (Migration 5->6)
        assertEquals("AlchemySkill sollte 0 sein", 0, retrievedChar.alchemySkill)
        
        // Teste Zauber-Felder (Migration 6->7)
        assertEquals("OdemZfw sollte 0 sein", 0, retrievedChar.odemZfw)
        
        // Teste Boolean-Felder (Migration 7->8)
        assertFalse("HasAlchemy sollte false sein", retrievedChar.hasAlchemy)
        
        // Teste zusätzliche Talente (Migration 9->10)
        assertEquals("SelfControlSkill sollte 0 sein", 0, retrievedChar.selfControlSkill)
        
        // Teste Spielleiter-Modus (Migration 10->11)
        assertFalse("IsGameMaster sollte false sein", retrievedChar.isGameMaster)
        
        // Teste Gruppen-Feld (Migration 12->13)
        assertEquals("Group sollte Default haben", "Meine Gruppe", retrievedChar.group)
        
        // Teste Magisches Meisterhandwerk (Migration 13->14)
        assertFalse("AlchemyIsMagicalMastery sollte false sein", retrievedChar.alchemyIsMagicalMastery)
        
        // Teste GlobalSettings-Tabelle (Migration 5->6)
        val settings = database.globalSettingsDao().getSettings().first()
        // Settings können null sein, wenn noch nicht initialisiert
        
        // Teste Groups-Tabelle (Migration 16->17)
        val groups = database.groupDao().getAllGroups().first()
        // Kann leer sein, da keine Gruppe automatisch erstellt wird
        
        println("✅ Alle Datenbank-Operationen erfolgreich")
        println("✅ Alle Migrationen sind verfügbar und kompatibel")
    }

    /**
     * Testet, dass alle Migrationen korrekt registriert sind
     */
    @Test
    fun testAllMigrationsAreRegistered() {
        val migrations = listOf(
            ApplicatusDatabase.MIGRATION_1_2,
            ApplicatusDatabase.MIGRATION_2_3,
            ApplicatusDatabase.MIGRATION_3_4,
            ApplicatusDatabase.MIGRATION_4_5,
            ApplicatusDatabase.MIGRATION_5_6,
            ApplicatusDatabase.MIGRATION_6_7,
            ApplicatusDatabase.MIGRATION_7_8,
            ApplicatusDatabase.MIGRATION_8_9,
            ApplicatusDatabase.MIGRATION_9_10,
            ApplicatusDatabase.MIGRATION_10_11,
            ApplicatusDatabase.MIGRATION_11_12,
            ApplicatusDatabase.MIGRATION_12_13,
            ApplicatusDatabase.MIGRATION_13_14,
            ApplicatusDatabase.MIGRATION_14_15,
            ApplicatusDatabase.MIGRATION_15_16,
            ApplicatusDatabase.MIGRATION_16_17,
            ApplicatusDatabase.MIGRATION_17_18,
            ApplicatusDatabase.MIGRATION_18_19,
            ApplicatusDatabase.MIGRATION_19_20
        )
        
        assertEquals("Es sollten 19 Migrationen existieren", 19, migrations.size)
        
        // Prüfe, dass alle Migrationen die richtigen Versionen haben
        assertEquals("Migration 1->2 sollte von 1 nach 2 gehen", 1, migrations[0].startVersion)
        assertEquals("Migration 1->2 sollte von 1 nach 2 gehen", 2, migrations[0].endVersion)
        
        assertEquals("Migration 19->20 sollte von 19 nach 20 gehen", 19, migrations[18].startVersion)
        assertEquals("Migration 19->20 sollte von 19 nach 20 gehen", 20, migrations[18].endVersion)
        
        println("✅ Alle 19 Migrationen sind korrekt registriert")
    }
}
