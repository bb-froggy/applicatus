package de.applicatus.app.ui.component.character

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterAttributesCard(
    character: Character,
    isEditMode: Boolean,
    onEditCharacter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eigenschaften",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isEditMode) {
                    IconButton(onClick = onEditCharacter) {
                        Icon(Icons.Default.Edit, contentDescription = "Eigenschaften bearbeiten")
                    }
                }
            }
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PropertyItem("MU", character.mu)
                PropertyItem("KL", character.kl)
                PropertyItem("IN", character.inValue)
                PropertyItem("CH", character.ch)
                PropertyItem("FF", character.ff)
                PropertyItem("GE", character.ge)
                PropertyItem("KO", character.ko)
                PropertyItem("KK", character.kk)
            }
        }
    }
}

@Composable
private fun PropertyItem(name: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(name, style = MaterialTheme.typography.labelSmall)
        Text(value.toString(), style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicatusInfoCard(
    character: Character,
    onDurationChange: (de.applicatus.app.data.model.spell.ApplicatusDuration) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Applicatus",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("ZfW: ${character.applicatusZfw}", style = MaterialTheme.typography.bodyMedium)
            Text("Probe: KL/FF/FF", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Wirkungsdauer:", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                de.applicatus.app.data.model.spell.ApplicatusDuration.values().forEach { duration ->
                    FilterChip(
                        selected = character.applicatusDuration == duration,
                        onClick = { onDurationChange(duration) },
                        label = { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(duration.displayName, style = MaterialTheme.typography.labelSmall)
                                Text("+${duration.difficultyModifier}", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
