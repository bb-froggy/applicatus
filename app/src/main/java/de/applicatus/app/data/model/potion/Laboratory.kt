package de.applicatus.app.data.model.potion

/**
 * Typ des Labors, das für ein Rezept benötigt wird
 */
enum class Laboratory(val shortName: String, val displayName: String) {
    ARCANE("AR", "Archaisches Labor"),
    WITCHES_KITCHEN("HK", "Hexenküche"),
    LABORATORY("L", "Labor")
}
