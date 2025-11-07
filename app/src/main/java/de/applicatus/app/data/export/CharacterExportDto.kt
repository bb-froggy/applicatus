package de.applicatus.app.data.export

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.AnalysisStatus
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.RecipeKnowledge
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.model.spell.SlotType
import de.applicatus.app.data.model.spell.SpellSlot
import kotlinx.serialization.Serializable

/**
 * DTO für den Export/Import eines Charakters mit allen zugehörigen Daten.
 * WICHTIG: Keine Standardwerte für version und exportTimestamp,
 * da diese sonst bei Serialisierung wegoptimiert werden könnten.
 */
@Serializable
data class CharacterExportDto(
    val version: Int,
    val character: CharacterDto,
    val spellSlots: List<SpellSlotDto>,
    val potions: List<PotionDto> = emptyList(),
    val recipeKnowledge: List<RecipeKnowledgeDto> = emptyList(),
    val exportTimestamp: Long
)

/**
 * DTO für Charakterdaten (ohne Room-Annotationen).
 */
@Serializable
data class CharacterDto(
    val id: Long = 0,
    val guid: String,  // Eindeutige GUID für Import/Export-Abgleich
    val name: String,
    val mu: Int = 8,
    val kl: Int = 8,
    val inValue: Int = 8,
    val ch: Int = 8,
    val ff: Int = 8,
    val ge: Int = 8,
    val ko: Int = 8,
    val kk: Int = 8,
    val hasApplicatus: Boolean = false,
    val applicatusZfw: Int = 0,
    val applicatusModifier: Int = 0,
    val hasAlchemy: Boolean = false,
    val alchemySkill: Int = 0,
    val hasCookingPotions: Boolean = false,
    val cookingPotionsSkill: Int = 0,
    val hasOdem: Boolean = false,
    val odemZfw: Int = 0,
    val hasAnalys: Boolean = false,
    val analysZfw: Int = 0,
    val currentLe: Int = 30,
    val maxLe: Int = 30,
    val leRegenBonus: Int = 0,
    val hasAe: Boolean = false,
    val currentAe: Int = 0,
    val maxAe: Int = 0,
    val aeRegenBonus: Int = 0,
    val hasMasteryRegeneration: Boolean = false,
    val hasKe: Boolean = false,
    val currentKe: Int = 0,
    val maxKe: Int = 0
) {
    companion object {
        fun fromCharacter(character: Character) = CharacterDto(
            id = character.id,
            guid = character.guid,
            name = character.name,
            mu = character.mu,
            kl = character.kl,
            inValue = character.inValue,
            ch = character.ch,
            ff = character.ff,
            ge = character.ge,
            ko = character.ko,
            kk = character.kk,
            hasApplicatus = character.hasApplicatus,
            applicatusZfw = character.applicatusZfw,
            applicatusModifier = character.applicatusModifier,
            hasAlchemy = character.hasAlchemy,
            alchemySkill = character.alchemySkill,
            hasCookingPotions = character.hasCookingPotions,
            cookingPotionsSkill = character.cookingPotionsSkill,
            hasOdem = character.hasOdem,
            odemZfw = character.odemZfw,
            hasAnalys = character.hasAnalys,
            analysZfw = character.analysZfw,
            currentLe = character.currentLe,
            maxLe = character.maxLe,
            leRegenBonus = character.leRegenBonus,
            hasAe = character.hasAe,
            currentAe = character.currentAe,
            maxAe = character.maxAe,
            aeRegenBonus = character.aeRegenBonus,
            hasMasteryRegeneration = character.hasMasteryRegeneration,
            hasKe = character.hasKe,
            currentKe = character.currentKe,
            maxKe = character.maxKe
        )
    }
    
    fun toCharacter() = Character(
        id = 0, // Neue ID wird bei Insert generiert
        guid = guid,  // GUID wird übernommen
        name = name,
        mu = mu,
        kl = kl,
        inValue = inValue,
        ch = ch,
        ff = ff,
        ge = ge,
        ko = ko,
        kk = kk,
        hasApplicatus = hasApplicatus,
        applicatusZfw = applicatusZfw,
        applicatusModifier = applicatusModifier,
        hasAlchemy = hasAlchemy,
        alchemySkill = alchemySkill,
        hasCookingPotions = hasCookingPotions,
        cookingPotionsSkill = cookingPotionsSkill,
        hasOdem = hasOdem,
        odemZfw = odemZfw,
        hasAnalys = hasAnalys,
        analysZfw = analysZfw,
        currentLe = currentLe,
        maxLe = maxLe,
        leRegenBonus = leRegenBonus,
        hasAe = hasAe,
        currentAe = currentAe,
        maxAe = maxAe,
        aeRegenBonus = aeRegenBonus,
        hasMasteryRegeneration = hasMasteryRegeneration,
        hasKe = hasKe,
        currentKe = currentKe,
        maxKe = maxKe
    )
}

