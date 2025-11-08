package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.applicatus.app.data.model.potion.*
import de.applicatus.app.logic.*
import de.applicatus.app.ui.viewmodel.PotionViewModel

/**
 * Dialog für Strukturanalyse mit Proben-Serie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StructureAnalysisDialog(
    potion: Potion,
    recipe: Recipe,
    character: de.applicatus.app.data.model.character.Character,
    characterId: Long,
    viewModel: PotionViewModel,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var selectedMethod by remember { 
        mutableStateOf(
            when {
                character.hasAnalys && character.hasAe && character.maxAe > 0 -> StructureAnalysisMethod.ANALYS_SPELL
                character.hasAlchemy -> StructureAnalysisMethod.BY_SIGHT
                else -> StructureAnalysisMethod.BY_SIGHT  // Fallback auf Augenschein
            }
        )
    }
    
    var acceptHarderProbe by remember { mutableStateOf(false) }
    var accumulatedTap by remember { mutableStateOf(potion.accumulatedStructureAnalysisTap) }
    var probeNumber by remember { mutableStateOf(1) }
    var currentProbeResult by remember { mutableStateOf<StructureAnalysisProbeResult?>(null) }
    var finalResult by remember { mutableStateOf<StructureAnalysisFinalResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    val recipeKnowledge by viewModel.getRecipeKnowledge(recipe.id).collectAsState(null)
    val isRecipeKnown = recipeKnowledge?.knowledgeLevel == RecipeKnowledgeLevel.UNDERSTOOD
    
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
                    text = "Strukturanalyse",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Divider()
                
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (potion.bestStructureAnalysisFacilitation > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Erleichterung aus vorheriger Analyse:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "-${potion.bestStructureAnalysisFacilitation}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                if (accumulatedTap > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Akkumulierte TaP*:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "$accumulatedTap",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Zeige Probenergebnisse an (auch bei fehlgeschlagener Analyse)
                currentProbeResult?.let { probeResult ->
                    Divider()
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (probeResult.success) {
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
                                text = if (probeResult.success) "Probe erfolgreich!" else "Probe fehlgeschlagen",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Divider()
                            
                            // Eigenschaften und Fertigkeitswert
                            Text(
                                text = "Probe:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${probeResult.attributes.first.first}/${probeResult.attributes.second.first}/${probeResult.attributes.third.first} " +
                                      "(${probeResult.attributes.first.second}/${probeResult.attributes.second.second}/${probeResult.attributes.third.second})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "Fertigkeitswert: ${probeResult.fertigkeitswert}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            // Modifikator
                            val modText = when {
                                probeResult.difficulty > 0 -> "Erschwernis: +${probeResult.difficulty}"
                                probeResult.difficulty < 0 -> "Erleichterung: ${probeResult.difficulty}"
                                else -> "Modifikator: ±0"
                            }
                            Text(
                                text = modText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    probeResult.difficulty > 0 -> MaterialTheme.colorScheme.error
                                    probeResult.difficulty < 0 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            
                            Divider()
                            
                            Text(
                                text = "Würfe: ${probeResult.probeRolls.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "TaP*: ${probeResult.tap}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            if (selectedMethod == StructureAnalysisMethod.BY_SIGHT && probeResult.success) {
                                Text(
                                    text = "Effektiv: ${probeResult.effectiveTap} TaP* (halbiert)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            
                            Divider()
                            
                            Text(
                                text = probeResult.message,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                if (finalResult == null) {
                    // Buttons für weitere Proben (nur wenn Analyse noch nicht abgeschlossen)
                    currentProbeResult?.let { probeResult ->
                        if (probeResult.canContinue && probeResult.success) {
                            Button(
                                onClick = {
                                    probeNumber++
                                    currentProbeResult = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Weitere Probe durchführen")
                            }
                        }
                        
                        if (probeResult.success) {
                            OutlinedButton(
                                onClick = {
                                    val final = ElixirAnalyzer.calculateFinalStructureAnalysisResult(
                                        totalAccumulatedTap = accumulatedTap,
                                        actualQuality = potion.actualQuality,
                                        currentIntensity = potion.intensityQuality,
                                        isRecipeKnown = isRecipeKnown,
                                        method = selectedMethod,
                                        acceptHarderProbe = acceptHarderProbe
                                    )
                                    finalResult = final
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Analyse abschließen")
                            }
                        }
                    }
                    
                    Divider()
                    
                    Text(
                        text = "Probe ${probeNumber}:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    val availableMethods = buildList {
                        // ANALYS benötigt den Zauber UND Astralenergie
                        if (character.hasAnalys && character.hasAe && character.maxAe > 0) {
                            add(StructureAnalysisMethod.ANALYS_SPELL)
                        }
                        if (character.hasAlchemy) {
                            add(StructureAnalysisMethod.BY_SIGHT)
                            add(StructureAnalysisMethod.LABORATORY)
                        }
                    }
                    
                    if (availableMethods.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Keine Analysemethode verfügbar",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Der Charakter benötigt entweder Alchimie oder ANALYS ARKANSTRUKTUR (mit AE)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        availableMethods.forEach { method ->
                            val methodText = when (method) {
                                StructureAnalysisMethod.ANALYS_SPELL -> "ANALYS (${character.analysZfw} ZfW)"
                                StructureAnalysisMethod.BY_SIGHT -> "Augenschein (${character.alchemySkill} TaW, ½ TaP*, max 8)"
                                StructureAnalysisMethod.LABORATORY -> "Labor (${character.alchemySkill} TaW)"
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedMethod == method,
                                    onClick = { selectedMethod = method },
                                    enabled = currentProbeResult == null
                                )
                                Text(methodText)
                            }
                        }
                    }
                    
                    if (selectedMethod == StructureAnalysisMethod.LABORATORY) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = acceptHarderProbe,
                                onCheckedChange = { acceptHarderProbe = it },
                                enabled = currentProbeResult == null
                            )
                            Text(
                                text = "Probe um 3 erschweren (Trank wird nicht verbraucht)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    if (currentProbeResult == null) {
                        Button(
                            onClick = {
                                isAnalyzing = true
                                val probeResult = ElixirAnalyzer.performStructureAnalysisProbe(
                                    character = character,
                                    recipe = recipe,
                                    method = selectedMethod,
                                    currentFacilitation = potion.bestStructureAnalysisFacilitation,
                                    probeNumber = probeNumber,
                                    acceptHarderProbe = acceptHarderProbe
                                )
                                currentProbeResult = probeResult
                                
                                if (probeResult.success) {
                                    accumulatedTap += probeResult.effectiveTap
                                }
                                
                                if (!probeResult.canContinue || !probeResult.success) {
                                    val final = ElixirAnalyzer.calculateFinalStructureAnalysisResult(
                                        totalAccumulatedTap = accumulatedTap,
                                        actualQuality = potion.actualQuality,
                                        currentIntensity = potion.intensityQuality,
                                        isRecipeKnown = isRecipeKnown,
                                        method = selectedMethod,
                                        acceptHarderProbe = acceptHarderProbe
                                    )
                                    finalResult = final
                                }
                                
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
                                Text("Probe durchführen")
                            }
                        }
                    }
                }
                
                finalResult?.let { final ->
                    Divider()
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Analyse abgeschlossen!",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Text(
                                text = "Gesamt: ${final.totalTap} TaP*",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Divider()
                            
                            Text(
                                text = final.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            if (final.potionConsumed) {
                                Divider()
                                Text(
                                    text = "⚠️ Der Trank wurde bei der Laboranalyse verbraucht!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            val updatedPotion = potion.copy(
                                categoryKnown = final.categoryKnown,
                                knownQualityLevel = final.knownQualityLevel,
                                intensityQuality = if (final.intensityQuality != IntensityQuality.UNKNOWN) 
                                    final.intensityQuality else potion.intensityQuality,
                                refinedQuality = final.refinedQuality,
                                knownExactQuality = final.knownExactQuality,
                                shelfLifeKnown = final.shelfLifeKnown,
                                bestStructureAnalysisFacilitation = maxOf(
                                    potion.bestStructureAnalysisFacilitation,
                                    final.newFacilitation
                                ),
                                accumulatedStructureAnalysisTap = 0
                            )
                            
                            if (final.potionConsumed) {
                                viewModel.deletePotion(potion)
                            } else {
                                viewModel.updatePotion(updatedPotion)
                            }
                            
                            if (final.recipeKnown) {
                                viewModel.setRecipeKnowledge(
                                    characterId = characterId,
                                    recipeId = recipe.id,
                                    knowledgeLevel = RecipeKnowledgeLevel.UNDERSTOOD
                                )
                            }
                            
                            onComplete()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK")
                    }
                }
                
                // Zurück und Schließen nur anzeigen, wenn Analyse noch nicht abgeschlossen
                if (finalResult == null) {
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
}
