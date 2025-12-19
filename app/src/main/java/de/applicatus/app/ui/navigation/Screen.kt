package de.applicatus.app.ui.navigation

sealed class Screen(val route: String) {
    object CharacterList : Screen("character_list")
    object CharacterHome : Screen("character_home/{characterId}") {
        fun createRoute(characterId: Long) = "character_home/$characterId"
    }
    object SpellStorage : Screen("spell_storage/{characterId}") {
        fun createRoute(characterId: Long) = "spell_storage/$characterId"
    }
    object PotionScreen : Screen("potions/{characterId}") {
        fun createRoute(characterId: Long) = "potions/$characterId"
    }
    object RecipeKnowledgeScreen : Screen("recipe_knowledge/{characterId}") {
        fun createRoute(characterId: Long) = "recipe_knowledge/$characterId"
    }
    object InventoryScreen : Screen("inventory/{characterId}") {
        fun createRoute(characterId: Long) = "inventory/$characterId"
    }
    object CharacterJournal : Screen("character_journal/{characterId}") {
        fun createRoute(characterId: Long) = "character_journal/$characterId"
    }
    object NearbySync : Screen("nearby_sync?characterId={characterId}&characterName={characterName}") {
        // Mit characterId für Senden von spezifischem Charakter
        fun createRoute(characterId: Long, characterName: String) = 
            "nearby_sync?characterId=$characterId&characterName=${java.net.URLEncoder.encode(characterName, "UTF-8")}"
        
        // Ohne Parameter für Empfangen (von CharacterListScreen)
        fun createRouteForReceive() = "nearby_sync"
    }
    
    object MagicSignScreen : Screen("magic_signs/{characterId}") {
        fun createRoute(characterId: Long) = "magic_signs/$characterId"
    }
    
    object HerbSearchScreen : Screen("herb_search/{characterId}") {
        fun createRoute(characterId: Long) = "herb_search/$characterId"
    }
}
