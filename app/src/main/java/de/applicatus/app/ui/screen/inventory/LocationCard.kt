package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.Weight

@Composable
fun LocationCard(
    location: Location?,
    items: List<ItemWithLocation>,
    totalWeight: Weight,
    draggedItem: ItemWithLocation?,
    isEditMode: Boolean,
    isGameMaster: Boolean,
    onAddItem: () -> Unit,
    onEditItem: (ItemWithLocation) -> Unit,
    onDeleteItem: (ItemWithLocation) -> Unit,
    onDeleteLocation: () -> Unit,
    onTransferLocation: () -> Unit,
    onCarriedChanged: (Boolean) -> Unit,
    onStartDrag: (ItemWithLocation) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (ItemWithLocation, Offset) -> Unit,
    onRegisterDropTarget: (String, LocationDropTarget) -> Unit,
    onPurseAmountChange: (Long, Int) -> Unit
) {
    var cardPosition by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Registriere diese Card als Drop-Target
    LaunchedEffect(cardPosition, cardSize) {
        if (cardSize.width > 0 && cardSize.height > 0) {
            val bounds = androidx.compose.ui.geometry.Rect(
                left = cardPosition.x,
                top = cardPosition.y,
                right = cardPosition.x + cardSize.width,
                bottom = cardPosition.y + cardSize.height
            )
            onRegisterDropTarget(
                "location_${location?.id ?: "null"}",
                LocationDropTarget(
                    locationId = location?.id,
                    locationName = location?.name ?: "Ohne Ort",
                    bounds = bounds
                )
            )
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = location?.name ?: "Ohne Ort",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Spezielle Anzeige für Rüstung/Kleidung (halbes Gewicht)
                    if (location?.name == "Rüstung/Kleidung" && location.isDefault) {
                        val effectiveWeight = Weight.fromOunces(totalWeight.toOunces() / 2)
                        
                        // Gewicht in eigener Zeile
                        Text(
                            text = "${totalWeight.toDisplayString()} (eff. ${effectiveWeight.toDisplayString()})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        // Checkbox "Getragen" in nächster Zeile mit geringem Abstand
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
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
                    } else {
                        // Kompakte Zeile: Gewicht und Checkbox für alle anderen Orte
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = totalWeight.toDisplayString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Checkbox "Getragen" (nur für normale Locations)
                            if (location != null) {
                                Spacer(modifier = Modifier.width(12.dp))
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
                            Icon(Icons.Default.Send, "Ort übertragen")
                        }
                    }
                    
                    if (location != null && !location.isDefault) {
                        IconButton(onClick = onDeleteLocation) {
                            Icon(Icons.Default.Delete, "Ort löschen")
                        }
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Item-Liste
            if (items.isEmpty()) {
                Text(
                    text = "Keine Gegenstände",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                items.forEach { item ->
                    ItemRow(
                        item = item,
                        isBeingDragged = draggedItem?.id == item.id,
                        isEditMode = isEditMode,
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) },
                        onStartDrag = { onStartDrag(item) },
                        onDragUpdate = onDragUpdate,
                        onDragEnd = { offset -> onDragEnd(item, offset) },
                        onPurseAmountChange = { newAmount -> onPurseAmountChange(item.id, newAmount) }
                    )
                }
            }
        }
    }
}
