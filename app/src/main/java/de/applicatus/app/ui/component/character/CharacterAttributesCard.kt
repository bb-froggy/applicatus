package de.applicatus.app.ui.component.character

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
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
    onDurationChange: (de.applicatus.app.data.model.spell.ApplicatusDuration) -> Unit,
    onModifierChange: (Int) -> Unit,
    onAspSavingPercentChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Applicatus",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "ZfW: ${character.applicatusZfw}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Probe: KL/FF/FF", style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mod:", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${if (character.applicatusModifier >= 0) "+" else ""}${character.applicatusModifier}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = { onModifierChange(character.applicatusModifier - 1) }) {
                                Icon(Icons.Filled.Remove, contentDescription = "Modifikator verringern")
                            }
                            OutlinedButton(onClick = { onModifierChange(character.applicatusModifier + 1) }) {
                                Icon(Icons.Filled.Add, contentDescription = "Modifikator erhöhen")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
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
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            // AsP-Kosten-Ersparnis
            Text(
                text = "AsP-Kosten-Ersparnis",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            val requiredZfw = (character.applicatusAspSavingPercent / 10) * 3
            if (requiredZfw > character.applicatusZfw) {
                Text(
                    text = "⚠ Benoetigt ZfW ${requiredZfw} (aktuell: ${character.applicatusZfw})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (character.applicatusAspSavingPercent > 0) {
                Text(
                    text = "✓ ZfW ${character.applicatusZfw} ausreichend (min. ${requiredZfw})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = character.applicatusAspSavingPercent.toFloat(),
                onValueChange = { onAspSavingPercentChange(it.toInt()) },
                valueRange = 0f..50f,
                steps = 4,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0%", style = MaterialTheme.typography.labelSmall)
                Text("10%", style = MaterialTheme.typography.labelSmall)
                Text("20%", style = MaterialTheme.typography.labelSmall)
                Text("30%", style = MaterialTheme.typography.labelSmall)
                Text("40%", style = MaterialTheme.typography.labelSmall)
                Text("50%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
