# Character Journal System - Integration Guide

## Übersicht

Das Character Journal System protokolliert automatisch alle wichtigen Ereignisse eines Charakters mit:
- **Irdischem Zeitstempel** (Unix milliseconds für präzise Sortierung)
- **Derischem Datum** (automatisch aus Group.currentDerianDate)
- **Kategorie** (für Icons, Farben und Filterung)
- **Spieler-Nachricht** (immer sichtbar)
- **Spielleiter-Nachricht** (nur im GM-Modus sichtbar)

## Datenbank

### Entity: CharacterJournalEntry
```kotlin
@Entity(tableName = "character_journal_entries")
data class CharacterJournalEntry(
    val id: Long = 0,
    val characterId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val derianDate: String,
    val category: String,
    val playerMessage: String,
    val gmMessage: String? = null
)
```

### Kategorien (JournalCategory)
Vordefinierte Konstanten für konsistente Kategorisierung:
- `Potion.Brewed`, `Potion.Consumed`, `Potion.Analysis.*`
- `Spell.Cast`, `Spell.Stored`, `Spell.Cleared`
- `Energy.Regeneration`, `Energy.LE.Changed`, etc.
- `Inventory.Item.Acquired`, `Inventory.Item.Moved`
- `Recipe.Learned`, `Talent.Improved`, `Combat.*`

## Integration in ViewModels

### Beispiel: Trank brauen (PotionViewModel)

```kotlin
suspend fun brewPotion(
    recipe: Recipe,
    talent: Talent,
    // ... weitere Parameter
) {
    val char = character.value ?: return
    
    // Brauprobe durchführen
    val result = PotionBrewer.brewPotion(...)
    
    // Trank erstellen
    val potion = Potion(...)
    repository.insertPotion(potion)
    
    // Journal-Eintrag erstellen
    repository.logCharacterEvent(
        characterId = char.id,
        category = JournalCategory.POTION_BREWED,
        playerMessage = "${recipe.name} gebraut",
        gmMessage = "Qualität: ${result.quality.displayName}, " +
                   "QP: ${result.qualityPoints}, " +
                   "haltbar bis ${potion.expiryDate}"
    )
    
    // AsP-Verbrauch ebenfalls loggen (optional)
    if (aspCost > 0) {
        repository.logCharacterEvent(
            characterId = char.id,
            category = JournalCategory.ENERGY_AE_CHANGED,
            playerMessage = "$aspCost AsP verbraucht",
            gmMessage = "Magisches Meisterhandwerk beim Brauen"
        )
    }
}
```

### Beispiel: Zauber wirken (SpellStorageViewModel)

```kotlin
fun castSpell(slot: SpellSlot, spell: Spell) {
    viewModelScope.launch {
        val char = character.value ?: return@launch
        
        // Zauberprobe durchführen
        val result = SpellChecker.performApplicatusCheck(...)
        
        // Slot aktualisieren
        repository.updateSlot(slot.copy(isFilled = false))
        
        // Journal-Eintrag
        repository.logCharacterEvent(
            characterId = char.id,
            category = JournalCategory.SPELL_CAST,
            playerMessage = "${spell.name} gewirkt",
            gmMessage = if (result.overallSuccess) {
                "ZfP*: ${result.totalZfp}, AsP: ${result.totalAspCost}"
            } else {
                "Fehlgeschlagen, AsP: ${result.totalAspCost}"
            }
        )
    }
}
```

### Beispiel: Regeneration (CharacterHomeViewModel)

```kotlin
fun regenerateEnergy() {
    viewModelScope.launch {
        val char = character.value ?: return@launch
        
        // Regeneration berechnen
        val leRegen = RegenerationCalculator.calculateLeRegeneration(...)
        val aeRegen = RegenerationCalculator.calculateAeRegeneration(...)
        
        // Charakter aktualisieren
        val updated = char.copy(
            currentLe = (char.currentLe + leRegen).coerceAtMost(char.maxLe),
            currentAe = (char.currentAe + aeRegen).coerceAtMost(char.maxAe)
        )
        repository.updateCharacter(updated)
        
        // Journal-Eintrag
        val parts = mutableListOf<String>()
        if (leRegen > 0) parts.add("+$leRegen LE")
        if (aeRegen > 0) parts.add("+$aeRegen AE")
        
        if (parts.isNotEmpty()) {
            repository.logCharacterEvent(
                characterId = char.id,
                category = JournalCategory.ENERGY_REGENERATION,
                playerMessage = "Regeneriert: ${parts.joinToString(", ")}",
                gmMessage = "Automatische Regeneration (8 Stunden Schlaf)"
            )
        }
    }
}
```

## UI Integration

### Navigation hinzufügen

In `NavGraph.kt`:
```kotlin
composable("character/{characterId}/journal") { backStackEntry ->
    val characterId = backStackEntry.arguments?.getString("characterId")?.toLongOrNull()
    if (characterId != null) {
        CharacterJournalScreen(
            characterId = characterId,
            viewModelFactory = CharacterJournalViewModelFactory(
                repository = repository,
                characterId = characterId
            ),
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

### Menü-Button in CharacterHomeScreen

```kotlin
IconButton(onClick = { 
    navController.navigate("character/${characterId}/journal") 
}) {
    Icon(Icons.Default.MenuBook, "Journal")
}
```

## Export/Import

### Export erweitern (CharacterExporter)

```kotlin
data class CharacterExportDto(
    // ... existierende Felder
    val journalEntries: List<JournalEntryDto> = emptyList()
)

