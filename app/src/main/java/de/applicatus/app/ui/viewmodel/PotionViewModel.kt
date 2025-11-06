package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.Potion
import de.applicatus.app.data.model.PotionQuality
import de.applicatus.app.data.model.PotionWithRecipe
import de.applicatus.app.data.model.Recipe
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PotionViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    private val _potions = MutableStateFlow<List<PotionWithRecipe>>(emptyList())
    val potions: StateFlow<List<PotionWithRecipe>> = _potions.asStateFlow()
    
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()
    
    init {
        loadPotions()
        loadRecipes()
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
    
    fun addPotion(recipeId: Long, quality: PotionQuality, expiryDate: String) {
        viewModelScope.launch {
            val potion = Potion(
                characterId = characterId,
                recipeId = recipeId,
                quality = quality,
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
