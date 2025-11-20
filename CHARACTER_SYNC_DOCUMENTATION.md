# Character Synchronisation System

## Überblick

Das Character Synchronisation System ermöglicht die Echtzeit-Synchronisation von Charakterdaten zwischen mehreren Geräten über Nearby Connections (Bluetooth/WLAN-Direct). Es ist speziell für Rollenspiel-Sessions konzipiert, bei denen ein Spielleiter (Game Master) die Charaktere mehrerer Spieler simultan sehen und verfolgen möchte.

**Wichtig**: Synchronisation funktioniert nur, solange beide Apps aktiv sind. Bei App-Beendigung wird die Session getrennt und beim Neustart durch einen Full-Sync wiederhergestellt.

## Architektur

### Star-Topologie

```
        [Spielleiter-Gerät]
              (Host)
                |
    +-----------+-----------+
    |           |           |
[Spieler 1] [Spieler 2] [Spieler 3]
 (Client)    (Client)    (Client)
```

- **Host (Spielleiter)**: Startet Advertising, akzeptiert eingehende Verbindungen (bis zu 4-6 Clients)
- **Clients (Spieler)**: Verbinden sich mit dem Host
- **Bidirektionale Kommunikation**: Beide Seiten können Updates senden
- **Last-Write-Wins**: Keine komplexe Konfliktauflösung, letzte Änderung gewinnt

### Komponenten

#### 1. CharacterRealtimeSyncManager

Zentrale Verwaltung der Synchronisation mit vereinfachtem Protokoll:

**Funktionen:**
- Verbindungsmanagement (Host/Client) über `NearbyConnectionsInterface`
- Full-Snapshot-basierte Synchronisation (verwendet `CharacterExportDto`)
- Update-Debouncing (500ms) zur Performance-Optimierung
- Last-Write-Wins Konfliktauflösung (GUID-basiert)
- Watchdog für Verbindungsüberwachung (Warnung nach 15s ohne Aktivität)

**API:**
```kotlin
// Host-Session starten (ein Charakter pro Session)
suspend fun startHostSession(characterId: Long, deviceName: String)

// Client-Session starten
suspend fun startClientSession(characterId: Long, hostEndpointId: String, deviceName: String)

// Session beenden
fun stopSession()

// Status beobachten
val syncStatus: StateFlow<SyncStatus>
```

**SyncStatus:**
```kotlin
sealed class SyncStatus {
    object Idle                                          // Keine Sync
    data class Connecting(deviceName: String)            // Verbindungsaufbau
    data class Syncing(characterGuid, endpointId, name)  // Aktive Sync
    data class Warning(characterGuid, message, staleSince) // > 15s keine Aktivität
    data class Error(message: String)                    // Fehler
}
```

#### 2. Repository-Integration

Neue Repository-Funktion für stillen Snapshot-Import:

```kotlin
suspend fun applySnapshotFromSync(
    snapshot: CharacterExportDto,
    allowCreateNew: Boolean = false
): Result<Long>
```

**Verhalten:**
- Sucht Charakter per GUID (nicht per Name!)
- Überschreibt komplett ohne UI-Dialoge (Charakter, Slots, Tränke, Items, Locations)
- `lastModifiedDate` wird auf `exportTimestamp` gesetzt
- Gruppe bleibt beim Überschreiben erhalten
- Optional: Neue Charaktere anlegen (nur wenn `allowCreateNew = true`)

#### 3. NearbyConnectionsInterface (unverändert)

Verwendet bestehende Methoden:

```kotlin
// Charakter-Daten senden (nutzt CharacterExportDto)
fun sendCharacterData(
    endpointId: String,
    characterData: CharacterExportDto,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)

// Charakter-Daten empfangen
fun receiveCharacterData(
    onDataReceived: (CharacterExportDto) -> Unit,
    onError: (String) -> Unit
)

// Verbindungsmanagement
fun startAdvertising(deviceName: String): Flow<ConnectionState>
fun startDiscovery(onDeviceFound: (String, String) -> Unit): Flow<ConnectionState>
fun connectToEndpoint(endpointId: String, deviceName: String): Flow<ConnectionState>
fun stopAllConnections()
```

## Protokoll-Spezifikation

### Vereinfachtes Full-Snapshot-Protokoll

**Kernprinzip**: Jede Änderung führt zu einem kompletten Export des Charakters als `CharacterExportDto`, der über Nearby gesendet wird.

