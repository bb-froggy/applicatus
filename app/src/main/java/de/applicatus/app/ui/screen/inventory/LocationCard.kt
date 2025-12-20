package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.ItemWithMagic
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.model.inventory.MagicIndicator

@Composable
fun LocationCard(
    location: Location?,
    itemsWithMagic: List<ItemWithMagic>, // Items mit Magic-Indikatoren und Gewichtsreduktionen
    totalWeight: Weight,
    originalWeight: Weight? = null, // Originalgewicht vor magischen Reduktionen
    onMagicIndicatorClick: (MagicIndicator) -> Unit = {},
    draggedItem: ItemWithLocation?,
    isEditMode: Boolean,
    isGameMaster: Boolean,
    onAddItem: () -> Unit,
    onEditItem: (ItemWithLocation) -> Unit,
    onDeleteItem: (ItemWithLocation) -> Unit,
    onDeleteLocation: () -> Unit,
    onTransferLocation: () -> Unit,
    onCarriedChanged: (Boolean) -> Unit,
    onHerbPouchChanged: (Boolean) -> Unit = {},
    onStartDrag: (ItemWithLocation) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (ItemWithLocation, Offset) -> Unit,
    onRegisterDropTarget: (String, LocationDropTarget) -> Unit,
    onPurseAmountChange: (Long, Int) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    // Neue Parameter für Location-Drag
    onStartLocationDrag: (Location) -> Unit = {},
    onLocationDragUpdate: (Offset) -> Unit = {},
    onLocationDragEnd: (Location, Offset) -> Unit = { _, _ -> },
    onRegisterLocationDropTarget: (String, LocationDropTargetInfo) -> Unit = { _, _ -> }
) {
    var cardPosition by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var currentLocationDragOffset by remember { mutableStateOf(Offset.Zero) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Registriere diese Card als Drop-Target
    LaunchedEffect(cardPosition, cardSize) {
        if (cardSize.width > 0 && cardSize.height > 0) {
            val bounds = androidx.compose.ui.geometry.Rect(
                left = cardPosition.x,
                top = cardPosition.y,
                right = cardPosition.x + cardSize.width,
                bottom = cardPosition.y + cardSize.height
            )
            // Registriere als Item-Drop-Target
            onRegisterDropTarget(
                "location_${location?.id ?: "null"}",
                LocationDropTarget(
                    locationId = location?.id,
                    locationName = location?.name ?: "Ohne Ort",
                    bounds = bounds
                )
            )
            // Registriere als Location-Drop-Target (nur für nicht-Standard-Locations)
            if (location != null && !location.isDefault) {
                onRegisterLocationDropTarget(
                    "location_sort_${location.id}",
                    LocationDropTargetInfo(
                        locationId = location.id,
                        locationName = location.name,
                        sortOrder = location.sortOrder,
                        bounds = bounds
                    )
                )
            }
        }
    }
    
    val isDragTarget = draggedItem != null && draggedItem.locationId != location?.id
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                cardPosition = coordinates.positionInRoot()
                cardSize = coordinates.size
            }
            .then(
                if (isDragTarget) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header mit Ort und Gesamtgewicht
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag-Handle (nur für nicht-Standard-Locations)
                if (location != null && !location.isDefault) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                MaterialTheme.shapes.small
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.small
                            )
                            .pointerInput(location.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        currentLocationDragOffset = Offset.Zero
                                        onStartLocationDrag(location)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentLocationDragOffset += dragAmount
                                        onLocationDragUpdate(cardPosition + currentLocationDragOffset)
                                    },
                                    onDragEnd = {
                                        onLocationDragEnd(location, cardPosition + currentLocationDragOffset)
                                        currentLocationDragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        onLocationDragEnd(location, cardPosition + currentLocationDragOffset)
                                        currentLocationDragOffset = Offset.Zero
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Gepunktetes Muster (3 Reihen mit je 2 Punkten)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(2) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    androidx.compose.foundation.shape.CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = location?.name ?: "Ohne Ort",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Spezielle Anzeige für Rüstung/Kleidung (halbes Gewicht)
                    if (location?.name == "Rüstung/Kleidung" && location.isDefault) {
                        val effectiveWeight = Weight.fromOunces(totalWeight.toOunces() / 2)
                        val hasReduction = originalWeight != null && originalWeight.toOunces() > totalWeight.toOunces()
                        
                        // Gewicht - ggf. auf zwei Zeilen aufgeteilt bei magischer Reduktion
                        if (hasReduction) {
                            // Zeile 1: Original-Gewicht
                            Text(
                                text = "${originalWeight!!.toDisplayString()} →",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            // Zeile 2: Reduziertes Gewicht + effektiv
                            Text(
                                text = "${totalWeight.toDisplayString()} (eff. ${effectiveWeight.toDisplayString()})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else {
                            // Single line: Gewicht + effektiv
                            Text(
                                text = "${totalWeight.toDisplayString()} (eff. ${effectiveWeight.toDisplayString()})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        // Checkbox "Getragen" in eigener Zeile
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Checkbox(
                                checked = location.isCarried,
                                onCheckedChange = onCarriedChanged,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Getragen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Checkbox "Als Kräutertasche" (nur im Bearbeitungsmodus)
                        if (isEditMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Checkbox(
                                    checked = location.isHerbPouch,
                                    onCheckedChange = onHerbPouchChanged,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Als Kräutertasche",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Normale Locations
                        val hasReduction = originalWeight != null && originalWeight.toOunces() > totalWeight.toOunces()
                        
                        // Gewicht - ggf. auf zwei Zeilen aufgeteilt bei magischer Reduktion
                        if (hasReduction) {
                            // Zeile 1: Original-Gewicht
                            Text(
                                text = "${originalWeight!!.toDisplayString()} →",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            // Zeile 2: Reduziertes Gewicht
                            Text(
                                text = totalWeight.toDisplayString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else {
                            // Single line: nur reduziertes Gewicht
                            Text(
                                text = totalWeight.toDisplayString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        // Checkbox "Getragen" in eigener Zeile (nur für normale Locations)
                        if (location != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Checkbox(
                                    checked = location.isCarried,
                                    onCheckedChange = onCarriedChanged,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Getragen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Checkbox "Als Kräutertasche" (nur im Bearbeitungsmodus)
                            if (isEditMode) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = location.isHerbPouch,
                                        onCheckedChange = onHerbPouchChanged,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Als Kräutertasche",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = onAddItem) {
                        Icon(Icons.Default.Add, "Gegenstand hinzufügen")
                    }
                    
                    // Übertragen-Button (nur für Spielleiter und nicht für "Rüstung/Kleidung")
                    if (location != null && isGameMaster && location.name != "Rüstung/Kleidung") {
                        IconButton(onClick = onTransferLocation) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Ort übertragen")
                        }
                    }
                    
                    if (location != null && !location.isDefault && isEditMode) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, "Ort löschen")
                        }
                    }
                }
            }
            
            // Bestätigungsdialog für das Löschen einer Location
            if (showDeleteConfirmation && location != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Ort löschen?") },
                    text = { 
                        Text("Möchtest du den Ort \"${location.name}\" wirklich löschen? " +
                             "Enthaltene Gegenstände werden nach \"ohne Ort\" verschoben.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmation = false
                                onDeleteLocation()
                            }
                        ) {
                            Text("Löschen", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Abbrechen")
                        }
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Item-Liste
            if (itemsWithMagic.isEmpty()) {
                Text(
                    text = "Keine Gegenstände",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                itemsWithMagic.forEach { itemWithMagic ->
                    val item = itemWithMagic.item
                    ItemRow(
                        item = item,
                        isBeingDragged = draggedItem?.id == item.id,
                        isEditMode = isEditMode,
                        isGameMaster = isGameMaster,
                        isSelfItem = item.isSelfItem,
                        magicIndicators = itemWithMagic.magicIndicators,
                        originalWeight = itemWithMagic.originalWeight,
                        reducedWeight = itemWithMagic.reducedWeight,
                        onMagicIndicatorClick = onMagicIndicatorClick,
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) },
                        onStartDrag = { onStartDrag(item) },
                        onDragUpdate = onDragUpdate,
                        onDragEnd = { offset -> onDragEnd(item, offset) },
                        onPurseAmountChange = { newAmount -> onPurseAmountChange(item.id, newAmount) },
                        onQuantityChange = { newQuantity -> onQuantityChange(item.id, newQuantity) }
                    )
                }
            }
        }
    }
}
