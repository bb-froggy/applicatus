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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import de.applicatus.app.logic.PotionHelper
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
    val groupCharacters by viewModel.groupCharacters.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBrewDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<PotionWithRecipe?>(null) }
    var showAnalysisDialog by remember { mutableStateOf<PotionWithRecipe?>(null) }

    val isGameMaster = character?.isGameMaster ?: false

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
            // Beide Buttons für alle Spieler - Spielleiter und Spieler
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isGameMaster) {
                    // Spielleiter: Button zum direkten Hinzufügen
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_potion))
                    }
                }
                // Brauen-Button für alle (Spieler und Spielleiter)
                FloatingActionButton(onClick = { showBrewDialog = true }) {
                    Icon(Icons.Default.Build, contentDescription = "Trank brauen")
                }
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
                    val recipeKnowledge by viewModel.getRecipeKnowledge(potionWithRecipe.recipe.id).collectAsState(null)
                    val isRecipeKnown = recipeKnowledge?.knowledgeLevel == de.applicatus.app.data.model.potion.RecipeKnowledgeLevel.UNDERSTOOD
                    
                    PotionCard(
                        potionWithRecipe = potionWithRecipe,
                        isGameMaster = character?.isGameMaster ?: false,
                        isRecipeKnown = isRecipeKnown,
                        groupCharacters = groupCharacters,
                        onDelete = { showDeleteDialog = potionWithRecipe },
                        onAnalyze = { showAnalysisDialog = potionWithRecipe },
                        onTransfer = { targetId ->
                            viewModel.transferPotionToCharacter(potionWithRecipe.potion, targetId)
                        }
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
                currentDate = "1. Praios 1040 BF", // TODO: Aktuelles Datum aus Charaktereinstellungen
                onDismiss = { showAddDialog = false },
                onAdd = { recipeId, actualQuality, appearance, expiryDate ->
                    viewModel.addPotion(recipeId, actualQuality, appearance, expiryDate)
                    showAddDialog = false
                }
            )
        }
    }

    if (showBrewDialog && character != null) {
        BrewPotionDialog(
            character = character!!,
            viewModel = viewModel,
            onDismiss = { showBrewDialog = false }
        )
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
    isRecipeKnown: Boolean,
    groupCharacters: List<de.applicatus.app.data.model.character.Character> = emptyList(),
    onDelete: () -> Unit,
    onAnalyze: () -> Unit,
    onTransfer: (Long) -> Unit = {}
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
                    // Spieler sieht den Namen nur, wenn das Rezept bekannt/verstanden ist
                    if (isGameMaster) {
                        Text(
                            text = potionWithRecipe.recipe.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            text = if (isRecipeKnown) potionWithRecipe.recipe.name else "Unbekannter Trank",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    // Bekannte Informationen über das Elixier anzeigen
                    val knowledge = de.applicatus.app.data.model.potion.PotionKnowledgeDisplay.fromPotion(
                        potionWithRecipe.potion,
                        potionWithRecipe.recipe
                    )
                    
                    // Aussehen anzeigen (falls vorhanden)
                    if (potionWithRecipe.potion.appearance.isNotBlank()) {
                        Text(
                            text = "Aussehen: ${potionWithRecipe.potion.appearance}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
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
                    
                    // Ablaufdatum anzeigen (Spielleiter sieht es immer)
                    if (isGameMaster || potionWithRecipe.potion.shelfLifeKnown) {
                        Text(
                            text = "Ablaufdatum: ${potionWithRecipe.potion.expiryDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (potionWithRecipe.potion.expiryDate == de.applicatus.app.logic.PotionHelper.UNLIMITED_DATE) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAnalyze,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Analysieren",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analysieren")
                }
                
                // Übergeben-Button nur anzeigen, wenn es andere Charaktere in der Gruppe gibt
                if (groupCharacters.isNotEmpty()) {
                    var showTransferDialog by remember { mutableStateOf(false) }
                    
                    OutlinedButton(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Übergeben")
                    }
                    
                    if (showTransferDialog) {
                        TransferPotionDialog(
                            characters = groupCharacters,
                            onDismiss = { showTransferDialog = false },
                            onConfirm = { targetId ->
                                onTransfer(targetId)
                                showTransferDialog = false
                            }
                        )
                    }
                }
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
    currentDate: String,
    onDismiss: () -> Unit,
    onAdd: (Long, PotionQuality, String, String) -> Unit
) {
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var selectedQuality by remember { mutableStateOf(PotionQuality.C) }
    var appearance by remember { mutableStateOf("") }
    
    // Datum-State
    var isUnlimited by remember { mutableStateOf(false) }
    var expiryDay by remember { mutableStateOf(1) }
    var expiryMonth by remember { mutableStateOf(1) }
    var expiryYear by remember { mutableStateOf(1040) }

    var expandedRecipe by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }
    
    // Initialisiere Datum mit aktuellem Datum
    LaunchedEffect(Unit) {
        PotionHelper.parseDerischenDate(currentDate)?.let { (day, month, year) ->
            expiryDay = day
            expiryMonth = month
            expiryYear = year
        }
    }
    
    // Aktualisiere Aussehen wenn Rezept gewählt wird
    LaunchedEffect(selectedRecipe) {
        selectedRecipe?.let { recipe ->
            if (appearance.isEmpty() && recipe.appearance.isNotEmpty()) {
                appearance = recipe.appearance
            }
            // Berechne Ablaufdatum basierend auf Haltbarkeit
            if (!isUnlimited) {
                val calculatedDate = PotionHelper.calculateExpiryDate(currentDate, recipe.shelfLife)
                if (calculatedDate != PotionHelper.UNLIMITED_DATE) {
                    PotionHelper.parseDerischenDate(calculatedDate)?.let { (day, month, year) ->
                        expiryDay = day
                        expiryMonth = month
                        expiryYear = year
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_potion)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rezept-Auswahl
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

                // Qualitäts-Auswahl
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

                // Aussehen-Eingabe mit Zufallsgenerator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = appearance,
                        onValueChange = { appearance = it },
                        label = { Text(stringResource(R.string.appearance)) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = { appearance = PotionHelper.generateRandomAppearance() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.generate_random_appearance)
                        )
                    }
                }

                // Haltbarkeit - Unbegrenzt Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isUnlimited,
                        onCheckedChange = { isUnlimited = it }
                    )
                    Text(stringResource(R.string.unlimited))
                }

                // Datum-Eingabe mit +/- Buttons (nur wenn nicht unbegrenzt)
                if (!isUnlimited) {
                    Text(
                        text = stringResource(R.string.expiry_date),
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    // Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.day),
                            modifier = Modifier.width(60.dp)
                        )
                        IconButton(
                            onClick = {
                                expiryDay = (expiryDay - 1).coerceAtLeast(1)
                            }
                        ) {
                            Text("-", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            text = expiryDay.toString(),
                            modifier = Modifier.width(40.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                val maxDays = PotionHelper.getDaysInMonth(expiryMonth)
                                expiryDay = (expiryDay + 1).coerceAtMost(maxDays)
                            }
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    
                    // Monat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.month),
                            modifier = Modifier.width(60.dp)
                        )
                        IconButton(
                            onClick = {
                                expiryMonth = if (expiryMonth > 1) expiryMonth - 1 else 12
                                // Passe Tag an falls nötig
                                val maxDays = PotionHelper.getDaysInMonth(expiryMonth)
                                if (expiryDay > maxDays) expiryDay = maxDays
                            }
                        ) {
                            Text("-", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            text = expiryMonth.toString(),
                            modifier = Modifier.width(40.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                expiryMonth = if (expiryMonth < 12) expiryMonth + 1 else 1
                                // Passe Tag an falls nötig
                                val maxDays = PotionHelper.getDaysInMonth(expiryMonth)
                                if (expiryDay > maxDays) expiryDay = maxDays
                            }
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    
                    // Jahr
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.year),
                            modifier = Modifier.width(60.dp)
                        )
                        IconButton(
                            onClick = {
                                expiryYear = (expiryYear - 1).coerceAtLeast(1)
                            }
                        ) {
                            Text("-", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            text = "$expiryYear BF",
                            modifier = Modifier.width(80.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                expiryYear++
                            }
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    
                    // Formatiertes Datum anzeigen
                    Text(
                        text = PotionHelper.formatDerischenDate(expiryDay, expiryMonth, expiryYear),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedRecipe?.let { recipe ->
                        val finalExpiryDate = if (isUnlimited) {
                            PotionHelper.UNLIMITED_DATE
                        } else {
                            PotionHelper.formatDerischenDate(expiryDay, expiryMonth, expiryYear)
                        }
                        onAdd(recipe.id, selectedQuality, appearance, finalExpiryDate)
                    }
                },
                enabled = selectedRecipe != null && appearance.isNotBlank()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferPotionDialog(
    characters: List<de.applicatus.app.data.model.character.Character>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedCharacter by remember { mutableStateOf<de.applicatus.app.data.model.character.Character?>(null) }
    var expandedCharacter by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trank übergeben") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Wähle einen Charakter aus deiner Gruppe:")
                
                ExposedDropdownMenuBox(
                    expanded = expandedCharacter,
                    onExpandedChange = { expandedCharacter = it }
                ) {
                    OutlinedTextField(
                        value = selectedCharacter?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Zielcharakter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCharacter) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedCharacter,
                        onDismissRequest = { expandedCharacter = false }
                    ) {
                        characters.forEach { character ->
                            DropdownMenuItem(
                                text = { Text(character.name) },
                                onClick = {
                                    selectedCharacter = character
                                    expandedCharacter = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCharacter?.let { onConfirm(it.id) }
                },
                enabled = selectedCharacter != null
            ) {
                Text("Übergeben")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

