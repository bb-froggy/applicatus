package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.*
import kotlin.random.Random

/**
 * Ergebnis einer Intensitätsbestimmung
 */
data class IntensityDeterminationResult(
    val success: Boolean,              // War die Bestimmung erfolgreich?
    val zfp: Int,                      // Zauberfertigkeit-Punkte* übrig
    val intensityQuality: IntensityQuality,  // Erkannte Intensität (schwach/stark)
    val rolls: List<Int>,              // Die drei Würfelwürfe
    val message: String                // Beschreibung des Ergebnisses
)

/**
 * Ergebnis einer einzelnen Strukturanalyse-Probe
 */
data class StructureAnalysisProbeResult(
    val success: Boolean,              // War die Probe erfolgreich?
    val tap: Int,                      // Talentpunkte*/ZfP* dieser Probe
    val effectiveTap: Int,             // Effektive TaP* (bei Augenschein halbiert)
    val selfControlSuccess: Boolean,   // War die Selbstbeherrschungsprobe erfolgreich?
    val selfControlRolls: List<Int>,   // Würfe der Selbstbeherrschungsprobe
    val canContinue: Boolean,          // Kann die Analyse fortgesetzt werden?
    val probeRolls: List<Int>,         // Die drei Würfelwürfe der Hauptprobe
    val message: String                // Beschreibung des Ergebnisses
)

/**
 * Ergebnis der finalen Strukturanalyse
 */
data class StructureAnalysisFinalResult(
    val totalTap: Int,                 // Gesamte akkumulierte TaP*
    val categoryKnown: Boolean,        // Kategorie bekannt (bei Erfolg)
    val knownQualityLevel: KnownQualityLevel,  // Wie genau ist die Qualität bekannt
    val intensityQuality: IntensityQuality,    // Intensitätsqualität (falls neu bestimmt)
    val refinedQuality: RefinedQuality,        // Verfeinerte Qualität
    val knownExactQuality: PotionQuality?,     // Genaue Qualität (ab 13 TaP*)
    val shelfLifeKnown: Boolean,       // Haltbarkeit bekannt (ab 8 TaP*)
    val recipeKnown: Boolean,          // Rezept bekannt (ab 19 TaP*)
    val newFacilitation: Int,          // Neue Erleichterung für zukünftige Analysen
    val potionConsumed: Boolean,       // Wurde der Trank bei der Analyse verbraucht?
    val message: String                // Beschreibung des Gesamtergebnisses
)

/**
 * Logik für Elixier-Analysen nach DSA-Regeln
 */
object ElixirAnalyzer {
    
    /**
     * Führt eine Intensitätsbestimmung durch (z.B. mit Odem)
     * 
     * @param character Der analysierende Charakter
     * @param recipe Das Rezept des Elixiers
     * @param actualQuality Die tatsächliche Qualität des Elixiers
     * @return IntensityDeterminationResult mit allen Details
     */
    fun determineIntensity(
        character: Character,
        recipe: Recipe,
        actualQuality: PotionQuality
    ): IntensityDeterminationResult {
        // Odem-Probe: KL/IN/CH
        val zfw = character.odemZfw
        val difficulty = recipe.analysisDifficulty
        
        val attributes = listOf(character.kl, character.inValue, character.ch)
        val rolls = List(3) { rollD20() }
        
        // Prüfe auf besondere Würfe
        val ones = rolls.count { it == 1 }
        val twenties = rolls.count { it == 20 }
        
        // Berechne ZfP*
        var zfp = zfw - difficulty
        
        // Bei besonderen Würfen
        if (ones >= 2) {
            // Doppel-1 oder Dreifach-1: Automatischer Erfolg mit max ZfP*
            val intensity = determineIntensityFromQuality(actualQuality, true)
            return IntensityDeterminationResult(
                success = true,
                zfp = zfw,
                intensityQuality = intensity,
                rolls = rolls,
                message = if (ones == 3) 
                    "Dreifach-1! Meisterhafte Intensitätsbestimmung! Intensität: ${intensityToString(intensity)}" 
                else 
                    "Doppel-1! Hervorragende Intensitätsbestimmung! Intensität: ${intensityToString(intensity)}"
            )
        }
        
        if (twenties >= 2) {
            // Doppel-20 oder Dreifach-20: Automatischer Patzer
            return IntensityDeterminationResult(
                success = false,
                zfp = zfp,
                intensityQuality = IntensityQuality.UNKNOWN,
                rolls = rolls,
                message = if (twenties == 3) 
                    "Dreifach-20! Katastrophaler Patzer!" 
                else 
                    "Doppel-20! Patzer!"
            )
        }
        
        // Normale Probe: Abzüge für Überwürfe
        for (i in rolls.indices) {
            val roll = rolls[i]
            val attribute = attributes[i]
            if (roll > attribute) {
                zfp -= (roll - attribute)
            }
        }
        
        // Erfolg wenn ZfP* >= 0
        val success = zfp >= 0
        
        if (!success) {
            return IntensityDeterminationResult(
                success = false,
                zfp = zfp,
                intensityQuality = IntensityQuality.UNKNOWN,
                rolls = rolls,
                message = "Intensitätsbestimmung fehlgeschlagen ($zfp ZfP*)"
            )
        }
        
        // Kann ab 3 ZfP* schwach von stark unterscheiden
        val canDifferentiate = zfp >= 3
        val intensity = if (canDifferentiate) {
            determineIntensityFromQuality(actualQuality, false)
        } else {
            IntensityQuality.UNKNOWN
        }
        
        val message = if (canDifferentiate) {
            "Intensitätsbestimmung erfolgreich mit $zfp ZfP*! Intensität: ${intensityToString(intensity)}"
        } else {
            "Intensitätsbestimmung erfolgreich mit $zfp ZfP*, aber zu schwach um die Intensität zu bestimmen (benötigt 3+ ZfP*)"
        }
        
        return IntensityDeterminationResult(
            success = true,
            zfp = zfp,
            intensityQuality = intensity,
            rolls = rolls,
            message = message
        )
    }
    
