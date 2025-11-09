package de.applicatus.app.ui.screen.spell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.model.spell.SpellSlotWithSpell
import de.applicatus.app.data.model.spell.SlotType
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellStorageScreen(
    viewModel: CharacterDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNearbySync: (Long, String) -> Unit = { _, _ -> }
) {
    val character by viewModel.character.collectAsState()
    val spellSlots by viewModel.spellSlots.collectAsState()
    val allSpells by viewModel.allSpells.collectAsState()
    val isEditMode = viewModel.isEditMode
    val spellCastMessage = viewModel.spellCastMessage
    val context = LocalContext.current
    
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var showEditCharacterDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    
    // File pickers
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportCharacterToFile(context, it) }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCharacterFromFile(context, it) }
    }
    
    // Export State Dialog
    when (val state = viewModel.exportState) {
        is CharacterDetailViewModel.ExportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetExportState() },
                title = { Text("Erfolg") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetExportState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is CharacterDetailViewModel.ExportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetExportState() },
                title = { Text("Fehler") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetExportState() }) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }
    
    // Zauber-Auslösungs-Meldung Dialog
    spellCastMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSpellCastMessage() },
            title = { Text("Zauber ausgelöst") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSpellCastMessage() }) {
                    Text("OK")
                }
            }
        )
    }
    
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
                    
                    // More Menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mehr")
                        }
                        
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Als JSON exportieren") },
                                leadingIcon = {
                                    Icon(Icons.Default.ArrowForward, null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    exportLauncher.launch("${character?.name ?: "character"}.json")
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("JSON importieren") },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                            )
                            
                            Divider()
                            
                            DropdownMenuItem(
                                text = { Text("Nearby Sync") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    character?.let { char ->
                                        onNavigateToNearbySync(char.id, char.name)
                                    }
                                }
                            )
                        }
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
                            isGameMaster = character?.isGameMaster ?: false,
                            showAnimation = viewModel.showSpellAnimation && viewModel.animatingSlotId == slotWithSpell.slot.id,
                            onCastSpell = {
                                slotWithSpell.spell?.let { spell ->
                                    viewModel.castSpell(slotWithSpell.slot, spell)
                                }
                            },
                            onClearSlot = {
                                viewModel.clearSlot(slotWithSpell.slot)
                            },
                            onAnimationEnd = {
                                viewModel.hideSpellAnimation()
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
            Text("Probe: KL/FF/FF", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Usage Mode - Kompakte Ansicht
@Composable
fun SpellSlotCardUsageMode(
    slotWithSpell: SpellSlotWithSpell,
    isGameMaster: Boolean,
    showAnimation: Boolean,
    onCastSpell: () -> Unit,
    onClearSlot: () -> Unit,
    onAnimationEnd: () -> Unit = {}
) {
    val slot = slotWithSpell.slot
    val spell = slotWithSpell.spell
    
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
                        IconButton(onClick = onClearSlot) {
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
                        singleLine = true,
                        isError = (volumePointsText.toIntOrNull() ?: 0) < 1
                    )
                    if ((volumePointsText.toIntOrNull() ?: 0) < 1) {
                        Text(
                            text = "Mindestens 1 VP erforderlich",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
    var group by remember { mutableStateOf(character.group) }
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
    
    var maxLe by remember { mutableStateOf(character.maxLe.toString()) }
    var hasAe by remember { mutableStateOf(character.hasAe) }
    var maxAe by remember { mutableStateOf(character.maxAe.toString()) }
    var hasKe by remember { mutableStateOf(character.hasKe) }
    var maxKe by remember { mutableStateOf(character.maxKe.toString()) }
    var leRegenBonus by remember { mutableStateOf(character.leRegenBonus.toString()) }
    var aeRegenBonus by remember { mutableStateOf(character.aeRegenBonus.toString()) }
    var hasMasteryRegeneration by remember { mutableStateOf(character.hasMasteryRegeneration) }
    
    // Alchimie & Zauber
    var hasAlchemy by remember { mutableStateOf(character.hasAlchemy) }
    var alchemySkill by remember { mutableStateOf(character.alchemySkill.toString()) }
    var alchemyIsMagicalMastery by remember { mutableStateOf(character.alchemyIsMagicalMastery) }
    var hasCookingPotions by remember { mutableStateOf(character.hasCookingPotions) }
    var cookingPotionsSkill by remember { mutableStateOf(character.cookingPotionsSkill.toString()) }
    var cookingPotionsIsMagicalMastery by remember { mutableStateOf(character.cookingPotionsIsMagicalMastery) }
    var hasOdem by remember { mutableStateOf(character.hasOdem) }
    var odemZfw by remember { mutableStateOf(character.odemZfw.toString()) }
    var hasAnalys by remember { mutableStateOf(character.hasAnalys) }
    var analysZfw by remember { mutableStateOf(character.analysZfw.toString()) }
    
    // Zusätzliche Talente für Analyse
    var selfControlSkill by remember { mutableStateOf(character.selfControlSkill.toString()) }
    var sensoryAcuitySkill by remember { mutableStateOf(character.sensoryAcuitySkill.toString()) }
    var magicalLoreSkill by remember { mutableStateOf(character.magicalLoreSkill.toString()) }
    var herbalLoreSkill by remember { mutableStateOf(character.herbalLoreSkill.toString()) }
    
    // Spieler/Spielleiter-Modus
    var isGameMaster by remember { mutableStateOf(character.isGameMaster) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charakter bearbeiten") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Allgemein:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    OutlinedTextField(
                        value = group,
                        onValueChange = { group = it },
                        label = { Text("Gruppe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                    Text("Energien:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    OutlinedTextField(
                        value = maxLe,
                        onValueChange = { maxLe = it.filter { c -> c.isDigit() } },
                        label = { Text("Max LE") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = leRegenBonus,
                        onValueChange = { 
                            val num = it.filter { c -> c.isDigit() || c == '-' }
                            leRegenBonus = num
                        },
                        label = { Text("LE-Regenerationsbonus (-3 bis +3)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasAe,
                            onCheckedChange = { hasAe = it }
                        )
                        Text("Hat Astralenergie")
                    }
                }
                if (hasAe) {
                    item {
                        OutlinedTextField(
                            value = maxAe,
                            onValueChange = { maxAe = it.filter { c -> c.isDigit() } },
                            label = { Text("Max AE") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = aeRegenBonus,
                            onValueChange = { 
                                val num = it.filter { c -> c.isDigit() || c == '-' }
                                aeRegenBonus = num
                            },
                            label = { Text("AE-Regenerationsbonus (-3 bis +3)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = hasMasteryRegeneration,
                                onCheckedChange = { hasMasteryRegeneration = it }
                            )
                            Text("Meisterliche Regeneration")
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasKe,
                            onCheckedChange = { hasKe = it }
                        )
                        Text("Hat Karmaenergie")
                    }
                }
                if (hasKe) {
                    item {
                        OutlinedTextField(
                            value = maxKe,
                            onValueChange = { maxKe = it.filter { c -> c.isDigit() } },
                            label = { Text("Max KE") },
                            modifier = Modifier.fillMaxWidth()
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
                
                // Alchimie & Kochkunst
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Alchimie:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasAlchemy,
                            onCheckedChange = { hasAlchemy = it }
                        )
                        Text("Beherrscht Alchimie")
                    }
                }
                if (hasAlchemy) {
                    item {
                        OutlinedTextField(
                            value = alchemySkill,
                            onValueChange = { alchemySkill = it.filter { c -> c.isDigit() } },
                            label = { Text("Alchimie TaW (0-18)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Magisches Meisterhandwerk für Alchimie (nur bei AE)
                    if (hasAe) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = alchemyIsMagicalMastery,
                                    onCheckedChange = { alchemyIsMagicalMastery = it }
                                )
                                Text("Magisches Meisterhandwerk", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasCookingPotions,
                            onCheckedChange = { hasCookingPotions = it }
                        )
                        Text("Beherrscht Kochen (Tränke)")
                    }
                }
                if (hasCookingPotions) {
                    item {
                        OutlinedTextField(
                            value = cookingPotionsSkill,
                            onValueChange = { cookingPotionsSkill = it.filter { c -> c.isDigit() } },
                            label = { Text("Kochen (Tränke) TaW (0-18)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Magisches Meisterhandwerk für Kochen (nur bei AE)
                    if (hasAe) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = cookingPotionsIsMagicalMastery,
                                    onCheckedChange = { cookingPotionsIsMagicalMastery = it }
                                )
                                Text("Magisches Meisterhandwerk", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                
                // Zusätzliche Talente für Analyse (immer sichtbar)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analyse-Talente:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    OutlinedTextField(
                        value = selfControlSkill,
                        onValueChange = { selfControlSkill = it.filter { c -> c.isDigit() } },
                        label = { Text("Selbstbeherrschung TaW (0-18)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = sensoryAcuitySkill,
                        onValueChange = { sensoryAcuitySkill = it.filter { c -> c.isDigit() } },
                        label = { Text("Sinnenschärfe TaW (0-18)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (hasAe || hasAlchemy) {
                    item {
                        OutlinedTextField(
                            value = magicalLoreSkill,
                            onValueChange = { magicalLoreSkill = it.filter { c -> c.isDigit() } },
                            label = { Text("Magiekunde TaW (0-18)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (hasAlchemy) {
                    item {
                        OutlinedTextField(
                            value = herbalLoreSkill,
                            onValueChange = { herbalLoreSkill = it.filter { c -> c.isDigit() } },
                            label = { Text("Pflanzenkunde TaW (0-18)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Analyse-Zauber (nur für magisch Begabte)
                if (hasAe) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Analyse-Zauber:", style = MaterialTheme.typography.titleSmall)
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = hasOdem,
                                onCheckedChange = { hasOdem = it }
                            )
                            Text("Beherrscht ODEM ARCANUM")
                        }
                    }
                    if (hasOdem) {
                        item {
                            OutlinedTextField(
                                value = odemZfw,
                                onValueChange = { odemZfw = it.filter { c -> c.isDigit() } },
                                label = { Text("ODEM ARCANUM ZfW (0-18)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = hasAnalys,
                                onCheckedChange = { hasAnalys = it }
                            )
                            Text("Beherrscht ANALYS ARKANSTRUKTUR")
                        }
                    }
                    if (hasAnalys) {
                        item {
                            OutlinedTextField(
                                value = analysZfw,
                                onValueChange = { analysZfw = it.filter { c -> c.isDigit() } },
                                label = { Text("ANALYS ZfW (0-18)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Spieler/Spielleiter-Modus
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Spielmodus:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isGameMaster,
                            onCheckedChange = { isGameMaster = it }
                        )
                        Text("Spielleiter-Modus (zeigt alle Informationen)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newMaxLe = maxLe.toIntOrNull() ?: character.maxLe
                    val newMaxAe = if (hasAe) maxAe.toIntOrNull() ?: character.maxAe else 0
                    val newMaxKe = if (hasKe) maxKe.toIntOrNull() ?: character.maxKe else 0
                    
                    val updatedCharacter = character.copy(
                        group = group,
                        mu = mu.toIntOrNull() ?: 8,
                        kl = kl.toIntOrNull() ?: 8,
                        inValue = inValue.toIntOrNull() ?: 8,
                        ch = ch.toIntOrNull() ?: 8,
                        ff = ff.toIntOrNull() ?: 8,
                        ge = ge.toIntOrNull() ?: 8,
                        ko = ko.toIntOrNull() ?: 8,
                        kk = kk.toIntOrNull() ?: 8,
                        maxLe = newMaxLe,
                        currentLe = character.currentLe.coerceAtMost(newMaxLe),
                        leRegenBonus = (leRegenBonus.toIntOrNull() ?: 0).coerceIn(-3, 3),
                        hasAe = hasAe,
                        maxAe = newMaxAe,
                        currentAe = if (hasAe) character.currentAe.coerceAtMost(newMaxAe) else 0,
                        aeRegenBonus = if (hasAe) (aeRegenBonus.toIntOrNull() ?: 0).coerceIn(-3, 3) else 0,
                        hasMasteryRegeneration = if (hasAe) hasMasteryRegeneration else false,
                        hasKe = hasKe,
                        maxKe = newMaxKe,
                        currentKe = if (hasKe) character.currentKe.coerceAtMost(newMaxKe) else 0,
                        hasApplicatus = hasApplicatus,
                        applicatusZfw = applicatusZfw.toIntOrNull() ?: 0,
                        applicatusModifier = applicatusModifier.toIntOrNull() ?: 0,
                        hasAlchemy = hasAlchemy,
                        alchemySkill = if (hasAlchemy) (alchemySkill.toIntOrNull() ?: 0) else 0,
                        alchemyIsMagicalMastery = if (hasAlchemy && hasAe) alchemyIsMagicalMastery else false,
                        hasCookingPotions = hasCookingPotions,
                        cookingPotionsSkill = if (hasCookingPotions) (cookingPotionsSkill.toIntOrNull() ?: 0) else 0,
                        cookingPotionsIsMagicalMastery = if (hasCookingPotions && hasAe) cookingPotionsIsMagicalMastery else false,
                        hasOdem = if (hasAe) hasOdem else false,
                        odemZfw = if (hasAe && hasOdem) (odemZfw.toIntOrNull() ?: 0) else 0,
                        hasAnalys = if (hasAe) hasAnalys else false,
                        analysZfw = if (hasAe && hasAnalys) (analysZfw.toIntOrNull() ?: 0) else 0,
                        selfControlSkill = (selfControlSkill.toIntOrNull() ?: 0).coerceIn(0, 18),
                        sensoryAcuitySkill = (sensoryAcuitySkill.toIntOrNull() ?: 0).coerceIn(0, 18),
                        magicalLoreSkill = (magicalLoreSkill.toIntOrNull() ?: 0).coerceIn(0, 18),
                        herbalLoreSkill = (herbalLoreSkill.toIntOrNull() ?: 0).coerceIn(0, 18),
                        isGameMaster = isGameMaster
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
