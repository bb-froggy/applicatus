package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.Substitution
import de.applicatus.app.data.model.potion.SubstitutionType
import de.applicatus.app.data.model.talent.Talent
import de.applicatus.app.logic.PotionBrewer
import de.applicatus.app.ui.viewmodel.PotionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewPotionDialog(
    character: Character,
    viewModel: PotionViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val knownRecipes by viewModel.getKnownRecipes().collectAsState(initial = emptyList())
    val availableTalents = viewModel.getAvailableBrewingTalents()
    
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var selectedTalent by remember { mutableStateOf<Talent?>(availableTalents.firstOrNull()) }
    var selectedLaboratory by remember { mutableStateOf(character.defaultLaboratory ?: Laboratory.ARCANE) }
    var voluntaryHandicap by remember { mutableStateOf(0) }
    var substitutions by remember { mutableStateOf(listOf<Substitution>()) }
    var magicalMasteryAsp by remember { mutableStateOf(0) }
    var astralCharging by remember { mutableStateOf(0) }
    
    var brewingResult by remember { mutableStateOf<PotionBrewer.BrewingResult?>(null) }
    var brewedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Talentwert und Magisches Meisterhandwerk ermitteln
    val skillValue = when (selectedTalent) {
        Talent.ALCHEMY -> character.alchemySkill
        Talent.COOKING_POTIONS -> character.cookingPotionsSkill
        else -> 0
    }
    
    val isMagicalMastery = when (selectedTalent) {
        Talent.ALCHEMY -> character.alchemyIsMagicalMastery
        Talent.COOKING_POTIONS -> character.cookingPotionsIsMagicalMastery
        else -> false
    }
    
    // Filter Rezepte basierend auf Talent
    val filteredRecipes = knownRecipes.filter { recipe ->
        // Kochen (Tränke) kann keine Alchimistenlabor-Rezepte brauen
        if (selectedTalent == Talent.COOKING_POTIONS) {
            recipe.lab != Laboratory.ALCHEMIST_LABORATORY
        } else {
            true
        }
    }
    
    // Wenn gewähltes Rezept nicht mehr verfügbar, zurücksetzen
    if (selectedRecipe != null && !filteredRecipes.contains(selectedRecipe)) {
        selectedRecipe = null
    }
    
    // Wenn gewähltes Labor bei Kochen (Tränke) nicht erlaubt, zurücksetzen
    if (selectedTalent == Talent.COOKING_POTIONS && selectedLaboratory == Laboratory.ALCHEMIST_LABORATORY) {
        selectedLaboratory = Laboratory.ARCANE
    }
    
    // Berechne Modifikatoren
    val laborModifier = selectedRecipe?.lab?.getBrewingModifier(selectedLaboratory) ?: 0
    val totalSubstitutionModifier = substitutions.sumOf { it.type.modifier }
    val totalModifier = laborModifier + (selectedRecipe?.brewingDifficulty ?: 0) + voluntaryHandicap + totalSubstitutionModifier
    
    // Berechne max freiwilligen Handicap
    val maxVoluntaryHandicap = selectedRecipe?.let { PotionBrewer.calculateMaxVoluntaryHandicap(it) } ?: 0
    
    // Berechne AsP-Kosten für beide Systeme
    val maxMagicalMasteryAsp = skillValue / 2  // Max TaW/2 AsP für Magisches Meisterhandwerk
    val astralChargingCost = PotionBrewer.calculateAspCostForQualityPoints(astralCharging)
    val totalAspCost = magicalMasteryAsp + astralChargingCost
    val maxAstralCharging = PotionBrewer.calculateMaxQualityPointsFromAsp(
        if (character.hasAe) character.currentAe - magicalMasteryAsp else 0
    )
    
    // Prüfe, ob Brauen möglich ist
    val canBrew = selectedRecipe?.let { 
        PotionBrewer.canBrew(it, selectedLaboratory) 
    } ?: false
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trank brauen") },
        text = {
            if (brewingResult != null) {
                // Ergebnis anzeigen
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (character.isGameMaster) {
                        // Spielleiter sieht alle Details
                        Text(
                            text = PotionBrewer.formatBrewingResult(brewingResult!!, true),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        // Spieler sieht Name, Aussehen und Kategorie (aber keine Details zu QP/Qualität)
                        Text(
                            text = "Brauprobe abgeschlossen!",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (brewedRecipe != null) {
                            Text(
                                text = "Gebrauter Trank: ${brewedRecipe!!.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gruppe: ${brewedRecipe!!.gruppe}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                // Brau-Einstellungen
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rezept-Auswahl
                    item {
                        Text("Rezept", style = MaterialTheme.typography.titleMedium)
                        if (filteredRecipes.isEmpty()) {
                            Text(
                                if (knownRecipes.isEmpty()) {
                                    "Keine bekannten Rezepte"
                                } else {
                                    "Keine passenden Rezepte für dieses Talent"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedRecipe?.name ?: "Bitte wählen...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    filteredRecipes.forEach { recipe ->
                                        DropdownMenuItem(
                                            text = { 
                                                Column {
                                                    Text(recipe.name)
                                                    Text(
                                                        "Schwierigkeit: +${recipe.brewingDifficulty}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedRecipe = recipe
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Talent-Auswahl
                    item {
                        Text("Talent", style = MaterialTheme.typography.titleMedium)
                        if (availableTalents.isEmpty()) {
                            Text(
                                "Keine Brau-Talente verfügbar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            availableTalents.forEach { talent ->
                                val talentValue = when (talent) {
                                    Talent.ALCHEMY -> character.alchemySkill
                                    Talent.COOKING_POTIONS -> character.cookingPotionsSkill
                                    else -> 0
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedTalent == talent,
                                        onClick = { selectedTalent = talent }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${talent.talentName} ($talentValue)")
                                }
                            }
                        }
                    }
                    
                    // Labor-Auswahl
                    item {
                        Text("Labor", style = MaterialTheme.typography.titleMedium)
                        
                        // Verfügbare Labore filtern basierend auf Talent
                        val availableLabs = if (selectedTalent == Talent.COOKING_POTIONS) {
                            // Kochen (Tränke) darf nur Archaisches Labor und Hexenküche verwenden
                            listOf(Laboratory.ARCANE, Laboratory.WITCHES_KITCHEN)
                        } else {
                            Laboratory.values().toList()
                        }
                        
                        availableLabs.forEach { lab ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedLaboratory == lab,
                                    onClick = { selectedLaboratory = lab }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(lab.displayName)
                            }
                        }
                        
                        if (selectedRecipe != null && !canBrew) {
                            Text(
                                "Labor nicht ausreichend!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Freiwilliger Handicap
                    item {
                        Text("Freiwillige Erschwernis", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "0 oder mindestens 2, maximal $maxVoluntaryHandicap",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { 
                                    voluntaryHandicap = when {
                                        voluntaryHandicap > 2 -> voluntaryHandicap - 1
                                        voluntaryHandicap == 2 -> 0
                                        else -> 0
                                    }
                                },
                                enabled = voluntaryHandicap > 0
                            ) {
                                Text("-")
                            }
                            Text(
                                text = "+$voluntaryHandicap",
                                modifier = Modifier.width(60.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(
                                onClick = { 
                                    voluntaryHandicap = when {
                                        voluntaryHandicap == 0 -> 2
                                        voluntaryHandicap < maxVoluntaryHandicap -> voluntaryHandicap + 1
                                        else -> voluntaryHandicap
                                    }
                                },
                                enabled = voluntaryHandicap < maxVoluntaryHandicap
                            ) {
                                Text("+")
                            }
                        }
                        if (voluntaryHandicap > 0) {
                            Text(
                                "→ +${voluntaryHandicap * 2} Qualitätspunkte bei Erfolg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Substitutionen
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Substitutionen", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = {
                                substitutions = substitutions + Substitution(SubstitutionType.EQUIVALENT)
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Substitution hinzufügen")
                            }
                        }
                        
                        if (substitutions.isNotEmpty()) {
                            Text(
                                "Gesamt: ${if (totalSubstitutionModifier >= 0) "+" else ""}$totalSubstitutionModifier",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (totalSubstitutionModifier > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    items(substitutions.withIndex().toList()) { (index, substitution) ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    var expanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = substitution.type.displayName,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            SubstitutionType.values().forEach { type ->
                                                DropdownMenuItem(
                                                    text = { 
                                                        Text("${type.displayName} (${if (type.modifier >= 0) "+" else ""}${type.modifier})")
                                                    },
                                                    onClick = {
                                                        substitutions = substitutions.toMutableList().apply {
                                                            this[index] = substitution.copy(type = type)
                                                        }
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    substitutions = substitutions.filterIndexed { i, _ -> i != index }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                                }
                            }
                        }
                    }
                    
                    // Magisches Meisterhandwerk (nur wenn verfügbar und AE vorhanden)
                    if (character.hasAe && isMagicalMastery) {
                        item {
                            Text("Magisches Meisterhandwerk (+2 TaW pro AsP)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Max ${maxMagicalMasteryAsp} AsP (TaW $skillValue → max ${skillValue * 2})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { 
                                        if (magicalMasteryAsp > 0) magicalMasteryAsp--
                                    },
                                    enabled = magicalMasteryAsp > 0
                                ) {
                                    Text("-")
                                }
                                Column(
                                    modifier = Modifier.width(120.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$magicalMasteryAsp AsP",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "+${magicalMasteryAsp * 2} TaW",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(
                                    onClick = { magicalMasteryAsp++ },
                                    enabled = magicalMasteryAsp < maxMagicalMasteryAsp && magicalMasteryAsp < character.currentAe
                                ) {
                                    Text("+")
                                }
                            }
                        }
                    }
                    
                    // Astrale Aufladung (immer verfügbar wenn AE vorhanden)
                    if (character.hasAe) {
                        item {
                            Text("Astrale Aufladung (Qualitätspunkte)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Verbleibende AsP: ${character.currentAe - magicalMasteryAsp}/${character.maxAe}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { 
                                        if (astralCharging > 0) astralCharging--
                                    },
                                    enabled = astralCharging > 0
                                ) {
                                    Text("-")
                                }
                                Column(
                                    modifier = Modifier.width(100.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$astralCharging QP",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "$astralChargingCost AsP",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(
                                    onClick = { astralCharging++ },
                                    enabled = astralCharging < maxAstralCharging && totalAspCost < character.currentAe
                                ) {
                                    Text("+")
                                }
                            }
                        }
                    }
                    
                    // Zusammenfassung
                    item {
                        Divider()
                        Text("Zusammenfassung", style = MaterialTheme.typography.titleMedium)
                        if (selectedRecipe != null) {
                            Text("Brauschwierigkeit: +${selectedRecipe!!.brewingDifficulty}")
                            if (laborModifier != 0) {
                                Text("Labor: ${if (laborModifier >= 0) "+" else ""}$laborModifier")
                            }
                            if (voluntaryHandicap > 0) {
                                Text("Freiwillig: +$voluntaryHandicap")
                            }
                            if (totalSubstitutionModifier != 0) {
                                Text("Substitutionen: ${if (totalSubstitutionModifier >= 0) "+" else ""}$totalSubstitutionModifier")
                            }
                            Text(
                                "Gesamt: ${if (totalModifier >= 0) "+" else ""}$totalModifier",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (totalModifier > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Fehler anzeigen
                    if (errorMessage != null) {
                        item {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (brewingResult != null) {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            } else {
                TextButton(
                    onClick = {
                        if (selectedRecipe != null && selectedTalent != null && canBrew) {
                            scope.launch {
                                try {
                                    val result = viewModel.brewPotion(
                                        recipe = selectedRecipe!!,
                                        talent = selectedTalent!!,
                                        laboratory = selectedLaboratory,
                                        voluntaryHandicap = voluntaryHandicap,
                                        substitutions = substitutions,
                                        magicalMasteryAsp = magicalMasteryAsp,
                                        astralCharging = astralCharging
                                    )
                                    brewingResult = result
                                    brewedRecipe = selectedRecipe
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Fehler beim Brauen"
                                }
                            }
                        }
                    },
                    enabled = selectedRecipe != null && selectedTalent != null && canBrew
                ) {
                    Text("Brauen")
                }
            }
        },
        dismissButton = {
            if (brewingResult == null) {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}
