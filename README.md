# Applicatus - DSA Zauberspeicher App

Eine Android-App zur Verwaltung von Zauberspeichern und Alchimie für das Pen-and-Paper-Rollenspiel Das Schwarze Auge (DSA) 4.1.

## Features

### Charakterverwaltung
- **Charaktererstellung**: Erstellen und verwalten Sie mehrere Charaktere mit ihren Eigenschaftswerten (MU, KL, IN, CH, FF, GE, KO, KK)
- **Energien**: Verwaltung von Lebensenergie (LE), Astralenergie (AE) und Karmaenergie (KE)
- **Talente**: Unterstützung für Alchimie-relevante Talente (Alchimie, Kochen, Selbstbeherrschung, Sinnenschärfe, Magiekunde, Pflanzenkunde)
- **Spielleiter-Modus**: Optionaler Modus, der alle versteckten Informationen anzeigt (Trank-Rezepte, Analyseergebnisse, etc.)

### Zauberspeicher
- **Zwei Slot-Typen**: 
  - Applicatus-Slots für den gleichnamigen Zauber
  - Zauberspeicher-Slots mit konfigurierbaren Volumenpunkten (max. 100 gesamt)
- **Zauberprobe**: Automatische Würfelprobe beim Einspeichern von Zaubern mit W20-Würfeln
- **Besondere Würfelergebnisse**: Erkennung von Doppel-1, Dreifach-1, Doppel-20 und Dreifach-20
- **Zauberliste**: Über 235 vordefinierte Zauber aus DSA (Standard + Hexenzauber)
- **Bearbeitungs- und Nutzungsmodus**: Getrennte Modi für Vorbereitung und Spielen

### Hexenküche (Alchimie)
- **Trankverwaltung**: Verwalten Sie selbst gebraute Tränke mit Qualitätsstufen
- **Rezeptverwaltung**: Über 30 vordefinierte Trank-Rezepte
- **Trank-Analyse**: 
  - ODEM ARCANUM zur Intensitätsbestimmung
  - ANALYS ARKANSTRUKTUR + Alchimie-Probe zur Strukturanalyse
  - Verschiedene Analysemethoden (Augenschein, Labor, Strukturanalyse-Serie)
- **Rezeptwissen**: Tracken Sie, welche Rezepte Ihr Charakter kennt
- **Spielleiter-Integration**: Im Spielleiter-Modus werden alle Informationen angezeigt

### Export/Import & Synchronisation
- **JSON-Export/Import**: 
  - Charaktere als JSON-Datei exportieren/importieren
  - Enthält alle Charakterdaten, Slots, Tränke (inklusive Analyse-Status) und bekannte Rezepte
  - Versionskontrolle mit Warnungen bei Versionsunterschieden
- **Nearby Connections**: 
  - Direkte Geräte-zu-Gerät-Übertragung via Bluetooth/WLAN
  - Keine Internetverbindung erforderlich
  - Perfekt für den Austausch am Spieltisch
- **Datenschutz**: Spielleiter-Modus wird beim Export NICHT übertragen (bleibt lokal)

### Persistenz & Datenbank
- **Room-Datenbank**: Alle Daten werden lokal gespeichert
- **Automatische Initialisierung**: Zauber und Rezepte werden beim ersten Start geladen
- **Zauber-Synchronisation**: Neue Zauber können über Menü nachgeladen werden

## Export & Synchronisation

### JSON-Export/Import
- Exportieren Sie Charaktere als JSON-Dateien
- Importieren Sie Charaktere auf anderen Geräten
- Automatische Versionskompatibilitätsprüfung
- Ideal für Backups und Gerätewechsel

### Nearby Connections
- Direkte Übertragung zwischen zwei Geräten via Bluetooth/WLAN
- Keine Internetverbindung erforderlich
- Sicher und schnell
- Perfekt für den Austausch am Spieltisch

## Technische Details

- **Sprache**: Kotlin
- **UI Framework**: Jetpack Compose
- **Datenbank**: Room (SQLite)
- **Architektur**: MVVM (Model-View-ViewModel)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Projektstruktur

