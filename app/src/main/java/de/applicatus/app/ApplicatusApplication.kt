package de.applicatus.app

import android.app.Application
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApplicatusApplication : Application() {
    private val database by lazy { ApplicatusDatabase.getDatabase(this) }
    val repository by lazy {
        ApplicatusRepository(
            database.spellDao(),
            database.characterDao(),
            database.spellSlotDao(),
            database.recipeDao(),
            database.potionDao(),
            database.globalSettingsDao(),
            database.recipeKnowledgeDao()
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        // Stelle sicher, dass GlobalSettings beim Start existieren
        CoroutineScope(Dispatchers.IO).launch {
            repository.ensureGlobalSettingsExist()
        }
    }
}
