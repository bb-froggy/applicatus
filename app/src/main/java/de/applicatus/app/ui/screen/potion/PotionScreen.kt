package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.R
import de.applicatus.app.data.model.potion.KnownQualityLevel
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.PotionWithRecipe
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.ui.screen.potion.PotionAnalysisDialog
import de.applicatus.app.ui.viewmodel.PotionViewModel
import de.applicatus.app.ui.viewmodel.PotionViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotionScreen(
    characterId: Long,
    viewModelFactory: PotionViewModelFactory,
    onNavigateBack: () -> Unit,
    onNavigateToRecipeKnowledge: () -> Unit = {}
) {
    val viewModel: PotionViewModel = viewModel(factory = viewModelFactory)
    val potions by viewModel.potions.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val character by viewModel.character.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<PotionWithRecipe?>(null) }
    var showAnalysisDialog by remember { mutableStateOf<PotionWithRecipe?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.witch_kitchen)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToRecipeKnowledge) {
                        Text("Rezepte")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_potion))
            }
        }
    ) { paddingValues ->
        if (potions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_potions))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(potions) { potionWithRecipe ->
                    PotionCard(
                        potionWithRecipe = potionWithRecipe,
                        isGameMaster = character?.isGameMaster ?: false,
                        onDelete = { showDeleteDialog = potionWithRecipe },
                        onAnalyze = { showAnalysisDialog = potionWithRecipe }
                    )
                }
            }
        }
    }

    showAnalysisDialog?.let { potionWithRecipe ->
        val character by viewModel.character.collectAsState()
        
        character?.let { char ->
            PotionAnalysisDialog(
                potion = potionWithRecipe.potion,
                recipe = potionWithRecipe.recipe,
                character = char,
                characterId = characterId,
                viewModel = viewModel,
                onDismiss = { showAnalysisDialog = null }
            )
        }
    }

    if (showAddDialog) {
        if (recipes.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.no_recipes)) },
                text = { Text(stringResource(R.string.no_recipes_message)) },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        } else {
            AddPotionDialog(
                recipes = recipes,
                onDismiss = { showAddDialog = false },
                onAdd = { recipeId, quality, expiryDate ->
                    viewModel.addPotion(recipeId, quality, expiryDate)
                    showAddDialog = false
                }
            )
        }
    }

    showDeleteDialog?.let { potionWithRecipe ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_potion)) },
            text = { Text(stringResource(R.string.delete_potion_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePotion(potionWithRecipe.potion)
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun PotionCard(
    potionWithRecipe: PotionWithRecipe,
    isGameMaster: Boolean,
    onDelete: () -> Unit,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Spielleiter sieht immer den Rezeptnamen
                    // Spieler sieht "Unbekannter Trank" wenn keine Kategorie bekannt ist
                    if (isGameMaster) {
                        Text(
                            text = potionWithRecipe.recipe.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        val hasAnyKnowledge = potionWithRecipe.potion.categoryKnown || 
                                            potionWithRecipe.potion.shelfLifeKnown ||
                                            potionWithRecipe.potion.knownQualityLevel != KnownQualityLevel.UNKNOWN
                        Text(
                            text = if (hasAnyKnowledge) potionWithRecipe.recipe.name else "Unbekannter Trank",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    // Bekannte Informationen über das Elixier anzeigen
                    val knowledge = de.applicatus.app.data.model.potion.PotionKnowledgeDisplay.fromPotion(
                        potionWithRecipe.potion,
                        potionWithRecipe.recipe
                    )
                    
                    // Spielleiter sieht alles, Spieler nur analysierte Infos
                    if (isGameMaster) {
                        Text(
                            text = "Tatsächliche Qualität: ${potionWithRecipe.potion.actualQuality}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = knowledge.qualityText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = knowledge.qualityText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (isGameMaster || potionWithRecipe.potion.categoryKnown) {
                        Text(
                            text = knowledge.categoryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isGameMaster) {
                        Text(
                            text = "Tatsächliche Haltbarkeit: ${potionWithRecipe.recipe.shelfLife}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isGameMaster || potionWithRecipe.potion.shelfLifeKnown) {
                        Text(
                            text = knowledge.shelfLifeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isGameMaster) {
                        Text(
                            text = "Status: ${knowledge.analysisProgressText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_potion),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Analysieren",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Trank analysieren")
            }
        }
    }
}

@Composable
private fun getQualityLabel(quality: PotionQuality): String {
    return when (quality) {
        PotionQuality.A -> stringResource(R.string.quality_a)
        PotionQuality.B -> stringResource(R.string.quality_b)
        PotionQuality.C -> stringResource(R.string.quality_c)
        PotionQuality.D -> stringResource(R.string.quality_d)
        PotionQuality.E -> stringResource(R.string.quality_e)
        PotionQuality.F -> stringResource(R.string.quality_f)
        PotionQuality.M -> stringResource(R.string.quality_m)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPotionDialog(
    recipes: List<Recipe>,
    onDismiss: () -> Unit,
    onAdd: (Long, PotionQuality, String) -> Unit
) {
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var selectedQuality by remember { mutableStateOf(PotionQuality.C) }
    var expiryDate by remember { mutableStateOf("") }

    var expandedRecipe by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_potion)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedRecipe,
                    onExpandedChange = { expandedRecipe = it }
                ) {
                    OutlinedTextField(
                        value = selectedRecipe?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.recipe)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRecipe) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRecipe,
                        onDismissRequest = { expandedRecipe = false }
                    ) {
                        recipes.forEach { recipe ->
                            DropdownMenuItem(
                                text = { Text(recipe.name) },
                                onClick = {
                                    selectedRecipe = recipe
                                    expandedRecipe = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedQuality,
                    onExpandedChange = { expandedQuality = it }
                ) {
                    OutlinedTextField(
                        value = getQualityLabel(selectedQuality),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.quality)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQuality) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedQuality,
                        onDismissRequest = { expandedQuality = false }
                    ) {
                        PotionQuality.values().forEach { quality ->
                            DropdownMenuItem(
                                text = { Text(getQualityLabel(quality)) },
                                onClick = {
                                    selectedQuality = quality
                                    expandedQuality = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text(stringResource(R.string.expiry_date)) },
                    placeholder = { Text(stringResource(R.string.expiry_date_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedRecipe?.let { recipe ->
                        onAdd(recipe.id, selectedQuality, expiryDate)
                    }
                },
                enabled = selectedRecipe != null && expiryDate.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