```
app/
├── data/
│   ├── model/           # Datenmodelle
│   │   ├── character/   # Character mit Energien, Talenten, Zaubern
│   │   ├── spell/       # Spell, SystemSpell (ODEM, ANALYS)
│   │   ├── talent/      # Talent-Enum mit Eigenschaftsproben
│   │   └── potion/      # Potion, Recipe, PotionAnalysisStatus, RecipeKnowledge
│   ├── dao/             # Room DAOs
│   ├── repository/      # Repository-Pattern
│   ├── export/          # Export/Import-Logik (JSON, DTOs)
│   ├── nearby/          # Nearby Connections Service
│   └── InitialSpells.kt # Vordefinierte Zauber
├── logic/
│   ├── ProbeChecker.kt      # Zentrale Proben-Logik (Talente, Zauber, System-Zauber)
│   ├── SpellChecker.kt      # Zauberprobe mit Applicatus-Unterstützung
│   ├── ElixirAnalyzer.kt    # Alchimie-Analysen (ODEM, ANALYS, Alchimie)
│   ├── PotionAnalyzer.kt    # Trank-Analyse-Logik
│   └── RegenerationCalculator.kt # LE/AE/KE-Regeneration
├── ui/
│   ├── screen/          # Composable Screens
│   │   ├── spell/       # Zauberspeicher-Screens
│   │   ├── potion/      # Hexenküche-Screens (Tränke, Rezepte, Analyse)
│   │   ├── CharacterHomeScreen.kt
│   │   ├── CharacterListScreen.kt
│   │   └── NearbySyncScreen.kt
│   ├── viewmodel/       # ViewModels
│   └── navigation/      # Navigation
└── MainActivity.kt
```

## Build-Anleitung

1. Öffnen Sie das Projekt in Android Studio
2. Warten Sie, bis Gradle synchronisiert ist
3. Führen Sie die App auf einem Emulator oder Gerät aus

## Spielmechanik

### Zauberprobe

Beim Einspeichern eines Zaubers wird eine Probe durchgeführt:

1. **Basis-ZfP***: ZfW - Modifikator
2. **Drei Würfelwürfe**: Jeweils ein W20 auf die drei Eigenschaften des Zaubers
3. **Überwürfe**: Bei Überwürfen wird die Differenz von den ZfP* abgezogen
4. **Erfolg**: ZfP* ≥ 0 → Zauber erfolgreich, ZfP* werden auf ZfW gedeckelt
5. **Misserfolg**: ZfP* < 0 → Zauber fehlgeschlagen

### Besondere Fälle

- **Doppel-1**: Automatischer Erfolg mit maximalen ZfP*
- **Dreifach-1**: Automatischer Erfolg mit maximalen ZfP*
- **Doppel-20**: Automatischer Patzer
- **Dreifach-20**: Katastrophaler Patzer

### Trank-Analyse

**1. Intensitätsbestimmung (ODEM ARCANUM)**:
- Zauberprobe auf KL/IN/IN mit ODEM-ZfW
- Bestimmt die magische Intensität des Tranks

**2. Strukturanalyse (ANALYS + Alchimie)**:
- ANALYS ARKANSTRUKTUR: Zauberprobe auf KL/KL/IN
- Alchimie-Probe: Talentprobe auf MU/KL/FF
- Bei 19+ TaP* gesamt: Rezept vollständig verstanden

**3. Alternative Analysemethoden**:
- **Augenschein**: Sinnenschärfe-Probe (KL/IN/IN)
- **Labor**: Magiekunde oder Pflanzenkunde (KL/KL/IN bzw. KL/FF/KK)
- **Strukturanalyse-Serie**: Mehrere ANALYS-Proben mit Selbstbeherrschung

### Spielleiter-Modus

Charaktere können als "Spielleiter" markiert werden, wodurch:
- Alle Trank-Rezepte sichtbar sind (nicht nur die untersuchten)
- Vollständige Analyseergebnisse angezeigt werden
- ZfP*-Werte bei Zauberproben immer sichtbar sind
- Patzer-Hinweise detailliert angezeigt werden

**Wichtig**: Der Spielleiter-Modus wird beim Export NICHT übertragen und bleibt immer lokal!

## Lizenz

Dieses Projekt ist ein privates Hilfstool für DSA-Spieler. DSA ist ein eingetragenes Warenzeichen von Ulisses Spiele.
