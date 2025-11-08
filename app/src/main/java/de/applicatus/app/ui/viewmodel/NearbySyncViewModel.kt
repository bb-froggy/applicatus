package de.applicatus.app.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.CharacterExportManager
import de.applicatus.app.data.nearby.NearbyConnectionsInterface
import de.applicatus.app.data.nearby.NearbyConnectionsService
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NearbySyncViewModel(
    private val repository: ApplicatusRepository,
    private val nearbyService: NearbyConnectionsInterface
) : ViewModel() {
    
    private val exportManager = CharacterExportManager(repository)
    
    var connectionState by mutableStateOf<NearbyConnectionsInterface.ConnectionState>(
        NearbyConnectionsInterface.ConnectionState.Idle
    )
        private set
    
    var syncState by mutableStateOf<SyncState>(SyncState.Idle)
        private set
    
    var isAdvertising by mutableStateOf(false)
        private set
    
    var isDiscovering by mutableStateOf(false)
        private set
    
    private var currentEndpointId: String? = null
    
    val discoveredDevices = mutableStateListOf<DiscoveredDevice>()
    
    sealed class SyncState {
        object Idle : SyncState()
        object Sending : SyncState()
        object Receiving : SyncState()
        data class Success(val message: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }
    
    data class DiscoveredDevice(
        val endpointId: String,
        val name: String
    )
    
    fun startAdvertising(deviceName: String) {
        viewModelScope.launch {
            nearbyService.startAdvertising(deviceName).collect { state ->
                connectionState = state
                isAdvertising = state is NearbyConnectionsInterface.ConnectionState.Advertising
                
                if (state is NearbyConnectionsInterface.ConnectionState.Connected) {
                    currentEndpointId = state.endpointId
                    // Bereit zum Empfangen
                    setupReceiver()
                }
            }
        }
    }
    
    fun startDiscovery(onDeviceFound: (String, String) -> Unit) {
        discoveredDevices.clear()
        viewModelScope.launch {
            nearbyService.startDiscovery { endpointId, name ->
                val device = DiscoveredDevice(endpointId, name)
                if (discoveredDevices.none { it.endpointId == endpointId }) {
                    discoveredDevices.add(device)
                }
                onDeviceFound(endpointId, name)
            }.collect { state ->
                connectionState = state
                isDiscovering = state is NearbyConnectionsInterface.ConnectionState.Discovering
                
                if (state is NearbyConnectionsInterface.ConnectionState.Connected) {
                    currentEndpointId = state.endpointId
                    // Bereit zum Senden/Empfangen
                    setupReceiver()
                }
            }
        }
    }
    
    fun connectToDevice(device: DiscoveredDevice, deviceName: String) {
        viewModelScope.launch {
            nearbyService.connectToEndpoint(device.endpointId, deviceName).collect { state ->
                connectionState = state
                
                if (state is NearbyConnectionsInterface.ConnectionState.Connected) {
                    currentEndpointId = state.endpointId
                    setupReceiver()
                }
            }
        }
    }
    
    private fun setupReceiver() {
        nearbyService.receiveCharacterData(
            onDataReceived = { dto ->
                viewModelScope.launch {
                    handleReceivedCharacter(dto)
                }
            },
            onError = { error ->
                syncState = SyncState.Error(error)
            }
        )
    }
    
    private suspend fun handleReceivedCharacter(dto: CharacterExportDto) {
        syncState = SyncState.Receiving
        
        // Prüfe, ob Charakter bereits existiert
        val characters = repository.allCharacters.first()
        val existingCharacter = characters.find { it.name == dto.character.name }
        
        if (existingCharacter != null) {
            // Zeige Warnung - hier würde in der UI eine Bestätigung kommen
            syncState = SyncState.Error(
                "Charakter '${dto.character.name}' existiert bereits. " +
                "Bitte in der UI Überschreiben bestätigen."
            )
        } else {
            // Import direkt
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }
            val jsonString = json.encodeToString(
                kotlinx.serialization.serializer<CharacterExportDto>(),
                dto
            )
            // Import ohne targetCharacterId = neuer Charakter wird angelegt
            val result = exportManager.importCharacter(
                jsonString,
                targetCharacterId = null
            )
            
            syncState = if (result.isSuccess) {
                val (_, warning) = result.getOrNull()!!
                val message = if (warning != null) {
                    "Charakter empfangen mit Warnung:\n$warning"
                } else {
                    "Charakter '${dto.character.name}' erfolgreich empfangen"
                }
                SyncState.Success(message)
            } else {
                SyncState.Error("Empfang fehlgeschlagen: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun sendCharacter(characterId: Long) {
        viewModelScope.launch {
            val endpointId = currentEndpointId
            if (endpointId == null) {
                syncState = SyncState.Error("Keine Verbindung vorhanden")
                return@launch
            }
            
            syncState = SyncState.Sending
            
            // Exportiere Charakter
            val exportResult = exportManager.exportCharacter(characterId)
            if (exportResult.isFailure) {
                syncState = SyncState.Error("Export fehlgeschlagen: ${exportResult.exceptionOrNull()?.message}")
                return@launch
            }
            
            // Parse zu DTO
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val dto = json.decodeFromString<CharacterExportDto>(exportResult.getOrNull()!!)
            
            // Sende über Nearby
            nearbyService.sendCharacterData(
                endpointId = endpointId,
                characterData = dto,
                onSuccess = {
                    syncState = SyncState.Success("Charakter erfolgreich gesendet")
                },
                onFailure = { error ->
                    syncState = SyncState.Error("Senden fehlgeschlagen: $error")
                }
            )
        }
    }
    
    fun stopAllConnections() {
        nearbyService.stopAllConnections()
        connectionState = NearbyConnectionsInterface.ConnectionState.Idle
        isAdvertising = false
        isDiscovering = false
        currentEndpointId = null
        discoveredDevices.clear()
    }
    
    fun resetSyncState() {
        syncState = SyncState.Idle
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAllConnections()
    }
}

class NearbySyncViewModelFactory(
    private val repository: ApplicatusRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NearbySyncViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NearbySyncViewModel(
                repository,
                NearbyConnectionsService(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