data class JournalEntryDto(
    val timestamp: Long,
    val derianDate: String,
    val category: String,
    val playerMessage: String,
    val gmMessage: String?
)

// In exportCharacter()
val journalEntries = repository.getJournalEntriesOnce(characterId)
    .map { JournalEntryDto(...) }

return CharacterExportDto(
    // ... andere Felder
    journalEntries = journalEntries
)
```

### Import erweitern

```kotlin
// In importCharacter()
snapshot.journalEntries.forEach { entryDto ->
    val entry = CharacterJournalEntry(
        characterId = newCharacterId,
        timestamp = entryDto.timestamp,
        derianDate = entryDto.derianDate,
        category = entryDto.category,
        playerMessage = entryDto.playerMessage,
        gmMessage = entryDto.gmMessage
    )
    repository.insertJournalEntry(entry)
}
```

## Best Practices

### 1. Wann loggen?
- ✅ Bei allen spielrelevanten Ereignissen (Zauber, Tränke, Kämpfe)
- ✅ Bei Energie-Änderungen (Regeneration, Schaden, Heilung)
- ✅ Bei wichtigen Entdeckungen (Rezepte lernen, Analyse-Erfolge)
- ❌ NICHT bei UI-Aktionen (Ansicht wechseln, Sortieren)
- ❌ NICHT bei temporären Zuständen (Dialoge öffnen/schließen)

### 2. Nachrichtenformatierung

**Spieler-Nachricht**: Kurz und prägnant
```kotlin
playerMessage = "Heiltrank gebraut"
playerMessage = "IGNIFAXIUS gewirkt"
playerMessage = "Regeneriert: +8 LE, +12 AE"
```

**GM-Nachricht**: Detaillierte Spielinformationen
```kotlin
gmMessage = "Qualität: C (5 QP), haltbar bis 15. Praios 1041 BF"
gmMessage = "ZfP*: 7, AsP-Kosten: 14 (inkl. 8 AsP Magisches Meisterhandwerk)"
gmMessage = "Proben: 13/8/15 auf MU/KL/FF"
```

### 3. Kategorien konsistent verwenden

```kotlin
// RICHTIG - Konstante verwenden
category = JournalCategory.POTION_BREWED

// FALSCH - Magic String
category = "Potion.Brewed"  // Tippfehler-anfällig!
```

### 4. Fehlerbehandlung

```kotlin
try {
    repository.logCharacterEvent(...)
} catch (e: Exception) {
    // Journal-Fehler sollten den Hauptfluss nicht unterbrechen
    Log.e("Journal", "Failed to log event", e)
}
```

## UI-Komponenten

### Icons und Farben

Das System ordnet automatisch Icons und Farben basierend auf der Kategorie zu:
- Tränke (Potion.*): Reagenzglas-Icon, Tertiär-Farbe
- Zauber (Spell.*): Stern-Icon, Primär-Farbe  
- Energie (Energy.*): Herz-Icon, Error-Farbe (Rot)
- Inventar (Inventory.*): Rucksack-Icon, OnSurfaceVariant

### Filtering (zukünftig)

```kotlin
// Alle Trank-Ereignisse
val potionEvents = viewModel.getEntriesByCategoryPattern("Potion.%")

// Nur Brau-Ereignisse
val brewEvents = viewModel.getEntriesByCategory(JournalCategory.POTION_BREWED)
```

## Datenbankversion

- **Aktuelle Version**: 34
- **Migration**: MIGRATION_33_34
- **Tabelle**: `character_journal_entries`
- **Indices**: `characterId`, `timestamp`
- **Foreign Key**: CASCADE DELETE bei Character-Löschung

## Testing

```kotlin
@Test
fun testJournalEntry() = runTest {
    // Charakter erstellen
    val charId = repository.insertCharacter(testCharacter)
    
    // Event loggen
    repository.logCharacterEvent(
        characterId = charId,
        category = JournalCategory.POTION_BREWED,
        playerMessage = "Test-Trank gebraut",
        gmMessage = "Qualität: A"
    )
    
    // Verifizieren
    val entries = repository.getJournalEntriesOnce(charId)
    assertEquals(1, entries.size)
    assertEquals("Test-Trank gebraut", entries[0].playerMessage)
}
```

## Nächste Schritte

1. ✅ Datenmodell und DAO implementiert
2. ✅ Repository-Methoden implementiert  
3. ✅ ViewModel und Screen erstellt
4. ⏳ Navigation integrieren
5. ⏳ Journal-Logging in existierende ViewModels integrieren
6. ⏳ Export/Import erweitern
7. ⏳ UI-Tests erstellen

## Weitere Informationen

Siehe auch:
- **[README.md](README.md)** - Projekt-Übersicht
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Gesamtübersicht der Implementierung
- **[POTION_BREWING_DOCUMENTATION.md](POTION_BREWING_DOCUMENTATION.md)** - Trank-Brauen (Journal-Einträge)
- **[EXPORT_IMPORT_GUIDE.md](EXPORT_IMPORT_GUIDE.md)** - Export/Import (Journal wird exportiert)
