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
import de.applicatus.app.data.dao.PotionDao
import de.applicatus.app.data.dao.RecipeDao
import de.applicatus.app.data.dao.RecipeKnowledgeDao
import de.applicatus.app.data.dao.SpellDao
import de.applicatus.app.data.dao.SpellSlotDao
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.GlobalSettings
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.RecipeKnowledge
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SpellSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Spell::class, Character::class, SpellSlot::class, Recipe::class, Potion::class, GlobalSettings::class, RecipeKnowledge::class],
    version = 16,
    exportSchema = false
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
    
    companion object {
        @Volatile
        private var INSTANCE: ApplicatusDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
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
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
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
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
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
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Regenerations-Felder zu Character-Tabelle hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN leRegenBonus INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN aeRegenBonus INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasMasteryRegeneration INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
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
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
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
        
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Boolean-Felder hinzufügen, um zu tracken ob Talente/Zauber überhaupt beherrscht werden
                database.execSQL("ALTER TABLE characters ADD COLUMN hasAlchemy INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasCookingPotions INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasOdem INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN hasAnalys INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        private val MIGRATION_8_9 = object : Migration(8, 9) {
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
        
        private val MIGRATION_9_10 = object : Migration(9, 10) {
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
        
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Character-Tabelle erweitern mit Spieler/Spielleiter-Flag
                database.execSQL("ALTER TABLE characters ADD COLUMN isGameMaster INTEGER NOT NULL DEFAULT 0")
                
                // SpellSlot-Tabelle erweitern mit Patzer-Flag
                database.execSQL("ALTER TABLE spell_slots ADD COLUMN isBotched INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        private val MIGRATION_11_12 = object : Migration(11, 12) {
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
        
        private val MIGRATION_12_13 = object : Migration(12, 13) {
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
        
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Magisches Meisterhandwerk-Felder zu Character-Tabelle hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN alchemyIsMagicalMastery INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE characters ADD COLUMN cookingPotionsIsMagicalMastery INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Default-Labor-Feld zu Character-Tabelle hinzufügen
                database.execSQL("ALTER TABLE characters ADD COLUMN defaultLaboratory TEXT")
            }
        }
        
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // nameKnown-Feld zu Potion-Tabelle hinzufügen
                database.execSQL("ALTER TABLE potions ADD COLUMN nameKnown INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        fun getDatabase(context: Context): ApplicatusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApplicatusDatabase::class.java,
                    "applicatus_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
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
