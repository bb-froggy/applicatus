package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.applicatus.app.data.model.potion.*
import de.applicatus.app.logic.*
import de.applicatus.app.ui.viewmodel.PotionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntensityDeterminationDialog(
    potion: Potion,
    recipe: Recipe,
    character: de.applicatus.app.data.model.character.Character,
    viewModel: PotionViewModel,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var result by remember { mutableStateOf<IntensityDeterminationResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Intensitätsbestimmung",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Divider()
                
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ODEM-Zauber",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "ZfW: ${character.odemZfw}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Eigenschaften: KL ${character.kl}, IN ${character.inValue}, IN ${character.inValue}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (result == null) {
                    Button(
                        onClick = {
                            isAnalyzing = true
                            val determinationResult = ElixirAnalyzer.determineIntensity(
                                character = character,
                                recipe = recipe,
                                actualQuality = potion.actualQuality
                            )
                            result = determinationResult
                            isAnalyzing = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAnalyzing
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("ODEM wirken")
                        }
                    }
                } else {
                    Divider()
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result!!.success) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (result!!.success) "Zauber erfolgreich!" else "Zauber fehlgeschlagen",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Text(
                                text = "Würfe: ${result!!.rolls.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "ZfP*: ${result!!.zfp}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Divider()
                            
                            if (result!!.success) {
                                val intensityText = when (result!!.intensityQuality) {
                                    IntensityQuality.WEAK -> "Schwach"
                                    IntensityQuality.STRONG -> "Stark"
                                    IntensityQuality.UNKNOWN -> "Unbekannt"
                                }
                                
                                Text(
                                    text = "Intensität: $intensityText",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Text(
                                text = result!!.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            val updatedPotion = potion.copy(
                                intensityQuality = result!!.intensityQuality,
                                intensityDeterminationZfp = result!!.zfp
                            )
                            viewModel.updatePotion(updatedPotion)
                            onComplete()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ergebnis speichern")
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Zurück")
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Schließen")
                    }
                }
            }
        }
    }
}
