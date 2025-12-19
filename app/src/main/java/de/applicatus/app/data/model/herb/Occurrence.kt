package de.applicatus.app.data.model.herb

/**
 * Vorkommenshäufigkeit einer Pflanze in einer Landschaft
 * 
 * Die Werte entsprechen den Erschwernissen aus Zoo-Botanica Aventurica
 */
enum class Occurrence(val modifier: Int) {
    VERY_COMMON(1),      // Sehr häufig
    COMMON(2),           // Häufig
    OCCASIONAL(4),       // Gelegentlich
    RARE(8),             // Selten
    VERY_RARE(16),       // Sehr selten
    NONE(100)            // Keine Vorkommen
}
