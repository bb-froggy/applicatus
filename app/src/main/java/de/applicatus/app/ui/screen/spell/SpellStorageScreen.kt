package de.applicatus.app.ui.screen.spell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.ui.screen.spell.components.*
import de.applicatus.app.ui.component.character.EditCharacterDialog
import de.applicatus.app.ui.component.character.CharacterAttributesCard
import de.applicatus.app.ui.component.character.ApplicatusInfoCard
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellStorageScreen(
    viewModel: CharacterDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val character by viewModel.character.collectAsState()
    val spellSlots by viewModel.spellSlots.collectAsState()
    val allSpells by viewModel.allSpells.collectAsState()
    val groupDate by viewModel.groupDate.collectAsState()
    val isEditMode = viewModel.isEditMode
    val spellCastMessage = viewModel.spellCastMessage
    
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var showEditCharacterDialog by remember { mutableStateOf(false) }
    
    // Zauber-Auslösungs-Meldung Dialog
    spellCastMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSpellCastMessage() },
            title = { Text("Zauber ausgelöst") },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
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
            
            // Applicatus info if available (immer anzeigen)
            character?.let { char ->
                if (char.hasApplicatus) {
                    ApplicatusInfoCard(
                        character = char,
                        onDurationChange = { duration ->
                            viewModel.updateCharacter(char.copy(applicatusDuration = duration))
                        },
                        onModifierChange = { modifier ->
                            viewModel.updateCharacter(char.copy(applicatusModifier = modifier))
                        }
                    )
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
                            onDurationFormulaChanged = { formula ->
                                viewModel.updateSlotDurationFormula(slotWithSpell.slot, formula)
                            },
                            onDeleteSlot = {
                                viewModel.removeSlot(slotWithSpell.slot)
                            }
                        )
                    } else {
                        SpellSlotCardUsageMode(
                            slotWithSpell = slotWithSpell,
                            currentDate = groupDate,
                            isGameMaster = character?.isGameMaster ?: false,
                            showAnimation = viewModel.showSpellAnimation && viewModel.animatingSlotId == slotWithSpell.slot.id,
                            onCastSpell = {
                                slotWithSpell.spell?.let { spell ->
                                    viewModel.castSpell(slotWithSpell.slot, spell)
                                }
                            },
                            onClearSlot = {
                                viewModel.clearSlot(slotWithSpell.slot, slotWithSpell.spell)
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
