package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.inventory.Location
import de.applicatus.app.data.model.inventory.Weight

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
    onConfirm: (String, Weight, Long?, Boolean, Boolean, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var stone by remember { mutableStateOf("0") }
    var ounces by remember { mutableStateOf("0") }
    var locationId by remember { mutableStateOf(selectedLocationId) }
    var expanded by remember { mutableStateOf(false) }
    var isPurse by remember { mutableStateOf(false) }
    var isCountable by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    
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
                
                // Checkbox für Geldbeutel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isPurse,
                        onCheckedChange = { 
                            isPurse = it
                            if (it) isCountable = false // Geldbeutel können nicht zählbar sein
                        }
                    )
                    Text("Geldbeutel (Gewicht wird automatisch berechnet)")
                }
                
                // Checkbox für Zählbar
                if (!isPurse) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isCountable,
                            onCheckedChange = { isCountable = it }
                        )
                        Text("Zählbarer Gegenstand (mehrere gleichartige Exemplare)")
                    }
                    
                    // Mengenangabe nur für zählbare Gegenstände
                    if (isCountable) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                            label = { Text("Menge") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Gewicht nur anzeigen, wenn kein Geldbeutel
                if (!isPurse) {
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
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
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
                    val weight = if (isPurse) {
                        Weight.ZERO // Wird automatisch berechnet
                    } else {
                        Weight(
                            stone = stone.toIntOrNull() ?: 0,
                            ounces = ounces.toIntOrNull() ?: 0
                        )
                    }
                    val finalQuantity = if (isCountable) {
                        (quantity.toIntOrNull() ?: 1).coerceAtLeast(1)
                    } else {
                        1
                    }
                    onConfirm(name, weight, locationId, isPurse, isCountable, finalQuantity)
                },
                enabled = name.isNotBlank() && (!isCountable || (quantity.toIntOrNull() ?: 0) > 0)
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
    var isCountable by remember { mutableStateOf(item.isCountable) }
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    
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
                
                // Checkbox für Zählbar (nur wenn kein Geldbeutel)
                if (!item.isPurse) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isCountable,
                            onCheckedChange = { isCountable = it }
                        )
                        Text("Zählbarer Gegenstand (mehrere gleichartige Exemplare)")
                    }
                    
                    // Mengenangabe nur für zählbare Gegenstände
                    if (isCountable) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                            label = { Text("Menge") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
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
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
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
                    val finalQuantity = if (isCountable) {
                        (quantity.toIntOrNull() ?: 1).coerceAtLeast(1)
                    } else {
                        1
                    }
                    onConfirm(item.copy(
                        name = name,
                        weight = weight,
                        locationId = locationId,
                        isCountable = isCountable,
                        quantity = finalQuantity
                    ))
                },
                enabled = name.isNotBlank() && (!isCountable || (quantity.toIntOrNull() ?: 0) > 0)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPurseDialog(
    item: Item,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val currency = de.applicatus.app.data.model.inventory.Currency.fromKreuzer(item.kreuzerAmount)
    var dukaten by remember { mutableStateOf(currency.dukaten.toString()) }
    var silbertaler by remember { mutableStateOf(currency.silbertaler.toString()) }
    var heller by remember { mutableStateOf(currency.heller.toString()) }
    var kreuzer by remember { mutableStateOf(currency.kreuzer.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${item.name} - Münzen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Dukaten mit +10/-10 Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { dukaten = maxOf(0, (dukaten.toIntOrNull() ?: 0) - 10).toString() }, modifier = Modifier.size(32.dp)) {
                        Text("-10", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { dukaten = maxOf(0, (dukaten.toIntOrNull() ?: 0) - 1).toString() }, modifier = Modifier.size(32.dp)) {
                        Text("-1", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedTextField(value = dukaten, onValueChange = { dukaten = it.filter { c -> c.isDigit() } }, label = { Text("D") }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { dukaten = ((dukaten.toIntOrNull() ?: 0) + 1).toString() }, modifier = Modifier.size(32.dp)) {
                        Text("+1", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { dukaten = ((dukaten.toIntOrNull() ?: 0) + 10).toString() }, modifier = Modifier.size(32.dp)) {
                        Text("+10", style = MaterialTheme.typography.labelSmall)
                    }
                }
                // Silbertaler
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { val c = silbertaler.toIntOrNull() ?: 0; if (c > 0) silbertaler = (c - 1).toString() }, modifier = Modifier.size(32.dp)) { Text("-", style = MaterialTheme.typography.labelSmall) }
                    OutlinedTextField(value = silbertaler, onValueChange = { val filtered = it.filter { c -> c.isDigit() }; val num = filtered.toIntOrNull() ?: 0; if (num >= 10) { val d = dukaten.toIntOrNull() ?: 0; dukaten = (d + num / 10).toString(); silbertaler = (num % 10).toString() } else { silbertaler = filtered } }, label = { Text("S") }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { val c = silbertaler.toIntOrNull() ?: 0; if (c + 1 >= 10) { val d = dukaten.toIntOrNull() ?: 0; dukaten = (d + 1).toString(); silbertaler = "0" } else { silbertaler = (c + 1).toString() } }, modifier = Modifier.size(32.dp)) { Text("+", style = MaterialTheme.typography.labelSmall) }
                }
                // Heller
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { val c = heller.toIntOrNull() ?: 0; if (c > 0) heller = (c - 1).toString() }, modifier = Modifier.size(32.dp)) { Text("-", style = MaterialTheme.typography.labelSmall) }
                    OutlinedTextField(value = heller, onValueChange = { val filtered = it.filter { c -> c.isDigit() }; val num = filtered.toIntOrNull() ?: 0; if (num >= 10) { val s = silbertaler.toIntOrNull() ?: 0; val newS = s + num / 10; if (newS >= 10) { val d = dukaten.toIntOrNull() ?: 0; dukaten = (d + newS / 10).toString(); silbertaler = (newS % 10).toString() } else { silbertaler = newS.toString() }; heller = (num % 10).toString() } else { heller = filtered } }, label = { Text("H") }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { val c = heller.toIntOrNull() ?: 0; if (c + 1 >= 10) { val s = silbertaler.toIntOrNull() ?: 0; if (s + 1 >= 10) { val d = dukaten.toIntOrNull() ?: 0; dukaten = (d + 1).toString(); silbertaler = "0" } else { silbertaler = (s + 1).toString() }; heller = "0" } else { heller = (c + 1).toString() } }, modifier = Modifier.size(32.dp)) { Text("+", style = MaterialTheme.typography.labelSmall) }
                }
                // Kreuzer
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { val c = kreuzer.toIntOrNull() ?: 0; if (c > 0) kreuzer = (c - 1).toString() }, modifier = Modifier.size(32.dp)) { Text("-", style = MaterialTheme.typography.labelSmall) }
                    OutlinedTextField(value = kreuzer, onValueChange = { val filtered = it.filter { c -> c.isDigit() }; val num = filtered.toIntOrNull() ?: 0; if (num >= 10) { val h = heller.toIntOrNull() ?: 0; val newH = h + num / 10; if (newH >= 10) { val s = silbertaler.toIntOrNull() ?: 0; val newS = s + newH / 10; if (newS >= 10) { val d = dukaten.toIntOrNull() ?: 0; dukaten = (d + newS / 10).toString(); silbertaler = (newS % 10).toString() } else { silbertaler = newS.toString() }; heller = (newH % 10).toString() } else { heller = newH.toString() }; kreuzer = (num % 10).toString() } else { kreuzer = filtered } }, label = { Text("K") }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { val c = kreuzer.toIntOrNull() ?: 0; if (c + 1 >= 10) { val h = heller.toIntOrNull() ?: 0; if (h + 1 >= 10) { val s = silbertaler.toIntOrNull() ?: 0; if (s + 1 >= 10) { val d = dukaten.toIntOrNull() ?: 0; dukaten = (d + 1).toString(); silbertaler = "0" } else { silbertaler = (s + 1).toString() }; heller = "0" } else { heller = (h + 1).toString() }; kreuzer = "0" } else { kreuzer = (c + 1).toString() } }, modifier = Modifier.size(32.dp)) { Text("+", style = MaterialTheme.typography.labelSmall) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(de.applicatus.app.data.model.inventory.Currency(dukaten.toIntOrNull() ?: 0, silbertaler.toIntOrNull() ?: 0, heller.toIntOrNull() ?: 0, kreuzer.toIntOrNull() ?: 0).toKreuzer()) }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
