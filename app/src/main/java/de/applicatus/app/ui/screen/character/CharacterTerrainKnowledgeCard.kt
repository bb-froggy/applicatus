package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character

/**
 * Karte zur Anzeige der Geländekunde eines Charakters
 * 
 * Zeigt alle Landschaften an, für die der Charakter Geländekunde besitzt.
 */
@Composable
fun CharacterTerrainKnowledgeCard(
    character: Character,
    isEditMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Nur anzeigen, wenn der Charakter Geländekunde hat oder im Edit-Modus
    if (character.gelaendekunde.isEmpty() && !isEditMode) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEditMode) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.terrain_knowledge),
                style = MaterialTheme.typography.titleMedium
            )
            
            if (character.gelaendekunde.isEmpty()) {
                Text(
                    text = "Keine Geländekunde vorhanden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HorizontalDivider()
                
                // Liste der Landschaften als Text
                Text(
                    text = character.gelaendekunde.sorted().joinToString(", "),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
