package de.applicatus.app.logic

import kotlin.math.roundToInt

/**
 * Parser für die baseQuantity-Angaben der Kräuter
 * 
 * Unterstützt folgende Formate:
 * 1. Würfelnotationen: "W6 Blätter", "2W20 Blüten", "W20+5 Schoten"
 * 2. Festgelegte Mengen mit Zahlwörtern: "eine Pflanze", "zwei Ranken", "vier Blätter", "12 Stängel"
 * 3. Mehrere Produkte: "2W6 Kolben und W3 Stängel", "Strauch mit W6 Blüten"
 * 4. Komplexe Beschreibungen mit Abhängigkeiten: "W6 erntereife Schoten mit je W3 Kernen"
 * 5. Leere Strings: "" (wenn Kraut nichts Erntbares hat)
 * 6. TaP*-Bedingungen: "IF TaP*>=7: 7W6 Beeren" (nur wenn TaP* hoch genug)
 * 
 * Gibt eine Liste von HerbHarvestItem zurück, jedes mit Produktname und Menge.
 * Die Menge kann entweder fest sein oder eine Würfelnotation enthalten.
 */
object BaseQuantityParser {
    
    /**
     * Ein geerntetes Produkt von einem Kraut
     * 
     * @param productName Name des Produkts (z.B. "Blätter", "Blüten", "Wurzel")
     * @param quantity Menge als String (entweder Zahl oder Würfelnotation wie "W6", "2W20+5")
     * @param rolled Ob die Menge gewürfelt wurde (true) oder fest ist (false)
     * @param diceRoll Die ursprüngliche Würfelnotation (z.B. "2W6" wenn quantity "7" ist)
     * @param individualRolls Liste aller Einzelwürfe bei Mehrfachportionen (z.B. [5, 7, 6] bei 3 Portionen)
     * @param requiredTapStar Minimale TaP* die erforderlich sind (null wenn bedingungslos)
     */
    data class HerbHarvestItem(
        val productName: String,
        val quantity: String,
        val rolled: Boolean = false,
        val diceRoll: String? = null,
        val individualRolls: List<Int> = emptyList(),
        val requiredTapStar: Int? = null
    )
    
    /**
     * Parst eine baseQuantity-Angabe und gibt Liste von Ernte-Items zurück
     * 
     * @param baseQuantity Der zu parsende String
     * @param roll Soll gewürfelt werden? Wenn false, bleiben Würfelnotationen als String
     * @param tapStar Talentwert-Punkte aus der Probe (für TaP*-Bedingungen)
     * @return Liste der gefundenen Produkte
     */
    fun parse(baseQuantity: String, roll: Boolean = false, tapStar: Int = 0): List<HerbHarvestItem> {
        if (baseQuantity.isBlank()) return emptyList()
        
        // Entferne Klammern-Kommentare
        val cleaned = baseQuantity.replace(Regex("""\s*\([^)]*\)"""), "").trim()
        
        // Mehrere Produkte durch Semikolon (primär) oder "und" (Fallback)
        // Semikolon ist der Standard-Trenner, "und" für Rückwärtskompatibilität
        val parts = if (cleaned.contains(";")) {
            cleaned.split(Regex("""\s*;\s*"""))
        } else {
            cleaned.split(Regex("""\s+und\s+"""))
        }
        
        return parts.mapNotNull { part ->
            parseSingleItem(part.trim(), roll, tapStar)
        }
    }
    
