package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.talent.Talent
import de.applicatus.app.logic.PotionBrewer
import de.applicatus.app.ui.component.MagicalMasteryControl
import de.applicatus.app.ui.viewmodel.PotionViewModel
import kotlinx.coroutines.launch
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DilutionDialog(
    potion: Potion,
    recipe: Recipe,
    character: Character,
    viewModel: PotionViewModel,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var selectedTalent by remember { mutableStateOf<Talent?>(null) }
    var dilutionSteps by remember { mutableStateOf(1) }
    var magicalMasteryAsp by remember { mutableStateOf(0) }
    var expandedTalent by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Verfügbare Talente
    val availableTalents = buildList {
        if (character.alchemySkill > 0) add(Talent.ALCHEMY)
        if (character.cookingPotionsSkill > 0) add(Talent.COOKING_POTIONS)
    }
    
    // Automatische Vorauswahl wenn nur ein Talent verfügbar ist
    LaunchedEffect(availableTalents) {
        if (availableTalents.size == 1 && selectedTalent == null) {
            selectedTalent = availableTalents.first()
        }
    }
    
    // Magisches Meisterhandwerk verfügbar?
    val isMagicalMastery = selectedTalent?.let { talent ->
        when (talent) {
            Talent.ALCHEMY -> character.alchemyIsMagicalMastery
            Talent.COOKING_POTIONS -> character.cookingPotionsIsMagicalMastery
            else -> false
        }
    } ?: false
    
    // TaW für das gewählte Talent
    val skillValue = selectedTalent?.let { talent ->
        when (talent) {
            Talent.ALCHEMY -> character.alchemySkill
            Talent.COOKING_POTIONS -> character.cookingPotionsSkill
            else -> 0
        }
    } ?: 0
    
    // Erleichterung aus vorheriger Analyse berechnen
    val facilitationFromAnalysis = (potion.bestStructureAnalysisFacilitation + 1) / 2
    
    // Maximale Verdünnungsstufen: 5 (F→A ist sinnvoll, darüber hinaus wird es X)
    val maxDilutionSteps = 5
    
    // Prüfen ob Spieler die exakte Qualität kennt
    val knowsExactQuality = character.isGameMaster || 
                            potion.knownExactQuality != null ||
                            potion.knownQualityLevel == de.applicatus.app.data.model.potion.KnownQualityLevel.EXACT
    
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                    Text(
                        text = "Qualifizierte Verdünnung",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Divider()
                
                if (resultMessage == null) {
                    // Konfigurationsphase
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // Aktuelle Qualität nur für Spielleiter anzeigen
                    if (character.isGameMaster) {
                        Text(
                            text = "Aktuelle Qualität: ${potion.actualQuality.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Talent-Auswahl
                    ExposedDropdownMenuBox(
                        expanded = expandedTalent,
                        onExpandedChange = { expandedTalent = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTalent?.let { getTalentDisplayName(it) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Talent") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTalent) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedTalent,
                            onDismissRequest = { expandedTalent = false }
                        ) {
                            availableTalents.forEach { talent ->
                                DropdownMenuItem(
                                    text = { Text(getTalentDisplayName(talent)) },
                                    onClick = {
                                        selectedTalent = talent
                                        expandedTalent = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Verdünnungsstufen
                    Text(
                        text = "Verdünnungsstufen: $dilutionSteps",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { dilutionSteps = (dilutionSteps - 1).coerceAtLeast(1) },
                            enabled = dilutionSteps > 1
                        ) {
                            Text("-")
                        }
                        Text(
                            text = "$dilutionSteps Stufe${if (dilutionSteps > 1) "n" else ""}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Button(
                            onClick = { dilutionSteps = (dilutionSteps + 1).coerceAtMost(maxDilutionSteps) },
                            enabled = dilutionSteps < maxDilutionSteps
                        ) {
                            Text("+")
                        }
                    }
                    
                    // Neue Qualität anzeigen - nur wenn Spielleiter ODER Spieler die exakte Qualität kennt
                    if (knowsExactQuality) {
                        // Bei M bleibt es immer M
                        val newQualityIfSuccess = if (potion.actualQuality == PotionQuality.M) {
                            PotionQuality.M
                        } else {
                            val newQualityIndex = potion.actualQuality.ordinal - dilutionSteps
                            if (newQualityIndex < 0) {
                                PotionQuality.X
                            } else {
                                PotionQuality.values()[newQualityIndex]
                            }
                        }
                        
                        Text(
                            text = "Neue Qualität bei Erfolg: ${newQualityIfSuccess.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Neue Qualität: Unbekannt",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "Anzahl Tränke: ${dilutionSteps + 1}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Modifikatoren
                    Divider()
                    Text(
                        text = "Modifikatoren:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Brauschwierigkeit: +${recipe.brewingDifficulty}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (facilitationFromAnalysis > 0) {
                        Text(
                            text = "Erleichterung aus Analyse: -$facilitationFromAnalysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val totalModifier = recipe.brewingDifficulty - facilitationFromAnalysis
                    Text(
                        text = "Gesamt: ${if (totalModifier >= 0) "+" else ""}$totalModifier",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Magisches Meisterhandwerk
                    if (isMagicalMastery && character.hasAe) {
                        Divider()
                        MagicalMasteryControl(
                            skillValue = skillValue,
                            currentAsp = character.currentAe,
                            magicalMasteryAsp = magicalMasteryAsp,
                            onMagicalMasteryAspChange = { magicalMasteryAsp = it }
                        )
                    }
                    
                    Divider()
                    
                    // Buttons
                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val result = viewModel.dilutePotion(
                                        potion = potion,
                                        talent = selectedTalent!!,
                                        dilutionSteps = dilutionSteps,
                                        magicalMasteryAsp = magicalMasteryAsp
                                    )
                                    resultMessage = PotionBrewer.formatDilutionResult(result, character.isGameMaster)
                                } catch (e: Exception) {
                                    resultMessage = "Fehler: ${e.message}"
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedTalent != null && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Verdünnen")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Text("Abbrechen")
                    }
                } else {
                    // Ergebnisphase
                    Text(
                        text = "Ergebnis",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = resultMessage!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Divider()
                    
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fertig")
                    }
                }
            }
        }
    }
}

@Composable
private fun getTalentDisplayName(talent: Talent): String {
    return when (talent) {
        Talent.ALCHEMY -> "Alchimie"
        Talent.COOKING_POTIONS -> "Kochen (Tränke)"
        else -> talent.name
    }
}
