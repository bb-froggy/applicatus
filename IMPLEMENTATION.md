# Applicatus - Implementierungsübersicht

## ⚠️ Wichtige Hinweise für Entwickler

### DSA-Regelkonformität: Rundungsregeln

**WICHTIG: Bei DSA 4.1 gibt es unterschiedliche Rundungsregeln je nach Kontext!**

#### 1. Division durch 2: Immer aufrunden (kaufmännisches Runden)
Wenn TaP*/ZfP*/FP* halbiert werden, wird **immer aufgerundet**:
- **Beispiel**: Erleichterung bei Strukturanalyse = Hälfte der vorherigen TaP*
  - 5 TaP* → 3 Erleichterung (nicht 2)
  - 7 TaP* → 4 Erleichterung (nicht 3)

**Implementierung**: `(wert + 1) / 2`

```kotlin
// Strukturanalyse-Erleichterung
val newFacilitation = (totalAccumulatedTap + 1) / 2

// Augenschein (halbierte TaP*)
val effectiveTap = (probeResult.qualityPoints + 1) / 2
```

#### 2. Division durch 3: Kaufmännisch runden
Bei Divisionen durch 3 wird kaufmännisch gerundet (bei 0.5 und höher aufrunden):

**Implementierung**: `(wert + 1) / 3`

```kotlin
// Meisterliche Regeneration: max(KL, IN) / 3
val masteryRegen = (baseValue + 1) / 3 + 3
```

#### 3. "Je 3 Punkte"-Regeln: Nur volle 3 Punkte zählen (Abrundung)
Bei Talent-Boni wie "Je 3 Punkte in Magiekunde über 7" werden **nur volle 3 Punkte** gezählt:
- **Beispiel**: Magiekunde 9 → (9-7) = 2 Punkte → **0** Bonus (nicht 1!)
- **Beispiel**: Magiekunde 10 → (10-7) = 3 Punkte → **1** Bonus
- **Beispiel**: Magiekunde 11 → (11-7) = 4 Punkte → **1** Bonus (nicht 2!)

**Implementierung**: Einfache Integer-Division ohne Rundung: `wert / 3`

```kotlin
// Magiekunde-Bonus bei ANALYS
methodBonus = (character.magicalLoreSkill - 7) / 3  // Abrundung ist hier korrekt!

// Sinnenschärfe-Bonus bei Augenschein
methodBonus = character.sensoryAcuitySkill / 3  // Abrundung ist hier korrekt!
```

#### Zusammenfassung
- **Division durch 2**: Immer aufrunden → `(wert + 1) / 2`
- **Division durch 3 (Berechnung)**: Kaufmännisch runden → `(wert + 1) / 3`
- **"Je 3 Punkte" (Schwellenwerte)**: Nur volle 3 Punkte → `wert / 3` (normale Division)

### DSA-Regelkonformität: Magisches Meisterhandwerk

**WICHTIG: Beim Magischen Meisterhandwerk kann der TaW maximal verdoppelt werden!**

Beim Einsatz von AsP zur TaW-Erhöhung gibt es zwei wichtige Regeln:
- **Verhältnis**: 1 AsP = +2 TaW
- **Maximum**: TaW kann maximal verdoppelt werden
- **Daraus folgt**: Maximal ⌈TaW/2⌉ AsP können eingesetzt werden (aufgerundet)

**Beispiele**:
- TaW 10 → max +10 TaW (Verdopplung) → max 5 AsP (10/2)
- TaW 11 → max +11 TaW (Verdopplung) → max 6 AsP (⌈11/2⌉)
- TaW 15 → max +15 TaW (Verdopplung) → max 8 AsP (⌈15/2⌉)

**Implementierung**: `ceil(skillValue / 2.0).toInt()` oder `(skillValue + 1) / 2`

```kotlin
// Magisches Meisterhandwerk: Max TaW/2 (aufgerundet) AsP
val maxMagicalMasteryAsp = (skillValue + 1) / 2
```

**Gemeinsame UI-Komponente**: `MagicalMasteryControl.kt`
- Wiederverwendbare Compose-Komponente für alle Dialoge
- Verwendet in: `BrewPotionDialog`, `DilutionDialog`, `StructureAnalysisDialog`
- Automatische Berechnung und Validierung der AsP-Limits

### DSA-Regelkonformität: AsP-Kosten für Qualitätspunkte

Beim Brauen von Tränken mit Magischem Meisterhandwerk können zusätzliche Qualitätspunkte durch AsP-Einsatz erkauft werden:
- **Formel**: AsP-Kosten = 2^(n-1), wobei n = Anzahl der Qualitätspunkte
- **Beispiele**: 
  - 1 QP = 1 AsP (2^0)
  - 2 QP = 2 AsP (2^1)
  - 3 QP = 4 AsP (2^2)
  - 4 QP = 8 AsP (2^3)

**Implementierung**: `2.0.pow(qualityPoints - 1).toInt()`

### Build-Prozess und Testing

**WICHTIG: Immer nach Code-Änderungen einen Build durchführen!**

Nach jeder Änderung am Code sollte ein Build durchgeführt werden, um Fehler frühzeitig zu erkennen:

```bash
# Schneller Debug-Build zum Testen
.\gradlew.bat assembleDebug

# Vollständiger Build (Debug + Release)
.\gradlew.bat build

# Mit detailliertem Stacktrace bei Fehlern
.\gradlew.bat build --stacktrace
```

Wenn man UI-Tests hinzugefügt hat, sollen sie auch ausgeführt werden. Weil die UI-Tests lange dauern, sollten möglichst in jedem Durchlauf nur die UI-Tests ausgeführt werden, die auch tatsächlich interessant sind und sich verändert haben. Nach größeren Änderungen an der UI sollten alle UI-Tests durchgeführt werden und gegebenenfalls an die neue UI angepasst werden.

### Datenbank-Migrationen testen

**WICHTIG: Datenbank-Migrationen müssen immer getestet werden!**

Der `DatabaseMigrationTest` stellt sicher, dass alle Migrationen von Version 1 bis zur aktuellen Version funktionieren:

```bash
# Datenbank-Migrationstest ausführen (benötigt verbundenes Gerät/Emulator)
.\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=de.applicatus.app.data.DatabaseMigrationTest
```

**Ablauf des Tests**:
1. Erstellt Datenbank mit Schema Version 1 und Testdaten
2. Migriert schrittweise durch alle Versionen (1 → 2 → 3 → ... → 20)
3. Prüft nach jeder Migration, dass keine Exception auftritt
4. Validiert am Ende, dass alle Daten sinnvoll vorhanden sind

**Bei neuen Migrationen**:
- Migration als `val` (nicht `private val`) deklarieren
- Migration in `.addMigrations()` Liste hinzufügen
- Test erweitern um neuen Migrationsschritt
- Test ausführen, bevor ein Update veröffentlicht wird

Siehe auch: [DATABASE_MIGRATION_TEST.md](app/src/androidTest/java/de/applicatus/app/data/DATABASE_MIGRATION_TEST.md)

### Häufige Build-Fehler und deren Vermeidung

