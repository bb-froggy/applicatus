package de.applicatus.app.data.model.potion

/**
 * Typ des Labors, das für ein Rezept benötigt wird oder zur Verfügung steht.
 * Die Reihenfolge entspricht der Qualitätsstufe: ARCHAIC < WITCHES_KITCHEN < ALCHEMIST_LABORATORY
 */
enum class Laboratory(val shortName: String, val displayName: String, val level: Int) {
    ARCANE("AR", "Archaisches Labor", 1),
    WITCHES_KITCHEN("HK", "Hexenküche", 2),
    ALCHEMIST_LABORATORY("AL", "Alchimistenlabor", 3);
    
    /**
     * Berechnet den Modifikator für die Brauprobe basierend auf dem verfügbaren Labor.
     * @param availableLab Das verfügbare Labor
     * @return Modifikator für die Brauprobe (negativ = Erleichterung, positiv = Erschwernis)
     */
    fun getBrewingModifier(availableLab: Laboratory): Int {
        val levelDiff = this.level - availableLab.level
        return when {
            levelDiff > 0 -> levelDiff * 7  // schlechteres Labor = 7 Punkte Erschwernis pro Stufe
            levelDiff <= -2 -> -3  // zwei Stufen besseres Labor = 3 Punkte Erleichterung
            else -> 0  // gleiches oder eine Stufe besser = kein Modifikator
        }
    }
    
    /**
     * Prüft, ob das Brauen mit dem verfügbaren Labor möglich ist.
     * Ein archaisches Labor reicht nicht für ein Alchimistenlabor-Rezept.
     */
    fun canBrewWith(availableLab: Laboratory): Boolean {
        return this.level - availableLab.level <= 1
    }
}
