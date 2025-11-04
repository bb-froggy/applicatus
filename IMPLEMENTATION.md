# Applicatus - Implementierungsübersicht

## ✅ Fertiggestellte Komponenten

### 1. Projektstruktur
- ✅ Gradle Build-Konfiguration (app/build.gradle.kts, build.gradle.kts, settings.gradle.kts)
- ✅ AndroidManifest.xml mit allen Permissions
- ✅ Strings, Themes, Colors
- ✅ ProGuard-Regeln
- ✅ .gitignore

### 2. Datenmodell (data/model/)
- ✅ **Spell**: Zauber mit Name und drei Eigenschaftsproben
- ✅ **Character**: Charakter mit 8 Eigenschaftswerten (MU, KL, IN, CH, FF, GE, KO, KK)
  - ✅ Applicatus-Support (hasApplicatus, applicatusZfw, applicatusModifier)
- ✅ **SlotType**: Enum für Slot-Typen (APPLICATUS, SPELL_STORAGE)
- ✅ **SpellSlot**: Zauberslot mit ZfW, Modifikator, Variante, Füllstatus, ZfP*
  - ✅ SlotType (Applicatus oder Zauberspeicher)
  - ✅ Volumenpunkte für Zauberspeicher (1-100, max. 100 gesamt)
  - ✅ Applicatus-Würfelergebnis
- ✅ **SpellSlotWithSpell**: View-Objekt für Join zwischen Slot und Zauber

### 3. Datenbank (data/)
- ✅ **Room DAOs**: SpellDao, CharacterDao, SpellSlotDao
- ✅ **TypeConverters**: SlotType-Converter
- ✅ **ApplicatusDatabase**: Room-Datenbank mit automatischer Initialisierung
  - ✅ Migration von Version 1 zu 2 (neue Felder)
- ✅ **ApplicatusRepository**: Repository-Pattern für Datenzugriff
- ✅ **InitialSpells**: 190+ vordefinierte Zauber aus magierzauber.txt

### 4. Geschäftslogik (logic/)
- ✅ **SpellChecker**: Komplette Zauberprobe-Implementierung
  - ✅ 3x W20-Würfelwürfe
  - ✅ Berechnung von ZfP* mit Modifikatoren
  - ✅ Erkennung von Doppel-1, Dreifach-1, Doppel-20, Dreifach-20
  - ✅ Deckelung auf ZfW
  - ✅ Formatierte Ergebnis-Strings
  - ✅ **Applicatus-Probe**: Doppelte Zauberprobe (Applicatus + eigentlicher Zauber)

### 5. ViewModels (ui/viewmodel/)
- ✅ **CharacterListViewModel**: Verwaltung der Charakterliste
  - ✅ Liste aller Charaktere
  - ✅ Hinzufügen von Charakteren mit Applicatus-Unterstützung
  - ✅ Löschen von Charakteren
  - ✅ Keine automatische Slot-Initialisierung mehr
  
- ✅ **CharacterDetailViewModel**: Verwaltung eines Charakters
  - ✅ **Bearbeitungsmodus**: Umschaltbar zwischen Nutzungs- und Bearbeitungsmodus
  - ✅ Anzeige aller Zauberslots
  - ✅ **Slot-Verwaltung**:
    - ✅ Slots hinzufügen (mit Typ-Auswahl)
    - ✅ Slots entfernen
    - ✅ Volumenpunkte-Prüfung (max. 100 gesamt)
  - ✅ Zauber auswählen, ZfW/Modifikator/Variante setzen
  - ✅ Globale Modifikator-Anpassung
  - ✅ Zauber einspeichern mit automatischer Probe
  - ✅ Slots leeren
  - ✅ **Charaktereigenschaften bearbeiten**
  - ✅ Applicatus-Verwaltung (ZfW, Modifikator)

### 6. UI-Screens (ui/screen/)
- ✅ **CharacterListScreen**: 
  - ✅ Liste aller Charaktere mit Eigenschaftswerten
  - ✅ FAB zum Hinzufügen
  - ✅ Dialog mit allen 8 Eigenschaftsfeldern + Applicatus-Feldern
  - ✅ Navigation zu Details
  
