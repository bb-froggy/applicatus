package de.applicatus.app.logic

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
    
    /**
     * Gibt die Anzahl der Tage für einen Monat zurück
     */
    private fun getDaysInMonth(monthIndex: Int): Int {
        return if (monthIndex == NAMELESS_DAYS_INDEX) 5 else 30
    }
    
    /**
     * Berechnet das Haltbarkeitsdatum eines Tranks
     * 
     * @param currentDate Aktuelles Datum im Format "Tag Monat Jahr BF" (z.B. "15 Praios 1040 BF")
     * @param shelfLife Haltbarkeit (z.B. "3 Monde", "1 Jahr", "2 Wochen")
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
            
            // Parse Haltbarkeit
            val shelfLifeParts = shelfLife.trim().split(" ")
            if (shelfLifeParts.size < 2) return currentDate
            
            val amount = shelfLifeParts[0].toIntOrNull() ?: return currentDate
            val unit = shelfLifeParts[1].lowercase()
            
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
        // Der erste Tag (1 Praios 1 BF) ist ein Windstag
        val weekdayIndex = (totalDays - 1) % 7
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
        val dayInCycle = totalDays % MADA_CYCLE
        
        return when (dayInCycle) {
            0 -> MoonPhase.NEW_MOON
            in 1..3 -> MoonPhase.WAXING_CRESCENT
            in 4..6 -> MoonPhase.FIRST_QUARTER
            in 7..10 -> MoonPhase.WAXING_GIBBOUS
            in 11..14 -> MoonPhase.FULL_MOON
            in 15..17 -> MoonPhase.WANING_GIBBOUS
            in 18..20 -> MoonPhase.LAST_QUARTER
            else -> MoonPhase.WANING_CRESCENT
        }
    }
}
