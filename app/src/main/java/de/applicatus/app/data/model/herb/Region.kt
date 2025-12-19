package de.applicatus.app.data.model.herb

/**
 * Regionen Aventuriens mit ihren Landschaften und verfügbaren Kräutern
 * 
 * @param name Name der Region
 * @param landscapes Verfügbare Landschaftstypen in dieser Region
 * @param herbs Namen der in dieser Region vorkommenden Kräuter
 */
data class Region(
    val name: String,
    val landscapes: List<Landscape>,
    val herbs: List<String>  // Pflanzennamen, die in dieser Region vorkommen
)
