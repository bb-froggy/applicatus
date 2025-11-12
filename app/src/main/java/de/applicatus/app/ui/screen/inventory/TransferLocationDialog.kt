package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.inventory.Location

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferLocationDialog(
    location: Location,
    groupMembers: List<Character>,
    currentCharacterId: Long,
    onDismiss: () -> Unit,
    onConfirm: (targetCharacterId: Long) -> Unit
) {
    // Filtere den aktuellen Charakter aus der Liste
    val availableMembers = groupMembers.filter { it.id != currentCharacterId }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("${location.name} übertragen") 
        },
        text = {
            Column {
                Text(
                    text = "Wähle den Zielcharakter aus, an den '${location.name}' übertragen werden soll:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (availableMembers.isEmpty()) {
                    Text(
                        text = "Keine anderen Charaktere in der Gruppe verfügbar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableMembers) { character ->
                            Card(
                                onClick = { onConfirm(character.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = character.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Kein Confirm-Button, da die Auswahl direkt über die Cards erfolgt
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
