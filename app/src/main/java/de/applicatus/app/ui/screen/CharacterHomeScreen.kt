package de.applicatus.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.ui.screen.spell.EditCharacterDialog
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModel
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterHomeScreen(
    characterId: Long,
    viewModelFactory: CharacterHomeViewModelFactory,
    onNavigateBack: () -> Unit,
    onNavigateToSpellStorage: (Long) -> Unit,
    onNavigateToPotions: (Long) -> Unit
) {
    val viewModel: CharacterHomeViewModel = viewModel(factory = viewModelFactory)
    val character by viewModel.character.collectAsState()
    val lastRegenerationResult by viewModel.lastRegenerationResult.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showRegenerationDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_character))
                    }
                }
            )
        }
    ) { paddingValues ->
        character?.let { char ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Eigenschaften
                PropertiesCard(character = char)
                
                // Energien
                EnergiesCard(
                    character = char,
                    onAdjustLe = { delta -> viewModel.adjustCurrentLe(delta) },
                    onAdjustAe = { delta -> viewModel.adjustCurrentAe(delta) },
                    onAdjustKe = { delta -> viewModel.adjustCurrentKe(delta) },
                    onRegeneration = { showRegenerationDialog = true }
                )
                
                // Talente
                TalentsCard(character = char)
                
                // Zauber
                SpellsCard(character = char)
                
                // Navigation Buttons
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { onNavigateToSpellStorage(characterId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.spell_storage))
                }
                
                Button(
                    onClick = { onNavigateToPotions(characterId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.witch_kitchen))
                }
            }
        }
    }
    
    if (showRegenerationDialog) {
        RegenerationDialog(
            onDismiss = { showRegenerationDialog = false },
            onRegenerate = { modifier ->
                viewModel.performRegeneration(modifier)
                showRegenerationDialog = false
            }
        )
    }
    
    // Zeige Regenerationsergebnis
    lastRegenerationResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearRegenerationResult() },
            title = { Text(stringResource(R.string.regeneration)) },
            text = { Text(result.getFormattedResult()) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearRegenerationResult() }) {
                    Text("OK")
                }
            }
        )
    }
    
    if (showEditDialog && character != null) {
        EditCharacterDialog(
            character = character!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedChar ->
                viewModel.updateCharacter(updatedChar)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun PropertiesCard(character: Character) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.properties),
                style = MaterialTheme.typography.titleMedium
            )
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PropertyItem("MU", character.mu)
                PropertyItem("KL", character.kl)
                PropertyItem("IN", character.inValue)
                PropertyItem("CH", character.ch)
                PropertyItem("FF", character.ff)
                PropertyItem("GE", character.ge)
                PropertyItem("KO", character.ko)
                PropertyItem("KK", character.kk)
            }
        }
    }
}

