package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.ApplicatusApplication
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.ui.viewmodel.InventoryViewModel
import de.applicatus.app.ui.viewmodel.InventoryViewModelFactory

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
    var isEditMode by remember { mutableStateOf(false) }
    
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
                    // Bearbeitungsmodus Toggle
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            if (isEditMode) "Bearbeitung beenden" else "Bearbeiten"
                        )
                    }
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
                            isEditMode = isEditMode,
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
                            },
                            onPurseAmountChange = { itemId, newAmount ->
                                viewModel.updatePurseAmount(itemId, newAmount)
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
                            isEditMode = isEditMode,
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
                            },
                            onPurseAmountChange = { itemId, newAmount ->
                                viewModel.updatePurseAmount(itemId, newAmount)
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
            onConfirm = { name, weight, locationId, isPurse ->
                viewModel.addItem(name, weight, locationId, isPurse)
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
