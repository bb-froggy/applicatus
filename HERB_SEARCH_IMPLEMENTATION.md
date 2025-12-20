# Kräutersuche - Implementierungsdetails

## Überblick

Die Kräutersuche in Applicatus basiert auf den Regeln aus "Zoo-Botanica Aventurica" (DSA 4.1) und implementiert eine umfassende Datenbank mit über 100 Kräutern und Pflanzen.

## Sucharten

### Gezielte Suche

Der Spieler sucht nach einem **spezifischen Kraut**:
- Probe gegen Suchschwierigkeit (Erkennung + Häufigkeit - Boni)
- Bei Erfolg: 1+ Portionen des gesuchten Krauts
- Zusätzliche Portionen kosten halbe Suchschwierigkeit an TaP*

**Beispiel**: Suche nach Alveranie in einer Lichtung
- Erkennung +5, Häufigkeit +6 → Erschwernis +11
- Bei Ortskenntnis: +11-7 = +4
- Bei TaP* 15: 1 + (14 ÷ 6) = 3 Portionen

### Allgemeine/Ungezielte Suche

✅ **Implementiert**: Der Spieler sucht **irgendwelche verwertbaren Pflanzen**:
- Probe ohne spezifische Erschwernis (nur Boni: Geländekunde +3, Ortskenntnis +7)
- Iterative Zufallsfunde: TaP* werden schrittweise verbraucht
- Pro Fund: Zufällige Pflanze, deren **halbe** Suchschwierigkeit ≤ verbleibende TaP*
- Nach jedem Fund: TaP* -= halbe Suchschwierigkeit (mindestens 1)
- Suche endet, wenn keine findbare Pflanze mehr übrig ist

**Ablauf:**
1. Talentprobe mit TaW (ohne Erschwernis, aber mit Boni)
2. Wiederhole bis TaP* aufgebraucht:
   - Filtere Pflanzen: halbe Suchschwierigkeit ≤ TaP*
   - Falls keine → Ende
   - Wähle zufällige Pflanze aus Liste
   - Berechne Portionen: 1 + (TaP*-1) ÷ (halbe Suchschwierigkeit)
   - Würfel Mengen (mit TaP* für Bedingungen)
   - Reduziere TaP* um halbe Suchschwierigkeit

**Beispiel**: Allgemeine Suche im Wald mit TaP* 20
1. Fund: Alveranie (Schwierigkeit 11, halbe=6) → 1+(19÷6)=4 Portionen, TaP* → 14
2. Fund: Basiliskenzunge (Schwierigkeit 8, halbe=4) → 1+(13÷4)=4 Portionen, TaP* → 10
3. Fund: Finage (Schwierigkeit 4, halbe=2) → 1+(9÷2)=5 Portionen, TaP* → 8
4. Fund: ... usw. bis TaP* < kleinste halbe Suchschwierigkeit

**UI**: Im Kräuter-Dropdown gibt es die Option "Allgemeine Suche" (oben, hervorgehoben)

### Monatsabhängige Produkte

Einige Pflanzen liefern unterschiedliche Produkte je nach Erntemonat. Diese sind als separate Datenbankeinträge mit identischem Namen implementiert:

**Beispiele:**

- **Satuariensbusch**:
  - Ingerimm/Rahja: Blätter + Blüten + Saft
  - Praios/Namenlose Tage: Blätter + Saft  
  - Phex/Peraine: nur Saft
  - Efferd/Travia: nur Früchte

- **Finage**:
  - Peraine: Triebe
  - Boron/Hesinde/Firun: Bast

- **Nothilf**:
  - Praios: Blüten
  - Peraine: Blätter

- **Grauer Mohn**:
  - Rondra: Samenkapsel + Blüte
  - Andere Monate: nur Blüte

- **Schwarzer Mohn**

### Schwierigkeitsänderungen

**Roter Drachenschlund**:
- Normal: Bestimmungsschwierigkeit +10
- Blühend (Ingerimm/Rahja): Bestimmungsschwierigkeit +3

Implementiert als zwei separate Einträge: "Roter Drachenschlund" und "Roter Drachenschlund (blühend)"

### Regionsabhängige Erntezeiten

**Naftanstaude**:
- Nordaventurien: Nur Ingerimm bis Rondra (Sommer), Steppe/Grasslands/Forest Edge
- Südaventurien (Winter): Nur Boron/Hesinde/Firun, Küste
- Südaventurien (Sommer): Die meisten Monate, Steppe/Grasslands/Forest Edge

Implementiert als drei separate Einträge mit unterschiedlichen Landschaftsverteilungen und Erntezeiten.

**Vragieswurzel**:
- Regenwald: Efferd, Travia, Boron
- Andere Regionen: nur Efferd, Travia

Implementiert als zwei separate Einträge mit unterschiedlichen Landschaftsverteilungen.