#### 1. Deprecated Compose-Komponenten
**Problem**: Einige Compose-Komponenten wurden zugunsten verbesserter Alternativen als deprecated markiert.

**Wichtige Migrationen (Compose BOM 2024.12.01+)**:

| Deprecated | Neu | Import |
|------------|-----|--------|
| `Divider()` | `HorizontalDivider()` | `androidx.compose.material3.HorizontalDivider` |
| `Icons.Default.ArrowBack` | `Icons.AutoMirrored.Filled.ArrowBack` | `androidx.compose.material.icons.automirrored.filled.ArrowBack` |
| `Icons.Default.ArrowForward` | `Icons.AutoMirrored.Filled.ArrowForward` | `androidx.compose.material.icons.automirrored.filled.ArrowForward` |
| `Icons.Default.Send` | `Icons.AutoMirrored.Filled.Send` | `androidx.compose.material.icons.automirrored.filled.Send` |
| `Icons.Default.MenuBook` | `Icons.AutoMirrored.Filled.MenuBook` | `androidx.compose.material.icons.automirrored.filled.MenuBook` |
| `Icons.Default.CallSplit` | `Icons.AutoMirrored.Filled.CallSplit` | `androidx.compose.material.icons.automirrored.filled.CallSplit` |
| `LinearProgressIndicator(progress = value)` | `LinearProgressIndicator(progress = { value })` | Lambda-basiert |
| `Modifier.menuAnchor()` | `Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)` | Mit Parametern |

**Hinweis**: AutoMirrored-Icons werden automatisch für RTL-Sprachen gespiegelt.

#### 2. Import-Statements prüfen
**Problem**: Fehlende oder falsche Import-Statements führen zu "Unresolved reference"-Fehlern.

**Lösung**:
- Prüfe alle verwendeten Icons/Komponenten auf korrekte Imports
- Bei AutoMirrored Icons: `androidx.compose.material.icons.automirrored.filled.*`
- Bei Standard Icons: `androidx.compose.material.icons.filled.*`
- Bei Material 3 Komponenten: `androidx.compose.material3.*`

#### 3. String-Ressourcen mit Platzhaltern
**Problem**: Strings mit mehreren Platzhaltern (`%d`, `%s`) benötigen das `formatted="false"` Attribut.

**Beispiel**:
```xml
<!-- FALSCH - führt zu Build-Fehler -->
<string name="message">%d neue%s Zauber wurde%s hinzugefügt.</string>

<!-- RICHTIG - mit formatted="false" -->
<string name="message" formatted="false">%d neue%s Zauber wurde%s hinzugefügt.</string>
```

**Regel**: Sobald ein String mehr als einen Platzhalter enthält oder nicht-positionierte Formate verwendet, muss `formatted="false"` hinzugefügt werden.

#### 4. Lint-Fehler vs. Compilation-Fehler
**Problem**: Lint kann manchmal Fehler melden, obwohl der Code korrekt kompiliert.

**Unterscheidung**:
- **Compilation-Fehler** (kritisch): Der Code kann nicht gebaut werden → muss behoben werden
- **Lint-Fehler** (Warnung): Meist Code-Stil oder potenzielle Probleme → können ignoriert werden

**Workaround bei Lint-Bugs**:
```bash
# Build ohne Lint-Checks (wenn Lint selbst Fehler hat)
.\gradlew.bat assembleDebug -x lint
.\gradlew.bat assembleRelease -x lint
```

### Best Practices

1. **Inkrementelle Änderungen**: Mache kleinere, testbare Änderungen statt großer Umbauten
2. **Build nach jedem Feature**: Baue die App nach jeder abgeschlossenen Änderung
3. **Fehler sofort beheben**: Behebe Build-Fehler sofort, bevor du weitermachst
4. **Kompatibilität prüfen**: Prüfe die Kompatibilität neuer APIs mit der Min SDK Version (API 26)

## ✅ Fertiggestellte Komponenten

### 1. Projektstruktur
- ✅ Gradle Build-Konfiguration (app/build.gradle.kts, build.gradle.kts, settings.gradle.kts)
- ✅ AndroidManifest.xml mit allen Permissions
- ✅ Strings, Themes, Colors
- ✅ ProGuard-Regeln
- ✅ .gitignore

### 2. Datenmodell (data/model/)
- ✅ **Spell**: Zauber mit Name und drei Eigenschaftsproben
- ✅ **SystemSpell**: System-Zauber (ODEM KL/IN/IN, ANALYS KL/KL/IN) mit festen Eigenschaftsproben
- ✅ **Talent**: Enum für alle bekannten Talente mit ihren Eigenschaftsproben
  - Alchimie (MU/KL/FF), Kochen (MU/KL/FF), Magiekunde (KL/KL/IN)
  - Pflanzenkunde (KL/FF/KK), Selbstbeherrschung (MU/MU/KO), Sinnenschärfe (KL/IN/IN)
- ✅ **Character**: Charakter mit 8 Eigenschaftswerten (MU, KL, IN, CH, FF, GE, KO, KK)
  - ✅ Applicatus-Support (hasApplicatus, applicatusZfw, applicatusModifier)
  - ✅ **Applicatus-Wirkdauer** (applicatusDuration: DAY/MOON/QUARTER/WINTER_SOLSTICE)
  - ✅ **Verlängerte Zauberdauer** (applicatusExtendedDuration) und **AsP-Kostenersparnis** (applicatusAspSavingPercent)
  - ✅ **Kraftkontrolle & Kraftfokus** (kraftkontrolle, hasStaffWithKraftfokus)
  - ✅ **Zauberzeichen-Support** (ritualKnowledgeValue, hasZauberzeichen, hasKonzentrationsstärke)
  - ✅ Alchimie-Talente (hasAlchemy, alchemySkill, alchemyIsMagicalMastery, hasCookingPotions, cookingPotionsSkill, cookingPotionsIsMagicalMastery, etc.)
  - ✅ System-Zauber (hasOdem, odemZfw, hasAnalys, analysZfw)
  - ✅ Labor-System (defaultLaboratory für Brauproben)
  - ✅ Energien (LE, AE, KE mit aktuell/max/regenBonus)
  - ✅ Spielleiter-Modus (isGameMaster)
  - ✅ Gruppen-System (groupId, group)
  - ✅ GUID für Import/Export
- ✅ **SlotType**: Enum für Slot-Typen (APPLICATUS, SPELL_STORAGE)
- ✅ **ApplicatusDuration**: Enum für Applicatus-Wirkdauer (DAY, MOON, QUARTER, WINTER_SOLSTICE) mit Erschwernis-Modifikatoren
- ✅ **SpellSlot**: Zauberslot mit ZfW, Modifikator, Variante, Füllstatus, ZfP*
  - ✅ SlotType (Applicatus oder Zauberspeicher)
  - ✅ Volumenpunkte für Zauberspeicher (1-100, max. 100 gesamt)
  - ✅ Applicatus-Würfelergebnis
