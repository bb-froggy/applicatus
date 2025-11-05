package de.applicatus.app.data.export

import de.applicatus.app.data.DataModelVersion
import de.applicatus.app.data.model.Character
import de.applicatus.app.data.model.SlotType
import de.applicatus.app.data.model.SpellSlot
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
    val applicatusModifier: Int = 0
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
            applicatusModifier = character.applicatusModifier
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
        applicatusModifier = applicatusModifier
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
