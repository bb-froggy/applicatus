# Applicatus - Implementierungsübersicht

## ⚠️ Wichtige Hinweise für Entwickler

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

### Häufige Build-Fehler und deren Vermeidung

#### 1. Nicht existierende Compose-Komponenten
**Problem**: Verwendung von Compose-Komponenten, die in der verwendeten Version nicht verfügbar sind.

**Beispiel**: `HorizontalDivider` (Material 3) vs. `Divider` (Material 2)

**Lösung**: 
- Prüfe die verfügbaren Komponenten in der verwendeten Compose-Version
- Bei Unsicherheit: Verwende etablierte Komponenten wie `Divider` statt neuerer Alternativen
- Teste den Build nach dem Hinzufügen neuer UI-Komponenten

#### 2. String-Ressourcen mit Platzhaltern
**Problem**: Strings mit mehreren Platzhaltern (`%d`, `%s`) benötigen das `formatted="false"` Attribut.

**Beispiel**:
```xml
<!-- FALSCH - führt zu Build-Fehler -->
<string name="message">%d neue%s Zauber wurde%s hinzugefügt.</string>

<!-- RICHTIG - mit formatted="false" -->
<string name="message" formatted="false">%d neue%s Zauber wurde%s hinzugefügt.</string>
```

**Regel**: Sobald ein String mehr als einen Platzhalter enthält oder nicht-positionierte Formate verwendet, muss `formatted="false"` hinzugefügt werden.

#### 3. Import-Statements prüfen
**Problem**: Fehlende oder falsche Import-Statements führen zu "Unresolved reference"-Fehlern.

**Lösung**:
- Prüfe alle verwendeten Icons/Komponenten auf korrekte Imports
- Bei Material Design Icons: `androidx.compose.material.icons.filled.*`
- Bei Material 3 Komponenten: `androidx.compose.material3.*`

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
  - ✅ Alchimie-Talente (hasAlchemy, alchemySkill, hasCookingPotions, cookingPotionsSkill, etc.)
  - ✅ System-Zauber (hasOdem, odemZfw, hasAnalys, analysZfw)
  - ✅ Energien (LE, AE, KE mit aktuell/max/regenBonus)
  - ✅ Spielleiter-Modus (isGameMaster)
- ✅ **SlotType**: Enum für Slot-Typen (APPLICATUS, SPELL_STORAGE)
- ✅ **SpellSlot**: Zauberslot mit ZfW, Modifikator, Variante, Füllstatus, ZfP*
  - ✅ SlotType (Applicatus oder Zauberspeicher)
  - ✅ Volumenpunkte für Zauberspeicher (1-100, max. 100 gesamt)
  - ✅ Applicatus-Würfelergebnis
- ✅ **SpellSlotWithSpell**: View-Objekt für Join zwischen Slot und Zauber
- ✅ **Potion**: Trank mit Name, Rezept-Referenz, Qualität, Analyse-Status
- ✅ **Recipe**: Trank-Rezept mit Name, Beschreibung, Wirkung
- ✅ **PotionAnalysisStatus**: Status der Trank-Analyse (Intensität, Struktur, verstanden)
- ✅ **RecipeKnowledge**: Verknüpfung zwischen Charakter und bekannten Rezepten

### 3. Datenbank (data/)
- ✅ **Room DAOs**: SpellDao, CharacterDao, SpellSlotDao, PotionDao, RecipeDao, RecipeKnowledgeDao
- ✅ **TypeConverters**: SlotType-Converter, PotionAnalysisStatus-Converter
- ✅ **ApplicatusDatabase**: Room-Datenbank mit automatischer Initialisierung
  - ✅ Migration von Version 1 zu 2 (neue Felder)
  - ✅ Migration von Version 2 zu 3 (Alchimie-Features)
  - ✅ Migration von Version 3 zu 4 (LE/AE/KE, Spielleiter-Modus)
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
  - ✅ Strukturanalyse (ANALYS: KL/KL/IN, Alchimie: MU/KL/FF)
  - ✅ Selbstbeherrschungsprobe (MU/MU/KO)
  - ✅ Strukturanalyse mit mehreren Proben
  - ✅ Selbstbeherrschungs-Proben
  - ✅ Berechnung von Analyseergebnissen

- ✅ **PotionAnalyzer**: Tranksanalyse-Implementierung (nutzt ProbeChecker)
  - ✅ Analyse nach verschiedenen Methoden (ODEM, Augenschein, Labor, Strukturanalyse-Serie)
  - ✅ Bestimmung des Analysestatus
  - ✅ Rezept-Verständnis bei 19+ TaP*
  - ✅ Vollständige Integration mit PotionAnalysisStatus

