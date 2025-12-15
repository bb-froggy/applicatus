# Applicatus - DSA Zauberspeicher App

Eine Android-App zur Verwaltung von Zauberspeichern (Zauberstab und Applicatus), Alchimie und Inventar für das Pen-and-Paper-Rollenspiel Das Schwarze Auge (DSA) 4.1. Ein Werkzeug für Meister und Spieler am Spieltisch.

## Features

### Charakterverwaltung
- **Charaktererstellung**: Erstellen und verwalten Sie mehrere Charaktere mit ihren Eigenschaftswerten (MU, KL, IN, CH, FF, GE, KO, KK)
- **Energien**: Verwaltung von Lebensenergie (LE), Astralenergie (AE) und Karmaenergie (KE)
- **Talente**: Unterstützung für Alchimie-relevante Talente mit **Magischem Meisterhandwerk**
  - Alchimie (mit optionalem Magischen Meisterhandwerk)
  - Kochen (Tränke) (mit optionalem Magischen Meisterhandwerk)
  - Selbstbeherrschung, Sinnenschärfe
  - Magiekunde, Pflanzenkunde
- **Labor-System**: Standard-Labor pro Charakter (Archaisch, Hexenküche, Labor)
- **Gruppen-System**:
  - Charaktere gehören zu Heldengruppen
  - Austausch von Tränken und anderem Inventar in der Gruppe
  - Datum auf Dere in der Gruppe mit Anzeige des Wochentags und der Mondphase
- **Spielleiter-Modus**: Optionaler Modus, der alle versteckten Informationen anzeigt (Trank-Rezepte, Analyseergebnisse, etc.)
- **Charakterjournal**: Automatisches Protokollieren aller Ereignisse
  - Irdische Zeitstempel und derisches Datum
  - Kategorisierte Einträge (Tränke, Zauber, Energie, etc.)
  - Spieler-sichtbare und Spielleiter-exklusive Informationen
  - Export zusammen mit dem Charakter
- **Kraftkontrolle & Kraftfokus**: AsP-Kostenreduktion beim Zaubern
  - Kraftkontrolle: -1 AsP pro Zauber
  - Zauberstab mit Kraftfokus: -1 AsP (nicht bei Zauberspeicher-Nutzung)

### Zauberzeichen
- **Zauberzeichen-Verwaltung**: Verwalten Sie Zauberzeichen auf Inventar-Gegenständen
  - Voraussetzung: SF Zauberzeichen und Ritualkenntniswert (RkW)
  - Zauberzeichen auf beliebigen Items erstellen
  - Aktivierungsprobe auf KL/IN/FF mit RkW
- **Wirkdauer-Optionen**: 
  - RkW/2 Tage (aufgerundet)
  - 1 Monat, 1 Quartal
  - Bis zur Wintersonnenwende
- **Spezialeffekte**:
  - Sigille des Unsichtbaren Trägers: Gewichtsreduktion um RkP* × 2 Stein
  - Benutzerdefinierte Effekte als Freitext
- **Integration**: Zauberzeichen werden im Inventar mit Magie-Indikator angezeigt

### Zauberspeicher
- **Zwei Slot-Typen**: 
  - Applicatus-Slots für den gleichnamigen Zauber
  - Zauberspeicher-Slots mit konfigurierbaren Volumenpunkten (max. 100 gesamt)
- **Applicatus-Wirkdauer**: Konfigurierbare Speicherdauer mit Erschwernis-Modifikatoren
  - Tag (+0): Bis zum nächsten Sonnenaufgang
  - Mond (+3): Bis zum Ende des aktuellen Mondes
  - Quartal (+5): Bis zur nächsten Quartalsgrenze
  - Wintersonnenwende (+7): Bis zur nächsten Wintersonnenwende
- **Verlängerte Zauberdauer**: Optional +4 Erleichterung
- **AsP-Kostenersparnis**: Bis zu 50% Reduktion der AsP-Kosten
- **Zauberprobe**: Automatische Würfelprobe beim Einspeichern von Zaubern mit W20-Würfeln
- **Besondere Würfelergebnisse**: Erkennung von Doppel-1, Dreifach-1, Doppel-20 und Dreifach-20
- **Zauberliste**: Über 235 vordefinierte Zauber aus DSA (Standard + Hexenzauber)
- **Bearbeitungs- und Nutzungsmodus**: Getrennte Modi für Vorbereitung und Spielen

