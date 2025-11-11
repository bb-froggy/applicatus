package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.ApplicatusApplication
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.ui.viewmodel.InventoryViewModel
import de.applicatus.app.ui.viewmodel.InventoryViewModelFactory

/**
 * Daten-Klasse für Drag-and-Drop-State
 */
data class LocationDropTarget(
    val locationId: Long?,
    val locationName: String,
    val bounds: androidx.compose.ui.geometry.Rect
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    characterId: Long,
    onNavigateBack: () -> Unit,
    application: ApplicatusApplication
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    val viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(application.repository, characterId)
    )
    
    val character by viewModel.character.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val itemsByLocation by viewModel.itemsByLocation.collectAsState()
    val weightByLocation by viewModel.weightByLocation.collectAsState()
    val carriedWeight by viewModel.carriedWeight.collectAsState()
    val carryingCapacity by viewModel.carryingCapacity.collectAsState()
    val encumbrancePenalty by viewModel.encumbrancePenalty.collectAsState()
    
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var selectedLocationForNewItem by remember { mutableStateOf<Long?>(null) }
    var showEditItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }
    
    // Drag-and-Drop-State
    var draggedItem by remember { mutableStateOf<ItemWithLocation?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val dropTargets = remember { mutableStateMapOf<String, LocationDropTarget>() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${character?.name ?: ""} - Packesel") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddLocationDialog = true }) {
                        Icon(Icons.Default.Add, "Ort hinzufügen")
                    }
                }
            )
        },
        floatingActionButton = {
            // Entfernt - Plus-Buttons sind bei jedem Ort vorhanden
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Getragenes Gesamtgewicht
                item(key = "carried_weight") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Zeile 1: Getragenes Gewicht
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Getragenes Gewicht",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = carriedWeight.toDisplayString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Zeile 2: Tragfähigkeit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tragfähigkeit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = carryingCapacity.toDisplayString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Zeile 3: Last-BE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Last-BE",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "$encumbrancePenalty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (encumbrancePenalty > 0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Für jeden Ort eine Karte
                locations.forEach { location ->
                    item(key = "location_${location.id}") {
                        LocationCard(
                            location = location,
                            items = itemsByLocation[location] ?: emptyList(),
                            totalWeight = weightByLocation[location.id] ?: Weight.ZERO,
                            draggedItem = draggedItem,
                            onAddItem = {
                                selectedLocationForNewItem = location.id
                                showAddItemDialog = true
                            },
                            onEditItem = { item ->
                                editingItem = Item(
                                    id = item.id,
                                    characterId = item.characterId,
                                    locationId = item.locationId,
                                    name = item.name,
                                    weight = item.weight,
                                    sortOrder = item.sortOrder
                                )
                                showEditItemDialog = true
                            },
                            onDeleteItem = { item ->
                                if (item.id > 0) { // Echte Items (keine Tränke)
                                    viewModel.deleteItem(Item(
                                        id = item.id,
                                        characterId = item.characterId,
                                        locationId = item.locationId,
                                        name = item.name,
                                        weight = item.weight
                                    ))
                                }
                            },
                            onDeleteLocation = {
                                if (!location.isDefault) {
                                    viewModel.deleteLocation(location)
                                }
                            },
                            onCarriedChanged = { isCarried ->
                                viewModel.updateLocationIsCarried(location.id, isCarried)
                            },
                            onStartDrag = { item ->
                                draggedItem = item
                            },
                            onDragUpdate = { offset ->
                                dragOffset = offset
                            },
                            onDragEnd = { item, finalOffset ->
                                // Finde den Ziel-Ort basierend auf der finalen Position
                                val targetLocation = dropTargets.values.find { target ->
                                    target.bounds.contains(finalOffset)
                                }
                                
                                if (targetLocation != null && targetLocation.locationId != item.locationId) {
                                    // Verschiebe das Item
                                    if (item.id > 0) {
                                        viewModel.moveItemToLocation(item.id, targetLocation.locationId)
                                    } else {
                                        viewModel.movePotionToLocation(-item.id, targetLocation.locationId)
                                    }
                                }
                                
                                draggedItem = null
                                dragOffset = Offset.Zero
                            },
                            onRegisterDropTarget = { id, target ->
                                dropTargets[id] = target
                            }
                        )
                    }
                }
                
                // Items ohne Ort
                val itemsWithoutLocation = itemsByLocation[null]
                if (!itemsWithoutLocation.isNullOrEmpty()) {
                    item(key = "location_null") {
                        LocationCard(
                            location = null,
                            items = itemsWithoutLocation,
                            totalWeight = weightByLocation[null] ?: Weight.ZERO,
                            draggedItem = draggedItem,
                            onAddItem = {
                                selectedLocationForNewItem = null
                                showAddItemDialog = true
                            },
                            onEditItem = { item ->
                                editingItem = Item(
                                    id = item.id,
                                    characterId = item.characterId,
                                    locationId = item.locationId,
                                    name = item.name,
                                    weight = item.weight,
                                    sortOrder = item.sortOrder
                                )
                                showEditItemDialog = true
                            },
                            onDeleteItem = { item ->
                                if (item.id > 0) {
                                    viewModel.deleteItem(Item(
                                        id = item.id,
                                        characterId = item.characterId,
                                        locationId = item.locationId,
                                        name = item.name,
                                        weight = item.weight
                                    ))
                                }
                            },
                            onDeleteLocation = {},
                            onCarriedChanged = { /* Keine Aktion für "Ohne Ort" */ },
                            onStartDrag = { item ->
                                draggedItem = item
                            },
                            onDragUpdate = { offset ->
                                dragOffset = offset
                            },
                            onDragEnd = { item, finalOffset ->
                                val targetLocation = dropTargets.values.find { target ->
                                    target.bounds.contains(finalOffset)
                                }
                                
                                if (targetLocation != null && targetLocation.locationId != item.locationId) {
                                    if (item.id > 0) {
                                        viewModel.moveItemToLocation(item.id, targetLocation.locationId)
                                    } else {
                                        viewModel.movePotionToLocation(-item.id, targetLocation.locationId)
                                    }
                                }
                                
                                draggedItem = null
                                dragOffset = Offset.Zero
                            },
                            onRegisterDropTarget = { id, target ->
                                dropTargets[id] = target
                            }
                        )
                    }
                }
            }
            
            // Floating dragged item
            if (draggedItem != null && dragOffset != Offset.Zero) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = (dragOffset.x / density.density).dp,
                            y = (dragOffset.y / density.density).dp
                        )
                        .zIndex(1000f)
                        .width(200.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            MaterialTheme.shapes.medium
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = draggedItem!!.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = draggedItem!!.weight.toDisplayString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
    
    // Dialoge
    if (showAddLocationDialog) {
        AddLocationDialog(
            onDismiss = { showAddLocationDialog = false },
            onConfirm = { name ->
                viewModel.addLocation(name)
                showAddLocationDialog = false
            }
        )
    }
    
    if (showAddItemDialog) {
        AddItemDialog(
            locations = locations,
            selectedLocationId = selectedLocationForNewItem,
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, weight, locationId ->
                viewModel.addItem(name, weight, locationId)
                showAddItemDialog = false
            }
        )
    }
    
    if (showEditItemDialog && editingItem != null) {
        EditItemDialog(
            item = editingItem!!,
            locations = locations,
            onDismiss = { showEditItemDialog = false },
            onConfirm = { updatedItem ->
                viewModel.updateItem(updatedItem)
                showEditItemDialog = false
                editingItem = null
            }
        )
    }
}

