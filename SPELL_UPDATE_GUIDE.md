# Zauber-Datenbank aktualisieren

## Warum ist das notwendig?

Wenn neue Versionen der Applicatus-App erscheinen und neue Zauber zu `InitialSpells.kt` hinzugefügt wurden, haben Nutzer, die bereits eine ältere Version der App installiert haben, diese neuen Zauber nicht automatisch in ihrer lokalen Datenbank.

## Wie aktualisiere ich die Zauber-Datenbank?

### Schritt-für-Schritt-Anleitung

1. **Öffne die Charakterliste**
   - Die Charakterliste ist der Hauptbildschirm der App

2. **Öffne das Menü**
   - Tippe auf das Drei-Punkte-Menü (⋮) oben rechts

3. **Wähle "Zauber-Datenbank aktualisieren"**
   - Dies ist der erste Menüpunkt mit einem Aktualisieren-Symbol (🔄)

4. **Warte auf die Synchronisation**
   - Ein Dialog mit einem Ladebalken erscheint
   - Die App vergleicht die Zauber in der Datenbank mit den Initial-Zaubern
   - Fehlende Zauber werden automatisch hinzugefügt

5. **Ergebnis**
   - Bei Erfolg erscheint ein Dialog mit der Anzahl der hinzugefügten Zauber
   - Wenn keine neuen Zauber gefunden wurden, wird dies ebenfalls angezeigt
   - Die neuen Zauber sind nun verfügbar und können in Zauberslots ausgewählt werden

## Technische Details

### Was passiert bei der Synchronisation?

Die Funktion vergleicht die Namen aller Zauber in der Datenbank mit den Zaubern aus `InitialSpells.kt`:

```kotlin
suspend fun syncMissingSpells(): Int {
    val existingSpellNames = spellDao.getAllSpellNames().toSet()
    val initialSpells = InitialSpells.getDefaultSpells()
    
    val missingSpells = initialSpells.filter { spell ->
        spell.name !in existingSpellNames
    }
    
    if (missingSpells.isNotEmpty()) {
        insertSpells(missingSpells)
    }
    
    return missingSpells.size
}
```

### Wichtige Hinweise

- **Keine Duplikate**: Zauber, die bereits in der Datenbank vorhanden sind, werden nicht doppelt hinzugefügt
- **Sichere Operation**: Die Synchronisation überschreibt keine bestehenden Zauber
- **Schnell**: Die Operation dauert nur wenige Sekunden
- **Keine Daten gehen verloren**: Charaktere und ihre Zauberslots bleiben unverändert

### Wann sollte ich die Datenbank aktualisieren?

- Nach jedem App-Update
- Wenn neue Zauber in der Zauberauswahl fehlen
- Nach einem Import oder einer Wiederherstellung der App-Daten

## Fehlerbehebung

### "Alle Zauber sind bereits aktuell"

Dies bedeutet, dass Ihre Datenbank bereits alle verfügbaren Initial-Zauber enthält. Es ist alles in Ordnung!

### "Synchronisation fehlgeschlagen"

Dies kann passieren, wenn:
- Die Datenbank beschädigt ist
- Nicht genügend Speicherplatz vorhanden ist
- Die App keine Schreibrechte auf die Datenbank hat

**Lösung**: Versuchen Sie, die App neu zu starten. Wenn das Problem weiterhin besteht, kontaktieren Sie den Support.

## Für Entwickler

### Implementierte Komponenten

1. **SpellDao.kt**
   - Neue Methode: `getAllSpellNames(): List<String>`
   - Ermöglicht effizienten Vergleich der vorhandenen Zauber

2. **ApplicatusRepository.kt**
   - Neue Methode: `syncMissingSpells(): Int`
   - Enthält die Logik zum Erkennen und Hinzufügen fehlender Zauber

3. **CharacterListViewModel.kt**
   - Neuer State: `SpellSyncState` (Idle, Syncing, Success, Error)
   - Neue Methode: `syncMissingSpells()`
   - Neue Methode: `resetSpellSyncState()`

4. **CharacterListScreen.kt**
   - Neuer Menüpunkt mit Refresh-Icon
   - Dialoge für Status-Feedback
   - Integration in die Charakterlisten-Ansicht

### Erweiterungsmöglichkeiten

- **Automatische Synchronisation**: Beim App-Start automatisch prüfen und synchronisieren
- **Version Tracking**: Zauber mit Versionsnummern versehen und nur neuere Versionen synchronisieren
- **Changelog**: Anzeige, welche Zauber konkret hinzugefügt wurden
- **Benachrichtigungen**: Push-Benachrichtigung, wenn neue Zauber verfügbar sind
