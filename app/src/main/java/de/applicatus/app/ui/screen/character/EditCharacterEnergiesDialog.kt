package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character

@Composable
fun EditCharacterEnergiesDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var maxLe by remember { mutableStateOf(character.maxLe.toString()) }
    var hasAe by remember { mutableStateOf(character.hasAe) }
    var maxAe by remember { mutableStateOf(character.maxAe.toString()) }
    var hasKe by remember { mutableStateOf(character.hasKe) }
    var maxKe by remember { mutableStateOf(character.maxKe.toString()) }
    var leRegenBonus by remember { mutableStateOf(character.leRegenBonus.toString()) }
    var aeRegenBonus by remember { mutableStateOf(character.aeRegenBonus.toString()) }
    var hasMasteryRegeneration by remember { mutableStateOf(character.hasMasteryRegeneration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.energies)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = maxLe,
                    onValueChange = { maxLe = it },
                    label = { Text("Max ${stringResource(R.string.le_short)}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = leRegenBonus,
                    onValueChange = { leRegenBonus = it },
                    label = { Text("${stringResource(R.string.le_short)} Regenerationsbonus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Astralenergie (${stringResource(R.string.ae_short)})")
                    Switch(
                        checked = hasAe,
                        onCheckedChange = { hasAe = it }
                    )
                }
                
                if (hasAe) {
                    OutlinedTextField(
                        value = maxAe,
                        onValueChange = { maxAe = it },
                        label = { Text("Max ${stringResource(R.string.ae_short)}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = aeRegenBonus,
                        onValueChange = { aeRegenBonus = it },
                        label = { Text("${stringResource(R.string.ae_short)} Regenerationsbonus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Meisterliche Regeneration")
                        Switch(
                            checked = hasMasteryRegeneration,
                            onCheckedChange = { hasMasteryRegeneration = it }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Karmaenergie (${stringResource(R.string.ke_short)})")
                    Switch(
                        checked = hasKe,
                        onCheckedChange = { hasKe = it }
                    )
                }
                
                if (hasKe) {
                    OutlinedTextField(
                        value = maxKe,
                        onValueChange = { maxKe = it },
                        label = { Text("Max ${stringResource(R.string.ke_short)}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedChar = character.copy(
                        maxLe = maxLe.toIntOrNull() ?: character.maxLe,
                        currentLe = (maxLe.toIntOrNull() ?: character.maxLe).coerceAtMost(character.currentLe),
                        hasAe = hasAe,
                        maxAe = if (hasAe) maxAe.toIntOrNull() ?: character.maxAe else 0,
                        currentAe = if (hasAe) (maxAe.toIntOrNull() ?: character.maxAe).coerceAtMost(character.currentAe) else 0,
                        hasKe = hasKe,
                        maxKe = if (hasKe) maxKe.toIntOrNull() ?: character.maxKe else 0,
                        currentKe = if (hasKe) (maxKe.toIntOrNull() ?: character.maxKe).coerceAtMost(character.currentKe) else 0,
                        leRegenBonus = leRegenBonus.toIntOrNull() ?: character.leRegenBonus,
                        aeRegenBonus = aeRegenBonus.toIntOrNull() ?: character.aeRegenBonus,
                        hasMasteryRegeneration = hasMasteryRegeneration
                    )
                    onConfirm(updatedChar)
                }
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
