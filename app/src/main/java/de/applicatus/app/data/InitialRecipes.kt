package de.applicatus.app.data

import de.applicatus.app.data.model.Recipe

/**
 * Initiale Rezepte für die Datenbank
 */
object InitialRecipes {
    fun getDefaultRecipes(): List<Recipe> = listOf(
        Recipe(name = "Heiltrank"),
        Recipe(name = "Zaubertrank")
    )
}
