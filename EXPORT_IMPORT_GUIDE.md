# Export & Import Anleitung

## JSON-Export

1. Öffnen Sie einen Charakter in der Detailansicht
2. Tippen Sie auf das Drei-Punkte-Menü (⋮) oben rechts
3. Wählen Sie "Als JSON exportieren"
4. Wählen Sie einen Speicherort und Dateinamen
5. Die Datei wird mit allen Charakterdaten, Slots und Zaubern gespeichert

## JSON-Import

1. Öffnen Sie einen beliebigen Charakter oder die Charakterliste
2. Tippen Sie auf das Drei-Punkte-Menü (⋮) oben rechts
3. Wählen Sie "JSON importieren"
4. Wählen Sie die zu importierende JSON-Datei
5. Der Charakter wird importiert:
   - Wenn der Name bereits existiert, wird eine Warnung angezeigt
   - Bei Versionsunterschieden werden Sie informiert
   - Zauber werden nach Namen automatisch zugeordnet

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

### JSON-Import schlägt fehl
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