**Vorteile:**
- Nutzt bestehende, getestete Export/Import-Infrastruktur
- Keine komplexe Diff-Logik notwendig
- Einfache Versionierung über `DataModelVersion`
- Robuste Fehlerbehandlung durch etablierte Code-Pfade

### Verbindungsaufbau

#### Host (Spielleiter):

1. Ruft `startHostSession(characterId, deviceName)` auf
2. Startet Advertising über `NearbyConnectionsInterface`
3. Akzeptiert eingehende Verbindungen automatisch
4. Sendet initialen Full-Snapshot an verbundenen Client
5. Startet Beobachtung des Charakters für weitere Updates

#### Client (Spieler):

1. Startet Discovery, findet Host über `NearbyConnectionsInterface`
2. Ruft `startClientSession(characterId, hostEndpointId, deviceName)` auf
3. Verbindet sich mit Host
4. Sendet initialen Full-Snapshot an Host
5. Startet Beobachtung des eigenen Charakters für weitere Updates

### Datenfluss

```
Client                          Host
  |                              |
  |--- Initial FullSync -------->|
  |<--- Initial FullSync ---------|
  |                              |
  [Client ändert LE: 30→25]      |
  |                              |
  |--- FullSync (LE=25) -------->|
  |     (nach 500ms Debounce)    |
  |                              |
  |                       [Host ändert AE: 25→20]
  |                              |
  |<--- FullSync (AE=20) ---------|
  |     (nach 500ms Debounce)    |
```

### Update-Debouncing

Um Netzwerk-Overhead zu minimieren:

1. Änderungen am Charakter werden per `Flow` beobachtet
2. `.debounce(500ms)` verhindert zu häufiges Senden
3. Bei mehreren schnellen Änderungen wird nur der finale Stand gesendet

**Beispiel:**
```
t=0ms:    LE-Änderung (30→29) -> Debounce startet
t=100ms:  LE-Änderung (29→28) -> Debounce neu gestartet
t=300ms:  LE-Änderung (28→27) -> Debounce neu gestartet
t=800ms:  Debounce abgelaufen -> FullSync mit LE=27 wird gesendet
```

### GUID-basierte Identität & Last-Write-Wins

**Identifikation:**
- Charaktere werden über ihre GUID identifiziert (nicht über Namen!)
- Jeder `CharacterExportDto` enthält die GUID im `character`-Feld
- Beim Empfang wird per GUID geprüft, ob der Charakter lokal existiert

**Konfliktauflösung (Last-Write-Wins):**
1. Jeder Snapshot enthält `exportTimestamp` (Zeitpunkt der Erstellung)
2. Beim Empfang wird der lokale Charakter **immer** überschrieben (keine Merge-Logik)
3. `lastModifiedDate` wird auf `exportTimestamp` des empfangenen Snapshots gesetzt
4. Bei bidirektionalen Änderungen: Die zuletzt gesendete Änderung gewinnt

**Empfehlung für Nutzung:**
- Spieler sollen nur ihren eigenen Charakter ändern
- Spielleiter hat Read-Only-Zugriff auf Spieler-Charaktere (beobachtet nur)
- Bei Konflikten: Nutzer sprechen sich am Spieltisch ab, wer gerade ändert
- Optionales Feature für später: Manueller "Lock/Unlock"-Toggle in der UI

### Watchdog & Verbindungsüberwachung

Alle 2 Sekunden prüft ein Watchdog:
- Wann war das letzte erfolgreiche Senden? (`lastSuccessfulSendTime`)
- Wann war das letzte erfolgreiche Empfangen? (`lastSuccessfulReceiveTime`)
- Falls > 15 Sekunden seit letzter Aktivität:
  - Status wechselt zu `SyncStatus.Warning`
  - UI kann Warnung anzeigen: "Sync möglicherweise veraltet"
- Bei erneutem Erfolg: Status zurück zu `SyncStatus.Syncing`

## Performance-Optimierungen

### 1. Full-Snapshot mit Debouncing

Statt inkrementeller Updates wird immer der komplette Charakter gesendet:
- **Vorteil**: Einfache Implementierung, robuste Synchronisation, keine Diff-Logik
- **Nachteil**: Größere Payloads (~2-5 KB pro Snapshot)
- **Mitigation**: 500ms Debouncing reduziert Übertragungsfrequenz drastisch

**Typische Payload-Größe:**
- Charakter mit 10 Slots, 5 Tränken, 10 Items: ~3-4 KB JSON
- Bei Bluetooth 2.0: ~2.1 Mbit/s → ~260 KB/s → ~65 Snapshots/s theoretisch möglich
- Bei WLAN-Direct: Deutlich höher

