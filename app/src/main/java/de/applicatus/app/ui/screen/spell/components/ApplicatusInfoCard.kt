package de.applicatus.app.ui.screen.spell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicatusInfoCard(
    character: Character,
    onDurationChange: (de.applicatus.app.data.model.spell.ApplicatusDuration) -> Unit,
    onModifierChange: (Int) -> Unit,
    onAspSavingPercentChange: (Int) -> Unit,
    onExtendedDurationChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Erste Zeile: Titel, Extended Duration Chip, Modifikator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Titel und Probe-Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Applicatus",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "ZfW: ${character.applicatusZfw} | KL/FF/FF",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Extended Duration Chip
                FilterChip(
                    selected = character.applicatusExtendedDuration,
                    onClick = { onExtendedDurationChange(!character.applicatusExtendedDuration) },
                    label = {
                        Text("ZD verlängern -4", style = MaterialTheme.typography.labelSmall)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                // Modifikator mit kleineren Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (character.applicatusModifier >= 0) "+" else ""}${character.applicatusModifier}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        OutlinedButton(
                            onClick = { onModifierChange(character.applicatusModifier + 1) },
                            modifier = Modifier.height(24.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add, 
                                contentDescription = "Erhöhen",
                                modifier = Modifier.height(12.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { onModifierChange(character.applicatusModifier - 1) },
                            modifier = Modifier.height(24.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                Icons.Filled.Remove, 
                                contentDescription = "Verringern",
                                modifier = Modifier.height(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Dauer-Chips in einer Zeile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                de.applicatus.app.data.model.spell.ApplicatusDuration.entries.forEach { duration ->
                    FilterChip(
                        selected = character.applicatusDuration == duration,
                        onClick = { onDurationChange(duration) },
                        label = {
                            Text(
                                "${duration.displayName.replace("sonnenwende", "sw.")} +${duration.difficultyModifier}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // AsP-Kosten-Ersparnis mit kompakterem Layout
            val extendedBonus = if (character.applicatusExtendedDuration) 4 else 0
            val requiredZfw = (character.applicatusAspSavingPercent / 10) * 3 + character.applicatusDuration.difficultyModifier
            val requiredZfWmitHausregel = requiredZfw - extendedBonus
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kosten sparen",
                    style = MaterialTheme.typography.titleSmall
                )
                
                if (requiredZfWmitHausregel > character.applicatusZfw) {
                    Text(
                        text = "⚠ ZfW $requiredZfw ($requiredZfWmitHausregel mit Hausregel) nötig",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (requiredZfw > character.applicatusZfw) {
                    Text(
                        text = "✓ ZfW ${character.applicatusZfw} nur mit Hausregel ok",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (character.applicatusAspSavingPercent > 0) {
                    Text(
                        text = "✓ ZfW ${character.applicatusZfw} ok",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = character.applicatusAspSavingPercent.toFloat(),
                onValueChange = { onAspSavingPercentChange(it.toInt()) },
                valueRange = 0f..50f,
                steps = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
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