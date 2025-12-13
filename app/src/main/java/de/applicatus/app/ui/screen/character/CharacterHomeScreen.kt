package de.applicatus.app.ui.screen.character

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.ui.component.EnergyChangeNotification
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModel
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModelFactory
import de.applicatus.app.data.sync.CharacterRealtimeSyncManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterHomeScreen(
    characterId: Long,
    viewModelFactory: CharacterHomeViewModelFactory,
    onNavigateBack: () -> Unit,
    onNavigateToSpellStorage: (Long) -> Unit,
    onNavigateToPotions: (Long) -> Unit,
    onNavigateToInventory: (Long) -> Unit,
    onNavigateToJournal: (Long) -> Unit = {},
    onNavigateToNearbySync: (Long, String) -> Unit = { _, _ -> },
    onNavigateToMagicSigns: (Long) -> Unit = {}
) {
    val viewModel: CharacterHomeViewModel = viewModel(factory = viewModelFactory)
    val character by viewModel.character.collectAsState()
    val lastRegenerationResult by viewModel.lastRegenerationResult.collectAsState()
    val lastAstralMeditationResult by viewModel.lastAstralMeditationResult.collectAsState()
    val energyChanges by viewModel.energyChanges.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val discoveredEndpoints by viewModel.discoveredEndpoints.collectAsState()
    val context = LocalContext.current
    val importState = viewModel.importState
    val exportState = viewModel.exportState
    
    var isEditMode by remember { mutableStateOf(false) }
    var showEditPropertiesDialog by remember { mutableStateOf(false) }
    var showEditEnergiesDialog by remember { mutableStateOf(false) }
    var showEditTalentsDialog by remember { mutableStateOf(false) }
    var showEditSpellsDialog by remember { mutableStateOf(false) }
    var showRegenerationDialog by remember { mutableStateOf(false) }
    var showAstralMeditationDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRealtimeSyncDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var previousSyncStatus by remember { mutableStateOf<CharacterRealtimeSyncManager.SyncStatus>(CharacterRealtimeSyncManager.SyncStatus.Idle) }
    
    // Get required permissions based on Android version
    val nearbyPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    // Check if all required permissions are granted
    fun arePermissionsGranted(): Boolean {
        return nearbyPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showRealtimeSyncDialog = true
        } else {
            showPermissionDialog = true
        }
    }
    
    // Helper function to request permission or open sync dialog
    fun requestSyncPermissionsOrOpenDialog() {
        if (arePermissionsGranted()) {
            showRealtimeSyncDialog = true
        } else {
            permissionLauncher.launch(nearbyPermissions.toTypedArray())
        }
    }

    LaunchedEffect(syncStatus, showRealtimeSyncDialog) {
        val connectionLost = previousSyncStatus is CharacterRealtimeSyncManager.SyncStatus.Syncing &&
            syncStatus is CharacterRealtimeSyncManager.SyncStatus.Connecting
        val shouldShowDialog = !showRealtimeSyncDialog && (
            syncStatus is CharacterRealtimeSyncManager.SyncStatus.Error ||
            syncStatus is CharacterRealtimeSyncManager.SyncStatus.Warning ||
            connectionLost
        )
        if (shouldShowDialog) {
            showRealtimeSyncDialog = true
        }
        previousSyncStatus = syncStatus
    }
    
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
                    // Sync Status Indicator
                    SyncStatusIndicator(
                        syncStatus = syncStatus,
                        onClick = { requestSyncPermissionsOrOpenDialog() }
                    )
                    
                    // Journal Button
                    IconButton(onClick = { onNavigateToJournal(characterId) }) {
                        Icon(
                            Icons.Default.MenuBook, 
                            contentDescription = "Journal"
                        )
                    }
                    
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = if (isEditMode) "Bearbeitung beenden" else stringResource(R.string.edit_character),
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
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
                                text = { Text("Echtzeit-Sync") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    requestSyncPermissionsOrOpenDialog()
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Nearby Sync (Alt)") },
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
                CharacterPropertiesCard(
                    character = char,
                    isEditMode = isEditMode,
                    onClick = { showEditPropertiesDialog = true }
                )
                
                // Energien mit Change Notification Overlay
                Box {
                    CharacterEnergiesCard(
                        character = char,
                        onAdjustLe = { delta -> viewModel.adjustCurrentLe(delta) },
                        onAdjustAe = { delta -> viewModel.adjustCurrentAe(delta) },
                        onAdjustKe = { delta -> viewModel.adjustCurrentKe(delta) },
                        onRegeneration = { showRegenerationDialog = true },
                        onAstralMeditation = { showAstralMeditationDialog = true },
                        isEditMode = isEditMode,
                        onClick = { showEditEnergiesDialog = true }
                    )
                    
                    // Energie-Änderungs-Benachrichtigung
                    if (energyChanges.isNotEmpty()) {
                        EnergyChangeNotification(
                            changes = energyChanges,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            onAnimationEnd = { viewModel.clearEnergyChanges() }
                        )
                    }
                }
                
                // Talente
                CharacterTalentsCard(
                    character = char,
                    isEditMode = isEditMode,
                    onClick = { showEditTalentsDialog = true }
                )
                
                // Zauber
                CharacterSpellsCard(
                    character = char,
                    isEditMode = isEditMode,
                    onClick = { showEditSpellsDialog = true }
                )
                
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
                
                // Zauberzeichen nur anzeigen, wenn Charakter die SF hat und RkW > 0
                if (char.hasZauberzeichen && char.ritualKnowledgeValue > 0) {
                    Button(
                        onClick = { onNavigateToMagicSigns(characterId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zauberzeichen")
                    }
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
    
    if (showAstralMeditationDialog) {
        character?.let { char ->
            AstralMeditationDialog(
                character = char,
                onDismiss = { showAstralMeditationDialog = false },
                onConfirm = { leToConvert ->
                    viewModel.performAstralMeditation(leToConvert)
                    showAstralMeditationDialog = false
                }
            )
        }
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
    
    // Zeige Astrale Meditation Ergebnis
    lastAstralMeditationResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAstralMeditationResult() },
            title = { Text("Astrale Meditation") },
            text = { Text(result) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAstralMeditationResult() }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Edit Dialogs
    if (showEditPropertiesDialog && character != null) {
        EditCharacterPropertiesDialog(
            character = character!!,
            onDismiss = { showEditPropertiesDialog = false },
            onConfirm = { updatedChar ->
                viewModel.updateCharacter(updatedChar)
                showEditPropertiesDialog = false
            }
        )
    }
    
    if (showEditEnergiesDialog && character != null) {
        EditCharacterEnergiesDialog(
            character = character!!,
            onDismiss = { showEditEnergiesDialog = false },
            onConfirm = { updatedChar ->
                viewModel.updateCharacter(updatedChar)
                showEditEnergiesDialog = false
            }
        )
    }
    
    if (showEditTalentsDialog && character != null) {
        EditCharacterTalentsDialog(
            character = character!!,
            onDismiss = { showEditTalentsDialog = false },
            onConfirm = { updatedChar ->
                viewModel.updateCharacter(updatedChar)
                showEditTalentsDialog = false
            }
        )
    }
    
    if (showEditSpellsDialog && character != null) {
        EditCharacterSpellsDialog(
            character = character!!,
            onDismiss = { showEditSpellsDialog = false },
            onConfirm = { updatedChar ->
                viewModel.updateCharacter(updatedChar)
                showEditSpellsDialog = false
            }
        )
    }
    
    // Permission Dialog - shown when Bluetooth/Nearby permissions are denied
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Berechtigungen erforderlich") },
            text = { 
                Text(
                    "Für die Echtzeit-Synchronisation werden Bluetooth- und Standort-Berechtigungen benötigt.\n\n" +
                    "Bitte erteile die Berechtigungen in den Einstellungen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Einstellungen öffnen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
    
    // Real-Time Sync Discovery Dialog
    if (showRealtimeSyncDialog) {
        RealtimeSyncDiscoveryDialog(
            characterName = character?.name ?: "Charakter",
            syncStatus = syncStatus,
            discoveredEndpoints = discoveredEndpoints,
            onHost = {
                val deviceName = android.os.Build.MODEL ?: "Unbekanntes Gerät"
                viewModel.startHostSession(deviceName)
            },
            onStartDiscovery = {
                viewModel.startDiscovery()
            },
            onJoin = { endpointId, endpointName ->
                viewModel.startClientSession(endpointId, endpointName)
            },
            onStopSync = {
                viewModel.stopSyncSession()
            },
            onDismiss = {
                showRealtimeSyncDialog = false
                viewModel.stopDiscovery()
            }
        )
    }
}
