package de.applicatus.app.ui.screen.magicsign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.inventory.Item
import de.applicatus.app.data.model.magicsign.MagicSign
import de.applicatus.app.data.model.magicsign.MagicSignDuration
import de.applicatus.app.data.model.magicsign.MagicSignEffect
import de.applicatus.app.data.model.magicsign.MagicSignWithItem
import de.applicatus.app.logic.MagicSignChecker
import de.applicatus.app.ui.viewmodel.MagicSignViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicSignScreen(
    viewModel: MagicSignViewModel,
    onNavigateBack: () -> Unit
) {
    val character by viewModel.character.collectAsState()
    val magicSignsWithItems by viewModel.magicSignsWithItems.collectAsState()
    val isGameMaster by viewModel.isGameMasterGroup.collectAsState()
    val availableTargets by viewModel.availableTargets.collectAsState()
    val currentDate by viewModel.currentDerianDate.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var signToDelete by remember { mutableStateOf<MagicSign?>(null) }
    
    val canUseZauberzeichen = character?.let { MagicSignChecker.canUseZauberzeichen(it) } ?: false
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zauberzeichen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (isGameMaster) {
                        Icon(
                            painter = painterResource(R.drawable.ic_game_master_mask),
                            contentDescription = "Spielleiter-Modus",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (canUseZauberzeichen && availableTargets.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Zauberzeichen hinzufügen")
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
            // Charakterinfo-Card
            character?.let { char ->
                CharacterInfoCard(
                    rkw = char.ritualKnowledgeValue,
                    hasZauberzeichen = char.hasZauberzeichen,
                    canUseZauberzeichen = canUseZauberzeichen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (!canUseZauberzeichen) {
                // Hinweis wenn Voraussetzungen nicht erfüllt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Voraussetzungen nicht erfüllt",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Um Zauberzeichen zu nutzen, benötigt der Charakter:\n" +
                                   "• Die Sonderfertigkeit \"Zauberzeichen\"\n" +
                                   "• Einen Ritualkenntniswert (RkW) > 0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else if (magicSignsWithItems.isEmpty()) {
                // Leere Liste
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Noch keine Zauberzeichen erstellt.\n\nTippe auf + um ein neues Zauberzeichen hinzuzufügen.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Liste der Zauberzeichen
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(magicSignsWithItems, key = { it.magicSign.id }) { signWithItem ->
                        MagicSignCard(
                            signWithItem = signWithItem,
                            isGameMaster = isGameMaster,
                            isExpired = viewModel.isExpired(signWithItem.magicSign),
                            onActivate = { viewModel.activateMagicSign(signWithItem.magicSign) },
                            onDelete = { signToDelete = signWithItem.magicSign }
                        )
                    }
                }
            }
        }
    }
    
    // Aktivierungsergebnis-Dialog
    if (viewModel.showActivationResult) {
        viewModel.lastActivationResult?.let { result ->
            ActivationResultDialog(
                result = result,
                onDismiss = { viewModel.dismissActivationResult() }
            )
        }
    }
    
    // Hinzufügen-Dialog
    if (showAddDialog) {
        AddMagicSignDialog(
            availableTargets = availableTargets,
            rkw = character?.ritualKnowledgeValue ?: 0,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description, effect, modifier, duration, targetId ->
                viewModel.createMagicSign(name, description, effect, modifier, duration, targetId)
                showAddDialog = false
            }
        )
    }
    
    // Lösch-Bestätigung
    signToDelete?.let { sign ->
        AlertDialog(
            onDismissRequest = { signToDelete = null },
            title = { Text("Zauberzeichen entfernen?") },
            text = { Text("Möchtest du das Zauberzeichen '${sign.name}' wirklich entfernen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMagicSign(sign)
                        signToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Entfernen")
                }
            },
            dismissButton = {
                TextButton(onClick = { signToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun CharacterInfoCard(
    rkw: Int,
    hasZauberzeichen: Boolean,
    canUseZauberzeichen: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (canUseZauberzeichen) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ritualkenntnis (RkW)",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "$rkw",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SF Zauberzeichen",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = if (hasZauberzeichen) "✓ Ja" else "✗ Nein",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasZauberzeichen) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MagicSignCard(
    signWithItem: MagicSignWithItem,
    isGameMaster: Boolean,
    isExpired: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    val sign = signWithItem.magicSign
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                sign.isBotched && isGameMaster -> MaterialTheme.colorScheme.errorContainer
                isExpired -> MaterialTheme.colorScheme.surfaceVariant
                sign.isActivated -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Titel-Zeile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sign.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auf: ${signWithItem.itemName ?: "Unbekannt"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status-Badge
                when {
                    sign.isBotched && isGameMaster -> {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("Verdorben", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    isExpired -> {
                        Badge(containerColor = MaterialTheme.colorScheme.outline) {
                            Text("Abgelaufen", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    sign.isActivated -> {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("Aktiv", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    else -> {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                            Text("Inaktiv", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Details
            if (sign.effectDescription.isNotBlank()) {
                Text(
                    text = sign.effectDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Technische Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Erleichterung: +${sign.activationModifier}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Dauer: ${sign.duration.displayName}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Aktivierte Details
            if (sign.isActivated && !sign.isBotched) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    sign.activationRkpStar?.let { rkp ->
                        Text(
                            text = "RkP*: $rkp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    sign.expiryDate?.let { date ->
                        Text(
                            text = "Wirkt bis: $date",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Spezialeffekt-Info
                if (sign.effect == MagicSignEffect.WEIGHT_REDUCTION && sign.activationRkpStar != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🪶 Gewichtsreduktion: -${sign.activationRkpStar * 2} Stein",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            // Spielleiter-Info (Patzer)
            if (sign.isBotched && isGameMaster) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Patzer bei der Aktivierung - das Zeichen ist wirkungslos!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            
            // Aktionen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Aktivieren (nur wenn nicht aktiviert und nicht verdorben)
                if (!sign.isActivated && !sign.isBotched) {
                    TextButton(onClick = onActivate) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aktivieren")
                    }
                }
                
                // Löschen
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Entfernen")
                }
            }
        }
    }
}

@Composable
private fun ActivationResultDialog(
    result: de.applicatus.app.logic.MagicSignActivationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    result.isTripleOne -> "🌟 Dreifach-1!"
                    result.isDoubleOne -> "⭐ Doppel-1!"
                    result.isTripleTwenty -> "💀💀💀 Dreifach-20!"
                    result.isDoubleTwenty -> "💀💀 Doppel-20!"
                    result.success -> "✅ Aktivierung erfolgreich"
                    else -> "❌ Aktivierung fehlgeschlagen"
                }
            )
        },
        text = {
            Column {
                Text(result.formattedRollResult)
                
                if (result.success && !result.isBotched && result.calculatedExpiryDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Das Zauberzeichen wirkt bis: ${result.calculatedExpiryDate}",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (result.isBotched) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Das Zauberzeichen ist verdorben und wirkungslos!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMagicSignDialog(
    availableTargets: List<Item>,
    rkw: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String, MagicSignEffect, Int, MagicSignDuration, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEffect by remember { mutableStateOf(MagicSignEffect.NONE) }
    var activationModifier by remember { mutableStateOf(0) }
    var selectedDuration by remember { mutableStateOf(MagicSignDuration.HALF_RKW_DAYS) }
    var selectedTargetId by remember { mutableStateOf<Long?>(null) }
    var showTargetDropdown by remember { mutableStateOf(false) }
    var showEffectDropdown by remember { mutableStateOf(false) }
    var showDurationDropdown by remember { mutableStateOf(false) }
    
    val isValid = name.isNotBlank() && selectedTargetId != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Zauberzeichen") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name des Zeichens") },
                        placeholder = { Text("z.B. Sigille des Unsichtbaren Trägers") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Wirkungsbeschreibung (optional)") },
                        placeholder = { Text("z.B. Reduziert das Gewicht...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Zielobjekt
                item {
                    Text("Zielobjekt:", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(
                        expanded = showTargetDropdown,
                        onExpandedChange = { showTargetDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = availableTargets.find { it.id == selectedTargetId }?.name ?: "Bitte auswählen",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTargetDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showTargetDropdown,
                            onDismissRequest = { showTargetDropdown = false }
                        ) {
                            availableTargets.forEach { target ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            if (target.isSelfItem) "📍 ${target.name}" else target.name
                                        )
                                    },
                                    onClick = {
                                        selectedTargetId = target.id
                                        showTargetDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Spezialeffekt
                item {
                    Text("Spezialeffekt:", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(
                        expanded = showEffectDropdown,
                        onExpandedChange = { showEffectDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEffect.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEffectDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showEffectDropdown,
                            onDismissRequest = { showEffectDropdown = false }
                        ) {
                            MagicSignEffect.entries.forEach { effect ->
                                DropdownMenuItem(
                                    text = { Text(effect.displayName) },
                                    onClick = {
                                        selectedEffect = effect
                                        // Wenn Sigille des Unsichtbaren Trägers ausgewählt wird, Name vorausfüllen
                                        if (effect == MagicSignEffect.WEIGHT_REDUCTION && name.isBlank()) {
                                            name = "Sigille des Unsichtbaren Trägers"
                                            description = "Reduziert das Gewicht des Objekts um RkP* × 2 Stein"
                                        }
                                        showEffectDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Wirkdauer
                item {
                    Text("Wirkdauer:", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(
                        expanded = showDurationDropdown,
                        onExpandedChange = { showDurationDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = when (selectedDuration) {
                                MagicSignDuration.HALF_RKW_DAYS -> "${(rkw + 1) / 2} Tage (RkW/2)"
                                else -> selectedDuration.displayName
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDurationDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showDurationDropdown,
                            onDismissRequest = { showDurationDropdown = false }
                        ) {
                            MagicSignDuration.entries.forEach { duration ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            when (duration) {
                                                MagicSignDuration.HALF_RKW_DAYS -> "${(rkw + 1) / 2} Tage (RkW/2)"
                                                else -> duration.displayName
                                            }
                                        )
                                    },
                                    onClick = {
                                        selectedDuration = duration
                                        showDurationDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Erleichterung
                item {
                    Text("Erleichterung auf Aktivierung:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (activationModifier > 0) activationModifier-- }
                        ) {
                            Text("-", style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(
                            text = "+$activationModifier",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(
                            onClick = { if (activationModifier < 20) activationModifier++ }
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    Text(
                        text = "Hängt von der Qualität der Erstellung ab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedTargetId?.let { targetId ->
                        onConfirm(name, description, selectedEffect, activationModifier, selectedDuration, targetId)
                    }
                },
                enabled = isValid
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