- ✅ **SpellSlotWithSpell**: View-Objekt für Join zwischen Slot und Zauber
- ✅ **Potion**: Trank mit Name, Rezept-Referenz, Qualität, Analyse-Status, **locationId**, **GUID**, **Haltbarkeitsdatum**
  - ✅ Tatsächliche Eigenschaften (actualQuality, appearance, expiryDate)
  - ✅ Wissens-Status (nameKnown, categoryKnown, knownQualityLevel, intensityQuality, refinedQuality, knownExactQuality)
  - ✅ Strukturanalyse-Status (structureAnalysisTap, bestStructureAnalysisFacilitation)
- ✅ **Recipe**: Trank-Rezept mit Name, Beschreibung, Wirkung, **Brauschwierigkeit**, **Labor-Anforderung**, **Zutatenpreise**, **Verbreitung**, **Haltbarkeit**
- ✅ **PotionQuality**: Enum für Trank-Qualität (A-F, M für Meisterwerk)
- ✅ **IntensityQuality**: Enum für Intensitätsbestimmung (UNKNOWN, WEAK, STRONG)
- ✅ **RefinedQuality**: Enum für verfeinerte Qualität (UNKNOWN, WEAK_LOW, WEAK_HIGH, STRONG_LOW, STRONG_HIGH)
- ✅ **KnownQualityLevel**: Enum für Qualitätswissen (UNKNOWN, INTENSITY, REFINED, EXACT)
- ✅ **Laboratory**: Enum für Labore (ARCANE, WITCHES_KITCHEN, LABORATORY)
- ✅ **Substitution**: Ersatzstoffe für Brauen (Art, Modifier)
- ✅ **SubstitutionType**: Enum für Ersatzstoff-Typen
- ✅ **RecipeKnowledge**: Verknüpfung zwischen Charakter und bekannten Rezepten
- ✅ **RecipeKnowledgeLevel**: Enum für Rezeptwissen-Level (UNKNOWN, BASIC, FULL)
- ✅ **Weight**: Gewicht in Stein und Unzen (1 Stein = 40 Unzen)
- ✅ **Currency**: Währung (Dukaten, Silbertaler, Heller, Kreuzer)
- ✅ **Location**: Lagerort für Gegenstände (Rüstung/Kleidung, Rucksack, eigene Orte)
- ✅ **Item**: Gegenstand mit Name, Gewicht, Lagerort
- ✅ **ItemWithLocation**: View-Objekt für Items mit Location-Namen
- ✅ **Group**: Spielgruppe mit eigenem derischen Datum
- ✅ **GlobalSettings**: Globale App-Einstellungen (derisches Datum)
- ✅ **CharacterJournalEntry**: Journaleintrag mit Timestamp, derischem Datum, Kategorie und Nachrichten
- ✅ **MagicSign**: Zauberzeichen auf Inventar-Gegenständen
  - ✅ Aktivierungsmodifikator und RkP*
  - ✅ Wirkdauer und Ablaufdatum
  - ✅ Spezialeffekte (MagicSignEffect)
- ✅ **MagicSignEffect**: Enum für Zauberzeichen-Effekte (NONE, WEIGHT_REDUCTION)
- ✅ **MagicSignDuration**: Enum für Wirkdauern (HALF_RKW_DAYS, ONE_MONTH, ONE_QUARTER, UNTIL_WINTER_SOLSTICE)
- ✅ **MagicSignWithItem**: View-Objekt für Join zwischen MagicSign und Item

### 3. Datenbank (data/)
- ✅ **Room DAOs**: SpellDao, CharacterDao, SpellSlotDao, PotionDao, RecipeDao, RecipeKnowledgeDao, **ItemDao, LocationDao, GroupDao, GlobalSettingsDao, CharacterJournalDao, MagicSignDao**
- ✅ **TypeConverters**: SlotType-Converter, PotionAnalysisStatus-Converter, **Weight-Converter, Currency-Converter, Laboratory-Converter, ApplicatusDuration-Converter, MagicSignEffect-Converter, MagicSignDuration-Converter**
- ✅ **ApplicatusDatabase**: Room-Datenbank mit automatischer Initialisierung (aktuelle Version: **39**)
  - ✅ Migration von Version 1 zu 2 (neue Felder)
  - ✅ Migration von Version 2 zu 3 (Alchimie-Features)
  - ✅ Migration von Version 3 zu 4 (LE/AE/KE, Spielleiter-Modus)
  - ✅ Migration von Version 17 zu 18 (Inventar-Feature)
  - ✅ Migration zu aktueller Version (Gruppen, Brauen, erweiterte Analyse, Zauberzeichen)
- ✅ **ApplicatusRepository**: Repository-Pattern für Datenzugriff (inkl. Bereinigung von Rezeptwissen beim Import)
- ✅ **InitialSpells**: 235+ vordefinierte Zauber (magierzauber.txt + hexenzauber.txt)
- ✅ **InitialRecipes**: 30+ vordefinierte Trank-Rezepte (Rezepte.csv)

### 4. Geschäftslogik (logic/)

- ✅ **ProbeChecker**: Zentrale Klasse für DSA-Proben
  - ✅ Drei-Eigenschafts-Proben (Talente, Zauber, etc.)
  - ✅ Einfache Attributsproben (KO, IN, etc.)
  - ✅ W20 und W6 Würfelwürfe
  - ✅ Erkennung von Doppel-1, Dreifach-1, Doppel-20, Dreifach-20
  - ✅ Berechnung von Qualitätspunkten (TaP*, ZfP*, FP*)
  - ✅ Unterstützung für Erschwernisse und Erleichterungen
  - ✅ **performTalentProbe()**: Talentproben mit automatischer Eigenschaftsauswahl
  - ✅ **performSpellProbe()**: Zauberproben mit automatischer Eigenschaftsauswahl
  - ✅ **performSystemSpellProbe()**: System-Zauberproben (ODEM, ANALYS)
  - ✅ Zentrale Verwaltung aller Eigenschaften-Mappings
  
- ✅ **SpellChecker**: Zauberprobe-Implementierung (nutzt ProbeChecker)
  - ✅ Zauberproben mit ZfW und Modifikatoren
  - ✅ Formatierte Ergebnis-Strings
  - ✅ **Applicatus-Probe**: Doppelte Zauberprobe (Applicatus + eigentlicher Zauber)

- ✅ **ElixirAnalyzer**: Elixier-Analyse-Implementierung (nutzt ProbeChecker)
  - ✅ Intensitätsbestimmung (ODEM ARCANUM: KL/IN/IN)
  - ✅ Strukturanalyse (ANALYS: KL/KL/IN, Alchimie: MU/KL/FF) - eine Probe pro Analyse
  - ✅ Erleichterung aus Intensitätsbestimmung oder vorheriger Strukturanalyse (jeweils halbe Punkte aufgerundet)
  - ✅ Mehrere unabhängige Strukturanalysen mit kumulativer Verbesserung der Erleichterung
  - ✅ Berechnung von Analyseergebnissen

