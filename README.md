# Applicatus - DSA Zauberspeicher App

Eine Android-App zur Verwaltung von Zauberspeichern und Alchimie für das Pen-and-Paper-Rollenspiel Das Schwarze Auge (DSA) 4.1.

## Features

- **Charakterverwaltung**: Erstellen und verwalten Sie mehrere Charaktere mit ihren Eigenschaftswerten (MU, KL, IN, CH, FF, GE, KO, KK)
- **Zwei Slot-Typen**: 
  - Applicatus-Slots für den gleichnamigen Zauber
  - Zauberspeicher-Slots mit konfigurierbaren Volumenpunkten (max. 100 gesamt)
- **Zauberprobe**: Automatische Würfelprobe beim Einspeichern von Zaubern mit W20-Würfeln
- **Besondere Würfelergebnisse**: Erkennung von Doppel-1, Dreifach-1, Doppel-20 und Dreifach-20
- **Zauberliste**: Über 190 vordefinierte Zauber aus DSA
- **Bearbeitungs- und Nutzungsmodus**: Getrennte Modi für Vorbereitung und Spielen
- **Export/Import**: 
  - JSON-Export und -Import von Charakteren
  - Nearby Connections für direkten Transfer zwischen Geräten
  - Versionskontrolle mit Warnungen bei Versionsunterschieden
- **Persistente Speicherung**: Alle Daten werden lokal gespeichert

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
│   ├── model/           # Datenmodelle (Spell, Character, SpellSlot)
│   ├── dao/             # Room DAOs
│   ├── repository/      # Repository-Pattern
│   └── InitialSpells.kt # Vordefinierte Zauber
├── logic/
│   └── SpellChecker.kt  # Zauberprobe-Logik mit W20-Würfeln
├── ui/
│   ├── screen/          # Composable Screens
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

## Lizenz

Dieses Projekt ist ein privates Hilfstool für DSA-Spieler. DSA ist ein eingetragenes Warenzeichen von Ulisses Spiele.