### Hexenküche (Alchimie)
- **Trankverwaltung**: Verwalten Sie selbst gebraute Tränke mit Qualitätsstufen
- **Trank-Brauen**: 
  - Vollständige Brauproben nach DSA 4.1-Regeln
  - Rezeptauswahl aus bekannten Rezepten
  - Talent-Auswahl (Alchimie, Kochen Tränke)
  - Labor-Modifikatoren (Archaisch, Hexenküche, Labor)
  - Freiwilliger Handicap und Substitutionen
  - **Magisches Meisterhandwerk**: 
    - Zusätzliche Qualitätspunkte durch AsP-Einsatz (2^(n-1) AsP)
    - Astrale Aufladung für Bonus-Wirkungen
  - Qualitätsberechnung: A-F, M
  - Brau-Animation
  - Automatische Haltbarkeitsdatum-Berechnung (derischer Kalender)
- **Rezeptverwaltung**: Über 30 vordefinierte Trank-Rezepte mit detaillierten Informationen
  - Rezeptwissen-Level: UNKNOWN, BASIC, FULL
  - Brau- und Analyseschwierigkeit
  - Labor-Anforderungen
  - Preise und Verbreitung
  - Haltbarkeit
- **Trank-Analyse**: 
  - ODEM ARCANUM zur Intensitätsbestimmung
  - ANALYS ARKANSTRUKTUR + Alchimie-Probe zur Strukturanalyse
  - Verschiedene Analysemethoden (Augenschein, Labor, Strukturanalyse-Serie)
- **Rezeptwissen**: Tracken Sie, welche Rezepte Ihr Charakter kennt
- **Spielleiter-Integration**: Im Spielleiter-Modus werden alle Informationen angezeigt

### Packesel (Inventarverwaltung)
- **Lagerorte**: Verwalten Sie Gegenstände an verschiedenen Orten
  - Standard-Orte: "Am Körper" und "Rucksack"
  - Beliebig viele eigene Orte hinzufügbar (z.B. "Pferd", "Wagen")
- **Gegenstände**: 
  - Freitext-Name
  - Gewicht in Stein und/oder Unzen (1 Stein = 40 Unzen)
  - Zuordnung zu einem Ort
- **Tränke-Integration**: Alle Tränke werden automatisch als Gegenstände angezeigt (je 4 Unzen)
- **Gewichtsberechnung**: Automatische Berechnung des Gesamtgewichts pro Ort

### Export/Import & Synchronisation
- **JSON-Export/Import**: 
  - Charaktere als JSON-Datei exportieren/importieren
  - Enthält alle Charakterdaten, Slots, Tränke (inklusive Analyse-Status), Rezeptwissen und Gruppen-Zugehörigkeit
  - Versionskontrolle (v6) mit Warnungen bei Versionsunterschieden
- **Vollständiges Datenbank-Backup**:
  - Exportiert/Importiert die komplette App-Datenbank
  - Enthält alle Zauber, Rezepte, Gruppen und Charaktere
  - Ideal für Gerätewechsel und Kampagnen-Austausch
- **Nearby Connections**: 
  - Direkte Geräte-zu-Gerät-Übertragung via Bluetooth/WLAN
  - Keine Internetverbindung erforderlich
  - Perfekt für den Austausch am Spieltisch
