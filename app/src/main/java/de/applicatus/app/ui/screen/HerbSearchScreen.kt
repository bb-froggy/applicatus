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
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedHerb by viewModel.selectedHerb.collectAsState()
    val hasOrtskenntnis by viewModel.hasOrtskenntnis.collectAsState()
    val hasDoubledSearchTime by viewModel.hasDoubledSearchTime.collectAsState()
    val availableLandscapes by viewModel.availableLandscapes.collectAsState()
    val availableHerbs by viewModel.availableHerbs.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    
    var showRegionMenu by remember { mutableStateOf(false) }
    var showLandscapeMenu by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }
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
            // Character Info
            character?.let { char ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = char.name,
                            style = MaterialTheme.typography.titleLarge
                        )
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
                        viewModel.getHerbSearchTaW()?.let { taw ->
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Kräutersuche-TaW: $taw",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
            
            // Month Selection
            if (selectedLandscape != null) {
                Box {
                    OutlinedButton(
                        onClick = { showMonthMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedMonth.displayName)
                    }
                    
                    DropdownMenu(
                        expanded = showMonthMenu,
                        onDismissRequest = { showMonthMenu = false }
                    ) {
                        viewModel.getAllMonths().forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month.displayName) },
                                onClick = {
                                    viewModel.selectMonth(month)
                                    showMonthMenu = false
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
                        Text(selectedHerb?.name ?: "Kraut wählen...")
                    }
                    
                    DropdownMenu(
                        expanded = showHerbMenu,
                        onDismissRequest = { showHerbMenu = false }
                    ) {
                        availableHerbs.forEach { herb ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(herb.name)
                                        Text(
                                            text = "Erschwer ${herb.identificationDifficulty}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
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
            
            // Modifiers
            if (selectedHerb != null) {
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
                            Text("Doppelte Suchdauer (-2)")
                        }
                        
                        // Zeige Geländekunde für diese Landschaft an
                        selectedLandscape?.let { landscape ->
                            if (character?.gelaendekunde?.contains(landscape.displayName) == true) {
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
                                    Text("Geländekunde ${landscape.displayName} (-3)")
                                }
                            }
                        }
                        
                        viewModel.getSearchDifficulty()?.let { difficulty ->
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Gesamte Erschwernis: $difficulty",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Search Button
                Button(
                    onClick = { viewModel.performSearch() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = character != null
                ) {
                    Text("Suche durchführen")
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
                            text = "Würfe: ${result.roll1}, ${result.roll2}, ${result.roll3}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Text(
                            text = "TaP*: ${result.qualityPoints}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        result.foundQuantity?.let { quantity ->
                            Text(
                                text = "Gefundene Menge: $quantity",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Button(
                            onClick = { viewModel.resetSearch() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Neue Suche")
                        }
                    }
                }
            }
        }
    }
}
