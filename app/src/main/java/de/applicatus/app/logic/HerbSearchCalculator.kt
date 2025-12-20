package de.applicatus.app.logic

import de.applicatus.app.data.InitialHerbs
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.herb.Herb
import de.applicatus.app.data.model.herb.Landscape
import de.applicatus.app.data.model.herb.hasGelaendekundeIn
import de.applicatus.app.logic.DerianDateCalculator.DerianMonth
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Logik für die Kräutersuche in DSA 4.1
 * 
 * Basierend auf Zoo-Botanica Aventurica und den Regeln für Talentproben.
 * Die Kräutersuche verwendet den Talentwert "Kräutersuche", der aus
 * Sinnenschärfe, Wildnisleben und Pflanzenkunde berechnet wird.
 */
class HerbSearchCalculator {
    
    /**
     * Berechnet den effektiven Kräutersuche-Talentwert
     * 
     * TaW(Kräutersuche) = (Sinnenschärfe + Wildnisleben + Pflanzenkunde) / 3 (aufgerundet)
     * Aber: TaW darf nicht größer als das Doppelte des kleinsten Einzeltalents sein
     * 
     * @return Der effektive Kräutersuche-TaW
     */
    fun calculateHerbSearchTaW(character: Character): Int {
        val senses = character.sensoryAcuitySkill
        val wilderness = character.wildernessSkill
        val herbalLore = character.herbalLoreSkill
        
        // Durchschnitt berechnen (kaufmännisch runden)
        val average = ((senses + wilderness + herbalLore + 1) / 3)
        
        // TaW darf nicht mehr als das Doppelte des kleinsten Einzeltalents sein
        val minTalent = minOf(senses, wilderness, herbalLore)
        val maxAllowedTaw = minTalent * 2
        
        return minOf(average, maxAllowedTaw)
    }
    
    /**
     * Berechnet die Gesamterschwer

nis für die Kräutersuche
     * 
     * Erschwer

nis = Bestimmungsschwierigkeit + Häufigkeit - Geländekunde - Ortskenntnis
     * 
     * @param herb Das gesuchte Kraut
     * @param landscape Die Landschaft
     * @param hasOrtskenntnis Hat der Charakter Ortskenntnis? (+7 Bonus)
     * @param character Der Charakter (für Geländekunde)
     * @return Die Probenerschwernis (positiv = Erschwernis, negativ = Erleichterung)
     */
    fun calculateSearchDifficulty(
        herb: Herb,
        landscape: Landscape,
        hasOrtskenntnis: Boolean,
        character: Character
    ): Int {
        return herb.calculateSearchDifficulty(
            landscape = landscape,
            hasGelaendekunde = landscape.hasGelaendekundeIn(character.gelaendekunde),
            hasOrtskenntnis = hasOrtskenntnis
        )
    }
    
    /**
     * Filtert verfügbare Kräuter nach Region, Landschaft und Monat
     * 
     * @param regionHerbs Liste der Kräuternamen in der Region
     * @param landscape Die gewählte Landschaft
     * @param month Der Suchmonat
     * @return Liste der verfügbaren Kräuter
     */
    fun getAvailableHerbs(
        regionHerbs: List<String>,
        landscape: Landscape,
        month: DerianMonth
    ): List<Herb> {
        return regionHerbs.mapNotNull { herbName ->
            InitialHerbs.findHerbByName(herbName)
        }.filter { herb ->
            herb.isAvailableInLandscape(landscape) && herb.isAvailableInMonth(month)
        }.sortedBy { it.name }
    }
    
    /**
     * Datenklasse für einzelne Funde in einer allgemeinen Suche
     */
    data class GeneralSearchHerbResult(
        val herb: Herb,
        val harvestedItems: List<de.applicatus.app.logic.BaseQuantityParser.HerbHarvestItem>
    )
    
    /**
     * Datenklasse für das Ergebnis einer Kräutersuche
     */
    data class HerbSearchResult(
        val success: Boolean,
        val diceRolls: List<Int>, // Die drei Würfelwürfe
        val qualityPoints: Int,  // TaP* (Talentpunkte übrig, negativ bei Fehlschlag)
        val effectiveTaW: Int, // Effektiver TaW (mit Modifikatoren)
        val difficulty: Int, // Erschwernis (bei gezielter Suche)
        val foundHerb: Herb? = null, // Gefundenes Kraut (bei gezielter Suche)
        val foundQuantity: String? = null,   // Gefundene Menge (bei Erfolg)
        val harvestedItems: List<de.applicatus.app.logic.BaseQuantityParser.HerbHarvestItem> = emptyList(), // Tatsächlich gewürfelte Items
        val portionCount: Int = 1, // Anzahl gefundener Portionen (basierend auf TaP*)
        val searchDuration: String = "1 Stunde", // Suchdauer
        val generalSearchResults: List<GeneralSearchHerbResult>? = null // Mehrere Funde bei allgemeiner Suche
    ) {
        // Alte Kompatibilitäts-Properties
        val roll1: Int get() = diceRolls.getOrNull(0) ?: 0
        val roll2: Int get() = diceRolls.getOrNull(1) ?: 0
        val roll3: Int get() = diceRolls.getOrNull(2) ?: 0
        val isSpectacular: Boolean get() = (roll1 == 1 && roll2 == 1) || (roll2 == 1 && roll3 == 1) || (roll1 == 1 && roll3 == 1)
        val isCatastrophic: Boolean get() = (roll1 == 20 && roll2 == 20) || (roll2 == 20 && roll3 == 20) || (roll1 == 20 && roll3 == 20)
    }
    
