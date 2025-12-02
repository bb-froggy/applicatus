package de.applicatus.app.data.model.inventory

import de.applicatus.app.data.model.magicsign.MagicSignEffect

/**
 * Typ des magischen Indikators
 */
enum class MagicIndicatorType {
    /** Zauberzeichen (🔮) */
    MAGIC_SIGN,
    /** Applicatus (✨) */
    APPLICATUS,
    /** Langwirkender Zauber (⏳) */
    LONG_DURATION_SPELL
}

/**
 * Generischer Indikator für magische Effekte auf einem Item.
 * Kann ein Zauberzeichen, Applicatus oder langwirkender Zauber sein.
 */
data class MagicIndicator(
    val type: MagicIndicatorType,
    
    /** Name des Zaubers/Zeichens */
    val name: String,
    
    /** Beschreibung/Effekt */
    val description: String,
    
    /** Ist der Effekt aktiv? (Bei Zauberzeichen: nach Aktivierung) */
    val isActive: Boolean,
    
    /** Ist der Effekt verdorben/fehlgeschlagen? (Nur für GM sichtbar) */
    val isBotched: Boolean = false,
    
    /** Ablaufdatum (derisches Format) */
    val expiryDate: String? = null,
    
    /** Übrig gebliebene ZfP* / RkP* */
    val effectPoints: Int? = null,
    
    /** Original-ID für Detail-Abfragen */
    val sourceId: Long,
    
    // Spezifische Felder für verschiedene Typen:
    
    /** Für Zauberzeichen: Spezialeffekt */
    val magicSignEffect: MagicSignEffect? = null,
    
    /** Für Zauberzeichen: Aktivierungsmodifikator */
    val activationModifier: Int? = null,
    
    /** Für Applicatus/Langzauber: Slot-Nummer */
    val slotNumber: Int? = null,
    
    /** Für Applicatus/Langzauber: Zaubervariante */
    val variant: String? = null,
    
    /** Für Applicatus/Langzauber: ASP-Kosten */
    val aspCost: String? = null
) {
    /**
     * Symbol für den Indikator-Typ
     */
    val symbol: String
        get() = when (type) {
            MagicIndicatorType.MAGIC_SIGN -> "🔮"
            MagicIndicatorType.APPLICATUS -> "✨"
            MagicIndicatorType.LONG_DURATION_SPELL -> "⏳"
        }
    
    /**
     * Kurze Beschreibung für Tooltip
     */
    val shortDescription: String
        get() = when (type) {
            MagicIndicatorType.MAGIC_SIGN -> if (isActive) "Aktives Zauberzeichen" else "Inaktives Zauberzeichen"
            MagicIndicatorType.APPLICATUS -> "Applicatus"
            MagicIndicatorType.LONG_DURATION_SPELL -> "Langwirkender Zauber"
        }
}

/**
 * Erweiterung von ItemWithLocation um Magic-Indikatoren
 */
data class ItemWithMagic(
    val item: ItemWithLocation,
    val magicIndicators: List<MagicIndicator> = emptyList(),
    
    /** Ob das Item ein Eigenobjekt (Self-Item) einer Location ist */
    val isSelfItem: Boolean = false,
    
    /** Original-Gewicht (vor Gewichtsreduktion durch Zauberzeichen) */
    val originalWeight: Weight? = null,
    
    /** Reduziertes Gewicht (nach Gewichtsreduktion durch Zauberzeichen) */
    val reducedWeight: Weight? = null
) {
    /** Hat das Item aktive magische Effekte? */
    val hasMagic: Boolean
        get() = magicIndicators.isNotEmpty()
    
    /** Hat das Item aktive Gewichtsreduktion? */
    val hasWeightReduction: Boolean
        get() = originalWeight != null && reducedWeight != null && originalWeight != reducedWeight
    
    /** Anzeigegewicht (reduziert wenn vorhanden, sonst original) */
    val displayWeight: Weight
        get() = reducedWeight ?: item.weight
    
    /** Gesamtgewicht mit Berücksichtigung von Menge und Reduktion */
    val displayTotalWeight: Weight
        get() = if (item.isCountable) {
            displayWeight * item.quantity
        } else {
            displayWeight
        }
}
