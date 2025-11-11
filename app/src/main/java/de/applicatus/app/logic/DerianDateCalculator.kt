package de.applicatus.app.logic

import kotlin.random.Random

/**
 * Utility-Klasse für derische Datumsberechnungen
 * 
 * Derischer Kalender:
 * - 12 Monate (Göttermonate): Praios, Rondra, Efferd, Travia, Boron, Hesinde, 
 *   Firun, Tsa, Phex, Peraine, Ingerimm, Rahja
 * - Jeder Monat hat 30 Tage
 * - 5 Namenlose Tage zwischen Rahja und Praios (als 13. "Monat" mit 5 Tagen)
 * - Jahr = 365 Tage (360 + 5 Namenlose)
 */
object DerianDateCalculator {
    
    // Liste der derischen Monate (inkl. Namenlose Tage)
    private val months = listOf(
        "Praios", "Rondra", "Efferd", "Travia", "Boron", "Hesinde",
        "Firun", "Tsa", "Phex", "Peraine", "Ingerimm", "Rahja", "Namenlose Tage"
    )
    
    // Derische Wochentage (7-Tage-Woche)
    private val weekdays = listOf(
        "Windstag",     // 1. Tag der Woche
        "Erdstag",      // 2. Tag
        "Markttag",     // 3. Tag
        "Praiostag",    // 4. Tag
        "Rohalstag",    // 5. Tag
        "Feuertag",     // 6. Tag
        "Wassertag"     // 7. Tag
    )
    
    // Namenlose Tage als spezieller "Monat" Index
    private const val NAMELESS_DAYS_INDEX = 12
    
    // Mondphasen-Zyklus: 28 Tage (Mada)
    private const val MADA_CYCLE = 28
    
    // Regex für Würfelnotationen: z.B. "3W6+2", "2W20-5", "1W10"
    private val diceRegex = Regex("""(\d+)W(\d+)([+\-]\d+)?""", RegexOption.IGNORE_CASE)
    
    /**
     * Gibt die Anzahl der Tage für einen Monat zurück
     */
    private fun getDaysInMonth(monthIndex: Int): Int {
        return if (monthIndex == NAMELESS_DAYS_INDEX) 5 else 30
    }
    
    /**
     * Parst und würfelt eine Würfelnotation
     * 
     * @param diceNotation Würfelnotation (z.B. "3W6+2", "2W20-5", "1W10")
     * @return Gewürfeltes Ergebnis oder null bei ungültiger Notation
     */
    fun rollDice(diceNotation: String): Int? {
        val match = diceRegex.matchEntire(diceNotation.trim()) ?: return null
        
        val numDice = match.groupValues[1].toIntOrNull() ?: return null
        val diceSize = match.groupValues[2].toIntOrNull() ?: return null
        val modifier = match.groupValues[3].ifEmpty { "+0" }.toIntOrNull() ?: 0
        
        if (numDice < 1 || diceSize < 1) return null
        
        // Würfle alle Würfel und summiere
        var total = 0
        repeat(numDice) {
            total += Random.nextInt(1, diceSize + 1)
        }
        
        return total + modifier
    }
    
    /**
     * Extrahiert die Zeiteinheit aus einer Haltbarkeitsangabe
     * 
     * @param shelfLife Haltbarkeit (z.B. "3 Monde", "3W6+2 Wochen")
     * @return Zeiteinheit (z.B. "Monde", "Wochen") oder null
     */
    private fun extractTimeUnit(shelfLife: String): String? {
        val parts = shelfLife.trim().split(" ")
        if (parts.size < 2) return null
        
        // Die Zeiteinheit ist immer das letzte Wort
        return parts.last()
    }
    
    /**
     * Berechnet die Anzahl aus einer Haltbarkeitsangabe
     * Unterstützt sowohl feste Zahlen als auch Würfelnotationen
     * 
     * @param shelfLife Haltbarkeit (z.B. "3 Monde", "3W6+2 Wochen")
     * @return Berechnete Anzahl oder null bei Fehler
     */
    fun parseShelfLifeAmount(shelfLife: String): Int? {
        val parts = shelfLife.trim().split(" ")
        if (parts.isEmpty()) return null
        
        val amountPart = if (parts.size > 1) {
            // Bei mehreren Teilen ist der erste Teil die Menge
            parts.dropLast(1).joinToString(" ")
        } else {
            return null
        }
        
        // Prüfe auf Würfelnotation
        return if (diceRegex.matches(amountPart)) {
            rollDice(amountPart)
        } else {
            amountPart.toIntOrNull()
        }
    }
    
