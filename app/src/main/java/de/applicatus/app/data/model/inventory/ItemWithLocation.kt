package de.applicatus.app.data.model.inventory

/**
 * View-Objekt für die Anzeige von Items mit ihrem Location-Namen
 */
data class ItemWithLocation(
    val id: Long,
    val characterId: Long,
    val locationId: Long?,
    val name: String,
    val stone: Int,
    val ounces: Int,
    val sortOrder: Int,
    val locationName: String?, // Name des Ortes (kann null sein)
    val isPurse: Boolean = false,
    val kreuzerAmount: Int = 0
) {
    val weight: Weight
        get() = Weight(stone, ounces)
    
    val currency: Currency
        get() = Currency.fromKreuzer(kreuzerAmount)
}
