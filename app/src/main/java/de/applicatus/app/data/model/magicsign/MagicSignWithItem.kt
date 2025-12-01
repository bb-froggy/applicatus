package de.applicatus.app.data.model.magicsign

/**
 * View-Objekt für ein Zauberzeichen mit dem Namen des Ziel-Items.
 */
data class MagicSignWithItem(
    val magicSign: MagicSign,
    val itemName: String?,
    val locationName: String?
)
