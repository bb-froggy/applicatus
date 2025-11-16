package de.applicatus.app.ui.screen.inventory

/**
 * Daten-Klasse für Drag-and-Drop-State (Items)
 */
data class LocationDropTarget(
    val locationId: Long?,
    val locationName: String,
    val bounds: androidx.compose.ui.geometry.Rect
)

/**
 * Daten-Klasse für Drag-and-Drop-State (Locations)
 */
data class LocationDropTargetInfo(
    val locationId: Long,
    val locationName: String,
    val sortOrder: Int,
    val bounds: androidx.compose.ui.geometry.Rect
)
