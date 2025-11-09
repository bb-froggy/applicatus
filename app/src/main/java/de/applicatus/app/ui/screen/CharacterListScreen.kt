package de.applicatus.app.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.ui.viewmodel.CharacterListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    viewModel: CharacterListViewModel,
    onCharacterClick: (Long) -> Unit,
    onNearbySyncClick: () -> Unit
) {
    val context = LocalContext.current
    val characters by viewModel.characters.collectAsState()
    val globalSettings by viewModel.globalSettings.collectAsState()
    val importState = viewModel.importState
    val spellSyncState = viewModel.spellSyncState
    val isDateEditMode by remember { derivedStateOf { viewModel.isDateEditMode } }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // File picker für JSON-Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCharacterFromFile(context, it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charaktere") },
                actions = {
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
                        Divider()
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
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Neuer Charakter")
            }
        }
    ) { padding ->
        val groupedCharacters = characters.groupBy { it.group }.toSortedMap()
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Derisches Datum anzeigen
            item {
                DerianDateCard(
                    currentDate = globalSettings?.currentDerianDate ?: "1 Praios 1040 BF",
                    isEditMode = isDateEditMode,
                    onToggleEditMode = { viewModel.toggleDateEditMode() },
                    onUpdateDate = { viewModel.updateDerianDate(it) },
                    onIncrement = { viewModel.incrementDerianDate() },
                    onDecrement = { viewModel.decrementDerianDate() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            groupedCharacters.forEach { (group, charactersInGroup) ->
                item {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                
                items(charactersInGroup, key = { it.id }) { character ->
                    CharacterListItem(
                        character = character,
                        onClick = { onCharacterClick(character.id) },
                        onDelete = { viewModel.deleteCharacter(character) }
                    )
                }
            }
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
    
    if (showAddDialog) {
        AddCharacterDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, group, mu, kl, inValue, ch, ff, ge, ko, kk, hasApplicatus, applicatusZfw, applicatusModifier ->
                viewModel.addCharacter(
                    name, group, mu, kl, inValue, ch, ff, ge, ko, kk,
                    hasApplicatus, applicatusZfw, applicatusModifier
                )
                showAddDialog = false
            }
        )
    }
    
    // Import Status Dialog
    when (importState) {
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
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CharacterListItem(
    character: Character,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    val dismissState = rememberDismissState(
        confirmStateChange = { dismissValue ->
            if (dismissValue == DismissValue.DismissedToStart) {
                showDeleteConfirmation = true
                false // Verhindert automatisches Löschen, wir warten auf Bestätigung
            } else {
                false
            }
        }
    )
    
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart), // Nur von rechts nach links
        background = {
            // Roter Hintergrund mit Lösch-Icon beim Swipen
            val color = if (dismissState.dismissDirection == DismissDirection.EndToStart) {
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
                if (dismissState.dismissDirection == DismissDirection.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = Color.White
                    )
                }
            }
        },
        dismissContent = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
            dismissState.reset()
        }
    }
}

@Composable
fun AddCharacterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int, Int, Int, Int, Int, Int, Int, Boolean, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("Meine Gruppe") }
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
                            group,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Derisches Datum",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                TextButton(onClick = onToggleEditMode) {
                    Text(if (isEditMode) "Fertig" else "Bearbeiten")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isEditMode) {
                // Bearbeitungsmodus: Vollständige Datumsbearbeitung
                OutlinedTextField(
                    value = editingDate,
                    onValueChange = { editingDate = it },
                    label = { Text("Datum (z.B. 15 Praios 1040 BF)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        onUpdateDate(editingDate)
                        onToggleEditMode()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Datum speichern")
                }
            } else {
                // Nutzungsmodus: +/- Buttons
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