    /**
     * Berechnet das Haltbarkeitsdatum eines Tranks
     * 
     * @param currentDate Aktuelles Datum im Format "Tag Monat Jahr BF" (z.B. "15 Praios 1040 BF")
     * @param shelfLife Haltbarkeit (z.B. "3 Monde", "1 Jahr", "2 Wochen", "3W6+2 Wochen")
     * @return Haltbarkeitsdatum im gleichen Format
     */
    fun calculateExpiryDate(currentDate: String, shelfLife: String): String {
        try {
            // Parse aktuelles Datum
            val dateParts = currentDate.trim().split(" ")
            if (dateParts.size < 3) {
                return currentDate // Ungültiges Format
            }
            
            val day: Int = dateParts[0].toIntOrNull() ?: return currentDate
            
            // Behandle "Namenlose Tage" als zwei Wörter
            val monthName: String
            val year: Int
            val era: String
            
            if (dateParts.size >= 5 && dateParts[1] == "Namenlose" && dateParts[2] == "Tage") {
                monthName = "Namenlose Tage"
                year = dateParts[3].toIntOrNull() ?: return currentDate
                era = dateParts.getOrNull(4) ?: "BF"
            } else if (dateParts.size >= 4) {
                monthName = dateParts[1]
                year = dateParts[2].toIntOrNull() ?: return currentDate
                era = dateParts.getOrNull(3) ?: "BF"
            } else {
                return currentDate
            }
            
            val monthIndex = months.indexOf(monthName)
            if (monthIndex == -1) {
                return currentDate
            }
            
            // Parse Haltbarkeit (unterstützt Würfelnotationen)
            val amount = parseShelfLifeAmount(shelfLife) ?: return currentDate
            val unit = extractTimeUnit(shelfLife)?.lowercase() ?: return currentDate
            
            // Berechne Tage zum Addieren
            val daysToAdd = when {
                unit.startsWith("tag") -> amount
                unit.startsWith("woche") -> amount * 7
                unit.startsWith("mond") || unit.startsWith("monat") -> amount * 30
                unit.startsWith("jahr") -> amount * 365
                else -> 0
            }
            
            // Berechne neues Datum
            var newDay: Int = day
            var newMonth: Int = monthIndex
            var newYear: Int = year
            var remainingDays = daysToAdd
            
            while (remainingDays > 0) {
                val daysInCurrentMonth = getDaysInMonth(newMonth)
                val daysLeftInMonth = daysInCurrentMonth - newDay
                
                if (remainingDays <= daysLeftInMonth) {
                    newDay += remainingDays
                    remainingDays = 0
                } else {
                    remainingDays -= (daysLeftInMonth + 1)
                    newDay = 1
                    newMonth++
                    
                    // Nach Namenlosen Tagen kommt Praios im neuen Jahr
                    if (newMonth > NAMELESS_DAYS_INDEX) {
                        newMonth = 0
                        newYear++
                    }
                }
            }
            
            return "$newDay ${months[newMonth]} $newYear $era"
            
        } catch (e: Exception) {
            // Bei Fehler aktuelles Datum zurückgeben
            e.printStackTrace() // Für Debugging
            return currentDate
        }
    }
    