    /**
     * Bestimmt die Intensität basierend auf der tatsächlichen Qualität
     * Bei M wird zufällig schwach oder stark zurückgegeben
     */
    private fun determineIntensityFromQuality(quality: PotionQuality, alwaysDetermine: Boolean): IntensityQuality {
        return when (quality) {
            PotionQuality.A, PotionQuality.B, PotionQuality.C -> IntensityQuality.WEAK
            PotionQuality.D, PotionQuality.E, PotionQuality.F -> IntensityQuality.STRONG
            PotionQuality.M -> {
                // Bei M ist es Zufall
                if (Random.nextBoolean()) IntensityQuality.WEAK else IntensityQuality.STRONG
            }
        }
    }
    
    private fun intensityToString(intensity: IntensityQuality): String {
        return when (intensity) {
            IntensityQuality.WEAK -> "Schwach"
            IntensityQuality.STRONG -> "Stark"
            IntensityQuality.UNKNOWN -> "Unbekannt"
        }
    }
    
    /**
     * Führt eine einzelne Strukturanalyse-Probe durch
     * 
     * @param character Der analysierende Charakter
     * @param recipe Das Rezept des Elixiers
     * @param method Die gewählte Analysemethode
     * @param currentFacilitation Aktuelle Erleichterung aus vorherigen Analysen
     * @param probeNumber Die laufende Nummer dieser Probe (1, 2, 3, ...)
     * @param acceptHarderProbe Bei Laboranalyse: Um 3 erschweren statt Trank zu verbrauchen
     * @return StructureAnalysisProbeResult
     */
    fun performStructureAnalysisProbe(
        character: Character,
        recipe: Recipe,
        method: StructureAnalysisMethod,
        currentFacilitation: Int,
        probeNumber: Int,
        acceptHarderProbe: Boolean = false
    ): StructureAnalysisProbeResult {
        // Bestimme Talentwert/ZfW basierend auf Methode
        val baseTaw = when (method) {
            StructureAnalysisMethod.ANALYS_SPELL -> character.analysZfw
            StructureAnalysisMethod.BY_SIGHT -> character.alchemySkill
            StructureAnalysisMethod.LABORATORY -> character.alchemySkill
        }
        
        // Erschwernisse und Erleichterungen
        var difficulty = recipe.analysisDifficulty - currentFacilitation
        
        // Zusätzliche Erleichterungen basierend auf Methode
        when (method) {
            StructureAnalysisMethod.ANALYS_SPELL -> {
                // Je 3 TaP in Magiekunde über 7 → 1 Punkt Erleichterung
                val magickundeBonus = if (character.magicalLoreSkill > 7) {
                    (character.magicalLoreSkill - 7) / 3
                } else 0
                difficulty -= magickundeBonus
            }
            StructureAnalysisMethod.BY_SIGHT -> {
                // Je 3 TaP in Sinnenschärfe → 1 Punkt Erleichterung
                val sensoryBonus = character.sensoryAcuitySkill / 3
                difficulty -= sensoryBonus
            }
            StructureAnalysisMethod.LABORATORY -> {
                // Magiekunde oder Pflanzenkunde (höherer Wert)
                val loreSkill = maxOf(character.magicalLoreSkill, character.herbalLoreSkill)
                val loreBonus = if (loreSkill > 7) {
                    (loreSkill - 7) / 3
                } else 0
                difficulty -= loreBonus
                
                // Optional: +3 Erschwernis statt Trank zu verbrauchen
                if (acceptHarderProbe) {
                    difficulty += 3
                }
            }
        }
        
        // Eigenschaften für die Probe
        val attributes = when (method) {
            StructureAnalysisMethod.ANALYS_SPELL -> 
                listOf(character.kl, character.inValue, character.ch)
            StructureAnalysisMethod.BY_SIGHT, StructureAnalysisMethod.LABORATORY -> 
                listOf(character.kl, character.inValue, character.inValue)
        }
        
        // Drei W20-Würfe für die Hauptprobe
        val probeRolls = List(3) { rollD20() }
        
        // Prüfe auf besondere Würfe
        val ones = probeRolls.count { it == 1 }
        val twenties = probeRolls.count { it == 20 }
        
        // Berechne TaP*
        var tap = baseTaw - difficulty
        
        // Bei besonderen Würfen
        val success: Boolean
        if (ones >= 2) {
            // Doppel-1 oder Dreifach-1: Automatischer Erfolg mit max TaP*
            tap = baseTaw
            success = true
        } else if (twenties >= 2) {
            // Doppel-20 oder Dreifach-20: Automatischer Patzer
            success = false
        } else {
            // Normale Probe: Abzüge für Überwürfe
            for (i in probeRolls.indices) {
                val roll = probeRolls[i]
                val attribute = attributes[i]
                if (roll > attribute) {
                    tap -= (roll - attribute)
                }
            }
            success = tap >= 0
        }
        
        // Effektive TaP* (bei Augenschein nur die Hälfte, max 8)
        val effectiveTap = when (method) {
            StructureAnalysisMethod.BY_SIGHT -> minOf(tap / 2, 8)
            else -> tap
        }
        
        if (!success) {
            return StructureAnalysisProbeResult(
                success = false,
                tap = tap,
                effectiveTap = 0,
                selfControlSuccess = false,
                selfControlRolls = emptyList(),
                canContinue = false,
                probeRolls = probeRolls,
                message = "Strukturanalyse-Probe fehlgeschlagen ($tap TaP*)"
            )
        }
        
        // Selbstbeherrschungsprobe: Erschwernis ist n-1 (bei Probe n)
        val selfControlDifficulty = probeNumber - 1
        val selfControlResult = performSelfControlProbe(character, selfControlDifficulty)
        
        val message = buildString {
            if (ones >= 2) {
                append(if (ones == 3) "Dreifach-1! " else "Doppel-1! ")
            }
            append("Strukturanalyse-Probe erfolgreich mit $tap TaP*")
            if (method == StructureAnalysisMethod.BY_SIGHT) {
                append(" (effektiv $effectiveTap TaP* nach Halbierung)")
            }
            append(".\n")
            if (selfControlResult) {
                append("Selbstbeherrschung erfolgreich. Sie können weitere Proben ablegen.")
            } else {
                append("Selbstbeherrschung fehlgeschlagen. Die Analyse ist abgeschlossen.")
            }
        }
        
        return StructureAnalysisProbeResult(
            success = true,
            tap = tap,
            effectiveTap = effectiveTap,
            selfControlSuccess = selfControlResult,
            selfControlRolls = emptyList(),  // TODO: Würfe speichern wenn gewünscht
            canContinue = selfControlResult,
            probeRolls = probeRolls,
            message = message
        )
    }
    
