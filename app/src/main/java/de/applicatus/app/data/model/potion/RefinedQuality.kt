package de.applicatus.app.data.model.potion

/**
 * Feinere Qualitätskategorien für Strukturanalyse mit Intensitätsbestimmung
 * Sehr schwach = A, B
 * Mittel = C, D
 * Sehr stark = E, F
 */
enum class RefinedQuality {
    UNKNOWN,       // Noch nicht bestimmt
    VERY_WEAK,     // Sehr schwach (A oder B)
    MEDIUM,        // Mittel (C oder D)
    VERY_STRONG    // Sehr stark (E oder F)
}
