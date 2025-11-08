package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.*
import de.applicatus.app.data.model.spell.SystemSpell
import de.applicatus.app.data.model.talent.Talent
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
    val message: String,               // Beschreibung des Ergebnisses
    val fertigkeitswert: Int,          // Talentwert/ZfW
    val difficulty: Int,               // Erschwernis/Erleichterung
    val attributes: Triple<Pair<String, Int>, Pair<String, Int>, Pair<String, Int>>  // Eigenschaftsnamen und -werte
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
 * 
 * Verwendet die zentrale ProbeChecker-Klasse für Proben-Durchführung.
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
        // ODEM ARCANUM: Probe auf KL/IN/IN
        val probeResult = ProbeChecker.performSystemSpellProbe(
            systemSpell = SystemSpell.ODEM,
            character = character,
            zauberfertigkeit = character.odemZfw,
            difficulty = recipe.analysisDifficulty
        )
        
        if (!probeResult.success) {
            return IntensityDeterminationResult(
                success = false,
                zfp = probeResult.qualityPoints,
                intensityQuality = IntensityQuality.UNKNOWN,
                rolls = probeResult.rolls,
                message = "Intensitätsbestimmung fehlgeschlagen (${probeResult.qualityPoints} ZfP*)"
            )
        }
        
        // Bei Doppel-1 oder Dreifach-1: Perfekter Erfolg
        if (probeResult.isDoubleOne || probeResult.isTripleOne) {
            val intensity = determineIntensityFromQuality(actualQuality, true)
            return IntensityDeterminationResult(
                success = true,
                zfp = probeResult.qualityPoints,
                intensityQuality = intensity,
                rolls = probeResult.rolls,
                message = if (probeResult.isTripleOne) 
                    "Dreifach-1! Meisterhafte Intensitätsbestimmung! Intensität: ${intensityToString(intensity)}" 
                else 
                    "Doppel-1! Hervorragende Intensitätsbestimmung! Intensität: ${intensityToString(intensity)}"
            )
        }
        
        // Kann ab 3 ZfP* schwach von stark unterscheiden
        val canDifferentiate = probeResult.qualityPoints >= 3
        val intensity = if (canDifferentiate) {
            determineIntensityFromQuality(actualQuality, false)
        } else {
            IntensityQuality.UNKNOWN
        }
        
        val message = if (canDifferentiate) {
            "Intensitätsbestimmung erfolgreich mit ${probeResult.qualityPoints} ZfP*! Intensität: ${intensityToString(intensity)}"
        } else {
            "Intensitätsbestimmung erfolgreich mit ${probeResult.qualityPoints} ZfP*, aber zu schwach um die Intensität zu bestimmen (benötigt 3+ ZfP*)"
        }
        
        return IntensityDeterminationResult(
            success = true,
            zfp = probeResult.qualityPoints,
            intensityQuality = intensity,
            rolls = probeResult.rolls,
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
        
        // Führe Probe durch - je nach Methode unterschiedlich
        val probeResult = when (method) {
            StructureAnalysisMethod.ANALYS_SPELL -> {
                // ANALYS ARKANSTRUKTUR: Probe auf KL/KL/IN
                ProbeChecker.performSystemSpellProbe(
                    systemSpell = SystemSpell.ANALYS,
                    character = character,
                    zauberfertigkeit = baseTaw,
                    difficulty = difficulty
                )
            }
            StructureAnalysisMethod.BY_SIGHT, StructureAnalysisMethod.LABORATORY -> {
                // Alchimie: Probe auf MU/KL/FF
                ProbeChecker.performTalentProbe(
                    talent = Talent.ALCHEMY,
                    character = character,
                    talentwert = baseTaw,
                    difficulty = difficulty
                )
            }
        }
        
        // Eigenschaftsnamen für die Anzeige
        val attributeNames = when (method) {
            StructureAnalysisMethod.ANALYS_SPELL -> 
                Triple(
                    Pair("KL", character.kl),
                    Pair("KL", character.kl),
                    Pair("IN", character.inValue)
                )
            StructureAnalysisMethod.BY_SIGHT, StructureAnalysisMethod.LABORATORY -> 
                Triple(
                    Pair("MU", character.mu),
                    Pair("KL", character.kl),
                    Pair("FF", character.ff)
                )
        }
        
        if (!probeResult.success) {
            return StructureAnalysisProbeResult(
                success = false,
                tap = probeResult.qualityPoints,
                effectiveTap = 0,
                selfControlSuccess = false,
                selfControlRolls = emptyList(),
                canContinue = false,
                probeRolls = probeResult.rolls,
                message = "Strukturanalyse-Probe fehlgeschlagen (${probeResult.qualityPoints} TaP*)",
                fertigkeitswert = baseTaw,
                difficulty = difficulty,
                attributes = attributeNames
            )
        }
        
        // Effektive TaP* (bei Augenschein nur die Hälfte, max 8)
        val effectiveTap = when (method) {
            StructureAnalysisMethod.BY_SIGHT -> minOf(probeResult.qualityPoints / 2, 8)
            else -> probeResult.qualityPoints
        }
        
        // Selbstbeherrschungsprobe: Erschwernis ist n-1 (bei Probe n)
        val selfControlDifficulty = probeNumber - 1
        val selfControlResult = performSelfControlProbe(character, selfControlDifficulty)
        
        val message = buildString {
            if (probeResult.isTripleOne) {
                append("Dreifach-1! ")
            } else if (probeResult.isDoubleOne) {
                append("Doppel-1! ")
            }
            append("Strukturanalyse-Probe erfolgreich mit ${probeResult.qualityPoints} TaP*")
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
            tap = probeResult.qualityPoints,
            effectiveTap = effectiveTap,
            selfControlSuccess = selfControlResult,
            selfControlRolls = emptyList(),  // TODO: Würfe speichern wenn gewünscht
            canContinue = selfControlResult,
            probeRolls = probeResult.rolls,
            message = message,
            fertigkeitswert = baseTaw,
            difficulty = difficulty,
            attributes = attributeNames
        )
    }
    
    /**
     * Führt eine Selbstbeherrschungsprobe durch
     * Selbstbeherrschung: MU/MU/KO
     */
    private fun performSelfControlProbe(character: Character, difficulty: Int): Boolean {
        val probeResult = ProbeChecker.performTalentProbe(
            talent = Talent.SELF_CONTROL,
            character = character,
            talentwert = character.selfControlSkill,
            difficulty = difficulty
        )
        return probeResult.success
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
        // Kategorie ist nur bekannt, wenn mindestens 1 TaP* akkumuliert wurde
        val categoryKnown = totalAccumulatedTap >= 1
        
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
            if (totalAccumulatedTap == 0) {
                appendLine("❌ Keine Informationen über den Trank erhalten!")
            } else {
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
}