@Composable
fun LocationCard(
    location: Location?,
    items: List<ItemWithLocation>,
    totalWeight: Weight,
    draggedItem: ItemWithLocation?,
    onAddItem: () -> Unit,
    onEditItem: (ItemWithLocation) -> Unit,
    onDeleteItem: (ItemWithLocation) -> Unit,
    onDeleteLocation: () -> Unit,
    onCarriedChanged: (Boolean) -> Unit,
    onStartDrag: (ItemWithLocation) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (ItemWithLocation, Offset) -> Unit,
    onRegisterDropTarget: (String, LocationDropTarget) -> Unit
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
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) },
                        onStartDrag = { onStartDrag(item) },
                        onDragUpdate = onDragUpdate,
                        onDragEnd = { offset -> onDragEnd(item, offset) }
                    )
                }
            }
        }
    }
}

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
            Text(
                text = item.weight.toDisplayString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (!isPotion) {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Bearbeiten", tint = MaterialTheme.colorScheme.primary)
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

@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Ort") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    locations: List<Location>,
    selectedLocationId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, Weight, Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var stone by remember { mutableStateOf("0") }
    var ounces by remember { mutableStateOf("0") }
    var locationId by remember { mutableStateOf(selectedLocationId) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Gegenstand") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stone,
                        onValueChange = { stone = it.filter { c -> c.isDigit() } },
                        label = { Text("Stein") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = ounces,
                        onValueChange = { ounces = it.filter { c -> c.isDigit() } },
                        label = { Text("Unzen") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = locations.find { it.id == locationId }?.name ?: "Ohne Ort",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ort") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ohne Ort") },
                            onClick = {
                                locationId = null
                                expanded = false
                            }
                        )
                        locations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.name) },
                                onClick = {
                                    locationId = location.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weight = Weight(
                        stone = stone.toIntOrNull() ?: 0,
                        ounces = ounces.toIntOrNull() ?: 0
                    )
                    onConfirm(name, weight, locationId)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemDialog(
    item: Item,
    locations: List<Location>,
    onDismiss: () -> Unit,
    onConfirm: (Item) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var stone by remember { mutableStateOf(item.weight.stone.toString()) }
    var ounces by remember { mutableStateOf(item.weight.ounces.toString()) }
    var locationId by remember { mutableStateOf(item.locationId) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gegenstand bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stone,
                        onValueChange = { stone = it.filter { c -> c.isDigit() } },
                        label = { Text("Stein") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = ounces,
                        onValueChange = { ounces = it.filter { c -> c.isDigit() } },
                        label = { Text("Unzen") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = locations.find { it.id == locationId }?.name ?: "Ohne Ort",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ort") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ohne Ort") },
                            onClick = {
                                locationId = null
                                expanded = false
                            }
                        )
                        locations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.name) },
                                onClick = {
                                    locationId = location.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weight = Weight(
                        stone = stone.toIntOrNull() ?: 0,
                        ounces = ounces.toIntOrNull() ?: 0
                    )
                    onConfirm(item.copy(
                        name = name,
                        weight = weight,
                        locationId = locationId
                    ))
                },
                enabled = name.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