- **Echtzeit-Synchronisation**:
  - Star-Topologie: Spielleiter als Host, Spieler als Clients
  - Bidirektionale Kommunikation mit Last-Write-Wins
  - Sessions überleben Navigation zwischen Screens
  - Mehrere Charaktere gleichzeitig synchronisierbar
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
│   │   ├── character/   # Character, Group, GlobalSettings, CharacterJournalEntry
│   │   ├── spell/       # Spell, SpellSlot, SystemSpell, ApplicatusDuration
│   │   ├── talent/      # Talent-Enum mit Eigenschaftsproben
│   │   ├── potion/      # Potion, Recipe, RecipeKnowledge, PotionQuality, Laboratory
│   │   ├── inventory/   # Item, Location, Weight, Currency
│   │   └── magicsign/   # MagicSign, MagicSignEffect, MagicSignDuration
│   ├── dao/             # Room DAOs
│   ├── repository/      # Repository-Pattern
│   ├── export/          # Export/Import-Logik (CharacterExportManager, DatabaseBackupManager)
│   ├── nearby/          # Nearby Connections Service
│   ├── sync/            # Real-time Sync (CharacterRealtimeSyncManager, SyncSessionManager)
│   ├── InitialSpells.kt # Vordefinierte Zauber
│   └── InitialRecipes.kt # Vordefinierte Rezepte
├── logic/
│   ├── ProbeChecker.kt      # Zentrale Proben-Logik (Talente, Zauber, System-Zauber)
│   ├── SpellChecker.kt      # Zauberprobe mit Applicatus-Unterstützung
│   ├── ElixirAnalyzer.kt    # Alchimie-Analysen (ODEM, ANALYS, Alchimie)
│   ├── PotionBrewer.kt      # Trank-Brau-Logik mit Magischem Meisterhandwerk
│   ├── PotionHelper.kt      # Hilfsfunktionen für Trank-Verwaltung
│   ├── DerianDateCalculator.kt # Derischer Kalender
│   ├── MagicSignChecker.kt  # Zauberzeichen-Aktivierungsproben
│   └── RegenerationCalculator.kt # LE/AE/KE-Regeneration
├── ui/
│   ├── screen/          # Composable Screens
│   │   ├── character/   # Charakter-Screens (Home, List, Journal)
│   │   ├── spell/       # Zauberspeicher-Screens
│   │   ├── potion/      # Hexenküche-Screens (Tränke, Rezepte, Analyse, Brauen)
│   │   ├── inventory/   # Inventar-Screens (Packesel)
│   │   ├── magicsign/   # Zauberzeichen-Screens
│   │   └── NearbySyncScreen.kt
│   ├── viewmodel/       # ViewModels
│   ├── component/       # UI-Komponenten (SpellAnimation, PotionBrewAnimation)
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
- Bestimmt die magische Intensität des Tranks (schwach/stark)
- Gibt Erleichterung für nachfolgende Strukturanalysen

**2. Strukturanalyse (ANALYS + Alchimie)**:
- ANALYS ARKANSTRUKTUR: Zauberprobe auf KL/KL/IN
- Alchimie-Probe: Talentprobe auf MU/KL/FF
- Erleichterung aus Intensitätsbestimmung oder vorheriger Strukturanalyse (halbe Punkte aufgerundet)
- Bei 13+ TaP* gesamt: Verfeinerte Qualität bekannt (z.B. "stark, hochwertig")
- Bei 19+ TaP* gesamt: Rezept vollständig verstanden (FULL Rezeptwissen)

**3. Alternative Analysemethoden**:
- **Augenschein**: Sinnenschärfe-Probe (KL/IN/IN)
- **Labor**: Magiekunde oder Pflanzenkunde (KL/KL/IN bzw. KL/FF/KK)
- **Strukturanalyse-Serie**: Mehrere ANALYS-Proben mit Selbstbeherrschung

### Trank-Brauen

**1. Vorbereitung**:
- Rezept auswählen (nur bekannte Rezepte mit FULL-Wissen können gebraut werden)
- Talent wählen (Alchimie oder Kochen Tränke)
- Labor wählen (Archaisch +1, Hexenküche ±0, Labor -1)
- Optional: Freiwilliger Handicap (2 bis 1.5x Brauschwierigkeit)
- Optional: Substitutionen hinzufügen (Hochwertiger Ersatz -2/+50%, Minderwertiger +2/-50%)

**2. Magisches Meisterhandwerk** (nur mit alchemyIsMagicalMastery oder cookingPotionsIsMagicalMastery):
- **AsP-Einsatz für Qualitätspunkte**: 
  - 1 QP = 1 AsP, 2 QP = 2 AsP, 3 QP = 4 AsP, 4 QP = 8 AsP, etc.
  - Formel: 2^(n-1) AsP pro n Qualitätspunkten
  - Erhöht die Trank-Qualität direkt
- **Astrale Aufladung**: 
  - Zusätzliche AsP für magische Bonus-Wirkungen
  - Separate Eingabe

**3. Brauprobe**:
- Talentprobe mit zwei W20-Würfeln (Qualitätswürfel 1 & 2)
- Qualitätspunkte = TaW - Gesamterschwierigkeit - Überwürfe + Magisches Meisterhandwerk
- Gesamterschwierigkeit = Brauschwierigkeit + Labor + Freiwilliger Handicap + Substitutionen

**4. Qualitätsberechnung**:

