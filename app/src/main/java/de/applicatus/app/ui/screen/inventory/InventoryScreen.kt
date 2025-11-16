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
import de.applicatus.app.data.model.character.Character
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
    val groupMembers by viewModel.groupMembers.collectAsState()
    
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var selectedLocationForNewItem by remember { mutableStateOf<Long?>(null) }
    var showEditItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var showTransferLocationDialog by remember { mutableStateOf(false) }
    var locationToTransfer by remember { mutableStateOf<de.applicatus.app.data.model.inventory.Location?>(null) }
    
    // Split Item Dialog State
    var showSplitItemDialog by remember { mutableStateOf(false) }
    var itemToSplit by remember { mutableStateOf<ItemWithLocation?>(null) }
    var targetLocationForSplit by remember { mutableStateOf<Long?>(null) }
    
    // Drag-and-Drop-State
    var draggedItem by remember { mutableStateOf<ItemWithLocation?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val dropTargets = remember { mutableStateMapOf<String, LocationDropTarget>() }
    
    // Location Drag-and-Drop-State
    var draggedLocation by remember { mutableStateOf<de.applicatus.app.data.model.inventory.Location?>(null) }
    var locationDragOffset by remember { mutableStateOf(Offset.Zero) }
    val locationDropTargets = remember { mutableStateMapOf<String, LocationDropTargetInfo>() }
    
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
                            
                            // Auswirkungen der Last-BE (ab 1 Last-BE)
                            if (encumbrancePenalty >= 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Ab 1 Last-BE: Erschöpfung pro Stunde Marsch
                                    Text(
                                        text = "+$encumbrancePenalty Erschöpfung pro Stunde Marsch",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    
                                    // Ab 3 Last-BE: Keine Sprints + Rast-Regel
                                    if (encumbrancePenalty >= 3 && encumbrancePenalty < 5) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Keine Sprints möglich",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        
                                        val ko = character?.ko ?: 0
                                        if (ko > 0) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Alle $ko Spielrunden Rast für 1 SR",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    // Ab 5 Last-BE: Verschärfte Regeln
                                    if (encumbrancePenalty >= 5) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Keine Sprints, Dauerläufe und Eilmärsche möglich",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        
                                        val ko = character?.ko ?: 0
                                        if (ko > 0) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Last muss alle ${5 * ko} Schritt abgesetzt werden",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
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
                            isGameMaster = character?.isGameMaster ?: false,
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
                            onTransferLocation = {
                                locationToTransfer = location
                                showTransferLocationDialog = true
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
                                    // Check if item is countable and has quantity > 1 (excluding purses)
                                    // This includes potions (id < 0) as they are also countable
                                    if (item.isCountable && item.quantity > 1 && !item.isPurse) {
                                        // Show split dialog
                                        itemToSplit = item
                                        targetLocationForSplit = targetLocation.locationId
                                        showSplitItemDialog = true
                                    } else {
                                        // Verschiebe das Item direkt
                                        if (item.id > 0) {
                                            viewModel.moveItemToLocation(item.id, targetLocation.locationId)
                                        } else {
                                            viewModel.movePotionToLocation(-item.id, targetLocation.locationId)
                                        }
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
                            },
                            onQuantityChange = { itemId, newQuantity ->
                                viewModel.updateItemQuantity(itemId, newQuantity)
                            },
                            // Location Drag-Handler
                            onStartLocationDrag = { loc ->
                                draggedLocation = loc
                            },
                            onLocationDragUpdate = { offset ->
                                locationDragOffset = offset
                            },
                            onLocationDragEnd = { loc, finalOffset ->
                                // Finde den Ziel-Ort basierend auf der finalen Position
                                val targetLocationInfo = locationDropTargets.values
                                    .filter { it.locationId != loc.id }
                                    .find { target ->
                                        target.bounds.contains(finalOffset)
                                    }
                                
                                if (targetLocationInfo != null) {
                                    // Tausche die Sortierung
                                    viewModel.swapLocationOrder(loc.id, targetLocationInfo.locationId)
                                }
                                
                                draggedLocation = null
                                locationDragOffset = Offset.Zero
                            },
                            onRegisterLocationDropTarget = { id, target ->
                                locationDropTargets[id] = target
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
                            isGameMaster = character?.isGameMaster ?: false,
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
                            onTransferLocation = { /* Keine Übertragung für "Ohne Ort" */ },
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
                                    // Check if item is countable and has quantity > 1 (excluding purses)
                                    // This includes potions (id < 0) as they are also countable
                                    if (item.isCountable && item.quantity > 1 && !item.isPurse) {
                                        // Show split dialog
                                        itemToSplit = item
                                        targetLocationForSplit = targetLocation.locationId
                                        showSplitItemDialog = true
                                    } else {
                                        // Verschiebe das Item direkt
                                        if (item.id > 0) {
                                            viewModel.moveItemToLocation(item.id, targetLocation.locationId)
                                        } else {
                                            viewModel.movePotionToLocation(-item.id, targetLocation.locationId)
                                        }
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
                            },
                            onQuantityChange = { itemId, newQuantity ->
                                viewModel.updateItemQuantity(itemId, newQuantity)
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
            
            // Floating dragged location
            if (draggedLocation != null && locationDragOffset != Offset.Zero) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = (locationDragOffset.x / density.density).dp,
                            y = (locationDragOffset.y / density.density).dp
                        )
                        .zIndex(1001f)
                        .width(250.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                            MaterialTheme.shapes.medium
                        )
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = draggedLocation!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Ort verschieben",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
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
            onConfirm = { name, weight, locationId, isPurse, isCountable, quantity ->
                viewModel.addItem(name, weight, locationId, isPurse, isCountable, quantity)
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
    
    if (showSplitItemDialog && itemToSplit != null && targetLocationForSplit != null) {
        SplitItemDialog(
            item = itemToSplit!!,
            onDismiss = {
                showSplitItemDialog = false
                itemToSplit = null
                targetLocationForSplit = null
            },
            onConfirm = { quantityToMove ->
                val item = itemToSplit!!
                if (item.id > 0) {
                    // Regular item
                    viewModel.splitAndMoveItem(
                        itemId = item.id,
                        quantityToMove = quantityToMove,
                        targetLocationId = targetLocationForSplit!!
                    )
                } else {
                    // Potion (negative ID)
                    viewModel.splitAndMovePotion(
                        potionId = -item.id,
                        quantityToMove = quantityToMove,
                        targetLocationId = targetLocationForSplit!!
                    )
                }
                showSplitItemDialog = false
                itemToSplit = null
                targetLocationForSplit = null
            }
        )
    }
    
    if (showTransferLocationDialog && locationToTransfer != null) {
        TransferLocationDialog(
            location = locationToTransfer!!,
            groupMembers = groupMembers,
            currentCharacterId = characterId,
            onDismiss = { 
                showTransferLocationDialog = false
                locationToTransfer = null
            },
            onConfirm = { targetCharacterId ->
                viewModel.transferLocationToCharacter(locationToTransfer!!.id, targetCharacterId)
                showTransferLocationDialog = false
                locationToTransfer = null
            }
        )
    }
}
