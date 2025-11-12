package de.applicatus.app.logic

import kotlin.random.Random

/**
 * Hilfsfunktionen für die Trankverwaltung
 * 
 * Hinweise:
 * - Für Datums-Berechnungen: Verwenden Sie DerianDateCalculator
 * - Für Ablaufdaten mit Würfelunterstützung: DerianDateCalculator.calculateExpiryDate()
 * - Für Datums-Parsing und Formatierung: DerianDateCalculator.parseDerischenDate() / formatDerischenDate()
 */
object PotionHelper {
    
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
}
