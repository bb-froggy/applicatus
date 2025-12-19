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
import de.applicatus.app.data.model.herb.Landscape

/**
 * Dialog zum Bearbeiten der Geländekunde-Liste eines Charakters
 * 
 * Der Charakter kann mehrere Landschaften auswählen, für die er Geländekunde besitzt.
 * Dies gibt -3 Bonus auf die Kräutersuche in diesen Landschaften.
 */
@Composable
fun EditCharacterTerrainKnowledgeDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    // State für die ausgewählten Landschaften (als Set für schnelle Lookup)
    val selectedLandscapes = remember { 
        mutableStateListOf<String>().apply { 
            addAll(character.gelaendekunde) 
        }
    }
    
    // Alle verfügbaren Landschaften (außer SEA, da nicht relevant für Kräutersuche)
    val availableLandscapes = remember {
        Landscape.values()
            .filter { it != Landscape.SEA }
            .sortedBy { it.displayName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text(stringResource(R.string.terrain_knowledge))
                Text(
                    text = "Wähle Landschaften aus (-3 auf Kräutersuche)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(availableLandscapes) { landscape ->
                    val isSelected = selectedLandscapes.contains(landscape.displayName)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = landscape.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedLandscapes.add(landscape.displayName)
                                } else {
                                    selectedLandscapes.remove(landscape.displayName)
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
                            gelaendekunde = selectedLandscapes.toList().sorted()
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
