package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.ItemWithMagic
import de.applicatus.app.data.model.inventory.MagicIndicator

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ItemRow(
    item: ItemWithLocation,
    isBeingDragged: Boolean,
    isEditMode: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartDrag: () -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onPurseAmountChange: (Int) -> Unit,
    onQuantityChange: (Int) -> Unit,
    // Neue Parameter für Magic-Anzeige
    magicIndicators: List<MagicIndicator> = emptyList(),
    isGameMaster: Boolean = false,
    isSelfItem: Boolean = false,
    originalWeight: de.applicatus.app.data.model.inventory.Weight? = null,
    reducedWeight: de.applicatus.app.data.model.inventory.Weight? = null,
    onMagicIndicatorClick: (MagicIndicator) -> Unit = {}
) {
    val isPotion = item.id < 0 // Tränke haben negative IDs
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    var currentDragOffset by remember { mutableStateOf(Offset.Zero) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Self-Items können nicht per Swipe gelöscht werden
    val canSwipeToDelete = !item.isSelfItem
    
    val dismissState = rememberDismissState(
        confirmStateChange = { dismissValue ->
            if (dismissValue == DismissValue.DismissedToStart && canSwipeToDelete) {
                showDeleteConfirmation = true
                false // Warten auf Bestätigung
            } else {
                false
            }
        }
    )
    
    SwipeToDismiss(
        state = dismissState,
        directions = if (canSwipeToDelete) setOf(DismissDirection.EndToStart) else emptySet(),
        background = {
            val color = if (dismissState.dismissDirection == DismissDirection.EndToStart && canSwipeToDelete) {
                Color.Red
            } else {
                Color.Transparent
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == DismissDirection.EndToStart && canSwipeToDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = Color.White
                    )
                }
            }
        },
        dismissContent = {
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
                // Drag-Handle (gepunktete Fläche) - nicht für SelfItems (Eigenobjekt)
                // SelfItems sind an ihre Location gebunden und dürfen nicht verschoben werden
                val canDrag = !item.isSelfItem
                
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (canDrag) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            },
                            MaterialTheme.shapes.small
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = if (canDrag) 0.3f else 0.1f),
                            shape = MaterialTheme.shapes.small
                        )
                        .then(
                            if (canDrag) {
                                Modifier.pointerInput(item.id) {
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
                                }
                            } else {
                                Modifier // Keine Drag-Gesten für SelfItems
                            }
                        ),
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
                
                // Geldbeutel: Inline-Bearbeitung
                if (item.isPurse) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Self-Item Marker
                            if (isSelfItem) {
                                Text(text = "📍", fontSize = 14.sp)
                            }
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            // Inline Währungsanzeige mit +/- Buttons
                            PurseInlineEditor(
                                currency = item.currency,
                                onAmountChange = onPurseAmountChange
                            )
                        }
                        // Magic-Indikatoren
                        if (magicIndicators.isNotEmpty()) {
                            MagicIndicatorRow(
                                indicators = magicIndicators,
                                isGameMaster = isGameMaster,
                                onIndicatorClick = onMagicIndicatorClick,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        // Gewicht mit Reduktion
                        WeightWithReduction(
                            weight = reducedWeight ?: item.weight,
                            originalWeight = originalWeight
                        )
                    }
                } else if (item.isCountable) {
                    // Zählbare Gegenstände: Name mit Menge
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Self-Item Marker
                            if (isSelfItem) {
                                Text(text = "📍", fontSize = 14.sp)
                            }
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            // Inline Mengenanzeige mit +/- Buttons (nur wenn nicht Trank)
                            if (!isPotion) {
                                QuantityInlineEditor(
                                    quantity = item.quantity,
                                    onQuantityChange = onQuantityChange
                                )
                            } else {
                                // Tränke: Menge nur anzeigen, nicht editierbar
                                Text(
                                    text = "${item.quantity}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Magic-Indikatoren
                        if (magicIndicators.isNotEmpty()) {
                            MagicIndicatorRow(
                                indicators = magicIndicators,
                                isGameMaster = isGameMaster,
                                onIndicatorClick = onMagicIndicatorClick,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        // Gewicht (Gesamtgewicht bei Menge > 1)
                        if (item.quantity > 1) {
                            Column {
                                WeightWithReduction(
                                    weight = reducedWeight?.let { it * item.quantity } ?: item.totalWeight,
                                    originalWeight = originalWeight?.let { it * item.quantity }
                                )
                                Text(
                                    text = "(${item.quantity}x ${(reducedWeight ?: item.weight).toDisplayString()})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            WeightWithReduction(
                                weight = reducedWeight ?: item.weight,
                                originalWeight = originalWeight
                            )
                        }
                        // Aussehen (nur für Tränke)
                        if (isPotion && !item.appearance.isNullOrBlank()) {
                            Text(
                                text = item.appearance,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Self-Item Marker
                            if (item.isSelfItem) {
                                Text(text = "📍", fontSize = 14.sp)
                            }
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (item.isSelfItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Gewicht mit Reduktion
                        WeightWithReduction(
                            weight = reducedWeight ?: item.weight,
                            originalWeight = originalWeight
                        )
                        // Aussehen (nur für Tränke)
                        if (isPotion && !item.appearance.isNullOrBlank()) {
                            Text(
                                text = item.appearance,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // Magische Indikatoren
                        if (magicIndicators.isNotEmpty()) {
                            MagicIndicatorRow(
                                indicators = magicIndicators,
                                isGameMaster = isGameMaster,
                                onIndicatorClick = onMagicIndicatorClick
                            )
                        }
                    }
                }
                
                // Edit-Icon nur im Edit-Modus und nur für normale Items (nicht Tränke, Geldbeutel oder Eigenobjekte)
                if (isEditMode && !isPotion && !item.isPurse && !item.isSelfItem) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Bearbeiten", tint = MaterialTheme.colorScheme.primary)
                    }
                } else if (item.isSelfItem && isEditMode) {
                    // Eigenobjekte: Edit für Gewicht erlauben
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Eigenobjekt bearbeiten", tint = MaterialTheme.colorScheme.secondary)
                    }
                } else if (isPotion) {
                    // Tränke: Marker-Icon
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Trank",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    // Spacer für Layout-Konsistenz
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    )
    
    // Bestätigungs-Dialog für Löschen
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
            },
            title = { Text("Gegenstand löschen?") },
            text = { Text("Möchtest du '${item.name}' wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
    
    // Reset des Swipe-States
    LaunchedEffect(showDeleteConfirmation) {
        if (!showDeleteConfirmation) {
            dismissState.reset()
        }
    }
}

@Composable
fun PurseInlineEditor(
    currency: de.applicatus.app.data.model.inventory.Currency,
    onAmountChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Kompakte Anzeige mit Klick zum Erweitern
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = currency.toDisplayString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Weniger" else "Mehr",
                modifier = Modifier.size(18.dp)
            )
        }
    }
    
    // Erweiterte Bearbeitungsansicht
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("Münzen bearbeiten") },
            text = {
                PurseEditor(
                    currency = currency,
                    onAmountChange = onAmountChange
                )
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text("Fertig")
                }
            }
        )
    }
}

