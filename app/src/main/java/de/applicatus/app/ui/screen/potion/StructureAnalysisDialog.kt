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
    var astralEnergyCost by remember { mutableStateOf(0) }  // AE-Ausgabe für Magisches Meisterhandwerk
    var probeResult by remember { mutableStateOf<StructureAnalysisProbeResult?>(null) }
    var finalResult by remember { mutableStateOf<StructureAnalysisFinalResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    val recipeKnowledge by viewModel.getRecipeKnowledge(recipe.id).collectAsState(null)
    val isRecipeKnown = recipeKnowledge?.knowledgeLevel == RecipeKnowledgeLevel.UNDERSTOOD
    
    // Erleichterung: Maximum aus Intensitätsbestimmung oder vorheriger Strukturanalyse
    val intensityFacilitation = (potion.intensityDeterminationZfp + 1) / 2
    val bestFacilitation = maxOf(intensityFacilitation, potion.bestStructureAnalysisFacilitation)
    
    // Rezeptname nur anzeigen, wenn Spielleiter oder Rezept bekannt/verstanden
    val showRecipeName = character.isGameMaster || isRecipeKnown
    
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
                    text = if (showRecipeName) recipe.name else "Unbekannter Trank",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (bestFacilitation > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Erleichterung aus vorherigen Analysen:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "-$bestFacilitation",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (intensityFacilitation > 0 && potion.bestStructureAnalysisFacilitation > 0) {
                                Text(
                                    text = "Max. aus: Intensitätsbestimmung ($intensityFacilitation) oder vorheriger Strukturanalyse (${potion.bestStructureAnalysisFacilitation})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (intensityFacilitation > 0) {
                                Text(
                                    text = "Aus Intensitätsbestimmung (${potion.intensityDeterminationZfp} ZfP* / 2 aufgerundet)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "Aus vorheriger Strukturanalyse",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                // Zeige Probenergebnis an
                probeResult?.let { result ->
                    Divider()
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.success) {
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
                                text = if (result.success) "Probe erfolgreich!" else "Probe fehlgeschlagen",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Divider()
                            
                            // Eigenschaften und Fertigkeitswert
                            Text(
                                text = "Probe:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${result.attributes.first.first}/${result.attributes.second.first}/${result.attributes.third.first} " +
                                      "(${result.attributes.first.second}/${result.attributes.second.second}/${result.attributes.third.second})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "Fertigkeitswert: ${result.fertigkeitswert}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Divider()
                            
                            // Probenerschwernis-Breakdown
                            Text(
                                text = "Probenerschwernis:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "• Basis-Analyseerschwernis: +${result.baseDifficulty}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (result.facilitation > 0) {
                                Text(
                                    text = "• Erleichterung aus vorherigen Analysen: -${result.facilitation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (result.methodBonus > 0) {
                                val methodBonusText = when (selectedMethod) {
                                    StructureAnalysisMethod.ANALYS_SPELL -> "• Magiekunde-Bonus: -${result.methodBonus}"
                                    StructureAnalysisMethod.BY_SIGHT -> "• Sinnenschärfe-Bonus: -${result.methodBonus}"
                                    StructureAnalysisMethod.LABORATORY -> "• Wissenstalent-Bonus: -${result.methodBonus}"
                                }
                                Text(
                                    text = methodBonusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (acceptHarderProbe && selectedMethod == StructureAnalysisMethod.LABORATORY) {
                                Text(
                                    text = "• Trank nicht verbrauchen: +3",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            
                            // Modifikator
                            val modText = when {
                                result.difficulty > 0 -> "Gesamt: +${result.difficulty}"
                                result.difficulty < 0 -> "Gesamt: ${result.difficulty}"
                                else -> "Gesamt: ±0"
                            }
                            Text(
                                text = modText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    result.difficulty > 0 -> MaterialTheme.colorScheme.error
                                    result.difficulty < 0 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            
                            Divider()
                            
                            Text(
                                text = "Würfe: ${result.probeRolls.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "TaP*: ${result.tap}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            if (selectedMethod == StructureAnalysisMethod.BY_SIGHT && result.success) {
                                Text(
                                    text = "Effektiv: ${result.effectiveTap} TaP* (aufgerundet, max 8)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            
                            Divider()
                            
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                if (finalResult == null && probeResult == null) {
                    Divider()
                    
                    Text(
                        text = "Analysemethode wählen:",
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
                                    onClick = { selectedMethod = method }
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
                                onCheckedChange = { acceptHarderProbe = it }
                            )
                            Text(
                                text = "Probe um 3 erschweren (Trank wird nicht verbraucht)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    // AE-Ausgabe für Magisches Meisterhandwerk (nur bei Alchimie-Methoden und wenn Charakter AE hat)
                    val canUseMagicalMastery = character.hasAe && character.currentAe > 0 && 
                        ((selectedMethod == StructureAnalysisMethod.BY_SIGHT && character.alchemyIsMagicalMastery) ||
                         (selectedMethod == StructureAnalysisMethod.LABORATORY && character.alchemyIsMagicalMastery))
                    
                    if (canUseMagicalMastery) {
                        Divider()
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Magisches Meisterhandwerk",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                val maxAe = (character.alchemySkill + 1) / 2  // ⌈TaW/2⌉
                                Text(
                                    text = "AE ausgeben um TaW zu erhöhen (+2 TaW pro AE, max. ${character.alchemySkill * 2} TaW = $maxAe AE)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Aktuelle AE: ${character.currentAe}/${character.maxAe}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "AE ausgeben:",
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    IconButton(
                                        onClick = { if (astralEnergyCost > 0) astralEnergyCost-- },
                                        enabled = astralEnergyCost > 0
                                    ) {
                                        Text("-", style = MaterialTheme.typography.titleMedium)
                                    }
                                    
                                    Text(
                                        text = astralEnergyCost.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    
                                    IconButton(
                                        onClick = { astralEnergyCost++ },
                                        enabled = astralEnergyCost < character.currentAe && 
                                                 astralEnergyCost < (character.alchemySkill + 1) / 2  // Max ⌈TaW/2⌉
                                    ) {
                                        Text("+", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                
                                if (astralEnergyCost > 0) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    val effectiveTaw = minOf(character.alchemySkill + (astralEnergyCost * 2), character.alchemySkill * 2)
                                    Text(
                                        text = "Effektiver TaW: $effectiveTaw (Basis: ${character.alchemySkill}, Bonus: +${astralEnergyCost * 2})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            isAnalyzing = true
                            
                            // AE abziehen, wenn Magisches Meisterhandwerk verwendet wird
                            if (astralEnergyCost > 0) {
                                viewModel.adjustCurrentAe(characterId, -astralEnergyCost)
                            }
                            
                            val result = ElixirAnalyzer.performStructureAnalysisProbe(
                                character = character,
                                recipe = recipe,
                                method = selectedMethod,
                                currentFacilitation = bestFacilitation,
                                acceptHarderProbe = acceptHarderProbe,
                                astralEnergyCost = astralEnergyCost
                            )
                            probeResult = result
                            
                            // Nach der Probe direkt das finale Ergebnis berechnen
                            val final = ElixirAnalyzer.calculateStructureAnalysisResult(
                                totalTap = if (result.success) result.effectiveTap else 0,
                                actualQuality = potion.actualQuality,
                                currentIntensity = potion.intensityQuality,
                                isRecipeKnown = isRecipeKnown,
                                method = selectedMethod,
                                acceptHarderProbe = acceptHarderProbe
                            )
                            finalResult = final
                            
                            isAnalyzing = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAnalyzing && availableMethods.isNotEmpty()
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
                                )
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