/**
 * DTO für SpellSlot-Daten (ohne Room-Annotationen).
 */
@Serializable
data class SpellSlotDto(
    val slotNumber: Int,
    val slotType: String, // SlotType als String für Serialisierung
    val volumePoints: Int = 0,
    val spellId: Long?,
    val spellName: String?, // Zaubername zur Referenz (für manuelles Matching)
    val zfw: Int = 0,
    val modifier: Int = 0,
    val variant: String = "",
    val isFilled: Boolean = false,
    val zfpStar: Int? = null,
    val lastRollResult: String? = null,
    val applicatusRollResult: String? = null
) {
    companion object {
        fun fromSpellSlot(slot: SpellSlot, spellName: String?) = SpellSlotDto(
            slotNumber = slot.slotNumber,
            slotType = slot.slotType.name,
            volumePoints = slot.volumePoints,
            spellId = slot.spellId,
            spellName = spellName,
            zfw = slot.zfw,
            modifier = slot.modifier,
            variant = slot.variant,
            isFilled = slot.isFilled,
            zfpStar = slot.zfpStar,
            lastRollResult = slot.lastRollResult,
            applicatusRollResult = slot.applicatusRollResult
        )
    }
    
    fun toSpellSlot(characterId: Long, resolvedSpellId: Long?) = SpellSlot(
        id = 0, // Neue ID wird bei Insert generiert
        characterId = characterId,
        slotNumber = slotNumber,
        slotType = try { SlotType.valueOf(slotType) } catch (e: Exception) { SlotType.APPLICATUS },
        volumePoints = volumePoints,
        spellId = resolvedSpellId,
        zfw = zfw,
        modifier = modifier,
        variant = variant,
        isFilled = isFilled,
        zfpStar = zfpStar,
        lastRollResult = lastRollResult,
        applicatusRollResult = applicatusRollResult
    )
}

@Serializable
data class PotionDto(
    val recipeId: Long? = null,
    val recipeName: String? = null,
    val quality: String,
    val appearance: String = "",
    val analysisStatus: String = AnalysisStatus.NOT_ANALYZED.name,
    val expiryDate: String
) {
    companion object {
        fun fromPotion(potion: Potion, recipeName: String?) = PotionDto(
            recipeId = potion.recipeId,
            recipeName = recipeName,
            quality = potion.quality.name,
            appearance = potion.appearance,
            analysisStatus = potion.analysisStatus.name,
            expiryDate = potion.expiryDate
        )
    }

    fun toPotion(characterId: Long, resolvedRecipeId: Long): Potion {
        val safeQuality = runCatching { PotionQuality.valueOf(quality) }
            .getOrElse { PotionQuality.C }
        val safeStatus = runCatching { AnalysisStatus.valueOf(analysisStatus) }
            .getOrElse { AnalysisStatus.NOT_ANALYZED }
        return Potion(
            id = 0,
            characterId = characterId,
            recipeId = resolvedRecipeId,
            quality = safeQuality,
            appearance = appearance,
            analysisStatus = safeStatus,
            expiryDate = expiryDate
        )
    }
}

@Serializable
data class RecipeKnowledgeDto(
    val recipeId: Long? = null,
    val recipeName: String? = null,
    val knowledgeLevel: String = RecipeKnowledgeLevel.UNKNOWN.name
) {
    companion object {
        fun fromModel(knowledge: RecipeKnowledge, recipeName: String?) = RecipeKnowledgeDto(
            recipeId = knowledge.recipeId,
            recipeName = recipeName,
            knowledgeLevel = knowledge.knowledgeLevel.name
        )
    }

    fun toModel(characterId: Long, resolvedRecipeId: Long): RecipeKnowledge {
        val level = runCatching { RecipeKnowledgeLevel.valueOf(knowledgeLevel) }
            .getOrElse { RecipeKnowledgeLevel.UNKNOWN }
        return RecipeKnowledge(
            id = 0,
            characterId = characterId,
            recipeId = resolvedRecipeId,
            knowledgeLevel = level
        )
    }
}
