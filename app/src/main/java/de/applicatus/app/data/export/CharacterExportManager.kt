package de.applicatus.app.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.KnownQualityLevel
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Mergt zwei Tränke mit derselben GUID und übernimmt das bessere Analyse-Ergebnis.
 */
internal fun mergePotion(existing: Potion, imported: Potion): Potion {
    // Vergleiche Analyse-Qualitätslevel (höher ist besser)
    val existingLevel = existing.knownQualityLevel.ordinal
    val importedLevel = imported.knownQualityLevel.ordinal
    
    return if (importedLevel > existingLevel) {
        // Importierter Trank hat besseres Analyse-Ergebnis
        imported.copy(id = existing.id, characterId = existing.characterId)
    } else if (importedLevel == existingLevel) {
        // Gleicher Level -> Nehme höhere Erleichterung
        if (imported.bestStructureAnalysisFacilitation > existing.bestStructureAnalysisFacilitation) {
            imported.copy(id = existing.id, characterId = existing.characterId)
        } else {
            existing
        }
    } else {
        // Existierender Trank ist besser
        existing
    }
}

/**
 * Manager für Export und Import von Charakteren als JSON.
 */
class CharacterExportManager(
    private val repository: ApplicatusRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // Wichtig für Abwärtskompatibilität
    }
    
    /**
     * Exportiert einen Charakter als JSON-String.
     */
    suspend fun exportCharacter(characterId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val character = repository.getCharacterById(characterId)
                ?: return@withContext Result.failure(Exception("Charakter nicht gefunden"))
            
            // Lade Gruppenname für Export (falls vorhanden)
            val groupName = character.groupId?.let { gId ->
                repository.getGroupByIdOnce(gId)?.name
            }
            
            // Sammle Inventar: Locations und Items
            // Verwende paginierte Abfrage um CursorWindow-Overflow bei großen Item-Mengen zu vermeiden
            val locations = repository.getLocationsForCharacter(characterId).first()
            val locationDtos = locations.map { location ->
                LocationDto.fromLocation(location)
            }
            val locationsById = locations.associateBy { it.id }
            
            // Lade Items paginiert (einmal für GUID-Auflösung und Export)
            val allItemsPaged = repository.getItemsForCharacterPaged(characterId)
            val itemsById = allItemsPaged.associateBy { it.id }
            
            val items = allItemsPaged.map { item ->
                val locationName = item.locationId?.let { locationsById[it]?.name }
                // Für SelfItems: Ermittle den Namen der zugehörigen Location
                val selfItemForLocationName = item.selfItemForLocationId?.let { locationsById[it]?.name }
                ItemDto(
                    guid = item.guid,
                    locationName = locationName,
                    name = item.name,
                    weightStone = item.weight.stone,
                    weightOunces = item.weight.ounces,
                    sortOrder = item.sortOrder,
                    isPurse = item.isPurse,
                    kreuzerAmount = item.kreuzerAmount,
                    isCountable = item.isCountable,
                    quantity = item.quantity,
                    isSelfItem = item.isSelfItem,
                    selfItemForLocationName = selfItemForLocationName
                )
            }
            
            // Sammle Slots und zugehörige Zauber
            val slotsWithSpells = repository.getSlotsWithSpellsByCharacter(characterId).first()
            
            val slots = slotsWithSpells.map { slotWithSpell ->
                val itemGuid = slotWithSpell.slot.itemId?.let { itemId ->
                    itemsById[itemId]?.guid
                }
                SpellSlotDto.fromSpellSlot(slotWithSpell.slot, slotWithSpell.spell?.name, itemGuid)
            }
            
            // Sammle Tränke und Rezepte
            val potionsWithRecipes = repository.getPotionsForCharacter(characterId).first()
            val potions = potionsWithRecipes.map { potionWithRecipe ->
                PotionDto.fromPotion(potionWithRecipe.potion, potionWithRecipe.recipe.name)
            }
            
            // Sammle Rezeptwissen
            val allRecipes = repository.allRecipes.first()
            val recipeNamesById = allRecipes.associateBy { it.id }
            val recipeKnowledge = repository.getRecipeKnowledgeForCharacter(characterId).first().map { knowledge ->
                RecipeKnowledgeDto.fromModel(knowledge, recipeNamesById[knowledge.recipeId]?.name)
            }
            
            // Sammle MagicSigns (seit v6)
            val magicSignsWithItems = repository.getMagicSignsWithItemsForCharacter(characterId).first()
            val magicSigns = magicSignsWithItems.mapNotNull { signWithItem ->
                // Nur exportieren, wenn Item gefunden wird
                val itemGuid = itemsById[signWithItem.magicSign.itemId]?.guid ?: return@mapNotNull null
                MagicSignDto.fromMagicSign(signWithItem.magicSign, itemGuid)
            }
            
            // Sammle Journal-Einträge
            val journalEntries = repository.getJournalEntriesOnce(characterId).map { entry ->
                JournalEntryDto.fromJournalEntry(entry)
            }
            
            val exportDto = CharacterExportDto(
                version = DataModelVersion.CURRENT_VERSION,
                character = CharacterDto.fromCharacter(character, groupName),
                spellSlots = slots,
                potions = potions,
                recipeKnowledge = recipeKnowledge,
                locations = locationDtos,
                items = items,
                journalEntries = journalEntries,
                magicSigns = magicSigns,
                exportTimestamp = System.currentTimeMillis()  // Explizit setzen
            )
            
            val jsonString = json.encodeToString(exportDto)
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Speichert einen Charakter als JSON-Datei.
     */
    suspend fun saveCharacterToFile(
        context: Context,
        characterId: Long,
        uri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonResult = exportCharacter(characterId)
            if (jsonResult.isFailure) {
                return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Export fehlgeschlagen"))
            }
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonResult.getOrNull()?.toByteArray())
            } ?: return@withContext Result.failure(IOException("Konnte Datei nicht öffnen"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Erstellt ein Share-Intent für einen Charakter, ohne die Datei im Dateisystem zu speichern.
     * Nutzt einen temporären Cache-File und FileProvider.
     */
    suspend fun createShareIntent(
        context: Context,
        characterId: Long
    ): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val character = repository.getCharacterById(characterId)
                ?: return@withContext Result.failure(Exception("Charakter nicht gefunden"))
            
            val jsonResult = exportCharacter(characterId)
            if (jsonResult.isFailure) {
                return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Export fehlgeschlagen"))
            }
            
            // Erstelle temporäre Datei im Cache-Verzeichnis
            val cacheDir = File(context.cacheDir, "shared_characters")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val fileName = "${character.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.json"
            val tempFile = File(cacheDir, fileName)
            tempFile.writeText(jsonResult.getOrNull()!!)
            
            // Erstelle URI mit FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            
            // Erstelle Share-Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "DSA Charakter: ${character.name}")
                putExtra(Intent.EXTRA_TEXT, "Charakterdaten für ${character.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Result.success(Intent.createChooser(shareIntent, "Charakter teilen"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Prüft, ob beim Import Warnungen auftreten würden, ohne den Import tatsächlich durchzuführen.
     * @param jsonString Der JSON-String mit den Charakterdaten
     * @param targetCharacterId Optional: ID eines bestehenden Charakters, der überschrieben werden soll
     * @return Result mit Pair<GUID, Warning?> - GUID des zu importierenden Charakters und optionale Warnung
     */
    suspend fun checkImportWarnings(
        jsonString: String,
        targetCharacterId: Long? = null
    ): Result<Pair<String, String?>> = withContext(Dispatchers.IO) {
        try {
            val exportDto = json.decodeFromString<CharacterExportDto>(jsonString)
            
            // Versionscheck
            val (isCompatible, warning) = DataModelVersion.checkCompatibility(exportDto.version)
            if (!isCompatible) {
                return@withContext Result.failure(Exception(warning ?: "Inkompatible Version"))
            }
            
            val overwriteWarning = DataModelVersion.checkOverwriteWarning(
                DataModelVersion.CURRENT_VERSION, 
                exportDto.version
            )
            
            // Suche nach bestehendem Charakter mit gleicher GUID
            val existingCharacterByGuid = repository.getCharacterByGuid(exportDto.character.guid)
            
            // Prüfe, ob targetCharacterId angegeben wurde und ob sie mit der GUID übereinstimmt
            val targetCharacter = if (targetCharacterId != null) {
                repository.getCharacterById(targetCharacterId)
            } else null
            
            // Bestimme, welcher Charakter aktualisiert werden soll
            val existingCharacter = when {
                // Falls targetCharacterId angegeben, muss GUID übereinstimmen
                targetCharacter != null -> {
                    if (targetCharacter.guid != exportDto.character.guid) {
                        return@withContext Result.failure(Exception(
                            "GUID-Mismatch: Der ausgewählte Charakter stimmt nicht mit dem importierten Charakter überein."
                        ))
                    }
                    targetCharacter
                }
                // Sonst verwende Charakter mit gleicher GUID (wenn vorhanden)
                existingCharacterByGuid != null -> existingCharacterByGuid
                // Kein bestehender Charakter gefunden
                else -> null
            }
            
            val additionalWarnings = mutableListOf<String>()
            
            // Prüfe lastModifiedDate, wenn Charakter aktualisiert wird
            if (existingCharacter != null && existingCharacter.lastModifiedDate > exportDto.exportTimestamp) {
                additionalWarnings += "WARNUNG: Der lokale Charakter wurde nach dem Export geändert (" +
                    "Lokal: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(existingCharacter.lastModifiedDate)}, " +
                    "Export: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(exportDto.exportTimestamp)}). " +
                    "Beim Import gehen diese Änderungen verloren!"
            }
            
            val finalWarning = listOfNotNull(
                warning,
                overwriteWarning,
                additionalWarnings.takeIf { it.isNotEmpty() }?.joinToString("\n")
            ).joinToString("\n\n")
            
            Result.success(Pair(exportDto.character.guid, finalWarning.ifEmpty { null }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Importiert einen Charakter aus einem JSON-String.
     * @param jsonString Der JSON-String mit den Charakterdaten
     * @param targetCharacterId Optional: ID eines bestehenden Charakters, der überschrieben werden soll
     *                          Wenn null, wird ein neuer Charakter angelegt
     * @return Result mit Pair<Character-ID, Warnung?>
     */
    suspend fun importCharacter(
        jsonString: String,
        targetCharacterId: Long? = null
    ): Result<Pair<Long, String?>> = withContext(Dispatchers.IO) {
        try {
            val exportDto = json.decodeFromString<CharacterExportDto>(jsonString)
            
            // Versionscheck
            val (isCompatible, warning) = DataModelVersion.checkCompatibility(exportDto.version)
            if (!isCompatible) {
                return@withContext Result.failure(Exception(warning ?: "Inkompatible Version"))
            }
            
            val overwriteWarning = DataModelVersion.checkOverwriteWarning(
                DataModelVersion.CURRENT_VERSION, 
                exportDto.version
            )
            
            // Suche nach bestehendem Charakter mit gleicher GUID
            val existingCharacterByGuid = repository.getCharacterByGuid(exportDto.character.guid)
            
            // Prüfe, ob targetCharacterId angegeben wurde und ob sie mit der GUID übereinstimmt
            val targetCharacter = if (targetCharacterId != null) {
                repository.getCharacterById(targetCharacterId)
            } else null
            
            // Bestimme, welcher Charakter aktualisiert werden soll
            val existingCharacter = when {
                // Falls targetCharacterId angegeben, muss GUID übereinstimmen
                targetCharacter != null -> {
                    if (targetCharacter.guid != exportDto.character.guid) {
                        return@withContext Result.failure(Exception(
                            "GUID-Mismatch: Der ausgewählte Charakter stimmt nicht mit dem importierten Charakter überein."
                        ))
                    }
                    targetCharacter
                }
                // Sonst verwende Charakter mit gleicher GUID (wenn vorhanden)
                existingCharacterByGuid != null -> existingCharacterByGuid
                // Kein bestehender Charakter gefunden
                else -> null
            }
            
            val additionalWarnings = mutableListOf<String>()
            
            // Prüfe lastModifiedDate, wenn Charakter aktualisiert wird
            if (existingCharacter != null && existingCharacter.lastModifiedDate > exportDto.exportTimestamp) {
                additionalWarnings += "WARNUNG: Der lokale Charakter wurde nach dem Export geändert (" +
                    "Lokal: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(existingCharacter.lastModifiedDate)}, " +
                    "Export: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(exportDto.exportTimestamp)}). " +
                    "Beim Import gehen diese Änderungen verloren!"
            }
            
            val characterId = if (existingCharacter != null) {
                // Bestehenden Charakter aktualisieren (ID und GUID beibehalten)
                // lastModifiedDate wird auf Export-Zeitstempel gesetzt (nicht aktuelle Zeit!)
                // WICHTIG: groupId wird beim Überschreiben NICHT geändert!
                
                val updatedCharacter = exportDto.character.toCharacter().copy(
                    id = existingCharacter.id,
                    guid = existingCharacter.guid,
                    groupId = existingCharacter.groupId,  // Gruppe wird NICHT überschrieben!
                    lastModifiedDate = exportDto.exportTimestamp
                )
                try {
                    repository.updateCharacter(updatedCharacter)
                } catch (e: Exception) {
                    throw Exception("Fehler beim Aktualisieren des Charakters: ${e.message}", e)
                }
                
                // Alte Slots löschen
                val oldSlots = repository.getSlotsByCharacter(existingCharacter.id).first()
                oldSlots.forEach { repository.deleteSlot(it) }
                
                // Alte Items löschen (damit Import nicht dupliziert)
                val oldItems = repository.getItemsForCharacter(existingCharacter.id).first()
                oldItems.forEach { repository.deleteItem(it) }
                
                // Alte nicht-Standard-Locations löschen (Standard-Locations bleiben erhalten)
                val oldLocations = repository.getLocationsForCharacter(existingCharacter.id).first()
                oldLocations.filter { !it.isDefault }.forEach { repository.deleteLocation(it) }
                
                existingCharacter.id
            } else {
                // Neuen Charakter erstellen (mit GUID aus Import)
                // lastModifiedDate wird auf Export-Zeitstempel gesetzt (nicht aktuelle Zeit!)
                
                // GroupId über Gruppennamen auflösen
                val allGroups = repository.allGroups.first()
                val resolvedGroupId = exportDto.character.groupName?.let { groupName ->
                    // Suche Group nach Namen
                    allGroups.find { it.name == groupName }?.id
                        ?: allGroups.find { it.name == "Unbekannte Gruppe" }?.id  // Fallback
                }
                
                if (exportDto.character.groupName != null && resolvedGroupId == null) {
                    additionalWarnings += "Die Gruppe '${exportDto.character.groupName}' wurde nicht gefunden. Charakter wird in 'Unbekannte Gruppe' eingefügt."
                }
                
                val newCharacter = exportDto.character.toCharacter().copy(
                    groupId = resolvedGroupId,
                    lastModifiedDate = exportDto.exportTimestamp
                )
                val newCharacterId = try {
                    repository.insertCharacter(newCharacter)
                } catch (e: Exception) {
                    throw Exception("Fehler beim Erstellen des Charakters: ${e.message}", e)
                }
                
                // Erstelle Standard-Locations (Rüstung/Kleidung, Rucksack)
                try {
                    repository.createDefaultLocationsForCharacter(newCharacterId)
                } catch (e: Exception) {
                    throw Exception("Fehler beim Erstellen der Standard-Locations: ${e.message}", e)
                }
                
                newCharacterId
            }
            
            // Rezepte für spätere Zuordnung cachen
            val allRecipes = repository.allRecipes.first()
            val recipesById = allRecipes.associateBy { it.id }
            val recipesByName = allRecipes.associateBy { it.name }
            
            // Bestehende Tränke laden (für GUID-basierten Merge)
            val existingPotions = repository.getPotionsForCharacter(characterId).first()
            val existingPotionsByGuid = existingPotions.associate { it.potion.guid to it.potion }
            
            // Bestehende Wissenseinträge zurücksetzen (Tränke werden gemerged, nicht gelöscht!)
            repository.deleteRecipeKnowledgeForCharacter(characterId)
            
            // Tränke importieren mit intelligentem Merge
            exportDto.potions.forEach { potionDto ->
                val resolvedRecipeId = potionDto.recipeId?.let { recipesById[it]?.id }
                    ?: potionDto.recipeName?.let { recipesByName[it]?.id }
                if (resolvedRecipeId == null) {
                    val identifier = potionDto.recipeName ?: potionDto.recipeId?.toString() ?: "Unbekanntes Rezept"
                    additionalWarnings += "Trank für Rezept '$identifier' konnte nicht importiert werden, da das Rezept nicht gefunden wurde."
                } else {
                    val importedPotion = potionDto.toPotion(characterId, resolvedRecipeId)
                    val existingPotion = existingPotionsByGuid[potionDto.guid]
                    
                    try {
                        if (existingPotion != null) {
                            // Trank existiert bereits -> Merge mit besserem Analyse-Ergebnis
                            val mergedPotion = mergePotion(existingPotion, importedPotion)
                            repository.updatePotion(mergedPotion)
                        } else {
                            // Neuer Trank -> Einfügen
                            repository.insertPotion(importedPotion)
                        }
                    } catch (e: Exception) {
                        val recipeName = potionDto.recipeName ?: "Unbekannt"
                        throw Exception("Fehler beim Einfügen/Aktualisieren des Tranks für Rezept '$recipeName' (Foreign Key: Recipe): ${e.message}", e)
                    }
                }
            }
            
            exportDto.recipeKnowledge.forEach { knowledgeDto ->
                val resolvedRecipeId = knowledgeDto.recipeId?.let { recipesById[it]?.id }
                    ?: knowledgeDto.recipeName?.let { recipesByName[it]?.id }
                if (resolvedRecipeId == null) {
                    val identifier = knowledgeDto.recipeName ?: knowledgeDto.recipeId?.toString() ?: "Unbekanntes Rezept"
                    additionalWarnings += "Rezeptwissen für '$identifier' konnte nicht importiert werden, da das Rezept nicht gefunden wurde."
                } else {
                    try {
                        repository.insertRecipeKnowledge(knowledgeDto.toModel(characterId, resolvedRecipeId))
                    } catch (e: Exception) {
                        val recipeName = knowledgeDto.recipeName ?: "Unbekannt"
                        throw Exception("Fehler beim Einfügen des Rezeptwissens für '$recipeName' (Foreign Key: Recipe): ${e.message}", e)
                    }
                }
            }
            
            // Inventar importieren: Locations und Items
            // Bestehende Locations laden (für Namen-Matching)
            val existingLocations = repository.getLocationsForCharacter(characterId).first()
            val existingLocationsByName = existingLocations.associateBy { it.name }
            
            // Map für neue Location-Namen -> Location-IDs
            val locationIdsByName = mutableMapOf<String, Long>()
            
            // Locations importieren
            exportDto.locations.forEach { locationDto ->
                val existingLocation = existingLocationsByName[locationDto.name]
                if (existingLocation != null) {
                    // Location existiert bereits -> ID merken, aber nicht neu erstellen
                    locationIdsByName[locationDto.name] = existingLocation.id
                } else {
                    // Neue Location erstellen
                    try {
                        val newLocationId = repository.insertLocation(locationDto.toLocation(characterId))
                        locationIdsByName[locationDto.name] = newLocationId
                    } catch (e: Exception) {
                        throw Exception("Fehler beim Erstellen der Location '${locationDto.name}' (Foreign Key: Character): ${e.message}", e)
                    }
                }
            }
            
            // Items importieren
            // Items importieren und GUID -> ID Mapping erstellen (für SpellSlots und MagicSigns)
            val itemIdsByGuid = mutableMapOf<String, Long>()
            exportDto.items.forEach { itemDto ->
                val resolvedLocationId = itemDto.locationName?.let { locationIdsByName[it] }
                // Für SelfItems: Ermittle die neue Location-ID über den Namen
                val resolvedSelfItemForLocationId = itemDto.selfItemForLocationName?.let { 
                    locationIdsByName[it] 
                }
                try {
                    val newItemId = repository.insertItem(
                        itemDto.toItem(characterId, resolvedLocationId, resolvedSelfItemForLocationId)
                    )
                    itemIdsByGuid[itemDto.guid] = newItemId
                    
                    // Wenn es ein SelfItem ist, aktualisiere hasSelfItem auf der Location
                    if (itemDto.isSelfItem && resolvedSelfItemForLocationId != null) {
                        val location = repository.getLocationById(resolvedSelfItemForLocationId)
                        if (location != null && !location.hasSelfItem) {
                            repository.updateLocation(location.copy(hasSelfItem = true))
                        }
                    }
                } catch (e: Exception) {
                    val locationInfo = itemDto.locationName?.let { " in Location '$it'" } ?: " ohne Location"
                    throw Exception("Fehler beim Einfügen des Items '${itemDto.name}'$locationInfo (Foreign Key: Character oder Location): ${e.message}", e)
                }
            }
            
            // Stelle sicher, dass alle Locations ein Eigenobjekt haben (auch bestehende)
            // HINWEIS: Nur für Locations die noch kein SelfItem haben (nicht aus Import)
            val allLocationsAfterImport = repository.getLocationsForCharacter(characterId).first()
            allLocationsAfterImport.forEach { location ->
                if (!location.hasSelfItem) {
                    try {
                        repository.createSelfItemForLocation(location)
                    } catch (e: Exception) {
                        // Ignoriere Fehler beim Erstellen von Eigenobjekten (nicht kritisch)
                    }
                }
            }
            
            // SpellSlots importieren (nach Items, da itemId aufgelöst werden muss)
            val allSpells = repository.allSpells.first()
            val characterGuid = exportDto.character.guid
            
            val newSlots = exportDto.spellSlots.map { slotDto ->
                val resolvedSpellId = slotDto.spellName?.let { spellName ->
                    allSpells.find { it.name == spellName }?.id
                }
                // Item-ID über GUID auflösen
                val resolvedItemId = slotDto.itemGuid?.let { guid ->
                    itemIdsByGuid[guid]
                }
                // creatorGuid mit Character-GUID als Fallback (für ältere Exporte)
                slotDto.toSpellSlot(characterId, resolvedSpellId, resolvedItemId, characterGuid)
            }
            
            try {
                repository.insertSlots(newSlots)
            } catch (e: Exception) {
                throw Exception("Fehler beim Einfügen der Zauber-Slots (Foreign Key: Spell oder Item): ${e.message}", e)
            }
            
            // MagicSigns importieren (seit v6)
            if (exportDto.magicSigns.isNotEmpty()) {
                exportDto.magicSigns.forEach { signDto ->
                    val resolvedItemId = itemIdsByGuid[signDto.itemGuid]
                    if (resolvedItemId != null) {
                        try {
                            val magicSign = signDto.toMagicSign(characterId, resolvedItemId, characterGuid)
                            repository.insertMagicSign(magicSign)
                        } catch (e: Exception) {
                            additionalWarnings += "Zauberzeichen '${signDto.name}' konnte nicht importiert werden: ${e.message}"
                        }
                    } else {
                        additionalWarnings += "Zauberzeichen '${signDto.name}' konnte nicht importiert werden, da das zugehörige Item nicht gefunden wurde."
                    }
                }
            }
            
            // Journal-Einträge importieren
            exportDto.journalEntries.forEach { journalDto ->
                try {
                    repository.insertJournalEntry(journalDto.toJournalEntry(characterId))
                } catch (e: Exception) {
                    throw Exception("Fehler beim Einfügen des Journal-Eintrags (Category: ${journalDto.category}): ${e.message}", e)
                }
            }
            
            // WICHTIG: Nach allen Inserts (die touchCharacter aufrufen) das lastModifiedDate
            // nochmal explizit auf den exportTimestamp setzen!
            val finalCharacter = repository.getCharacterById(characterId)
            finalCharacter?.let {
                repository.updateCharacter(it.copy(lastModifiedDate = exportDto.exportTimestamp))
            }
            
            val finalWarning = listOfNotNull(
                warning,
                overwriteWarning,
                additionalWarnings.takeIf { it.isNotEmpty() }?.joinToString("\n")
            ).joinToString("\n\n")
            Result.success(Pair(characterId, finalWarning.ifEmpty { null }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Lädt einen Charakter aus einer JSON-Datei.
     * @param context Android Context
     * @param uri URI der zu lesenden Datei
     * @param targetCharacterId Optional: ID eines bestehenden Charakters, der überschrieben werden soll
     */
    suspend fun loadCharacterFromFile(
        context: Context,
        uri: Uri,
        targetCharacterId: Long? = null
    ): Result<Pair<Long, String?>> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return@withContext Result.failure(IOException("Konnte Datei nicht lesen"))
            
            importCharacter(jsonString, targetCharacterId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
