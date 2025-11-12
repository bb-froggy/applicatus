package de.applicatus.app.ui.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.InitialRecipes
import de.applicatus.app.data.InitialSpells
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.ui.viewmodel.CharacterListViewModel
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class CharacterListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    private var arionId: Long = -1L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ApplicatusDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        repository = ApplicatusRepository(
            database.spellDao(),
            database.characterDao(),
            database.spellSlotDao(),
            database.recipeDao(),
            database.potionDao(),
            database.globalSettingsDao(),
            database.recipeKnowledgeDao(),
            database.groupDao(),
            database.itemDao(),
            database.locationDao()
        )

        runBlocking {
            // Initialisiere Initial-Daten (Zauber und Rezepte)
            database.spellDao().insertSpells(InitialSpells.getDefaultSpells())
            database.recipeDao().insertRecipes(InitialRecipes.getDefaultRecipes())
            
            arionId = repository.insertCharacter(Character(name = "Arion"))
            repository.insertCharacter(Character(name = "Bela"))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun characterListDisplaysInsertedCharacters() {
        val viewModel = CharacterListViewModel(repository)

        composeRule.setContent {
            CharacterListScreen(
                viewModel = viewModel,
                onCharacterClick = {},
                onNearbySyncClick = {}
            )
        }

        // Gib dem StateFlow Zeit, die Daten zu laden und das UI zu aktualisieren
        Thread.sleep(1000)
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("Arion").assertIsDisplayed()
        composeRule.onNodeWithText("Bela").assertIsDisplayed()
    }

    @Test
    fun clickingFabOpensAddCharacterDialog() {
        val viewModel = CharacterListViewModel(repository)

        composeRule.setContent {
            CharacterListScreen(
                viewModel = viewModel,
                onCharacterClick = {},
                onNearbySyncClick = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Neuer Charakter").performClick()
    composeRule.onNodeWithText("Neuer Charakter").assertIsDisplayed()
    composeRule.onNodeWithText("Name").assertIsDisplayed()
    }

    @Test
    fun clickingCharacterInvokesCallbackWithId() {
        val viewModel = CharacterListViewModel(repository)
        val clickedId = AtomicLong(-1L)

        composeRule.setContent {
            CharacterListScreen(
                viewModel = viewModel,
                onCharacterClick = { clickedId.set(it) },
                onNearbySyncClick = {}
            )
        }

        // Gib dem StateFlow Zeit, die Daten zu laden
        Thread.sleep(1000)
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("Arion").performClick()

        composeRule.waitForIdle()
        assertEquals("Expected click callback to receive the tapped character id", arionId, clickedId.get())
    }
}