**Realistisches Szenario:**
- 1 Charakter-Änderung alle 10 Sekunden im Kampf
- Mit Debouncing: 1 Snapshot alle 10+ Sekunden
- 5 Clients: Max. 5 Snapshots/10s = 0.5 Snapshots/s
- Bandbreite: ~2 KB/s → völlig unkritisch

### 2. Effiziente Serialisierung

Kotlin Serialization mit JSON:
- Kompaktes Format (`prettyPrint = false`)
- `ignoreUnknownKeys = true` für Abwärtskompatibilität
- Wiederverwendung der bestehenden `CharacterExportDto`-Struktur

### 3. Repository-Optimierungen

`applySnapshotFromSync` nutzt effiziente Bulk-Operationen:
- Batch-Delete für alte Slots/Items/Locations
- Batch-Insert für neue Daten
- Transaktionale Updates vermeiden inkonsistente Zustände

### 4. Zukünftige Optimierungen (optional)

Falls Full-Snapshots doch zu groß werden:
- **Teilweise Updates**: Nur geänderte Subsysteme senden (z.B. nur Energien, nur Slots)
- **Kompression**: gzip für JSON-Payloads (kann 50-70% Größe sparen)
- **Diff-Algorithmus**: Nur tatsächliche Änderungen übertragen

Aktuell nicht notwendig, da Full-Snapshots mit Debouncing ausreichend performant sind.

## Skalierung

### Empfohlene Limits

- **Maximale Clients pro Host**: 4-6 (Nearby P2P_STAR unterstützt ~7 gleichzeitige Verbindungen)
- **Ein Charakter pro Client-Session**: Jede Session synchronisiert genau einen Charakter
- **Update-Frequenz**: ~1 Snapshot pro 10-60 Sekunden (durch Debouncing geregelt)
- **Payload-Größe**: ~2-5 KB pro Snapshot

### Warum 4-6 Clients?

1. **Bluetooth-Limitierung**: BLE unterstützt ca. 7 gleichzeitige Verbindungen
2. **Bandbreite**: Bei 6 Clients à 1 Snapshot/10s = ~0.6 Snapshots/s → ~3 KB/s → unkritisch
3. **Praktische Spielrunden**: Typische DSA-Runde hat 3-5 Spieler

### Lasttest-Szenarien

**Szenario 1: Standard-Session (3 Spieler)**
- 3 Clients verbunden, je 1 Charakter
- Durchschnittlich 1 Änderung/Minute pro Spieler (außerhalb Kampf)
- Host sendet ~3 Snapshots/Minute an alle
- **Erwartete Last**: <1 KB/s, vernachlässigbar

**Szenario 2: Intensive Session (5 Spieler, Kampf)**
- 5 Clients verbunden, je 1 Charakter
- Durchschnittlich 6 Änderungen/Minute pro Spieler (Energie-Änderungen im Kampf)
- Debouncing reduziert auf ~3 Snapshots/Minute pro Spieler
- Host sendet ~15 Snapshots/Minute an alle
- **Erwartete Last**: ~10 KB/s Netzwerk, <5% CPU

**Szenario 3: Stress-Test**
- 6 Clients, maximale Änderungsfrequenz
- 20 Änderungen/Minute pro Spieler (unrealistisch schnell)
- Debouncing reduziert auf ~10 Snapshots/Minute pro Spieler
- **Erwartete Last**: ~20-30 KB/s Netzwerk, ~10% CPU
- **Ergebnis**: Immer noch weit unter Limits von Bluetooth/WLAN

## Fehlerbehandlung

### Verbindungsabbruch

**Erkennung:**
- `ConnectionState.Disconnected` von `NearbyConnectionsInterface`
- Watchdog meldet > 15 Sekunden keine Aktivität → `SyncStatus.Warning`
- Sende-Fehler bei `sendCharacterData()`

**Behandlung:**
1. Session-Status wechselt zu `SyncStatus.Error` oder `Idle`
2. Alle Coroutine-Jobs werden gestoppt (observe, send, receive, watchdog)
3. `stopSession()` räumt Ressourcen auf
4. Bei Wiederverbindung: Neue Session starten → initialer Full-Sync gleicht Stände ab

### App im Hintergrund / geschlossen

**Problem:** Android kann Prozesse pausieren/killen, Nearby-Verbindungen gehen verloren

