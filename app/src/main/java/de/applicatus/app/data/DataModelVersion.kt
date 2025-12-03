package de.applicatus.app.data

/**
 * Versionierung des Datenmodells für Export/Import und Synchronisation.
 * 
 * WICHTIG: Diese Version muss bei JEDER Änderung am Datenmodell erhöht werden:
 * - Hinzufügen/Entfernen von Feldern in Character, SpellSlot, Spell
 * - Änderung von Datentypen
 * - Änderung von Enum-Werten (z.B. SlotType)
 * - Änderung der Datenbank-Struktur
 * 
 * Aktuelle Version: 6
 * 
 * Versions-Historie:
 * v1: Initiale Version
 * v2: Hinzufügen von Applicatus-Unterstützung, SlotType, Volumenpunkte
 * v3: Export erweitert um Alchemie-, Energie-, Trank- und Rezeptwissen-Daten
 * v4: Hinzufügen von Gruppen für Charaktere, GUID für Tränke (Trank-Übergabe)
 * v5: Magisches Meisterhandwerk für Alchimie und Kochen (Tränke)
 * v6: Zauberzeichen-Export, Creator-GUID für SpellSlots/MagicSigns, Item-GUID, Item-Bindung für SpellSlots
 */
object DataModelVersion {
     const val CURRENT_VERSION = 6
    
    /**
     * Prüft, ob eine Import-Version mit der aktuellen Version kompatibel ist.
     * @return Pair<isCompatible, warningMessage?>
     */
    fun checkCompatibility(importVersion: Int): Pair<Boolean, String?> {
        return when {
            importVersion == CURRENT_VERSION -> Pair(true, null)
            importVersion < CURRENT_VERSION -> Pair(
                true, 
                "Warnung: Die Daten stammen von einer älteren Version (v$importVersion). " +
                "Einige neuere Funktionen könnten fehlen."
            )
            importVersion > CURRENT_VERSION -> Pair(
                false, 
                "Fehler: Die Daten stammen von einer neueren Version (v$importVersion). " +
                "Bitte aktualisieren Sie die App, um diese Daten zu importieren."
            )
            else -> Pair(false, "Unbekannte Version: $importVersion")
        }
    }
    
    /**
     * Prüft, ob ein existierender Charakter mit einer neueren Version überschrieben werden soll.
     */
    fun checkOverwriteWarning(existingVersion: Int, importVersion: Int): String? {
        if (existingVersion > importVersion) {
            return "Warnung: Sie versuchen, einen Charakter (v$existingVersion) mit einer " +
                   "älteren Version (v$importVersion) zu überschreiben. Dabei können Daten verloren gehen."
        }
        return null
    }
}
