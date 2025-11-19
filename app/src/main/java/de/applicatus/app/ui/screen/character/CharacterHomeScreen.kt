package de.applicatus.app.ui.screen.character

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModel
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterHomeScreen(
    characterId: Long,
    viewModelFactory: CharacterHomeViewModelFactory,
    onNavigateBack: () -> Unit,
    onNavigateToSpellStorage: (Long) -> Unit,
    onNavigateToPotions: (Long) -> Unit,
    onNavigateToInventory: (Long) -> Unit,
    onNavigateToNearbySync: (Long, String) -> Unit = { _, _ -> }
) {
    val viewModel: CharacterHomeViewModel = viewModel(factory = viewModelFactory)
    val character by viewModel.character.collectAsState()
    val lastRegenerationResult by viewModel.lastRegenerationResult.collectAsState()
    val context = LocalContext.current
    val importState = viewModel.importState
    val exportState = viewModel.exportState
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showRegenerationDialog by remember { mutableStateOf(false) }
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
        is CharacterHomeViewModel.ExportState.Success -> {
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
        is CharacterHomeViewModel.ExportState.Error -> {
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
    
    // Import Status Dialog
    when (importState) {
        is CharacterHomeViewModel.ImportState.ConfirmationRequired -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetImportState() },
                title = { Text("Import bestätigen") },
                text = { Text(importState.warning) },
                confirmButton = {
                    TextButton(onClick = { 
                        viewModel.confirmImport(importState.context, importState.uri, importState.targetCharacterId)
                    }) {
                        Text("Fortfahren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetImportState() }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
        is CharacterHomeViewModel.ImportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetImportState() },
                title = { Text("Import erfolgreich") },
                text = { Text(importState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetImportState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is CharacterHomeViewModel.ImportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetImportState() },
                title = { Text("Import fehlgeschlagen") },
                text = { Text(importState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetImportState() }) {
                        Text("OK")
                    }
                }
            )
        }
        CharacterHomeViewModel.ImportState.Importing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Importiere Charakter...") },
                text = { 
                    Row(horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {}
            )
        }
        else -> {}
    }
    
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
                                    onNavigateToNearbySync(characterId, character?.name ?: "Charakter")
                                }
                            )

                        }
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
                CharacterPropertiesCard(character = char)
                
                // Energien
                CharacterEnergiesCard(
                    character = char,
                    onAdjustLe = { delta -> viewModel.adjustCurrentLe(delta) },
                    onAdjustAe = { delta -> viewModel.adjustCurrentAe(delta) },
                    onAdjustKe = { delta -> viewModel.adjustCurrentKe(delta) },
                    onRegeneration = { showRegenerationDialog = true }
                )
                
                // Talente
                CharacterTalentsCard(character = char)
                
                // Zauber
                CharacterSpellsCard(character = char)
                
                // Navigation Buttons
                Spacer(modifier = Modifier.height(16.dp))
                
                // Zauberspeicher nur anzeigen, wenn Charakter AE hat
                if (char.maxAe > 0) {
                    Button(
                        onClick = { onNavigateToSpellStorage(characterId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.spell_storage))
                    }
                }
                
                Button(
                    onClick = { onNavigateToPotions(characterId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.witch_kitchen))
                }
                
                Button(
                    onClick = { onNavigateToInventory(characterId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Packesel")
                }
            }
        }
    }
    
    if (showRegenerationDialog) {
        CharacterRegenerationDialog(
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
