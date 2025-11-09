package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.PotionWithRecipe
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.RecipeKnowledge
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PotionViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    private val _potions = MutableStateFlow<List<PotionWithRecipe>>(emptyList())
    val potions: StateFlow<List<PotionWithRecipe>> = _potions.asStateFlow()
    
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()
    
    private val _character = MutableStateFlow<Character?>(null)
    val character: StateFlow<Character?> = _character.asStateFlow()
    
    private val _groupCharacters = MutableStateFlow<List<Character>>(emptyList())
    val groupCharacters: StateFlow<List<Character>> = _groupCharacters.asStateFlow()
    
    init {
        loadPotions()
        loadRecipes()
        loadCharacter()
        loadGroupCharacters()
    }
    
    private fun loadPotions() {
        viewModelScope.launch {
            repository.getPotionsForCharacter(characterId).collect { potionList ->
                _potions.value = potionList
            }
        }
    }
    
    private fun loadRecipes() {
        viewModelScope.launch {
            repository.allRecipes.collect { recipeList ->
                _recipes.value = recipeList
            }
        }
    }
    
    private fun loadCharacter() {
        viewModelScope.launch {
            repository.getCharacterByIdFlow(characterId).collect { char ->
                _character.value = char
            }
        }
    }
    
    private fun loadGroupCharacters() {
        viewModelScope.launch {
            repository.getCharacterByIdFlow(characterId).collect { currentChar ->
                if (currentChar != null) {
                    repository.allCharacters.collect { allChars ->
                        // Alle Charaktere in derselben Gruppe außer dem aktuellen
                        _groupCharacters.value = allChars.filter { 
                            it.group == currentChar.group && it.id != currentChar.id 
                        }
                    }
                }
            }
        }
    }
    
    fun transferPotionToCharacter(potion: Potion, targetCharacterId: Long) {
        viewModelScope.launch {
            // Prüfe ob Zielcharakter in gleicher Gruppe ist
            val currentChar = _character.value
            val targetChar = repository.getCharacterById(targetCharacterId)
            
            if (currentChar == null || targetChar == null) {
                return@launch
            }
            
            if (currentChar.group != targetChar.group) {
                return@launch // Nur innerhalb der Gruppe erlaubt
            }
            
            // Prüfe ob Trank mit dieser GUID bereits beim Ziel existiert
            val targetPotions = repository.getPotionsForCharacter(targetCharacterId).first()
            val alreadyExists = targetPotions.any { it.potion.guid == potion.guid }
            
            if (!alreadyExists) {
                // Trank zum Ziel hinzufügen (neue ID, aber gleiche GUID)
                val transferredPotion = potion.copy(
                    id = 0, // Neue ID wird generiert
                    characterId = targetCharacterId
                )
                repository.insertPotion(transferredPotion)
                
                // Original vom aktuellen Charakter entfernen
                repository.deletePotion(potion)
            }
        }
    }
    
    fun addPotion(recipeId: Long, actualQuality: PotionQuality, appearance: String, expiryDate: String) {
        viewModelScope.launch {
            val potion = Potion(
                characterId = characterId,
                recipeId = recipeId,
                actualQuality = actualQuality,
                appearance = appearance,
                expiryDate = expiryDate
            )
            repository.insertPotion(potion)
        }
    }
    
    fun deletePotion(potion: Potion) {
        viewModelScope.launch {
            repository.deletePotion(potion)
        }
    }
    
    fun updatePotion(potion: Potion) {
        viewModelScope.launch {
            repository.updatePotion(potion)
        }
    }
    
    fun getRecipeKnowledge(recipeId: Long): Flow<RecipeKnowledge?> {
        return repository.getRecipeKnowledgeForCharacter(characterId)
            .combine(repository.allRecipes) { knowledgeList, _ ->
                knowledgeList.firstOrNull { it.recipeId == recipeId }
            }
    }
    
    fun setRecipeKnowledge(characterId: Long, recipeId: Long, knowledgeLevel: RecipeKnowledgeLevel) {
        viewModelScope.launch {
            repository.updateRecipeKnowledgeLevel(characterId, recipeId, knowledgeLevel)
        }
    }
    
    /**
     * Passt die aktuelle AE eines Charakters an (für Magisches Meisterhandwerk)
     */
    fun adjustCurrentAe(characterId: Long, delta: Int) {
        viewModelScope.launch {
            val character = repository.getCharacterById(characterId)
            if (character != null) {
                val newAe = (character.currentAe + delta).coerceIn(0, character.maxAe)
                repository.updateCharacter(character.copy(currentAe = newAe))
            }
        }
    }
}

class PotionViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PotionViewModel::class.java)) {
            return PotionViewModel(repository, characterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
