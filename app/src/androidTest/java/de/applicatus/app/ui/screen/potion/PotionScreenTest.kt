package de.applicatus.app.ui.screen.potion

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.*
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.ui.viewmodel.PotionViewModel
import de.applicatus.app.ui.viewmodel.PotionViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class PotionScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    private var testCharacterId: Long = -1L
    private var testRecipeId: Long = -1L

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
            // Lösche alle Initial-Rezepte, die beim Datenbankstart eingefügt wurden
            repository.allRecipes.first().forEach { recipe ->
                repository.deleteRecipe(recipe)
            }
            
            testCharacterId = repository.insertCharacter(
                Character(
                    name = "Alchemist",
                    mu = 12,
                    kl = 14,
                    inValue = 13,
                    ch = 11,
                    ff = 15,
                    ge = 12,
                    ko = 13,
                    kk = 11,
                    maxLe = 30,
                    currentLe = 30,
                    hasAe = true,
                    maxAe = 40,
                    currentAe = 40,
                    hasKe = true,
                    maxKe = 35,
                    currentKe = 35,
                    hasAlchemy = true,
                    alchemySkill = 10  // Alchimie-Skill für Analyse-Tests
                )
            )

            testRecipeId = repository.insertRecipe(
                Recipe(
                    name = "Heiltrank",
                    brewingDifficulty = 0,
                    analysisDifficulty = 0,
                    appearance = "goldgelb",
                    shelfLife = "6 Monde"
                )
            )

            // Füge einen Trank hinzu
            repository.insertPotion(
                Potion(
                    characterId = testCharacterId,
                    recipeId = testRecipeId,
                    actualQuality = PotionQuality.C,
                    expiryDate = "1050 BF, Praios",
                    shelfLifeKnown = true  // Haltbarkeit ist bekannt, damit sie angezeigt wird
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun potionScreen_displaysTitle() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hexenküche").assertIsDisplayed()
    }

    @Test
    fun potionScreen_displaysPotions() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Heiltrank").assertIsDisplayed()
    }

    @Test
    fun potionScreen_displaysPotionQuality() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        // Qualität ist unbekannt, da kein Analysewissen gesetzt ist
        composeRule.onNodeWithText("Qualität: Unbekannt", substring = true).assertIsDisplayed()
    }

    @Test
    fun potionScreen_addButtonOpensDialog() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Add-Button
        composeRule.onNodeWithContentDescription("Trank hinzufügen").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, ob Dialog geöffnet wurde
        composeRule.onNodeWithText("Trank hinzufügen").assertIsDisplayed()
    }

    @Test
    fun potionScreen_backButtonNavigates() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)
        var navigatedBack = false

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = { navigatedBack = true }
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Zurück-Button
        composeRule.onNodeWithContentDescription("Abbrechen").performClick()
        
        composeRule.waitForIdle()
        
        assertTrue("Sollte zurück navigieren", navigatedBack)
    }

    @Test
    fun potionScreen_deleteButtonShowsConfirmDialog() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Delete-Button
        composeRule.onNodeWithContentDescription("Trank löschen").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, ob Bestätigungsdialog angezeigt wird
        composeRule.onNodeWithText("Trank löschen").assertIsDisplayed()
        composeRule.onNodeWithText("Möchten Sie diesen Trank wirklich löschen?").assertIsDisplayed()
    }

    @Test
    fun potionScreen_analyzeButtonOpensDialog() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Analyse-Button (Button mit Text, nicht contentDescription)
        composeRule.onNodeWithText("Trank analysieren").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, ob Analyse-Dialog geöffnet wurde
        // Der Dialog sollte jetzt "Analysemethode" oder die Methoden anzeigen
        composeRule.onNodeWithText("Analysemethode", substring = true).assertIsDisplayed()
    }

    @Test
    fun potionScreen_showsEmptyStateWhenNoPotions() {
        runBlocking {
            // Lösche alle Tränke
            repository.deletePotionsForCharacter(testCharacterId)
        }
        
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Überprüfe Empty State
        composeRule.onNodeWithText("Keine Tränke vorhanden").assertIsDisplayed()
    }

    @Test
    fun potionScreen_recipeKnowledgeButtonNavigates() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)
        var navigatedToRecipes = false

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {},
                onNavigateToRecipeKnowledge = { navigatedToRecipes = true }
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Rezepte-Button
        composeRule.onNodeWithText("Rezepte").performClick()
        
        composeRule.waitForIdle()
        
        assertTrue("Sollte zu Rezepten navigieren", navigatedToRecipes)
    }

    @Test
    fun potionScreen_showsExpiryDate() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Überprüfe, dass Haltbarkeit angezeigt wird (aus dem Rezept, da shelfLifeKnown=true)
        composeRule.onNodeWithText("Haltbarkeit: 6 Monde", substring = true).assertIsDisplayed()
    }

    @Test
    fun potionScreen_displaysAnalysisStatus() {
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Überprüfe Analyse-Status - Format ist "Status: Nicht analysiert"
        composeRule.onNodeWithText("Status: Nicht analysiert").assertIsDisplayed()
    }

    @Test
    fun potionScreen_addDialogShowsNoRecipesMessage() {
        runBlocking {
            // Lösche alle Rezepte
            val recipe = repository.getRecipeById(testRecipeId)
            if (recipe != null) {
                repository.deleteRecipe(recipe)
            }
        }
        
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Add-Button
        composeRule.onNodeWithContentDescription("Trank hinzufügen").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe "Keine Rezepte"-Nachricht
        composeRule.onNodeWithText("Keine Rezepte vorhanden").assertIsDisplayed()
    }

    @Test
    fun potionScreen_multipleQualitiesDisplayCorrectly() {
        runBlocking {
            // Füge Tränke verschiedener Qualität hinzu
            repository.insertPotion(
                Potion(
                    characterId = testCharacterId,
                    recipeId = testRecipeId,
                    actualQuality = PotionQuality.A,
                    expiryDate = "1050 BF, Praios"
                )
            )
            
            repository.insertPotion(
                Potion(
                    characterId = testCharacterId,
                    recipeId = testRecipeId,
                    actualQuality = PotionQuality.B,
                    expiryDate = "1050 BF, Praios"
                )
            )
        }
        
        val viewModelFactory = PotionViewModelFactory(repository, testCharacterId)

        composeRule.setContent {
            PotionScreen(
                characterId = testCharacterId,
                viewModelFactory = viewModelFactory,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Alle drei Tränke sollten angezeigt werden (mit dem Rezeptnamen)
        // Da kein Analysewissen gesetzt ist, zeigen alle "Qualität: Unbekannt"
        val potionCards = composeRule.onAllNodesWithText("Heiltrank", substring = true)
        potionCards.assertCountEquals(3)
        
        // Überprüfe, dass die Qualität unbekannt angezeigt wird (3x)
        val qualityNodes = composeRule.onAllNodesWithText("Qualität: Unbekannt", substring = true)
        qualityNodes.assertCountEquals(3)
    }
}
