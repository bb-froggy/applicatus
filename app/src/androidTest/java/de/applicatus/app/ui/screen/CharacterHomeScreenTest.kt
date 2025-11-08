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
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModel
import de.applicatus.app.ui.viewmodel.CharacterHomeViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class CharacterHomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    private var testCharacterId: Long = -1L
    
    // Test-Konstanten für konsistente Werte
    companion object {
        const val TEST_CHARACTER_NAME = "Testcharakter"
        const val TEST_MU = 12
        const val TEST_KL = 13
        const val TEST_IN = 14
        const val TEST_CH = 15
        const val TEST_FF = 16
        const val TEST_GE = 17
        const val TEST_KO = 18
        const val TEST_KK = 19
        const val TEST_MAX_LE = 30
        const val TEST_CURRENT_LE = 25
        const val TEST_MAX_AE = 35
        const val TEST_CURRENT_AE = 30
        const val TEST_MAX_KE = 40
        const val TEST_CURRENT_KE = 35
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ApplicatusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ApplicatusRepository(
            database.spellDao(),
            database.characterDao(),
            database.spellSlotDao(),
            database.recipeDao(),
            database.potionDao(),
            database.globalSettingsDao(),
            database.recipeKnowledgeDao()
        )

        runBlocking {
            testCharacterId = repository.insertCharacter(
                Character(
                    name = TEST_CHARACTER_NAME,
                    mu = TEST_MU,
                    kl = TEST_KL,
                    inValue = TEST_IN,
                    ch = TEST_CH,
                    ff = TEST_FF,
                    ge = TEST_GE,
                    ko = TEST_KO,
                    kk = TEST_KK,
                    maxLe = TEST_MAX_LE,
                    currentLe = TEST_CURRENT_LE,
                    hasAe = true,
                    maxAe = TEST_MAX_AE,
                    currentAe = TEST_CURRENT_AE,
                    hasKe = true,
                    maxKe = TEST_MAX_KE,
                    currentKe = TEST_CURRENT_KE
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun characterHomeScreen_displaysCharacterName() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(TEST_CHARACTER_NAME).assertIsDisplayed()
    }

    @Test
    fun characterHomeScreen_displaysCharacterProperties() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Überprüfe, ob Eigenschaften angezeigt werden (verwende Konstanten)
        composeRule.onNodeWithText("MU").assertIsDisplayed()
        composeRule.onNodeWithText("KL").assertIsDisplayed()
        composeRule.onNodeWithText(TEST_IN.toString()).assertIsDisplayed()
        composeRule.onNodeWithText(TEST_CH.toString()).assertIsDisplayed()
        composeRule.onNodeWithText(TEST_FF.toString()).assertIsDisplayed()
        composeRule.onNodeWithText(TEST_GE.toString()).assertIsDisplayed()
    }

    @Test
    fun characterHomeScreen_displaysCurrentEnergies() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Überprüfe LE (nicht LeP) mit Konstanten
        composeRule.onNodeWithText("LE").assertIsDisplayed()
        composeRule.onNodeWithText("$TEST_CURRENT_LE / $TEST_MAX_LE").assertIsDisplayed()
        
        // Überprüfe AE (nicht AsP) mit Konstanten
        composeRule.onNodeWithText("AE").assertIsDisplayed()
        composeRule.onNodeWithText("$TEST_CURRENT_AE / $TEST_MAX_AE").assertIsDisplayed()
        
        // Überprüfe KE (nicht KaP) mit Konstanten
        composeRule.onNodeWithText("KE").assertIsDisplayed()
        composeRule.onNodeWithText("$TEST_CURRENT_KE / $TEST_MAX_KE").assertIsDisplayed()
    }

    @Test
    fun characterHomeScreen_increaseLeButtonWorks() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Prüfe initial, dass der Wert korrekt ist
        composeRule.onNodeWithText("$TEST_CURRENT_LE / $TEST_MAX_LE").assertIsDisplayed()
        
        // Finde den + Button in der LE Sektion (Buttons zeigen "+", nicht "+1")
        composeRule.onAllNodesWithText("+")[0].performClick()
        
        // Warte auf UI-Update mit Timeout
        composeRule.waitUntil(timeoutMillis = 2000) {
            composeRule.onAllNodesWithText("${TEST_CURRENT_LE + 1} / $TEST_MAX_LE")
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Überprüfe, ob LE erhöht wurde (mit Konstanten)
        composeRule.onNodeWithText("${TEST_CURRENT_LE + 1} / $TEST_MAX_LE").assertIsDisplayed()
    }

    @Test
    fun characterHomeScreen_decreaseAeButtonWorks() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Finde den - Button in der AE Sektion (Buttons zeigen "-", nicht "-1")
        composeRule.onAllNodesWithText("-")[1].performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, ob AE verringert wurde (mit Konstanten)
        composeRule.onNodeWithText("${TEST_CURRENT_AE - 1} / $TEST_MAX_AE").assertIsDisplayed()
    }

    @Test
    fun characterHomeScreen_cannotExceedMaxLe() {
        runBlocking {
            // Setze LeP auf Maximum
            val char = repository.getCharacterById(testCharacterId)!!
            repository.updateCharacter(char.copy(currentLe = char.maxLe))
        }
        
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Versuche, LE zu erhöhen (Button zeigt "+", nicht "+1")
        composeRule.onAllNodesWithText("+")[0].performClick()
        
        composeRule.waitForIdle()
        
        // LE sollte immer noch am Maximum sein (mit Konstanten)
        composeRule.onNodeWithText("$TEST_MAX_LE / $TEST_MAX_LE").assertIsDisplayed()
    }

    @Test
    fun characterHomeScreen_editButtonOpensDialog() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Edit-Button
        composeRule.onNodeWithContentDescription("Charakter bearbeiten").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, ob Dialog geöffnet wurde
        composeRule.onNodeWithText("Charakter bearbeiten").assertIsDisplayed()
    }

    @Test
    fun characterHomeScreen_spellStorageButtonNavigates() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        var navigatedToSpellStorage = false
        var navigatedCharacterId = -1L
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = { id ->
                    navigatedToSpellStorage = true
                    navigatedCharacterId = id
                },
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Zauberspeicher-Button
        composeRule.onNodeWithText("Zauberspeicher").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe Navigation
        assertTrue("Sollte zu Zauberspeicher navigieren", navigatedToSpellStorage)
        assertEquals("Sollte korrekte Character-ID übergeben", testCharacterId, navigatedCharacterId)
    }

    @Test
    fun characterHomeScreen_potionsButtonNavigates() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        var navigatedToPotions = false
        var navigatedCharacterId = -1L
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = { id ->
                    navigatedToPotions = true
                    navigatedCharacterId = id
                }
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Hexenküche-Button
        composeRule.onNodeWithText("Hexenküche").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe Navigation
        assertTrue("Sollte zur Hexenküche navigieren", navigatedToPotions)
        assertEquals("Sollte korrekte Character-ID übergeben", testCharacterId, navigatedCharacterId)
    }

    @Test
    fun characterHomeScreen_backButtonNavigates() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        var navigatedBack = false
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = { navigatedBack = true },
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Zurück-Button
        composeRule.onNodeWithContentDescription("Abbrechen").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe Navigation
        assertTrue("Sollte zurück navigieren", navigatedBack)
    }

    @Test
    fun characterHomeScreen_regenerationDialogOpens() {
        val viewModelFactory = CharacterHomeViewModelFactory(repository, testCharacterId)
        
        composeRule.setContent {
            CharacterHomeScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToSpellStorage = {},
                onNavigateToPotions = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Regeneration-Button (jetzt ein Icon mit contentDescription)
        composeRule.onNodeWithContentDescription("Regeneration").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, ob Regenerations-Dialog geöffnet wurde (Text ist "Regen-Mod" nicht "Modifikator")
        composeRule.onNodeWithText("Regen-Mod").assertIsDisplayed()
    }
}
