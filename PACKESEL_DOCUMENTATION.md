# Packesel-Feature - Inventarverwaltung

## Übersicht

Das neue **Packesel-Feature** ermöglicht die Verwaltung des Inventars eines Charakters. Gegenstände können an verschiedenen Orten verstaut werden, und das Gesamtgewicht pro Ort wird automatisch berechnet.

## Features

### 1. Lagerorte (Locations)
- **Standard-Orte**: Jeder Charakter hat automatisch zwei Standard-Orte:
  - "Rüstung/Kleidung"
  - "Rucksack"
- **Eigene Orte**: Beliebig viele zusätzliche Orte können hinzugefügt werden (z.B. "Pferd", "Wagen", etc.)
- **Gesamtgewicht**: Für jeden Ort wird das Gesamtgewicht aller Gegenstände angezeigt

### 2. Gegenstände (Items)
- **Name**: Freitextfeld für den Namen des Gegenstands
- **Gewicht**: Eingabe in Stein und/oder Unzen
  - 1 Stein = 40 Unzen
  - Automatische Umrechnung und Anzeige
- **Ort**: Zuordnung zu einem Lagerort
- **Bearbeitung**: Gegenstände können jederzeit bearbeitet werden
  - Name ändern
  - Gewicht anpassen
  - Ort wechseln
- **Drag-and-Drop**: Gegenstände können per Ziehen zwischen Orten verschoben werden
  - **Drag-Handle**: Gepunktete Fläche auf der linken Seite jedes Gegenstands
  - Tippen und halten auf dem Handle, dann zu einem anderen Ort ziehen
  - Ziel-Ort wird mit blauem Rand markiert
  - Beim Loslassen wechselt der Gegenstand den Ort

### 3. Integration mit Tränken
- **Tränke als Gegenstände**: Alle Tränke aus der Hexenküche werden automatisch im Inventar angezeigt
- **Automatisches Gewicht**: Jeder Trank wiegt 4 Unzen
- **Gemeinsame Verwaltung**: Tränke können zwischen Orten verschoben werden
- **Kennzeichnung**: Tränke sind durch ein Icon gekennzeichnet und können nur in der Hexenküche bearbeitet werden

## Datenmodell

### Weight (Gewicht)
```kotlin
data class Weight(
    val stone: Int = 0,
    val ounces: Int = 0
)
```
- Umrechnung in Unzen: `toOunces()`
- Formatierte Anzeige: `toDisplayString()`
- Konstanten: `Weight.ZERO`, `Weight.POTION` (4 Unzen)

### Location (Lagerort)
```kotlin
@Entity
data class Location(
    val id: Long,
    val characterId: Long,
    val name: String,
    val isDefault: Boolean,
    val sortOrder: Int
)
```
- `isDefault`: Standard-Orte können nicht gelöscht werden

### Item (Gegenstand)
```kotlin
@Entity
data class Item(
    val id: Long,
    val characterId: Long,
    val locationId: Long?,
    val name: String,
    val weight: Weight,
    val sortOrder: Int
)
```
- `locationId`: Kann null sein (kein Ort zugeordnet)

### Potion (Trank) - Erweiterung
```kotlin
@Entity
data class Potion(
    // ... bestehende Felder ...
    val locationId: Long? = null  // NEU
)
```

## Datenbank-Migration

**Version 17 → 18**:
- Neue Tabelle `locations` mit Standard-Orten für alle bestehenden Charaktere
- Neue Tabelle `items`
- `potions.locationId` hinzugefügt
- Alle bestehenden Tränke werden automatisch in "Rucksack" gelegt

## UI-Komponenten

### InventoryScreen
- **Header**: Charaktername mit "Packesel"
- **Locations-Cards**: Jeder Ort als eigene Card mit:
  - Ortsname
  - Gesamtgewicht
  - Liste aller Gegenstände
  - Buttons zum Hinzufügen/Löschen
- **FAB**: Neuen Gegenstand hinzufügen

### Dialoge
1. **AddLocationDialog**: Neuen Ort hinzufügen
2. **AddItemDialog**: Neuen Gegenstand hinzufügen
   - Name
   - Gewicht (Stein/Unzen)
   - Ort-Auswahl (Dropdown)
3. **EditItemDialog**: Gegenstand bearbeiten
   - Gleiche Felder wie AddItemDialog
   - Ort kann geändert werden

