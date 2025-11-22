package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.RecipeKnowledge
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.repository.ApplicatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel für die Verwaltung der Rezept-Kenntnisse eines Charakters
 */
class RecipeKnowledgeViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    // Alle verfügbaren Rezepte
    val allRecipes: StateFlow<List<Recipe>> = repository.allRecipes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Rezept-Kenntnisse des Charakters
    private val recipeKnowledge: StateFlow<List<RecipeKnowledge>> = 
        repository.getRecipeKnowledgeForCharacter(characterId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    // Kombinierte Daten: Rezepte mit ihrem Wissensstatus
    data class RecipeWithKnowledge(
        val recipe: Recipe,
        val knowledgeLevel: RecipeKnowledgeLevel
    )
    
    val recipesWithKnowledge: StateFlow<List<RecipeWithKnowledge>> = 
        combine(allRecipes, recipeKnowledge) { recipes, knowledge ->
            recipes.map { recipe ->
                val knowledgeEntry = knowledge.find { it.recipeId == recipe.id }
                RecipeWithKnowledge(
                    recipe = recipe,
                    knowledgeLevel = knowledgeEntry?.knowledgeLevel ?: RecipeKnowledgeLevel.UNKNOWN
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * Setzt den Wissensgrad für ein Rezept
     */
    fun setRecipeKnowledge(recipeId: Long, level: RecipeKnowledgeLevel) {
        viewModelScope.launch {
            val existing = repository.getRecipeKnowledge(characterId, recipeId)
            val recipe = recipesWithKnowledge.value.firstOrNull { it.recipe.id == recipeId }?.recipe
            val recipeName = recipe?.name ?: "Unbekanntes Rezept"
            
            if (existing != null) {
                // Update nur wenn sich Level ändert
                if (existing.knowledgeLevel != level) {
                    repository.updateRecipeKnowledge(existing.copy(knowledgeLevel = level))
                    
                    // Journal-Eintrag
                    repository.logCharacterEvent(
                        characterId = characterId,
                        category = de.applicatus.app.data.model.character.JournalCategory.RECIPE_LEARNED,
                        playerMessage = "Rezeptwissen geändert: $recipeName",
                        gmMessage = "${existing.knowledgeLevel.name} → ${level.name}"
                    )
                }
            } else {
                // Insert
                repository.insertRecipeKnowledge(
                    de.applicatus.app.data.model.potion.RecipeKnowledge(
                        characterId = characterId,
                        recipeId = recipeId,
                        knowledgeLevel = level
                    )
                )
                
                // Journal-Eintrag
                repository.logCharacterEvent(
                    characterId = characterId,
                    category = de.applicatus.app.data.model.character.JournalCategory.RECIPE_LEARNED,
                    playerMessage = "Rezeptwissen erworben: $recipeName",
                    gmMessage = level.name
                )
            }
        }
    }
    
    /**
     * Entfernt das Wissen über ein Rezept (setzt auf UNKNOWN)
     */
    fun removeRecipeKnowledge(recipeId: Long) {
        viewModelScope.launch {
            val existing = repository.getRecipeKnowledge(characterId, recipeId)
            if (existing != null) {
                repository.deleteRecipeKnowledge(existing)
            }
        }
    }
}

class RecipeKnowledgeViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeKnowledgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeKnowledgeViewModel(repository, characterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
