package de.applicatus.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.ui.screen.CharacterDetailScreen
import de.applicatus.app.ui.screen.CharacterHomeScreen
import de.applicatus.app.ui.screen.CharacterListScreen
import de.applicatus.app.ui.screen.NearbySyncScreen
import de.applicatus.app.ui.screen.PotionScreen
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModel
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModelFactory
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModel
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModelFactory
import de.applicatus.app.ui.viewmodel.CharacterListViewModel
import de.applicatus.app.ui.viewmodel.CharacterListViewModelFactory
import de.applicatus.app.ui.viewmodel.NearbySyncViewModel
import de.applicatus.app.ui.viewmodel.NearbySyncViewModelFactory
import de.applicatus.app.ui.viewmodel.PotionViewModel
import de.applicatus.app.ui.viewmodel.PotionViewModelFactory
import java.net.URLDecoder

@Composable
fun ApplicatusNavHost(
    navController: NavHostController,
    repository: ApplicatusRepository
) {
    val context = LocalContext.current
    
    NavHost(
        navController = navController,
        startDestination = Screen.CharacterList.route
    ) {
        composable(Screen.CharacterList.route) {
            val viewModel: CharacterListViewModel = viewModel(
                factory = CharacterListViewModelFactory(repository)
            )
            CharacterListScreen(
                viewModel = viewModel,
                onCharacterClick = { characterId ->
                    navController.navigate(Screen.CharacterHome.createRoute(characterId))
                },
                onNearbySyncClick = {
                    navController.navigate(Screen.NearbySync.createRouteForReceive())
                }
            )
        }
        
        composable(
            route = Screen.CharacterHome.route,
            arguments = listOf(
                navArgument("characterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getLong("characterId") ?: return@composable
            CharacterHomeScreen(
                characterId = characterId,
                viewModelFactory = CharacterHomeViewModelFactory(repository, characterId),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSpells = { charId ->
                    navController.navigate(Screen.CharacterDetail.createRoute(charId))
                },
                onNavigateToPotions = { charId ->
                    navController.navigate(Screen.PotionScreen.createRoute(charId))
                }
            )
        }
        
        composable(
            route = Screen.CharacterDetail.route,
            arguments = listOf(
                navArgument("characterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getLong("characterId") ?: return@composable
            val viewModel: CharacterDetailViewModel = viewModel(
                factory = CharacterDetailViewModelFactory(repository, characterId)
            )
            CharacterDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNearbySync = { charId, charName ->
                    navController.navigate(Screen.NearbySync.createRoute(charId, charName))
                }
            )
        }
        
        composable(
            route = Screen.PotionScreen.route,
            arguments = listOf(
                navArgument("characterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getLong("characterId") ?: return@composable
            PotionScreen(
                characterId = characterId,
                viewModelFactory = PotionViewModelFactory(repository, characterId),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.NearbySync.route,
            arguments = listOf(
                navArgument("characterId") { 
                    type = NavType.LongType
                    defaultValue = -1L  // -1 bedeutet: Kein Charakter ausgewählt (Empfangsmodus)
                    nullable = false
                },
                navArgument("characterName") { 
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getLong("characterId") ?: -1L
            val characterName = backStackEntry.arguments?.getString("characterName")?.let {
                if (it.isNotEmpty()) URLDecoder.decode(it, "UTF-8") else ""
            } ?: ""
            
            val viewModel: NearbySyncViewModel = viewModel(
                factory = NearbySyncViewModelFactory(repository, context)
            )
            
            NearbySyncScreen(
                viewModel = viewModel,
                characterId = if (characterId != -1L) characterId else null,  // null = Empfangsmodus
                characterName = characterName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
