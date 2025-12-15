# Nearby Connections Test-Infrastruktur

## Überblick

Diese Test-Infrastruktur ermöglicht es, die Nearby-Connections-Funktionalität ohne echte Bluetooth/WLAN-Verbindungen zu testen. Die Tests simulieren die Kommunikation zwischen zwei Geräten direkt im Speicher.

## Architektur

### Interface-Abstraktion

Die Implementierung verwendet ein Interface-Pattern zur Abstraktion:

- **`NearbyConnectionsInterface`**: Interface, das die Kernfunktionalität definiert
- **`NearbyConnectionsService`**: Produktions-Implementierung mit Google Nearby Connections API
- **`FakeNearbyConnectionsService`**: Test-Implementierung für simulierte Verbindungen

### Fake-Implementierung

Die `FakeNearbyConnectionsService` bietet:

1. **Service-Registry**: Zentrale Verwaltung aller Fake-Services für simulierte Verbindungen
2. **Direkte Datenübertragung**: Daten werden direkt von einem Service zum anderen übertragen
3. **Bidirektionale Verbindungen**: Beide Seiten können senden und empfangen
4. **Verbindungszustände**: Vollständige Simulation von Connection States
5. **Fehler-Simulation**: Methoden zum Simulieren von Verbindungsabbrüchen und Fehlern

## Verwendung

