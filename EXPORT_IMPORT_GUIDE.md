# Export & Import Anleitung

## Vollständiges Datenbank-Backup

### Was ist ein vollständiges Backup?

Ein vollständiges Backup exportiert **alle Daten** der App in einer einzigen JSON-Datei:
- Alle Zauber und Rezepte (Libraries)
- Alle Spielgruppen mit ihren Daten
- Alle Charaktere mit sämtlichen Daten (Zauber, Tränke, Inventar, etc.)
- Globale Einstellungen

**Verwendungszwecke:**
- 🔄 **Gerätewechsel**: Übertragen Sie alle Daten auf ein neues Gerät
- 💾 **Backup**: Sichern Sie alle Kampagnendaten gegen Datenverlust
- 👥 **Kampagnen-Austausch**: Teilen Sie eine komplette Kampagne mit anderen Spielern
- 🔧 **Migration**: Aktualisieren Sie auf eine neue App-Version mit Datensicherung

### Backup exportieren

1. Öffnen Sie die Charakterliste
2. Tippen Sie auf das Drei-Punkte-Menü (⋮) oben rechts
3. Wählen Sie "Vollständiges Backup exportieren"
4. Wählen Sie einen Speicherort und Dateinamen (Vorschlag: `applicatus_backup_YYYY-MM-DD.json`)
5. Warten Sie, bis der Export abgeschlossen ist (mit Fortschrittsanzeige)

**Die Datei enthält:**
- Alle ${validation.spellCount}+ Zauber aus der Datenbank
- Alle ${validation.recipeCount}+ Rezepte
- Alle Spielgruppen mit derischem Datum
- Alle Charaktere mit:
  - Eigenschaften, Talenten und Fähigkeiten
  - Zauberslots (gefüllt und leer) mit ZfP*-Werten
  - Tränke mit vollständigen Analyse-Daten
  - Rezeptwissen
  - Komplettes Inventar (Locations und Items)
  - Zeitstempel der letzten Änderung

### Backup importieren

1. Öffnen Sie die Charakterliste
2. Tippen Sie auf das Drei-Punkte-Menü (⋮) oben rechts
3. Wählen Sie "Vollständiges Backup importieren"
4. Wählen Sie die Backup-Datei
5. **Bestätigungs-Dialog lesen**: Die App zeigt eine Übersicht der zu importierenden Daten und mögliche Warnungen
6. Tippen Sie auf "Importieren" zum Fortfahren oder "Abbrechen" zum Verwerfen
7. Warten Sie, bis der Import abgeschlossen ist (mit Fortschrittsanzeige)

### Merge-Strategie (Wie werden Daten zusammengeführt?)

Die App führt vorhandene und importierte Daten intelligent zusammen:

**Zauber & Rezepte:**
- Werden anhand des **Namens** abgeglichen
- Bei Duplikaten: Vorhandene bleiben erhalten (kein Überschreiben)
- Neue Zauber/Rezepte werden hinzugefügt

**Spielgruppen:**
- Werden anhand des **Namens** abgeglichen
- Bei Duplikaten: Vorhandene Gruppe wird verwendet
- Neue Gruppen werden erstellt

**Charaktere:**
- Werden anhand der **GUID** (eindeutige ID) abgeglichen
- Bei Duplikaten: Charakter wird **aktualisiert** mit importierten Daten
- Neue Charaktere werden hinzugefügt
- Zauberslots und Inventar werden **komplett ersetzt** (keine Duplikate)

**Tränke:**
- Werden anhand der **GUID** abgeglichen
- Bei Duplikaten: Intelligentes Merge (bessere Analyse-Ergebnisse bleiben erhalten)

**Ergebnis:** Nach dem Import haben Sie alle Daten aus beiden Quellen, ohne Duplikate!

### Wichtige Hinweise zum Backup-Import

**Warnungen beachten:**
Die App warnt Sie vor möglichen Konflikten:
- ⚠️ "Die Datenbank enthält bereits Daten" → Merge-Strategie wird angewendet
- ⚠️ "X Gruppe(n) existieren bereits" → Vorhandene Gruppen werden nicht überschrieben
- ⚠️ "X Charakter(e) existieren bereits" → Diese werden mit importierten Daten aktualisiert
- ⚠️ "Backup ist von einer älteren App-Version" → Import möglich, aber einige Features könnten fehlen

