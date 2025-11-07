package de.applicatus.app.data.model.potion

/**
 * Wissensgrad eines Charakters über ein Rezept
 */
enum class RecipeKnowledgeLevel {
    UNKNOWN,      // Rezept ist unbekannt
    UNDERSTOOD,   // Rezept wurde verstanden (durch Analyse mit 19+ TaP*)
    KNOWN         // Rezept ist bekannt (komplett verstanden und kann gebraut werden)
}
