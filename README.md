# Applicatus - DSA Zauberspeicher App

Eine Android-App zur Verwaltung von Zauberspeichern für das Pen-and-Paper-Rollenspiel Das Schwarze Auge (DSA).

## Features

- **Charakterverwaltung**: Erstellen und verwalten Sie mehrere Charaktere mit ihren Eigenschaftswerten (MU, KL, IN, CH, FF, GE, KO, KK)
- **Zauberslots**: Jeder Charakter hat 10 Zauberslots für den Applicatus-Zauber
- **Zauberprobe**: Automatische Würfelprobe beim Einspeichern von Zaubern mit W20-Würfeln
- **Besondere Würfelergebnisse**: Erkennung von Doppel-1, Dreifach-1, Doppel-20 und Dreifach-20
- **Zauberliste**: Über 200 vordefinierte Zauber aus DSA
- **Persistente Speicherung**: Alle Daten werden lokal gespeichert

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