@Composable
private fun PropertyItem(name: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(name, style = MaterialTheme.typography.labelSmall)
        Text(value.toString(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EnergiesCard(
    character: Character,
    onAdjustLe: (Int) -> Unit,
    onAdjustAe: (Int) -> Unit,
    onAdjustKe: (Int) -> Unit,
    onRegeneration: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Erste Zeile: Überschriften und Regeneration Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LE Überschrift
                Text(
                    text = stringResource(R.string.le_short),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                // AE Überschrift (nur wenn vorhanden)
                if (character.hasAe) {
                    Text(
                        text = stringResource(R.string.ae_short),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // KE Überschrift (nur wenn vorhanden)
                if (character.hasKe) {
                    Text(
                        text = stringResource(R.string.ke_short),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Regeneration Button
                Button(
                    onClick = onRegeneration,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.regeneration))
                }
            }
            
            // Zweite Zeile: Werte und Anpassungs-Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LE Werte
                EnergyValueRow(
                    current = character.currentLe,
                    max = character.maxLe,
                    onAdjust = onAdjustLe,
                    modifier = Modifier.weight(1f)
                )
                
                // AE Werte (nur wenn vorhanden)
                if (character.hasAe) {
                    EnergyValueRow(
                        current = character.currentAe,
                        max = character.maxAe,
                        onAdjust = onAdjustAe,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // KE Werte (nur wenn vorhanden)
                if (character.hasKe) {
                    EnergyValueRow(
                        current = character.currentKe,
                        max = character.maxKe,
                        onAdjust = onAdjustKe,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Platzhalter für Button-Spalte
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EnergyValueRow(
    current: Int,
    max: Int,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Wert-Anzeige
        Text(
            text = "$current / $max",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Anpassungs-Buttons
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // -5 Button
            IconButton(
                onClick = { onAdjust(-5) },
                enabled = current > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Text("-5", style = MaterialTheme.typography.labelSmall)
            }
            
            // -1 Button
            IconButton(
                onClick = { onAdjust(-1) },
                enabled = current > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium)
            }
            
            // +1 Button
            IconButton(
                onClick = { onAdjust(1) },
                enabled = current < max,
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
            
            // +5 Button
            IconButton(
                onClick = { onAdjust(5) },
                enabled = current < max,
                modifier = Modifier.size(32.dp)
            ) {
                Text("+5", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegenerationDialog(
    onDismiss: () -> Unit,
    onRegenerate: (Int) -> Unit
) {
    var modifier by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.regeneration)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.regen_modifier))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { modifier = (modifier - 1).coerceAtLeast(-6) }
                    ) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    
                    Text(
                        text = if (modifier >= 0) "+$modifier" else "$modifier",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    IconButton(
                        onClick = { modifier = (modifier + 1).coerceAtMost(2) }
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRegenerate(modifier) }) {
                Text(stringResource(R.string.regeneration))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun TalentsCard(character: Character) {
    // Prüfe, ob der Charakter überhaupt Talente hat
    val hasTalents = character.hasAlchemy || 
                     character.hasCookingPotions || 
                     character.selfControlSkill > 0 || 
                     character.sensoryAcuitySkill > 0 || 
                     character.magicalLoreSkill > 0 || 
                     character.herbalLoreSkill > 0
    
    if (!hasTalents) return
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.talents),
                style = MaterialTheme.typography.titleMedium
            )
            Divider()
            
            // Alchimie
            if (character.hasAlchemy) {
                TalentItem(
                    name = stringResource(R.string.alchemy),
                    value = character.alchemySkill
                )
            }
            
            // Kochen (Tränke)
            if (character.hasCookingPotions) {
                TalentItem(
                    name = stringResource(R.string.cooking_potions),
                    value = character.cookingPotionsSkill
                )
            }
            
            // Selbstbeherrschung
            if (character.selfControlSkill > 0) {
                TalentItem(
                    name = stringResource(R.string.self_control),
                    value = character.selfControlSkill
                )
            }
            
            // Sinnenschärfe
            if (character.sensoryAcuitySkill > 0) {
                TalentItem(
                    name = stringResource(R.string.sensory_acuity),
                    value = character.sensoryAcuitySkill
                )
            }
            
            // Magiekunde
            if (character.magicalLoreSkill > 0) {
                TalentItem(
                    name = stringResource(R.string.magical_lore),
                    value = character.magicalLoreSkill
                )
            }
            
            // Pflanzenkunde
            if (character.herbalLoreSkill > 0) {
                TalentItem(
                    name = stringResource(R.string.herbal_lore),
                    value = character.herbalLoreSkill
                )
            }
        }
    }
}

@Composable
private fun TalentItem(name: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SpellsCard(character: Character) {
    // Prüfe, ob der Charakter überhaupt Zauber beherrscht
    val hasSpells = character.hasApplicatus || 
                    character.hasOdem || 
                    character.hasAnalys
    
    if (!hasSpells) return
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.spells),
                style = MaterialTheme.typography.titleMedium
            )
            Divider()
            
            // Applicatus
            if (character.hasApplicatus) {
                SpellItem(
                    name = stringResource(R.string.applicatus),
                    zfw = character.applicatusZfw
                )
            }
            
            // ODEM ARCANUM
            if (character.hasOdem) {
                SpellItem(
                    name = stringResource(R.string.odem_arcanum),
                    zfw = character.odemZfw
                )
            }
            
            // ANALYS ARKANSTRUKTUR
            if (character.hasAnalys) {
                SpellItem(
                    name = stringResource(R.string.analys_arkanstruktur),
                    zfw = character.analysZfw
                )
            }
        }
    }
}

@Composable
private fun SpellItem(name: String, zfw: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.zfw) + ": $zfw",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
