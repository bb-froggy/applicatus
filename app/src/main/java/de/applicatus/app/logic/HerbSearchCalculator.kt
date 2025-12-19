package de.applicatus.app.logic

import de.applicatus.app.data.InitialHerbs
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.herb.Herb
import de.applicatus.app.data.model.herb.Landscape
import de.applicatus.app.logic.DerianDateCalculator.DerianMonth
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
     * @param hasDoubledSearchTime Hat der Charakter die Suchdauer verdoppelt? (-2 Bonus)
     * @param character Der Charakter (für Geländekunde)
     * @return Die Probenerschwernis (positiv = Erschwernis, negativ = Erleichterung)
     */
    fun calculateSearchDifficulty(
        herb: Herb,
        landscape: Landscape,
        hasOrtskenntnis: Boolean,
        hasDoubledSearchTime: Boolean,
        character: Character
    ): Int {
        var difficulty = herb.calculateSearchDifficulty(
            landscape = landscape,
            hasGelaendekunde = character.gelaendekunde.contains(landscape.displayName),
            hasOrtskenntnis = hasOrtskenntnis
        )
        
        // Verdoppelte Suchdauer gibt -2
        if (hasDoubledSearchTime) {
            difficulty -= 2
        }
        
        return difficulty
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
     * Datenklasse für das Ergebnis einer Kräutersuche
     */
    data class HerbSearchResult(
        val success: Boolean,
        val qualityPoints: Int,  // TaP* (Talentpunkte übrig, negativ bei Fehlschlag)
        val roll1: Int,
        val roll2: Int,
        val roll3: Int,
        val isSpectacular: Boolean = false,  // Doppel-1
        val isCatastrophic: Boolean = false, // Doppel-20
        val foundQuantity: String? = null    // Gefundene Menge (bei Erfolg)
    )
    
    /**
     * Führt eine Kräutersuche-Probe durch
     * 
     * Talentprobe auf MU/IN/FF mit Kräutersuche-TaW gegen die berechnete Erschwernis
     * 
     * @param character Der suchende Charakter
     * @param herb Das gesuchte Kraut
     * @param landscape Die Landschaft
     * @param hasOrtskenntnis Hat der Charakter Ortskenntnis?
     * @param hasDoubledSearchTime Hat der Charakter die Suchdauer verdoppelt?
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
        val taw = calculateHerbSearchTaW(character)
        val difficulty = calculateSearchDifficulty(
            herb, landscape, hasOrtskenntnis, hasDoubledSearchTime, character
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
        
        return HerbSearchResult(
            success = result.success,
            qualityPoints = result.qualityPoints,
            roll1 = result.rolls[0],
            roll2 = result.rolls[1],
            roll3 = result.rolls[2],
            isSpectacular = result.isDoubleOne || result.isTripleOne,
            isCatastrophic = result.isDoubleTwenty || result.isTripleTwenty,
            foundQuantity = if (result.success) herb.baseQuantity else null
        )
    }
}
