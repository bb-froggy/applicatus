package de.applicatus.app.ui.screen.spell.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.spell.SlotType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSlotDialog(
    canAddApplicatus: Boolean,
    remainingVolumePoints: Int,
    onDismiss: () -> Unit,
    onConfirm: (SlotType, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(SlotType.SPELL_STORAGE) }
    var volumePointsText by remember { mutableStateOf("10") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Zauberslot") },
        text = {
            Column {
                Text("Slot-Typ auswählen:")
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == SlotType.SPELL_STORAGE,
                        onClick = { selectedType = SlotType.SPELL_STORAGE },
                        label = { Text("Zauberspeicher") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    FilterChip(
                        selected = selectedType == SlotType.APPLICATUS,
                        onClick = { selectedType = SlotType.APPLICATUS },
                        label = { Text("Applicatus") },
                        enabled = canAddApplicatus,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == SlotType.LONG_DURATION,
                        onClick = { selectedType = SlotType.LONG_DURATION },
                        label = { Text("Langwirkend") },
                        modifier = Modifier.weight(1f)
                    )
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
                    
                    onConfirm(selectedType, volumePoints)
                },
                enabled = if (selectedType == SlotType.SPELL_STORAGE) {
                    val vp = volumePointsText.toIntOrNull() ?: 0
                    vp in 1..remainingVolumePoints
                } else {
                    true
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