- ✅ **PotionBrewer**: Trank-Brau-Implementierung (nutzt ProbeChecker)
  - ✅ Brauproben mit Talenten (Alchimie, Kochen Tränke)
  - ✅ Labor-Modifikatoren (Archaisch, Hexenküche, Labor)
  - ✅ Freiwilliger Handicap (2 bis 1.5x Brauschwierigkeit)
  - ✅ Substitutionen (Hochwertiger/Minderwertiger Ersatz)
  - ✅ **Magisches Meisterhandwerk**:
    - ✅ Zusätzliche Qualitätspunkte durch AsP-Einsatz (2^(n-1) AsP pro QP)
    - ✅ Astrale Aufladung (zusätzliche Wirkungen)
  - ✅ Qualitätsberechnung (A-F, M für Meisterwerk)
  - ✅ Haltbarkeitsdatum-Berechnung (derischer Kalender)

- ✅ **DerianDateCalculator**: Derischer Kalender-Implementierung
  - ✅ 12 Göttermonate à 30 Tage + 5 Namenlose Tage
  - ✅ Datumsberechnungen (Haltbarkeitsdatum, etc.)
  - ✅ Wochentags-Berechnung
  - ✅ Mondphasen-Zyklus (28 Tage = 1 Mada)

- ✅ **PotionHelper**: Hilfsfunktionen für Trank-Verwaltung
  - ✅ Qualitäts-Level-Bestimmung
  - ✅ Display-Namen für Qualitäten

- ✅ **RegenerationCalculator**: Regenerations-Berechnung (nutzt ProbeChecker)
  - ✅ LE-Regeneration mit KO-Probe
  - ✅ AE-Regeneration mit IN-Probe
  - ✅ KE-Regeneration
  - ✅ Unterstützung für Meisterliche Regeneration

- ✅ **MagicSignChecker**: Zauberzeichen-Aktivierungsprobe
  - ✅ Aktivierungsprobe auf KL/IN/FF mit RkW
  - ✅ Berücksichtigung von Aktivierungsmodifikatoren
  - ✅ Ablaufdatum-Berechnung basierend auf Wirkdauer
  - ✅ Patzer-Erkennung (verdorbenes Zeichen)
  - ✅ canUseZauberzeichen()-Prüfung (RkW > 0 und SF Zauberzeichen)

### 5. ViewModels (ui/viewmodel/)
- ✅ **CharacterListViewModel**: Verwaltung der Charakterliste
  - ✅ Liste aller Charaktere
  - ✅ Hinzufügen von Charakteren mit Applicatus-Unterstützung
  - ✅ Löschen von Charakteren
  - ✅ Keine automatische Slot-Initialisierung mehr
  
- ✅ **CharacterHomeViewModel**: Verwaltung der Charakter-Hauptseite
  - ✅ Energien-Verwaltung (LE, AE, KE)
  - ✅ Regeneration mit Proben
  - ✅ Spielleiter-Modus-Toggle
  
- ✅ **SpellStorageViewModel**: Verwaltung der Zauberspeicher
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

- ✅ **PotionViewModel**: Verwaltung der Tränke
  - ✅ Tränke hinzufügen, bearbeiten, löschen
  - ✅ **Tränke brauen** mit vollständiger Brauprobe
  - ✅ Analyse-Status-Verwaltung (Intensität, Struktur, Qualität)
  - ✅ Rezept-Verknüpfung
  - ✅ Integration mit Spielleiter-Modus
  - ✅ Haltbarkeitsdatum-Verwaltung

- ✅ **RecipeKnowledgeViewModel**: Verwaltung des Rezeptwissens
  - ✅ Bekannte Rezepte pro Charakter
  - ✅ Rezepte hinzufügen/entfernen
  - ✅ **Rezeptwissen-Level** (UNKNOWN, BASIC, FULL)
  - ✅ Filterung nach bekannten/unbekannten Rezepten

- ✅ **InventoryViewModel**: Verwaltung des Inventars (Packesel)
  - ✅ Locations und Items verwalten
  - ✅ Tränke als virtuelle Items integrieren
  - ✅ Gewichtsberechnung pro Location
  - ✅ Items zwischen Orten verschieben
  - ✅ **Magie-Indikatoren**: Anzeige von Zauberzeichen auf Items
  - ✅ **Gewichtsreduktion**: Automatische Berechnung bei Sigille des Unsichtbaren Trägers

- ✅ **MagicSignViewModel**: Verwaltung der Zauberzeichen
  - ✅ Zauberzeichen erstellen und löschen
  - ✅ Aktivierungsproben durchführen
  - ✅ Verfügbare Ziel-Items anzeigen
  - ✅ Ablaufdatum-Tracking

- ✅ **CharacterJournalViewModel**: Verwaltung des Charakterjournals
  - ✅ Journaleinträge anzeigen und filtern
  - ✅ Spielleiter-exklusive Einträge
  - ✅ Export zusammen mit Charakter

### 6. UI-Screens (ui/screen/)
- ✅ **CharacterListScreen**: 
  - ✅ Liste aller Charaktere mit Eigenschaftswerten
  - ✅ FAB zum Hinzufügen
  - ✅ Dialog mit allen 8 Eigenschaftsfeldern + Applicatus-Feldern + Alchimie-Feldern
  - ✅ Navigation zu CharacterHomeScreen
  
- ✅ **CharacterHomeScreen**:
  - ✅ Übersicht über Charakter
  - ✅ Energien-Verwaltung (LE, AE, KE)
  - ✅ Regeneration mit Proben
  - ✅ Spielleiter-Modus-Toggle
  - ✅ Navigation zu Zauberspeicher und Hexenküche
  
- ✅ **SpellStorageScreen**:
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
  - ✅ Spielleiter-Ansicht (zeigt alle Details)
  
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

- ✅ **PotionScreen** (Hexenküche):
  - ✅ Liste aller Tränke mit Qualität und Analyse-Status
  - ✅ Anzeige von Rezeptnamen (nur für Spielleiter oder analysierte Tränke)
  - ✅ FAB zum Hinzufügen neuer Tränke
  - ✅ **BrewPotionDialog**: Vollständiges Brauen von Tränken
    - ✅ Rezeptauswahl (nur bekannte Rezepte)
    - ✅ Talent-Auswahl (Alchimie, Kochen Tränke)
    - ✅ Labor-Auswahl mit Modifikatoren
    - ✅ Freiwilliger Handicap
    - ✅ Substitutionen verwalten
    - ✅ Magisches Meisterhandwerk (AsP-Einsatz, Astrale Aufladung)
    - ✅ Brau-Animation
    - ✅ Qualitätsberechnung mit zwei W20-Würfeln
    - ✅ Automatische Haltbarkeitsdatum-Berechnung
  - ✅ Trank bearbeiten/löschen
  - ✅ Analyse-Dialoge:
    - ✅ IntensityDeterminationDialog (ODEM ARCANUM)
    - ✅ StructureAnalysisDialog (ANALYS/Augenschein/Labor) - nur eine Probe
    - ✅ PotionAnalysisDialog (Auswahl der Analysemethode)
  - ✅ Navigation zu Rezeptwissen
  - ✅ Spielleiter-Integration (zeigt alle Infos)