@Composable
fun PurseEditor(
    currency: de.applicatus.app.data.model.inventory.Currency,
    onAmountChange: (Int) -> Unit
) {
    // Initialen Betrag nur einmal beim ersten Öffnen speichern
    val initialAmount = remember { currency.toKreuzer() }
    var dukaten by remember { mutableStateOf(currency.dukaten) }
    var silbertaler by remember { mutableStateOf(currency.silbertaler) }
    var heller by remember { mutableStateOf(currency.heller) }
    var kreuzer by remember { mutableStateOf(currency.kreuzer) }
    
    // Berechne die Differenz basierend auf dem gespeicherten Anfangswert
    val currentAmount = de.applicatus.app.data.model.inventory.Currency(dukaten, silbertaler, heller, kreuzer).toKreuzer()
    val difference = currentAmount - initialAmount
    val differenceDisplay = de.applicatus.app.data.model.inventory.Currency.fromKreuzer(kotlin.math.abs(difference))
    
    // Update callback
    val updateAmount = {
        val newCurrency = de.applicatus.app.data.model.inventory.Currency(dukaten, silbertaler, heller, kreuzer)
        onAmountChange(newCurrency.toKreuzer())
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Horizontale Anzeige aller Münzen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dukaten (mit +10/-10)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { dukaten = maxOf(0, dukaten - 10); updateAmount() }, modifier = Modifier.size(32.dp)) {
                        Text("-10", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { dukaten = maxOf(0, dukaten - 1); updateAmount() }, modifier = Modifier.size(32.dp)) {
                        Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "$dukaten D",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { dukaten += 1; updateAmount() }, modifier = Modifier.size(32.dp)) {
                        Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { dukaten += 10; updateAmount() }, modifier = Modifier.size(32.dp)) {
                        Text("+10", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Silbertaler
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { 
                    if (silbertaler > 0) { silbertaler -= 1; updateAmount() }
                }, modifier = Modifier.size(32.dp)) {
                    Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$silbertaler S",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { 
                    silbertaler += 1
                    if (silbertaler >= 10) { dukaten += 1; silbertaler = 0 }
                    updateAmount()
                }, modifier = Modifier.size(32.dp)) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            
            // Heller
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { 
                    if (heller > 0) { heller -= 1; updateAmount() }
                }, modifier = Modifier.size(32.dp)) {
                    Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$heller H",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { 
                    heller += 1
                    if (heller >= 10) {
                        silbertaler += 1
                        heller = 0
                        if (silbertaler >= 10) { dukaten += 1; silbertaler = 0 }
                    }
                    updateAmount()
                }, modifier = Modifier.size(32.dp)) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            
            // Kreuzer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { 
                    if (kreuzer > 0) { kreuzer -= 1; updateAmount() }
                }, modifier = Modifier.size(32.dp)) {
                    Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$kreuzer K",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { 
                    kreuzer += 1
                    if (kreuzer >= 10) {
                        heller += 1
                        kreuzer = 0
                        if (heller >= 10) {
                            silbertaler += 1
                            heller = 0
                            if (silbertaler >= 10) { dukaten += 1; silbertaler = 0 }
                        }
                    }
                    updateAmount()
                }, modifier = Modifier.size(32.dp)) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Differenz-Anzeige
        if (difference != 0) {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (difference > 0) "+" else "-",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (difference > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = differenceDisplay.toDisplayString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (difference > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (difference > 0) "hineingelegt" else "herausgenommen",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun QuantityInlineEditor(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (expanded) {
            // Erweiterte Ansicht mit +/- Buttons
            IconButton(
                onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${quantity}x",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { onQuantityChange(quantity + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = { expanded = false },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Weniger",
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            // Kompakte Anzeige mit Klick zum Erweitern
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${quantity}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Mehr",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
