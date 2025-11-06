package de.applicatus.app.ui.navigation

sealed class Screen(val route: String) {
    object CharacterList : Screen("character_list")
    object CharacterHome : Screen("character_home/{characterId}") {
        fun createRoute(characterId: Long) = "character_home/$characterId"
    }
    object CharacterDetail : Screen("character_detail/{characterId}") {
        fun createRoute(characterId: Long) = "character_detail/$characterId"
    }
    object PotionScreen : Screen("potions/{characterId}") {
        fun createRoute(characterId: Long) = "potions/$characterId"
    }
    object RecipeKnowledgeScreen : Screen("recipe_knowledge/{characterId}") {
        fun createRoute(characterId: Long) = "recipe_knowledge/$characterId"
    }
    object NearbySync : Screen("nearby_sync?characterId={characterId}&characterName={characterName}") {
        // Mit characterId für Senden von spezifischem Charakter
        fun createRoute(characterId: Long, characterName: String) = 
            "nearby_sync?characterId=$characterId&characterName=${java.net.URLEncoder.encode(characterName, "UTF-8")}"
        
        // Ohne Parameter für Empfangen (von CharacterListScreen)
        fun createRouteForReceive() = "nearby_sync"
    }
}