    /**
     * Führt eine Selbstbeherrschungsprobe durch
     */
    private fun performSelfControlProbe(character: Character, difficulty: Int): Boolean {
        val taw = character.selfControlSkill
        val attributes = listOf(character.mu, character.mu, character.ko)
        val rolls = List(3) { rollD20() }
        
        var tap = taw - difficulty
        for (i in rolls.indices) {
            val roll = rolls[i]
            val attribute = attributes[i]
            if (roll > attribute) {
                tap -= (roll - attribute)
            }
        }
        
        return tap >= 0
    }
    
    /**
     * Berechnet das finale Ergebnis der Strukturanalyse
     */
    fun calculateFinalStructureAnalysisResult(
        totalAccumulatedTap: Int,
        actualQuality: PotionQuality,
        currentIntensity: IntensityQuality,
        isRecipeKnown: Boolean,
        method: StructureAnalysisMethod,
        acceptHarderProbe: Boolean
    ): StructureAnalysisFinalResult {
        val categoryKnown = totalAccumulatedTap >= 0
        
        // Bestimme KnownQualityLevel und andere Informationen
        val knownQualityLevel: KnownQualityLevel
        var refinedQuality = RefinedQuality.UNKNOWN
        var knownExactQuality: PotionQuality? = null
        
        when {
            totalAccumulatedTap >= 13 -> {
                // Genaue Qualität bekannt
                knownQualityLevel = KnownQualityLevel.EXACT
                knownExactQuality = actualQuality
            }
            totalAccumulatedTap >= 4 && currentIntensity != IntensityQuality.UNKNOWN -> {
                // Verfeinerte Qualität (sehr schwach / mittel / sehr stark)
                knownQualityLevel = KnownQualityLevel.VERY_WEAK_MEDIUM_OR_VERY_STRONG
                refinedQuality = determineRefinedQuality(actualQuality, currentIntensity)
            }
            totalAccumulatedTap >= 4 -> {
                // Qualität wie bei Intensitätsbestimmung
                knownQualityLevel = KnownQualityLevel.WEAK_OR_STRONG
            }
            else -> {
                knownQualityLevel = KnownQualityLevel.UNKNOWN
            }
        }
        
        val shelfLifeKnown = totalAccumulatedTap >= 8
        val recipeKnown = totalAccumulatedTap >= 19 || isRecipeKnown
        
        // Neue Erleichterung: Hälfte der TaP*
        val newFacilitation = totalAccumulatedTap / 2
        
        // Wurde der Trank verbraucht?
        val potionConsumed = method == StructureAnalysisMethod.LABORATORY && !acceptHarderProbe
        
        val message = buildString {
            appendLine("Strukturanalyse abgeschlossen mit $totalAccumulatedTap TaP*!")
            appendLine()
            if (categoryKnown) {
                appendLine("✓ Kategorie erkannt")
            }
            when (knownQualityLevel) {
                KnownQualityLevel.EXACT -> {
                    appendLine("✓ Genaue Qualität erkannt: ${qualityToString(actualQuality)}")
                }
                KnownQualityLevel.VERY_WEAK_MEDIUM_OR_VERY_STRONG -> {
                    appendLine("✓ Verfeinerte Qualität erkannt: ${refinedQualityToString(refinedQuality)}")
                }
                KnownQualityLevel.WEAK_OR_STRONG -> {
                    appendLine("✓ Grobe Qualität erkannt (schwach/stark)")
                }
                else -> {}
            }
            if (shelfLifeKnown) {
                appendLine("✓ Haltbarkeit erkannt")
            }
            if (recipeKnown && totalAccumulatedTap >= 19) {
                appendLine("🎓 Rezept wurde verstanden!")
            }
            if (potionConsumed) {
                appendLine()
                appendLine("⚠️ Der Trank wurde bei der Analyse verbraucht!")
            }
        }
        
        return StructureAnalysisFinalResult(
            totalTap = totalAccumulatedTap,
            categoryKnown = categoryKnown,
            knownQualityLevel = knownQualityLevel,
            intensityQuality = currentIntensity,
            refinedQuality = refinedQuality,
            knownExactQuality = knownExactQuality,
            shelfLifeKnown = shelfLifeKnown,
            recipeKnown = recipeKnown,
            newFacilitation = newFacilitation,
            potionConsumed = potionConsumed,
            message = message
        )
    }
    