**Lösung:**
- **Kein Hintergrund-Service** (bewusste Design-Entscheidung für v1)
- Sync funktioniert nur, solange beide Apps aktiv/im Vordergrund sind
- Bei App-Beendigung:
  - Verbindung wird automatisch getrennt
  - Status wechselt zu `Disconnected` oder `Idle`
- Beim Wiedereinstieg:
  - Neue Session aufbauen
  - Initialer Full-Sync stellt identischen Stand wieder her

**Zukünftige Erweiterung (optional):**
- Android `ForegroundService` mit Notification für dauerhaften Hintergrund-Sync
- Erfordert mehr Permission-Handling und Battery-Optimierung

### Snapshot-Anwendung schlägt fehl

**Problem:** `applySnapshotFromSync` gibt Fehler zurück

**Mögliche Ursachen:**
- Versions-Inkompatibilität (zu alte/neue App-Version)
- GUID existiert nicht lokal und `allowCreateNew = false`
- Fehlende Foreign Keys (Zauber, Rezepte nicht in DB)
- Datenbankfehler

**Behandlung:**
- Fehler wird in `SyncStatus.Error` gemeldet
- UI zeigt Fehlermeldung an
- Sync-Session kann fortgesetzt werden (nächster Snapshot wird erneut versucht)
- Bei persistenten Fehlern: Session beenden und manuell prüfen

### Versionskompatibilität

**Problem:** Unterschiedliche App-Versionen zwischen Host und Client

**Lösung:**
- `CharacterExportDto` enthält `version`-Feld (`DataModelVersion.CURRENT_VERSION`)
- Beim Empfang prüft `applySnapshotFromSync` Kompatibilität via `DataModelVersion.checkCompatibility`
- Bei Inkompatibilität:
  - Snapshot wird abgelehnt
  - `SyncStatus.Error` mit Versions-Warnung
- `ignoreUnknownKeys = true` erlaubt Vorwärtskompatibilität (neue Felder werden ignoriert)

## Test-Infrastruktur

### FakeNearbyConnectionsService

Simuliert Nearby Connections ohne echtes Bluetooth/WLAN (bereits vorhanden):

```kotlin
// Setup
FakeNearbyConnectionsService.clearRegistry()
val hostService = FakeNearbyConnectionsService()
val clientService = FakeNearbyConnectionsService()

// Host startet
hostService.startAdvertising("Host Device")

// Client verbindet
clientService.connectToEndpoint("Host Device", "Client Device")

// Senden und Empfangen von CharacterExportDto
hostService.sendCharacterData(endpointId, characterDto, onSuccess, onFailure)
clientService.receiveCharacterData(onDataReceived, onError)
```

**Features:**
- Direkte In-Memory-Übertragung
- Unterstützt mehrere parallele Verbindungen
- Simulation von Disconnects/Fehlern via `simulateDisconnect()`, `simulateError()`
- Deterministisches Verhalten für Tests

### CharacterRealtimeSyncManagerTest

Neue Test-Suite für Sync-Manager:

**Test-Szenarien:**
1. **Host/Client-Verbindung**: Status wechselt zu `Syncing` nach erfolgreicher Verbindung
2. **Full-Snapshot-Send**: Änderungen am Charakter triggern Export und Senden
3. **Full-Snapshot-Receive**: Empfangene Snapshots werden per `applySnapshotFromSync` angewendet
4. **Last-Write-Wins**: Bei bidirektionalen Änderungen gewinnt der zuletzt gesendete Snapshot
5. **Watchdog-Warnung**: Nach 15s ohne Aktivität wechselt Status zu `Warning`
6. **Disconnect-Handling**: `stopSession()` räumt alle Ressourcen auf

**Verwendung:**
```kotlin
@Test
fun `test host and client sync character changes`() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    
    // Setup mit Mock-Repositories
    hostSyncManager = CharacterRealtimeSyncManager(
        hostRepository, hostService, hostExportManager, this
    )
    
    // Session starten
    launch(dispatcher) {
        hostSyncManager.startHostSession(characterId, "Host")
    }
    advanceUntilIdle()
    
    // Assertions
    val status = hostSyncManager.syncStatus.first()
    assertTrue(status is SyncStatus.Syncing)
}
```

## Sicherheit & Datenschutz

### Spielleiter-Modus

**WICHTIG:** Der `isGameMaster`-Flag wird NICHT synchronisiert!

- Spieler können Host-Charakter nicht als Spielleiter markieren
- Verhindert unerwünschte Sichtbarkeit von GM-Informationen
- Bleibt lokal auf jedem Gerät konfigurierbar

