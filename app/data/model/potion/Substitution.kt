package de.applicatus.app.data.model.potion

/**
 * Art der Substitution bei Trank-Zutaten
 */
enum class SubstitutionType(val displayName: String, val modifier: Int) {
    OPTIMIZING("Optimierende Substitution", -3),
    EQUIVALENT("Gleichwertige Substitution", 0),
    SENSIBLE("Sinnvolle Substitution", 3),
    POSSIBLE("Mögliche Substitution", 6)
}

/**
 * Eine Substitution bei der Trank-Herstellung
 */
data class Substitution(
    val type: SubstitutionType,
    val description: String = ""
)