fun SplitItemDialog(
    item: de.applicatus.app.data.model.inventory.ItemWithLocation,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityToMove by remember { mutableStateOf("1") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gegenstände aufteilen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wie viele Exemplare von '${item.name}' möchtest du verschieben?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Verfügbar: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            val current = quantityToMove.toIntOrNull() ?: 1
                            if (current > 1) quantityToMove = (current - 1).toString()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, "Weniger")
                    }
                    
                    OutlinedTextField(
                        value = quantityToMove,
                        onValueChange = { 
                            val filtered = it.filter { c -> c.isDigit() }
                            val num = filtered.toIntOrNull() ?: 1
                            quantityToMove = num.coerceIn(1, item.quantity).toString()
                        },
                        label = { Text("Menge") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = { 
                            val current = quantityToMove.toIntOrNull() ?: 1
                            if (current < item.quantity) quantityToMove = (current + 1).toString()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, "Mehr")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val qty = quantityToMove.toIntOrNull() ?: 1
                    onConfirm(qty.coerceIn(1, item.quantity))
                },
                enabled = (quantityToMove.toIntOrNull() ?: 0) in 1..item.quantity
            ) {
                Text("Verschieben")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun TransferLocationDialog(
    location: Location,
    groupMembers: List<Character>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedCharacterId by remember { mutableStateOf<Long?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${location.name} übertragen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (groupMembers.isEmpty()) {
                    Text(
                        text = "Keine anderen Charaktere in der Gruppe vorhanden.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "An welchen Charakter soll '${location.name}' mit allen Gegenständen übertragen werden?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    groupMembers.forEach { character ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCharacterId == character.id,
                                onClick = { selectedCharacterId = character.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedCharacterId == character.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (groupMembers.isNotEmpty()) {
                TextButton(
                    onClick = { selectedCharacterId?.let { onConfirm(it) } },
                    enabled = selectedCharacterId != null
                ) {
                    Text("Übertragen")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
