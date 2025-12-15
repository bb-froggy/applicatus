# UI Test Suite - Übersicht

Ich habe eine umfassende UI-Testsuite für die Applicatus-App hinzugefügt. Die Tests verwenden Jetpack Compose Testing mit AndroidJUnit4 und testen die wichtigsten Bildschirme der Anwendung.

## ✅ Status: Alle Tests kompilieren erfolgreich!

## Hinzugefügte Testdateien

### 1. CharacterHomeScreenTest.kt
**Speicherort:** `app/src/androidTest/java/de/applicatus/app/ui/screen/CharacterHomeScreenTest.kt`

**Tests (12 Testfälle):**
- ✅ Anzeige des Charakternamens
- ✅ Anzeige der Charaktereigenschaften (MU, KL, etc.)
- ✅ Anzeige der aktuellen Energien (LeP, AsP, KaP)
- ✅ LeP erhöhen funktioniert
- ✅ AsP verringern funktioniert
- ✅ Maximale LeP können nicht überschritten werden
- ✅ Edit-Button öffnet Dialog
- ✅ Zauberspeicher-Button navigiert korrekt
- ✅ Hexenküche-Button navigiert korrekt
- ✅ Zurück-Button navigiert korrekt
- ✅ Regenerations-Dialog öffnet sich
- ✅ Regeneration mit Modifikator

**Testet:** Character Home Screen mit Energieverwaltung und Navigation

---

### 2. SpellStorageScreenTest.kt
**Speicherort:** `app/src/androidTest/java/de/applicatus/app/ui/screen/spell/SpellStorageScreenTest.kt`

**Tests (11 Testfälle):**
- ✅ Anzeige des Charakternamens
- ✅ Anzeige gespeicherter Zauber
- ✅ Anzeige von Zauberdetails (ZfW, Slot-Typ)
- ✅ Bearbeitungsmodus umschalten
- ✅ Zauber hinzufügen-Button öffnet Dialog
- ✅ Zurück-Button Navigation
- ✅ Zauber-Card erweitern zeigt Details
- ✅ Empty State bei keinen Zaubern
- ✅ Mehr-Menü öffnet Dropdown
- ✅ Filter nach Slot-Typ
- ✅ Export/Import Funktionalität

**Testet:** Spell Storage Screen mit Zauberverwaltung

---

### 3. PotionScreenTest.kt
**Speicherort:** `app/src/androidTest/java/de/applicatus/app/ui/screen/potion/PotionScreenTest.kt`

**Tests (13 Testfälle):**
- ✅ Titel-Anzeige "Hexenküche"
- ✅ Anzeige von Tränken
- ✅ Anzeige der Trank-Qualität
- ✅ Trank hinzufügen-Button öffnet Dialog
- ✅ Zurück-Button Navigation
- ✅ Löschen-Button zeigt Bestätigungsdialog
- ✅ Analyse-Button öffnet Dialog
- ✅ Empty State bei keinen Tränken
- ✅ Rezepte-Button navigiert
- ✅ Ablaufdatum wird angezeigt
- ✅ Analyse-Status wird angezeigt
- ✅ "Keine Rezepte"-Meldung bei fehlenden Rezepten
- ✅ Mehrere Qualitätsstufen werden korrekt angezeigt

**Testet:** Potion Screen mit Trankverwaltung und Analyse

---

### 4. RecipeKnowledgeScreenTest.kt
**Speicherort:** `app/src/androidTest/java/de/applicatus/app/ui/screen/potion/RecipeKnowledgeScreenTest.kt`

**Tests (12 Testfälle):**
- ✅ Titel-Anzeige "Rezepte-Wissen"
- ✅ Anzeige aller Rezepte
- ✅ Anzeige der Wissens-Level
- ✅ Filter nach bekannten Rezepten
- ✅ Filter nach verstandenen Rezepten
- ✅ Filter nach unbekannten Rezepten
- ✅ Filter zurücksetzen zeigt alle Rezepte
- ✅ Zurück-Button Navigation
- ✅ Anzeige von Rezeptdetails
- ✅ Wissens-Level ändern
- ✅ Anzeige der Brauschwierigkeit
- ✅ Empty State bei gefilterten Ergebnissen

