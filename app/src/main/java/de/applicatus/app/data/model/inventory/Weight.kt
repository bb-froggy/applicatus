package de.applicatus.app.data.model.inventory

/**
 * Gewicht in DSA-Einheiten (Stein und Unzen)
 * 1 Stein = 40 Unzen
 */
data class Weight(
    val stone: Int = 0,
    val ounces: Int = 0
) {
    /**
     * Gibt das Gesamtgewicht in Unzen zurück
     */
    fun toOunces(): Int = stone * 40 + ounces
    
    /**
     * Gibt das Gewicht formatiert als String zurück (z.B. "2 Stein 15 Unzen")
     */
    fun toDisplayString(): String {
        return when {
            stone == 0 && ounces == 0 -> "0 Unzen"
            stone == 0 -> "$ounces Unzen"
            ounces == 0 -> "$stone Stein"
            else -> "$stone Stein $ounces Unzen"
        }
    }
    
    /**
     * Multipliziert das Gewicht mit einer Menge
     */
    operator fun times(quantity: Int): Weight {
        if (quantity <= 0) return ZERO
        val totalOunces = toOunces() * quantity
        return fromOunces(totalOunces)
    }
    
    companion object {
        /**
         * Erstellt ein Weight-Objekt aus einer Anzahl von Unzen
         */
        fun fromOunces(totalOunces: Int): Weight {
            val stone = totalOunces / 40
            val ounces = totalOunces % 40
            return Weight(stone, ounces)
        }
        
        /**
         * Null-Gewicht
         */
        val ZERO = Weight(0, 0)
        
        /**
         * Gewicht eines Tranks (4 Unzen)
         */
        val POTION = Weight(0, 4)
    }
}