**Sansaro**:
- Winter (Boron/Hesinde/Firun): nur Küste selten
- Andere Monate: Küste häufig + Meer selten

Implementiert als zwei separate Einträge mit unterschiedlichen Landschaftsverteilungen und Erntezeiten.

## Erweiterte Features

### TaP*-abhängige Produkte

✅ **Implementiert**: Das System unterstützt bedingte Produkte basierend auf TaP* (Talentwert-Punkten).

**Syntax**: `IF TaP*<operator><wert>: <produkt>`

**Unterstützte Operatoren**: `>=`, `<=`, `>`, `<`, `=`, `==`

**Implementierte Beispiele:**

- **Schwarzer Wein**: `IF TaP*>=7: 7W6 Beeren`
- **Tuur-Amash-Kelch**: `W6+3 Kelche; IF TaP*>=13: eine Beere`
- **Wasserrausch** (3 Einträge): `IF TaP*>=12: eine Frucht`

**Technische Details:**
- Bedingungen werden in `BaseQuantityParser.parse()` ausgewertet
- Unterstützt Kombinationen: `W6 Blätter; IF TaP*>=7: W20 Blüten`
- Rekursives Parsing für komplexe Ausdrücke
- TaP*-Wert wird durch die gesamte Verarbeitungskette durchgereicht

## Bekannte Einschränkungen

### Monat-abhängige Verbreitung

Einige Pflanzen ändern ihre Landschaftsverteilung je nach Monat:

**Nicht implementiert:**

- **Feuerschlick**: 
  - Rondra/Efferd + Vollmond: Küste sehr häufig
  - Sonst: Küste häufig + Meer sehr selten

**Grund**: Das Datenmodell `HerbDistribution` unterstützt keine monatsabhängigen Verteilungen. Die Landschaften sind statisch pro Pflanzeneintrag.

### Regionsabhängige Erntezeiten (ohne Landschaftszuordnung)

**Derzeit keine bekannten Fälle mehr.**

Die zuvor problematische Naftanstaude wurde durch Aufteilung in drei Einträge korrekt implementiert.

### Spezielle Suchbedingungen

Nicht implementiert:

- **Vollmond-Abhängigkeit**: Feuerschlick, Madablüte
- **Ruinen-Bonus**: Efeuer (Bestimmung erleichtert bei Ruinen)
- **Höhlen-Unterschiede**: Phosphorpilz (unterschiedliche Schwierigkeit in feuchten vs. trockenen Höhlen)

## Datenstruktur

```kotlin
data class Herb(
    val name: String,
    val identificationDifficulty: Int,
    val baseQuantity: String,  // Würfelnotation: "3W6 Blätter; W20 Blüten"
    val distributions: List<HerbDistribution>,
    val harvestMonths: List<DerianMonth>,
    val pageReference: Int,
    val dangers: String? = null
)

data class HerbDistribution(
    val landscape: Landscape,
    val occurrence: Occurrence  // VERY_COMMON, COMMON, OCCASIONAL, RARE, VERY_RARE
)
```

## Würfelnotation

Das System unterstützt DSA-Würfelnotationen inklusive Kurzformen:

- `"3W6"` → 3 sechsseitige Würfel
- `"W20"` → Kurzform für "1W20"
- `"W3"` → Kurzform für "1W3"
- `"2W6+5"` → 2W6 plus 5
- `"4W20 Blätter; W20 Blüten; W3 Flux Saft"` → Mehrere Produkte mit Semikolon getrennt

## Zukünftige Erweiterungen

Mögliche Verbesserungen:

1. ~~**TaP*-basierte Boni**: System erweitern um bedingte Produkte~~ ✅ Implementiert
2. **Monat-abhängige Verteilung**: Datenmodell um zeitliche Dimension erweitern (z.B. Feuerschlick)
3. **Regionen-System**: Geografische Regionen zusätzlich zu Landschaftstypen
4. **Spezielle Bedingungen**: Vollmond, Ruinen, Höhlentyp als Suchparameter
5. **Gefahren-Proben**: Automatische Proben für gefährliche Pflanzen
6. ~~**Allgemeine/Ungezielte Suche**: Zufällige Pflanzenfunde basierend auf TaP*~~ ✅ Implementiert

## Quellen

- Zoo-Botanica Aventurica (ZBA)
- Wege der Alchimie (WdA)
- DSA 4.1 Regelwerk

## Siehe auch

- [IMPLEMENTATION.md](IMPLEMENTATION.md) - Allgemeine Implementierungsdetails
- [InitialHerbs.kt](app/src/main/java/de/applicatus/app/data/InitialHerbs.kt) - Pflanzendatenbank
- [HerbSearchCalculator.kt](app/src/main/java/de/applicatus/app/logic/HerbSearchCalculator.kt) - Suchlogik
- [BaseQuantityParser.kt](app/src/main/java/de/applicatus/app/logic/BaseQuantityParser.kt) - Würfelnotation-Parser
