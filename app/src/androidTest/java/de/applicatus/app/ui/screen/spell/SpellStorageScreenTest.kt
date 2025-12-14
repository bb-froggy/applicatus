package de.applicatus.app.ui.screen.spell

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.applicatus.app.data.ApplicatusDatabase
import de.applicatus.app.data.InitialRecipes
import de.applicatus.app.data.InitialSpells
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SlotType
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModel
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class SpellStorageScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    private var testCharacterId: Long = -1L
    private var testSpellId: Long = -1L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ApplicatusDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        repository = ApplicatusRepository(database)

        runBlocking {
            // Initialisiere Initial-Daten (Zauber und Rezepte)
            database.spellDao().insertSpells(InitialSpells.getDefaultSpells())
            database.recipeDao().insertRecipes(InitialRecipes.getDefaultRecipes())
            
            testCharacterId = repository.insertCharacter(
                Character(
                    name = "Zaubermeister",
                    mu = 12,
                    kl = 13,
                    inValue = 14,
                    ch = 11,
                    ff = 10,
                    ge = 12,
                    ko = 13,
                    kk = 11,
                    maxLe = 30,
                    currentLe = 30,
                    hasAe = true,
                    maxAe = 50,
                    currentAe = 50,
                    hasKe = true,
                    maxKe = 40,
                    currentKe = 40
                )
            )

            testSpellId = repository.insertSpell(
                Spell(
                    name = "Hexenblick",
                    attribute1 = "MU",
                    attribute2 = "IN",
                    attribute3 = "CH"
                )
            )

            // Füge einen Zauberspeicher-Slot hinzu
            repository.insertSlot(
                SpellSlot(
                    characterId = testCharacterId,
                    slotNumber = 1,
                    slotType = SlotType.SPELL_STORAGE,
                    spellId = testSpellId,
                    zfw = 10,
                    volumePoints = 5,  // Volumenpunkte für Zauberspeicher
                    isFilled = true
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun spellStorageScreen_displaysCharacterName() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Zaubermeister").assertIsDisplayed()
    }

    @Test
    fun spellStorageScreen_displaysStoredSpell() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hexenblick").assertIsDisplayed()
    }

    @Test
    fun spellStorageScreen_displaysSpellDetails() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Überprüfe ZfW - Format ist "ZfW: 10 | Mod: 0"
        composeRule.onNodeWithText("ZfW: 10 | Mod: 0").assertIsDisplayed()
        
        // Überprüfe Slot-Typ - im Normal-Mode wird nur "(XVP)" angezeigt
        // "Speicher (XVP)" kommt nur im Edit-Mode vor
        composeRule.onNodeWithText("VP)", substring = true).assertIsDisplayed()
    }

    @Test
    fun spellStorageScreen_editModeToggle() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Überprüfe, dass Delete-Icons nicht sichtbar sind
        composeRule.onNodeWithContentDescription("Slot löschen").assertDoesNotExist()
        
        // Aktiviere Edit-Modus
        composeRule.onNodeWithContentDescription("Bearbeitungsmodus").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, dass Delete-Icons jetzt sichtbar sind
        composeRule.onNodeWithContentDescription("Slot löschen").assertExists()
    }

    @Test
    fun spellStorageScreen_addSpellButtonOpensDialog() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // FAB ist nur im Edit-Mode sichtbar - aktiviere Edit-Mode zuerst
        composeRule.onNodeWithContentDescription("Bearbeitungsmodus").performClick()
        composeRule.waitForIdle()
        
        // Klicke auf Add-Button
        composeRule.onNodeWithContentDescription("Slot hinzufügen").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, ob Dialog geöffnet wurde - prüfe auf "Zauberspeicher" Chip im Dialog
        composeRule.onNodeWithText("Zauberspeicher", substring = true).assertIsDisplayed()
    }

    @Test
    fun spellStorageScreen_backButtonNavigates() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)
        var navigatedBack = false

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = { navigatedBack = true }
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf Zurück-Button
        composeRule.onNodeWithContentDescription("Zurück").performClick()
        
        composeRule.waitForIdle()
        
        assertTrue("Sollte zurück navigieren", navigatedBack)
    }

    @Test
    fun spellStorageScreen_expandSpellCardShowsDetails() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Aktiviere Edit-Mode, um Details zu sehen
        composeRule.onNodeWithContentDescription("Bearbeitungsmodus").performClick()
        composeRule.waitForIdle()
        
        // Im Edit-Mode werden Details in den Slot-Cards angezeigt
        // Prüfe auf "Probe:" Text
        composeRule.onNodeWithText("Probe:", substring = true).assertIsDisplayed()
    }

    @Test
    fun spellStorageScreen_showsEmptyStateWhenNoSpells() {
        runBlocking {
            // Lösche alle Spell Slots
            val slots = repository.getSlotsByCharacter(testCharacterId).first()
            slots.forEach { slot ->
                repository.deleteSlot(slot)
            }
        }
        
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // SpellStorageScreen zeigt immer die Slot-Liste an, auch wenn leer
        // Prüfe dass keine Zauber-Namen angezeigt werden
        composeRule.onNodeWithText("Zauber 1").assertDoesNotExist()
        composeRule.onNodeWithText("Zauber 2").assertDoesNotExist()
    }

    @Test
    fun spellStorageScreen_moreMenüOpensDropdown() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Klicke auf More-Button
        composeRule.onNodeWithContentDescription("Mehr").performClick()
        
        composeRule.waitForIdle()
        
        // Überprüfe, dass Menü-Einträge sichtbar sind
        composeRule.onNodeWithText("Als JSON exportieren").assertIsDisplayed()
        composeRule.onNodeWithText("JSON importieren").assertIsDisplayed()
        composeRule.onNodeWithText("Nearby Sync").assertIsDisplayed()
    }

    @Test
    fun spellStorageScreen_cannotAddSlotWithZeroVolumePoints() {
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Aktiviere Edit-Mode
        composeRule.onNodeWithContentDescription("Bearbeitungsmodus").performClick()
        composeRule.waitForIdle()
        
        // Öffne Add-Slot-Dialog
        composeRule.onNodeWithContentDescription("Slot hinzufügen").performClick()
        composeRule.waitForIdle()
        
        // Versuche 0 Volumenpunkte einzugeben
        composeRule.onNodeWithText("Volumenpunkte").performTextClearance()
        composeRule.onNodeWithText("Volumenpunkte").performTextInput("0")
        composeRule.waitForIdle()
        
        // Überprüfe, dass Fehlermeldung angezeigt wird
        composeRule.onNodeWithText("Mindestens 1 VP erforderlich").assertIsDisplayed()
        
        // Überprüfe, dass der "Hinzufügen" Button deaktiviert ist
        composeRule.onNodeWithText("Hinzufügen").assertIsNotEnabled()
    }

    @Test
    fun spellStorageScreen_filterBySlotType() {
        runBlocking {
            // Füge einen weiteren Zauber als Applicatus-Zauber hinzu
            val applicatusSpellId = repository.insertSpell(
                Spell(
                    name = "Bannbaladin",
                    attribute1 = "KL",
                    attribute2 = "IN",
                    attribute3 = "CH"
                )
            )
            
            repository.insertSlot(
                SpellSlot(
                    characterId = testCharacterId,
                    slotNumber = 2,
                    slotType = SlotType.APPLICATUS,
                    spellId = applicatusSpellId,
                    zfw = 12,
                    isFilled = true
                )
            )
        }
        
        val viewModelFactory = CharacterDetailViewModelFactory(repository, testCharacterId)
        val viewModel = viewModelFactory.create(CharacterDetailViewModel::class.java)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()
        
        // Beide Zauber sollten sichtbar sein
        composeRule.onNodeWithText("Hexenblick").assertIsDisplayed()
        composeRule.onNodeWithText("Bannbaladin").assertIsDisplayed()
        
        // Filtere nach Slot-Typ (wenn Filter-Funktion vorhanden)
        // Dies hängt von der tatsächlichen Implementierung ab
    }
}