- ✅ **RegenerationCalculator**: Regenerations-Berechnung (nutzt ProbeChecker)
  - ✅ LE-Regeneration mit KO-Probe
  - ✅ AE-Regeneration mit IN-Probe
  - ✅ KE-Regeneration
  - ✅ Unterstützung für Meisterliche Regeneration

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
  - ✅ Analyse-Status-Verwaltung
  - ✅ Rezept-Verknüpfung
  - ✅ Integration mit Spielleiter-Modus

- ✅ **RecipeKnowledgeViewModel**: Verwaltung des Rezeptwissens
  - ✅ Bekannte Rezepte pro Charakter
  - ✅ Rezepte hinzufügen/entfernen
  - ✅ Filterung nach bekannten/unbekannten Rezepten

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
  - ✅ Trank bearbeiten/löschen
  - ✅ Analyse-Dialoge:
    - ✅ IntensityDeterminationDialog (ODEM ARCANUM)
    - ✅ StructureAnalysisDialog (ANALYS + Alchimie)
    - ✅ PotionAnalysisDialog (Augenschein, Labor, Strukturanalyse-Serie)
  - ✅ Navigation zu Rezeptwissen
  - ✅ Spielleiter-Integration (zeigt alle Infos)

- ✅ **RecipeKnowledgeScreen**:
  - ✅ Liste aller Rezepte (bekannte und unbekannte)
  - ✅ Filterung nach bekannten/unbekannten Rezepten
  - ✅ Rezepte als bekannt markieren/entfernen
  - ✅ Rezept-Details (Name, Beschreibung, Wirkung)
  - ✅ Spielleiter sieht alle Rezepte, Spieler nur bekannte

- ✅ **NearbySyncScreen**:
  - ✅ Verbindungsstatus-Anzeige
  - ✅ Geräteliste
  - ✅ Senden/Empfangen-Buttons
  - ✅ Permission-Handling
  - ✅ Anleitungstext

### 7. Navigation (ui/navigation/)
- ✅ **Screen**: Sealed Class für Routes
- ✅ **ApplicatusNavHost**: Jetpack Compose Navigation
  - CharacterList → CharacterHome mit characterId-Parameter
  - CharacterHome → SpellStorage mit characterId-Parameter
  - CharacterHome → Potion (Hexenküche) mit characterId-Parameter
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
  - ✅ Spielleiter sieht alle Infos, Spieler nur analysierte
  
- ✅ **Trank-Analyse**:
  - ✅ **Intensitätsbestimmung**: ODEM ARCANUM (KL/IN/IN)
  - ✅ **Strukturanalyse**: ANALYS (KL/KL/IN) + Alchimie (MU/KL/FF)
  - ✅ **Augenschein**: Sinnenschärfe (KL/IN/IN)
  - ✅ **Labor**: Magiekunde oder Pflanzenkunde
  - ✅ **Strukturanalyse-Serie**: Mehrere ANALYS-Proben + Selbstbeherrschung
  - ✅ Rezept verstehen bei 19+ TaP* gesamt
  
- ✅ **Rezeptverwaltung**:
  - ✅ 30+ vordefinierte Rezepte (Rezepte.csv)
  - ✅ Rezeptwissen pro Charakter
  - ✅ Rezepte als bekannt markieren
  - ✅ Filterung nach bekannten/unbekannten Rezepten
  - ✅ Automatisches Hinzufügen bei erfolgreicher Analyse

### Export/Import & Synchronisation
- ✅ **JSON-Export/Import**:
  - ✅ Charaktere als JSON exportieren
  - ✅ Inklusive Slots, Tränke, Analyse-Status, Rezeptwissen
  - ✅ Versionskontrolle (DataModelVersion)
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
- ✅ **Versionsnummer**: Aktuelle Version 4 des Datenmodells
- ✅ **Kompatibilitätscheck**: Prüfung bei Import/Sync
- ✅ **Warnungen**: 
  - Bei älteren Versionen (Import möglich mit Warnung)
  - Bei neueren Versionen (Import blockiert, App-Update nötig)
  - Beim Überschreiben mit älterer Version

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
  - Alchimie, Kochen (Tränke), Selbstbeherrschung
  - Sinnenschärfe, Magiekunde, Pflanzenkunde
- ✅ **System-Zauber**:
  - ODEM ARCANUM (KL/IN/IN)
  - ANALYS ARKANSTRUKTUR (KL/KL/IN)

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

