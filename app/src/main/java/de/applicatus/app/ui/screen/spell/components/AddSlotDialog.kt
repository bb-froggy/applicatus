package de.applicatus.app.ui.screen.spell.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.spell.SlotType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSlotDialog(
    canAddApplicatus: Boolean,
    remainingVolumePoints: Int,
    availableItems: List<Item> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (SlotType, Int, Long?) -> Unit
) {
    var selectedType by remember { mutableStateOf(SlotType.SPELL_STORAGE) }
    var volumePointsText by remember { mutableStateOf("10") }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var itemDropdownExpanded by remember { mutableStateOf(false) }
    
    // Reset item selection when type changes (except for APPLICATUS and LONG_DURATION)
    LaunchedEffect(selectedType) {
        if (selectedType == SlotType.SPELL_STORAGE) {
            selectedItem = null
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Zauberslot") },
        text = {
            Column {
                Text("Slot-Typ auswählen:")
                Spacer(modifier = Modifier.height(8.dp))

                FilterChip(
                    selected = selectedType == SlotType.SPELL_STORAGE,
                    onClick = { selectedType = SlotType.SPELL_STORAGE },
                    label = { Text("Zauberspeicher") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(vertical = 0.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                FilterChip(
                    selected = selectedType == SlotType.APPLICATUS,
                    onClick = { selectedType = SlotType.APPLICATUS },
                    label = { Text("Applicatus") },
                    enabled = canAddApplicatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(vertical = 0.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                FilterChip(
                    selected = selectedType == SlotType.LONG_DURATION,
                    onClick = { selectedType = SlotType.LONG_DURATION },
                    label = { Text("Langwirkend") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(vertical = 0.dp)
                )
                
                // Item-Auswahl für Applicatus (Pflicht) und Langwirkend (optional)
                if (selectedType == SlotType.APPLICATUS || selectedType == SlotType.LONG_DURATION) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedType == SlotType.APPLICATUS) 
                            "Gegenstand auswählen (Pflicht):" 
                        else 
                            "Gegenstand auswählen (optional):"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = itemDropdownExpanded,
                        onExpandedChange = { itemDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedItem?.name ?: if (selectedType == SlotType.LONG_DURATION) "Kein Gegenstand" else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gegenstand") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            isError = selectedType == SlotType.APPLICATUS && selectedItem == null
                        )
                        ExposedDropdownMenu(
                            expanded = itemDropdownExpanded,
                            onDismissRequest = { itemDropdownExpanded = false }
                        ) {
                            // Option "Kein Gegenstand" nur für langwirkende Zauber
                            if (selectedType == SlotType.LONG_DURATION) {
                                DropdownMenuItem(
                                    text = { Text("Kein Gegenstand") },
                                    onClick = {
                                        selectedItem = null
                                        itemDropdownExpanded = false
                                    }
                                )
                            }
                            availableItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name) },
                                    onClick = {
                                        selectedItem = item
                                        itemDropdownExpanded = false
                                    }
                                )
                            }
                            if (availableItems.isEmpty()) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "Keine Gegenstände vorhanden",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ) 
                                    },
                                    onClick = { itemDropdownExpanded = false },
                                    enabled = false
                                )
                            }
                        }
                    }
                    
                    if (selectedType == SlotType.APPLICATUS && selectedItem == null) {
                        Text(
                            text = "Ein Applicatus muss an einen Gegenstand gebunden werden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (selectedType == SlotType.SPELL_STORAGE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Volumenpunkte (1-100):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = volumePointsText,
                        onValueChange = { 
                            volumePointsText = it.filter { c -> c.isDigit() }
                        },
                        label = { Text("Volumenpunkte") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = (volumePointsText.toIntOrNull() ?: 0) < 1
                    )
                    if ((volumePointsText.toIntOrNull() ?: 0) < 1) {
                        Text(
                            text = "Mindestens 1 VP erforderlich",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "Verbleibend: $remainingVolumePoints VP",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (remainingVolumePoints >= (volumePointsText.toIntOrNull() ?: 0))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val volumePoints = if (selectedType == SlotType.SPELL_STORAGE) {
                        volumePointsText.toIntOrNull()?.coerceIn(1, 100) ?: 10
                    } else {
                        0
                    }
                    
                    // Prüfe Volumenpunkte-Limit
                    if (selectedType == SlotType.SPELL_STORAGE && volumePoints > remainingVolumePoints) {
                        return@TextButton
                    }
                    
                    // Prüfe Applicatus-Pflicht
                    if (selectedType == SlotType.APPLICATUS && selectedItem == null) {
                        return@TextButton
                    }
                    
                    onConfirm(selectedType, volumePoints, selectedItem?.id)
                },
                enabled = when (selectedType) {
                    SlotType.SPELL_STORAGE -> {
                        val vp = volumePointsText.toIntOrNull() ?: 0
                        vp in 1..remainingVolumePoints
                    }
                    SlotType.APPLICATUS -> selectedItem != null
                    SlotType.LONG_DURATION -> true
                }
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
