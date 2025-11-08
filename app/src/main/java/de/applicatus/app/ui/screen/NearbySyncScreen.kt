package de.applicatus.app.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.nearby.NearbyConnectionsInterface
import de.applicatus.app.ui.viewmodel.NearbySyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbySyncScreen(
    viewModel: NearbySyncViewModel,
    characterId: Long?,  // null = Empfangsmodus (von CharacterListScreen)
    characterName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf(android.os.Build.MODEL) }
    
    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            showPermissionDialog = true
        }
    }
    
    // Request Permissions on start
    LaunchedEffect(Unit) {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAllConnections()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (characterId != null) "Nearby Sync: $characterName" 
                        else "Nearby Sync: Empfangen"
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAllConnections()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection State
            ConnectionStateCard(viewModel.connectionState)
            
            // Sync State
            if (viewModel.syncState !is NearbySyncViewModel.SyncState.Idle) {
                SyncStateCard(viewModel.syncState) {
                    viewModel.resetSyncState()
                }
            }
            
            // Action Buttons
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Aktionen", style = MaterialTheme.typography.titleMedium)
                    
                    // Send Character Button (nur wenn characterId vorhanden)
                    if (characterId != null) {
                        Button(
                            onClick = { viewModel.sendCharacter(characterId) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.connectionState is NearbyConnectionsInterface.ConnectionState.Connected
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Charakter '$characterName' senden")
                        }
                    } else {
                        // Im Empfangsmodus: Hinweis anzeigen
                        Text(
                            text = "Empfangsmodus: Verbinde mit einem Gerät, um einen Charakter zu empfangen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Advertise Button
                    Button(
                        onClick = { viewModel.startAdvertising(deviceName) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isAdvertising && viewModel.connectionState !is NearbyConnectionsInterface.ConnectionState.Connected
                    ) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (viewModel.isAdvertising) "Warte auf Verbindung..." else "Als Empfänger bereitstellen")
                    }
                    
                    // Discovery Button
                    Button(
                        onClick = { 
                            viewModel.startDiscovery { _, _ -> }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isDiscovering && viewModel.connectionState !is NearbyConnectionsInterface.ConnectionState.Connected
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (viewModel.isDiscovering) "Suche läuft..." else "Nach Geräten suchen")
                    }
                    
                    // Disconnect Button
                    if (viewModel.connectionState is NearbyConnectionsInterface.ConnectionState.Connected ||
                        viewModel.isAdvertising || viewModel.isDiscovering) {
                        OutlinedButton(
                            onClick = { viewModel.stopAllConnections() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Verbindung trennen")
                        }
                    }
                }
            }
            
            // Discovered Devices
            if (viewModel.discoveredDevices.isNotEmpty()) {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Gefundene Geräte", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.discoveredDevices) { device ->
                                DeviceItem(
                                    device = device,
                                    onClick = {
                                        viewModel.connectToDevice(device, deviceName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "ℹ️ Nearby Connections",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Beide Geräte müssen Bluetooth und WLAN aktiviert haben\n" +
                        "• Ein Gerät muss als Empfänger bereitgestellt werden\n" +
                        "• Das andere Gerät sucht nach verfügbaren Geräten\n" +
                        "• Nach Verbindung kann der Charakter gesendet werden",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Berechtigungen erforderlich") },
            text = { Text("Nearby Connections benötigt Bluetooth- und Standort-Berechtigungen.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ConnectionStateCard(state: NearbyConnectionsInterface.ConnectionState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is NearbyConnectionsInterface.ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is NearbyConnectionsInterface.ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (state) {
                    is NearbyConnectionsInterface.ConnectionState.Idle -> Icons.Default.Info
                    is NearbyConnectionsInterface.ConnectionState.Advertising -> Icons.Default.Settings
                    is NearbyConnectionsInterface.ConnectionState.Discovering -> Icons.Default.Search
                    is NearbyConnectionsInterface.ConnectionState.Connected -> Icons.Default.Check
                    is NearbyConnectionsInterface.ConnectionState.Disconnected -> Icons.Default.Info
                    is NearbyConnectionsInterface.ConnectionState.Error -> Icons.Default.Warning
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    when (state) {
                        is NearbyConnectionsInterface.ConnectionState.Idle -> "Bereit"
                        is NearbyConnectionsInterface.ConnectionState.Advertising -> "Warte auf Verbindung..."
                        is NearbyConnectionsInterface.ConnectionState.Discovering -> "Suche nach Geräten..."
                        is NearbyConnectionsInterface.ConnectionState.Connected -> "Verbunden: ${state.endpointName}"
                        is NearbyConnectionsInterface.ConnectionState.Disconnected -> "Getrennt: ${state.reason}"
                        is NearbyConnectionsInterface.ConnectionState.Error -> "Fehler"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                
                if (state is NearbyConnectionsInterface.ConnectionState.Error) {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun SyncStateCard(
    state: NearbySyncViewModel.SyncState,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is NearbySyncViewModel.SyncState.Success -> MaterialTheme.colorScheme.primaryContainer
                is NearbySyncViewModel.SyncState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                when (state) {
                    is NearbySyncViewModel.SyncState.Sending -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Sende Daten...")
                    }
                    is NearbySyncViewModel.SyncState.Receiving -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Empfange Daten...")
                    }
                    is NearbySyncViewModel.SyncState.Success -> {
                        Icon(Icons.Default.Check, null)
                        Text(state.message)
                    }
                    is NearbySyncViewModel.SyncState.Error -> {
                        Icon(Icons.Default.Warning, null)
                        Text(state.message)
                    }
                    else -> {}
                }
            }
            
            if (state is NearbySyncViewModel.SyncState.Success || 
                state is NearbySyncViewModel.SyncState.Error) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Schließen")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceItem(
    device: NearbySyncViewModel.DiscoveredDevice,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Phone, null)
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tippen zum Verbinden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ArrowForward, null)
        }
    }
}

