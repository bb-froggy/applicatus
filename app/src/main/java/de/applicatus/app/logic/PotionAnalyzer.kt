package de.applicatus.app.logic

import de.applicatus.app.data.model.potion.AnalysisMethod
import de.applicatus.app.data.model.potion.AnalysisStatus
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Recipe
import kotlin.random.Random

/**
 * Ergebnis einer Tranksanalyse
 */
data class AnalysisResult(
    val success: Boolean,           // War die Analyse erfolgreich?
    val tap: Int,                   // Talentpunkte* übrig
    val newAnalysisStatus: AnalysisStatus,  // Neuer Analysestatus des Tranks
    val recipeUnderstood: Boolean,  // Wurde das Rezept verstanden (19+ TaP*)?
    val rolls: List<Int>,           // Die drei Würfelwürfe
    val message: String             // Beschreibung des Ergebnisses
)

/**
 * Logik für die Analyse von Tränken
 */
object PotionAnalyzer {
    
    /**
     * Führt eine Tranksanalyse durch
     * 
     * @param character Der analysierende Charakter
     * @param recipe Das Rezept des Tranks
     * @param method Die gewählte Analysemethode
     * @param currentAnalysisStatus Aktueller Analysestatus des Tranks
     * @return AnalysisResult mit allen Details
     */
    fun analyzePotion(
        character: Character,
        recipe: Recipe,
        method: AnalysisMethod,
        currentAnalysisStatus: AnalysisStatus
    ): AnalysisResult {
        // Bestimme Talentwert basierend auf Methode
        val taw = when (method) {
            AnalysisMethod.BY_SIGHT -> character.alchemySkill
            AnalysisMethod.LABORATORY -> character.alchemySkill
            AnalysisMethod.ODEM_SPELL -> character.odemZfw
            AnalysisMethod.ANALYS_SPELL -> character.analysZfw
        }
        
        // Erschwerniss durch Analyse-Schwierigkeit des Rezepts
        val difficulty = recipe.analysisDifficulty
        
        // Eigenschaften für die Probe (vereinfacht: KL/IN/IN für Alchemie)
        val attributes = when (method) {
            AnalysisMethod.BY_SIGHT, AnalysisMethod.LABORATORY -> 
                listOf(character.kl, character.inValue, character.inValue)
            AnalysisMethod.ODEM_SPELL -> 
                listOf(character.kl, character.inValue, character.ch)
            AnalysisMethod.ANALYS_SPELL -> 
                listOf(character.kl, character.inValue, character.ch)
        }
        
        // Drei W20-Würfe
        val rolls = List(3) { rollD20() }
        
        // Prüfe auf besondere Würfe
        val ones = rolls.count { it == 1 }
        val twenties = rolls.count { it == 20 }
        
        // Berechne TaP*
        var tap = taw - difficulty
        
        // Bei besonderen Würfen
        if (ones >= 2) {
            // Doppel-1 oder Dreifach-1: Automatischer Erfolg mit max TaP*
            return AnalysisResult(
                success = true,
                tap = taw,
                newAnalysisStatus = if (taw >= 6) AnalysisStatus.PRECISE_ANALYZED else AnalysisStatus.ROUGH_ANALYZED,
                recipeUnderstood = tap >= 19,
                rolls = rolls,
                message = if (ones == 3) "Dreifach-1! Meisterhafte Analyse!" else "Doppel-1! Hervorragende Analyse!"
            )
        }
        
        if (twenties >= 2) {
            // Doppel-20 oder Dreifach-20: Automatischer Patzer
            return AnalysisResult(
                success = false,
                tap = tap,
                newAnalysisStatus = currentAnalysisStatus, // Bleibt unverändert
                recipeUnderstood = false,
                rolls = rolls,
                message = if (twenties == 3) "Dreifach-20! Katastrophaler Patzer!" else "Doppel-20! Patzer!"
            )
        }
        
        // Normale Probe: Abzüge für Überwürfe
        for (i in rolls.indices) {
            val roll = rolls[i]
            val attribute = attributes[i]
            if (roll > attribute) {
                tap -= (roll - attribute)
            }
        }
        
        // Erfolg wenn TaP* >= 0
        val success = tap >= 0
        
        // Bestimme neuen Analysestatus
        val newStatus = when {
            !success -> currentAnalysisStatus // Fehlgeschlagen, bleibt wie vorher
            tap >= 6 -> AnalysisStatus.PRECISE_ANALYZED // Genau analysiert bei 6+ TaP*
            else -> AnalysisStatus.ROUGH_ANALYZED // Grob analysiert
        }
        
        // Wurde das Rezept verstanden? (19+ TaP*)
        val recipeUnderstood = success && tap >= 19
        
        val message = when {
            !success -> "Analyse fehlgeschlagen (${tap} TaP*)"
            recipeUnderstood -> "Rezept verstanden! Genau analysiert mit ${tap} TaP*"
            tap >= 6 -> "Genau analysiert mit ${tap} TaP*"
            else -> "Grob analysiert mit ${tap} TaP*"
        }
        
        return AnalysisResult(
            success = success,
            tap = tap,
            newAnalysisStatus = newStatus,
            recipeUnderstood = recipeUnderstood,
            rolls = rolls,
            message = message
        )
    }
    
    /**
     * Würfelt einen W20
     */
    private fun rollD20(): Int = Random.nextInt(1, 21)
    
    /**
     * Formatiert das Analyseergebnis für die Anzeige
     */
    fun formatAnalysisResult(result: AnalysisResult, method: AnalysisMethod): String {
        val methodName = when (method) {
            AnalysisMethod.BY_SIGHT -> "Nach Augenschein"
            AnalysisMethod.LABORATORY -> "Laboranalyse"
            AnalysisMethod.ODEM_SPELL -> "Zauber Odem"
            AnalysisMethod.ANALYS_SPELL -> "Zauber Analys"
        }
        
        return buildString {
            appendLine("Methode: $methodName")
            appendLine("Würfe: ${result.rolls.joinToString(", ")}")
            appendLine("TaP*: ${result.tap}")
            appendLine()
            appendLine(result.message)
            
            if (result.recipeUnderstood) {
                appendLine()
                appendLine("🎓 Das Rezept wurde verstanden!")
            }
        }
    }
}
