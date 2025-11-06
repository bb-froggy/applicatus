package de.applicatus.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.applicatus.app.data.dao.CharacterDao
import de.applicatus.app.data.dao.PotionDao
import de.applicatus.app.data.dao.RecipeDao
import de.applicatus.app.data.dao.SpellDao
import de.applicatus.app.data.dao.SpellSlotDao
import de.applicatus.app.data.model.Character
import de.applicatus.app.data.model.Potion
import de.applicatus.app.data.model.Recipe
import de.applicatus.app.data.model.Spell
import de.applicatus.app.data.model.SpellSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Spell::class, Character::class, SpellSlot::class, Recipe::class, Potion::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ApplicatusDatabase : RoomDatabase() {
    abstract fun spellDao(): SpellDao
    abstract fun characterDao(): CharacterDao
    abstract fun spellSlotDao(): SpellSlotDao
    abstract fun recipeDao(): RecipeDao
    abstract fun potionDao(): PotionDao
    
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
        
        fun getDatabase(context: Context): ApplicatusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApplicatusDatabase::class.java,
                    "applicatus_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
