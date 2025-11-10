package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.ApplicatusApplication
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.ItemWithLocation
import de.applicatus.app.data.model.inventory.Location
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
    val viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(application.repository, characterId)
    )
    
    val character by viewModel.character.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val itemsByLocation by viewModel.itemsByLocation.collectAsState()
    val weightByLocation by viewModel.weightByLocation.collectAsState()
    
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var selectedLocationForNewItem by remember { mutableStateOf<Long?>(null) }
    var showEditItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }
    
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
            FloatingActionButton(
                onClick = {
                    selectedLocationForNewItem = locations.firstOrNull()?.id
                    showAddItemDialog = true
                }
            ) {
                Icon(Icons.Default.Add, "Gegenstand hinzufügen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Für jeden Ort eine Karte
            locations.forEach { location ->
                item(key = "location_${location.id}") {
                    LocationCard(
                        location = location,
                        items = itemsByLocation[location] ?: emptyList(),
                        totalWeight = weightByLocation[location.id] ?: Weight.ZERO,
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
                        onDeleteLocation = {}
                    )
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
    onAddItem: () -> Unit,
    onEditItem: (ItemWithLocation) -> Unit,
    onDeleteItem: (ItemWithLocation) -> Unit,
    onDeleteLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    Text(
                        text = "Gesamtgewicht: ${totalWeight.toDisplayString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun ItemRow(
    item: ItemWithLocation,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPotion = item.id < 0 // Tränke haben negative IDs
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
