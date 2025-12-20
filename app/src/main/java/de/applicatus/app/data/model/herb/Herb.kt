package de.applicatus.app.data.model.herb

import de.applicatus.app.logic.DerianDateCalculator
import de.applicatus.app.logic.DerianDateCalculator.DerianMonth

/**
 * Repräsentiert die Verbreitung einer Pflanze in einer spezifischen Landschaft
 * 
 * @param landscape Die Landschaft
 * @param occurrence Die Häufigkeit des Vorkommens in dieser Landschaft
 */
data class HerbDistribution(
    val landscape: Landscape,
    val occurrence: Occurrence
)

/**
 * Datenmodell für ein Kraut/eine Pflanze aus Zoo-Botanica Aventurica
 * 
 * @param name Name der Pflanze
 * @param identificationDifficulty Grundschwierigkeit für die Bestimmungsprobe
 * @param baseQuantity Gefundene Grundmenge bei erfolgreichem Fund
 * @param distributions Verbreitung in verschiedenen Landschaften
 * @param harvestMonths Monate, in denen die Pflanze geerntet werden kann
 * @param pageReference Seitenreferenz in Zoo-Botanica Aventurica
 * @param dangers Optionale Gefahren bei der Suche/Ernte
 */
data class Herb(
    val name: String,
    val identificationDifficulty: Int,
    val baseQuantity: String,
    val distributions: List<HerbDistribution>,
    val harvestMonths: List<DerianMonth>,
    val pageReference: Int,
    val dangers: String? = null
) {
    /**
     * Prüft, ob die Pflanze in der angegebenen Landschaft vorkommt
     */
    fun isAvailableInLandscape(landscape: Landscape): Boolean {
        return distributions.any { it.landscape == landscape }
    }

    /**
     * Prüft, ob die Pflanze im angegebenen Monat geerntet werden kann
     */
    fun isAvailableInMonth(month: DerianMonth): Boolean {
        return month == DerianMonth.FULL_YEAR || harvestMonths.contains(month)
    }

    /**
     * Gibt die Häufigkeit in einer Landschaft zurück
     */
    fun getOccurrenceInLandscape(landscape: Landscape): Occurrence? {
        return distributions.find { it.landscape == landscape }?.occurrence
    }

    /**
     * Berechnet die Gesamtschwierigkeit für die Kräutersuche
     * 
     * @param landscape Die Landschaft
     * @param hasGelaendekunde Hat der Charakter Geländekunde? (+3 Bonus)
     * @param hasOrtskenntnis Hat der Charakter Ortskenntnis? (+7 Bonus)
     * @return Die Probenerschwernis (negativ = Erleichterung)
     */
    fun calculateSearchDifficulty(
        landscape: Landscape,
        hasGelaendekunde: Boolean,
        hasOrtskenntnis: Boolean
    ): Int {
        val occurrence = getOccurrenceInLandscape(landscape) ?: return 999
        
        var difficulty = identificationDifficulty + occurrence.modifier
        
        if (hasGelaendekunde) difficulty -= 3
        if (hasOrtskenntnis) difficulty -= 7
        
        return difficulty
    }
}
