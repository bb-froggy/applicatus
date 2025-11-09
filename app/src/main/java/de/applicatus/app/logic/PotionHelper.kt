package de.applicatus.app.logic

import kotlin.random.Random

/**
 * Hilfsfunktionen für die Trankverwaltung
 */
object PotionHelper {
    
    // Derische Monate (12 Monate)
    private val derischeMonateNamen = listOf(
        "Praios", "Rondra", "Efferd", "Travia", "Boron", "Hesinde",
        "Firun", "Tsa", "Phex", "Peraine", "Ingerimm", "Rahja"
    )
    
    // Derische Monate mit Tagen (Jahr hat 365 Tage)
    private val derischeMonateTage = listOf(
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 25 // Rahja hat nur 25 Tage, plus 5 Namenlose Tage
    )
    
    // Unbegrenzt-Datum: 1. Praios 1500 BF
    const val UNLIMITED_DATE = "1. Praios 1500 BF"
    
    /**
     * Generiert ein zufälliges Trank-Aussehen
     */
    fun generateRandomAppearance(): String {
        val containers = listOf("Phiole", "Tiegel", "Tonfläschchen", "Glasflasche", "Keramikgefäß", "Lederbeutel")
        val colors = listOf("blauer", "roter", "grüner", "goldener", "silberner", "violetter", "brauner", "schwarzer", "weißer", "transparenter")
        val consistencies = listOf("Flüssigkeit", "Creme", "Salbe", "Paste", "Pulver", "Pillen", "Tinktur")
        val smells = listOf("stechendem Geruch", "süßem Duft", "herbem Aroma", "scharfem Geruch", "neutralem Geruch", "unangenehmen Gestank", "würzigem Duft")
        
        return when (Random.nextInt(3)) {
            0 -> "${containers.random()} mit ${colors.random()} ${consistencies.random()}"
            1 -> "${containers.random()} mit ${smells.random()}"
            else -> "${colors.random().replaceFirstChar { it.uppercaseChar() }} ${consistencies.random()} in ${containers.random()}"
        }
    }
    
    /**
     * Berechnet das Ablaufdatum basierend auf aktuellem Datum und Haltbarkeit
     * 
     * @param currentDate Aktuelles derisches Datum (z.B. "15. Praios 1040 BF")
     * @param shelfLife Haltbarkeit aus dem Rezept (z.B. "3 Monde", "1 Jahr", "6 Tage")
     * @return Berechnetes Ablaufdatum oder aktuelles Datum bei Fehler
     */
    fun calculateExpiryDate(currentDate: String, shelfLife: String): String {
        try {
            // Parse aktuelles Datum
            val (day, month, year) = parseDerischenDate(currentDate) ?: return currentDate
            
            // Parse Haltbarkeit
            val shelfLifeLower = shelfLife.lowercase().trim()
            
            val (daysToAdd, monthsToAdd, yearsToAdd) = when {
                "unbegrenzt" in shelfLifeLower || "ewig" in shelfLifeLower -> return UNLIMITED_DATE
                "tag" in shelfLifeLower -> {
                    val days = extractNumber(shelfLifeLower)
                    Triple(days, 0, 0)
                }
                "mond" in shelfLifeLower || "monat" in shelfLifeLower -> {
                    val months = extractNumber(shelfLifeLower)
                    Triple(0, months, 0)
                }
                "jahr" in shelfLifeLower -> {
                    val years = extractNumber(shelfLifeLower)
                    Triple(0, 0, years)
                }
                else -> Triple(0, 0, 0) // Keine gültige Haltbarkeit
            }
            
            // Berechne neues Datum
            var newDay = day + daysToAdd
            var newMonth = month + monthsToAdd
            var newYear = year + yearsToAdd
            
            // Übertrag Monate
            while (newMonth > 12) {
                newMonth -= 12
                newYear++
            }
            
            // Übertrag Tage
            while (newDay > getDaysInMonth(newMonth)) {
                newDay -= getDaysInMonth(newMonth)
                newMonth++
                if (newMonth > 12) {
                    newMonth = 1
                    newYear++
                }
            }
            
            return formatDerischenDate(newDay, newMonth, newYear)
            
        } catch (e: Exception) {
            return currentDate // Bei Fehler aktuelles Datum zurückgeben
        }
    }
    
    /**
     * Parst ein derisches Datum im Format "15. Praios 1040 BF"
     * @return Triple(Tag, Monat, Jahr) oder null bei Fehler
     */
    fun parseDerischenDate(dateString: String): Triple<Int, Int, Int>? {
        try {
            val parts = dateString.replace("BF", "").trim().split(" ")
            if (parts.size < 3) return null
            
            val day = parts[0].replace(".", "").toIntOrNull() ?: return null
            val monthName = parts[1]
            val year = parts[2].toIntOrNull() ?: return null
            
            val month = derischeMonateNamen.indexOf(monthName) + 1
            if (month < 1) return null
            
            return Triple(day, month, year)
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Formatiert ein derisches Datum
     */
    fun formatDerischenDate(day: Int, month: Int, year: Int): String {
        val monthName = if (month in 1..12) derischeMonateNamen[month - 1] else "Praios"
        return "$day. $monthName $year BF"
    }
    
    /**
     * Gibt die Anzahl der Tage in einem derischen Monat zurück
     */
    fun getDaysInMonth(month: Int): Int {
        return if (month in 1..12) derischeMonateTage[month - 1] else 30
    }
    
    /**
     * Extrahiert eine Zahl aus einem String (z.B. "3 Monde" -> 3)
     */
    private fun extractNumber(text: String): Int {
        val number = text.filter { it.isDigit() }
        return if (number.isNotEmpty()) number.toInt() else 1
    }
    
    /**
     * Validiert ein derisches Datum
     */
    fun isValidDerischenDate(day: Int, month: Int, year: Int): Boolean {
        if (month !in 1..12) return false
        if (year < 0) return false
        if (day < 1 || day > getDaysInMonth(month)) return false
        return true
    }
}
