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
    
    /**
     * Derische Monate für die Erntezeit-Berechnung
     */
    enum class DerianMonth(val displayName: String) {
        PRAIOS("Praios"),
        RONDRA("Rondra"),
        EFFERD("Efferd"),
        TRAVIA("Travia"),
        BORON("Boron"),
        HESINDE("Hesinde"),
        FIRUN("Firun"),
        TSA("Tsa"),
        PHEX("Phex"),
        PERAINE("Peraine"),
        INGERIMM("Ingerimm"),
        RAHJA("Rahja"),
        NAMELESS_DAYS("Namenlose Tage"),
        FULL_YEAR("Ganzjährig");  // Für Suchen ohne Monats-Einschränkung
        
        companion object {
            fun fromMonthName(monthName: String): DerianMonth? {
                return values().find { it.displayName.equals(monthName, ignoreCase = true) }
            }
        }
    }
    
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
    
    // Unbegrenzt-Datum: 1. Praios 1500 BF (>100 Jahre in der Zukunft)
    const val UNLIMITED_DATE = "1 Praios 1500 BF"
    
    /**
     * Gibt die Anzahl der Tage für einen Monat zurück
     */
    fun getDaysInMonth(monthIndex: Int): Int {
        return if (monthIndex == NAMELESS_DAYS_INDEX) 5 else 30
    }
    
    /**
     * Extrahiert die Zeiteinheit aus einer Haltbarkeitsangabe
     * 
     * @param shelfLife Haltbarkeit (z.B. "3 Monde", "3W6+2 Wochen", "unbegrenzt")
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
     * @param shelfLife Haltbarkeit (z.B. "3 Monde", "3W6+2 Wochen", "unbegrenzt", "einige Jahre")
     * @return Berechnete Anzahl oder null bei Fehler oder "unbegrenzt"
     */
    fun parseShelfLifeAmount(shelfLife: String): Int? {
        val shelfLifeLower = shelfLife.trim().lowercase()
        
        // Prüfe auf "unbegrenzt" oder "ewig" oder "nahezu unbegrenzt"
        if ("unbegrenzt" in shelfLifeLower || "ewig" in shelfLifeLower || "nahezu unbegrenzt" in shelfLifeLower) {
            return null // Signal für unbegrenzte Haltbarkeit
        }
        
        // Spezielle Fälle: "einige Jahre", "mehrere Jahre"
        if ("einige jahre" in shelfLifeLower) {
            return 3 // ca. 3 Jahre
        }
        if ("mehrere jahre" in shelfLifeLower) {
            return 5 // ca. 5 Jahre
        }
        
        val parts = shelfLife.trim().split(" ")
        if (parts.isEmpty()) return null
        
        // Entferne "etwa" am Anfang falls vorhanden
        val cleanedParts = if (parts.size > 1 && parts[0].lowercase() == "etwa") {
            parts.drop(1)
        } else {
            parts
        }
        
        if (cleanedParts.isEmpty()) return null
        
        val amountPart = if (cleanedParts.size > 1) {
            // Bei mehreren Teilen ist der erste Teil die Menge
            cleanedParts.dropLast(1).joinToString(" ")
        } else {
            return null
        }
        
        // Prüfe auf Würfelnotation (mit oder ohne führende Zahl vor W)
        // Beispiele: "3W6+2", "W6+2" (wird zu "1W6+2"), "2W20"
        val diceRegex = Regex("""(\d*)W(\d+)([+\-]\d+)?""", RegexOption.IGNORE_CASE)
        val diceMatch = diceRegex.matchEntire(amountPart)
        
        return if (diceMatch != null) {
            // Wenn keine Zahl vor W steht, nehme 1 an
            val normalizedDice = if (diceMatch.groupValues[1].isEmpty()) {
                "1" + amountPart
            } else {
                amountPart
            }
            ProbeChecker.rollDice(normalizedDice)
        } else {
            amountPart.toIntOrNull()
        }
    }
    
    /**
     * Berechnet das Haltbarkeitsdatum eines Tranks
     * 
     * @param currentDate Aktuelles Datum im Format "Tag Monat Jahr BF" (z.B. "15 Praios 1040 BF")
     * @param shelfLife Haltbarkeit (z.B. "3 Monde", "1 Jahr", "2 Wochen", "3W6+2 Wochen", "unbegrenzt")
     * @return Haltbarkeitsdatum im gleichen Format (oder UNLIMITED_DATE bei unbegrenzter Haltbarkeit)
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
            
            // Parse Haltbarkeit (unterstützt Würfelnotationen und "unbegrenzt")
            val amount = parseShelfLifeAmount(shelfLife)
            
            // Wenn null zurückgegeben wird, ist es entweder "unbegrenzt" oder ein Fehler
            if (amount == null) {
                val shelfLifeLower = shelfLife.trim().lowercase()
                if ("unbegrenzt" in shelfLifeLower || "ewig" in shelfLifeLower || "nahezu unbegrenzt" in shelfLifeLower) {
                    return UNLIMITED_DATE
                }
                return currentDate // Fehler beim Parsen
            }
            
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
     * Parst ein derisches Datum im Format "15. Praios 1040 BF" (mit Punkt nach Tag)
     * 
     * Hinweis: Dies ist ein alternatives Format zu "15 Praios 1040 BF" (ohne Punkt).
     * Diese Funktion wird hauptsächlich für die UI-Eingabe verwendet.
     * 
     * @param dateString Datum im Format "15. Praios 1040 BF"
     * @return Triple(Tag, Monat-Index 1-12, Jahr) oder null bei Fehler
     */
    fun parseDerischenDate(dateString: String): Triple<Int, Int, Int>? {
        try {
            val parts = dateString.replace("BF", "").trim().split(" ")
            if (parts.size < 3) return null
            
            val day = parts[0].replace(".", "").toIntOrNull() ?: return null
            val monthName = parts[1]
            val year = parts[2].toIntOrNull() ?: return null
            
            // Finde Monat-Index (1-12, nicht 0-11!)
            val monthIndex = months.indexOf(monthName)
            if (monthIndex == -1) return null
            
            val month = monthIndex + 1 // Konvertiere zu 1-basiertem Index
            
            return Triple(day, month, year)
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Formatiert ein derisches Datum im Format "15. Praios 1040 BF" (mit Punkt nach Tag)
     * 
     * @param day Tag (1-30 für normale Monate, 1-5 für Namenlose Tage)
     * @param month Monat-Index (1-12)
     * @param year Jahr
     * @return Formatiertes Datum
     */
    fun formatDerischenDate(day: Int, month: Int, year: Int): String {
        val monthName = if (month in 1..12) months[month - 1] else "Praios"
        return "$day. $monthName $year BF"
    }
   
    /**
     * Validiert ein derisches Datum (für UI-Eingabe)
     * 
     * @param day Tag
     * @param month Monat-Index (1-12)
     * @param year Jahr
     * @return true wenn gültig
     */
    fun isValidDerischenDate(day: Int, month: Int, year: Int): Boolean {
        if (month !in 1..12) return false
        if (year < 0) return false
        val maxDays = getDaysInMonth(month)
        if (day < 1 || day > maxDays) return false
        return true
    }
    
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
    
    /**
     * Berechnet das Ablaufdatum für einen gespeicherten Zauber
     * 
     * @param currentDate Aktuelles Datum im Format "Tag Monat Jahr BF"
     * @param slotType Typ des Zauberslots (SPELL_STORAGE für Zauberstab, APPLICATUS für Applicatus)
     * @param applicatusDuration Wirkungsdauer des Applicatus (nur relevant wenn slotType = APPLICATUS)
     * @return Ablaufdatum im Format "Tag Monat Jahr BF" oder UNLIMITED_DATE
     */
    fun calculateSpellExpiry(
        currentDate: String,
        slotType: de.applicatus.app.data.model.spell.SlotType,
        applicatusDuration: de.applicatus.app.data.model.spell.ApplicatusDuration = de.applicatus.app.data.model.spell.ApplicatusDuration.DAY
    ): String {
        // Für SPELL_STORAGE: nächste Sommersonnenwende (1. Praios)
        if (slotType == de.applicatus.app.data.model.spell.SlotType.SPELL_STORAGE) {
            return calculateNextSummerSolstice(currentDate)
        }
        
        // Für APPLICATUS: je nach gewählter Wirkungsdauer
        return when (applicatusDuration) {
            de.applicatus.app.data.model.spell.ApplicatusDuration.DAY -> {
                // Bis zum nächsten Tag (Mitternacht)
                calculateExpiryDate(currentDate, "1 Tage")
            }
            de.applicatus.app.data.model.spell.ApplicatusDuration.MOON -> {
                // Bis zum Ende des aktuellen Mondes (Vollmond -> Neumond Übergang)
                calculateNextMoonEnd(currentDate)
            }
            de.applicatus.app.data.model.spell.ApplicatusDuration.QUARTER -> {
                // Bis zum Ende des aktuellen Quartals (alle 3 Monate)
                calculateNextQuarterEnd(currentDate)
            }
            de.applicatus.app.data.model.spell.ApplicatusDuration.WINTER_SOLSTICE -> {
                // Bis zur nächsten Wintersonnenwende (1. Firun)
                calculateNextWinterSolstice(currentDate)
            }
        }
    }
    
    /**
     * Prüft, ob ein Zauber abgelaufen ist
     * 
     * @param expiryDate Ablaufdatum im Format "Tag Monat Jahr BF"
     * @param currentDate Aktuelles Datum im Format "Tag Monat Jahr BF"
     * @return true wenn der Zauber abgelaufen ist
     */
    fun isSpellExpired(expiryDate: String, currentDate: String): Boolean {
        // "unbegrenzt" läuft nie ab
        if (expiryDate == UNLIMITED_DATE) return false
        
        val expiryDays = parseDateToDays(expiryDate)
        val currentDays = parseDateToDays(currentDate)
        return if (expiryDays != null && currentDays != null) {
            currentDays >= expiryDays
        } else {
            false
        }
    }
    
    /**
     * Hilfsfunktion: Parst ein Datum und gibt (Tag, Monat, Jahr, Ära) zurück
     */
    private fun parseDateComponents(date: String): Quadruple<Int, String, Int, String>? {
        try {
            val dateParts = date.split(" ")
            
            // Behandle "Namenlose Tage" als zwei Wörter
            if (dateParts.size >= 5 && dateParts[1] == "Namenlose" && dateParts[2] == "Tage") {
                val day = dateParts[0].toIntOrNull() ?: return null
                val month = "Namenlose Tage"
                val year = dateParts[3].toIntOrNull() ?: return null
                val era = dateParts.getOrNull(4) ?: "BF"
                return Quadruple(day, month, year, era)
            } else if (dateParts.size >= 4) {
                val day = dateParts[0].toIntOrNull() ?: return null
                val month = dateParts[1]
                val year = dateParts[2].toIntOrNull() ?: return null
                val era = dateParts.getOrNull(3) ?: "BF"
                return Quadruple(day, month, year, era)
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Extrahiert den Monat aus einem derischen Datum
     * 
     * @param date Das derische Datum (z.B. "15 Praios 1040 BF")
     * @return Der DerianMonth oder FULL_YEAR bei Fehler
     */
    fun getMonthFromDate(date: String): DerianMonth {
        val components = parseDateComponents(date) ?: return DerianMonth.FULL_YEAR
        return DerianMonth.fromMonthName(components.second) ?: DerianMonth.FULL_YEAR
    }
    
    /**
     * Hilfsklasse für Quadrupel
     */
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    
    /**
     * Berechnet die nächste Sommersonnenwende (1. Praios)
     * Wird für SPELL_STORAGE (Zauberstab) verwendet
     */
    private fun calculateNextSummerSolstice(currentDate: String): String {
        val parsed = parseDateComponents(currentDate) ?: return currentDate
        val (day, month, year, era) = parsed
        
        val targetYear = year + 1
        
        return "1 Praios $targetYear $era"
    }
    
    /**
     * Berechnet die nächste Wintersonnenwende (1. Firun)
     * Wird für APPLICATUS mit WINTER_SOLSTICE Wirkungsdauer und Zauberzeichen verwendet
     */
    fun getNextWinterSolstice(currentDate: String): String {
        val parsed = parseDateComponents(currentDate) ?: return currentDate
        val (day, month, year, era) = parsed
        
        val currentMonthIndex = months.indexOf(month)
        val firunIndex = months.indexOf("Firun") // Index 6
        
        // Wenn wir bereits im oder nach Firun sind, dann nächstes Jahr
        val targetYear = if (currentMonthIndex >= firunIndex) {
            year + 1
        } else {
            year
        }
        
        return "1 Firun $targetYear $era"
    }
    
    /**
     * Berechnet die nächste Wintersonnenwende (1. Firun)
     * Wird für APPLICATUS mit WINTER_SOLSTICE Wirkungsdauer verwendet
     * @deprecated Use getNextWinterSolstice instead
     */
    private fun calculateNextWinterSolstice(currentDate: String): String {
        return getNextWinterSolstice(currentDate)
    }
    
    /**
     * Berechnet das Ende des aktuellen Mondes (28-Tage-Zyklus)
     * Nächster Neumond nach aktuellem Datum
     */
    private fun calculateNextMoonEnd(currentDate: String): String {
        val totalDays = parseDateToDays(currentDate) ?: return currentDate
        val dayInCycle = (totalDays % MADA_CYCLE + MADA_CYCLE) % MADA_CYCLE
        
        // Neumond ist an Tag 13 des Zyklus
        val daysUntilNewMoon = if (dayInCycle <= 13) {
            13 - dayInCycle
        } else {
            MADA_CYCLE - dayInCycle + 13
        }
        
        return calculateExpiryDate(currentDate, "$daysUntilNewMoon Tage")
    }
    
    /**
     * Berechnet das Ende des aktuellen Quartals
     * Quartale: Praios-Efferd (1-3), Travia-Hesinde (4-6), Firun-Phex (7-9), Peraine-Namenlose (10-13)
     */
    private fun calculateNextQuarterEnd(currentDate: String): String {
        val parsed = parseDateComponents(currentDate) ?: return currentDate
        val (_, month, year, era) = parsed
        
        val currentMonthIndex = months.indexOf(month)
        
        // Bestimme das Quartals-Ende
        val (endMonth, endYear) = when (currentMonthIndex) {
            in 0..2 -> "Efferd" to year        // Quartal 1: bis Ende Efferd
            in 3..5 -> "Hesinde" to year       // Quartal 2: bis Ende Hesinde
            in 6..8 -> "Phex" to year          // Quartal 3: bis Ende Phex
            else -> "Namenlose Tage" to year   // Quartal 4: bis Ende Namenlose Tage
        }
        
        val endMonthIndex = months.indexOf(endMonth)
        val maxDay = getDaysInMonth(endMonthIndex)
        
        return "$maxDay $endMonth $endYear $era"
    }
}
