package de.applicatus.app.data.export

import de.applicatus.app.data.model.character.GlobalSettings
import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.spell.Spell
import kotlinx.serialization.Serializable

/**
 * DTO für den Export/Import der kompletten Datenbank als Backup.
 * 
 * Enthält alle Entities:
 * - Spell und Recipe Libraries (für zukünftige gerätespezifische Änderungen)
 * - Globale Einstellungen
 * - Alle Gruppen mit Charakteren
 * - Alle zugehörigen Daten (Slots, Tränke, Inventar, etc.)
 */
@Serializable
data class DatabaseBackupDto(
    val backupVersion: Int,
    val backupTimestamp: Long,
    val spells: List<SpellDto>,
    val recipes: List<RecipeDto>,
    val globalSettings: GlobalSettingsDto?,
    val groups: List<GroupDto>,
    val characters: List<CharacterExportDto>
)

/**
 * DTO für Spell-Daten.
 */
@Serializable
data class SpellDto(
    val id: Long,
    val name: String,
    val attribute1: String,
    val attribute2: String,
    val attribute3: String
) {
    companion object {
        fun fromSpell(spell: Spell) = SpellDto(
            id = spell.id,
            name = spell.name,
            attribute1 = spell.attribute1,
            attribute2 = spell.attribute2,
            attribute3 = spell.attribute3
        )
    }

    fun toSpell() = Spell(
        id = id,
        name = name,
        attribute1 = attribute1,
        attribute2 = attribute2,
        attribute3 = attribute3
    )
}

/**
 * DTO für Recipe-Daten.
 */
@Serializable
data class RecipeDto(
    val id: Long,
    val name: String,
    val gruppe: String,
    val lab: String?,  // Laboratory as String for serialization
    val preis: Int?,
    val zutatenPreis: Int?,
    val zutatenVerbreitung: Int,
    val verbreitung: Int,
    val brewingDifficulty: Int,
    val analysisDifficulty: Int,
    val appearance: String,
    val shelfLife: String,
    val quantityProduced: Int
) {
    companion object {
        fun fromRecipe(recipe: Recipe) = RecipeDto(
            id = recipe.id,
            name = recipe.name,
            gruppe = recipe.gruppe,
            lab = recipe.lab?.name,
            preis = recipe.preis,
            zutatenPreis = recipe.zutatenPreis,
            zutatenVerbreitung = recipe.zutatenVerbreitung,
            verbreitung = recipe.verbreitung,
            brewingDifficulty = recipe.brewingDifficulty,
            analysisDifficulty = recipe.analysisDifficulty,
            appearance = recipe.appearance,
            shelfLife = recipe.shelfLife,
            quantityProduced = recipe.quantityProduced
        )
    }

    fun toRecipe() = Recipe(
        id = id,
        name = name,
        gruppe = gruppe,
        lab = lab?.let { runCatching { de.applicatus.app.data.model.potion.Laboratory.valueOf(it) }.getOrNull() },
        preis = preis,
        zutatenPreis = zutatenPreis,
        zutatenVerbreitung = zutatenVerbreitung,
        verbreitung = verbreitung,
        brewingDifficulty = brewingDifficulty,
        analysisDifficulty = analysisDifficulty,
        appearance = appearance,
        shelfLife = shelfLife,
        quantityProduced = quantityProduced
    )
}

/**
 * DTO für GlobalSettings-Daten.
 */
@Serializable
data class GlobalSettingsDto(
    val currentDerianDate: String
) {
    companion object {
        fun fromGlobalSettings(settings: GlobalSettings) = GlobalSettingsDto(
            currentDerianDate = settings.currentDerianDate
        )
    }

    fun toGlobalSettings() = GlobalSettings(
        id = 1, // Singleton ID
        currentDerianDate = currentDerianDate
    )
}

/**
 * DTO für Group-Daten.
 */
@Serializable
data class GroupDto(
    val id: Long,
    val name: String,
    val currentDerianDate: String,
    val isGameMasterGroup: Boolean = false
) {
    companion object {
        fun fromGroup(group: Group) = GroupDto(
            id = group.id,
            name = group.name,
            currentDerianDate = group.currentDerianDate,
            isGameMasterGroup = group.isGameMasterGroup
        )
    }

    fun toGroup() = Group(
        id = 0, // Neue ID wird bei Insert generiert oder bei Merge verwendet
        name = name,
        currentDerianDate = currentDerianDate,
        isGameMasterGroup = isGameMasterGroup
    )
}