    /**
     * Parst ein derisches Datum und gibt die Anzahl der Tage seit 1 Praios 1 BF zurück
     * Nützlich für Vergleiche
     */
    fun parseDateToDays(date: String): Int? {
        try {
            val dateParts = date.split(" ")
            if (dateParts.size < 3) return null
            
            val day = dateParts[0].toIntOrNull() ?: return null
            
            // Behandle "Namenlose Tage" als zwei Wörter
            if (dateParts.size >= 4 && dateParts[1] == "Namenlose" && dateParts[2] == "Tage") {
                val year = dateParts[3].toIntOrNull() ?: return null
                val monthIndex = months.indexOf("Namenlose Tage")
                if (monthIndex == -1) return null
                
                // Berechne Tage: Jahr * 365 + Tage der vorherigen Monate + aktueller Tag
                var totalDays = year * 365
                for (i in 0 until monthIndex) {
                    totalDays += getDaysInMonth(i)
                }
                return totalDays + day
            } else {
                val monthName = dateParts[1]
                val year = dateParts[2].toIntOrNull() ?: return null
                
                val monthIndex = months.indexOf(monthName)
                if (monthIndex == -1) return null
                
                // Berechne Tage: Jahr * 365 + Tage der vorherigen Monate + aktueller Tag
                var totalDays = year * 365
                for (i in 0 until monthIndex) {
                    totalDays += getDaysInMonth(i)
                }
                return totalDays + day
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Prüft, ob ein Trank abgelaufen ist
     */
    fun isExpired(expiryDate: String, currentDate: String): Boolean {
        val expiryDays = parseDateToDays(expiryDate)
        val currentDays = parseDateToDays(currentDate)
        
        return if (expiryDays != null && currentDays != null) {
            currentDays > expiryDays
        } else {
            false
        }
    }
    
    /**
     * Formatiert ein Datum (Validierung und Normalisierung)
     */
    fun formatDate(day: Int, month: String, year: Int, era: String = "BF"): String {
        val monthIndex = months.indexOf(month)
        if (monthIndex == -1) {
            return "1 Praios 1040 BF" // Fallback
        }
        
        val maxDay = getDaysInMonth(monthIndex)
        if (day < 1 || day > maxDay) {
            return "1 Praios 1040 BF" // Fallback
        }
        
        return "$day $month $year $era"
    }
    
    /**
     * Gibt die Liste aller derischen Monate zurück
     */
    fun getMonths(): List<String> = months
    
    /**
     * Berechnet den Wochentag für ein derisches Datum
     * Derische Woche hat 7 Tage (Windstag bis Wassertag)
     * 
     * @param date Datum im Format "Tag Monat Jahr BF"
     * @return Name des Wochentags
     */
    fun getWeekday(date: String): String {
        val totalDays = parseDateToDays(date) ?: return "Windstag"
        // Der erste Tag (1 Praios 0 BF) ist ein Praiostag
        // Sichere Modulo-Berechnung für negative Zahlen
        val weekdayIndex = ((totalDays + 2) % 7 + 7) % 7
        return weekdays[weekdayIndex]
    }
    
    /**
     * Mondphase als Enum
     */
    enum class MoonPhase(val symbol: String, val displayName: String) {
        NEW_MOON("🌑", "Neumond"),
        WAXING_CRESCENT("🌒", "Zunehmende Sichel"),
        FIRST_QUARTER("🌓", "Erstes Viertel"),
        WAXING_GIBBOUS("🌔", "Zunehmender Mond"),
        FULL_MOON("🌕", "Vollmond"),
        WANING_GIBBOUS("🌖", "Abnehmender Mond"),
        LAST_QUARTER("🌗", "Letztes Viertel"),
        WANING_CRESCENT("🌘", "Abnehmende Sichel")
    }
    
    /**
     * Berechnet die Mondphase von Mada
     * Mada hat einen 28-Tage-Zyklus
     * 
     * @param date Datum im Format "Tag Monat Jahr BF"
     * @return Mondphase von Mada
     */
    fun getMadaPhase(date: String): MoonPhase {
        val totalDays = parseDateToDays(date) ?: return MoonPhase.NEW_MOON
        // Sichere Modulo-Berechnung für negative Zahlen
        val dayInCycle = (totalDays % MADA_CYCLE + MADA_CYCLE) % MADA_CYCLE
        
        return when (dayInCycle) {
            in 0..3 -> MoonPhase.WANING_GIBBOUS
            in 4..8 -> MoonPhase.LAST_QUARTER
            in 9..12 -> MoonPhase.WANING_CRESCENT
            13 -> MoonPhase.NEW_MOON
            in 14..17 -> MoonPhase.WAXING_CRESCENT
            in 18..22 -> MoonPhase.FIRST_QUARTER
            in 23..26 -> MoonPhase.WAXING_GIBBOUS
            27 -> MoonPhase.FULL_MOON
            else -> MoonPhase.WANING_CRESCENT
        }
    }
}
