package de.applicatus.app

import android.app.Application
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.repository.ApplicatusRepository

class ApplicatusApplication : Application() {
    private val database by lazy { ApplicatusDatabase.getDatabase(this) }
    val repository by lazy {
        ApplicatusRepository(
            database.spellDao(),
            database.characterDao(),
            database.spellSlotDao(),
            database.recipeDao(),
            database.potionDao()
        )
    }
}
