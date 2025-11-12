# Datenbank-Migrationstest

## Übersicht

Der `DatabaseMigrationTest` stellt sicher, dass alle Datenbank-Migrationen von Version 1 bis zur aktuellen Version (20) korrekt funktionieren. Dies ist wichtig, um zu garantieren, dass Benutzer nach einem App-Update ihre vorhandenen Daten nicht verlieren.

## Was wird getestet?

Der Test:

1. **Erstellt eine Datenbank mit Schema Version 1** mit Testdaten:
   - 2 Zauber (ABVENENUM, ADLERSCHWINGE)
   - 2 Charaktere (Arion der Weise, Bela die Heilerin)
   - 2 Zauberslots für Arion

2. **Migriert schrittweise durch alle Versionen** (1 → 2 → 3 → ... → 20):
   - Jede Migration wird einzeln ausgeführt
   - Nach jeder Migration werden grundlegende Daten geprüft
   - Es wird sichergestellt, dass keine Exception geworfen wird

3. **Validiert die finalen Daten** nach allen Migrationen:
   - Alle Zauber sind noch vorhanden
   - Alle Charaktere existieren mit korrekten Eigenschaften
   - Neue Felder haben sinnvolle Standardwerte
   - Slots sind korrekt migriert
   - Gruppen wurden automatisch erstellt
   - Standard-Locations wurden hinzugefügt

## Migrations-Übersicht

| Migration | Änderungen |
|-----------|-----------|
| 1 → 2 | Applicatus-Support (hasApplicatus, slotType, volumePoints) |
| 2 → 3 | GUID-Spalte für Charaktere |
| 3 → 4 | Energien (LE/AE/KE), Recipe + Potion Tabellen |
| 4 → 5 | Regenerations-Boni |
| 5 → 6 | Recipe-Erweiterung, Alchemie-Talente, GlobalSettings |
| 6 → 7 | ODEM/ANALYS Zauber, RecipeKnowledge-Tabelle |
| 7 → 8 | Boolean-Felder für Talente/Zauber |
| 8 → 9 | Recipe-Erweiterung mit Preisen |
| 9 → 10 | Potion-Tabelle neu, zusätzliche Alchemie-Talente |
| 10 → 11 | Spielleiter-Modus, Patzer-Flag |
| 11 → 12 | Potion-Tabelle Restrukturierung |
| 12 → 13 | Gruppen-Feld, Potion GUID |
| 13 → 14 | Magisches Meisterhandwerk |
| 14 → 15 | Default-Labor |
| 15 → 16 | Potion nameKnown |
| 16 → 17 | Groups-Tabelle mit automatischer Migration |
| 17 → 18 | Inventar (Locations + Items) |
| 18 → 19 | Locations isCarried-Feld |
| 19 → 20 | Items Geldbeutel-Felder |

## Test ausführen

### Voraussetzungen
- Android-Gerät oder Emulator mit API 26+
- Alle Gradle-Dependencies installiert

### Über Android Studio
1. Öffne `DatabaseMigrationTest.kt`
2. Klicke auf den grünen Pfeil neben `migrateAll()`
3. Wähle ein Gerät/Emulator aus
4. Der Test wird ausgeführt

### Über Kommandozeile
```bash
# Alle instrumentierten Tests ausführen
.\gradlew.bat connectedAndroidTest

# Nur den Migrationstest ausführen
.\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=de.applicatus.app.data.DatabaseMigrationTest
```

## Test-Dauer

Der Test dauert ca. 5-10 Sekunden, da er:
- 20 Migrationsschritte durchführt
- Nach jedem Schritt Validierungen durchführt
- Die finale Datenbank mit allen DAOs prüft

## Fehlerbehebung

### "Migration didn't properly handle..."
**Problem**: Eine Migration hat Daten nicht korrekt übertragen.

**Lösung**: 
1. Prüfe die entsprechende Migration in `ApplicatusDatabase.kt`
2. Stelle sicher, dass alle Spalten korrekt kopiert werden
3. Bei Tabellen-Neustrukturierung: Prüfe die SELECT/INSERT Statements

### "Table ... doesn't have a column named ..."
**Problem**: Eine neue Spalte wurde nicht in der richtigen Version hinzugefügt.

**Lösung**:
1. Prüfe, in welcher Migration die Spalte hinzugefügt wurde
2. Stelle sicher, dass die Migration ausgeführt wird
3. Prüfe die SQL-Syntax (z.B. `'group'` muss in Anführungszeichen, da es ein SQL-Keyword ist)

### Test schlägt bei finaler Validierung fehl
**Problem**: Daten sind nach allen Migrationen nicht wie erwartet.

**Lösung**:
1. Prüfe die Assertions im Test
2. Führe den Test mit Debugger aus
3. Prüfe, ob alle Migrationen korrekt ausgeführt wurden

## Best Practices für neue Migrationen

Wenn du eine neue Migration hinzufügst:

1. **Füge die Migration in `ApplicatusDatabase.kt` hinzu**
2. **Mache sie public** (entferne `private val`), damit der Test darauf zugreifen kann
3. **Füge sie zur `addMigrations()`-Liste hinzu**
4. **Erweitere den Test**:
   ```kotlin
   // Migration X → Y
   helper.runMigrationsAndValidate(TEST_DB, Y, true,
       getDatabaseMigration(X, Y)
   ).apply {
       // Validierung hier
       close()
   }
   ```
5. **Füge die Migration zur `getDatabaseMigration()` Hilfsfunktion hinzu**
6. **Teste die Migration gründlich** - dieser Test garantiert, dass alte Daten nicht verloren gehen!

## Wichtige Hinweise

- **Migrationen sind kritisch**: Sie müssen immer rückwärtskompatibel sein
- **Keine Datenverluste**: Alle Migrationen müssen bestehende Daten erhalten
- **Standardwerte**: Neue Spalten brauchen sinnvolle Standardwerte
- **Testen ist Pflicht**: Dieser Test muss immer grün sein, bevor ein Update veröffentlicht wird
- **Versionsnummer erhöhen**: Bei jeder Schema-Änderung die Versionsnummer in `@Database(version = X)` erhöhen

## Weitere Informationen

Siehe auch:
- [IMPLEMENTATION.md](../../../../../../../IMPLEMENTATION.md) - Vollständige Implementierungsübersicht
- [ApplicatusDatabase.kt](../../main/java/de/applicatus/app/data/ApplicatusDatabase.kt) - Alle Migrationen
- [Room Migration Testing](https://developer.android.com/training/data-storage/room/migrating-db-versions#test) - Offizielle Android-Dokumentation
