package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character

@Composable
fun CharacterEnergiesCard(
    character: Character,
    onAdjustLe: (Int) -> Unit,
    onAdjustAe: (Int) -> Unit,
    onAdjustKe: (Int) -> Unit,
    onRegeneration: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Erste Zeile: Überschriften und Regeneration Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LE Überschrift
                OutlinedCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.le_short),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                // AE Überschrift (nur wenn vorhanden)
                if (character.hasAe) {
                    OutlinedCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.ae_short),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // KE Überschrift (nur wenn vorhanden)
                if (character.hasKe) {
                    OutlinedCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.ke_short),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // Regeneration Button mit Icon
                FilledTonalButton(
                    onClick = onRegeneration,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NightsStay,
                        contentDescription = stringResource(R.string.regeneration),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Zweite Zeile: Werte und Anpassungs-Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LE Werte
                OutlinedCard(
                    modifier = Modifier.weight(1f)
                ) {
                    EnergyValueRow(
                        current = character.currentLe,
                        max = character.maxLe,
                        onAdjust = onAdjustLe,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                // AE Werte (nur wenn vorhanden)
                if (character.hasAe) {
                    OutlinedCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        EnergyValueRow(
                            current = character.currentAe,
                            max = character.maxAe,
                            onAdjust = onAdjustAe,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                // KE Werte (nur wenn vorhanden)
                if (character.hasKe) {
                    OutlinedCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        EnergyValueRow(
                            current = character.currentKe,
                            max = character.maxKe,
                            onAdjust = onAdjustKe,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                // Platzhalter für Button-Spalte
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EnergyValueRow(
    current: Int,
    max: Int,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Wert-Anzeige
        Text(
            text = "$current / $max",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Anpassungs-Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // -5 Button
            IconButton(
                onClick = { onAdjust(-5) },
                enabled = current > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Text("-5", style = MaterialTheme.typography.labelSmall)
            }
            
            // -1 Button
            IconButton(
                onClick = { onAdjust(-1) },
                enabled = current > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium)
            }
            
            // +1 Button
            IconButton(
                onClick = { onAdjust(1) },
                enabled = current < max,
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
            
            // +5 Button
            IconButton(
                onClick = { onAdjust(5) },
                enabled = current < max,
                modifier = Modifier.size(32.dp)
            ) {
                Text("+5", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