### Datensichtbarkeit

**Client (Spieler):**
- Sieht nur eigenen Charakter
- Erhält Updates nur für eigenen Charakter

**Host (Spielleiter):**
- Sieht alle synchronisierten Charaktere
- Erhält Updates von allen Clients
- Kann Read-Only-Ansicht aktivieren (keine Änderungen senden)

### Verschlüsselung

**Aktuell:** Keine End-to-End-Verschlüsselung

**Begründung:**
- Nearby Connections nutzt bereits verschlüsselte Bluetooth/WLAN-Verbindungen
- Alle Geräte sind physisch am gleichen Tisch (vertrauenswürdige Umgebung)

**Future:** Optionale Passwort-Absicherung für Session-Pairing

## Integration in die App

### 1. CharacterRealtimeSyncManager erstellen

```kotlin
val syncManager = CharacterRealtimeSyncManager(
    repository = applicatusRepository,
    nearbyService = nearbyConnectionsService, // oder FakeNearbyConnectionsService für Tests
    exportManager = characterExportManager,
    scope = viewModelScope
)
```

### 2. Host-Modus (Spielleiter)

```kotlin
// Starte Sync für einen Charakter
lifecycleScope.launch {
    syncManager.startHostSession(
        characterId = myCharacterId,
        deviceName = "GM Tablet"
    )
}

// Status beobachten
syncManager.syncStatus.collect { status ->
    when (status) {
        is CharacterRealtimeSyncManager.SyncStatus.Syncing -> {
            // Zeige "Synchronisiert mit ${status.endpointName}"
        }
        is CharacterRealtimeSyncManager.SyncStatus.Warning -> {
            // Zeige Warnung: "Sync veraltet seit ${status.staleSince}"
        }
        is CharacterRealtimeSyncManager.SyncStatus.Error -> {
            // Zeige Fehler: status.message
        }
        else -> { /* Idle oder Connecting */ }
    }
}

// Session beenden
syncManager.stopSession()
```

### 3. Client-Modus (Spieler)

```kotlin
// Verbinde mit Host
lifecycleScope.launch {
    syncManager.startClientSession(
        characterId = myCharacterId,
        hostEndpointId = "GM Tablet", // Von Discovery erhalten
        deviceName = "Player Phone"
    )
}

// Updates werden automatisch empfangen und in Repository gespeichert
// Keine manuelle Intervention nötig!
```

### 4. Charakteränderungen (automatisch synchronisiert)

```kotlin
// Einfach Charakter normal ändern:
fun takeDamage(damage: Int) {
    val updated = myCharacter.copy(currentLe = myCharacter.currentLe - damage)
    repository.updateCharacter(updated)
    
    // Sync-Manager beobachtet automatisch und sendet Update nach 500ms Debounce
}

// Keine expliziten sync*-Aufrufe nötig!
```

## Roadmap

### Phase 1 (✅ Implementiert)
- `CharacterRealtimeSyncManager` mit Full-Snapshot-Protokoll
- `applySnapshotFromSync` in `ApplicatusRepository`
- Bidirektionale Host/Client-Sessions
- Debouncing (500ms) für Performance
- Watchdog für 15-Sekunden-Warnung
- `FakeNearbyConnectionsService` für Tests
- Unit-Tests für Sync-Manager

### Phase 2 (Geplant)
- UI-Integration: Sync-Screen mit Status-Anzeige
- Discovery-Flow für Gerätesuche
- Verbindungsmanagement in CharacterDetailScreen
- Optional: "Lock/Unlock"-Toggle für explizite Schreibrechte

### Phase 3 (Optional)
- Optimierungen: Teilweise Updates (nur Energien, nur Slots)
- Kompression für große Payloads (gzip)
- Auto-Reconnect bei Verbindungsabbruch
- Session-Historie/Logs

### Phase 4 (Future)
- Android ForegroundService für Hintergrund-Sync
- Multi-Charakter-Sessions (Host synchronisiert mehrere Charaktere)
- Charakter-Log für "Wer hat was wann geändert"
- Session-Passwort-Schutz

## Troubleshooting

### Problem: Snapshots kommen nicht an

**Lösungen:**
1. Prüfe `syncStatus`: Ist Status `Syncing` oder `Warning`/`Error`?
2. Prüfe Logs auf Send/Receive-Fehler
3. Prüfe Nearby-Verbindung: `ConnectionState.Connected`?
4. Bei schlechter Verbindung: Geräte näher zusammenbringen (< 5m)

