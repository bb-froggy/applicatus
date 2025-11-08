package de.applicatus.app.data.model.potion

/**
 * Methoden für die Strukturanalyse von Elixieren
 */
enum class StructureAnalysisMethod {
    ANALYS_SPELL,        // Zauber Analys Arkanstruktur
    BY_SIGHT,            // Analyse nach Augenschein mit Alchimie
    LABORATORY           // Laboranalyse mit Alchimie (kann Trank zerstören)
}