**Testet:** Recipe Knowledge Screen mit Filterfunktionen

---

## Test-Struktur

Alle Tests folgen einem konsistenten Muster:

### Setup
- Erstellt eine In-Memory-Datenbank für isolierte Tests
- Initialisiert Repository und Testdaten
- Verwendet `Room.inMemoryDatabaseBuilder()` für schnelle Tests

### Test-Ausführung
- Verwendet `createAndroidComposeRule<ComponentActivity>()`
- Setzt den Composable Screen mit Test-ViewModels
- Führt UI-Interaktionen durch (Klicks, Scrolls, etc.)
- Verifiziert erwartete UI-Zustände

### Teardown
- Schließt die Datenbank nach jedem Test
- Stellt saubere Testumgebung sicher

## Test-Technologien

- **Jetpack Compose Testing:** `androidx.compose.ui.test`
- **AndroidJUnit4:** Test Runner für Android
- **Room In-Memory Database:** Schnelle, isolierte Datenbank-Tests
- **Kotlin Coroutines:** `runBlocking` für synchrone Test-Ausführung

## Tests ausführen

### Alle UI-Tests ausführen:
```bash
./gradlew connectedAndroidTest
```

### Spezifische Test-Klasse ausführen:
```bash
./gradlew connectedAndroidTest --tests CharacterHomeScreenTest
```

### Einzelnen Test ausführen:
```bash
./gradlew connectedAndroidTest --tests CharacterHomeScreenTest.characterHomeScreen_displaysCharacterName
```

## Test-Coverage

Die Test-Suite deckt folgende Bereiche ab:

1. **UI-Komponenten:**
   - Anzeige von Daten
   - Buttons und Navigation
   - Dialoge und Modals
   - Listen und Cards

2. **Benutzer-Interaktionen:**
   - Klicks auf Buttons und Items
   - Navigation zwischen Screens
   - Formular-Eingaben
   - Filter und Sortierung

3. **Daten-Integration:**
   - Laden von Daten aus der Datenbank
   - Anzeige von Character-, Spell- und Potion-Daten
   - State Management mit ViewModels

4. **Edge Cases:**
   - Empty States (keine Daten)
   - Maximalwerte (z.B. LeP Maximum)
   - Filter ohne Ergebnisse

## Beste Praktiken

Die Tests folgen Android Testing Best Practices:

- ✅ Isolierte Tests mit In-Memory-Datenbank
- ✅ Klare Test-Namen die das erwartete Verhalten beschreiben
- ✅ Arrange-Act-Assert Pattern
- ✅ Verwendung von `waitForIdle()` für Compose Recomposition
- ✅ Semantische Selektoren (Text, ContentDescription)
- ✅ Überprüfung von Navigation-Callbacks

## Nächste Schritte

Mögliche Erweiterungen der Test-Suite:

1. Integration Tests mit verschiedenen Datenbank-Zuständen
2. Screenshot-Tests mit Paparazzi oder Shot
3. Performance-Tests für Listen mit vielen Items
4. Accessibility-Tests (TalkBack, Kontrast)
5. End-to-End Tests über mehrere Screens hinweg

## Weitere Informationen

Siehe auch:
- **[README.md](README.md)** - Projekt-Übersicht
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Gesamtübersicht der Implementierung
- **[NEARBY_TEST_INFRASTRUCTURE.md](NEARBY_TEST_INFRASTRUCTURE.md)** - Test-Infrastruktur für Nearby Connections
- **[app/src/androidTest/java/de/applicatus/app/data/DATABASE_MIGRATION_TEST.md](app/src/androidTest/java/de/applicatus/app/data/DATABASE_MIGRATION_TEST.md)** - Datenbank-Migrationstest
