package de.applicatus.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import de.applicatus.app.data.dao.CharacterDao
import de.applicatus.app.data.dao.SpellDao
import de.applicatus.app.data.dao.SpellSlotDao
import de.applicatus.app.data.model.Character
import de.applicatus.app.data.model.Spell
import de.applicatus.app.data.model.SpellSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Spell::class, Character::class, SpellSlot::class],
    version = 1,
    exportSchema = false
)
abstract class ApplicatusDatabase : RoomDatabase() {
    abstract fun spellDao(): SpellDao
    abstract fun characterDao(): CharacterDao
    abstract fun spellSlotDao(): SpellSlotDao
    
    companion object {
        @Volatile
        private var INSTANCE: ApplicatusDatabase? = null
        
        fun getDatabase(context: Context): ApplicatusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApplicatusDatabase::class.java,
                    "applicatus_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Initiale Zauber beim ersten Start einfügen
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                database.spellDao().insertSpells(InitialSpells.getDefaultSpells())
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
