package de.applicatus.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.ui.screen.CharacterDetailScreen
import de.applicatus.app.ui.screen.CharacterListScreen
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModel
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModelFactory
import de.applicatus.app.ui.viewmodel.CharacterListViewModel
import de.applicatus.app.ui.viewmodel.CharacterListViewModelFactory

@Composable
fun ApplicatusNavHost(
    navController: NavHostController,
    repository: ApplicatusRepository
) {
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
                    navController.navigate(Screen.CharacterDetail.createRoute(characterId))
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
