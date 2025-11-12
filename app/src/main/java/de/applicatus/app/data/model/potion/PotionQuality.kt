package de.applicatus.app.data.model.potion

/**
 * Qualität eines alchimistischen Produkts
 * X ist wirkungslos (überverdünnt), A ist die schwächste wirkende Qualitätsstufe, F die stärkste
 */
enum class PotionQuality {
    X,  // Wirkungslos (überverdünnt)
    A,  // Sehr schlecht
    B,  // Schlecht
    C,  // Unterdurchschnittlich
    D,  // Durchschnittlich
    E,  // Gut
    F,  // Sehr gut
    M   // Misslungen
}
