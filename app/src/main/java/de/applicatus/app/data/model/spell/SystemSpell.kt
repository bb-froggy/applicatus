package de.applicatus.app.data.model.spell

/**
 * Spezielle System-Zauber mit festen Eigenschaften
 * 
 * Diese Zauber werden für Alchimie-Analysen verwendet und haben
 * fest definierte Eigenschaftsproben.
 */
enum class SystemSpell(
    val spellName: String,
    val attribute1: String,
    val attribute2: String,
    val attribute3: String
) {
    /**
     * ODEM ARCANUM - Intensitätsbestimmung
     * Probe auf KL/IN/IN
     */
    ODEM("ODEM ARCANUM", "KL", "IN", "IN"),
    
    /**
     * ANALYS ARKANSTRUKTUR - Strukturanalyse
     * Probe auf KL/KL/IN
     */
    ANALYS("ANALYS ARKANSTRUKTUR", "KL", "KL", "IN"),
    
    /**
     * APPLICATUS - Zaubereinspeicherung
     * Probe auf KL/FF/FF
     */
    APPLICATUS("APPLICATUS", "KL", "FF", "FF");
    
    companion object {
        /**
         * Findet einen System-Zauber anhand seines Namens
         */
        fun fromName(name: String): SystemSpell? {
            return entries.find { it.spellName.equals(name, ignoreCase = true) }
        }
    }
}