**Versionskompatibilität:**
- Backup-Version wird automatisch geprüft
- Ältere Backups: Import mit Warnung möglich
- Neuere Backups: Import blockiert → Bitte App aktualisieren!

**Datensicherheit:**
- Backup-Dateien sind **nicht verschlüsselt** → Sicher aufbewahren!
- Keine Daten werden ins Internet übertragen
- Alle Daten bleiben auf Ihren Geräten

### Unterschied: Charakter-Export vs. Vollständiges Backup

| Feature | Charakter-Export | Vollständiges Backup |
|---------|------------------|----------------------|
| **Umfang** | Ein einzelner Charakter | Alle Charaktere + Gruppen + Libraries |
| **Verwendung** | Charakter teilen/übertragen | Gerätewechsel, Backup, Kampagnen-Migration |
| **Datei-Größe** | Klein (~50-200 KB) | Größer (~500 KB - 5 MB je nach Daten) |
| **Import-Verhalten** | Einzelner Charakter wird hinzugefügt/aktualisiert | Alle Daten werden gemerged |
| **Gruppen** | Nur Gruppenname | Alle Gruppen mit vollständigen Daten |
| **Zauber/Rezepte** | Nur Referenzen | Komplette Libraries |

**Faustregel:**
- **Charakter-Export**: Zum Teilen eines einzelnen Charakters am Spieltisch
- **Vollständiges Backup**: Für Gerätewechsel oder komplette Datensicherung

---

## JSON-Export (Einzelner Charakter)

1. Öffnen Sie einen Charakter in der Detailansicht
2. Tippen Sie auf das Drei-Punkte-Menü (⋮) oben rechts
3. Wählen Sie "Als JSON exportieren"
4. Wählen Sie einen Speicherort und Dateinamen
5. Die Datei enthält sämtliche Charakterdaten inklusive:
   - Charaktereigenschaften und Talente
   - Zaubersprüche (Slots) und ZfP*-Werte
   - Tränke mit Analyse-Status und Rezeptwissen
   - **Inventar (Locations und Items)**
   - Zeitstempel der letzten Änderung

## JSON-Import

1. Öffnen Sie einen beliebigen Charakter oder die Charakterliste
2. Tippen Sie auf das Drei-Punkte-Menü (⋮) oben rechts
3. Wählen Sie "JSON importieren"
4. Wählen Sie die zu importierende JSON-Datei
5. **Bestätigung bei Warnungen**: Wenn Warnungen auftreten (z.B. neuere lokale Änderungen), können Sie:
   - **"Fortfahren"** wählen, um den Import trotzdem durchzuführen
   - **"Abbrechen"** wählen, um den Import zu verwerfen und lokale Änderungen zu bewahren
6. **Der Charakter wird automatisch aktualisiert**, wenn bereits ein Charakter mit derselben GUID existiert:
   - Alle Eigenschaften werden mit den importierten Werten aktualisiert
   - Zaubersprüche werden ersetzt
   - **Inventar wird komplett ersetzt** (alte Items werden gelöscht, damit keine Duplikate entstehen)
   - Tränke werden intelligent gemerged (bessere Analyse-Ergebnisse bleiben erhalten)
   - Bei Versionsunterschieden werden Sie informiert
   - **Warnung bei neueren lokalen Änderungen**: Wenn der lokale Charakter nach dem Export-Zeitpunkt geändert wurde, erhalten Sie eine Warnung
7. Wenn kein passender Charakter existiert, wird ein neuer angelegt:
   - Standard-Locations (Rüstung/Kleidung, Rucksack) werden erstellt
   - Zauber werden nach Namen automatisch zugeordnet

### Wichtige Hinweise zum Import

- **GUID-basiertes Matching**: Die App erkennt automatisch, ob ein Charakter bereits existiert (anhand der eindeutigen GUID)
- **Keine Duplikate**: Es wird kein zweiter Charakter mit gleichem Namen angelegt
- **Inventar-Ersetzung**: Beim Import eines bestehenden Charakters wird das alte Inventar gelöscht und durch das importierte ersetzt (außer Standard-Locations)
- **Zeitstempel-Prüfung**: Sie werden gewarnt, wenn lokale Änderungen neuer sind als der Export
- **Abbruch möglich**: Bei Warnungen können Sie den Import abbrechen, ohne Änderungen vorzunehmen
- **Rezeptwissen**: Vorhandenes Rezeptwissen wird vor dem Import bereinigt und neu importiert

