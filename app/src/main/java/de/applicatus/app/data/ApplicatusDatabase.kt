package de.applicatus.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.applicatus.app.data.dao.CharacterDao
import de.applicatus.app.data.dao.GlobalSettingsDao
import de.applicatus.app.data.dao.GroupDao
import de.applicatus.app.data.dao.ItemDao
import de.applicatus.app.data.dao.LocationDao
import de.applicatus.app.data.dao.PotionDao
import de.applicatus.app.data.dao.RecipeDao
import de.applicatus.app.data.dao.RecipeKnowledgeDao
import de.applicatus.app.data.dao.SpellDao
import de.applicatus.app.data.dao.SpellSlotDao
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.GlobalSettings
import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.RecipeKnowledge
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SpellSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Spell::class, Character::class, SpellSlot::class, Recipe::class, Potion::class, GlobalSettings::class, RecipeKnowledge::class, Group::class, Item::class, Location::class],
    version = 26,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ApplicatusDatabase : RoomDatabase() {
    abstract fun spellDao(): SpellDao
    abstract fun characterDao(): CharacterDao
    abstract fun spellSlotDao(): SpellSlotDao
    abstract fun recipeDao(): RecipeDao
    abstract fun potionDao(): PotionDao
    abstract fun globalSettingsDao(): GlobalSettingsDao
    abstract fun recipeKnowledgeDao(): RecipeKnowledgeDao
    abstract fun groupDao(): GroupDao
    abstract fun itemDao(): ItemDao
    abstract fun locationDao(): LocationDao
    
    companion object {
        @Volatile
        private var INSTANCE: ApplicatusDatabase? = null
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Character-Tabelle erweitern
                database.execSQL("ALTER TABLE characters ADD COLUMN hasApplicatus INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN applicatusZfw INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN applicatusModifier INTEGER NOT NULL DEFAULT 0")
                
                // SpellSlot-Tabelle erweitern
                database.execSQL("ALTER TABLE spell_slots ADD COLUMN slotType TEXT NOT NULL DEFAULT 'APPLICATUS'")
                database.execSQL("ALTER TABLE spell_slots ADD COLUMN volumePoints INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE spell_slots ADD COLUMN applicatusRollResult TEXT")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // GUID-Spalte zu Character-Tabelle hinzufügen
                // Generiere UUIDs für bestehende Charaktere
                database.execSQL("ALTER TABLE characters ADD COLUMN guid TEXT NOT NULL DEFAULT ''")
                
                // Update bestehende Charaktere mit neuen GUIDs
                // Leider kann SQLite keine UUID-Funktion direkt aufrufen, daher werden sie in Code generiert
                val cursor = database.query("SELECT id FROM characters")
                val updates = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val guid = java.util.UUID.randomUUID().toString()
                    updates.add("UPDATE characters SET guid = '$guid' WHERE id = $id")
                }
                cursor.close()
                updates.forEach { database.execSQL(it) }
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Energie-Spalten zu Character-Tabelle hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN currentLe INTEGER NOT NULL DEFAULT 30")
                database.execSQL("ALTER TABLE characters ADD COLUMN maxLe INTEGER NOT NULL DEFAULT 30")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasAe INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN currentAe INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN maxAe INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasKe INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN currentKe INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN maxKe INTEGER NOT NULL DEFAULT 0")
                
                // Recipe-Tabelle erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Potion-Tabelle erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS potions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterId INTEGER NOT NULL,
                        recipeId INTEGER NOT NULL,
                        quality TEXT NOT NULL,
                        expiryDate TEXT NOT NULL,
                        FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE,
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Indices für Potions erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_potions_recipeId ON potions(recipeId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_potions_characterId ON potions(characterId)")
            }
        }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Regenerations-Felder zu Character-Tabelle hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN leRegenBonus INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN aeRegenBonus INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasMasteryRegeneration INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recipe-Tabelle erweitern
                database.execSQL("ALTER TABLE recipes ADD COLUMN brewingDifficulty INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE recipes ADD COLUMN analysisDifficulty INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE recipes ADD COLUMN appearance TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE recipes ADD COLUMN shelfLife TEXT NOT NULL DEFAULT '1 Mond'")
                
                // Potion-Tabelle erweitern
                database.execSQL("ALTER TABLE potions ADD COLUMN appearance TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE potions ADD COLUMN analysisStatus TEXT NOT NULL DEFAULT 'NOT_ANALYZED'")
                
                // Character-Tabelle erweitern (Alchemie-Talente)
                database.execSQL("ALTER TABLE characters ADD COLUMN alchemySkill INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN cookingPotionsSkill INTEGER NOT NULL DEFAULT 0")
                
                // GlobalSettings-Tabelle erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS global_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        currentDerianDate TEXT NOT NULL DEFAULT '1 Praios 1040 BF'
                    )
                """.trimIndent())
                
                // Standard-Settings einfügen
                database.execSQL("INSERT INTO global_settings (id, currentDerianDate) VALUES (1, '1 Praios 1040 BF')")
            }
        }
        
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Character-Tabelle erweitern (Zauber für Alchemie)
                database.execSQL("ALTER TABLE characters ADD COLUMN odemZfw INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN analysZfw INTEGER NOT NULL DEFAULT 0")
                
                // RecipeKnowledge-Tabelle erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_knowledge (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterId INTEGER NOT NULL,
                        recipeId INTEGER NOT NULL,
                        knowledgeLevel TEXT NOT NULL DEFAULT 'UNKNOWN',
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE,
                        FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Indices für RecipeKnowledge erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_knowledge_characterId ON recipe_knowledge(characterId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_knowledge_recipeId ON recipe_knowledge(recipeId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recipe_knowledge_characterId_recipeId ON recipe_knowledge(characterId, recipeId)")
            }
        }
        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Boolean-Felder hinzufügen, um zu tracken ob Talente/Zauber überhaupt beherrscht werden
                database.execSQL("ALTER TABLE characters ADD COLUMN hasAlchemy INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasCookingPotions INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasOdem INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasAnalys INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recipe-Tabelle erweitern mit Feldern aus Rezepte.csv
                database.execSQL("ALTER TABLE recipes ADD COLUMN gruppe TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE recipes ADD COLUMN lab TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE recipes ADD COLUMN preis INTEGER")
                database.execSQL("ALTER TABLE recipes ADD COLUMN zutatenPreis INTEGER")
                database.execSQL("ALTER TABLE recipes ADD COLUMN zutatenVerbreitung INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE recipes ADD COLUMN verbreitung INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // WICHTIG: Da es noch keine Anwender mit Elixieren gibt, erstellen wir die Potion-Tabelle neu
                // Alte Potion-Tabelle löschen
                database.execSQL("DROP TABLE IF EXISTS potions")
                
                // Neue Potion-Tabelle mit erweiterten Analyse-Feldern erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS potions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterId INTEGER NOT NULL,
                        recipeId INTEGER NOT NULL,
                        actualQuality TEXT NOT NULL,
                        appearance TEXT NOT NULL DEFAULT '',
                        expiryDate TEXT NOT NULL,
                        categoryKnown INTEGER NOT NULL DEFAULT 0,
                        knownQualityLevel TEXT NOT NULL DEFAULT 'UNKNOWN',
                        intensityQuality TEXT NOT NULL DEFAULT 'UNKNOWN',
                        refinedQuality TEXT NOT NULL DEFAULT 'UNKNOWN',
                        knownExactQuality TEXT,
                        shelfLifeKnown INTEGER NOT NULL DEFAULT 0,
                        intensityDeterminationZfp INTEGER NOT NULL DEFAULT 0,
                        bestStructureAnalysisFacilitation INTEGER NOT NULL DEFAULT 0,
                        accumulatedStructureAnalysisTap INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE,
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Indices für Potions erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_potions_recipeId ON potions(recipeId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_potions_characterId ON potions(characterId)")
                
                // Character-Tabelle erweitern mit zusätzlichen Talenten für Alchemie
                database.execSQL("ALTER TABLE characters ADD COLUMN selfControlSkill INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN sensoryAcuitySkill INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN magicalLoreSkill INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN herbalLoreSkill INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Character-Tabelle erweitern mit Spieler/Spielleiter-Flag
                database.execSQL("ALTER TABLE characters ADD COLUMN isGameMaster INTEGER NOT NULL DEFAULT 0")
                
                // SpellSlot-Tabelle erweitern mit Patzer-Flag
                database.execSQL("ALTER TABLE spell_slots ADD COLUMN isBotched INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Entferne nur accumulatedStructureAnalysisTap aus Potion-Tabelle
                // bestStructureAnalysisFacilitation bleibt erhalten!
                // Da SQLite keine ALTER TABLE DROP COLUMN unterstützt, müssen wir die Tabelle neu erstellen
                
                // 1. Temporäre Tabelle mit neuer Struktur erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS potions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterId INTEGER NOT NULL,
                        recipeId INTEGER NOT NULL,
                        actualQuality TEXT NOT NULL,
                        appearance TEXT NOT NULL DEFAULT '',
                        expiryDate TEXT NOT NULL,
                        categoryKnown INTEGER NOT NULL DEFAULT 0,
                        knownQualityLevel TEXT NOT NULL DEFAULT 'UNKNOWN',
                        intensityQuality TEXT NOT NULL DEFAULT 'UNKNOWN',
                        refinedQuality TEXT NOT NULL DEFAULT 'UNKNOWN',
                        knownExactQuality TEXT,
                        shelfLifeKnown INTEGER NOT NULL DEFAULT 0,
                        intensityDeterminationZfp INTEGER NOT NULL DEFAULT 0,
                        bestStructureAnalysisFacilitation INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE,
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // 2. Daten von alter Tabelle in neue Tabelle kopieren (ohne accumulatedStructureAnalysisTap)
                database.execSQL("""
                    INSERT INTO potions_new (
                        id, characterId, recipeId, actualQuality, appearance, expiryDate,
                        categoryKnown, knownQualityLevel, intensityQuality, refinedQuality,
                        knownExactQuality, shelfLifeKnown, intensityDeterminationZfp,
                        bestStructureAnalysisFacilitation
                    )
                    SELECT 
                        id, characterId, recipeId, actualQuality, appearance, expiryDate,
                        categoryKnown, knownQualityLevel, intensityQuality, refinedQuality,
                        knownExactQuality, shelfLifeKnown, intensityDeterminationZfp,
                        bestStructureAnalysisFacilitation
                    FROM potions
                """.trimIndent())
                
                // 3. Alte Tabelle löschen
                database.execSQL("DROP TABLE potions")
                
                // 4. Neue Tabelle umbenennen
                database.execSQL("ALTER TABLE potions_new RENAME TO potions")
                
                // 5. Indices neu erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_potions_recipeId ON potions(recipeId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_potions_characterId ON potions(characterId)")
            }
        }
        
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Gruppen-Feld zu Character-Tabelle hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN 'group' TEXT NOT NULL DEFAULT 'Meine Gruppe'")
                
                // GUID-Feld zu Potion-Tabelle hinzufügen
                database.execSQL("ALTER TABLE potions ADD COLUMN guid TEXT NOT NULL DEFAULT ''")
                
                // Generiere UUIDs für bestehende Tränke
                val cursor = database.query("SELECT id FROM potions")
                val updates = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val guid = java.util.UUID.randomUUID().toString()
                    updates.add("UPDATE potions SET guid = '$guid' WHERE id = $id")
                }
                cursor.close()
                updates.forEach { database.execSQL(it) }
            }
        }
        
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Magisches Meisterhandwerk-Felder zu Character-Tabelle hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN alchemyIsMagicalMastery INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN cookingPotionsIsMagicalMastery INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Default-Labor-Feld zu Character-Tabelle hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN defaultLaboratory TEXT")
            }
        }
        
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // nameKnown-Feld zu Potion-Tabelle hinzufügen
                database.execSQL("ALTER TABLE potions ADD COLUMN nameKnown INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Groups-Tabelle erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        currentDerianDate TEXT NOT NULL DEFAULT '1 Praios 1040 BF'
                    )
                """.trimIndent())
                
                // 2. Standard-Gruppe erstellen und deren ID ermitteln
                database.execSQL("INSERT INTO groups (name, currentDerianDate) VALUES ('Meine Gruppe', '1 Praios 1040 BF')")
                val cursor = database.query("SELECT last_insert_rowid()")
                var defaultGroupId: Long = 1
                if (cursor.moveToFirst()) {
                    defaultGroupId = cursor.getLong(0)
                }
                cursor.close()
                
                // 3. Für jede eindeutige Gruppe aus characters eine neue Group erstellen
                val groupCursor = database.query("SELECT DISTINCT 'group' FROM characters WHERE 'group' != 'Meine Gruppe'")
                val groupMapping = mutableMapOf<String, Long>()
                groupMapping["Meine Gruppe"] = defaultGroupId
                
                while (groupCursor.moveToNext()) {
                    val groupName = groupCursor.getString(0)
                    if (groupName.isNotEmpty()) {
                        database.execSQL("INSERT INTO groups (name, currentDerianDate) VALUES (?, '1 Praios 1040 BF')", arrayOf(groupName))
                        val idCursor = database.query("SELECT last_insert_rowid()")
                        if (idCursor.moveToFirst()) {
                            groupMapping[groupName] = idCursor.getLong(0)
                        }
                        idCursor.close()
                    }
                }
                groupCursor.close()
                
                // 4. Hole aktuelles derisches Datum aus global_settings
                val settingsCursor = database.query("SELECT currentDerianDate FROM global_settings WHERE id = 1")
                var currentDate = "1 Praios 1040 BF"
                if (settingsCursor.moveToFirst()) {
                    currentDate = settingsCursor.getString(0)
                }
                settingsCursor.close()
                
                // 5. Setze das globale Datum auf die Standard-Gruppe
                database.execSQL("UPDATE groups SET currentDerianDate = ? WHERE id = ?", arrayOf(currentDate, defaultGroupId))
                
                // 6. Temporäre groupId-Spalte zu characters hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN groupId INTEGER")
                
                // 7. groupId für bestehende Charaktere setzen
                groupMapping.forEach { (groupName, groupId) ->
                    database.execSQL("UPDATE characters SET groupId = ? WHERE 'group' = ?", arrayOf(groupId, groupName))
                }
                
                // 8. Charaktere ohne groupId auf Standard-Gruppe setzen
                database.execSQL("UPDATE characters SET groupId = ? WHERE groupId IS NULL", arrayOf(defaultGroupId))
                
                // 9. Tabelle neu erstellen mit Foreign Key (SQLite unterstützt kein ALTER TABLE für FK)
                // 9.1. Neue Tabelle mit Foreign Key erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS characters_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        guid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        mu INTEGER NOT NULL,
                        kl INTEGER NOT NULL,
                        inValue INTEGER NOT NULL,
                        ch INTEGER NOT NULL,
                        ff INTEGER NOT NULL,
                        ge INTEGER NOT NULL,
                        ko INTEGER NOT NULL,
                        kk INTEGER NOT NULL,
                        hasApplicatus INTEGER NOT NULL,
                        applicatusZfw INTEGER NOT NULL,
                        applicatusModifier INTEGER NOT NULL,
                        currentLe INTEGER NOT NULL,
                        maxLe INTEGER NOT NULL,
                        hasAe INTEGER NOT NULL,
                        currentAe INTEGER NOT NULL,
                        maxAe INTEGER NOT NULL,
                        hasKe INTEGER NOT NULL,
                        currentKe INTEGER NOT NULL,
                        maxKe INTEGER NOT NULL,
                        leRegenBonus INTEGER NOT NULL,
                        aeRegenBonus INTEGER NOT NULL,
                        hasMasteryRegeneration INTEGER NOT NULL,
                        alchemySkill INTEGER NOT NULL,
                        cookingPotionsSkill INTEGER NOT NULL,
                        odemZfw INTEGER NOT NULL,
                        analysZfw INTEGER NOT NULL,
                        hasAlchemy INTEGER NOT NULL,
                        hasCookingPotions INTEGER NOT NULL,
                        hasOdem INTEGER NOT NULL,
                        hasAnalys INTEGER NOT NULL,
                        selfControlSkill INTEGER NOT NULL,
                        sensoryAcuitySkill INTEGER NOT NULL,
                        magicalLoreSkill INTEGER NOT NULL,
                        herbalLoreSkill INTEGER NOT NULL,
                        isGameMaster INTEGER NOT NULL,
                        'group' TEXT NOT NULL,
                        alchemyIsMagicalMastery INTEGER NOT NULL,
                        cookingPotionsIsMagicalMastery INTEGER NOT NULL,
                        defaultLaboratory TEXT,
                        groupId INTEGER,
                        FOREIGN KEY(groupId) REFERENCES groups(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // 9.2. Daten von alter Tabelle kopieren
                database.execSQL("""
                    INSERT INTO characters_new (
                        id, guid, name, mu, kl, inValue, ch, ff, ge, ko, kk,
                        hasApplicatus, applicatusZfw, applicatusModifier,
                        currentLe, maxLe, hasAe, currentAe, maxAe, hasKe, currentKe, maxKe,
                        leRegenBonus, aeRegenBonus, hasMasteryRegeneration,
                        alchemySkill, cookingPotionsSkill,
                        odemZfw, analysZfw,
                        hasAlchemy, hasCookingPotions, hasOdem, hasAnalys,
                        selfControlSkill, sensoryAcuitySkill, magicalLoreSkill, herbalLoreSkill,
                        isGameMaster,
                        'group',
                        alchemyIsMagicalMastery, cookingPotionsIsMagicalMastery,
                        defaultLaboratory,
                        groupId
                    )
                    SELECT 
                        id, guid, name, mu, kl, inValue, ch, ff, ge, ko, kk,
                        hasApplicatus, applicatusZfw, applicatusModifier,
                        currentLe, maxLe, hasAe, currentAe, maxAe, hasKe, currentKe, maxKe,
                        leRegenBonus, aeRegenBonus, hasMasteryRegeneration,
                        alchemySkill, cookingPotionsSkill,
                        odemZfw, analysZfw,
                        hasAlchemy, hasCookingPotions, hasOdem, hasAnalys,
                        selfControlSkill, sensoryAcuitySkill, magicalLoreSkill, herbalLoreSkill,
                        isGameMaster,
                        'group',
                        alchemyIsMagicalMastery, cookingPotionsIsMagicalMastery,
                        defaultLaboratory,
                        groupId
                    FROM characters
                """.trimIndent())
                
                // 9.3. Alte Tabelle löschen
                database.execSQL("DROP TABLE characters")
                
                // 9.4. Neue Tabelle umbenennen
                database.execSQL("ALTER TABLE characters_new RENAME TO characters")
                
                // 9.5. Index für groupId erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_characters_groupId ON characters(groupId)")
            }
        }
        
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Locations-Tabelle erstellen (OHNE DEFAULT-Werte in der Tabellendefinition)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        isDefault INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Index für characterId erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_locations_characterId ON locations(characterId)")
                
                // 2. Items-Tabelle erstellen (OHNE DEFAULT-Werte in der Tabellendefinition)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterId INTEGER NOT NULL,
                        locationId INTEGER,
                        name TEXT NOT NULL,
                        stone INTEGER NOT NULL,
                        ounces INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE,
                        FOREIGN KEY(locationId) REFERENCES locations(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                
                // Indices für items erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_items_characterId ON items(characterId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_items_locationId ON items(locationId)")
                
                // 3. Potion-Tabelle erweitern mit locationId
                database.execSQL("ALTER TABLE potions ADD COLUMN locationId INTEGER")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_potions_locationId ON potions(locationId)")
                
                // 4. Standard-Locations für alle bestehenden Charaktere erstellen
                val charactersCursor = database.query("SELECT id FROM characters")
                while (charactersCursor.moveToNext()) {
                    val characterId = charactersCursor.getLong(0)
                    
                    // "Am Körper" Location
                    database.execSQL("""
                        INSERT INTO locations (characterId, name, isDefault, sortOrder) 
                        VALUES (?, 'Am Körper', 1, 0)
                    """.trimIndent(), arrayOf(characterId))
                    
                    val bodyLocationIdCursor = database.query("SELECT last_insert_rowid()")
                    var bodyLocationId: Long = 0
                    if (bodyLocationIdCursor.moveToFirst()) {
                        bodyLocationId = bodyLocationIdCursor.getLong(0)
                    }
                    bodyLocationIdCursor.close()
                    
                    // "Rucksack" Location
                    database.execSQL("""
                        INSERT INTO locations (characterId, name, isDefault, sortOrder) 
                        VALUES (?, 'Rucksack', 1, 1)
                    """.trimIndent(), arrayOf(characterId))
                    
                    val backpackLocationIdCursor = database.query("SELECT last_insert_rowid()")
                    var backpackLocationId: Long = 0
                    if (backpackLocationIdCursor.moveToFirst()) {
                        backpackLocationId = backpackLocationIdCursor.getLong(0)
                    }
                    backpackLocationIdCursor.close()
                    
                    // Alle bestehenden Tränke des Charakters in "Rucksack" legen
                    database.execSQL("""
                        UPDATE potions SET locationId = ? WHERE characterId = ?
                    """.trimIndent(), arrayOf(backpackLocationId, characterId))
                }
                charactersCursor.close()
            }
        }
        
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Locations-Tabelle erweitern um isCarried
                database.execSQL("ALTER TABLE locations ADD COLUMN isCarried INTEGER NOT NULL DEFAULT 0")
                
                // 2. "Am Körper" umbenennen zu "Rüstung/Kleidung" und als getragen markieren
                database.execSQL("""
                    UPDATE locations 
                    SET name = 'Rüstung/Kleidung', isCarried = 1 
                    WHERE name = 'Am Körper' AND isDefault = 1
                """)
                
                // 3. "Rucksack" als getragen markieren
                database.execSQL("""
                    UPDATE locations 
                    SET isCarried = 1 
                    WHERE name = 'Rucksack' AND isDefault = 1
                """)
            }
        }
        
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Items-Tabelle um Geldbeutel-Felder erweitern
                database.execSQL("ALTER TABLE items ADD COLUMN isPurse INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE items ADD COLUMN kreuzerAmount INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Potions-Tabelle um Haltbarmachen-Felder erweitern
                database.execSQL("ALTER TABLE potions ADD COLUMN createdDate TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE potions ADD COLUMN preservationAttempted INTEGER NOT NULL DEFAULT 0")
                
                // Setze createdDate für bestehende Tränke auf "unbekannt"
                // In der Praxis sollte dies das aktuelle Datum sein, aber wir können das nicht rekonstruieren
                database.execSQL("UPDATE potions SET createdDate = 'Unbekannt'")
            }
        }
        
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recipes-Tabelle neu erstellen mit lab als nullable
                // SQLite unterstützt kein ALTER COLUMN, daher müssen wir die Tabelle neu erstellen
                
                // 1. Temporäre Tabelle mit korrektem Schema erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        brewingDifficulty INTEGER NOT NULL,
                        analysisDifficulty INTEGER NOT NULL,
                        appearance TEXT NOT NULL,
                        shelfLife TEXT NOT NULL,
                        gruppe TEXT NOT NULL,
                        lab TEXT,
                        preis INTEGER,
                        zutatenPreis INTEGER,
                        zutatenVerbreitung INTEGER NOT NULL,
                        verbreitung INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // 2. Daten von alter Tabelle kopieren
                // Konvertiere leere lab-Strings zu NULL
                database.execSQL("""
                    INSERT INTO recipes_new (
                        id, name, brewingDifficulty, analysisDifficulty, appearance, shelfLife,
                        gruppe, lab, preis, zutatenPreis, zutatenVerbreitung, verbreitung
                    )
                    SELECT 
                        id, name, brewingDifficulty, analysisDifficulty, appearance, shelfLife,
                        gruppe, CASE WHEN lab = '' THEN NULL ELSE lab END, preis, zutatenPreis, zutatenVerbreitung, verbreitung
                    FROM recipes
                """.trimIndent())
                
                // 3. Alte Tabelle löschen
                database.execSQL("DROP TABLE recipes")
                
                // 4. Neue Tabelle umbenennen
                database.execSQL("ALTER TABLE recipes_new RENAME TO recipes")
            }
        }
        
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Entferne das veraltete 'group'-String-Feld aus der characters-Tabelle
                // SQLite unterstützt kein ALTER TABLE DROP COLUMN, daher müssen wir die Tabelle neu erstellen
                
                // 1. Temporäre Tabelle ohne 'group'-Spalte erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS characters_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        guid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        mu INTEGER NOT NULL,
                        kl INTEGER NOT NULL,
                        inValue INTEGER NOT NULL,
                        ch INTEGER NOT NULL,
                        ff INTEGER NOT NULL,
                        ge INTEGER NOT NULL,
                        ko INTEGER NOT NULL,
                        kk INTEGER NOT NULL,
                        hasApplicatus INTEGER NOT NULL,
                        applicatusZfw INTEGER NOT NULL,
                        applicatusModifier INTEGER NOT NULL,
                        currentLe INTEGER NOT NULL,
                        maxLe INTEGER NOT NULL,
                        hasAe INTEGER NOT NULL,
                        currentAe INTEGER NOT NULL,
                        maxAe INTEGER NOT NULL,
                        hasKe INTEGER NOT NULL,
                        currentKe INTEGER NOT NULL,
                        maxKe INTEGER NOT NULL,
                        leRegenBonus INTEGER NOT NULL,
                        aeRegenBonus INTEGER NOT NULL,
                        hasMasteryRegeneration INTEGER NOT NULL,
                        alchemySkill INTEGER NOT NULL,
                        cookingPotionsSkill INTEGER NOT NULL,
                        odemZfw INTEGER NOT NULL,
                        analysZfw INTEGER NOT NULL,
                        hasAlchemy INTEGER NOT NULL,
                        hasCookingPotions INTEGER NOT NULL,
                        hasOdem INTEGER NOT NULL,
                        hasAnalys INTEGER NOT NULL,
                        selfControlSkill INTEGER NOT NULL,
                        sensoryAcuitySkill INTEGER NOT NULL,
                        magicalLoreSkill INTEGER NOT NULL,
                        herbalLoreSkill INTEGER NOT NULL,
                        isGameMaster INTEGER NOT NULL,
                        alchemyIsMagicalMastery INTEGER NOT NULL,
                        cookingPotionsIsMagicalMastery INTEGER NOT NULL,
                        defaultLaboratory TEXT,
                        groupId INTEGER,
                        FOREIGN KEY(groupId) REFERENCES groups(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // 2. Daten von alter Tabelle kopieren (ohne 'group'-Spalte)
                database.execSQL("""
                    INSERT INTO characters_new (
                        id, guid, name, mu, kl, inValue, ch, ff, ge, ko, kk,
                        hasApplicatus, applicatusZfw, applicatusModifier,
                        currentLe, maxLe, hasAe, currentAe, maxAe, hasKe, currentKe, maxKe,
                        leRegenBonus, aeRegenBonus, hasMasteryRegeneration,
                        alchemySkill, cookingPotionsSkill,
                        odemZfw, analysZfw,
                        hasAlchemy, hasCookingPotions, hasOdem, hasAnalys,
                        selfControlSkill, sensoryAcuitySkill, magicalLoreSkill, herbalLoreSkill,
                        isGameMaster,
                        alchemyIsMagicalMastery, cookingPotionsIsMagicalMastery,
                        defaultLaboratory,
                        groupId
                    )
                    SELECT 
                        id, guid, name, mu, kl, inValue, ch, ff, ge, ko, kk,
                        hasApplicatus, applicatusZfw, applicatusModifier,
                        currentLe, maxLe, hasAe, currentAe, maxAe, hasKe, currentKe, maxKe,
                        leRegenBonus, aeRegenBonus, hasMasteryRegeneration,
                        alchemySkill, cookingPotionsSkill,
                        odemZfw, analysZfw,
                        hasAlchemy, hasCookingPotions, hasOdem, hasAnalys,
                        selfControlSkill, sensoryAcuitySkill, magicalLoreSkill, herbalLoreSkill,
                        isGameMaster,
                        alchemyIsMagicalMastery, cookingPotionsIsMagicalMastery,
                        defaultLaboratory,
                        groupId
                    FROM characters
                """.trimIndent())
                
                // 3. Alte Tabelle löschen
                database.execSQL("DROP TABLE characters")
                
                // 4. Neue Tabelle umbenennen
                database.execSQL("ALTER TABLE characters_new RENAME TO characters")
                
                // 5. Index für groupId erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_characters_groupId ON characters(groupId)")
            }
        }
        
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Füge quantity und isCountable Felder zur items-Tabelle hinzu
                // Füge quantityProduced Feld zur recipes-Tabelle hinzu
                
                // 1. Items-Tabelle erweitern
                // Temporäre Tabelle mit neuen Feldern erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterId INTEGER NOT NULL,
                        locationId INTEGER,
                        name TEXT NOT NULL,
                        stone INTEGER NOT NULL,
                        ounces INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        isPurse INTEGER NOT NULL,
                        kreuzerAmount INTEGER NOT NULL,
                        isCountable INTEGER NOT NULL DEFAULT 0,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE,
                        FOREIGN KEY(locationId) REFERENCES locations(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                
                // Daten kopieren
                database.execSQL("""
                    INSERT INTO items_new (
                        id, characterId, locationId, name, stone, ounces, 
                        sortOrder, isPurse, kreuzerAmount, isCountable, quantity
                    )
                    SELECT 
                        id, characterId, locationId, name, stone, ounces, 
                        sortOrder, isPurse, kreuzerAmount, 0, 1
                    FROM items
                """.trimIndent())
                
                // Alte Tabelle löschen und neue umbenennen
                database.execSQL("DROP TABLE items")
                database.execSQL("ALTER TABLE items_new RENAME TO items")
                
                // Indizes neu erstellen
                database.execSQL("CREATE INDEX IF NOT EXISTS index_items_characterId ON items(characterId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_items_locationId ON items(locationId)")
                
                // 2. Recipes-Tabelle erweitern
                // Temporäre Tabelle mit neuem Feld erstellen
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        gruppe TEXT NOT NULL,
                        lab TEXT,
                        preis INTEGER,
                        zutatenPreis INTEGER,
                        zutatenVerbreitung INTEGER NOT NULL,
                        verbreitung INTEGER NOT NULL,
                        brewingDifficulty INTEGER NOT NULL,
                        analysisDifficulty INTEGER NOT NULL,
                        appearance TEXT NOT NULL,
                        shelfLife TEXT NOT NULL,
                        quantityProduced INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                
                // Daten kopieren mit Standard-Mengen
                database.execSQL("""
                    INSERT INTO recipes_new (
                        id, name, gruppe, lab, preis, zutatenPreis, zutatenVerbreitung,
                        verbreitung, brewingDifficulty, analysisDifficulty, appearance, 
                        shelfLife, quantityProduced
                    )
                    SELECT 
                        id, name, gruppe, lab, preis, zutatenPreis, zutatenVerbreitung,
                        verbreitung, brewingDifficulty, analysisDifficulty, appearance, 
                        shelfLife,
                        CASE name
                            WHEN 'Zauberkreide' THEN 12
                            WHEN 'Pastillen gegen Erschöpfung' THEN 3
                            WHEN 'Dingens gegen Unsichtbares' THEN 3
                            WHEN 'Beschwörungskerzen' THEN 7
                            WHEN 'Purpurwasser' THEN 3
                            WHEN 'Regenbogenstaub' THEN 5
                            WHEN 'Mengbiller Bannbalöl' THEN 8
                            WHEN 'Liebestrunk' THEN 5
                            WHEN 'Hauch der Weissagung' THEN 7
                            WHEN 'Schlafgift' THEN 2
                            WHEN 'Waffenbalsam' THEN 5
                            ELSE 1
                        END
                    FROM recipes
                """.trimIndent())
                
                // Alte Tabelle löschen und neue umbenennen
                database.execSQL("DROP TABLE recipes")
                database.execSQL("ALTER TABLE recipes_new RENAME TO recipes")
            }
        }
        
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Füge quantity Feld zur potions-Tabelle hinzu
                database.execSQL("ALTER TABLE potions ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1")
            }
        }
        
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Füge lastModifiedDate zur characters-Tabelle hinzu
                // Setze initial auf aktuelle Zeit
                val currentTime = System.currentTimeMillis()
                database.execSQL("ALTER TABLE characters ADD COLUMN lastModifiedDate INTEGER NOT NULL DEFAULT $currentTime")
            }
        }
        
        fun getDatabase(context: Context): ApplicatusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApplicatusDatabase::class.java,
                    "applicatus_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Initiale Zauber und Rezepte beim ersten Start einfügen
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                database.spellDao().insertSpells(InitialSpells.getDefaultSpells())
                                database.recipeDao().insertRecipes(InitialRecipes.getDefaultRecipes())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