- ✅ **RecipeKnowledgeScreen**:
  - ✅ Liste aller Rezepte (bekannte und unbekannte)
  - ✅ Filterung nach bekannten/unbekannten Rezepten
  - ✅ Rezepte als bekannt markieren/entfernen
  - ✅ **Rezeptwissen-Level** anzeigen und ändern
  - ✅ Rezept-Details (Name, Beschreibung, Wirkung, **Brauschwierigkeit, Analyseschwierigkeit, Labor, Preise, Verbreitung, Haltbarkeit**)
  - ✅ Spielleiter sieht alle Rezepte, Spieler nur bekannte

- ✅ **NearbySyncScreen**:
  - ✅ Verbindungsstatus-Anzeige
  - ✅ Geräteliste
  - ✅ Senden/Empfangen-Buttons
  - ✅ Permission-Handling
  - ✅ Anleitungstext

- ✅ **InventoryScreen** (Packesel):
  - ✅ Liste aller Locations mit Gesamtgewicht
  - ✅ Items pro Location
  - ✅ Tränke als virtuelle Items
  - ✅ Dialoge zum Hinzufügen/Bearbeiten von Locations und Items
  - ✅ Gewichtsanzeige in Stein und Unzen
  - ✅ **Magie-Indikatoren** für Items mit Zauberzeichen/Zauberspeichern

- ✅ **MagicSignScreen** (Zauberzeichen):
  - ✅ Liste aller Zauberzeichen des Charakters
  - ✅ Charakterinfo-Card (RkW, SF Zauberzeichen)
  - ✅ FAB zum Hinzufügen neuer Zauberzeichen
  - ✅ **AddMagicSignDialog**: Auswahl von Ziel-Item und Wirkdauer
  - ✅ **ActivateMagicSignDialog**: Aktivierungsprobe mit Ergebnis-Anzeige
  - ✅ Zeigt Ablaufdatum und Aktivierungsstatus
  - ✅ Spielleiter-Integration

- ✅ **CharacterJournalScreen**:
  - ✅ Chronologische Liste aller Journaleinträge
  - ✅ Filterung nach Kategorien
  - ✅ Spielleiter-exklusive Informationen
  - ✅ Derisches Datum und irdischer Zeitstempel

### 7. Navigation (ui/navigation/)
- ✅ **Screen**: Sealed Class für Routes
- ✅ **ApplicatusNavHost**: Jetpack Compose Navigation
  - CharacterList → CharacterHome mit characterId-Parameter
  - CharacterHome → SpellStorage mit characterId-Parameter
  - CharacterHome → Potion (Hexenküche) mit characterId-Parameter
  - CharacterHome → **Inventory (Packesel)** mit characterId-Parameter
  - CharacterHome → **MagicSign (Zauberzeichen)** mit characterId-Parameter
  - CharacterHome → **CharacterJournal** mit characterId-Parameter
  - Potion → RecipeKnowledge mit characterId-Parameter
  - CharacterHome → NearbySync mit characterId-Parameter

### 8. App-Setup
- ✅ **ApplicatusApplication**: Application-Klasse mit Repository
- ✅ **MainActivity**: Activity mit Compose-Integration

## 🎯 Implementierte Features

### Charakterverwaltung
- ✅ Charaktere erstellen mit Name und 8 Eigenschaftswerten
- ✅ Energien-Verwaltung (LE, AE, KE)
- ✅ Regeneration mit Proben (KO für LE, IN für AE, automatisch für KE)
- ✅ Meisterliche Regeneration-Support
- ✅ Applicatus-Unterstützung (optional)
  - ✅ Applicatus ZfW und Modifikator
  - ✅ Automatische Probe auf KL/IN/CH beim Zaubern
- ✅ Alchimie-Talente und -Zauber
  - ✅ Alchimie, Kochen (Tränke), Selbstbeherrschung, Sinnenschärfe
  - ✅ Magiekunde, Pflanzenkunde
  - ✅ ODEM ARCANUM, ANALYS ARKANSTRUKTUR
- ✅ Spielleiter-Modus (zeigt alle versteckten Informationen)
- ✅ Charaktere anzeigen und löschen
- ✅ Charaktereigenschaften bearbeiten
- ✅ Persistente Speicherung

### Zauberverwaltung
- ✅ 235+ vordefinierte Zauber (Initial-Zauber + Hexenzauber)
- ✅ Automatische Initialisierung beim ersten Start
- ✅ **Zauber-Datenbank-Synchronisation**:
  - ✅ Menüpunkt "Zauber-Datenbank aktualisieren"
  - ✅ Erkennung fehlender Zauber (Vergleich mit InitialSpells)
  - ✅ Automatisches Hinzufügen neuer Zauber nach App-Updates
  - ✅ Statusmeldung über Anzahl hinzugefügter Zauber
  - ✅ Keine Duplikate (Abgleich über Zaubernamen)

### Zauberslot-System
- ✅ Variable Anzahl von Slots (nicht mehr fix 10)
- ✅ **Zwei Slot-Typen**:
  - ✅ **Applicatus-Slots**: Nutzen Applicatus-Zauber
  - ✅ **Zauberspeicher-Slots**: Direkte Speicherung
    - ✅ Volumenpunkte (1-100 pro Slot)
    - ✅ Max. 100 Volumenpunkte gesamt
- ✅ Slots hinzufügen und entfernen
- ✅ Zauberauswahl aus 235+ Zaubern
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
  - ✅ Spielleiter-Ansicht (zeigt alle Details)
  
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

### Hexenküche (Alchimie)
- ✅ **Trankverwaltung**:
  - ✅ Tränke erstellen mit Name, Rezept, Qualität
  - ✅ Tränke bearbeiten und löschen
  - ✅ Analyse-Status pro Trank
  - ✅ **Haltbarkeitsdatum** (automatisch berechnet)
  - ✅ Spielleiter sieht alle Infos, Spieler nur analysierte
  
- ✅ **Trank-Brauen**:
  - ✅ Rezeptauswahl aus bekannten Rezepten
  - ✅ Talent-Auswahl (Alchimie, Kochen Tränke)
  - ✅ Labor-Modifikatoren (Archaisch +1, Hexenküche ±0, Labor -1)
  - ✅ Freiwilliger Handicap (min. 2, max. 1.5x Brauschwierigkeit)
  - ✅ Substitutionen:
    - ✅ Hochwertiger Ersatz (-2 Erschwernis, +50% Kosten)
    - ✅ Minderwertiger Ersatz (+2 Erschwernis, -50% Kosten)
  - ✅ **Magisches Meisterhandwerk** (nur mit alchemyIsMagicalMastery oder cookingPotionsIsMagicalMastery):
    - ✅ Zusätzliche Qualitätspunkte durch AsP-Einsatz (Kosten: 2^(n-1) AsP)
    - ✅ Astrale Aufladung (Bonus-Wirkungen durch zusätzliche AsP)
  - ✅ Qualitätsberechnung:
    - ✅ Zwei W20-Würfel (Qualitätswürfel 1 & 2)
    - ✅ QP = TaW - Erschwernis - Überwürfe + MagischeMeisterhandwerk-Bonus
    - ✅ Qualität: A (13+), B (10-12), C (7-9), D (4-6), E (1-3), F (≤0)
    - ✅ Meisterwerk (M): Beide Würfel = 1
  - ✅ Brau-Animation
  - ✅ Automatische Haltbarkeitsdatum-Berechnung
  
