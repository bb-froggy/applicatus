package de.applicatus.app.data.model.inventory

/**
 * View-Objekt für die Anzeige von Items mit ihrem Location-Namen
 */
data class ItemWithLocation(
    val id: Long,
    val guid: String = "",
    val characterId: Long,
    val locationId: Long?,
    val name: String,
    val stone: Int,
    val ounces: Int,
    val sortOrder: Int,
    val locationName: String?, // Name des Ortes (kann null sein)
    val isPurse: Boolean = false,
    val kreuzerAmount: Int = 0,
    val appearance: String? = null, // Aussehen (nur für Tränke, nicht aus DB)
    val isCountable: Boolean = false,
    val quantity: Int = 1,
    val isSelfItem: Boolean = false, // Eigenobjekt des Ortes
    val selfItemForLocationId: Long? = null // Für welchen Ort ist dies das Eigenobjekt
) {
    val weight: Weight
        get() = Weight(stone, ounces)
    
    val totalWeight: Weight
        get() = weight * quantity
    
    val currency: Currency
        get() = Currency.fromKreuzer(kreuzerAmount)
}
