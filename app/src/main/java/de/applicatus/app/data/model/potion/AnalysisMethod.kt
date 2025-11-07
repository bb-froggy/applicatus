package de.applicatus.app.data.model.potion

/**
 * Analysemethode für Tränke
 */
enum class AnalysisMethod {
    BY_SIGHT,         // Nach Augenschein (Alchemie)
    LABORATORY,       // Laboranalyse (Alchemie)
    ODEM_SPELL,       // Zauber Odem (nur mit AE)
    ANALYS_SPELL      // Zauber Analys (nur mit AE)
}
