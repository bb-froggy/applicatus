package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.ui.viewmodel.RecipeKnowledgeViewModel
import de.applicatus.app.ui.viewmodel.RecipeKnowledgeViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeKnowledgeScreen(
    characterId: Long,
    viewModelFactory: RecipeKnowledgeViewModelFactory,
    onNavigateBack: () -> Unit
) {
    val viewModel: RecipeKnowledgeViewModel = viewModel(factory = viewModelFactory)
    val recipesWithKnowledge by viewModel.recipesWithKnowledge.collectAsState()

    var filterLevel by remember { mutableStateOf<RecipeKnowledgeLevel?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rezepte-Wissen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Filtern"
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Alle anzeigen") },
                            onClick = {
                                filterLevel = null
                                showFilterMenu = false
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Nur Bekannte") },
                            onClick = {
                                filterLevel = RecipeKnowledgeLevel.KNOWN
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nur Verstandene") },
                            onClick = {
                                filterLevel = RecipeKnowledgeLevel.UNDERSTOOD
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nur Unbekannte") },
                            onClick = {
                                filterLevel = RecipeKnowledgeLevel.UNKNOWN
                                showFilterMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val filteredRecipes = if (filterLevel != null) {
            recipesWithKnowledge.filter { it.knowledgeLevel == filterLevel }
        } else {
            recipesWithKnowledge
        }

        if (filteredRecipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterLevel != null) {
                        "Keine Rezepte mit diesem Status"
                    } else {
                        "Keine Rezepte vorhanden"
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRecipes) { recipeWithKnowledge ->
                    RecipeKnowledgeCard(
                        recipeWithKnowledge = recipeWithKnowledge,
                        onKnowledgeLevelChange = { newLevel ->
                            viewModel.setRecipeKnowledge(recipeWithKnowledge.recipe.id, newLevel)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeKnowledgeCard(
    recipeWithKnowledge: RecipeKnowledgeViewModel.RecipeWithKnowledge,
    onKnowledgeLevelChange: (RecipeKnowledgeLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = recipeWithKnowledge.recipe.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Brauschwierigkeit: +${'$'}{recipeWithKnowledge.recipe.brewingDifficulty}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Analyse: +${'$'}{recipeWithKnowledge.recipe.analysisDifficulty}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Haltbarkeit: ${'$'}{recipeWithKnowledge.recipe.shelfLife}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Aussehen:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = recipeWithKnowledge.recipe.appearance.ifEmpty { "-" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = getKnowledgeLevelDisplayName(recipeWithKnowledge.knowledgeLevel),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Wissensstatus") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    RecipeKnowledgeLevel.values().forEach { level ->
                        DropdownMenuItem(
                            text = { Text(getKnowledgeLevelDisplayName(level)) },
                            onClick = {
                                onKnowledgeLevelChange(level)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getKnowledgeLevelDisplayName(level: RecipeKnowledgeLevel): String {
    return when (level) {
        RecipeKnowledgeLevel.UNKNOWN -> "Unbekannt"
        RecipeKnowledgeLevel.UNDERSTOOD -> "Verstanden"
        RecipeKnowledgeLevel.KNOWN -> "Bekannt"
    }
}
