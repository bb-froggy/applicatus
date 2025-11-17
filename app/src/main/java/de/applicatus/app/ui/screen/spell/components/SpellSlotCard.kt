package de.applicatus.app.ui.screen.spell.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.model.spell.SpellSlotWithSpell
import de.applicatus.app.data.model.spell.SlotType
import de.applicatus.app.logic.DerianDateCalculator

// Usage Mode - Kompakte Ansicht
@Composable
fun SpellSlotCardUsageMode(
    slotWithSpell: SpellSlotWithSpell,
    currentDate: String,
    isGameMaster: Boolean,
    showAnimation: Boolean,
    onCastSpell: () -> Unit,
    onClearSlot: () -> Unit,
    onAnimationEnd: () -> Unit = {}
) {
    val slot = slotWithSpell.slot
    val spell = slotWithSpell.spell
    
    var showExpiryWarningDialog by remember { mutableStateOf(false) }
    
    // Prüfe, ob Zauber abgelaufen ist
    val isExpired = slot.expiryDate?.let { expiryDate ->
        DerianDateCalculator.isSpellExpired(expiryDate, currentDate)
    } ?: false
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${slot.slotNumber + 1}. ",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = spell?.name ?: "Leer",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isExpired) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                        if (isExpired && slot.isFilled) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Abgelaufen",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (slot.slotType == SlotType.SPELL_STORAGE) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${slot.volumePoints}VP)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else if (slot.slotType == SlotType.LONG_DURATION) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(langwirkend)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    if (spell != null) {
                        Text(
                            text = "ZfW: ${slot.zfw} | Mod: ${slot.modifier}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (slot.variant.isNotBlank()) {
                            Text(
                                text = slot.variant,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (slot.slotType == SlotType.LONG_DURATION && slot.longDurationFormula.isNotBlank()) {
                            Text(
                                text = "Wirkdauer: ${slot.longDurationFormula}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    // Zeige Ablaufdatum, wenn vorhanden
                    if (slot.expiryDate != null && slot.isFilled) {
                        Text(
                            text = "Ablaufdatum: ${slot.expiryDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    if (slot.isFilled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isGameMaster) {
                            // Spielleiter sieht alles: ZfP* oder Patzer-Hinweis
                            if (slot.isBotched) {
                                Text(
                                    text = "✗ Verpatzt!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = "✓ Gefüllt: ${slot.zfpStar} ZfP*",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            // Spieler sieht immer nur "Gefüllt" (auch bei Patzer!)
                            // Er erfährt erst beim Auslösen, dass es verpatzt war
                            Text(
                                text = "✓ Gefüllt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Würfelergebnisse nur für Spielleiter
                    if (isGameMaster) {
                        if (slot.lastRollResult != null) {
                            Text(
                                text = slot.lastRollResult,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (slot.isFilled) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        
                        if (slot.applicatusRollResult != null) {
                            Text(
                                text = "Applicatus: ${slot.applicatusRollResult}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                
                // Actions
                if (spell != null) {
                    if (!slot.isFilled) {
                        Button(onClick = onCastSpell) {
                            Text("Sprechen")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (isExpired) {
                                    showExpiryWarningDialog = true
                                } else {
                                    onClearSlot()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Leeren")
                        }
                    }
                }
            }
            
            // Sternchen-Animation beim Einspeichern
            if (showAnimation) {
                de.applicatus.app.ui.component.SpellCastAnimation(
                    modifier = Modifier.align(Alignment.Center),
                    onAnimationEnd = onAnimationEnd
                )
            }
        }
    }
    
    // Warndialog für abgelaufene Zauber
    if (showExpiryWarningDialog) {
        AlertDialog(
            onDismissRequest = { showExpiryWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Zauber abgelaufen!") },
            text = {
                Text(
                    "Dieser Zauber ist seit ${slot.expiryDate} abgelaufen. " +
                    "Möchten Sie ihn trotzdem auslösen? Die Wirkung könnte unvorhersehbar sein."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExpiryWarningDialog = false
                        onClearSlot()
                    }
                ) {
                    Text("Ja, auslösen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryWarningDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

// Edit Mode - Ausführliche Ansicht
@Composable
fun SpellSlotCardEditMode(
    slotWithSpell: SpellSlotWithSpell,
    allSpells: List<Spell>,
    onSpellSelected: (Spell) -> Unit,
    onZfwChanged: (Int) -> Unit,
    onModifierChanged: (Int) -> Unit,
    onVariantChanged: (String) -> Unit,
    onDurationFormulaChanged: (String) -> Unit,
    onDeleteSlot: () -> Unit
) {
    val slot = slotWithSpell.slot
    val spell = slotWithSpell.spell
    var showSpellPicker by remember { mutableStateOf(false) }
    var zfwText by remember(slot.zfw) { mutableStateOf(slot.zfw.toString()) }
    var modifierText by remember(slot.modifier) { mutableStateOf(slot.modifier.toString()) }
    var variantText by remember(slot.variant) { mutableStateOf(slot.variant) }
    var durationFormulaText by remember(slot.longDurationFormula) { mutableStateOf(slot.longDurationFormula) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Slot ${slot.slotNumber + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Chip(
                        onClick = {},
                        label = {
                            val slotLabel = when (slot.slotType) {
                                SlotType.APPLICATUS -> "Applicatus"
                                SlotType.SPELL_STORAGE -> "Zauberspeicher (${slot.volumePoints}VP)"
                                SlotType.LONG_DURATION -> "Langwirkend"
                            }
                            Text(slotLabel)
                        }
                    )
                }
                IconButton(onClick = onDeleteSlot) {
                    Icon(Icons.Default.Delete, contentDescription = "Slot löschen")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Spell selection
            OutlinedButton(
                onClick = { showSpellPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(spell?.name ?: "Zauber auswählen")
            }
            
            if (spell != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Probe: ${spell.attribute1}/${spell.attribute2}/${spell.attribute3}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // ZfW, Modifier
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = zfwText,
                    onValueChange = {
                        zfwText = it.filter { c -> c.isDigit() }
                        it.toIntOrNull()?.let { value -> onZfwChanged(value) }
                    },
                    label = { Text("ZfW") },
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val current = modifierText.toIntOrNull() ?: 0
                            val newValue = current - 1
                            modifierText = newValue.toString()
                            onModifierChanged(newValue)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    OutlinedTextField(
                        value = modifierText,
                        onValueChange = {
                            modifierText = it
                            it.toIntOrNull()?.let { value -> onModifierChanged(value) }
                        },
                        label = { Text("Mod") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = modifierText.toIntOrNull() ?: 0
                            val newValue = current + 1
                            modifierText = newValue.toString()
                            onModifierChanged(newValue)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Mod +1", modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (slot.slotType == SlotType.LONG_DURATION) {
                OutlinedTextField(
                    value = durationFormulaText,
                    onValueChange = {
                        durationFormulaText = it
                        onDurationFormulaChanged(it)
                    },
                    label = { Text("Wirkdauer-Formel") },
                    placeholder = { Text("z. B. ZfP* Wochen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Formel + Einheit, z. B. 3*ZfP*+2 Tage oder 2W6+3 Monde.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            OutlinedTextField(
                value = variantText,
                onValueChange = {
                    variantText = it
                    onVariantChanged(it)
                },
                label = { Text("Variante/Notiz") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
    
    if (showSpellPicker) {
        SpellPickerDialog(
            spells = allSpells,
            onDismiss = { showSpellPicker = false },
            onSpellSelected = { selectedSpell ->
                onSpellSelected(selectedSpell)
                showSpellPicker = false
            }
        )
    }
}

@Composable
fun SpellPickerDialog(
    spells: List<Spell>,
    onDismiss: () -> Unit,
    onSpellSelected: (Spell) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSpells = remember(spells, searchQuery) {
        if (searchQuery.isBlank()) {
            spells
        } else {
            spells.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zauber auswählen") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Suchen...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    items(filteredSpells, key = { it.id }) { spell ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSpellSelected(spell) }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = spell.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${spell.attribute1}/${spell.attribute2}/${spell.attribute3}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun Chip(
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                label()
            }
        }
    }
}