### Problem: Zu hoher Akkuverbrauch

**Lösungen:**
1. Debounce-Zeit erhöhen (aktuell 500ms → 1000ms)
2. Watchdog-Intervall erhöhen (aktuell 2s → 5s)
3. WiFi-Direct statt Bluetooth nutzen (schneller, energieeffizienter)
4. Session beenden, wenn nicht aktiv gespielt wird

### Problem: Verbindung bricht häufig ab

**Lösungen:**
1. Geräte näher zusammenbringen (< 5m)
2. Andere Bluetooth-Geräte entfernen (Interferenzen)
3. Prüfe auf WLAN/Mikrowellen-Interferenzen
4. Nutze WLAN-Direct statt Bluetooth
5. Android Battery-Optimierung für App deaktivieren

### Problem: "Sync veraltet"-Warnung nach 15 Sekunden

**Ursachen:**
- Verbindung unterbrochen, aber nicht explizit getrennt
- Sehr schlechte Bluetooth/WLAN-Qualität
- App im Hintergrund / Power-Saving aktiv

**Lösungen:**
1. Session neu starten (`stopSession()` → `startHostSession()`/`startClientSession()`)
2. Geräte näher zusammenbringen
3. App im Vordergrund halten

### Problem: Konflikte bei gleichzeitigen Änderungen

**Lösung:**
- Last-Write-Wins: Zuletzt gesendete Änderung gewinnt automatisch
- Am Spieltisch absprechen, wer gerade welchen Charakter ändert
- Optional: UI-Feature "Lock/Unlock" für explizite Schreibrechte (Phase 2)

### Problem: Version inkompatibel

**Lösung:**
- Beide Geräte auf gleiche App-Version aktualisieren
- Bei Minor-Version-Unterschieden: Sollte funktionieren (`ignoreUnknownKeys`)
- Bei Major-Version-Unterschieden: Update zwingend erforderlich

## Beispiel-Session

```kotlin
// === Spielleiter-Gerät ===
val gmSyncManager = CharacterRealtimeSyncManager(
    repository = applicatusRepository,
    nearbyService = NearbyConnectionsService(context),
    exportManager = CharacterExportManager(applicatusRepository),
    scope = viewModelScope
)

// Starte Host-Session für eigenen Spielleiter-Charakter
launch {
    gmSyncManager.startHostSession(
        characterId = gmCharacterId,
        deviceName = "GM iPad"
    )
}

// Beobachte Status
gmSyncManager.syncStatus.collect { status ->
    when (status) {
        is CharacterRealtimeSyncManager.SyncStatus.Syncing -> {
            println("Synchronisiert mit: ${status.endpointName}")
        }
        is CharacterRealtimeSyncManager.SyncStatus.Warning -> {
            println("WARNUNG: ${status.message}")
        }
        else -> { /* Idle, Connecting, Error */ }
    }
}

// Charakterdaten werden automatisch synchronisiert beim Update:
// repository.updateCharacter(gmCharacter.copy(currentLe = 25))


// === Spieler-Gerät ===
val playerSyncManager = CharacterRealtimeSyncManager(
    repository = applicatusRepository,
    nearbyService = NearbyConnectionsService(context),
    exportManager = CharacterExportManager(applicatusRepository),
    scope = viewModelScope
)

// Discovery durchführen (über NearbyConnectionsInterface)
nearbyService.startDiscovery { endpointId, endpointName ->
    if (endpointName == "GM iPad") {
        // Verbinde mit GM
        launch {
            playerSyncManager.startClientSession(
                characterId = myCharacterId,
                hostEndpointId = endpointId,
                deviceName = "Player Phone"
            )
        }
    }
}

// Ändere Charakter (wird automatisch synchronisiert)
fun takeDamage(damage: Int) {
    val updated = myCharacter.copy(currentLe = myCharacter.currentLe - damage)
    repository.updateCharacter(updated)
    // Nach 500ms Debounce wird Full-Snapshot an GM gesendet
}

// GM sieht automatisch die Änderungen:
repository.getCharacterByIdFlow(playerCharacterId).collect { character ->
    // UI aktualisiert sich automatisch mit Spieler-Änderungen
    updateUI(character)
}
```

---

**Hinweis:** Diese Dokumentation beschreibt die aktuelle vereinfachte Implementierung (Phase 1). Full-Snapshot-Protokoll mit Last-Write-Wins, keine inkrementellen Updates.
