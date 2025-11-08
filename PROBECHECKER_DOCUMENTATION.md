# ProbeChecker - Zentrale DSA-Proben-Logik

## Überblick

Die `ProbeChecker`-Klasse ist die zentrale Implementierung für alle DSA-Proben in der Applicatus-App. Sie wurde eingeführt, um Code-Duplikation zu vermeiden und eine konsistente Implementierung der DSA-Regeln zu gewährleisten.

## Motivation

Vor der Einführung von `ProbeChecker` war die Proben-Logik redundant in mehreren Klassen implementiert:
- `SpellChecker.kt` - Zauberproben
- `ElixirAnalyzer.kt` - Intensitäts- und Strukturanalysen
- `RegenerationCalculator.kt` - Attributsproben und Würfelwürfe

Dies führte zu:
- Code-Duplikation
- Inkonsistenzen zwischen verschiedenen Proben
- Erhöhtem Wartungsaufwand
- Fehleranfälligkeit bei Regeländerungen

## Funktionen

### 1. Drei-Eigenschafts-Proben

Die Hauptfunktion für Talent- und Zauberproben:

```kotlin
fun performThreeAttributeProbe(
    fertigkeitswert: Int,        // FW, TaW oder ZfW
    difficulty: Int,              // Erschwernis (positiv) oder Erleichterung (negativ)
    attribute1: Int,              // Erste Eigenschaft
    attribute2: Int,              // Zweite Eigenschaft
    attribute3: Int,              // Dritte Eigenschaft
    diceRoll: () -> Int = { rollD20() },  // Überschreibbar für Tests
    qualityPointName: String = "FP*"      // Name für Ausgabe (TaP*, ZfP*, etc.)
): ProbeResult
```

**DSA-Regelwerk-Implementierung:**
- 3x W20 würfeln
- Jeder Wurf wird gegen eine Eigenschaft geprüft
- Bei Überwurf: Differenz von FP abziehen
- Erfolg bei FP >= 0
- **Besondere Würfe:**
  - Doppel-1 / Dreifach-1: Automatischer Erfolg mit max FP
  - Doppel-20 / Dreifach-20: Automatischer Patzer
- **Erschwerte Proben:** Wenn FW <= Erschwernis, muss jede Eigenschaft um die Erschwernis unterwürfelt werden

**Rückgabe:**
```kotlin
data class ProbeResult(
    val success: Boolean,              // Erfolg oder Misserfolg
    val qualityPoints: Int,            // Übrige Qualitätspunkte
    val rolls: List<Int>,              // Die drei Würfe
    val message: String,               // Beschreibung
    val isDoubleOne: Boolean = false,
    val isTripleOne: Boolean = false,
    val isDoubleTwenty: Boolean = false,
    val isTripleTwenty: Boolean = false
)
```

### 2. Einfache Attributsproben

Für einzelne Eigenschaftsproben (z.B. KO-Probe bei Regeneration):

```kotlin
fun performAttributeProbe(
    attributeValue: Int,         // Eigenschaftswert
    difficulty: Int = 0,         // Erschwernis
    diceRoll: () -> Int = { rollD20() }
): AttributeProbeResult
```

**Rückgabe:**
```kotlin
data class AttributeProbeResult(
    val success: Boolean,
    val roll: Int,
    val attribute: Int
)
```

### 3. Würfelfunktionen

Zentrale Würfelfunktionen für Konsistenz:

```kotlin
fun rollD20(): Int              // Würfelt 1W20 (1-20)
fun rollD6(): Int               // Würfelt 1W6 (1-6)
fun rollMultipleD20(count: Int): List<Int>  // Mehrere W20
fun countOnes(rolls: List<Int>): Int        // Zählt Einsen
fun countTwenties(rolls: List<Int>): Int    // Zählt Zwanziger
```

## Verwendung in anderen Klassen

### SpellChecker

```kotlin
fun performSpellCheck(...): SpellCheckResult {
    val probeResult = ProbeChecker.performThreeAttributeProbe(
        fertigkeitswert = zfw,
        difficulty = modifier,
        attribute1 = attribute1,
        attribute2 = attribute2,
        attribute3 = attribute3,
        qualityPointName = "ZfP*"
    )
    
    // Konvertiere zu SpellCheckResult
    return SpellCheckResult(...)
}
```

### ElixirAnalyzer

```kotlin
fun determineIntensity(...): IntensityDeterminationResult {
    val probeResult = ProbeChecker.performThreeAttributeProbe(
        fertigkeitswert = character.odemZfw,
        difficulty = recipe.analysisDifficulty,
        attribute1 = character.kl,
        attribute2 = character.inValue,
        attribute3 = character.ch,
        qualityPointName = "ZfP*"
    )
    
    // Verarbeite Ergebnis für Intensitätsbestimmung
    ...
}
```

### PotionAnalyzer

```kotlin
fun analyzePotion(...): AnalysisResult {
    val probeResult = ProbeChecker.performThreeAttributeProbe(
        fertigkeitswert = taw,
        difficulty = difficulty,
        attribute1 = attributes.first,
        attribute2 = attributes.second,
        attribute3 = attributes.third,
        qualityPointName = "TaP*"
    )
    
    // Verarbeite Ergebnis für Tranksanalyse
    ...
}
```

### RegenerationCalculator

```kotlin
fun performRegeneration(...): RegenerationResult {
    // W6 für Regenerationswürfe
    val leDice = ProbeChecker.rollD6()
    
    // Attributsprobe für zusätzliche LE
    val koProbe = ProbeChecker.performAttributeProbe(character.ko)
    if (koProbe.success) {
        leGain += 1
    }
    
    ...
}
```

## Vorteile

1. **Einheitliche Implementierung**: Alle Proben folgen exakt denselben DSA-Regeln
2. **Wartbarkeit**: Regeländerungen müssen nur an einer Stelle vorgenommen werden
3. **Testbarkeit**: Zentrale Tests für die Proben-Logik möglich
4. **Überschreibbare Würfelwürfe**: Für Unit-Tests können deterministische Würfe injiziert werden
5. **Flexibilität**: Verschiedene Qualitätspunkt-Namen (TaP*, ZfP*, FP*) für unterschiedliche Kontexte
6. **Dokumentation**: Alle Proben-Regeln sind an einer Stelle dokumentiert

## Testing

Die überschreibbaren `diceRoll`-Parameter ermöglichen deterministische Tests:

```kotlin
// Test mit festen Würfelergebnissen
val testRolls = listOf(15, 12, 18).iterator()
val result = ProbeChecker.performThreeAttributeProbe(
    fertigkeitswert = 10,
    difficulty = 2,
    attribute1 = 14,
    attribute2 = 13,
    attribute3 = 15,
    diceRoll = { testRolls.next() }
)
```

## Best Practices

1. **Immer ProbeChecker verwenden**: Keine eigenen Würfel- oder Proben-Implementierungen mehr anlegen
2. **Konsistente Qualitätspunkt-Namen**: Verwende die etablierten Namen (TaP*, ZfP*, FP*)
3. **Proben-Ergebnisse überprüfen**: Nutze die Boolean-Flags für besondere Würfe
4. **Dokumentation**: Verweise in neuen Klassen auf ProbeChecker

## Weitere Informationen

Siehe auch:
- `app/src/main/java/de/applicatus/app/logic/ProbeChecker.kt` - Quellcode
- `IMPLEMENTATION.md` - Gesamtübersicht der Implementierung
- DSA 4.1 Regelwerk - Offizielle Regelreferenz
