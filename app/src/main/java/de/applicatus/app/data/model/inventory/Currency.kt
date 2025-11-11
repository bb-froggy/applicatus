package de.applicatus.app.data.model.inventory

/**
 * DSA Währungssystem
 * 1 Dukaten (D) = 10 Silbertaler (S) = 100 Heller (H) = 1000 Kreuzer (K)
 */
data class Currency(
    val dukaten: Int = 0,
    val silbertaler: Int = 0,
    val heller: Int = 0,
    val kreuzer: Int = 0
) {
    /**
     * Konvertiert zu Kreuzern (Basis-Einheit)
     */
    fun toKreuzer(): Int {
        return dukaten * 1000 + silbertaler * 100 + heller * 10 + kreuzer
    }
    
    /**
     * Berechnet das Gewicht der Münzen nach DSA-Regeln
     * - 1 Dukaten = 1 Unze
     * - 1 Silbertaler, Heller, Kreuzer = 1 Skrupel = 1/5 Unze (5 Silbertaler = 1 Unze)
     */
    fun toWeight(): Weight {
        // Berechne Gesamtgewicht in Unzen (mit Bruchrechnung)
        val totalOunces = dukaten * 1.0 +           // 1 D = 1 Unze
                         silbertaler * 0.2 +        // 1 S = 1/5 Unze
                         heller * 0.2 +             // 1 H = 1/5 Unze
                         kreuzer * 0.2              // 1 K = 1/5 Unze
        
        return Weight.fromOunces(kotlin.math.ceil(totalOunces).toInt())
    }
    
    /**
     * Formatiert die Währung für die Anzeige (nur nicht-null Werte)
     */
    fun toDisplayString(): String {
        val parts = mutableListOf<String>()
        if (dukaten > 0) parts.add("$dukaten D")
        if (silbertaler > 0) parts.add("$silbertaler S")
        if (heller > 0) parts.add("$heller H")
        if (kreuzer > 0) parts.add("$kreuzer K")
        
        return if (parts.isEmpty()) "0 K" else parts.joinToString(" ")
    }
    
    companion object {
        /**
         * Erstellt Currency aus Kreuzern (normalisiert automatisch)
         */
        fun fromKreuzer(totalKreuzer: Int): Currency {
            var remaining = totalKreuzer
            
            val dukaten = remaining / 1000
            remaining %= 1000
            
            val silbertaler = remaining / 100
            remaining %= 100
            
            val heller = remaining / 10
            remaining %= 10
            
            val kreuzer = remaining
            
            return Currency(dukaten, silbertaler, heller, kreuzer)
        }
        
        val ZERO = Currency(0, 0, 0, 0)
    }
}
