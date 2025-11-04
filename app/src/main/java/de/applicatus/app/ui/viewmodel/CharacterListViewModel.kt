package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.Character
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CharacterListViewModel(
    private val repository: ApplicatusRepository
) : ViewModel() {
    
    val characters: StateFlow<List<Character>> = repository.allCharacters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun addCharacter(
        name: String,
        mu: Int = 8,
        kl: Int = 8,
        inValue: Int = 8,
        ch: Int = 8,
        ff: Int = 8,
        ge: Int = 8,
        ko: Int = 8,
        kk: Int = 8,
        hasApplicatus: Boolean = false,
        applicatusZfw: Int = 0,
        applicatusModifier: Int = 0
    ) {
        viewModelScope.launch {
            val character = Character(
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
            val characterId = repository.insertCharacter(character)
            // Keine automatische Initialisierung von Slots mehr
            // Benutzer fügt Slots selbst im Bearbeitungsmodus hinzu
        }
    }
    
    fun deleteCharacter(character: Character) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
        }
    }
}

class CharacterListViewModelFactory(
    private val repository: ApplicatusRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CharacterListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
