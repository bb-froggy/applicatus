package de.applicatus.app.data.model.herb

/**
 * Landschaftstypen aus Zoo-Botanica Aventurica
 * 
 * Jede Region hat verschiedene Landschaftstypen, in denen verschiedene Kräuter vorkommen
 */
enum class Landscape(val displayName: String) {
    ICE("Eis"),
    DESERT("Wüste und Wüstenrand"),
    MOUNTAINS("Gebirge"),
    HIGHLANDS("Hochland"),
    STEPPE("Steppe"),
    GRASSLANDS("Grasland, Wiesen"),
    RIVER_LAKE("Fluss- und Seeufer, Teiche"),
    COAST("Küste, Strand"),
    RIVER_FLOODPLAINS("Flussauen"),
    SWAMP("Sumpf und Moor"),
    RAINFOREST("Regenwald"),
    FOREST("Wald"),
    FOREST_EDGE("Waldrand"),
    SEA("Meer"),
    CAVE_WET("Höhle (feucht)"),
    CAVE_DRY("Höhle (trocken)");
    
    /**
     * Gibt die passende DSA-Geländekunde für diese Landschaft zurück.
     * 
     * Mapping:
     * - Wald-Kunde: Wald, Waldrand
     * - Meer-Kunde: Meer, Küste/Strand
     * - Steppe-Kunde: Steppe, Hochland, Grasland
     * - Höhle-Kunde: Höhle (feucht/trocken)
     * - Dschungel-Kunde: Regenwald
     * - Sumpf-Kunde: Sumpf/Moor, Fluss-/Seeufer, Flussauen
     * - Gebirge-Kunde: Gebirge
     * - Wüste-Kunde: Wüste/Wüstenrand
     * - Eis-Kunde: Eis
     * 
     * @return Name der Geländekunde (z.B. "Wald", "Meer", "Sumpf")
     */
    fun getGelaendekunde(): String {
        return when (this) {
            FOREST, FOREST_EDGE -> "Wald"
            SEA, COAST -> "Meer"
            STEPPE, HIGHLANDS, GRASSLANDS -> "Steppe"
            CAVE_WET, CAVE_DRY -> "Höhle"
            RAINFOREST -> "Dschungel"
            SWAMP, RIVER_LAKE, RIVER_FLOODPLAINS -> "Sumpf"
            MOUNTAINS -> "Gebirge"
            DESERT -> "Wüste"
            ICE -> "Eis"
        }
    }
}

/**
 * Extension: Prüft ob der Charakter Geländekunde für diese Landschaft hat
 */
fun Landscape.hasGelaendekundeIn(gelaendekundeList: List<String>): Boolean {
    return gelaendekundeList.contains(this.getGelaendekunde())
}

/**
 * Gibt alle verfügbaren Geländekunde-Namen zurück (eindeutig, sortiert)
 * 
 * Diese Liste enthält nur die 10 echten DSA-Geländekunden:
 * Dschungel, Eis, Gebirge, Höhle, Meer, Steppe, Sumpf, Wald, Wüste
 * (+ Maraskan, falls vorhanden)
 */
fun getAllGelaendekunden(): List<String> {
    return Landscape.values()
        .map { it.getGelaendekunde() }
        .distinct()
        .sorted()
}
