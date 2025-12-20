package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.herb.getAllGelaendekunden

/**
 * Dialog zum Bearbeiten der Geländekunde-Liste eines Charakters
 * 
 * Der Charakter kann mehrere Geländekunden auswählen, für die er Geländekunde besitzt.
 * Dies gibt -3 Bonus auf die Kräutersuche in allen entsprechenden Landschaften.
 */
@Composable
fun EditCharacterTerrainKnowledgeDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    // State für die ausgewählten Geländekunden (als Set für schnelle Lookup)
    val selectedGelaendekunden = remember { 
        mutableStateListOf<String>().apply { 
            addAll(character.gelaendekunde) 
        }
    }
    
    // Alle verfügbaren Geländekunden (10 DSA-Geländekunden)
    val availableGelaendekunden = remember {
        getAllGelaendekunden()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text(stringResource(R.string.terrain_knowledge))
                Text(
                    text = "Wähle Geländekunden aus (-3 Erleichterung)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(availableGelaendekunden) { gelaendekunde ->
                    val isSelected = selectedGelaendekunden.contains(gelaendekunde)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = gelaendekunde,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedGelaendekunden.add(gelaendekunde)
                                } else {
                                    selectedGelaendekunden.remove(gelaendekunde)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        character.copy(
                            gelaendekunde = selectedGelaendekunden.toList().sorted()
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