- ✅ **Trank-Analyse**:
  - ✅ **Intensitätsbestimmung**: ODEM ARCANUM (KL/IN/IN)
  - ✅ **Strukturanalyse**: ANALYS (KL/KL/IN) + Alchimie (MU/KL/FF) - eine Probe pro Analyse
  - ✅ **Augenschein**: Sinnenschärfe (KL/IN/IN)
  - ✅ **Labor**: Magiekunde oder Pflanzenkunde
  - ✅ **Mehrere unabhängige Strukturanalysen möglich**
  - ✅ Erleichterung aus Intensitätsbestimmung (halbe ZfP* aufgerundet) ODER vorheriger Strukturanalyse (halbe TaP* aufgerundet)
  - ✅ Beste Erleichterung wird gespeichert und bei nächster Analyse verwendet
  - ✅ Rezept verstehen bei 19+ TaP*
  
- ✅ **Rezeptverwaltung**:
  - ✅ 30+ vordefinierte Rezepte (Rezepte.csv)
  - ✅ Rezeptwissen pro Charakter mit Levels (UNKNOWN, BASIC, FULL)
  - ✅ Rezepte als bekannt markieren
  - ✅ Filterung nach bekannten/unbekannten Rezepten
  - ✅ Automatisches Hinzufügen bei erfolgreicher Analyse

### Packesel (Inventarverwaltung)
- ✅ **Lagerorte**:
  - ✅ Standard-Orte: "Rüstung/Kleidung" und "Rucksack" (automatisch erstellt)
  - ✅ Eigene Orte hinzufügen/löschen
  - ✅ Gesamtgewicht pro Ort
  
- ✅ **Gegenstände**:
  - ✅ Freitext-Name
  - ✅ Gewicht in Stein und/oder Unzen (1 Stein = 40 Unzen)
  - ✅ Zuordnung zu einem Ort
  - ✅ Hinzufügen, Bearbeiten, Löschen
  - ✅ Zwischen Orten verschieben
  
- ✅ **Tränke-Integration**:
  - ✅ Alle Tränke werden automatisch als Gegenstände angezeigt
  - ✅ Festes Gewicht: 4 Unzen pro Trank
  - ✅ Kennzeichnung durch Icon
  - ✅ Verwaltung über Hexenküche
  
- ✅ **Gewichtsberechnung**:
  - ✅ Automatische Berechnung pro Ort
  - ✅ Anzeige in Stein und Unzen
  - ✅ Berücksichtigung von Items und Tränken

### Export/Import & Synchronisation
- ✅ **JSON-Export/Import**:
  - ✅ Charaktere als JSON exportieren
  - ✅ Inklusive Slots, Tränke, Analyse-Status, Rezeptwissen, **Gruppen-Zugehörigkeit**
  - ✅ Versionskontrolle (DataModelVersion = 5)
  - ✅ Kompatibilitätsprüfung
  - ✅ Warnung bei Versionsunterschieden
  - ✅ Warnung beim Überschreiben
  - ✅ **Spielleiter-Modus wird NICHT exportiert** (bleibt lokal)
  
- ✅ **Nearby Connections**:
  - ✅ Gerätesuche via Bluetooth/WLAN
  - ✅ Direkte Peer-to-Peer-Verbindung
  - ✅ Charakter-Übertragung
  - ✅ Versionsprüfung
  - ✅ Permission-Management
  - ✅ **Spielleiter-Modus wird NICHT übertragen** (bleibt lokal)

### Spielleiter-Modus
- ✅ **Pro Charakter aktivierbar** (isGameMaster-Flag)
- ✅ **Zeigt alle versteckten Informationen**:
  - ✅ Trank-Rezepte (auch nicht analysierte)
  - ✅ Vollständige Analyseergebnisse
  - ✅ ZfP*-Werte bei Zauberproben
  - ✅ Detaillierte Patzer-Hinweise
- ✅ **Bleibt immer lokal**:
  - ✅ Wird NICHT im Export-JSON gespeichert
  - ✅ Wird NICHT via Nearby Connections übertragen
  - ✅ Beim Import wird existierender Wert beibehalten
- ✅ **Spieler/Spielleiter können Charaktere austauschen**:
  - ✅ Jeder behält seine eigene Ansicht
  - ✅ Spielleiter sieht alle Details
  - ✅ Spieler sieht nur analysierte/bekannte Infos

### Persistenz
- ✅ Room-Datenbank für alle Daten
- ✅ Migration von v1 → v2 → v3 → v4
- ✅ Automatische Initialisierung mit Zaubern und Rezepten beim ersten Start
- ✅ Status der gefüllten Slots bleibt erhalten
- ✅ Alle Änderungen werden automatisch gespeichert
- ✅ Bereinigung von Tränken und Rezeptwissen beim Import

### UI/UX
- ✅ Material Design 3
- ✅ Jetpack Compose
- ✅ Responsive Layouts
- ✅ Intuitive Navigation
- ✅ Farbcodierung für Erfolg/Misserfolg
- ✅ Kompakte und ausführliche Ansichten
- ✅ Kontextabhängige Dialoge

## 📝 Nicht implementiert (optional für Zukunft)

- ⚪ Zauber bearbeiten/hinzufügen in der App
- ⚪ Statistiken über Würfelerfolge
- ⚪ Themes (Hell/Dunkel)
- ⚪ Cloud-Backup-Integration

## 🚀 Build & Run

Die App ist komplett und funktionsfähig. Um sie zu bauen:

1. Öffnen Sie das Projekt in Android Studio
2. Warten Sie auf Gradle-Sync
3. Führen Sie die App aus (Run → Run 'app' oder Shift+F10)

Die App benötigt:
- Min SDK: Android 8.0 (API 26)
- Target SDK: Android 14 (API 34)

Bei der ersten Ausführung werden automatisch alle 235+ Zauber und 30+ Rezepte in die Datenbank geladen.

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

## 🆕 Neue Features (Version 3 - Export/Import & Synchronisation)

### Charakter-Export/Import
- ✅ **JSON-Export**: Charaktere als JSON-Datei exportieren
  - Enthält alle Charakterdaten, Slots, Tränke (inklusive Analyse-Status) und bekannte Rezepte
  - Mit Datenmodell-Versionsnummer
  - Zeitstempel des Exports
  - **Spielleiter-Modus wird NICHT exportiert** (bleibt lokal)
- ✅ **JSON-Import**: Charaktere aus JSON-Dateien importieren
  - Automatische Versionskompatibilitätsprüfung
  - Zauber-Matching nach Namen
  - Warnung bei Überschreiben existierender Charaktere
  - Warnung bei Versionsunterschieden
  - **Spielleiter-Modus bleibt lokal erhalten** (wird nicht überschrieben)

