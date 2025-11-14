package de.applicatus.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.PotionWithRecipe
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.RecipeKnowledge
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.model.potion.Substitution
import de.applicatus.app.data.model.talent.Talent
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.PotionBrewer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

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
    
    private val _currentGroup = MutableStateFlow<de.applicatus.app.data.model.character.Group?>(null)
    val currentGroup: StateFlow<de.applicatus.app.data.model.character.Group?> = _currentGroup.asStateFlow()
    
    init {
        loadPotions()
        loadRecipes()
        loadCharacter()
        loadGroupCharacters()
        loadCurrentGroup()
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
                        // Prüfe ob es einen Spielleiter-Charakter gibt
                        val hasGameMaster = allChars.any { it.isGameMaster }
                        
                        // Wenn ein Spielleiter existiert: Alle anderen Charaktere anzeigen
                        // Sonst: Nur Charaktere aus derselben Gruppe
                        _groupCharacters.value = if (hasGameMaster) {
                            allChars.filter { it.id != currentChar.id }
                        } else {
                            allChars.filter { 
                                it.groupId == currentChar.groupId && it.id != currentChar.id 
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun loadCurrentGroup() {
        viewModelScope.launch {
            repository.getCharacterByIdFlow(characterId).collect { char ->
                if (char?.groupId != null) {
                    repository.getGroupById(char.groupId).collect { group ->
                        _currentGroup.value = group
                    }
                }
            }
        }
    }
    
    fun transferPotionToCharacter(potion: Potion, targetCharacterId: Long) {
        viewModelScope.launch {
            // Prüfe ob Zielcharakter existiert
            val currentChar = _character.value
            val targetChar = repository.getCharacterById(targetCharacterId)
            
            if (currentChar == null || targetChar == null) {
                return@launch
            }
            
            // Prüfe ob es einen Spielleiter gibt
            val allChars = repository.allCharacters.first()
            val hasGameMaster = allChars.any { it.isGameMaster }
            
            // Gruppen-Einschränkung nur ohne Spielleiter
            if (!hasGameMaster && currentChar.groupId != targetChar.groupId) {
                return@launch // Nur innerhalb der Gruppe erlaubt (wenn kein Spielleiter)
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
    
    fun addPotion(recipeId: Long, actualQuality: PotionQuality, appearance: String, createdDate: String, expiryDate: String) {
        viewModelScope.launch {
            val potion = Potion(
                characterId = characterId,
                recipeId = recipeId,
                actualQuality = actualQuality,
                appearance = appearance,
                createdDate = createdDate,
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
    
    fun consumePotion(potion: Potion) {
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
    
    /**
     * Reduziert die AE des aktuellen Charakters
     */
    fun reduceAe(amount: Int) {
        adjustCurrentAe(characterId, -amount)
    }
    
    /**
     * Gibt die bekannten Rezepte für den Charakter zurück (KNOWN, nicht nur UNDERSTOOD)
     */
    fun getKnownRecipes(): Flow<List<Recipe>> {
        return repository.getRecipeKnowledgeForCharacter(characterId)
            .combine(repository.allRecipes) { knowledgeList, allRecipes ->
                val knownRecipeIds = knowledgeList
                    .filter { it.knowledgeLevel == RecipeKnowledgeLevel.KNOWN }
                    .map { it.recipeId }
                    .toSet()
                
                allRecipes.filter { it.id in knownRecipeIds }
            }
    }
    
    /**
     * Prüft, ob der Charakter das angegebene Talent zum Brauen nutzen kann
     */
    fun canUseBrewingTalent(talent: Talent): Boolean {
        val char = _character.value ?: return false
        return when (talent) {
            Talent.ALCHEMY -> char.hasAlchemy
            Talent.COOKING_POTIONS -> char.hasCookingPotions
            else -> false
        }
    }
    
    /**
     * Gibt die verfügbaren Brau-Talente für den Charakter zurück
     */
    fun getAvailableBrewingTalents(): List<Talent> {
        val char = _character.value ?: return emptyList()
        val talents = mutableListOf<Talent>()
        if (char.hasAlchemy) talents.add(Talent.ALCHEMY)
        if (char.hasCookingPotions) talents.add(Talent.COOKING_POTIONS)
        return talents
    }
    
    /**
     * Braut einen Trank und fügt ihn zur Charakterliste hinzu
     * 
     * @param recipe Das Rezept
     * @param talent Das verwendete Talent (ALCHEMY oder COOKING_POTIONS)
     * @param laboratory Das verfügbare Labor
     * @param voluntaryHandicap Freiwilliger Handicap (0 oder min. 2)
     * @param substitutions Liste der Substitutionen
     * @param magicalMasteryAsp AsP für Magisches Meisterhandwerk (TaW-Erhöhung)
     * @param astralCharging Anzahl der Qualitätspunkte durch astrale Aufladung
     * @return Das Brau-Ergebnis
     */
    suspend fun brewPotion(
        recipe: Recipe,
        talent: Talent,
        laboratory: Laboratory,
        voluntaryHandicap: Int = 0,
        substitutions: List<Substitution> = emptyList(),
        magicalMasteryAsp: Int = 0,
        astralCharging: Int = 0
    ): PotionBrewer.BrewingResult {
        val char = _character.value ?: throw IllegalStateException("Character not loaded")
        
        // Prüfe, ob der Charakter das Rezept kennt
        val knownRecipes = getKnownRecipes().first()
        if (!knownRecipes.any { it.id == recipe.id }) {
            throw IllegalStateException("Rezept nicht bekannt")
        }
        
        // Prüfe, ob das Talent verfügbar ist
        if (!canUseBrewingTalent(talent)) {
            throw IllegalStateException("Talent nicht verfügbar")
        }
        
        // Prüfe, ob das Labor ausreichend ist
        if (!PotionBrewer.canBrew(recipe, laboratory)) {
            throw IllegalStateException("Labor nicht ausreichend")
        }
        
        // Berechne AsP-Kosten (Magisches Meisterhandwerk + Astrale Aufladung)
        val astralChargingCost = PotionBrewer.calculateAspCostForQualityPoints(astralCharging)
        val totalAspCost = magicalMasteryAsp + astralChargingCost
        
        if (char.hasAe && totalAspCost > char.currentAe) {
            throw IllegalStateException("Nicht genug AsP (benötigt $totalAspCost, verfügbar ${char.currentAe})")
        }
        
        // Führe Brauprobe durch
        val result = PotionBrewer.brewPotion(
            character = char,
            recipe = recipe,
            talent = talent,
            availableLaboratory = laboratory,
            voluntaryHandicap = voluntaryHandicap,
            substitutions = substitutions,
            magicalMasteryAsp = magicalMasteryAsp,
            astralCharging = astralCharging
        )
        
        // AsP abziehen (falls verwendet)
        if (char.hasAe && totalAspCost > 0) {
            val updatedChar = char.copy(currentAe = char.currentAe - totalAspCost)
            repository.updateCharacter(updatedChar)
        }
        
        // Zufälliges Aussehen generieren (falls nicht im Rezept definiert)
        val appearance = if (recipe.appearance.isBlank()) {
            generateRandomAppearance()
        } else {
            recipe.appearance
        }
        
        // Berechne Ablaufdatum basierend auf aktuellem derischen Datum und Haltbarkeit
        val group = repository.getGroupForCharacter(char.id).first() 
            ?: throw IllegalStateException("Gruppe nicht gefunden")
        val currentDate = group.currentDerianDate
        val calculatedExpiryDate = de.applicatus.app.logic.DerianDateCalculator.calculateExpiryDate(
            currentDate, 
            recipe.shelfLife
        )
        
        // Trank zur Datenbank hinzufügen
        // Beim Brauen sind Name und Kategorie immer bekannt
        val potion = Potion(
            characterId = characterId,
            recipeId = recipe.id,
            actualQuality = result.quality,
            appearance = appearance,
            createdDate = currentDate,
            expiryDate = calculatedExpiryDate,
            preservationAttempted = false,
            nameKnown = true,      // Gebraute Tränke haben bekannten Namen
            categoryKnown = true   // Gebraute Tränke haben bekannte Kategorie
        )
        repository.insertPotion(potion)
        
        return result
    }
    
    /**
     * Generiert ein zufälliges Aussehen für einen Trank
     */
    private fun generateRandomAppearance(): String {
        val colors = listOf(
            "klar", "trüb", "goldgelb", "bernsteinfarben", "smaragdgrün", 
            "rubinrot", "saphirblau", "violett", "silbrig", "kupferfarben",
            "milchig", "schimmernd", "dunkelrot", "hellblau", "grünlich"
        )
        val properties = listOf(
            "glitzernd", "schimmernd", "leuchtend", "dampfend", "blubbernde",
            "ölig", "dickflüssig", "wässrig", "klebrig", "sirupartig"
        )
        val color = colors.random()
        val property = properties.random()
        return "$color, $property"
    }
    
    /**
     * Aktualisiert das Standard-Labor des Charakters
     */
    fun updateDefaultLaboratory(laboratory: Laboratory?) {
        viewModelScope.launch {
            val char = _character.value ?: return@launch
            repository.updateCharacter(char.copy(defaultLaboratory = laboratory))
        }
    }
    
    /**
     * Verdünnt einen Trank qualifiziert
     * 
     * @param potion Der zu verdünnende Trank
     * @param talent Das verwendete Talent (ALCHEMY oder COOKING_POTIONS)
     * @param dilutionSteps Um wie viele Stufen soll verdünnt werden (1-6)
     * @param magicalMasteryAsp AsP für Magisches Meisterhandwerk
     * @return Ergebnis der Verdünnungsprobe
     */
    suspend fun dilutePotion(
        potion: Potion,
        talent: Talent,
        dilutionSteps: Int,
        magicalMasteryAsp: Int = 0
    ): PotionBrewer.DilutionResult {
        val char = _character.value ?: throw IllegalStateException("Kein Charakter geladen")
        val recipe = repository.getRecipeById(potion.recipeId) ?: throw IllegalStateException("Rezept nicht gefunden")
        
        // Erleichterung aus vorheriger Analyse berechnen (halbe Punkte aufgerundet)
        val facilitationFromAnalysis = (potion.bestStructureAnalysisFacilitation + 1) / 2
        
        // Verdünnungsprobe durchführen
        val result = PotionBrewer.dilutePotion(
            character = char,
            potion = potion,
            recipe = recipe,
            talent = talent,
            dilutionSteps = dilutionSteps,
            facilitationFromAnalysis = facilitationFromAnalysis,
            magicalMasteryAsp = magicalMasteryAsp
        )
        
        // AsP abziehen wenn Magisches Meisterhandwerk verwendet wurde
        if (magicalMasteryAsp > 0) {
            adjustCurrentAe(characterId, -magicalMasteryAsp)
        }
        
        // Nachträglich eingesetzte AsP abziehen
        if (result.retroactiveAspUsed > 0) {
            adjustCurrentAe(characterId, -result.retroactiveAspUsed)
        }
        
        // Originalen Trank löschen
        repository.deletePotion(potion)
        
        // Neue Tränke erstellen
        repeat(result.numberOfPotions) {
            val newPotion = potion.copy(
                id = 0, // Neue ID generieren
                guid = UUID.randomUUID().toString(), // Neue GUID
                actualQuality = result.newQuality,
                // Aussehen, Haltbarkeit und Analyse-Status bleiben erhalten
            )
            repository.insertPotion(newPotion)
        }
        
        return result
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
