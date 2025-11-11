# Trank-Brauen - Dokumentation

## Überblick

Das Trank-Brauen-Feature ermöglicht es Spielern, Tränke nach DSA 4.1-Regeln zu brauen. Spielleiter können weiterhin Tränke direkt hinzufügen.

## Spieler vs. Spielleiter

### Spieler
- Können nur Tränke brauen, wenn sie das Rezept **kennen** (RecipeKnowledgeLevel.KNOWN)
- Müssen eine Brauprobe durchführen
- Kennen die Qualität des gebrauten Tranks **nicht** (außer bei Misserfolg)

### Spielleiter
- Können Tränke direkt hinzufügen (wie bisher)
- Sehen alle Qualitätsstufen und Details

## Brau-Prozess

### 1. Voraussetzungen

**Rezeptwissen**: Der Spieler muss das Rezept kennen (KNOWN). Nur verstanden (UNDERSTOOD) reicht nicht aus.

**Talent**: Der Spieler muss entweder Alchimie oder Kochen (Tränke) beherrschen.

**Labor**: Der Spieler benötigt ein ausreichendes Labor:
- Archaisches Labor (Stufe 1)
- Hexenküche (Stufe 2)
- Alchimistenlabor (Stufe 3)

### 2. Labor-Modifikatoren

Die Brauprobe wird je nach verfügbarem Labor modifiziert:

| Rezept benötigt | Labor verfügbar | Modifikator | Brauen möglich? |
|----------------|-----------------|-------------|-----------------|
| Archaisch | Archaisch | 0 | Ja |
| Archaisch | Hexenküche | -3 | Ja |
| Archaisch | Alchimist | -6 | Ja |
| Hexenküche | Archaisch | +7 | Ja |
| Hexenküche | Hexenküche | 0 | Ja |
| Hexenküche | Alchimist | -3 | Ja |
| Alchimist | Archaisch | - | **Nein** |
| Alchimist | Hexenküche | +7 | Ja |
| Alchimist | Alchimist | 0 | Ja |

**Regel**: Ein Labor, das 2 Stufen schlechter ist als gefordert, reicht nicht aus.

### 3. Erschwernis-Modifikatoren

Die Brauprobe wird durch verschiedene Faktoren erschwert oder erleichtert:

#### Brauschwierigkeit
Jedes Rezept hat eine Brauschwierigkeit (0-18), die als Erschwernis auf die Probe wirkt.

#### Freiwillige Erschwernis
Der Spieler kann sich freiwillig erschweren:
- Minimum: 2 Punkte (1 Punkt ist nicht erlaubt)
- Maximum: Anderthalbfache Brauschwierigkeit (aufgerundet)
- Vorteil: Jeder Punkt freiwillige Erschwernis gibt **2 Qualitätspunkte** bei Erfolg

**Beispiel**: Brauschwierigkeit 6 → max. 9 Punkte freiwillige Erschwernis
- 2 Punkte → 4 QP
- 9 Punkte → 18 QP

#### Substitutionen
Der Spieler kann Zutaten substituieren:
- **Optimierende Substitution**: -3 (Erleichterung)
- **Gleichwertige Substitution**: 0
- **Sinnvolle Substitution**: +3 (Erschwernis)
- **Mögliche Substitution**: +6 (Erschwernis)

Mehrere Substitutionen werden addiert.

### 4. Astrale Aufladung

Charaktere mit Astralenergie können AsP einsetzen, um Qualitätspunkte zu erhalten:

| Qualitätspunkte | AsP-Kosten |
|----------------|-----------|
| 1 | 1 |
| 2 | 2 |
| 3 | 4 |
| 4 | 8 |
| 5 | 16 |
| 6 | 32 |

**Formel**: Für n Qualitätspunkte: 2^(n-1) AsP

### 5. Magisches Meisterhandwerk

Wenn ein Talent als Magisches Meisterhandwerk beherrscht wird, kann der Charakter bei der Brauprobe AsP einsetzen:
- Pro AsP: +2 auf TaW
- Maximum: 2x ursprünglicher TaW

**Beispiel**: Alchimie 10 mit Magischem Meisterhandwerk
- 3 AsP → TaW 16 (10 + 6, aber max. 20)
- 5 AsP → TaW 20 (10 + 10)