- ✅ **CharacterDetailScreen**:
  - ✅ **Mode-Toggle**: Button zum Umschalten zwischen Nutzungs- und Bearbeitungsmodus
  - ✅ Anzeige der Charaktereigenschaften (editierbar im Bearbeitungsmodus)
  - ✅ Applicatus-Info-Card (wenn vorhanden)
  - ✅ Volumenpunkte-Anzeige (verbleibend / 100)
  
  **Nutzungsmodus** (kompakte Ansicht):
  - ✅ Globale Modifikator-Controls
  - ✅ Slot-Liste mit Zauberinfo
  - ✅ "Sprechen" Button zum Zaubern
  - ✅ "Leeren" Button für gefüllte Slots
  - ✅ Anzeige von ZfP*, Würfelergebnissen
  - ✅ Applicatus-Würfelergebnis-Anzeige
  
  **Bearbeitungsmodus** (ausführliche Ansicht):
  - ✅ FAB zum Hinzufügen von Slots
  - ✅ Slot-Typ-Auswahl (Applicatus/Zauberspeicher)
  - ✅ Volumenpunkte-Eingabe für Zauberspeicher
  - ✅ Zauber-Auswahl-Dialog (durchsuchbar)
  - ✅ ZfW-Eingabe
  - ✅ Modifikator mit +/- Buttons
  - ✅ Variante/Notiz-Feld
  - ✅ Slot-Löschen-Button
  - ✅ Eigenschaften-Bearbeiten-Dialog
  - ✅ Applicatus-Bearbeitung

### 7. Navigation (ui/navigation/)
- ✅ **Screen**: Sealed Class für Routes
- ✅ **ApplicatusNavHost**: Jetpack Compose Navigation
  - CharacterList → CharacterDetail mit characterId-Parameter

### 8. App-Setup
- ✅ **ApplicatusApplication**: Application-Klasse mit Repository
- ✅ **MainActivity**: Activity mit Compose-Integration

## 🎯 Implementierte Features

### Charakterverwaltung
- ✅ Charaktere erstellen mit Name und 8 Eigenschaftswerten
- ✅ Applicatus-Unterstützung (optional)
  - ✅ Applicatus ZfW und Modifikator
  - ✅ Automatische Probe auf KL/IN/CH beim Zaubern
- ✅ Charaktere anzeigen und löschen
- ✅ Charaktereigenschaften bearbeiten
- ✅ Persistente Speicherung

### Zauberslot-System
- ✅ Variable Anzahl von Slots (nicht mehr fix 10)
- ✅ **Zwei Slot-Typen**:
  - ✅ **Applicatus-Slots**: Nutzen Applicatus-Zauber
  - ✅ **Zauberspeicher-Slots**: Direkte Speicherung
    - ✅ Volumenpunkte (1-100 pro Slot)
    - ✅ Max. 100 Volumenpunkte gesamt
- ✅ Slots hinzufügen und entfernen
- ✅ Zauberauswahl aus 190+ Zaubern
- ✅ Durchsuchbare Zauberliste
- ✅ ZfW (0-28), Modifikator (-8 bis +4), Variante-Notiz
- ✅ Individuelle +/- Buttons pro Slot (Bearbeitungsmodus)
- ✅ Globale +/- Buttons für alle Slots (Nutzungsmodus)

### Modi-System
- ✅ **Nutzungsmodus**:
  - ✅ Kompakte Slot-Darstellung
  - ✅ Zauber sprechen und Slots leeren
  - ✅ Globale Modifikator-Anpassung
  - ✅ Fokus auf Spielfluss
  
- ✅ **Bearbeitungsmodus**:
  - ✅ Slots hinzufügen/entfernen
  - ✅ Zauber auswählen
  - ✅ ZfW und Modifikatoren anpassen
  - ✅ Notizen bearbeiten
  - ✅ Charaktereigenschaften ändern
  - ✅ Volumenpunkte-Verwaltung

### Zauberprobe
- ✅ **Zauberspeicher**: Direkte W20-Würfelprobe
- ✅ **Applicatus**: Doppelte Probe
  1. ✅ Applicatus-Probe (KL/IN/CH)
  2. ✅ Bei Erfolg: eigentliche Zauberprobe
  3. ✅ Beide Ergebnisse werden angezeigt
