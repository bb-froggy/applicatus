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
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
            // Lebensenergie (immer vorhanden)
            EnergyRow(
                label = stringResource(R.string.le_short),
                current = character.currentLe,
                max = character.maxLe,
                onAdjust = onAdjustLe
            )
            
            // Astralenergie (optional)
            if (character.hasAe) {
                EnergyRow(
                    label = stringResource(R.string.ae_short),
                    current = character.currentAe,
                    max = character.maxAe,
                    onAdjust = onAdjustAe
                )
            }
            
            // Karmaenergie (optional)
            if (character.hasKe) {
                EnergyRow(
                    label = stringResource(R.string.ke_short),
                    current = character.currentKe,
                    max = character.maxKe,
                    onAdjust = onAdjustKe
                )
            }
            
            // Regeneration Button
            Button(
                onClick = onRegeneration,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.regeneration))
            }
        }
    }
}

@Composable
private fun EnergyRow(
    label: String,
    current: Int,
    max: Int,
    onAdjust: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.15f)
        )
        
        Row(
            modifier = Modifier.weight(0.85f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // -5 Button
            IconButton(
                onClick = { onAdjust(-5) },
                enabled = current > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Text("-5", style = MaterialTheme.typography.labelSmall)
            }
            
            // -1 Button
            IconButton(
                onClick = { onAdjust(-1) },
                enabled = current > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium)
            }
            
            Text(
                text = "$current / $max",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            // +1 Button
            IconButton(
                onClick = { onAdjust(1) },
                enabled = current < max,
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
            
            // +5 Button
            IconButton(
                onClick = { onAdjust(5) },
                enabled = current < max,
                modifier = Modifier.size(36.dp)
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
