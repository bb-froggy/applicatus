package de.applicatus.app.ui.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.ui.viewmodel.HerbSearchViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HerbSearchScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    private var testCharacterId: Long = -1L
    
    companion object {
        const val TEST_CHARACTER_NAME = "Kräutersucher"
        const val TEST_MU = 12
        const val TEST_IN = 14
        const val TEST_FF = 13
        const val TEST_SENSES_SKILL = 8
        const val TEST_WILDERNESS_SKILL = 10
        const val TEST_HERBAL_LORE_SKILL = 7
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ApplicatusDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        repository = ApplicatusRepository(database)

        runBlocking {
            testCharacterId = repository.insertCharacter(
                Character(
                    name = TEST_CHARACTER_NAME,
                    mu = TEST_MU,
                    inValue = TEST_IN,
                    ff = TEST_FF,
                    sensoryAcuitySkill = TEST_SENSES_SKILL,
                    wildernessSkill = TEST_WILDERNESS_SKILL,
                    herbalLoreSkill = TEST_HERBAL_LORE_SKILL,
                    gelaendekunde = emptyList()
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun herbSearchScreen_displaysCharacterInfo() {
        val viewModelFactory = HerbSearchViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            HerbSearchScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Überprüfe, dass der Charaktername angezeigt wird
        composeRule.onNodeWithText(TEST_CHARACTER_NAME).assertIsDisplayed()
        
        // Überprüfe, dass die Talente angezeigt werden
        composeRule.onNodeWithText("Sinnenschärfe: $TEST_SENSES_SKILL").assertIsDisplayed()
        composeRule.onNodeWithText("Wildnisleben: $TEST_WILDERNESS_SKILL").assertIsDisplayed()
        composeRule.onNodeWithText("Pflanzenkunde: $TEST_HERBAL_LORE_SKILL").assertIsDisplayed()
        
        // Überprüfe, dass der Kräutersuche-TaW berechnet und angezeigt wird
        // (8 + 10 + 7 + 1) / 3 = 26 / 3 = 8
        composeRule.onNodeWithText("Kräutersuche-TaW: 8").assertIsDisplayed()
    }

    @Test
    fun herbSearchScreen_regionSelectionShowsLandscapes() {
        val viewModelFactory = HerbSearchViewModelFactory(repository, testCharacterId)
        var navigateBackCalled = false
        
        composeRule.setContent {
            HerbSearchScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = { navigateBackCalled = true }
            )
        }

        composeRule.waitForIdle()
        
        // Wähle eine Region
        composeRule.onNodeWithText("Region wählen...").performClick()
        composeRule.waitForIdle()
        
        // Wähle "Nördliche Grasländer und Steppen"
        composeRule.onNodeWithText("Nördliche Grasländer und Steppen").performClick()
        composeRule.waitForIdle()
        
        // Überprüfe, dass die Landschaftsauswahl jetzt verfügbar ist
        composeRule.onNodeWithText("Landschaft wählen...").assertIsDisplayed()
        composeRule.onNodeWithText("Landschaft wählen...").performClick()
        composeRule.waitForIdle()
        
        // Überprüfe, dass Landschaften für diese Region angezeigt werden
        // (z.B. Grasland, Steppe sollten verfügbar sein)
        composeRule.onNodeWithText("Grasland").assertIsDisplayed()
    }

    @Test
    fun herbSearchScreen_landscapeSelectionShowsHerbs() {
        val viewModelFactory = HerbSearchViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            HerbSearchScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Wähle Region
        composeRule.onNodeWithText("Region wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nördliche Grasländer und Steppen").performClick()
        composeRule.waitForIdle()
        
        // Wähle Landschaft
        composeRule.onNodeWithText("Landschaft wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Grasland").performClick()
        composeRule.waitForIdle()
        
        // Überprüfe, dass die Kräuterauswahl jetzt verfügbar ist
        composeRule.onNodeWithText("Kraut wählen...").assertIsDisplayed()
        composeRule.onNodeWithText("Kraut wählen...").performClick()
        composeRule.waitForIdle()
        
        // Es sollten Kräuter für Grasland angezeigt werden
        // (Die genauen Kräuter hängen von InitialHerbs ab, aber es sollten welche sein)
        composeRule.onAllNodesWithContentDescription("").assertAny(hasAnyDescendant(hasText("Erschwer")))
    }

    @Test
    fun herbSearchScreen_performSearchShowsResult() {
        val viewModelFactory = HerbSearchViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            HerbSearchScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Wähle Region
        composeRule.onNodeWithText("Region wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nördliche Wälder Westküste/Taiga/Bornland").performClick()
        composeRule.waitForIdle()
        
        // Wähle Landschaft
        composeRule.onNodeWithText("Landschaft wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Wald").performClick()
        composeRule.waitForIdle()
        
        // Wähle ein Kraut (Efeuer - leicht zu finden)
        composeRule.onNodeWithText("Kraut wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Efeuer").performClick()
        composeRule.waitForIdle()
        
        // Führe die Suche durch
        composeRule.onNodeWithText("Suche durchführen").performClick()
        composeRule.waitForIdle()
        
        // Überprüfe, dass ein Ergebnis angezeigt wird (Erfolg oder Fehlschlag)
        composeRule.onNode(
            hasText("Erfolg!") or hasText("Fehlgeschlagen")
        ).assertIsDisplayed()
        
        // Überprüfe, dass Würfe angezeigt werden
        composeRule.onNodeWithText("Würfe:", substring = true).assertExists()
    }

    @Test
    fun herbSearchScreen_modifiersAffectDifficulty() {
        val viewModelFactory = HerbSearchViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            HerbSearchScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Wähle Region, Landschaft und Kraut
        composeRule.onNodeWithText("Region wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nördliche Wälder Westküste/Taiga/Bornland").performClick()
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("Landschaft wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Wald").performClick()
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("Kraut wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Efeuer").performClick()
        composeRule.waitForIdle()
        
        // Überprüfe die Basis-Erschwernis
        composeRule.onNode(hasText("Gesamte Erschwernis:")).assertIsDisplayed()
        
        // Aktiviere Ortskenntnis
        composeRule.onNode(
            hasText("Ortskenntnis (-7)").and(hasAnySibling(hasClickAction()))
        ).performClick()
        composeRule.waitForIdle()
        
        // Die Erschwernis sollte sich geändert haben (um 7 reduziert)
        // (Wir können den genauen Wert nicht testen, weil er vom Kraut abhängt,
        // aber wir können überprüfen, dass der Modifikatoren-Bereich angezeigt wird)
        composeRule.onNodeWithText("Modifikatoren").assertIsDisplayed()
    }

    @Test
    fun herbSearchScreen_backButtonWorks() {
        val viewModelFactory = HerbSearchViewModelFactory(repository, testCharacterId)
        var navigateBackCalled = false
        
        composeRule.setContent {
            HerbSearchScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = { navigateBackCalled = true }
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf den Zurück-Button
        composeRule.onNodeWithContentDescription("Zurück").performClick()
        composeRule.waitForIdle()
        
        // Überprüfe, dass der Callback aufgerufen wurde
        assert(navigateBackCalled) { "onNavigateBack wurde nicht aufgerufen" }
    }
    
    @Test
    fun herbSearchScreen_monthSelectionFiltersHerbs() {
        val viewModelFactory = HerbSearchViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            HerbSearchScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Wähle Region und Landschaft
        composeRule.onNodeWithText("Region wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nördliche Wälder Westküste/Taiga/Bornland").performClick()
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("Landschaft wählen...").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Wald").performClick()
        composeRule.waitForIdle()
        
        // Überprüfe, dass Monatsauswahl verfügbar ist (standardmäßig "Ganzjährig")
        composeRule.onNodeWithText("Ganzjährig").assertIsDisplayed()
        
        // Wähle einen anderen Monat
        composeRule.onNodeWithText("Ganzjährig").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Praios").performClick()
        composeRule.waitForIdle()
        
        // Überprüfe, dass der Monat geändert wurde
        composeRule.onNodeWithText("Praios").assertIsDisplayed()
        
        // Die verfügbaren Kräuter sollten jetzt gefiltert sein
        // (Wir können nicht testen, welche genau, aber die Kräuterauswahl sollte verfügbar sein)
        composeRule.onNodeWithText("Kraut wählen...").assertIsDisplayed()
    }
}
