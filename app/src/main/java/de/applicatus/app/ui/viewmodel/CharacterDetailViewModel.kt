package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.Character
import de.applicatus.app.data.model.Spell
import de.applicatus.app.data.model.SpellSlot
import de.applicatus.app.data.model.SpellSlotWithSpell
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.SpellChecker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CharacterDetailViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    val character: StateFlow<Character?> = repository.getCharacterByIdFlow(characterId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val spellSlots: StateFlow<List<SpellSlotWithSpell>> = 
        repository.getSlotsWithSpellsByCharacter(characterId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    val allSpells: StateFlow<List<Spell>> = repository.allSpells
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun updateSlotSpell(slot: SpellSlot, spellId: Long?) {
        viewModelScope.launch {
            repository.updateSlot(
                slot.copy(
                    spellId = spellId,
                    isFilled = false,
                    zfpStar = null,
                    lastRollResult = null
                )
            )
        }
    }
    
    fun updateSlotZfw(slot: SpellSlot, zfw: Int) {
        viewModelScope.launch {
            repository.updateSlot(slot.copy(zfw = zfw))
        }
    }
    
    fun updateSlotModifier(slot: SpellSlot, modifier: Int) {
        viewModelScope.launch {
            repository.updateSlot(slot.copy(modifier = modifier))
        }
    }
    
    fun updateSlotVariant(slot: SpellSlot, variant: String) {
        viewModelScope.launch {
            repository.updateSlot(slot.copy(variant = variant))
        }
    }
    
    fun updateAllModifiers(delta: Int) {
        viewModelScope.launch {
            spellSlots.value.forEach { slotWithSpell ->
                val slot = slotWithSpell.slot
                repository.updateSlot(
                    slot.copy(modifier = slot.modifier + delta)
                )
            }
        }
    }
    
    fun castSpell(slot: SpellSlot, spell: Spell) {
        viewModelScope.launch {
            val char = character.value ?: return@launch
            
            // Hole die Eigenschaftswerte basierend auf dem Zauber
            val attr1 = getAttributeValue(char, spell.attribute1)
            val attr2 = getAttributeValue(char, spell.attribute2)
            val attr3 = getAttributeValue(char, spell.attribute3)
            
            // Führe die Zauberprobe durch
            val result = SpellChecker.performSpellCheck(
                zfw = slot.zfw,
                modifier = slot.modifier,
                attribute1 = attr1,
                attribute2 = attr2,
                attribute3 = attr3
            )
            
            // Aktualisiere den Slot
            repository.updateSlot(
                slot.copy(
                    isFilled = result.success,
                    zfpStar = if (result.success) result.zfpStar else null,
                    lastRollResult = formatRollResult(result)
                )
            )
        }
    }
    
    fun clearSlot(slot: SpellSlot) {
        viewModelScope.launch {
            repository.updateSlot(
                slot.copy(
                    isFilled = false,
                    zfpStar = null,
                    lastRollResult = null
                )
            )
        }
    }
    
    private fun getAttributeValue(character: Character, attributeName: String): Int {
        return when (attributeName.uppercase()) {
            "MU" -> character.mu
            "KL" -> character.kl
            "IN" -> character.inValue
            "CH" -> character.ch
            "FF" -> character.ff
            "GE" -> character.ge
            "KO" -> character.ko
            "KK" -> character.kk
            else -> 8 // Default
        }
    }
    
    private fun formatRollResult(result: de.applicatus.app.logic.SpellCheckResult): String {
        val rollsStr = result.rolls.joinToString(", ")
        return when {
            result.isTripleOne -> "Dreifach-1! [$rollsStr] ZfP*: ${result.zfpStar}"
            result.isDoubleOne -> "Doppel-1! [$rollsStr] ZfP*: ${result.zfpStar}"
            result.isTripleTwenty -> "Dreifach-20! [$rollsStr] Katastrophe!"
            result.isDoubleTwenty -> "Doppel-20! [$rollsStr] Patzer!"
            result.success -> "Erfolg! [$rollsStr] ZfP*: ${result.zfpStar}"
            else -> "Fehlgeschlagen! [$rollsStr]"
        }
    }
}

class CharacterDetailViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CharacterDetailViewModel(repository, characterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
