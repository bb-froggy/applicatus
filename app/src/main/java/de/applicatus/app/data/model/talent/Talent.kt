package de.applicatus.app.data.model.talent

/**
 * Alle bekannten DSA-Talente mit ihren Eigenschaftsproben
 * 
 * Jedes Talent hat drei Eigenschaften, auf die gewürfelt wird.
 * Diese werden als Strings gespeichert, die mit den Eigenschaftsnamen
 * im Character-Modell übereinstimmen müssen.
 */
enum class Talent(
    val talentName: String,
    val attribute1: String,
    val attribute2: String,
    val attribute3: String
) {
    // Handwerkstalente
    ALCHEMY("Alchimie", "MU", "KL", "FF"),
    COOKING_POTIONS("Kochen (Tränke)", "KL", "IN", "FF"),
    
    // Wissenstalente  
    MAGICAL_LORE("Magiekunde", "KL", "KL", "IN"),
    HERBAL_LORE("Pflanzenkunde", "KL", "IN", "FF"),
    
    // Körperliche Talente
    SELF_CONTROL("Selbstbeherrschung", "MU", "KO", "KK"),
    
    // Sinnestalente
    SENSORY_ACUITY("Sinnenschärfe", "KL", "IN", "IN");
    
    companion object {
        /**
         * Findet ein Talent anhand seines Namens
         */
        fun fromName(name: String): Talent? {
            return entries.find { it.talentName.equals(name, ignoreCase = true) }
        }
        
        /**
         * Gibt alle Talentnamen zurück
         */
        fun getAllTalentNames(): List<String> {
            return entries.map { it.talentName }
        }
    }
}
