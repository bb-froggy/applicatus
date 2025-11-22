package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.data.model.character.CharacterJournalEntry
import de.applicatus.app.data.model.character.JournalCategory
import de.applicatus.app.ui.viewmodel.CharacterJournalViewModel
import de.applicatus.app.ui.viewmodel.CharacterJournalViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Character Journal Screen
 * 
 * Displays all journal entries for a character with:
 * - Chronological order (newest first)
 * - Category icons and colors
 * - Player message (always visible)
 * - GM message (only visible in GM mode)
 * - Earthly and Derian timestamps
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterJournalScreen(
    characterId: Long,
    viewModelFactory: CharacterJournalViewModelFactory,
    onNavigateBack: () -> Unit
) {
    val viewModel: CharacterJournalViewModel = viewModel(factory = viewModelFactory)
    val character by viewModel.character.collectAsState()
    val isGameMaster by viewModel.isGameMasterGroup.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal - ${character?.name ?: "Charakter"}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (journalEntries.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Noch keine Einträge",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Ereignisse werden automatisch protokolliert",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(journalEntries, key = { it.id }) { entry ->
                    JournalEntryCard(
                        entry = entry,
                        isGameMaster = isGameMaster
                    )
                }
            }
        }
    }
}

/**
 * Card displaying a single journal entry.
 */
@Composable
fun JournalEntryCard(
    entry: CharacterJournalEntry,
    isGameMaster: Boolean
) {
    val icon = getCategoryIcon(entry.category)
    val iconColor = getCategoryColor(entry.category)
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category icon
            Icon(
                imageVector = icon,
                contentDescription = entry.category,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Timestamp row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Derian date
                    Text(
                        text = entry.derianDate,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Earthly timestamp
                    Text(
                        text = dateFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Category badge
                Text(
                    text = getCategoryDisplayName(entry.category),
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                // Player message (always visible)
                Text(
                    text = entry.playerMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // GM message (only visible in GM mode)
                if (isGameMaster && entry.gmMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "Spielleiter-Info",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = entry.gmMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get icon for a journal entry category.
 */
fun getCategoryIcon(category: String): ImageVector {
    return when {
        category.startsWith("Potion.Brew") -> Icons.Default.Science
        category.startsWith("Potion.Analysis") -> Icons.Default.Biotech
        category.startsWith("Potion.Consumed") -> Icons.Default.LocalDrink
        category.startsWith("Potion.") -> Icons.Default.LocalPharmacy
        category.startsWith("Spell.Cast") -> Icons.Default.AutoFixHigh
        category.startsWith("Spell.Stored") -> Icons.Default.AddCircleOutline
        category.startsWith("Spell.") -> Icons.Default.AutoAwesome
        category.startsWith("Energy.Regeneration") -> Icons.Default.BedtimeOff
        category.startsWith("Energy.") -> Icons.Default.Favorite
        category.startsWith("Recipe.") -> Icons.Default.MenuBook
        category.startsWith("Inventory.") -> Icons.Default.Backpack
        category.startsWith("Talent.") -> Icons.Default.Star
        category.startsWith("Combat.") -> Icons.Default.Security
        category.startsWith("Group.") -> Icons.Default.Group
        category.startsWith("Character.") -> Icons.Default.Person
        else -> Icons.Default.Info
    }
}

/**
 * Get color for a journal entry category.
 */
@Composable
fun getCategoryColor(category: String): androidx.compose.ui.graphics.Color {
    return when {
        category.startsWith("Potion.") -> MaterialTheme.colorScheme.tertiary
        category.startsWith("Spell.") -> MaterialTheme.colorScheme.primary
        category.startsWith("Energy.") -> MaterialTheme.colorScheme.error
        category.startsWith("Recipe.") -> MaterialTheme.colorScheme.secondary
        category.startsWith("Inventory.") -> MaterialTheme.colorScheme.onSurfaceVariant
        category.startsWith("Combat.") -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
}

/**
 * Get display name for a category.
 */
fun getCategoryDisplayName(category: String): String {
    return when (category) {
        JournalCategory.POTION_BREWED -> "Trank gebraut"
        JournalCategory.POTION_CONSUMED -> "Trank getrunken"
        JournalCategory.POTION_ANALYSIS_INTENSITY -> "Intensität bestimmt"
        JournalCategory.POTION_ANALYSIS_STRUCTURE -> "Struktur analysiert"
        JournalCategory.POTION_ANALYSIS_AUGENSCHEIN -> "Augenschein"
        JournalCategory.POTION_ANALYSIS_LABOR -> "Labor-Analyse"
        JournalCategory.POTION_DILUTED -> "Trank verdünnt"
        JournalCategory.POTION_ACQUIRED -> "Trank erhalten"
        JournalCategory.POTION_GIVEN -> "Trank weitergegeben"
        JournalCategory.SPELL_CAST -> "Zauber gewirkt"
        JournalCategory.SPELL_STORED -> "Zauber gespeichert"
        JournalCategory.SPELL_CLEARED -> "Zauber entfernt"
        JournalCategory.SPELL_SLOT_ADDED -> "Slot hinzugefügt"
        JournalCategory.ENERGY_REGENERATION -> "Regeneriert"
        JournalCategory.ENERGY_LE_CHANGED -> "LE verändert"
        JournalCategory.ENERGY_AE_CHANGED -> "AE verändert"
        JournalCategory.ENERGY_KE_CHANGED -> "KE verändert"
        JournalCategory.RECIPE_LEARNED -> "Rezept gelernt"
        JournalCategory.RECIPE_DISCOVERED -> "Rezept entdeckt"
        JournalCategory.INVENTORY_ITEM_ACQUIRED -> "Gegenstand erhalten"
        JournalCategory.INVENTORY_ITEM_MOVED -> "Gegenstand bewegt"
        JournalCategory.CHARACTER_CREATED -> "Charakter erstellt"
        JournalCategory.GROUP_JOINED -> "Gruppe beigetreten"
        JournalCategory.GROUP_DATE_ADVANCED -> "Datum fortgeschritten"
        else -> category.substringAfterLast('.', category)
    }
}
