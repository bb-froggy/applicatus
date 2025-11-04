package de.applicatus.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.Spell
import de.applicatus.app.data.model.SpellSlot
import de.applicatus.app.data.model.SpellSlotWithSpell
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    viewModel: CharacterDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val character by viewModel.character.collectAsState()
    val spellSlots by viewModel.spellSlots.collectAsState()
    val allSpells by viewModel.allSpells.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character?.name ?: "Charakter") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Character attributes
            character?.let { char ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Eigenschaften",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("MU: ${char.mu}  KL: ${char.kl}  IN: ${char.inValue}  CH: ${char.ch}")
                        Text("FF: ${char.ff}  GE: ${char.ge}  KO: ${char.ko}  KK: ${char.kk}")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Global modifier controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Globaler Modifikator:",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.updateAllModifiers(-1) }) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = { viewModel.updateAllModifiers(1) }) {
                        Icon(Icons.Default.Add, contentDescription = "Alle +1")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Spell slots
            Text(
                text = "Zauberslots",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(spellSlots, key = { it.slot.id }) { slotWithSpell ->
                    SpellSlotCard(
                        slotWithSpell = slotWithSpell,
                        allSpells = allSpells,
                        onSpellSelected = { spell ->
                            viewModel.updateSlotSpell(slotWithSpell.slot, spell.id)
                        },
                        onZfwChanged = { zfw ->
                            viewModel.updateSlotZfw(slotWithSpell.slot, zfw)
                        },
                        onModifierChanged = { modifier ->
                            viewModel.updateSlotModifier(slotWithSpell.slot, modifier)
                        },
                        onVariantChanged = { variant ->
                            viewModel.updateSlotVariant(slotWithSpell.slot, variant)
                        },
                        onCastSpell = {
                            slotWithSpell.spell?.let { spell ->
                                viewModel.castSpell(slotWithSpell.slot, spell)
                            }
                        },
                        onClearSlot = {
                            viewModel.clearSlot(slotWithSpell.slot)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SpellSlotCard(
    slotWithSpell: SpellSlotWithSpell,
    allSpells: List<Spell>,
    onSpellSelected: (Spell) -> Unit,
    onZfwChanged: (Int) -> Unit,
    onModifierChanged: (Int) -> Unit,
    onVariantChanged: (String) -> Unit,
    onCastSpell: () -> Unit,
    onClearSlot: () -> Unit
) {
    val slot = slotWithSpell.slot
    val spell = slotWithSpell.spell
    var showSpellPicker by remember { mutableStateOf(false) }
    var zfwText by remember(slot.zfw) { mutableStateOf(slot.zfw.toString()) }
    var modifierText by remember(slot.modifier) { mutableStateOf(slot.modifier.toString()) }
    var variantText by remember(slot.variant) { mutableStateOf(slot.variant) }
    
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
                Text(
                    text = "Slot ${slot.slotNumber + 1}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (slot.isFilled) {
                    Chip(
                        onClick = {},
                        label = { Text("Gefüllt: ${slot.zfpStar} ZfP*") }
                    )
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
            
            // ZfW, Modifier, Variant
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cast/Clear buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!slot.isFilled) {
                    Button(
                        onClick = onCastSpell,
                        modifier = Modifier.weight(1f),
                        enabled = spell != null
                    ) {
                        Text("Zauber einspeichern")
                    }
                } else {
                    Button(
                        onClick = onClearSlot,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Leeren")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Slot leeren")
                    }
                }
            }
            
            // Show last roll result
            if (slot.lastRollResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (slot.isFilled) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = slot.lastRollResult,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
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