    /**
     * Berechnet die Anzahl der gefundenen Portionen basierend auf TaP*
     * 
     * Jede weitere Portion kostet die halbe Erschwernis (mindestens 1) an TaP*
     * 
     * @param qualityPoints Die übrigen TaP* nach der Probe
     * @param difficulty Die Gesamterschwernis der Suche
     * @return Anzahl der Portionen (mindestens 1 bei Erfolg)
     */
    fun calculatePortionCount(qualityPoints: Int, difficulty: Int): Int {
        if (qualityPoints < 0) return 0 // Fehlschlag
        
        var portions = 1 // Erste Portion ist "gratis"
        var remainingTap = qualityPoints
        val costPerPortion = maxOf(1, difficulty / 2) // Halbe Erschwernis, mindestens 1
        
        while (remainingTap >= costPerPortion) {
            portions++
            remainingTap -= costPerPortion
        }
        
        return portions
    }
    
    /**
     * Führt eine Talentprobe durch (für allgemeine Suche ohne spezifisches Kraut)
     */
    fun performProbe(
        mu: Int,
        inValue: Int,
        ff: Int,
        taw: Int,
        difficulty: Int
    ): ProbeResult {
        return ProbeChecker.performThreeAttributeProbe(
            fertigkeitswert = taw,
            difficulty = difficulty,
            attribute1 = mu,
            attribute2 = inValue,
            attribute3 = ff,
            qualityPointName = "TaP*"
        )
    }
    
    /**
     * Führt eine Kräutersuche-Probe durch
     * 
     * Talentprobe auf MU/IN/FF mit Kräutersuche-TaW gegen die berechnete Erschwernis
     * 
     * Bei doppelter Suchdauer wird der TaW auf 1.5x erhöht (aufgerundet).
     * 
     * Bei hohen TaP* werden mehrere Portionen gefunden: Jede weitere Portion kostet
     * die halbe Erschwernis (mindestens 1) an TaP*.
     * 
     * @param character Der suchende Charakter
     * @param herb Das gesuchte Kraut
     * @param landscape Die Landschaft
     * @param hasOrtskenntnis Hat der Charakter Ortskenntnis?
     * @param hasDoubledSearchTime Hat der Charakter die Suchdauer verdoppelt? (TaW wird auf 1.5x erhöht)
     * @param providedRolls Optionale fixe Würfelwürfe [roll1, roll2, roll3] (für Tests)
     * @return Das Suchergebnis
     */
    fun performHerbSearch(
        character: Character,
        herb: Herb,
        landscape: Landscape,
        hasOrtskenntnis: Boolean,
        hasDoubledSearchTime: Boolean,
        providedRolls: List<Int>? = null
    ): HerbSearchResult {
        val baseTaw = calculateHerbSearchTaW(character)
        val taw = if (hasDoubledSearchTime) {
            // TaW auf 1.5x erhöhen (aufgerundet)
            ceil(baseTaw * 1.5).toInt()
        } else {
            baseTaw
        }
        val difficulty = calculateSearchDifficulty(
            herb, landscape, hasOrtskenntnis, character
        )
        
        // Eigenschaften: MU/IN/FF
        val mu = character.mu
        val inValue = character.inValue
        val ff = character.ff
        
        // Talentprobe durchführen (MU/IN/FF)
        val result = if (providedRolls != null && providedRolls.size == 3) {
            // Für Tests: Verwende fixe Würfe
            var rollIndex = 0
            ProbeChecker.performThreeAttributeProbe(
                fertigkeitswert = taw,
                difficulty = difficulty,
                attribute1 = mu,
                attribute2 = inValue,
                attribute3 = ff,
                diceRoll = { providedRolls[rollIndex++] },
                qualityPointName = "TaP*"
            )
        } else {
            // Normal: Zufällige Würfe
            ProbeChecker.performThreeAttributeProbe(
                fertigkeitswert = taw,
                difficulty = difficulty,
                attribute1 = mu,
                attribute2 = inValue,
                attribute3 = ff,
                qualityPointName = "TaP*"
            )
        }
        
        val portionCount = if (result.success) {
            calculatePortionCount(result.qualityPoints, difficulty)
        } else {
            0
        }
        
        val searchDuration = if (hasDoubledSearchTime) "2 Stunden" else "1 Stunde"
        
        return HerbSearchResult(
            success = result.success,
            diceRolls = result.rolls,
            qualityPoints = result.qualityPoints,
            effectiveTaW = taw,
            difficulty = difficulty,
            foundHerb = herb,
            foundQuantity = if (result.success) herb.baseQuantity else null,
            portionCount = portionCount,
            searchDuration = searchDuration
        )
    }
}