### Nearby Connections Synchronisation
- ✅ **Gerätesuche**: Entdeckung von Geräten in der Nähe via Bluetooth/WLAN
- ✅ **Verbindungsaufbau**: Direkte Peer-to-Peer-Verbindung zwischen Geräten
- ✅ **Charakter-Übertragung**: Senden und Empfangen von Charakterdaten
- ✅ **Versionsprüfung**: Warnung bei inkompatiblen Datenmodell-Versionen
- ✅ **Berechtigungsverwaltung**: Automatische Anfrage erforderlicher Permissions
- ✅ **Spielleiter-Modus wird NICHT übertragen** (bleibt lokal)

### Datenmodell-Versionierung
- ✅ **Versionsnummer**: Aktuelle Version **6** des Datenmodells
- ✅ **Kompatibilitätscheck**: Prüfung bei Import/Sync
- ✅ **Warnungen**: 
  - Bei älteren Versionen (Import möglich mit Warnung)
  - Bei neueren Versionen (Import blockiert, App-Update nötig)
  - Beim Überschreiben mit älterer Version
- ✅ **Versions-Historie**:
  - v1: Initiale Version
  - v2: Applicatus-Unterstützung, SlotType, Volumenpunkte
  - v3: Alchemie-, Energie-, Trank- und Rezeptwissen-Daten
  - v4: Gruppen für Charaktere, GUID für Tränke (Trank-Übergabe)
  - v5: Magisches Meisterhandwerk für Alchimie und Kochen (Tränke)
  - **v6**: Zauberzeichen-Export, Creator-GUID für SpellSlots/MagicSigns, Item-GUID, Item-Bindung für SpellSlots

## 🆕 Neue Features (Version 5 - Trank-Brauen & Magisches Meisterhandwerk)

### Trank-Brauen-System
- ✅ **Vollständige Brauprobe-Implementierung**:
  - Rezeptauswahl aus bekannten Rezepten
  - Talent-Auswahl (Alchimie, Kochen Tränke)
  - Labor-Modifikatoren
  - Freiwilliger Handicap (2 bis 1.5x Brauschwierigkeit)
  - Substitutionen (Hochwertiger/Minderwertiger Ersatz)
- ✅ **Qualitätsberechnung**:
  - Zwei W20-Würfel für Qualitätswürfel
  - Qualitätspunkte = TaW - Erschwernis - Überwürfe
  - Qualitätsstufen: A (13+), B (10-12), C (7-9), D (4-6), E (1-3), F (≤0)
  - Meisterwerk (M): Beide Würfel = 1
- ✅ **Haltbarkeitsdatum**:
  - Automatische Berechnung nach derischem Kalender
  - Unterstützt Monde, Wochen, Jahre

### Magisches Meisterhandwerk
- ✅ **AsP-Einsatz für zusätzliche Qualitätspunkte**:
  - Kosten: 2^(n-1) AsP pro n Qualitätspunkten
  - Nur verfügbar mit alchemyIsMagicalMastery oder cookingPotionsIsMagicalMastery
  - Erhöht die Trank-Qualität
- ✅ **Astrale Aufladung**:
  - Zusätzliche AsP für magische Bonus-Wirkungen
  - Separate Eingabe in der UI

### Derischer Kalender
- ✅ **DerianDateCalculator**:
  - 12 Göttermonate à 30 Tage
  - 5 Namenlose Tage (zwischen Rahja und Praios)
  - Wochentags-Berechnung (7-Tage-Woche)
  - Mondphasen-Zyklus (28 Tage = 1 Mada)
- ✅ **GlobalSettings & Gruppen**:
  - Globales derisches Datum
  - Gruppen-spezifische Daten
  - Unterstützt parallele Spielgruppen

### Erweiterte Trank-Analyse
- ✅ **Qualitätsstufen-System**:
  - IntensityQuality (WEAK, STRONG)
  - RefinedQuality (WEAK_LOW, WEAK_HIGH, STRONG_LOW, STRONG_HIGH)
  - KnownQualityLevel (UNKNOWN, INTENSITY, REFINED, EXACT)
- ✅ **Rezeptwissen-Level**:
  - UNKNOWN: Rezept unbekannt
  - BASIC: Grundlegendes Wissen (Name, grobe Wirkung)
  - FULL: Vollständiges Wissen (alle Details, kann brauen)

### UI-Verbesserungen
- ✅ **BrewPotionDialog**: Kompletter Dialog für Trank-Brauen
- ✅ **PotionBrewAnimation**: Animierte Brau-Sequenz
- ✅ **Erweiterte Rezept-Details**: Preise, Verbreitung, Haltbarkeit, Labor-Anforderungen

## 🆕 Neue Features (Version 4 - Alchimie & Spielleiter-Modus)

### Hexenküche (Alchimie-System)
- ✅ **Trankverwaltung**: Erstellen, Bearbeiten und Löschen von Tränken
  - Name, Rezept-Verknüpfung, Qualitätsstufe (1-6)
  - Analyse-Status (Intensität, Struktur, verstanden)
- ✅ **Trank-Analyse**: Verschiedene Analysemethoden
  - ODEM ARCANUM zur Intensitätsbestimmung
  - ANALYS + Alchimie zur Strukturanalyse
  - Augenschein (Sinnenschärfe)
  - Laboranalyse (Magiekunde/Pflanzenkunde)
  - Strukturanalyse-Serie (mehrere ANALYS + Selbstbeherrschung)
- ✅ **Rezeptverwaltung**: 30+ vordefinierte Rezepte
  - Rezeptwissen pro Charakter
  - Automatisches Hinzufügen bei erfolgreicher Analyse
  - Filterung nach bekannten/unbekannten Rezepten

### Spielleiter-Modus
- ✅ **Pro Charakter aktivierbar**: Optional per Toggle
- ✅ **Zeigt alle versteckten Informationen**:
  - Trank-Rezepte (auch nicht analysierte)
  - Vollständige Analyseergebnisse
  - ZfP*-Werte bei Zauberproben
  - Detaillierte Patzer-Hinweise
- ✅ **Bleibt immer lokal**: Wird NICHT exportiert oder übertragen
- ✅ **Spieler/Spielleiter-Kompatibilität**:
  - Charaktere können zwischen Spielern und Spielleitern ausgetauscht werden
  - Jeder behält seine eigene Ansicht
  - Export enthält KEIN isGameMaster-Feld
  - Import behält existierenden isGameMaster-Wert bei

### Charakter-Erweiterungen
- ✅ **Energien-System**:
  - Lebensenergie (LE): Aktuell/Max/RegenBonus
  - Astralenergie (AE): Aktuell/Max/RegenBonus
  - Karmaenergie (KE): Aktuell/Max
  - Meisterliche Regeneration-Support
- ✅ **Talente (für Alchimie relevant)**:
  - Alchimie (mit Magischem Meisterhandwerk-Option)
  - Kochen (Tränke) (mit Magischem Meisterhandwerk-Option)
  - Selbstbeherrschung, Sinnenschärfe
  - Magiekunde, Pflanzenkunde
- ✅ **System-Zauber**:
  - ODEM ARCANUM (KL/IN/IN)
  - ANALYS ARKANSTRUKTUR (KL/KL/IN)
- ✅ **Labor-System**:
  - Standard-Labor pro Charakter
  - Drei Labor-Typen: Archaisch (+1), Hexenküche (±0), Labor (-1)
