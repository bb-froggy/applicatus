package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onRegeneration: () -> Unit,
    onAstralMeditation: () -> Unit = {},
    isEditMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEditMode) Modifier.clickable(onClick = onClick) else Modifier)
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
                
                // Astrale Meditation Button (nur wenn AE vorhanden)
                if (character.hasAe) {
                    FilledTonalButton(
                        onClick = onAstralMeditation,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Text("AM", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    // Platzhalter wenn keine AE
                    Spacer(modifier = Modifier.weight(1f))
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
                if (character.hasAe) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    // Zwei Platzhalter wenn keine AE (weil ein Button fehlt)
                    Spacer(modifier = Modifier.weight(1f))
                }
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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Wert-Anzeige
        Text(
            text = "$current / $max",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // 2x2 Button-Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Erste Zeile: -5 und -1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onAdjust(-5) },
                    enabled = current > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-5", style = MaterialTheme.typography.labelSmall)
                }
                
                Spacer(modifier = Modifier.width(2.dp))
                
                IconButton(
                    onClick = { onAdjust(-1) },
                    enabled = current > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            // Zweite Zeile: +1 und +5
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onAdjust(1) },
                    enabled = current < max,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.width(2.dp))
                
                IconButton(
                    onClick = { onAdjust(5) },
                    enabled = current < max,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+5", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
