package de.applicatus.app.ui.screen

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
import de.applicatus.app.data.model.character.Group
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SlotType
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.ui.screen.spell.SpellStorageScreen
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModel
import de.applicatus.app.ui.viewmodel.CharacterDetailViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-Tests für die Spieler/Spielleiter-Funktionalität
 * 
 * Das isGameMasterGroup-Feld ist auf der Gruppe definiert.
 * Charaktere in einer Spielleiter-Gruppe sehen alle Details,
 * Charaktere in einer Spieler-Gruppe sehen nur eingeschränkte Informationen.
 */
@RunWith(AndroidJUnit4::class)
class GameMasterModeUITest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: ApplicatusDatabase
    private lateinit var repository: ApplicatusRepository
    private var playerGroupId: Long = -1L
    private var gameMasterGroupId: Long = -1L
    private var playerCharacterId: Long = -1L
    private var gameMasterCharacterId: Long = -1L
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
            
            // Erstelle eine Spieler-Gruppe (isGameMasterGroup = false)
            playerGroupId = repository.insertGroup(
                Group(
                    name = "Spieler Gruppe",
                    isGameMasterGroup = false
                )
            )
            
            // Erstelle eine Spielleiter-Gruppe (isGameMasterGroup = true)
            gameMasterGroupId = repository.insertGroup(
                Group(
                    name = "Spielleiter Gruppe",
                    isGameMasterGroup = true
                )
            )
            
            // Erstelle einen Spieler-Charakter in der Spieler-Gruppe
            playerCharacterId = repository.insertCharacter(
                Character(
                    name = "Spieler Held",
                    mu = 12,
                    kl = 13,
                    inValue = 14,
                    groupId = playerGroupId
                )
            )

            // Erstelle einen Spielleiter-Charakter in der Spielleiter-Gruppe
            gameMasterCharacterId = repository.insertCharacter(
                Character(
                    name = "Spielleiter Held",
                    mu = 12,
                    kl = 13,
                    inValue = 14,
                    groupId = gameMasterGroupId
                )
            )

            // Erstelle einen Test-Zauber
            testSpellId = repository.insertSpell(
                Spell(
                    name = "Hexenblick",
                    attribute1 = "MU",
                    attribute2 = "IN",
                    attribute3 = "CH"
                )
            )

            // Füge einen gefüllten Slot mit ZfP* zum Spieler hinzu
            repository.insertSlot(
                SpellSlot(
                    characterId = playerCharacterId,
                    slotNumber = 0,
                    slotType = SlotType.SPELL_STORAGE,
                    spellId = testSpellId,
                    zfw = 10,
                    volumePoints = 5,
                    isFilled = true,
                    zfpStar = 8,
                    lastRollResult = "Erfolg! [5, 7, 9] ZfP*: 8"
                )
            )

            // Füge einen verpatzten Slot zum Spieler hinzu
            repository.insertSlot(
                SpellSlot(
                    characterId = playerCharacterId,
                    slotNumber = 1,
                    slotType = SlotType.SPELL_STORAGE,
                    spellId = testSpellId,
                    zfw = 10,
                    volumePoints = 5,
                    isFilled = true,
                    isBotched = true,
                    zfpStar = null,
                    lastRollResult = "Fehlgeschlagen! [15, 18, 20] ZfP*: -3"
                )
            )

            // Füge die gleichen Slots zum Spielleiter hinzu
            repository.insertSlot(
                SpellSlot(
                    characterId = gameMasterCharacterId,
                    slotNumber = 0,
                    slotType = SlotType.SPELL_STORAGE,
                    spellId = testSpellId,
                    zfw = 10,
                    volumePoints = 5,
                    isFilled = true,
                    zfpStar = 8,
                    lastRollResult = "Erfolg! [5, 7, 9] ZfP*: 8"
                )
            )

            repository.insertSlot(
                SpellSlot(
                    characterId = gameMasterCharacterId,
                    slotNumber = 1,
                    slotType = SlotType.SPELL_STORAGE,
                    spellId = testSpellId,
                    zfw = 10,
                    volumePoints = 5,
                    isFilled = true,
                    isBotched = true,
                    zfpStar = null,
                    lastRollResult = "Fehlgeschlagen! [15, 18, 20] ZfP*: -3"
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun spielerSiehtKeineZfpBeiErfolgreichemZauber() {
        val viewModel = CharacterDetailViewModel(repository, playerCharacterId)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Warte bis UI geladen ist
        composeRule.waitForIdle()

        // Spieler sieht "Gefüllt" (für beide Slots) aber keine ZfP*
        // Es gibt 2 gefüllte Slots (erfolgreicher und verpatzter)
        composeRule.onAllNodesWithText("✓ Gefüllt").assertCountEquals(2)
        composeRule.onNodeWithText("ZfP*: 8", substring = true).assertDoesNotExist()
        
        // Spieler sieht keine Würfelergebnisse
        composeRule.onNodeWithText("Erfolg!", substring = true).assertDoesNotExist()
    }

    @Test
    fun spielerSiehtAuchBeiPatzerNurGefuellt() {
        val viewModel = CharacterDetailViewModel(repository, playerCharacterId)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()

        // Spieler sieht auch bei Patzer "Gefüllt" (täuschend!)
        // Er erfährt den Patzer erst beim Auslösen
        composeRule.onAllNodesWithText("✓ Gefüllt").assertCountEquals(2) // Beide Slots zeigen "Gefüllt"
        
        // Keine Patzer-Warnung für Spieler sichtbar
        composeRule.onNodeWithText("Verpatzt", substring = true).assertDoesNotExist()
    }

    @Test
    fun spielleiterSiehtAlleInformationen() {
        val viewModel = CharacterDetailViewModel(repository, gameMasterCharacterId)

        composeRule.setContent {
            SpellStorageScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()

        // Spielleiter sieht ZfP* bei erfolgreichem Zauber
        composeRule.onNodeWithText("✓ Gefüllt: 8 ZfP*").assertExists()
        
        // Spielleiter sieht Würfelergebnis
        composeRule.onNodeWithText("Erfolg! [5, 7, 9] ZfP*: 8").assertExists()
        
        // Spielleiter sieht "Verpatzt!" beim Patzer-Slot
        composeRule.onNodeWithText("✗ Verpatzt!").assertExists()
        
        // Spielleiter sieht Details beim Patzer
        composeRule.onNodeWithText("Fehlgeschlagen! [15, 18, 20] ZfP*: -3").assertExists()
    }
}