- ✅ **Gruppen-System**:
  - Charaktere können zu Gruppen gehören
  - Gruppen haben eigenes derisches Datum
  - Ermöglicht Trank-Übergabe zwischen Charakteren

### Regeneration
- ✅ **LE-Regeneration**: KO-Probe mit Bonus
- ✅ **AE-Regeneration**: IN-Probe mit Bonus
- ✅ **KE-Regeneration**: Automatisch (1 pro Tag)
- ✅ **Meisterliche Regeneration**: Hohe AE-Regeneration

### Implementierte Komponenten

#### Backend
- ✅ `CharacterExportManager.kt`: Export/Import-Logik mit Dateiverwaltung (bereinigt Tränke & Rezeptwissen vor Import)
- ✅ `DataModelVersion.kt`: Versionsverwaltung und Kompatibilitätsprüfung
- ✅ `CharacterExportDto.kt`: DTOs für Serialisierung (Character, SpellSlot, Potion, RecipeKnowledge)
- ✅ `NearbyConnectionsService.kt`: Wrapper für Google Nearby Connections API
  - Advertising (als Empfänger bereitstellen)
  - Discovery (nach Geräten suchen)
  - Connection Management
  - Datentransfer

#### ViewModels
- ✅ `CharacterDetailViewModel`: Erweitert um Export/Import-Funktionen
- ✅ `NearbySyncViewModel`: Neues ViewModel für Nearby-Synchronisation
  - Geräteverwaltung
  - Verbindungsstatus
  - Sende-/Empfangsstatus

#### UI
- ✅ `CharacterDetailScreen`: Erweitert um Export/Import-Menu
  - "Als JSON exportieren" Option
  - "JSON importieren" Option
  - "Nearby Sync" Navigation
  - Erfolgs-/Fehlermeldungen
- ✅ `NearbySyncScreen`: Neuer Screen für Nearby-Synchronisation
  - Verbindungsstatus-Anzeige
  - Geräteliste
  - Senden/Empfangen-Buttons
  - Permission-Handling
  - Anleitungstext

#### Dependencies
- ✅ `kotlinx-serialization-json`: JSON-Serialisierung
- ✅ `play-services-nearby`: Google Nearby Connections API

#### Permissions
- ✅ Bluetooth-Permissions (BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_ADVERTISE, etc.)
- ✅ WLAN-Permissions (ACCESS_WIFI_STATE, CHANGE_WIFI_STATE)
- ✅ Standort-Permissions (ACCESS_FINE_LOCATION für Nearby)
- ✅ Nearby-WLAN-Permissions (NEARBY_WIFI_DEVICES für Android 13+)

### Nutzungsszenarien

1. **Backup erstellen**: Charakter als JSON exportieren und auf Cloud speichern
2. **Gerät wechseln**: Charakter exportieren, auf neues Gerät übertragen und importieren
3. **Schnelle Übertragung**: Nearby Sync für direkten Transfer zwischen zwei Geräten
4. **Charaktere teilen**: JSON-Datei mit anderen Spielern teilen

## 🆕 Neue Features (Version 6 - Zauberzeichen & Erweiterte Applicatus-Optionen)

### Zauberzeichen-System
- ✅ **Zauberzeichen auf Items**: Magische Zeichen auf Inventar-Gegenständen
  - Voraussetzung: SF Zauberzeichen und RkW > 0
  - Beliebige Items als Ziel wählbar
  - Aktivierungsprobe auf KL/IN/FF mit RkW
- ✅ **Wirkdauer-Optionen**:
  - RkW/2 Tage (aufgerundet)
  - 1 Monat (30 Tage)
  - 1 Quartal (90 Tage)
  - Bis zur Wintersonnenwende (1. Firun)
- ✅ **Spezialeffekte**:
  - NONE: Nur Freitext-Beschreibung
  - WEIGHT_REDUCTION: Sigille des Unsichtbaren Trägers (RkP* × 2 Stein Gewichtsreduktion)
- ✅ **Ablaufdatum**: Automatische Berechnung basierend auf derischem Kalender
- ✅ **Patzer-Handling**: Bei Doppel-20/Dreifach-20 ist das Zeichen verdorben

### Erweiterte Applicatus-Optionen
- ✅ **Wirkdauer-Konfiguration** (ApplicatusDuration):
  - Tag (+0): Bis zum nächsten Sonnenaufgang
  - Mond (+3): Bis zum Ende des aktuellen Mondes
  - Quartal (+5): Bis zur nächsten Quartalsgrenze
  - Wintersonnenwende (+7): Bis zur nächsten Wintersonnenwende
- ✅ **Verlängerte Zauberdauer**: +4 Erleichterung beim Einspeichern
- ✅ **AsP-Kostenersparnis**: 0-50% Reduktion der Applicatus-Kosten

### Kraftkontrolle & Kraftfokus
- ✅ **Kraftkontrolle**: SF für -1 AsP pro Zauber
- ✅ **Zauberstab mit Kraftfokus**: -1 AsP (nicht bei Zauberspeicher-Nutzung)

### Vollständiges Datenbank-Backup
- ✅ **DatabaseBackupManager**: Export/Import der kompletten Datenbank
  - Alle Zauber und Rezepte
  - Alle Gruppen mit derischem Datum
  - Alle Charaktere mit allen Daten
  - Fortschrittsanzeige während Export/Import

### Echtzeit-Synchronisation
- ✅ **CharacterRealtimeSyncManager**: Bidirektionale Live-Synchronisation
- ✅ **SyncSessionManager**: Singleton für Session-Verwaltung auf Application-Ebene
  - Sessions überleben Navigation zwischen Screens
  - Mehrere Charaktere gleichzeitig synchronisierbar
  - Multi-Character-Sync: GUID-basiertes Routing von eingehenden Snapshots
  - Star-Topologie (Spielleiter als Host, Spieler als Clients)
  - Last-Write-Wins Konfliktauflösung
- ✅ **Journal-Synchronisation**: Journal-Einträge werden mit synchronisiert
- ✅ **Trank-Lagerorte**: locationName wird korrekt exportiert/importiert

### Charakterjournal
- ✅ **CharacterJournalEntry**: Automatisches Protokollieren aller Ereignisse
  - Irdischer Timestamp und derisches Datum
  - Kategorisierte Einträge (Potion.*, Spell.*, Energy.*, etc.)
  - Spieler-sichtbare und Spielleiter-exklusive Nachrichten
- ✅ **JournalCategory**: Vordefinierte Konstanten für Ereignistypen
- ✅ **Export**: Journal wird mit Charakter exportiert

## 📚 Weiterführende Dokumentation

Für detaillierte Informationen zu einzelnen Bereichen siehe:

- **[README.md](README.md)** - Projekt-Übersicht und Feature-Beschreibung
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
- **[app/src/androidTest/java/de/applicatus/app/data/DATABASE_MIGRATION_TEST.md](app/src/androidTest/java/de/applicatus/app/data/DATABASE_MIGRATION_TEST.md)** - Datenbank-Migrationstest