**5. Haltbarkeitsdatum**:
- Automatische Berechnung nach derischem Kalender
- Basis-Haltbarkeit aus Rezept
  - Feste Zeiträume: "3 Monde", "1 Jahr", "2 Wochen"
  - **Würfelnotationen**: "3W6+2 Wochen", "2W6-1 Tage", "1W6 Monde"
- Berücksichtigt Göttermonate (30 Tage) und Namenlose Tage

### Derischer Kalender

Der derische Kalender wird für Haltbarkeitsdaten verwendet:
- **12 Göttermonate** à 30 Tage: Praios, Rondra, Efferd, Travia, Boron, Hesinde, Firun, Tsa, Phex, Peraine, Ingerimm, Rahja
- **5 Namenlose Tage** zwischen Rahja und Praios
- **Jahr** = 365 Tage (360 + 5)
- **Wochentage**: Windstag, Erdstag, Markttag, Praiostag, Rohalstag, Feuertag, Wassertag
- **Mondphasen**: 28 Tage = 1 Mada

**Würfelnotations-Unterstützung**:
Die Haltbarkeit kann mit Würfelnotationen variabel gestaltet werden:
- Format: `<Anzahl>W<Würfelgröße>[+/-Modifikator] <Einheit>`
- Beispiele:
  - "3W6+2 Wochen" → Würfelt 3W6, addiert 2, Ergebnis in Wochen (5-20 Wochen)
  - "2W6-1 Tage" → Würfelt 2W6, subtrahiert 1, Ergebnis in Tagen (1-11 Tage)
  - "1W6 Monde" → Würfelt 1W6, Ergebnis in Monden (1-6 Monde)
- Jeder Trank erhält bei der Herstellung eine individuell gewürfelte Haltbarkeit
- Unterstützte Einheiten: Tage, Wochen, Monde/Monate, Jahre

**Gruppen-System**:
- Charaktere können zu Spielgruppen gehören
- Jede Gruppe hat ihr eigenes derisches Datum
- Ermöglicht parallele Kampagnen
- Unterstützt Trank-Übergabe zwischen Charakteren der gleichen Gruppe

### Spielleiter-Modus

Charaktere können als "Spielleiter" markiert werden, wodurch:
- Alle Trank-Rezepte sichtbar sind (nicht nur die untersuchten)
- Vollständige Analyseergebnisse angezeigt werden
- ZfP*-Werte bei Zauberproben immer sichtbar sind
- Patzer-Hinweise detailliert angezeigt werden

**Wichtig**: Der Spielleiter-Modus wird beim Export NICHT übertragen und bleibt immer lokal!

## Weiterführende Dokumentation

Für detaillierte Informationen zu einzelnen Bereichen siehe:

- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Technische Implementierungsübersicht und Entwickler-Hinweise
- **[PROBECHECKER_DOCUMENTATION.md](PROBECHECKER_DOCUMENTATION.md)** - Zentrale DSA-Proben-Logik
- **[TALENT_SYSTEM_DOCUMENTATION.md](TALENT_SYSTEM_DOCUMENTATION.md)** - Talent- und System-Zauber-System
- **[POTION_BREWING_DOCUMENTATION.md](POTION_BREWING_DOCUMENTATION.md)** - Trank-Brau-System im Detail
- **[PACKESEL_DOCUMENTATION.md](PACKESEL_DOCUMENTATION.md)** - Inventarverwaltung (Packesel)
- **[EXPORT_IMPORT_GUIDE.md](EXPORT_IMPORT_GUIDE.md)** - Export/Import und Backup-Funktionen
- **[CHARACTER_SYNC_DOCUMENTATION.md](CHARACTER_SYNC_DOCUMENTATION.md)** - Echtzeit-Synchronisation
- **[JOURNAL_INTEGRATION_GUIDE.md](JOURNAL_INTEGRATION_GUIDE.md)** - Charakterjournal-System
- **[SPELL_UPDATE_GUIDE.md](SPELL_UPDATE_GUIDE.md)** - Zauber-Datenbank aktualisieren
- **[UI_TESTS_DOCUMENTATION.md](UI_TESTS_DOCUMENTATION.md)** - UI-Test-Suite
- **[NEARBY_TEST_INFRASTRUCTURE.md](NEARBY_TEST_INFRASTRUCTURE.md)** - Test-Infrastruktur für Nearby Connections

## Lizenz

Dieses Projekt ist ein privates Hilfstool für DSA-Spieler. DSA ist ein eingetragenes Warenzeichen von Ulisses Spiele.
