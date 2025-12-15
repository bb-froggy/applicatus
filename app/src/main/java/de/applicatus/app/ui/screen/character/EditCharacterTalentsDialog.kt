package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character

@Composable
fun EditCharacterTalentsDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var hasAlchemy by remember { mutableStateOf(character.hasAlchemy) }
    var alchemySkill by remember { mutableStateOf(character.alchemySkill.toString()) }
    var alchemyIsMagicalMastery by remember { mutableStateOf(character.alchemyIsMagicalMastery) }
    
    var hasCookingPotions by remember { mutableStateOf(character.hasCookingPotions) }
    var cookingPotionsSkill by remember { mutableStateOf(character.cookingPotionsSkill.toString()) }
    var cookingPotionsIsMagicalMastery by remember { mutableStateOf(character.cookingPotionsIsMagicalMastery) }
    
    var selfControlSkill by remember { mutableStateOf(character.selfControlSkill.toString()) }
    var sensoryAcuitySkill by remember { mutableStateOf(character.sensoryAcuitySkill.toString()) }
    var magicalLoreSkill by remember { mutableStateOf(character.magicalLoreSkill.toString()) }
    var herbalLoreSkill by remember { mutableStateOf(character.herbalLoreSkill.toString()) }
    
    var ritualKnowledgeValue by remember { mutableStateOf(character.ritualKnowledgeValue.toString()) }
    var hasKonzentrationsstärke by remember { mutableStateOf(character.hasKonzentrationsstärke) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.talents)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.alchemy))
                        Switch(
                            checked = hasAlchemy,
                            onCheckedChange = { hasAlchemy = it }
                        )
                    }
                }
                
                if (hasAlchemy) {
                    item {
                        OutlinedTextField(
                            value = alchemySkill,
                            onValueChange = { alchemySkill = it },
                            label = { Text("${stringResource(R.string.alchemy)} TaW") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (character.hasAe) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text("Magisches Meisterhandwerk")
                                Switch(
                                    checked = alchemyIsMagicalMastery,
                                    onCheckedChange = { alchemyIsMagicalMastery = it }
                                )
                            }
                        }
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.cooking_potions))
                        Switch(
                            checked = hasCookingPotions,
                            onCheckedChange = { hasCookingPotions = it }
                        )
                    }
                }
                
                if (hasCookingPotions) {
                    item {
                        OutlinedTextField(
                            value = cookingPotionsSkill,
                            onValueChange = { cookingPotionsSkill = it },
                            label = { Text("${stringResource(R.string.cooking_potions)} TaW") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (character.hasAe) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text("Magisches Meisterhandwerk")
                                Switch(
                                    checked = cookingPotionsIsMagicalMastery,
                                    onCheckedChange = { cookingPotionsIsMagicalMastery = it }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = selfControlSkill,
                        onValueChange = { selfControlSkill = it },
                        label = { Text("${stringResource(R.string.self_control)} TaW") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = sensoryAcuitySkill,
                        onValueChange = { sensoryAcuitySkill = it },
                        label = { Text("${stringResource(R.string.sensory_acuity)} TaW") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = magicalLoreSkill,
                        onValueChange = { magicalLoreSkill = it },
                        label = { Text("${stringResource(R.string.magical_lore)} TaW") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = herbalLoreSkill,
                        onValueChange = { herbalLoreSkill = it },
                        label = { Text("${stringResource(R.string.herbal_lore)} TaW") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Astrale Meditation
                if (character.hasAe) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    
                    item {
                        Text(
                            "Astrale Meditation",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = ritualKnowledgeValue,
                            onValueChange = { ritualKnowledgeValue = it },
                            label = { Text("Ritualkenntnis RkW") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("SF Konzentrationsstärke")
                            Switch(
                                checked = hasKonzentrationsstärke,
                                onCheckedChange = { hasKonzentrationsstärke = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        character.copy(
                            hasAlchemy = hasAlchemy,
                            alchemySkill = if (hasAlchemy) alchemySkill.toIntOrNull() ?: 0 else 0,
                            alchemyIsMagicalMastery = if (hasAlchemy && character.hasAe) alchemyIsMagicalMastery else false,
                            hasCookingPotions = hasCookingPotions,
                            cookingPotionsSkill = if (hasCookingPotions) cookingPotionsSkill.toIntOrNull() ?: 0 else 0,
                            cookingPotionsIsMagicalMastery = if (hasCookingPotions && character.hasAe) cookingPotionsIsMagicalMastery else false,
                            selfControlSkill = selfControlSkill.toIntOrNull() ?: 0,
                            sensoryAcuitySkill = sensoryAcuitySkill.toIntOrNull() ?: 0,
                            magicalLoreSkill = magicalLoreSkill.toIntOrNull() ?: 0,
                            herbalLoreSkill = herbalLoreSkill.toIntOrNull() ?: 0,
                            ritualKnowledgeValue = if (character.hasAe) ritualKnowledgeValue.toIntOrNull() ?: 0 else 0,
                            hasKonzentrationsstärke = if (character.hasAe) hasKonzentrationsstärke else false
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
