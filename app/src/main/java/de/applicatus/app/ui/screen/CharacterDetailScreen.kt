package de.applicatus.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.Character
import de.applicatus.app.data.model.Spell
import de.applicatus.app.data.model.SpellSlot
import de.applicatus.app.data.model.SpellSlotWithSpell
import de.applicatus.app.data.model.SlotType
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
    val isEditMode = viewModel.isEditMode
    
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var showEditCharacterDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character?.name ?: "Charakter") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleEditMode() }) {
                        Icon(
                            if (isEditMode) Icons.Default.Clear else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Bearbeitungsmodus beenden" else "Bearbeitungsmodus"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isEditMode) {
                FloatingActionButton(onClick = { showAddSlotDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Slot hinzufügen")
                }
            }
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
                CharacterAttributesCard(
                    character = char,
                    isEditMode = isEditMode,
                    onEditCharacter = { showEditCharacterDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Applicatus info if available
            character?.let { char ->
                if (char.hasApplicatus && isEditMode) {
                    ApplicatusInfoCard(character = char)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Volume points info
            if (isEditMode) {
                val remainingVolume = viewModel.getRemainingVolumePoints()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (remainingVolume >= 0) 
                            MaterialTheme.colorScheme.secondaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Verbleibende Volumenpunkte: $remainingVolume / 100",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Global modifier controls (only in usage mode)
            if (!isEditMode) {
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
            }
            
            // Spell slots
            Text(
                text = "Zauberslots",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(if (isEditMode) 12.dp else 8.dp)
            ) {
                items(spellSlots, key = { it.slot.id }) { slotWithSpell ->
                    if (isEditMode) {
                        SpellSlotCardEditMode(
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
                            onDeleteSlot = {
                                viewModel.removeSlot(slotWithSpell.slot)
                            }
                        )
                    } else {
                        SpellSlotCardUsageMode(
                            slotWithSpell = slotWithSpell,
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
    
    if (showAddSlotDialog) {
        AddSlotDialog(
            canAddApplicatus = viewModel.canAddApplicatusSlot(),
            remainingVolumePoints = viewModel.getRemainingVolumePoints(),
            onDismiss = { showAddSlotDialog = false },
            onConfirm = { slotType, volumePoints ->
                viewModel.addSlot(slotType, volumePoints)
                showAddSlotDialog = false
            }
        )
    }
    
    if (showEditCharacterDialog) {
        character?.let { char ->
            EditCharacterDialog(
                character = char,
                onDismiss = { showEditCharacterDialog = false },
                onConfirm = { updatedCharacter ->
                    viewModel.updateCharacter(updatedCharacter)
                    showEditCharacterDialog = false
                }
            )
        }
    }
}

@Composable
fun CharacterAttributesCard(
    character: Character,
    isEditMode: Boolean,
    onEditCharacter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eigenschaften",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isEditMode) {
                    IconButton(onClick = onEditCharacter) {
                        Icon(Icons.Default.Edit, contentDescription = "Eigenschaften bearbeiten")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("MU: ${character.mu}  KL: ${character.kl}  IN: ${character.inValue}  CH: ${character.ch}")
            Text("FF: ${character.ff}  GE: ${character.ge}  KO: ${character.ko}  KK: ${character.kk}")
        }
    }
}

@Composable
fun ApplicatusInfoCard(character: Character) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Applicatus",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("ZfW: ${character.applicatusZfw}  Modifikator: ${character.applicatusModifier}")
            Text("Probe: KL/IN/CH", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Usage Mode - Kompakte Ansicht
@Composable
fun SpellSlotCardUsageMode(
    slotWithSpell: SpellSlotWithSpell,
    onCastSpell: () -> Unit,
    onClearSlot: () -> Unit
) {
    val slot = slotWithSpell.slot
    val spell = slotWithSpell.spell
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (slot.slotType == SlotType.SPELL_STORAGE) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${slot.volumePoints}VP)",
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
                }
                
                if (slot.isFilled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ Gefüllt: ${slot.zfpStar} ZfP*",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
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
            
            // Actions
            if (spell != null) {
                if (!slot.isFilled) {
                    Button(onClick = onCastSpell) {
                        Text("Sprechen")
                    }
                } else {
                    IconButton(onClick = onClearSlot) {
                        Icon(Icons.Default.Clear, contentDescription = "Leeren")
                    }
                }
            }
        }
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
    onDeleteSlot: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Slot ${slot.slotNumber + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Chip(
                        onClick = {},
                        label = { 
                            Text(
                                if (slot.slotType == SlotType.APPLICATUS) 
                                    "Applicatus" 
                                else 
                                    "Speicher (${slot.volumePoints}VP)"
                            ) 
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSlotDialog(
    canAddApplicatus: Boolean,
    remainingVolumePoints: Int,
    onDismiss: () -> Unit,
    onConfirm: (SlotType, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(SlotType.SPELL_STORAGE) }
    var volumePointsText by remember { mutableStateOf("10") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Zauberslot") },
        text = {
            Column {
                Text("Slot-Typ auswählen:")
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == SlotType.SPELL_STORAGE,
                        onClick = { selectedType = SlotType.SPELL_STORAGE },
                        label = { Text("Zauberspeicher") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    FilterChip(
                        selected = selectedType == SlotType.APPLICATUS,
                        onClick = { selectedType = SlotType.APPLICATUS },
                        label = { Text("Applicatus") },
                        enabled = canAddApplicatus,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (selectedType == SlotType.SPELL_STORAGE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Volumenpunkte (1-100):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = volumePointsText,
                        onValueChange = { 
                            volumePointsText = it.filter { c -> c.isDigit() }
                        },
                        label = { Text("Volumenpunkte") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        text = "Verbleibend: $remainingVolumePoints VP",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (remainingVolumePoints >= (volumePointsText.toIntOrNull() ?: 0))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val volumePoints = if (selectedType == SlotType.SPELL_STORAGE) {
                        volumePointsText.toIntOrNull()?.coerceIn(1, 100) ?: 10
                    } else {
                        0
                    }
                    
                    // Prüfe Volumenpunkte-Limit
                    if (selectedType == SlotType.SPELL_STORAGE && volumePoints > remainingVolumePoints) {
                        return@TextButton
                    }
                    
                    onConfirm(selectedType, volumePoints)
                },
                enabled = if (selectedType == SlotType.SPELL_STORAGE) {
                    val vp = volumePointsText.toIntOrNull() ?: 0
                    vp in 1..remainingVolumePoints
                } else {
                    true
                }
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun EditCharacterDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var mu by remember { mutableStateOf(character.mu.toString()) }
    var kl by remember { mutableStateOf(character.kl.toString()) }
    var inValue by remember { mutableStateOf(character.inValue.toString()) }
    var ch by remember { mutableStateOf(character.ch.toString()) }
    var ff by remember { mutableStateOf(character.ff.toString()) }
    var ge by remember { mutableStateOf(character.ge.toString()) }
    var ko by remember { mutableStateOf(character.ko.toString()) }
    var kk by remember { mutableStateOf(character.kk.toString()) }
    var hasApplicatus by remember { mutableStateOf(character.hasApplicatus) }
    var applicatusZfw by remember { mutableStateOf(character.applicatusZfw.toString()) }
    var applicatusModifier by remember { mutableStateOf(character.applicatusModifier.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charakter bearbeiten") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Eigenschaften:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mu,
                            onValueChange = { mu = it.filter { c -> c.isDigit() } },
                            label = { Text("MU") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kl,
                            onValueChange = { kl = it.filter { c -> c.isDigit() } },
                            label = { Text("KL") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inValue,
                            onValueChange = { inValue = it.filter { c -> c.isDigit() } },
                            label = { Text("IN") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ch,
                            onValueChange = { ch = it.filter { c -> c.isDigit() } },
                            label = { Text("CH") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ff,
                            onValueChange = { ff = it.filter { c -> c.isDigit() } },
                            label = { Text("FF") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ge,
                            onValueChange = { ge = it.filter { c -> c.isDigit() } },
                            label = { Text("GE") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ko,
                            onValueChange = { ko = it.filter { c -> c.isDigit() } },
                            label = { Text("KO") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kk,
                            onValueChange = { kk = it.filter { c -> c.isDigit() } },
                            label = { Text("KK") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Applicatus:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasApplicatus,
                            onCheckedChange = { hasApplicatus = it }
                        )
                        Text("Charakter hat Applicatus")
                    }
                }
                if (hasApplicatus) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = applicatusZfw,
                                onValueChange = { applicatusZfw = it.filter { c -> c.isDigit() } },
                                label = { Text("Applicatus ZfW") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = applicatusModifier,
                                onValueChange = { applicatusModifier = it },
                                label = { Text("Applicatus Mod") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedCharacter = character.copy(
                        mu = mu.toIntOrNull() ?: 8,
                        kl = kl.toIntOrNull() ?: 8,
                        inValue = inValue.toIntOrNull() ?: 8,
                        ch = ch.toIntOrNull() ?: 8,
                        ff = ff.toIntOrNull() ?: 8,
                        ge = ge.toIntOrNull() ?: 8,
                        ko = ko.toIntOrNull() ?: 8,
                        kk = kk.toIntOrNull() ?: 8,
                        hasApplicatus = hasApplicatus,
                        applicatusZfw = applicatusZfw.toIntOrNull() ?: 0,
                        applicatusModifier = applicatusModifier.toIntOrNull() ?: 0
                    )
                    onConfirm(updatedCharacter)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
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
