package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.inventory.ItemWithLocation

@Composable
fun ItemRow(
    item: ItemWithLocation,
    isBeingDragged: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartDrag: () -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit
) {
    val isPotion = item.id < 0 // Tränke haben negative IDs
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    var currentDragOffset by remember { mutableStateOf(Offset.Zero) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onGloballyPositioned { coordinates ->
                itemPosition = coordinates.positionInRoot()
            }
            .then(
                if (isBeingDragged) {
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.shapes.small
                        )
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag-Handle (gepunktete Fläche)
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
                .pointerInput(item.id) {
                    detectDragGestures(
                        onDragStart = { 
                            currentDragOffset = Offset.Zero
                            onStartDrag()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentDragOffset += dragAmount
                            onDragUpdate(itemPosition + currentDragOffset)
                        },
                        onDragEnd = {
                            onDragEnd(itemPosition + currentDragOffset)
                            currentDragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            onDragEnd(itemPosition + currentDragOffset)
                            currentDragOffset = Offset.Zero
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
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge
            )
            
            // Geldbeutel: Zeige Währung statt Gewicht
            if (item.isPurse) {
                Text(
                    text = item.currency.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.weight.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = item.weight.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (!isPotion && !item.isPurse) {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Bearbeiten", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }
        } else if (item.isPurse) {
            // Geldbeutel: Bearbeiten und Löschen
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Münzen bearbeiten", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            // Tränke können nicht direkt hier bearbeitet werden
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Trank",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
