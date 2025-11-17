package de.applicatus.app.data.model.spell

/**
 * Wirkungsdauer für Applicatus-Zauber
 * 
 * Definiert die möglichen Wirkungsdauern und die damit verbundenen Erschwernisse
 * für das Einspeichern von Zaubern mit dem Applicatus-Zauber.
 */
enum class ApplicatusDuration(
    val displayName: String,
    val difficultyModifier: Int
) {
    /** Bis zum nächsten Sonnenaufgang (+0 Erschwernis) */
    DAY("Tag", 0),
    
    /** Bis zum Ende des aktuellen Mondes (+3 Erschwernis) */
    MOON("Mond", 3),
    
    /** Bis zur nächsten Quartalsgrenze (+5 Erschwernis) */
    QUARTER("Quartal", 5),
    
    /** Bis zur nächsten Wintersonnenwende (+7 Erschwernis) */
    WINTER_SOLSTICE("Wintersonnenwende", 7);
    
    companion object {
        /** Standard-Wirkungsdauer wenn nicht explizit gesetzt */
        val DEFAULT = DAY
    }
}
