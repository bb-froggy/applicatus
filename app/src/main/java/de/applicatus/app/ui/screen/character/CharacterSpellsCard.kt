package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character

@Composable
fun CharacterSpellsCard(character: Character) {
    // Prüfe, ob der Charakter überhaupt Zauber beherrscht
    val hasSpells = character.hasApplicatus || 
                    character.hasOdem || 
                    character.hasAnalys
    
    if (!hasSpells) return
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.spells),
                style = MaterialTheme.typography.titleMedium
            )
            Divider()
            
            // Applicatus
            if (character.hasApplicatus) {
                SpellItem(
                    name = stringResource(R.string.applicatus),
                    zfw = character.applicatusZfw
                )
            }
            
            // ODEM ARCANUM
            if (character.hasOdem) {
                SpellItem(
                    name = stringResource(R.string.odem_arcanum),
                    zfw = character.odemZfw
                )
            }
            
            // ANALYS ARKANSTRUKTUR
            if (character.hasAnalys) {
                SpellItem(
                    name = stringResource(R.string.analys_arkanstruktur),
                    zfw = character.analysZfw
                )
            }
        }
    }
}

@Composable
private fun SpellItem(name: String, zfw: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.zfw) + ": $zfw",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