    /**
     * Parst ein einzelnes Ernte-Item
     * 
     * Beispiele:
     * - "W6 Blätter" → HerbHarvestItem("Blätter", "W6")
     * - "2W20 Blüten" → HerbHarvestItem("Blüten", "2W20")
     * - "eine Wurzel" → HerbHarvestItem("Wurzel", "1")
     * - "12 Stängel" → HerbHarvestItem("Stängel", "12")
     * - "Strauch mit W6 Blüten" → HerbHarvestItem("Blüten", "W6")
     * - "IF TaP*>=7: 7W6 Beeren" → HerbHarvestItem("Beeren", "7W6", requiredTapStar=7) nur wenn tapStar >= 7
     */
    private fun parseSingleItem(item: String, roll: Boolean, tapStar: Int): HerbHarvestItem? {
        if (item.isBlank()) return null
        
        // Prüfe auf TaP*-Bedingung: "IF TaP*>=7: 7W6 Beeren" oder "IF TaP*>6: 7W6 Beeren"
        val tapConditionPattern = Regex("""IF\s+TaP\*\s*([><=]+)\s*(\d+)\s*:\s*(.+)""", RegexOption.IGNORE_CASE)
        val tapMatch = tapConditionPattern.matchEntire(item)
        
        if (tapMatch != null) {
            val operator = tapMatch.groupValues[1]
            val requiredValue = tapMatch.groupValues[2].toIntOrNull() ?: 0
            val contentAfterCondition = tapMatch.groupValues[3].trim()
            
            // Prüfe ob Bedingung erfüllt ist
            val conditionMet = when {
                operator.contains(">=") -> tapStar >= requiredValue
                operator.contains("<=") -> tapStar <= requiredValue
                operator.contains(">") && !operator.contains("=") -> tapStar > requiredValue
                operator.contains("<") && !operator.contains("=") -> tapStar < requiredValue
                operator == "=" || operator == "==" -> tapStar == requiredValue
                else -> false
            }
            
            if (!conditionMet) {
                return null // Bedingung nicht erfüllt, kein Item zurückgeben
            }
            
            // Bedingung erfüllt, parse den Rest
            val innerItem = parseSingleItem(contentAfterCondition, roll, tapStar)
            return innerItem?.copy(requiredTapStar = requiredValue)
        }
        
        // Entferne "mit" und nachfolgendes (z.B. "Strauch mit W6 Blüten")
        val withPattern = Regex(""".*\s+mit\s+(.+)""")
        val withMatch = withPattern.matchEntire(item)
        val workingItem = if (withMatch != null) {
            withMatch.groupValues[1]
        } else {
            item
        }
        
        // Entferne zusätzliche Beschreibungen nach Komma (z.B. "5 Blüten, die kurz..." → "5 Blüten")
        val cleanedItem = workingItem.split(",")[0].trim()
        
        // Muster: [Menge] [Produktname]
        // Würfelnotationen: W6, 2W20, W20+5, 1W3
        val dicePattern = Regex("""(\d*W\d+(?:[+\-]\d+)?)\s+(.+)""", RegexOption.IGNORE_CASE)
        val diceMatch = dicePattern.matchEntire(cleanedItem)
        
        if (diceMatch != null) {
            val diceNotation = diceMatch.groupValues[1]
            val productName = diceMatch.groupValues[2].trim()
            
            return if (roll) {
                // Verwende ProbeChecker.rollDice statt eigener Implementierung
                val rolledValue = ProbeChecker.rollDice(diceNotation)
                HerbHarvestItem(productName, (rolledValue ?: 1).toString(), rolled = true, diceRoll = diceNotation)
            } else {
                HerbHarvestItem(productName, diceNotation, rolled = false)
            }
        }
        
        // Zahlwörter und Zahlen
        val numberPattern = Regex("""(\d+|eine?|zwei|drei|vier|fünf|sechs|sieben|acht|neun|zehn|elf|zwölf)\s+(.+)""", RegexOption.IGNORE_CASE)
        val numberMatch = numberPattern.matchEntire(cleanedItem)
        
        if (numberMatch != null) {
            val numberStr = numberMatch.groupValues[1].lowercase()
            val productName = numberMatch.groupValues[2].trim()
            val quantity = wordToNumber(numberStr)
            
            return HerbHarvestItem(productName, quantity.toString(), rolled = false)
        }
        
        // Sonderfälle: "Saft einer Pflanze" → "Saft", 1
        if (cleanedItem.startsWith("Saft", ignoreCase = true)) {
            return HerbHarvestItem("Saft", "1", rolled = false)
        }
        
        // Fallback: Ganzes Item als Produktname, Menge 1
        return HerbHarvestItem(cleanedItem, "1", rolled = false)
    }
    
    /**
     * Konvertiert deutsche Zahlwörter in Zahlen
     */
    private fun wordToNumber(word: String): Int {
        return when (word.lowercase()) {
            "ein", "eine", "einer", "einem", "einen" -> 1
            "zwei" -> 2
            "drei" -> 3
            "vier" -> 4
            "fünf" -> 5
            "sechs" -> 6
            "sieben" -> 7
            "acht" -> 8
            "neun" -> 9
            "zehn" -> 10
            "elf" -> 11
            "zwölf" -> 12
            else -> word.toIntOrNull() ?: 1
        }
    }
    
    /**
     * Würfelt eine baseQuantity und gibt konkrete Mengen zurück
     * 
     * @param baseQuantity Der zu parsende und würfelnde String
     * @param tapStar Talentwert-Punkte aus der Probe (für TaP*-Bedingungen)
     * @return Liste der Produkte mit gewürfelten Mengen
     */
    fun rollQuantity(baseQuantity: String, tapStar: Int = 0): List<HerbHarvestItem> {
        return parse(baseQuantity, roll = true, tapStar = tapStar)
    }
}
