package de.applicatus.app.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import de.applicatus.app.R
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.sync.CharacterRealtimeSyncManager
import de.applicatus.app.ui.viewmodel.CharacterListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CharacterListScreen(
    viewModel: CharacterListViewModel,
    pendingImportUri: Uri? = null,
    onImportHandled: () -> Unit = {},
    onCharacterClick: (Long) -> Unit,
    onNearbySyncClick: () -> Unit
) {
    val context = LocalContext.current
    val characters by viewModel.characters.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val currentGroup by viewModel.currentGroup.collectAsState()
    val activeSyncStatuses by viewModel.activeSyncStatuses.collectAsState()
    val importState = viewModel.importState
    val exportState = viewModel.exportState
    val backupImportState = viewModel.backupImportState
    val backupExportState = viewModel.backupExportState
    val spellSyncState = viewModel.spellSyncState
    val recipeSyncState = viewModel.recipeSyncState
    val isDateEditMode by remember { derivedStateOf { viewModel.isDateEditMode } }
    val isGroupEditMode by remember { derivedStateOf { viewModel.isGroupEditMode } }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var draggedCharacter by remember { mutableStateOf<Character?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var characterToExport by remember { mutableStateOf<Character?>(null) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    
    // File picker für JSON-Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCharacterFromFile(context, it) }
    }
    
    // File picker für JSON-Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { 
            characterToExport?.let { character ->
                viewModel.exportCharacterToFile(context, character.id, it)
            }
        }
    }
    
    // File picker für Vollständiges Backup Export
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportFullBackup(context, it) }
    }
    
    // File picker für Vollständiges Backup Import
    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.validateAndPrepareBackupImport(context, it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charaktere") },
                actions = {
                    // Edit-Modus Toggle für Gruppen
                    IconButton(onClick = { viewModel.toggleGroupEditMode() }) {
                        Icon(
                            imageVector = if (isGroupEditMode) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isGroupEditMode) "Bearbeitung beenden" else "Gruppen bearbeiten",
                            tint = if (isGroupEditMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Zauber-Datenbank aktualisieren") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            onClick = {
                                showMenu = false
                                viewModel.syncMissingSpells()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rezept-Datenbank aktualisieren") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            onClick = {
                                showMenu = false
                                viewModel.syncMissingRecipes()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Charakter aus JSON importieren") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                showMenu = false
                                importLauncher.launch("application/json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Charakter über Nearby empfangen") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = {
                                showMenu = false
                                onNearbySyncClick()
                            }
                        )

                        HorizontalDivider()
                        
                        DropdownMenuItem(
                            text = { Text("Vollständiges Backup exportieren") },
                            leadingIcon = { 
                                Icon(
                                    painter = painterResource(R.drawable.ic_backup),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    .format(java.util.Date())
                                backupExportLauncher.launch("applicatus_backup_$timestamp.json")
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Vollständiges Backup importieren") },
                            leadingIcon = { 
                                Icon(
                                    painter = painterResource(R.drawable.ic_restore),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                backupImportLauncher.launch("application/json")
                            }
                        )

                        HorizontalDivider()
                        
                        DropdownMenuItem(
                            text = { Text("App-Informationen") },
                            leadingIcon = {
                                Icon(Icons.Default.Info, null)
                            },
                            onClick = {
                                showMenu = false
                                showInfoDialog = true
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Neue Gruppe erstellen
                SmallFloatingActionButton(
                    onClick = { showAddGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Neue Gruppe")
                }
                
                // Neuer Charakter
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Neuer Charakter")
                }
            }
        }
    ) { padding ->
        // Gruppiere Charaktere nach groupId und mappe sie auf die entsprechenden Group-Objekte
        // Charaktere ohne passende Gruppe werden mit einer Dummy-Gruppe angezeigt
        // Gruppen ohne Charaktere werden ebenfalls angezeigt
        val charactersByGroupId = characters.groupBy { it.groupId }
        
        // Erstelle eine Liste aller Gruppen (mit und ohne Charaktere)
        val groupedCharactersWithGroup = groups.map { group ->
            val charsInGroup = charactersByGroupId[group.id] ?: emptyList()
            group to charsInGroup
        }.toMutableList()
        
        // Füge Charaktere hinzu, deren Gruppe nicht existiert (Dummy-Gruppe)
        val groupIds = groups.map { it.id }.toSet()
        charactersByGroupId.forEach { (groupId, chars) ->
            if (groupId != null && !groupIds.contains(groupId)) {
                val dummyGroup = Group(
                    id = groupId,
                    name = "Unbekannte Gruppe",
                    currentDerianDate = "1 Praios 1000 BF"
                )
                groupedCharactersWithGroup.add(dummyGroup to chars)
            }
        }
        
        val sortedGroups = groupedCharactersWithGroup.sortedBy { it.first.name }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Für jede Gruppe: Datum-Card und Charaktere
            sortedGroups.forEach { (group, charactersInGroup) ->
                // Gruppenname als Überschrift mit Datum darunter
                item {
                    Column {
                        // Gruppenname mit Spielleiter-Toggle und Löschen-Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Löschen-Button nur im Edit-Modus anzeigen
                                if (isGroupEditMode) {
                                    IconButton(
                                        onClick = {
                                            groupToDelete = group
                                            showDeleteGroupDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Gruppe löschen",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                if (group.isGameMasterGroup) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_game_master_mask),
                                        contentDescription = "Spielleiter-Modus",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.updateGroupGameMasterMode(
                                            group.id,
                                            !group.isGameMasterGroup
                                        )
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_game_master_mask),
                                        contentDescription = if (group.isGameMasterGroup) 
                                            "Spielleiter-Modus deaktivieren" 
                                        else 
                                            "Spielleiter-Modus aktivieren",
                                        tint = if (group.isGameMasterGroup)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        DerianDateCard(
                            currentDate = group.currentDerianDate,
                            isEditMode = isDateEditMode && viewModel.selectedGroupId == group.id,
                            onToggleEditMode = { 
                                viewModel.selectGroup(group.id)
                                viewModel.toggleDateEditMode() 
                            },
                            onUpdateDate = { newDate ->
                                viewModel.updateDerianDate(group.id, newDate) 
                            },
                            onIncrement = { 
                                viewModel.incrementDerianDate(group.id) 
                            },
                            onDecrement = { 
                                viewModel.decrementDerianDate(group.id) 
                            }
                        )
                    }
                }
                
                // Charaktere dieser Gruppe
                items(charactersInGroup, key = { it.id }) { character ->
                    CharacterListItem(
                        character = character,
                        onClick = { onCharacterClick(character.id) },
                        onDelete = { viewModel.deleteCharacter(character) },
                        onLongPress = {
                            draggedCharacter = character
                            showMoveDialog = true
                        },
                        onExport = {
                            characterToExport = character
                            exportLauncher.launch("${character.name}.json")
                        },
                        onShare = {
                            viewModel.viewModelScope.launch {
                                val result = viewModel.shareCharacter(context, character.id)
                                result.onSuccess { intent ->
                                    context.startActivity(intent)
                                }.onFailure { error ->
                                    // Fehler wird über Toast angezeigt
                                    android.widget.Toast.makeText(
                                        context,
                                        "Teilen fehlgeschlagen: ${error.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        syncStatus = activeSyncStatuses[character.id]
                    )
                }
                
                // Abstand zwischen Gruppen
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    // Handle pending import from external file
    LaunchedEffect(pendingImportUri) {
        pendingImportUri?.let { uri ->
            viewModel.importCharacterFromFile(context, uri)
            onImportHandled() // URI zurücksetzen nach Import
        }
    }
    
    // Import-Status Snackbar
    LaunchedEffect(importState) {
        when (importState) {
            is CharacterListViewModel.ImportState.Success -> {
                // Automatisch zum importierten Charakter navigieren
                onCharacterClick(importState.characterId)
                viewModel.resetImportState()
            }
            is CharacterListViewModel.ImportState.Error -> {
                // Fehler wird als Snackbar angezeigt
                // In produktivem Code könnte man einen SnackbarHost verwenden
            }
            else -> {}
        }
    }
    
    // Dialog zum Bestätigen des Löschens einer Gruppe
    if (showDeleteGroupDialog && groupToDelete != null) {
        val groupName = groupToDelete!!.name
        val characterCount = characters.count { it.groupId == groupToDelete!!.id }
        
        AlertDialog(
            onDismissRequest = {
                showDeleteGroupDialog = false
                groupToDelete = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Gruppe löschen?") },
            text = {
                Column {
                    Text("Möchten Sie die Gruppe \"$groupName\" wirklich löschen?")
                    if (characterCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Achtung: Es werden auch alle $characterCount Charakter${if (characterCount == 1) "" else "e"} in dieser Gruppe unwiderruflich gelöscht!",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupToDelete?.let { group ->
                            viewModel.deleteGroupWithCharacters(group.id)
                        }
                        showDeleteGroupDialog = false
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteGroupDialog = false
                        groupToDelete = null
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
    
    // Export State Dialog
    when (val state = exportState) {
        is CharacterListViewModel.ExportState.Success -> {
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
        is CharacterListViewModel.ExportState.Error -> {
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
    
    if (showAddDialog) {
        AddCharacterDialog(
            groups = groups,
            selectedGroupId = viewModel.selectedGroupId,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, groupId, mu, kl, inValue, ch, ff, ge, ko, kk, hasApplicatus, applicatusZfw, applicatusModifier ->
                viewModel.addCharacter(
                    name = name,
                    groupId = groupId,
                    mu = mu,
                    kl = kl,
                    inValue = inValue,
                    ch = ch,
                    ff = ff,
                    ge = ge,
                    ko = ko,
                    kk = kk,
                    hasApplicatus = hasApplicatus,
                    applicatusZfw = applicatusZfw,
                    applicatusModifier = applicatusModifier
                )
                showAddDialog = false
            }
        )
    }
    
    // Dialog zum Erstellen einer neuen Gruppe
    if (showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { groupName ->
                viewModel.addGroup(groupName)
                showAddGroupDialog = false
            }
        )
    }
    
    // Dialog für ablaufende Zauber
    viewModel.expiryWarningMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissExpiryWarning() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Zauber laufen ab!") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissExpiryWarning() }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Dialog zum Verschieben eines Charakters
    if (showMoveDialog && draggedCharacter != null) {
        MoveCharacterDialog(
            character = draggedCharacter!!,
            groups = groups,
            currentGroupId = draggedCharacter!!.groupId,
            onDismiss = { 
                showMoveDialog = false
                draggedCharacter = null
            },
            onConfirm = { targetGroupId ->
                viewModel.moveCharacterToGroup(draggedCharacter!!.id, targetGroupId)
                showMoveDialog = false
                draggedCharacter = null
            }
        )
    }
    
    // Import Status Dialog
    when (importState) {
        is CharacterListViewModel.ImportState.ConfirmationRequired -> {
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
        is CharacterListViewModel.ImportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetImportState() },
                title = { Text("Import erfolgreich") },
                text = { Text(importState.message) },
                confirmButton = {
                    TextButton(onClick = { 
                        onCharacterClick(importState.characterId)
                        viewModel.resetImportState()
                    }) {
                        Text("Zum Charakter")
                    }
                }
            )
        }
        is CharacterListViewModel.ImportState.Error -> {
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
        CharacterListViewModel.ImportState.Importing -> {
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
    
    // Spell Sync Status Dialog
    when (spellSyncState) {
        is CharacterListViewModel.SpellSyncState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetSpellSyncState() },
                title = { Text("Zauber synchronisiert") },
                text = { 
                    val count = spellSyncState.count
                    Text(
                        if (count == 0) {
                            "Alle Zauber sind bereits aktuell. Es wurden keine neuen Zauber hinzugefügt."
                        } else {
                            "$count neue${if (count == 1) "r" else ""} Zauber wurde${if (count == 1) "" else "n"} zur Datenbank hinzugefügt."
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetSpellSyncState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is CharacterListViewModel.SpellSyncState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetSpellSyncState() },
                title = { Text("Synchronisation fehlgeschlagen") },
                text = { Text(spellSyncState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetSpellSyncState() }) {
                        Text("OK")
                    }
                }
            )
        }
        CharacterListViewModel.SpellSyncState.Syncing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Synchronisiere Zauber...") },
                text = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {}
            )
        }
        else -> {}
    }
    
    // Recipe Sync Status Dialog
    when (recipeSyncState) {
        is CharacterListViewModel.RecipeSyncState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetRecipeSyncState() },
                title = { Text("Rezepte synchronisiert") },
                text = { 
                    val count = recipeSyncState.count
                    Text(
                        if (count > 0) {
                            "$count Rezepte wurden hinzugefügt oder aktualisiert."
                        } else {
                            "Alle Rezepte sind bereits auf dem neuesten Stand."
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetRecipeSyncState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is CharacterListViewModel.RecipeSyncState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetRecipeSyncState() },
                title = { Text("Synchronisation fehlgeschlagen") },
                text = { Text(recipeSyncState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetRecipeSyncState() }) {
                        Text("OK")
                    }
                }
            )
        }
        CharacterListViewModel.RecipeSyncState.Syncing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Synchronisiere Rezepte...") },
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
    
    // Backup Export Status Dialog
    when (backupExportState) {
        is CharacterListViewModel.BackupExportState.Exporting -> {
            AlertDialog(
                onDismissRequest = { /* Nicht schließbar während Export */ },
                title = { Text("Backup wird exportiert...") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { backupExportState.percentage / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(backupExportState.progress)
                        Text(
                            "${backupExportState.percentage}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {}
            )
        }
        is CharacterListViewModel.BackupExportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupExportState() },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                title = { Text("Backup erfolgreich") },
                text = { Text(backupExportState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetBackupExportState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is CharacterListViewModel.BackupExportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupExportState() },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("Export fehlgeschlagen") },
                text = { Text(backupExportState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetBackupExportState() }) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }
    
    // Backup Import Status Dialog
    when (backupImportState) {
        is CharacterListViewModel.BackupImportState.Validating -> {
            AlertDialog(
                onDismissRequest = { /* Nicht schließbar während Validierung */ },
                title = { Text("Backup wird überprüft...") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bitte warten...")
                    }
                },
                confirmButton = {}
            )
        }
        is CharacterListViewModel.BackupImportState.ConfirmationRequired -> {
            val validation = backupImportState.validation
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupImportState() },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("Backup importieren?") },
                text = {
                    Column {
                        Text("Folgende Daten werden importiert:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• ${validation.characterCount} Charaktere")
                        Text("• ${validation.groupCount} Gruppen")
                        Text("• ${validation.spellCount} Zauber")
                        Text("• ${validation.recipeCount} Rezepte")
                        
                        if (validation.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("⚠️ Warnungen:", style = MaterialTheme.typography.bodyMedium)
                            validation.warnings.forEach { warning ->
                                Text("• $warning", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.importFullBackup(
                                backupImportState.context,
                                backupImportState.uri
                            )
                        }
                    ) {
                        Text("Importieren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetBackupImportState() }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
        is CharacterListViewModel.BackupImportState.Importing -> {
            AlertDialog(
                onDismissRequest = { /* Nicht schließbar während Import */ },
                title = { Text("Backup wird importiert...") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { backupImportState.percentage / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(backupImportState.progress)
                        Text(
                            "${backupImportState.percentage}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {}
            )
        }
        is CharacterListViewModel.BackupImportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupImportState() },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                title = { Text("Backup erfolgreich importiert") },
                text = { 
                    Text(
                        backupImportState.summary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetBackupImportState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is CharacterListViewModel.BackupImportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupImportState() },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("Import fehlgeschlagen") },
                text = { Text(backupImportState.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetBackupImportState() }) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }
    
    // Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("App-Informationen") },
            text = {
                Column {
                    Text("Datenbankschema-Version: ${DataModelVersion.CURRENT_VERSION}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Diese Version wird für den Import und Export von Charakteren verwendet. " +
                        "Bei Versionskonflikten kann es zu Warnungen kommen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CharacterListItem(
    character: Character,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit,
    onExport: () -> Unit = {},
    onShare: () -> Unit = {},
    syncStatus: CharacterRealtimeSyncManager.SyncStatus? = null
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirmation = true
                false // Verhindert automatisches Löschen, wir warten auf Bestätigung
            } else {
                false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Roter Hintergrund mit Lösch-Icon beim Swipen
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Color.Red
            } else {
                Color.Transparent
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = Color.White
                    )
                }
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongPress
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync-Status-Indikator (wenn aktiv)
                    if (syncStatus != null && syncStatus !is CharacterRealtimeSyncManager.SyncStatus.Idle) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = when (syncStatus) {
                                        is CharacterRealtimeSyncManager.SyncStatus.Syncing -> Color(0xFF4CAF50) // Grün
                                        is CharacterRealtimeSyncManager.SyncStatus.Warning -> Color(0xFFFF9800) // Orange
                                        is CharacterRealtimeSyncManager.SyncStatus.Connecting -> Color(0xFF2196F3) // Blau
                                        is CharacterRealtimeSyncManager.SyncStatus.Error -> Color(0xFFF44336) // Rot
                                        else -> Color.Gray
                                    },
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MU ${character.mu} | KL ${character.kl} | IN ${character.inValue} | CH ${character.ch}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "FF ${character.ff} | GE ${character.ge} | KO ${character.ko} | KK ${character.kk}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Teilen/Exportieren")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Teilen...") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showExportMenu = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Als Datei exportieren") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                                onClick = {
                                    showExportMenu = false
                                    onExport()
                                }
                            )
                        }
                    }
                }
            }
        }
    )
    
    // Bestätigungs-Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
            },
            title = { Text("Charakter löschen?") },
            text = { Text("Möchtest du '${character.name}' wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
    
    // Reset des Swipe-States wenn Dialog geschlossen wird
    LaunchedEffect(showDeleteConfirmation) {
        if (!showDeleteConfirmation) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
}

@Composable
fun AddCharacterDialog(
    groups: List<de.applicatus.app.data.model.character.Group>,
    selectedGroupId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Int, Int, Int, Int, Int, Int, Int, Int, Boolean, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf(selectedGroupId) }
    var mu by remember { mutableStateOf("8") }
    var kl by remember { mutableStateOf("8") }
    var inValue by remember { mutableStateOf("8") }
    var ch by remember { mutableStateOf("8") }
    var ff by remember { mutableStateOf("8") }
    var ge by remember { mutableStateOf("8") }
    var ko by remember { mutableStateOf("8") }
    var kk by remember { mutableStateOf("8") }
    var hasApplicatus by remember { mutableStateOf(false) }
    var applicatusZfw by remember { mutableStateOf("0") }
    var applicatusModifier by remember { mutableStateOf("0") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Charakter") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Gruppe:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    // Gruppen-Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    val currentGroupName = groups.find { it.id == groupId }?.name ?: "Meine Gruppe"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentGroupName)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name) },
                                    onClick = {
                                        groupId = group.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
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
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            groupId,
                            mu.toIntOrNull() ?: 8,
                            kl.toIntOrNull() ?: 8,
                            inValue.toIntOrNull() ?: 8,
                            ch.toIntOrNull() ?: 8,
                            ff.toIntOrNull() ?: 8,
                            ge.toIntOrNull() ?: 8,
                            ko.toIntOrNull() ?: 8,
                            kk.toIntOrNull() ?: 8,
                            hasApplicatus,
                            applicatusZfw.toIntOrNull() ?: 0,
                            applicatusModifier.toIntOrNull() ?: 0
                        )
                    }
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
fun DerianDateCard(
    currentDate: String,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onUpdateDate: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    var editingDate by remember(currentDate) { mutableStateOf(currentDate) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isEditMode) {
                // Bearbeitungsmodus: Vollständige Datumsbearbeitung
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editingDate,
                        onValueChange = { editingDate = it },
                        label = { Text("Datum (z.B. 15 Praios 1040 BF)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            editingDate = currentDate
                            onToggleEditMode()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }
                    Button(
                        onClick = {
                            onUpdateDate(editingDate)
                            onToggleEditMode()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Speichern")
                    }
                }
            } else {
                // Nutzungsmodus: Kompakte Anzeige
                // Erste Zeile: Mondphase, Wochentag, Bearbeiten-Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mondphase
                        val madaPhase = de.applicatus.app.logic.DerianDateCalculator.getMadaPhase(currentDate)
                        Text(
                            text = madaPhase.symbol,
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        // Wochentag
                        Text(
                            text = de.applicatus.app.logic.DerianDateCalculator.getWeekday(currentDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    
                    TextButton(onClick = onToggleEditMode) {
                        Text("Bearbeiten")
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Zweite Zeile: Datum mit +/- Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDecrement) {
                        Text("-", style = MaterialTheme.typography.headlineMedium)
                    }
                    
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    IconButton(onClick = onIncrement) {
                        Text("+", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupSelector(
    groups: List<Group>,
    selectedGroupId: Long?,
    onSelectGroup: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedGroup = groups.find { it.id == selectedGroupId }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gruppe",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = selectedGroup?.name ?: "Meine Gruppe",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Text(
                text = "▼",
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = group.name,
                            style = if (group.id == selectedGroupId) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.bodyMedium
                            }
                        )
                    },
                    onClick = {
                        onSelectGroup(group.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Gruppe erstellen") },
        text = {
            Column {
                Text(
                    text = "Gib einen Namen für die neue Gruppe ein:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Gruppenname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (groupName.isNotBlank()) {
                        onConfirm(groupName.trim())
                    }
                },
                enabled = groupName.isNotBlank()
            ) {
                Text("Erstellen")
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
fun MoveCharacterDialog(
    character: Character,
    groups: List<Group>,
    currentGroupId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedGroupId by remember { mutableStateOf(currentGroupId) }
    val currentGroup = groups.find { it.id == currentGroupId }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charakter verschieben") },
        text = {
            Column {
                Text(
                    text = "Verschiebe '${character.name}' in eine andere Gruppe:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Aktuelle Gruppe: ${currentGroup?.name ?: "Keine"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Liste der verfügbaren Gruppen
                groups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedGroupId = group.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGroupId == group.id,
                            onClick = { selectedGroupId = group.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = group.currentDerianDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    selectedGroupId?.let { onConfirm(it) }
                },
                enabled = selectedGroupId != null && selectedGroupId != currentGroupId
            ) {
                Text("Verschieben")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
