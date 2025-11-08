package de.applicatus.app.data.model.potion

/**
 * Grobe Qualitätskategorien für Intensitätsbestimmung
 * Schwach = A, B, C
 * Stark = D, E, F
 * Bei M ist es zufällig
 */
enum class IntensityQuality {
    UNKNOWN,  // Noch nicht bestimmt
    WEAK,     // Schwach (A, B oder C)
    STRONG    // Stark (D, E oder F)
}
