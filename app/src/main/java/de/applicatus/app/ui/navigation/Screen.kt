package de.applicatus.app.ui.navigation

sealed class Screen(val route: String) {
    object CharacterList : Screen("character_list")
    object CharacterDetail : Screen("character_detail/{characterId}") {
        fun createRoute(characterId: Long) = "character_detail/$characterId"
    }
    object NearbySync : Screen("nearby_sync/{characterId}/{characterName}") {
        fun createRoute(characterId: Long, characterName: String) = 
            "nearby_sync/$characterId/${java.net.URLEncoder.encode(characterName, "UTF-8")}"
    }
}
