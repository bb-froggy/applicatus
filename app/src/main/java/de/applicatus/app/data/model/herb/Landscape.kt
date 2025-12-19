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
    CAVE_DRY("Höhle (trocken)")
}
