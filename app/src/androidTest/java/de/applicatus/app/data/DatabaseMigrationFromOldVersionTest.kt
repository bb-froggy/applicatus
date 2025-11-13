package de.applicatus.app.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Test für Datenbank-Migrationen von alten Versionen.
 * 
 * Dieser Test reproduziert den Bug, bei dem die App beim Start
 * mit einer alten Datenbankversion abstürzt.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationFromOldVersionTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ApplicatusDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test Migration von Version 1 bis zur aktuellen Version.
     * 
     * Dieser Test:
     * 1. Erstellt eine Datenbank mit Version 1
     * 2. Fügt Testdaten ein
     * 3. Migriert Schritt für Schritt bis zur aktuellen Version
     * 4. Validiert, dass die Daten erhalten bleiben
     */
    @Test
    @Throws(IOException::class)
    fun testMigrationFrom1ToLatest() {
        // Erstelle Datenbank mit Version 1
        helper.createDatabase(TEST_DB, 1).apply {
            // Füge einen Test-Character ein
            execSQL("""
                INSERT INTO characters (id, name, mu, kl, inValue, ch, ff, ge, ko, kk)
                VALUES (1, 'Test Hero', 12, 13, 14, 11, 10, 12, 13, 11)
            """)
            
            // Füge einen Test-Spell ein
            execSQL("""
                INSERT INTO spells (id, name, attribute1, attribute2, attribute3)
                VALUES (1, 'IGNIFAXIUS', 'KL', 'FF', 'KO')
            """)
            
            close()
        }

        // Migriere von Version 1 bis 22 (aktuelle Version)
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            22,
            true,
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
            ApplicatusDatabase.MIGRATION_19_20,
            ApplicatusDatabase.MIGRATION_20_21,
            ApplicatusDatabase.MIGRATION_21_22
        )

        // Prüfe, dass die Daten erhalten geblieben sind
        val cursor = db.query("SELECT * FROM characters WHERE id = 1")
        assert(cursor.moveToFirst()) { "Character sollte nach Migration existieren" }
        
        val nameIndex = cursor.getColumnIndex("name")
        val name = cursor.getString(nameIndex)
        assert(name == "Test Hero") { "Character-Name sollte nach Migration erhalten bleiben" }
        
        cursor.close()
    }

    /**
     * Test Migration von Version 7 bis zur aktuellen Version.
     * 
     * Testet Migration von Version 7 (vor Rezept-Feldern gruppe, lab, etc.)
     * zu Version 21. Dies reproduziert den recipes-Tabelle Bug.
     */
    @Test
    @Throws(IOException::class)
    fun testMigrationFrom7ToLatest() {
        // Erstelle Datenbank mit Version 7
        helper.createDatabase(TEST_DB, 7).apply {
            // Füge ein Test-Recipe ein (nur mit name)
            execSQL("""
                INSERT INTO recipes (id, name)
                VALUES (1, 'Test Heiltrank')
            """)
            
            close()
        }

        // Migriere von Version 7 bis 22 (aktuelle Version)
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            22,
            true,
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
            ApplicatusDatabase.MIGRATION_19_20,
            ApplicatusDatabase.MIGRATION_20_21,
            ApplicatusDatabase.MIGRATION_21_22
        )

        // Prüfe, dass die Daten erhalten geblieben sind
        val cursor = db.query("SELECT * FROM recipes WHERE id = 1")
        assert(cursor.moveToFirst()) { "Recipe sollte nach Migration existieren" }
        
        val nameIndex = cursor.getColumnIndex("name")
        val name = cursor.getString(nameIndex)
        assert(name == "Test Heiltrank") { "Recipe-Name sollte nach Migration erhalten bleiben" }
        
        cursor.close()
    }

    /**
     * Test Migration von Version 15 bis zur aktuellen Version.
     * 
     * Dies ist der kritische Test, der den Bug reproduziert.
     * Version 15 ist vor der Einführung des Gruppen-Systems.
     */
    @Test
    @Throws(IOException::class)
    fun testMigrationFrom15ToLatest() {
        // Erstelle Datenbank mit Version 15
        helper.createDatabase(TEST_DB, 15).apply {
            // Füge einen Test-Character mit allen Feldern bis Version 15 ein
            execSQL("""
                INSERT INTO characters (
                    id, name, mu, kl, inValue, ch, ff, ge, ko, kk,
                    hasApplicatus, applicatusZfw, applicatusModifier,
                    guid,
                    currentLe, maxLe, hasAe, currentAe, maxAe, hasKe, currentKe, maxKe,
                    leRegenBonus, aeRegenBonus, hasMasteryRegeneration,
                    alchemySkill, cookingPotionsSkill,
                    odemZfw, analysZfw,
                    hasAlchemy, hasCookingPotions, hasOdem, hasAnalys,
                    selfControlSkill, sensoryAcuitySkill, magicalLoreSkill, herbalLoreSkill,
                    isGameMaster,
                    'group',
                    alchemyIsMagicalMastery, cookingPotionsIsMagicalMastery,
                    defaultLaboratory
                )
                VALUES (
                    1, 'Test Hero', 12, 13, 14, 11, 10, 12, 13, 11,
                    1, 10, 0,
                    'test-guid-123',
                    30, 30, 1, 20, 20, 0, 0, 0,
                    0, 0, 0,
                    5, 3,
                    8, 7,
                    1, 1, 1, 1,
                    4, 6, 8, 5,
                    0,
                    'Meine Gruppe',
                    1, 0,
                    'LABORATORY'
                )
            """)
            
            close()
        }

        // Migriere von Version 15 bis 22 (aktuelle Version)
        // Dies sollte den Bug reproduzieren: fehlende Foreign Key Constraint
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            22,
            true,
            ApplicatusDatabase.MIGRATION_15_16,
            ApplicatusDatabase.MIGRATION_16_17,
            ApplicatusDatabase.MIGRATION_17_18,
            ApplicatusDatabase.MIGRATION_18_19,
            ApplicatusDatabase.MIGRATION_19_20,
            ApplicatusDatabase.MIGRATION_20_21,
            ApplicatusDatabase.MIGRATION_21_22
        )

        // Prüfe, dass die Daten erhalten geblieben sind
        val cursor = db.query("SELECT * FROM characters WHERE id = 1")
        assert(cursor.moveToFirst()) { "Character sollte nach Migration existieren" }
        
        val nameIndex = cursor.getColumnIndex("name")
        val name = cursor.getString(nameIndex)
        assert(name == "Test Hero") { "Character-Name sollte nach Migration erhalten bleiben" }
        
        // Prüfe, dass groupId gesetzt wurde
        val groupIdIndex = cursor.getColumnIndex("groupId")
        val groupId = cursor.getLong(groupIdIndex)
        assert(groupId > 0) { "GroupId sollte gesetzt sein" }
        
        cursor.close()
        
        // Prüfe, dass die Foreign Key Constraint existiert
        // Dies ist der kritische Test - wenn die FK fehlt, schlägt die Room-Validierung fehl
    }
}