### Grundlegendes Test-Setup

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyNearbyTest {
    private lateinit var senderService: FakeNearbyConnectionsService
    private lateinit var receiverService: FakeNearbyConnectionsService
    
    @Before
    fun setup() {
        FakeNearbyConnectionsService.clearRegistry()
        senderService = FakeNearbyConnectionsService()
        receiverService = FakeNearbyConnectionsService()
    }
    
    @After
    fun tearDown() {
        senderService.stopAllConnections()
        receiverService.stopAllConnections()
        FakeNearbyConnectionsService.clearRegistry()
    }
}
```

### Verbindung herstellen

```kotlin
@Test
fun `test connection`() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    
    // Empfänger startet Advertising
    launch(dispatcher) {
        receiverService.startAdvertising("Receiver Device")
    }
    advanceUntilIdle()
    
    // Sender verbindet sich mit Empfänger
    launch(dispatcher) {
        senderService.connectToEndpoint("Receiver Device", "Sender Device")
    }
    advanceUntilIdle()
    
    // Prüfe Verbindungsstatus
    val state = senderService.connectionStateFlow.first()
    assertTrue(state is NearbyConnectionsInterface.ConnectionState.Connected)
}
```

### Daten senden und empfangen

```kotlin
@Test
fun `test data transfer`() = runTest {
    // ... Verbindung herstellen (siehe oben) ...
    
    // Empfänger bereit zum Empfangen
    var receivedData: CharacterExportDto? = null
    receiverService.receiveCharacterData(
        onDataReceived = { data -> receivedData = data },
        onError = { }
    )
    
    // Sender sendet Daten
    var sendSuccess = false
    senderService.sendCharacterData(
        endpointId = "Receiver Device",
        characterData = testData,
        onSuccess = { sendSuccess = true },
        onFailure = { }
    )
    advanceUntilIdle()
    
    // Assertions
    assertTrue(sendSuccess)
    assertNotNull(receivedData)
}
```

### Bidirektionale Kommunikation

Beide Seiten können gleichzeitig als Sender und Empfänger agieren:

```kotlin
@Test
fun `test bidirectional transfer`() = runTest {
    // ... Verbindung herstellen ...
    
    // Beide Seiten empfangen
    var dataReceivedByA: CharacterExportDto? = null
    receiverService.receiveCharacterData(
        onDataReceived = { data -> dataReceivedByA = data },
        onError = { }
    )
    
    var dataReceivedByB: CharacterExportDto? = null
    senderService.receiveCharacterData(
        onDataReceived = { data -> dataReceivedByB = data },
        onError = { }
    )
    
    // B sendet an A
    senderService.sendCharacterData("Device A", data1, { }, { })
    
    // A sendet an B
    receiverService.sendCharacterData("Device B", data2, { }, { })
}
```

## Implementierte Tests

Die aktuelle Test-Suite (`NearbyConnectionsTest`) enthält folgende Tests:

1. **`test advertising and discovery`**: Prüft, dass Geräte gefunden werden
2. **`test successful connection between two devices`**: Verbindungsaufbau
3. **`test character data transfer from sender to receiver`**: Einseitige Datenübertragung
4. **`test bidirectional data transfer`**: Bidirektionale Kommunikation
5. **`test connection failure when device not found`**: Fehlerbehandlung
6. **`test send failure when not connected`**: Sendefehler ohne Verbindung
7. **`test disconnect functionality`**: Verbindungstrennung
8. **`test simulated disconnect`**: Simulierte Verbindungsabbrüche
9. **`test version compatibility check on send`**: Versionsprüfung
10. **`test multiple devices in registry`**: Mehrere Geräte gleichzeitig

## Neue Tests hinzufügen

Um weitere Tests hinzuzufügen, folgen Sie diesem Muster:

```kotlin
@Test
fun `test my new feature`() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    
    // 1. Setup (Verbindung herstellen)
    launch(dispatcher) {
        receiverService.startAdvertising("Device A")
    }
    advanceUntilIdle()
    
    launch(dispatcher) {
        senderService.connectToEndpoint("Device A", "Device B")
    }
    advanceUntilIdle()
    
    // 2. Test-Aktion durchführen
    // ... Ihr Test-Code hier ...
    
    // 3. Assertions
    // ... Ihre Assertions hier ...
}
```

## Vorteile dieser Test-Infrastruktur

1. **Schnell**: Keine echte Netzwerk-Kommunikation, Tests laufen in Millisekunden
2. **Deterministisch**: Keine Timing-Probleme oder Race Conditions
3. **Isoliert**: Tests beeinflussen sich nicht gegenseitig
4. **Einfach erweiterbar**: Neue Test-Szenarien sind leicht hinzuzufügen
5. **Vollständige Kontrolle**: Fehler und Verbindungsabbrüche können gezielt simuliert werden

## Hinweise

- **Coroutine-Tests**: Verwenden Sie immer `runTest` und `advanceUntilIdle()` für asynchrone Operationen
- **Registry bereinigen**: Vergessen Sie nicht, die Registry in `@After` zu bereinigen
- **Test-Isolation**: Jeder Test sollte seine eigenen Service-Instanzen erstellen
- **Verbindungen trennen**: Rufen Sie `stopAllConnections()` in `tearDown()` auf

## Integration in Produktionscode

Das `NearbySyncViewModel` wurde so angepasst, dass es das Interface verwendet:

```kotlin
class NearbySyncViewModel(
    private val repository: ApplicatusRepository,
    private val nearbyService: NearbyConnectionsInterface
) : ViewModel() {
    // ...
}
```

In der Produktion wird die echte Implementierung injiziert:

```kotlin
class NearbySyncViewModelFactory(
    private val repository: ApplicatusRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NearbySyncViewModel(
            repository,
            NearbyConnectionsService(context)  // Echte Implementierung
        ) as T
    }
}
```

In Tests kann die Fake-Implementierung verwendet werden:

```kotlin
val viewModel = NearbySyncViewModel(
    mockRepository,
    FakeNearbyConnectionsService()  // Test-Implementierung
)
```

## Weitere Informationen

Siehe auch:
- **[README.md](README.md)** - Projekt-Übersicht
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Gesamtübersicht der Implementierung
- **[CHARACTER_SYNC_DOCUMENTATION.md](CHARACTER_SYNC_DOCUMENTATION.md)** - Echtzeit-Synchronisation
- **[EXPORT_IMPORT_GUIDE.md](EXPORT_IMPORT_GUIDE.md)** - Export/Import und Nearby Sync
- **[UI_TESTS_DOCUMENTATION.md](UI_TESTS_DOCUMENTATION.md)** - UI-Test-Suite
