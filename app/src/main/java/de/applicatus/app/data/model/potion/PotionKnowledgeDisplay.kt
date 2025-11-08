package de.applicatus.app.data.model.potion

/**
 * Hilfsobjekt zum Anzeigen des Analysewissens über ein Elixier
 */
data class PotionKnowledgeDisplay(
    val categoryText: String,              // "Kategorie: Spagyrik" oder "Kategorie: Unbekannt"
    val qualityText: String,               // "Qualität: Schwach" / "Sehr schwach (A/B)" / "Genau: B" / "Unbekannt"
    val shelfLifeText: String,             // "Haltbarkeit: 3 Monde" oder "Haltbarkeit: Unbekannt"
    val analysisProgressText: String       // "Nicht analysiert" / "Teilweise analysiert" / "Vollständig analysiert"
) {
    companion object {
        fun fromPotion(potion: Potion, recipe: Recipe): PotionKnowledgeDisplay {
            // Kategorie
            val categoryText = if (potion.categoryKnown) {
                "Kategorie: ${recipe.gruppe.ifEmpty { "Allgemein" }}"
            } else {
                "Kategorie: Unbekannt"
            }
            
            // Qualität
            val qualityText = when (potion.knownQualityLevel) {
                KnownQualityLevel.EXACT -> {
                    "Qualität: ${potion.knownExactQuality?.name ?: "Unbekannt"}"
                }
                KnownQualityLevel.VERY_WEAK_MEDIUM_OR_VERY_STRONG -> {
                    when (potion.refinedQuality) {
                        RefinedQuality.VERY_WEAK -> "Qualität: Sehr schwach (A oder B)"
                        RefinedQuality.MEDIUM -> "Qualität: Mittel (C oder D)"
                        RefinedQuality.VERY_STRONG -> "Qualität: Sehr stark (E oder F)"
                        RefinedQuality.UNKNOWN -> "Qualität: Unbekannt"
                    }
                }
                KnownQualityLevel.WEAK_OR_STRONG -> {
                    when (potion.intensityQuality) {
                        IntensityQuality.WEAK -> "Qualität: Schwach (A, B oder C)"
                        IntensityQuality.STRONG -> "Qualität: Stark (D, E oder F)"
                        IntensityQuality.UNKNOWN -> "Qualität: Unbekannt"
                    }
                }
                KnownQualityLevel.UNKNOWN -> {
                    // Auch wenn der genaue Level unbekannt ist, kann die Intensität bekannt sein
                    when (potion.intensityQuality) {
                        IntensityQuality.WEAK -> "Qualität: Schwach (A, B oder C)"
                        IntensityQuality.STRONG -> "Qualität: Stark (D, E oder F)"
                        IntensityQuality.UNKNOWN -> "Qualität: Unbekannt"
                    }
                }
            }
            
            // Haltbarkeit
            val shelfLifeText = if (potion.shelfLifeKnown) {
                "Haltbarkeit: ${recipe.shelfLife}"
            } else {
                "Haltbarkeit: Unbekannt"
            }
            
            // Analyse-Fortschritt
            val analysisProgressText = when {
                potion.knownQualityLevel == KnownQualityLevel.EXACT -> "Vollständig analysiert"
                potion.categoryKnown || potion.knownQualityLevel != KnownQualityLevel.UNKNOWN -> "Teilweise analysiert"
                potion.intensityQuality != IntensityQuality.UNKNOWN -> "Intensität bestimmt"
                else -> "Nicht analysiert"
            }
            
            return PotionKnowledgeDisplay(
                categoryText = categoryText,
                qualityText = qualityText,
                shelfLifeText = shelfLifeText,
                analysisProgressText = analysisProgressText
            )
        }
    }
}
