package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.talent.Talent
import de.applicatus.app.logic.PotionBrewer
import de.applicatus.app.ui.component.MagicalMasteryControl
import de.applicatus.app.ui.viewmodel.PotionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreservePotionDialog(
    potion: Potion,
    recipe: Recipe,
    character: Character,
    viewModel: PotionViewModel,
    currentDate: String,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var selectedTalent by remember { mutableStateOf<Talent?>(null) }
    var magicalMasteryAsp by remember { mutableStateOf(0) }
    var expandedTalent by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Prüfe ob bereits versucht wurde, den Trank haltbar zu machen
    if (potion.preservationAttempted) {
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
                        text = "Bereits haltbar gemacht",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Dieser Trank wurde bereits haltbar gemacht und kann nicht erneut konserviert werden.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
        return
    }
    
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
                        text = "Trank haltbar machen",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Divider()
                
                // Konfigurationsphase
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Haltbarkeitsdatum nur anzeigen, wenn es bekannt ist
                if (potion.shelfLifeKnown) {
                    Text(
                        text = "Aktuelles Verfallsdatum: ${potion.expiryDate}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Verfallsdatum unbekannt (nicht analysiert)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "Erstellt am: ${potion.createdDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                    
                    Divider()
                    
                    // Info-Box
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Haltbarmachen-Probe",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "• Erschwernis: +9\n" +
                                      "• Bei Erfolg: Haltbarkeit verdoppelt\n" +
                                      "• Bei Misserfolg: W6-Wurf entscheidet\n" +
                                      "  - 1-2: Doppelte Haltbarkeit\n" +
                                      "  - 3: 1,5x Haltbarkeit\n" +
                                      "  - 4: 1,5x Haltbarkeit, Qualität -1\n" +
                                      "  - 5: Keine Änderung, Qualität -1\n" +
                                      "  - 6-8: Trank wirkungslos (X)\n" +
                                      "  - 9-10: Trank misslungen (M)\n" +
                                      "  (Bei Patzer: W6 +4)\n\n" +
                                      "Hinweis: Du erfährst nicht, ob die Probe erfolgreich war. " +
                                      "Das Haltbarkeitsdatum muss erneut analysiert werden.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
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
                    
                    // Magisches Meisterhandwerk
                    if (isMagicalMastery && character.hasAe) {
                        MagicalMasteryControl(
                            skillValue = skillValue,
                            currentAsp = character.currentAe,
                            magicalMasteryAsp = magicalMasteryAsp,
                            onMagicalMasteryAspChange = { magicalMasteryAsp = it }
                        )
                    }
                    
                // Button zum Durchführen
                Button(
                    onClick = {
                        selectedTalent?.let { talent ->
                            isProcessing = true
                            scope.launch {
                                try {
                                    val result = PotionBrewer.preservePotion(
                                        character = character,
                                        potion = potion,
                                        talent = talent,
                                        magicalMasteryAsp = magicalMasteryAsp,
                                        currentDate = currentDate
                                    )
                                    
                                    // Trank aktualisieren: neues Verfallsdatum, Haltbarmachen markiert, Analysestatus zurücksetzen
                                    // Falls Qualität sich geändert hat, auch diese aktualisieren
                                    val updatedPotion = if (result.newQuality != null) {
                                        potion.copy(
                                            expiryDate = result.newExpiryDate,
                                            actualQuality = result.newQuality,
                                            preservationAttempted = true,
                                            shelfLifeKnown = false  // Spieler erfährt nicht, ob es funktioniert hat
                                        )
                                    } else {
                                        potion.copy(
                                            expiryDate = result.newExpiryDate,
                                            preservationAttempted = true,
                                            shelfLifeKnown = false  // Spieler erfährt nicht, ob es funktioniert hat
                                        )
                                    }
                                    viewModel.updatePotion(updatedPotion)
                                    
                                    // AE reduzieren (Magisches Meisterhandwerk)
                                    if (magicalMasteryAsp > 0) {
                                        viewModel.reduceAe(magicalMasteryAsp)
                                    }
                                    
                                    // Nachträglich eingesetzte AsP abziehen
                                    if (result.retroactiveAspUsed > 0) {
                                        viewModel.reduceAe(result.retroactiveAspUsed)
                                    }
                                    
                                    // Dialog schließen ohne Erfolgsmeldung
                                    onComplete()
                                } catch (e: Exception) {
                                    // Bei Fehler (z.B. bereits haltbar gemacht) nichts tun
                                    onDismiss()
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                    },
                    enabled = !isProcessing && selectedTalent != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Haltbar machen")
                    }
                }
            }
        }
    }
}

private fun getTalentDisplayName(talent: Talent): String {
    return when (talent) {
        Talent.ALCHEMY -> "Alchimie"
        Talent.COOKING_POTIONS -> "Kochen (Tränke)"
        else -> talent.name
    }
}
