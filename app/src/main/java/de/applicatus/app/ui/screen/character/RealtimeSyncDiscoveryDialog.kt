package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.sync.CharacterRealtimeSyncManager

/**
 * Dialog for discovering and connecting to devices for real-time character synchronization.
 * 
 * Supports two modes:
 * - Host: Advertise the character and wait for clients to connect
 * - Join: Discover available hosts and select one to connect to
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeSyncDiscoveryDialog(
    characterName: String,
    syncStatus: CharacterRealtimeSyncManager.SyncStatus,
    discoveredEndpoints: Map<String, String>, // endpointId -> endpointName
    onHost: () -> Unit,
    onStartDiscovery: () -> Unit,
    onJoin: (endpointId: String, endpointName: String) -> Unit,
    onStopSync: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf<SyncMode?>(null) }
    
    AlertDialog(
        onDismissRequest = {
            if (syncStatus is CharacterRealtimeSyncManager.SyncStatus.Idle) {
                onDismiss()
            }
        },
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Echtzeit-Synchronisation")
                if (syncStatus is CharacterRealtimeSyncManager.SyncStatus.Idle) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = characterName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                when (syncStatus) {
                    is CharacterRealtimeSyncManager.SyncStatus.Idle -> {
                        if (mode == null) {
                            // Mode Selection
                            Text("Wähle eine Option:")
                            
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mode = SyncMode.HOST },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Column {
                                        Text(
                                            "Als Host anbieten",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Andere Geräte können sich verbinden",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mode = SyncMode.JOIN },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Column {
                                        Text(
                                            "Zu Host verbinden",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Suche nach verfügbaren Hosts",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        } else if (mode == SyncMode.HOST) {
                            // Host Mode: Start Advertising
                            Text("Starte Host-Modus...")
                            Button(
                                onClick = { onHost() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Host starten")
                            }
                            TextButton(
                                onClick = { mode = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Zurück")
                            }
                        } else if (mode == SyncMode.JOIN) {
                            // Join Mode: Discover and Select Host
                            Text("Suche nach Hosts...")
                            
                            // Start discovery when entering join mode
                            LaunchedEffect(Unit) {
                                onStartDiscovery()
                            }
                            
                            if (discoveredEndpoints.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                                Text(
                                    "Keine Hosts gefunden. Stelle sicher, dass ein anderes Gerät als Host läuft.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(discoveredEndpoints.entries.toList()) { (endpointId, endpointName) ->
                                        OutlinedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onJoin(endpointId, endpointName) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null)
                                                Column {
                                                    Text(
                                                        endpointName,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        "Tippen zum Verbinden",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            TextButton(
                                onClick = { mode = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Zurück")
                            }
                        }
                    }
                    
                    is CharacterRealtimeSyncManager.SyncStatus.Connecting -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        Text(
                            text = "Verbinde mit ${syncStatus.deviceName}...",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    is CharacterRealtimeSyncManager.SyncStatus.Syncing -> {
                        Text(
                            "✅ Synchronisiert mit ${syncStatus.endpointName}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Alle Änderungen werden automatisch übertragen.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    is CharacterRealtimeSyncManager.SyncStatus.Warning -> {
                        Text(
                            "⚠️ ${syncStatus.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Verbindung seit ${(System.currentTimeMillis() - syncStatus.staleSince) / 1000}s unterbrochen",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    is CharacterRealtimeSyncManager.SyncStatus.Error -> {
                        Text(
                            "❌ Fehler: ${syncStatus.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (syncStatus !is CharacterRealtimeSyncManager.SyncStatus.Idle) {
                Button(onClick = onStopSync) {
                    Text("Sync beenden")
                }
            }
        }
    )
}

private enum class SyncMode {
    HOST,
    JOIN
}
