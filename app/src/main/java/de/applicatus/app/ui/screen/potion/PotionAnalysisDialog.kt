package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.applicatus.app.data.model.potion.*
import de.applicatus.app.ui.viewmodel.PotionViewModel

enum class AnalysisMode {
    INTENSITY,
    STRUCTURE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotionAnalysisDialog(
    potion: Potion,
    recipe: Recipe,
    character: de.applicatus.app.data.model.character.Character,
    characterId: Long,
    viewModel: PotionViewModel,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf<AnalysisMode?>(null) }
    
    when (selectedMode) {
        null -> {
            Dialog(onDismissRequest = onDismiss) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Trank analysieren",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        Divider()
                        
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Text(
                            text = "Wähle die Analysemethode:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        // ODEM ARCANUM - nur wenn Charakter Zauber beherrscht und AE hat
                        val canUseOdem = character.hasOdem && character.hasAe && character.maxAe > 0
                        Button(
                            onClick = { selectedMode = AnalysisMode.INTENSITY },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canUseOdem
                        ) {
                            Column {
                                Text("Intensitätsbestimmung (ODEM)")
                                if (!canUseOdem) {
                                    Text(
                                        text = when {
                                            !character.hasAe -> "Benötigt Astralenergie"
                                            !character.hasOdem -> "Benötigt ODEM ARCANUM"
                                            else -> "Nicht verfügbar"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        
                        // Strukturanalyse - benötigt Alchimie ODER ANALYS
                        val canUseStructureAnalysis = character.hasAlchemy || (character.hasAnalys && character.hasAe && character.maxAe > 0)
                        Button(
                            onClick = { selectedMode = AnalysisMode.STRUCTURE },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canUseStructureAnalysis
                        ) {
                            Column {
                                Text("Strukturanalyse (ANALYS/Sicht/Labor)")
                                if (!canUseStructureAnalysis) {
                                    Text(
                                        text = "Benötigt Alchimie oder ANALYS ARKANSTRUKTUR",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        
                        Divider()
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Abbrechen")
                        }
                    }
                }
            }
        }
        
        AnalysisMode.INTENSITY -> {
            IntensityDeterminationDialog(
                potion = potion,
                recipe = recipe,
                character = character,
                viewModel = viewModel,
                onDismiss = onDismiss,
                onBack = { selectedMode = null },
                onComplete = onDismiss
            )
        }
        
        AnalysisMode.STRUCTURE -> {
            StructureAnalysisDialog(
                potion = potion,
                recipe = recipe,
                character = character,
                characterId = characterId,
                viewModel = viewModel,
                onDismiss = onDismiss,
                onBack = { selectedMode = null },
                onComplete = onDismiss
            )
        }
    }
}