**Wichtig**: Dies ist NICHT die astrale Aufladung! Das sind zwei verschiedene Mechanismen:
- Magisches Meisterhandwerk erhöht den TaW der Probe
- Astrale Aufladung gibt direkt Qualitätspunkte

### 6. Die Brauprobe

1. **Talent-Auswahl**: Alchimie (MU/KL/FF) oder Kochen (Tränke) (KL/IN/FF)
2. **Modifikatoren berechnen**:
   - Labormodifikator
   - Brauschwierigkeit
   - Freiwillige Erschwernis
   - Substitutionen
3. **Probe durchführen**: Drei-Eigenschaften-Probe mit dem gewählten Talent
4. **Erfolg?**: 
   - Ja → Qualitätspunkte berechnen
   - Nein → Qualität M (Misslungen)

### 7. Qualitätspunkte-Berechnung

Bei erfolgreicher Probe:

1. **TaP* der Probe** (Talentpunkte übrig)
2. **+ Freiwillige Erschwernis × 2**
3. **+ Astrale Aufladung**
4. **+ 2W6** (gewürfelt)

**Summe = Gesamt-Qualitätspunkte**

### 8. Qualitätsstufen

Die Qualitätspunkte bestimmen die Qualitätsstufe des Tranks:

| Qualitätspunkte | Qualitätsstufe |
|----------------|----------------|
| 0-6 | A |
| 7-12 | B |
| 13-18 | C |
| 19-24 | D |
| 25-30 | E |
| 31+ | F |
| Misslungen | M |

**Wichtig**: Der Spieler sieht die Qualitätsstufe **nicht**! Nur der Spielleiter kennt sie.

## UI-Implementierung

### PotionScreen

Der FAB (Floating Action Button) ändert sich je nach Spieler/Spielleiter:
- **Spielleiter**: "Hinzufügen" (wie bisher)
- **Spieler**: "Brauen" (neuer Dialog)

### BrewPotionDialog

Der Brau-Dialog bietet folgende Optionen:

1. **Rezept**: Dropdown mit allen bekannten Rezepten
2. **Talent**: Auswahl zwischen Alchimie und Kochen (Tränke)
3. **Labor**: Auswahl des verfügbaren Labors
4. **Freiwillige Erschwernis**: +/- Buttons (0 oder min. 2)
5. **Substitutionen**: Liste mit Add/Remove-Buttons
6. **Astrale Aufladung**: +/- Buttons (falls AE verfügbar)
7. **Zusammenfassung**: Übersicht aller Modifikatoren

Nach dem Brauen wird das Ergebnis angezeigt:
- Würfelergebnisse
- TaP*
- Qualitätspunkte (Berechnung)
- **Qualität**: Nur für Spielleiter sichtbar!

## Backend-Architektur

### PotionBrewer (logic/)

Zentrale Klasse für die Brau-Logik:
- `brewPotion()`: Führt die komplette Brauprobe durch
- `calculateMaxVoluntaryHandicap()`: Berechnet maximale freiwillige Erschwernis
- `calculateAspCostForQualityPoints()`: Berechnet AsP-Kosten
- `calculateMaxQualityPointsFromAsp()`: Berechnet maximale QP aus AsP
- `canBrew()`: Prüft, ob Brauen möglich ist
- `formatBrewingResult()`: Formatiert Ergebnis für Anzeige

### DerianDateCalculator (logic/)

Utility-Klasse für derische Datumsberechnungen:
- `calculateExpiryDate()`: Berechnet Haltbarkeitsdatum (unterstützt Würfelnotationen)
- `rollDice()`: Würfelt Würfelnotationen (z.B. "3W6+2")
- `parseShelfLifeAmount()`: Parst Haltbarkeitsangaben (fix oder Würfel)
- `parseDateToDays()`: Konvertiert Datum zu Tageszahl
- `isExpired()`: Prüft, ob ein Datum abgelaufen ist
- `getWeekday()`: Berechnet den Wochentag
- `getMadaPhase()`: Berechnet die Mondphase

**Würfelnotations-Unterstützung**:
Die Haltbarkeit kann nun auch mit Würfelnotationen angegeben werden:
- Format: `<Anzahl>W<Würfelgröße>[+/-Modifikator] <Einheit>`
- Beispiele:
  - "3W6+2 Wochen" → Würfelt 3W6, addiert 2, Ergebnis in Wochen
  - "2W6-1 Tage" → Würfelt 2W6, subtrahiert 1, Ergebnis in Tagen
  - "1W6 Monde" → Würfelt 1W6, Ergebnis in Monden
  - "3 Monde" → Fixe Angabe (wie bisher)
