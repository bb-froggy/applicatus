package de.applicatus.app.data.model.potion

/**
 * Methoden für die Strukturanalyse von Elixieren
 */
enum class StructureAnalysisMethod {
    ANALYS_SPELL,              // Zauber Analys Arkanstruktur
    BY_SIGHT_ALCHEMY,          // Analyse nach Augenschein mit Alchimie
    BY_SIGHT_COOKING,          // Analyse nach Augenschein mit Kochen (Tränke)
    LABORATORY_ALCHEMY,        // Laboranalyse mit Alchimie (kann Trank zerstören)
    LABORATORY_COOKING,        // Laboranalyse mit Kochen (Tränke) (kann Trank zerstören)
    
    // Legacy-Werte für Abwärtskompatibilität
    @Deprecated("Use BY_SIGHT_ALCHEMY or BY_SIGHT_COOKING instead")
    BY_SIGHT,
    @Deprecated("Use LABORATORY_ALCHEMY or LABORATORY_COOKING instead")
    LABORATORY
}
