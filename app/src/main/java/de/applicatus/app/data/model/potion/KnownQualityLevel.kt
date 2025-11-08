package de.applicatus.app.data.model.potion

/**
 * Bekannte Qualitätsstufe eines Elixiers aus Sicht des Charakters
 * Stufen steigen mit zunehmender Analysetiefe
 */
enum class KnownQualityLevel {
    UNKNOWN,           // Keine Informationen über die Qualität
    WEAK_OR_STRONG,    // Schwach (A/B/C) oder stark (D/E/F) bekannt (ab 3 ZfP* Intensitätsbestimmung)
    VERY_WEAK_MEDIUM_OR_VERY_STRONG,  // Sehr schwach (A/B), mittel (C/D) oder sehr stark (E/F) (ab 4 TaP* Strukturanalyse mit vorheriger Intensitätsbestimmung)
    EXACT              // Genaue Qualität bekannt (ab 13 TaP* Strukturanalyse)
}
