package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.character.CharacterJournalEntry
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Character Journal Screen.
 * 
 * Manages:
 * - Journal entries for a character
 * - Game master mode visibility
 * - Category filtering
 */
class CharacterJournalViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    /**
     * Character being displayed.
     */
    val character: StateFlow<Character?> = repository.getCharacterByIdFlow(characterId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    /**
     * Whether the character's group is in game master mode.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val isGameMasterGroup: StateFlow<Boolean> = character
        .mapLatest { char ->
            char?.groupId?.let { groupId ->
                repository.getGroupByIdOnce(groupId)?.isGameMasterGroup
            } ?: false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * All journal entries for this character (newest first).
     */
    val journalEntries: StateFlow<List<CharacterJournalEntry>> = repository.getJournalEntries(characterId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * Total count of journal entries (for display).
     */
    suspend fun getEntryCount(): Int = repository.getJournalEntryCount(characterId)
    
    /**
     * Get entries filtered by category.
     */
    fun getEntriesByCategory(category: String): StateFlow<List<CharacterJournalEntry>> =
        repository.getJournalEntriesByCategory(characterId, category)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    /**
     * Get entries filtered by category pattern (e.g., "Potion.%" for all potion events).
     */
    fun getEntriesByCategoryPattern(categoryPattern: String): StateFlow<List<CharacterJournalEntry>> =
        repository.getJournalEntriesByCategoryPattern(characterId, categoryPattern)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}

/**
 * Factory for creating CharacterJournalViewModel instances.
 */
class CharacterJournalViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterJournalViewModel::class.java)) {
            return CharacterJournalViewModel(repository, characterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
