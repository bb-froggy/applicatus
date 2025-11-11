package de.applicatus.app.ui.screen.inventory

/**
 * Daten-Klasse für Drag-and-Drop-State
 */
data class LocationDropTarget(
    val locationId: Long?,
    val locationName: String,
    val bounds: androidx.compose.ui.geometry.Rect
)
