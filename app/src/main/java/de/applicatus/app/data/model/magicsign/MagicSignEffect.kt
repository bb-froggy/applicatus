package de.applicatus.app.data.model.magicsign

/**
 * Enum für spezielle Effekte von Zauberzeichen.
 * Die meisten Zauberzeichen haben keinen besonderen mechanischen Effekt in der App,
 * aber einige wie die Sigille des Unsichtbaren Trägers haben hardcodierte Auswirkungen.
 */
enum class MagicSignEffect(
    val displayName: String,
    val description: String
) {
    /** Kein besonderer Effekt - die Wirkung wird nur als Text beschrieben */
    NONE("Kein Spezialeffekt", "Die Wirkung des Zauberzeichens hat keinen mechanischen Effekt in der App."),
    
    /** 
     * Sigille des Unsichtbaren Trägers: Reduziert das Gewicht eines Ortes/Objekts
     * um RkP* × 2 Stein, mindestens 1 Stein (wenn das Objekt ohne Zeichen mindestens 1 Stein wiegt)
     */
    WEIGHT_REDUCTION(
        "Sigille des Unsichtbaren Trägers",
        "Reduziert das Gesamtgewicht des Objekts um RkP* × 2 Stein (mindestens 1 Stein)."
    )
}