    /**
     * Bestimmt die verfeinerte Qualität basierend auf tatsächlicher Qualität und Intensität
     */
    private fun determineRefinedQuality(
        actualQuality: PotionQuality,
        intensity: IntensityQuality
    ): RefinedQuality {
        return when (actualQuality) {
            PotionQuality.A, PotionQuality.B -> RefinedQuality.VERY_WEAK
            PotionQuality.C, PotionQuality.D -> RefinedQuality.MEDIUM
            PotionQuality.E, PotionQuality.F -> RefinedQuality.VERY_STRONG
            PotionQuality.M -> {
                // Bei M verwenden wir die Intensität als Hinweis
                when (intensity) {
                    IntensityQuality.WEAK -> RefinedQuality.VERY_WEAK
                    IntensityQuality.STRONG -> RefinedQuality.VERY_STRONG
                    else -> RefinedQuality.MEDIUM
                }
            }
        }
    }
    
    private fun qualityToString(quality: PotionQuality): String = quality.name
    
    private fun refinedQualityToString(quality: RefinedQuality): String {
        return when (quality) {
            RefinedQuality.VERY_WEAK -> "Sehr schwach (A oder B)"
            RefinedQuality.MEDIUM -> "Mittel (C oder D)"
            RefinedQuality.VERY_STRONG -> "Sehr stark (E oder F)"
            RefinedQuality.UNKNOWN -> "Unbekannt"
        }
    }
    
    /**
     * Würfelt einen W20
     */
    private fun rollD20(): Int = Random.nextInt(1, 21)
}