## ViewModel

### InventoryViewModel
- **Locations**: Flow aller Orte des Charakters
- **Items**: Flow aller Gegenstände mit Location-Namen
- **Potions**: Flow aller Tränke
- **itemsByLocation**: Gruppiert Items und Tränke nach Orten
- **weightByLocation**: Berechnet Gesamtgewicht pro Ort

**Methoden**:
- `addLocation(name: String)`
- `addItem(name: String, weight: Weight, locationId: Long?)`
- `updateItem(item: Item)`
- `deleteItem(item: Item)`
- `moveItemToLocation(itemId: Long, newLocationId: Long?)`
- `movePotionToLocation(potionId: Long, newLocationId: Long?)`

## Navigation

**Route**: `inventory/{characterId}`

**Von CharacterHomeScreen**:
- Neuer Button "Packesel" neben "Zauberspeicher" und "Hexenküche"
- Führt zu `InventoryScreen`

## Verwendung

1. **Ort hinzufügen**: 
   - App-Bar-Button (Plus-Icon)
   - Namen eingeben
   
2. **Gegenstand hinzufügen**:
   - FAB (Floating Action Button) oder
   - Plus-Button auf einer Location-Card
   - Name, Gewicht (Stein/Unzen) und Ort eingeben
   
3. **Gegenstand bearbeiten**:
   - Edit-Icon bei jedem Gegenstand
   - Name, Gewicht oder Ort ändern
   
4. **Gegenstand verschieben (Drag-and-Drop)**:
   - **Drag-Handle** auf der linken Seite des Gegenstands tippen und halten
   - Finger zu einem anderen Ort bewegen
   - Ziel-Ort wird mit blauem Rand markiert
   - Finger loslassen → Gegenstand wechselt den Ort
   
5. **Gegenstand löschen**:
   - Delete-Icon bei jedem Gegenstand
   - Bestätigung nicht erforderlich
   
6. **Ort löschen**:
   - Delete-Icon auf Location-Card
   - Nur für eigene Orte (nicht "Am Körper" oder "Rucksack")
   - Gegenstände des gelöschten Orts werden zu "Ohne Ort"

## Gewichtsberechnung

Das Gesamtgewicht wird automatisch berechnet:
- Items: Individuelles Gewicht aus Stein/Unzen
- Tränke: Festes Gewicht von 4 Unzen
- Anzeige pro Ort in Stein und Unzen (z.B. "5 Stein 23 Unzen")

## Besonderheiten

- **Tränke**: Werden als virtuelle Items angezeigt (negative ID)
  - Können per Drag-and-Drop verschoben werden
  - Können nur in der Hexenküche bearbeitet/gelöscht werden
- **Standard-Orte**: "Am Körper" und "Rucksack" können nicht gelöscht werden
- **Ohne Ort**: Items ohne Ort-Zuordnung werden in einer separaten Card angezeigt
- **Persistenz**: Alle Änderungen werden sofort in der Datenbank gespeichert
- **Sortierung**: Orte nach `sortOrder`, Items innerhalb eines Orts alphabetisch
- **Drag-Handle**: Gepunktetes Icon (3x2 Punkte) auf der linken Seite jedes Items
- **Visuelles Feedback**: 
  - Gezogenes Item: hellblauer Hintergrund
  - Ziel-Ort: blauer Rand während des Drag-Vorgangs

## Zukünftige Erweiterungen (optional)

- [ ] Tragkraft-Berechnung basierend auf Körperkraft
- [ ] Warnung bei Überlastung
- [ ] Kategorien für Gegenstände (Waffe, Rüstung, etc.)
- [ ] Wert von Gegenständen (Preis)
- [ ] Export/Import von Inventar
- [ ] Drag & Drop zwischen Orten

## Weitere Informationen

Siehe auch:
- **[README.md](README.md)** - Projekt-Übersicht
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Gesamtübersicht der Implementierung
- **[POTION_BREWING_DOCUMENTATION.md](POTION_BREWING_DOCUMENTATION.md)** - Trank-Brauen (Tränke im Inventar)
- **[EXPORT_IMPORT_GUIDE.md](EXPORT_IMPORT_GUIDE.md)** - Export/Import (Inventar wird mit exportiert)
