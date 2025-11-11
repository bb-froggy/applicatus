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
     * Berechnet das Gewicht der Münzen
     * 10 Münzen = 1 Unze (egal welche Sorte)
     */
    fun toWeight(): Weight {
        val totalCoins = dukaten + silbertaler + heller + kreuzer
        val ounces = (totalCoins + 9) / 10 // Aufrunden
        return Weight.fromOunces(ounces)
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
