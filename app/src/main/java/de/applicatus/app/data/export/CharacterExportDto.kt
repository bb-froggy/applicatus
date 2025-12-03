package de.applicatus.app.data.export

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.*
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
    val locations: List<LocationDto> = emptyList(),
    val items: List<ItemDto> = emptyList(),
    val journalEntries: List<JournalEntryDto> = emptyList(),
    val magicSigns: List<MagicSignDto> = emptyList(),  // Seit Version 6
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
    val alchemyIsMagicalMastery: Boolean = false,
    val hasCookingPotions: Boolean = false,
    val cookingPotionsSkill: Int = 0,
    val cookingPotionsIsMagicalMastery: Boolean = false,
    val selfControlSkill: Int = 0,
    val sensoryAcuitySkill: Int = 0,
    val magicalLoreSkill: Int = 0,
    val herbalLoreSkill: Int = 0,
    val hasOdem: Boolean = false,
    val odemZfw: Int = 0,
    val hasAnalys: Boolean = false,
    val analysZfw: Int = 0,
    val defaultLaboratory: String? = null,
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
    val maxKe: Int = 0,
    val groupName: String? = null,  // Gruppenname für portablen Export (nicht ID!)
    val lastModifiedDate: Long = System.currentTimeMillis()
) {
    companion object {
        suspend fun fromCharacter(character: Character, groupName: String?) = CharacterDto(
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
            alchemyIsMagicalMastery = character.alchemyIsMagicalMastery,
            hasCookingPotions = character.hasCookingPotions,
            cookingPotionsSkill = character.cookingPotionsSkill,
            cookingPotionsIsMagicalMastery = character.cookingPotionsIsMagicalMastery,
            selfControlSkill = character.selfControlSkill,
            sensoryAcuitySkill = character.sensoryAcuitySkill,
            magicalLoreSkill = character.magicalLoreSkill,
            herbalLoreSkill = character.herbalLoreSkill,
            hasOdem = character.hasOdem,
            odemZfw = character.odemZfw,
            hasAnalys = character.hasAnalys,
            analysZfw = character.analysZfw,
            defaultLaboratory = character.defaultLaboratory?.name,
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
            maxKe = character.maxKe,
            groupName = groupName,  // Gruppenname wird vom Aufrufer übergeben
            lastModifiedDate = character.lastModifiedDate
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
        alchemyIsMagicalMastery = alchemyIsMagicalMastery,
        hasCookingPotions = hasCookingPotions,
        cookingPotionsSkill = cookingPotionsSkill,
        cookingPotionsIsMagicalMastery = cookingPotionsIsMagicalMastery,
        selfControlSkill = selfControlSkill,
        sensoryAcuitySkill = sensoryAcuitySkill,
        magicalLoreSkill = magicalLoreSkill,
        herbalLoreSkill = herbalLoreSkill,
        hasOdem = hasOdem,
        odemZfw = odemZfw,
        hasAnalys = hasAnalys,
        analysZfw = analysZfw,
        defaultLaboratory = defaultLaboratory?.let { runCatching { de.applicatus.app.data.model.potion.Laboratory.valueOf(it) }.getOrNull() },
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
        maxKe = maxKe,
        groupId = null,  // Wird beim Import separat aufgelöst basierend auf groupName
        lastModifiedDate = lastModifiedDate
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
    val creatorGuid: String? = null,  // GUID des Erstellers (seit Version 6)
    val itemGuid: String? = null,  // GUID des gebundenen Items (seit Version 6)
    val variant: String = "",
    val isFilled: Boolean = false,
    val zfpStar: Int? = null,
    val lastRollResult: String? = null,
    val applicatusRollResult: String? = null,
    val isBotched: Boolean = false,
    val expiryDate: String? = null,
    val longDurationFormula: String = "",
    val aspCost: String = "",
    val useHexenRepresentation: Boolean = false
) {
    companion object {
        fun fromSpellSlot(slot: SpellSlot, spellName: String?, itemGuid: String?) = SpellSlotDto(
            slotNumber = slot.slotNumber,
            slotType = slot.slotType.name,
            volumePoints = slot.volumePoints,
            spellId = slot.spellId,
            spellName = spellName,
            zfw = slot.zfw,
            modifier = slot.modifier,
            creatorGuid = slot.creatorGuid,
            itemGuid = itemGuid,
            variant = slot.variant,
            isFilled = slot.isFilled,
            zfpStar = slot.zfpStar,
            lastRollResult = slot.lastRollResult,
            applicatusRollResult = slot.applicatusRollResult,
            isBotched = slot.isBotched,
            expiryDate = slot.expiryDate,
            longDurationFormula = slot.longDurationFormula,
            aspCost = slot.aspCost,
            useHexenRepresentation = slot.useHexenRepresentation
        )
    }
    
    fun toSpellSlot(characterId: Long, resolvedSpellId: Long?, resolvedItemId: Long?, creatorGuidFallback: String?) = SpellSlot(
        id = 0, // Neue ID wird bei Insert generiert
        characterId = characterId,
        slotNumber = slotNumber,
        slotType = try { SlotType.valueOf(slotType) } catch (e: Exception) { SlotType.APPLICATUS },
        volumePoints = volumePoints,
        spellId = resolvedSpellId,
        zfw = zfw,
        modifier = modifier,
        creatorGuid = creatorGuid ?: creatorGuidFallback,  // Fallback auf Character-GUID bei Migration
        itemId = resolvedItemId,
        variant = variant,
        isFilled = isFilled,
        zfpStar = zfpStar,
        lastRollResult = lastRollResult,
        applicatusRollResult = applicatusRollResult,
        isBotched = isBotched,
        expiryDate = expiryDate,
        longDurationFormula = longDurationFormula,
        aspCost = aspCost,
        useHexenRepresentation = useHexenRepresentation
    )
}

@Serializable
data class PotionDto(
    val guid: String,
    val recipeId: Long? = null,
    val recipeName: String? = null,
    val actualQuality: String,
    val appearance: String = "",
    val expiryDate: String,
    // Analysedaten
    val categoryKnown: Boolean = false,
    val knownQualityLevel: String = KnownQualityLevel.UNKNOWN.name,
    val intensityQuality: String = IntensityQuality.UNKNOWN.name,
    val refinedQuality: String = RefinedQuality.UNKNOWN.name,
    val knownExactQuality: String? = null,
    val shelfLifeKnown: Boolean = false,
    val intensityDeterminationZfp: Int = 0,
    val bestStructureAnalysisFacilitation: Int = 0
) {
    companion object {
        fun fromPotion(potion: Potion, recipeName: String?) = PotionDto(
            guid = potion.guid,
            recipeId = potion.recipeId,
            recipeName = recipeName,
            actualQuality = potion.actualQuality.name,
            appearance = potion.appearance,
            expiryDate = potion.expiryDate,
            categoryKnown = potion.categoryKnown,
            knownQualityLevel = potion.knownQualityLevel.name,
            intensityQuality = potion.intensityQuality.name,
            refinedQuality = potion.refinedQuality.name,
            knownExactQuality = potion.knownExactQuality?.name,
            shelfLifeKnown = potion.shelfLifeKnown,
            intensityDeterminationZfp = potion.intensityDeterminationZfp,
            bestStructureAnalysisFacilitation = potion.bestStructureAnalysisFacilitation
        )
    }

    fun toPotion(characterId: Long, resolvedRecipeId: Long): Potion {
        val safeActualQuality = runCatching { PotionQuality.valueOf(actualQuality) }
            .getOrElse { PotionQuality.C }
        val safeKnownQualityLevel = runCatching { KnownQualityLevel.valueOf(knownQualityLevel) }
            .getOrElse { KnownQualityLevel.UNKNOWN }
        val safeIntensityQuality = runCatching { IntensityQuality.valueOf(intensityQuality) }
            .getOrElse { IntensityQuality.UNKNOWN }
        val safeRefinedQuality = runCatching { RefinedQuality.valueOf(refinedQuality) }
            .getOrElse { RefinedQuality.UNKNOWN }
        val safeKnownExactQuality = knownExactQuality?.let { 
            runCatching { PotionQuality.valueOf(it) }.getOrNull()
        }
        
        return Potion(
            id = 0,
            guid = guid,
            characterId = characterId,
            recipeId = resolvedRecipeId,
            actualQuality = safeActualQuality,
            appearance = appearance,
            expiryDate = expiryDate,
            createdDate = expiryDate, // Fallback: verwende expiryDate als createdDate beim Import
            categoryKnown = categoryKnown,
            knownQualityLevel = safeKnownQualityLevel,
            intensityQuality = safeIntensityQuality,
            refinedQuality = safeRefinedQuality,
            knownExactQuality = safeKnownExactQuality,
            shelfLifeKnown = shelfLifeKnown,
            intensityDeterminationZfp = intensityDeterminationZfp,
            bestStructureAnalysisFacilitation = bestStructureAnalysisFacilitation,
            preservationAttempted = false
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

/**
 * DTO für Location-Daten (ohne Room-Annotationen).
 */
@Serializable
data class LocationDto(
    val name: String,
    val isDefault: Boolean = false,
    val isCarried: Boolean = false,
    val sortOrder: Int = 0
) {
    companion object {
        fun fromLocation(location: de.applicatus.app.data.model.inventory.Location) = LocationDto(
            name = location.name,
            isDefault = location.isDefault,
            isCarried = location.isCarried,
            sortOrder = location.sortOrder
        )
    }

    fun toLocation(characterId: Long) = de.applicatus.app.data.model.inventory.Location(
        id = 0, // Neue ID wird bei Insert generiert
        characterId = characterId,
        name = name,
        isDefault = isDefault,
        isCarried = isCarried,
        sortOrder = sortOrder
    )
}

/**
 * DTO für Item-Daten (ohne Room-Annotationen).
 */
@Serializable
data class ItemDto(
    val guid: String = java.util.UUID.randomUUID().toString(),  // GUID für Import/Export (seit Version 6)
    val locationName: String?, // Referenz zum Location-Namen (null = kein Ort)
    val name: String,
    val weightStone: Int = 0,
    val weightOunces: Int = 0,
    val sortOrder: Int = 0,
    val isPurse: Boolean = false,
    val kreuzerAmount: Int = 0,
    val isCountable: Boolean = false,
    val quantity: Int = 1
) {
    companion object {
        fun fromItem(item: de.applicatus.app.data.model.inventory.Item, locationName: String?) = ItemDto(
            guid = item.guid,
            locationName = locationName,
            name = item.name,
            weightStone = item.weight.stone,
            weightOunces = item.weight.ounces,
            sortOrder = item.sortOrder,
            isPurse = item.isPurse,
            kreuzerAmount = item.kreuzerAmount,
            isCountable = item.isCountable,
            quantity = item.quantity
        )
    }

    fun toItem(characterId: Long, resolvedLocationId: Long?) = de.applicatus.app.data.model.inventory.Item(
        id = 0, // Neue ID wird bei Insert generiert
        guid = guid,  // GUID übernehmen
        characterId = characterId,
        locationId = resolvedLocationId,
        name = name,
        weight = de.applicatus.app.data.model.inventory.Weight(
            stone = weightStone,
            ounces = weightOunces
        ),
        sortOrder = sortOrder,
        isPurse = isPurse,
        kreuzerAmount = kreuzerAmount,
        isCountable = isCountable,
        quantity = quantity
    )
}

/**
 * DTO für CharacterJournalEntry-Daten (ohne Room-Annotationen).
 */
@Serializable
data class JournalEntryDto(
    val timestamp: Long,
    val derianDate: String,
    val category: String,
    val playerMessage: String,
    val gmMessage: String? = null
) {
    companion object {
        fun fromJournalEntry(entry: de.applicatus.app.data.model.character.CharacterJournalEntry) = JournalEntryDto(
            timestamp = entry.timestamp,
            derianDate = entry.derianDate,
            category = entry.category,
            playerMessage = entry.playerMessage,
            gmMessage = entry.gmMessage
        )
    }

    fun toJournalEntry(characterId: Long) = de.applicatus.app.data.model.character.CharacterJournalEntry(
        id = 0, // Neue ID wird bei Insert generiert
        characterId = characterId,
        timestamp = timestamp,
        derianDate = derianDate,
        category = category,
        playerMessage = playerMessage,
        gmMessage = gmMessage
    )
}

/**
 * DTO für MagicSign-Daten (ohne Room-Annotationen). Seit Version 6.
 */
@Serializable
data class MagicSignDto(
    val guid: String,
    val itemGuid: String,  // GUID des Items, auf dem das Zauberzeichen angebracht ist
    val creatorGuid: String?,  // GUID des Charakters, der das Zauberzeichen erstellt hat
    val name: String,
    val effectDescription: String = "",
    val effect: String = de.applicatus.app.data.model.magicsign.MagicSignEffect.NONE.name,
    val activationModifier: Int = 0,
    val duration: String = de.applicatus.app.data.model.magicsign.MagicSignDuration.HALF_RKW_DAYS.name,
    val isActivated: Boolean = false,
    val isBotched: Boolean = false,
    val expiryDate: String? = null,
    val activationRkpStar: Int? = null,
    val lastRollResult: String? = null
) {
    companion object {
        fun fromMagicSign(sign: de.applicatus.app.data.model.magicsign.MagicSign, itemGuid: String) = MagicSignDto(
            guid = sign.guid,
            itemGuid = itemGuid,
            creatorGuid = sign.creatorGuid,
            name = sign.name,
            effectDescription = sign.effectDescription,
            effect = sign.effect.name,
            activationModifier = sign.activationModifier,
            duration = sign.duration.name,
            isActivated = sign.isActivated,
            isBotched = sign.isBotched,
            expiryDate = sign.expiryDate,
            activationRkpStar = sign.activationRkpStar,
            lastRollResult = sign.lastRollResult
        )
    }

    fun toMagicSign(characterId: Long, resolvedItemId: Long, creatorGuidFallback: String?) = de.applicatus.app.data.model.magicsign.MagicSign(
        id = 0, // Neue ID wird bei Insert generiert
        guid = guid,
        characterId = characterId,
        itemId = resolvedItemId,
        creatorGuid = creatorGuid ?: creatorGuidFallback,  // Fallback auf Character-GUID bei Migration
        name = name,
        effectDescription = effectDescription,
        effect = try { de.applicatus.app.data.model.magicsign.MagicSignEffect.valueOf(effect) } catch (e: Exception) { de.applicatus.app.data.model.magicsign.MagicSignEffect.NONE },
        activationModifier = activationModifier,
        duration = try { de.applicatus.app.data.model.magicsign.MagicSignDuration.valueOf(duration) } catch (e: Exception) { de.applicatus.app.data.model.magicsign.MagicSignDuration.HALF_RKW_DAYS },
        isActivated = isActivated,
        isBotched = isBotched,
        expiryDate = expiryDate,
        activationRkpStar = activationRkpStar,
        lastRollResult = lastRollResult
    )
}
