# Talent- und Zauber-System - Dokumentation

## Übersicht

Die Probe-Logik wurde zentralisiert und verbessert. Alle Talente und System-Zauber haben jetzt fest definierte Eigenschaftsproben, die zentral verwaltet werden.

## Neue Komponenten

### 1. Talent-Enum (`data/model/talent/Talent.kt`)

Definiert alle bekannten DSA-Talente mit ihren Eigenschaftsproben:

```kotlin
enum class Talent(
    val talentName: String,
    val attribute1: String,
    val attribute2: String,
    val attribute3: String
)
```

**Enthaltene Talente:**
- **Alchimie**: MU/KL/FF
- **Kochen (Tränke)**: KL/IN/FF
- **Magiekunde**: KL/KL/IN
- **Pflanzenkunde**: KL/IN/FF
- **Selbstbeherrschung**: MU/KO/KK
- **Sinnenschärfe**: KL/IN/IN

### 2. SystemSpell-Enum (`data/model/spell/SystemSpell.kt`)

Definiert System-Zauber für Alchimie-Analysen:

```kotlin
enum class SystemSpell(
    val spellName: String,
    val attribute1: String,
    val attribute2: String,
    val attribute3: String
)
```

**Enthaltene Zauber:**
- **ODEM ARCANUM**: KL/IN/IN (Intensitätsbestimmung)
- **ANALYS ARKANSTRUKTUR**: KL/KL/IN (Strukturanalyse)

### 3. Erweiterte ProbeChecker-Funktionen

Der `ProbeChecker` wurde um mehrere Hilfsfunktionen erweitert:

#### performTalentProbe
```kotlin
fun performTalentProbe(
    talent: Talent,
    character: Character,
    talentwert: Int,
    difficulty: Int = 0,
    diceRoll: () -> Int = { rollD20() }
): ProbeResult
```

Führt eine Talentprobe durch. Die Eigenschaften werden automatisch aus dem Talent-Enum geholt.

**Beispiel:**
```kotlin
val result = ProbeChecker.performTalentProbe(
    talent = Talent.ALCHEMY,
    character = character,
    talentwert = character.alchemySkill,
    difficulty = 2
)
```

#### performSpellProbe
```kotlin
fun performSpellProbe(
    spell: Spell,
    character: Character,
    zauberfertigkeit: Int,
    difficulty: Int = 0,
    diceRoll: () -> Int = { rollD20() }
): ProbeResult
```

Führt eine Zauberprobe durch. Die Eigenschaften werden aus der Spell-Datenbank geholt.

#### performSystemSpellProbe
```kotlin
fun performSystemSpellProbe(
    systemSpell: SystemSpell,
    character: Character,
    zauberfertigkeit: Int,
    difficulty: Int = 0,
    diceRoll: () -> Int = { rollD20() }
): ProbeResult
```

Führt eine System-Zauberprobe durch (ODEM, ANALYS).

## Korrigierte Proben

### Vorher (falsch)
- **ODEM ARCANUM**: KL/IN/CH ❌
- **ANALYS ARKANSTRUKTUR**: KL/IN/CH ❌
- **Alchimie**: KL/IN/IN ❌

### Nachher (korrekt)
- **ODEM ARCANUM**: KL/IN/IN ✅
- **ANALYS ARKANSTRUKTUR**: KL/KL/IN ✅
- **Alchimie**: MU/KL/FF ✅

## Verwendung in ElixirAnalyzer

### Intensitätsbestimmung (ODEM)
```kotlin
val probeResult = ProbeChecker.performSystemSpellProbe(
    systemSpell = SystemSpell.ODEM,
    character = character,
    zauberfertigkeit = character.odemZfw,
    difficulty = recipe.analysisDifficulty
)
```

### Strukturanalyse mit ANALYS
```kotlin
val probeResult = ProbeChecker.performSystemSpellProbe(
    systemSpell = SystemSpell.ANALYS,
    character = character,
    zauberfertigkeit = character.analysZfw,
    difficulty = difficulty
)
```

### Strukturanalyse mit Alchimie
```kotlin
val probeResult = ProbeChecker.performTalentProbe(
    talent = Talent.ALCHEMY,
    character = character,
    talentwert = character.alchemySkill,
    difficulty = difficulty
)
```

### Selbstbeherrschungsprobe
```kotlin
val probeResult = ProbeChecker.performTalentProbe(
    talent = Talent.SELF_CONTROL,
    character = character,
    talentwert = character.selfControlSkill,
    difficulty = probeNumber - 1
)
```

## Vorteile

1. **Zentrale Verwaltung**: Alle Eigenschaftsproben sind an einer Stelle definiert
2. **Typsicherheit**: Enums verhindern Tippfehler bei Talentnamen
3. **Konsistenz**: Alle Proben verwenden die gleichen Funktionen
4. **Wartbarkeit**: Änderungen an Eigenschaften müssen nur an einer Stelle gemacht werden
5. **Erweiterbarkeit**: Neue Talente/Zauber können einfach hinzugefügt werden

## Zukünftige Erweiterungen

Das System kann leicht erweitert werden:

1. **Weitere Talente**: Einfach zum `Talent`-Enum hinzufügen
2. **Weitere System-Zauber**: Einfach zum `SystemSpell`-Enum hinzufügen
3. **Kampftalente**: Neues Enum `CombatTalent` nach gleichem Muster
4. **Liturgien**: Neues Enum `Liturgy` für geweihte Charaktere

## Migration

### Alt (manuelle Eigenschaftsangabe)
```kotlin
val probeResult = ProbeChecker.performThreeAttributeProbe(
    fertigkeitswert = character.alchemySkill,
    difficulty = difficulty,
    attribute1 = character.mu,  // Manuell!
    attribute2 = character.kl,  // Manuell!
    attribute3 = character.ff,  // Manuell!
    qualityPointName = "TaP*"
)
```

### Neu (automatische Eigenschaftsauswahl)
```kotlin
val probeResult = ProbeChecker.performTalentProbe(
    talent = Talent.ALCHEMY,     // Eigenschaften werden automatisch geholt
    character = character,
    talentwert = character.alchemySkill,
    difficulty = difficulty
)
```

## Testing

Die neuen Funktionen können wie gewohnt getestet werden:

```kotlin
@Test
fun testAlchemyProbe() {
    val character = Character(
        mu = 12, kl = 13, ff = 11,
        alchemySkill = 8
    )
    
    val result = ProbeChecker.performTalentProbe(
        talent = Talent.ALCHEMY,
        character = character,
        talentwert = character.alchemySkill,
        difficulty = 0,
        diceRoll = { 10 } // Mockwert für Tests
    )
    
    assertTrue(result.success)
}
```

## Weitere Informationen

Siehe auch:
- **[README.md](README.md)** - Projekt-Übersicht
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Gesamtübersicht der Implementierung
- **[PROBECHECKER_DOCUMENTATION.md](PROBECHECKER_DOCUMENTATION.md)** - Zentrale Proben-Logik
- **[POTION_BREWING_DOCUMENTATION.md](POTION_BREWING_DOCUMENTATION.md)** - Trank-Brauen mit Talentproben