- Unterstützte Einheiten: Tage, Wochen, Monde/Monate, Jahre


### Laboratory (model/potion/)

Enum mit drei Stufen:
- `ARCANE` (Level 1)
- `WITCHES_KITCHEN` (Level 2)
- `ALCHEMIST_LABORATORY` (Level 3)

Methoden:
- `getBrewingModifier()`: Berechnet Labor-Modifikator
- `canBrewWith()`: Prüft, ob Labor ausreichend ist

### SubstitutionType (model/potion/)

Enum für Substitutions-Arten:
- `OPTIMIZING` (-3)
- `EQUIVALENT` (0)
- `SENSIBLE` (+3)
- `POSSIBLE` (+6)

### PotionViewModel

Neue Methoden:
- `getKnownRecipes()`: Gibt bekannte Rezepte zurück
- `getAvailableBrewingTalents()`: Gibt verfügbare Talente zurück
- `canUseBrewingTalent()`: Prüft Talent-Verfügbarkeit
- `brewPotion()`: Braut einen Trank (mit allen Checks)
- `updateDefaultLaboratory()`: Aktualisiert Standard-Labor

### Character (model/character/)

Neue Felder:
- `defaultLaboratory: Laboratory?` - Standard-Labor des Charakters

## Datenbank-Migration

**Version 14 → 15**:
- Hinzufügen des `defaultLaboratory`-Feldes in der `characters`-Tabelle

## Tests

### PotionBrewerTest

Umfangreiche Unit-Tests für alle Berechnungen:
- Labormodifikatoren
- Freiwillige Erschwernis
- Substitutionen
- AsP-Kosten und Qualitätspunkte
- Qualitätsstufen
- Formatierung (Spieler vs. Spielleiter)

**Alle 87 Tests bestehen erfolgreich.**

## Beispiel-Workflow

### Spieler braut einen Heiltrank

1. **Vorbereitung**:
   - Charakter: Alchimie 12, AE 30/30
   - Rezept: "Heiltrank" (bekannt, Brauschwierigkeit +4, Alchimistenlabor)
   - Labor: Alchimistenlabor verfügbar

2. **Brau-Entscheidungen**:
   - Talent: Alchimie
   - Labor: Alchimistenlabor (Modifikator: 0)
   - Freiwillige Erschwernis: +3 (→ +6 QP bei Erfolg)
   - Substitutionen: 1x Optimierend (-3)
   - Astrale Aufladung: 3 QP (kostet 4 AsP)

3. **Gesamtmodifikator**:
   - Labor: 0
   - Brauschwierigkeit: +4
   - Freiwillig: +3
   - Substitution: -3
   - **Gesamt: +4**

4. **Brauprobe**:
   - Alchimie-Probe (MU/KL/FF) mit +4 Erschwernis
   - TaW 12, Probe: 8/11/6 (alle OK!)
   - TaP*: 8 (von 12-4=8 blieben 8 übrig)

5. **Qualitätspunkte**:
   - TaP*: 8
   - Freiwillig: 3 × 2 = 6
   - Astral: 3
   - 2W6: 4+5 = 9
   - **Gesamt: 26 QP → Qualität E**

6. **Ergebnis**:
   - Spieler sieht: "Brauprobe erfolgreich, Trank wurde erstellt"
   - Spielleiter sieht: "Qualität E"
   - Neuer Trank in der Liste (Name bekannt, Qualität unbekannt)

## Bekannte Einschränkungen

1. **UI-Tests**: Keine automatisierten UI-Tests implementiert (benötigen Android-Kontext)
2. **Datum/Haltbarkeit**: Aktuell wird das Standard-Verfallsdatum aus dem Rezept verwendet
3. **Labor-Auswahl**: Kein Dialog zum permanenten Ändern des Standard-Labors (nur im Brau-Dialog)

## Zukünftige Erweiterungen

1. **Labor-Verwaltung**: Eigener Dialog zum Einstellen des Standard-Labors
2. **Substitutions-Bibliothek**: Vordefinierte Substitutionen pro Rezept
3. **Brau-Historie**: Statistik über gebraute Tränke
4. **Rezept-Details**: Anzeige von Zutaten und Beschreibung
5. **Batch-Brauen**: Mehrere Tränke auf einmal brauen
