package de.applicatus.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.data.model.herb.Herb
import de.applicatus.app.data.model.herb.Landscape
import de.applicatus.app.data.model.herb.hasGelaendekundeIn
import de.applicatus.app.data.model.herb.Region
import de.applicatus.app.logic.DerianDateCalculator
import de.applicatus.app.logic.HerbSearchCalculator
import de.applicatus.app.ui.viewmodel.HerbSearchViewModel
import de.applicatus.app.ui.viewmodel.HerbSearchViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerbSearchScreen(
    characterId: Long,
    viewModelFactory: HerbSearchViewModelFactory,
    onNavigateBack: () -> Unit
) {
    val viewModel: HerbSearchViewModel = viewModel(factory = viewModelFactory)
    val character by viewModel.character.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val selectedLandscape by viewModel.selectedLandscape.collectAsState()
    val selectedHerb by viewModel.selectedHerb.collectAsState()
    val isGeneralSearch by viewModel.isGeneralSearch.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val hasOrtskenntnis by viewModel.hasOrtskenntnis.collectAsState()
    val hasDoubledSearchTime by viewModel.hasDoubledSearchTime.collectAsState()
    val availableLandscapes by viewModel.availableLandscapes.collectAsState()
    val availableHerbs by viewModel.availableHerbs.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    
    // Initialisiere Region/Landschaft bei jedem Screen-Besuch
    LaunchedEffect(character) {
        character?.let {
            viewModel.initializeLastSearch()
        }
    }
    
    var showRegionMenu by remember { mutableStateOf(false) }
    var showLandscapeMenu by remember { mutableStateOf(false) }
    var showHerbMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kräutersuche") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Character Info (kompakt mit Akkordeon)
            character?.let { char ->
                var isExpanded by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            viewModel.getHerbSearchTaW()?.let { taw ->
                                Text(
                                    text = "Kräutersuche TaW: $taw (MU/IN/FF)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = if (isExpanded) "▼" else "▶",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        if (isExpanded) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Sinnenschärfe: ${char.sensoryAcuitySkill}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Wildnisleben: ${char.wildernessSkill}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Pflanzenkunde: ${char.herbalLoreSkill}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Current Date Display
            currentDate?.let { date ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktuelles Datum:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Region Selection
            Box {
                OutlinedButton(
                    onClick = { showRegionMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedRegion?.name ?: "Region wählen...")
                }
                
                DropdownMenu(
                    expanded = showRegionMenu,
                    onDismissRequest = { showRegionMenu = false }
                ) {
                    viewModel.getAllRegions().forEach { region ->
                        DropdownMenuItem(
                            text = { Text(region.name) },
                            onClick = {
                                viewModel.selectRegion(region)
                                showRegionMenu = false
                            }
                        )
                    }
                }
            }
            
            // Landscape Selection
            if (selectedRegion != null) {
                Box {
                    OutlinedButton(
                        onClick = { showLandscapeMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = availableLandscapes.isNotEmpty()
                    ) {
                        Text(selectedLandscape?.displayName ?: "Landschaft wählen...")
                    }
                    
                    DropdownMenu(
                        expanded = showLandscapeMenu,
                        onDismissRequest = { showLandscapeMenu = false }
                    ) {
                        availableLandscapes.forEach { landscape ->
                            DropdownMenuItem(
                                text = { Text(landscape.displayName) },
                                onClick = {
                                    viewModel.selectLandscape(landscape)
                                    showLandscapeMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Herb Selection
            if (availableHerbs.isNotEmpty()) {
                Box {
                    OutlinedButton(
                        onClick = { showHerbMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                isGeneralSearch -> "Allgemeine Suche"
                                selectedHerb != null -> selectedHerb?.name ?: ""
                                else -> "Kraut wählen..."
                            }
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showHerbMenu,
                        onDismissRequest = { showHerbMenu = false }
                    ) {
                        // Option: Allgemeine Suche
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Allgemeine Suche",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                viewModel.selectGeneralSearch()
                                showHerbMenu = false
                            }
                        )
                        
                        Divider()
                        
                        // Spezifische Kräuter
                        availableHerbs.forEach { herb ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(herb.name)
                                        selectedLandscape?.let { landscape ->
                                            val occurrence = herb.getOccurrenceInLandscape(landscape)
                                            occurrence?.let {
                                                Text(
                                                    text = "Erkennung: +${herb.identificationDifficulty}, Häufigkeit: +${it.modifier}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectHerb(herb)
                                    showHerbMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Modifiers und Probe-Vorschau zusammengefasst
            if (selectedHerb != null || isGeneralSearch) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Modifikatoren",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hasOrtskenntnis,
                                onCheckedChange = { viewModel.setOrtskenntnis(it) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ortskenntnis (-7)")
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hasDoubledSearchTime,
                                onCheckedChange = { viewModel.setDoubledSearchTime(it) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Doppelte Suchdauer (TaW x1.5)")
                        }
                        
                        // Zeige Geländekunde für diese Landschaft an
                        selectedLandscape?.let { landscape ->
                            if (landscape.hasGelaendekundeIn(character?.gelaendekunde ?: emptyList())) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = true,
                                        onCheckedChange = null,
                                        enabled = false
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Geländekunde ${landscape.getGelaendekunde()} (-3)")
                                }
                            }
                        }
                        
                        // Probe-Zusammenfassung
                        character?.let { char ->
                            viewModel.getEffectiveTaW()?.let { effectiveTaw ->
                                viewModel.getSearchDifficulty()?.let { difficulty ->
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    Text(
                                        text = "MU/IN/FF (${char.mu}/${char.inValue}/${char.ff}) mit TaW $effectiveTaw ${if (difficulty >= 0) "+" else ""}$difficulty",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Search Button
                Button(
                    onClick = { 
                        if (isGeneralSearch) {
                            viewModel.performGeneralSearch()
                        } else {
                            viewModel.performSearch()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = character != null && (selectedHerb != null || isGeneralSearch)
                ) {
                    Text(if (isGeneralSearch) "Allgemeine Suche durchführen" else "Suche durchführen")
                }
            }
            
            // Search Result
            searchResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (result.success) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (result.success) "Erfolg!" else "Fehlgeschlagen",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (result.success) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        
                        if (result.isSpectacular) {
                            Text(
                                text = "⭐ Spektakulärer Erfolg! ⭐",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        
                        if (result.isCatastrophic) {
                            Text(
                                text = "💀 Katastrophaler Patzer! 💀",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = "Würfe: ${result.roll1}, ${result.roll2}, ${result.roll3} | TaP*: ${result.qualityPoints}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        // Bei allgemeiner Suche: Zeige alle gefundenen Kräuter
                        if (result.generalSearchResults != null && result.generalSearchResults.isNotEmpty()) {
                            Text(
                                text = "🌿 ${result.generalSearchResults.size} verschiedene Pflanzen gefunden!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            result.generalSearchResults.forEach { herbResult ->
                                Text(
                                    text = herbResult.herb.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                herbResult.harvestedItems.forEach { item ->
                                    val displayText = if (item.diceRoll != null && item.rolled && item.individualRolls.isNotEmpty()) {
                                        if (item.individualRolls.size > 1) {
                                            val rollsText = item.individualRolls.joinToString(", ")
                                            "  • ${item.diceRoll} → [$rollsText] = ${item.quantity}x ${item.productName}"
                                        } else {
                                            "  • ${item.diceRoll} → ${item.quantity}x ${item.productName}"
                                        }
                                    } else if (item.diceRoll != null && item.rolled) {
                                        "  • ${item.diceRoll} → ${item.quantity}x ${item.productName}"
                                    } else {
                                        "  • ${item.quantity}x ${item.productName}"
                                    }
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else if (result.portionCount > 1) {
                            // Gezielte Suche mit mehreren Portionen
                            Text(
                                text = "🌿 ${result.portionCount} Portionen gefunden!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        
                        // Zeige gewürfelte Mengen (nur bei gezielter Suche)
                        if (result.harvestedItems.isNotEmpty() && result.generalSearchResults == null) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Gefunden:",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (result.success) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            result.harvestedItems.forEach { item ->
                                val displayText = if (item.diceRoll != null && item.rolled && item.individualRolls.isNotEmpty()) {
                                    // Bei Mehrfachportionen: Zeige alle Würfe einzeln
                                    if (item.individualRolls.size > 1) {
                                        val rollsText = item.individualRolls.joinToString(", ")
                                        "• ${item.diceRoll} → [$rollsText] = ${item.quantity}x ${item.productName}"
                                    } else {
                                        // Einzelne Portion
                                        "• ${item.diceRoll} → ${item.quantity}x ${item.productName}"
                                    }
                                } else if (item.diceRoll != null && item.rolled) {
                                    // Fallback falls individualRolls leer ist
                                    "• ${item.diceRoll} → ${item.quantity}x ${item.productName}"
                                } else {
                                    // Feste Menge ohne Würfel
                                    "• ${item.quantity}x ${item.productName}"
                                }
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else if (result.foundQuantity != null) {
                            // Fallback auf baseQuantity wenn harvestedItems leer
                            Text(
                                text = "Gefundene Menge: ${result.foundQuantity}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                    }
                }
            }
        }
    }
}