- ✅ Berechnung: ZfP = ZfW - Modifikator, dann Abzüge bei Überwürfen
- ✅ Deckelung auf ZfW
- ✅ Erfolg/Misserfolg-Anzeige
- ✅ Doppel-1 / Dreifach-1 (automatischer Erfolg)
- ✅ Doppel-20 / Dreifach-20 (automatischer Patzer)
- ✅ Formatierte Würfelergebnisse mit Details

### Persistenz
- ✅ Room-Datenbank für alle Daten
- ✅ Migration von v1 zu v2 (neue Felder)
- ✅ Automatische Initialisierung mit Zaubern beim ersten Start
- ✅ Status der gefüllten Slots bleibt erhalten
- ✅ Alle Änderungen werden automatisch gespeichert

### UI/UX
- ✅ Material Design 3
- ✅ Jetpack Compose
- ✅ Responsive Layouts
- ✅ Intuitive Navigation
- ✅ Farbcodierung für Erfolg/Misserfolg
- ✅ Kompakte und ausführliche Ansichten
- ✅ Kontextabhängige Dialoge

## 📝 Nicht implementiert (optional für Zukunft)

- ⚪ JSON/CSV Import/Export
- ⚪ Zauber bearbeiten/hinzufügen in der App
- ⚪ Statistiken über Würfelerfolge
- ⚪ Backup/Restore-Funktionalität
- ⚪ Themes (Hell/Dunkel)

## 🚀 Build & Run

Die App ist komplett und funktionsfähig. Um sie zu bauen:

1. Öffnen Sie das Projekt in Android Studio
2. Warten Sie auf Gradle-Sync
3. Führen Sie die App aus (Run → Run 'app' oder Shift+F10)

Die App benötigt:
- Min SDK: Android 8.0 (API 26)
- Target SDK: Android 14 (API 34)

Bei der ersten Ausführung werden automatisch alle 190+ Zauber in die Datenbank geladen.

## 🆕 Neue Features (Version 2)

### Bearbeitungs- und Nutzungsmodus
Die Charakterseite hat jetzt zwei Modi, die über einen Button in der App-Bar umgeschaltet werden können:

**Nutzungsmodus**: Optimiert für das Spielen
- Kompakte Slot-Darstellung für bessere Übersicht
- Schneller Zugriff auf "Sprechen" und "Leeren"
- Globale Modifikator-Anpassung
- Keine versehentlichen Änderungen an Slots

**Bearbeitungsmodus**: Optimiert für die Vorbereitung
- Slots hinzufügen und entfernen
- Zauber auswählen und konfigurieren
- Charaktereigenschaften bearbeiten
- Vollständige Kontrolle über alle Einstellungen

### Slot-Typen
Zwei verschiedene Slot-Typen für unterschiedliche Spielstile:

**Applicatus-Slots**:
- Nutzen den Applicatus-Zauber zum Einspeichern
- Erfordern zwei erfolgreiche Proben (Applicatus + Zauber)
- Nur verfügbar, wenn der Charakter Applicatus kann
- Zeigen beide Würfelergebnisse an

**Zauberspeicher-Slots**:
- Direkte Speicherung ohne Applicatus
- Benötigen Volumenpunkte (1-100)
- Maximale Gesamtkapazität: 100 Volumenpunkte
- Flexiblere Aufteilung möglich

### Volumenpunkte-System
- Jeder Zauberspeicher-Slot benötigt 1-100 Volumenpunkte
- Gesamtlimit: 100 Volumenpunkte pro Charakter
- Anzeige der verbleibenden Punkte im Bearbeitungsmodus
- Verhindert Überschreitung des Limits

### Applicatus-Unterstützung
- Optional pro Charakter aktivierbar
- Zentrale ZfW- und Modifikator-Verwaltung
- Automatische doppelte Probe beim Zaubern
- Probe auf KL/IN/CH (Applicatus-Eigenschaften)
- Beide Ergebnisse werden gespeichert und angezeigt
