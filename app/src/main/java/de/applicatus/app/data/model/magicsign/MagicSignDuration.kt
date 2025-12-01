package de.applicatus.app.data.model.magicsign

/**
 * Enum für die möglichen Wirkdauern von Zauberzeichen.
 */
enum class MagicSignDuration(
    val displayName: String,
    val description: String
) {
    /** Wirkdauer: RkW/2 Tage (aufgerundet) */
    HALF_RKW_DAYS("RkW/2 Tage", "Die Wirkdauer beträgt die Hälfte des Ritualkenntniswerts in Tagen."),
    
    /** Wirkdauer: Ein Monat (30 Tage) */
    ONE_MONTH("1 Monat", "Die Wirkdauer beträgt einen Monat (30 Tage)."),
    
    /** Wirkdauer: Ein Quartal (3 Monate = 90 Tage) */
    ONE_QUARTER("1 Quartal", "Die Wirkdauer beträgt ein Quartal (3 Monate)."),
    
    /** Wirkdauer: Bis zur nächsten Wintersonnenwende */
    UNTIL_WINTER_SOLSTICE("Bis Wintersonnenwende", "Die Wirkdauer endet an der nächsten Wintersonnenwende (1. Firun).")
}