## Nearby Sync (Geräte-zu-Gerät Übertragung)

### Vorbereitung
Beide Geräte müssen:
- Bluetooth aktiviert haben
- WLAN aktiviert haben
- Die App mit den erforderlichen Berechtigungen installiert haben
- Sich in räumlicher Nähe befinden (wenige Meter)

### Empfänger-Gerät (Gerät B)
1. Öffnen Sie den zu übertragenden Charakter
2. Tippen Sie auf ⋮ → "Nearby Sync"
3. Tippen Sie auf "Als Empfänger bereitstellen"
4. Warten Sie, bis eine Verbindung hergestellt wird
5. Der Charakter wird automatisch empfangen

### Sender-Gerät (Gerät A)
1. Öffnen Sie den zu sendenden Charakter
2. Tippen Sie auf ⋮ → "Nearby Sync"
3. Tippen Sie auf "Nach Geräten suchen"
4. Wählen Sie das Empfänger-Gerät aus der Liste
5. Tippen Sie auf "Charakter senden"
6. Warten Sie auf die Bestätigung

### Tipps
- Bei Problemen: Verbindung trennen und neu starten
- Stellen Sie sicher, dass keine anderen Bluetooth-Verbindungen aktiv sind
- Die Übertragung funktioniert auch ohne Internetverbindung
- Beide Geräte sollten möglichst die gleiche App-Version haben

## Versionskompatibilität

Die App prüft automatisch die Datenmodell-Version:

- **Gleiche Version**: Import/Sync funktioniert problemlos
- **Ältere Version**: Import möglich mit Warnung (einige Features könnten fehlen)
- **Neuere Version**: Import blockiert - bitte aktualisieren Sie die App

Beim Überschreiben eines existierenden Charakters mit einer älteren Version werden Sie gewarnt, da dabei Daten verloren gehen können.

## Fehlerbehebung

### Vollständiges Backup schlägt fehl
- Prüfen Sie, ob genügend Speicherplatz vorhanden ist
- Bei sehr großen Datenbanken kann der Export einige Sekunden dauern
- Versuchen Sie, die App neu zu starten und erneut zu exportieren

### Backup-Import schlägt fehl
- Prüfen Sie, ob die Datei eine gültige JSON-Datei ist
- Stellen Sie sicher, dass die Datei nicht beschädigt ist
- Überprüfen Sie die App-Version (neuere Backups benötigen neuere App-Version)
- Bei Fehlermeldungen: Notieren Sie die Fehlermeldung und erstellen Sie ein neues Backup

### JSON-Import schlägt fehl (einzelner Charakter)
- Prüfen Sie, ob die Datei eine gültige JSON-Datei ist
- Stellen Sie sicher, dass die Datei nicht beschädigt ist
- Versuchen Sie, die Datei erneut zu exportieren

### Nearby Sync findet keine Geräte
- Aktivieren Sie Bluetooth und WLAN auf beiden Geräten
- Erteilen Sie alle erforderlichen Berechtigungen
- Stellen Sie sicher, dass beide Geräte nah beieinander sind
- Starten Sie beide Apps neu

### Verbindung bricht ab
- Bluetooth/WLAN-Störungen sind die häufigste Ursache
- Halten Sie die Geräte näher zusammen
- Entfernen Sie andere Bluetooth-Geräte aus der Nähe
- Versuchen Sie es erneut

## Sicherheit

- JSON-Dateien sind nicht verschlüsselt - speichern Sie sie sicher
- Nearby Connections verwendet eine verschlüsselte Verbindung
- Keine Daten werden ins Internet übertragen
- Alle Daten bleiben auf Ihren Geräten

## Weitere Informationen

Siehe auch:
- **[README.md](README.md)** - Projekt-Übersicht
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Gesamtübersicht der Implementierung
- **[CHARACTER_SYNC_DOCUMENTATION.md](CHARACTER_SYNC_DOCUMENTATION.md)** - Echtzeit-Synchronisation
- **[NEARBY_TEST_INFRASTRUCTURE.md](NEARBY_TEST_INFRASTRUCTURE.md)** - Test-Infrastruktur für Nearby Connections
