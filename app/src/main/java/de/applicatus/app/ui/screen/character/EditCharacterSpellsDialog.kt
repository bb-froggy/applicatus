package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character

@Composable
fun EditCharacterSpellsDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var hasApplicatus by remember { mutableStateOf(character.hasApplicatus) }
    var applicatusZfw by remember { mutableStateOf(character.applicatusZfw.toString()) }
    var applicatusModifier by remember { mutableStateOf(character.applicatusModifier.toString()) }
    
    var hasOdem by remember { mutableStateOf(character.hasOdem) }
    var odemZfw by remember { mutableStateOf(character.odemZfw.toString()) }
    
    var hasAnalys by remember { mutableStateOf(character.hasAnalys) }
    var analysZfw by remember { mutableStateOf(character.analysZfw.toString()) }
    
    var kraftkontrolle by remember { mutableStateOf(character.kraftkontrolle) }
    var hasStaffWithKraftfokus by remember { mutableStateOf(character.hasStaffWithKraftfokus) }
    var hasZauberzeichen by remember { mutableStateOf(character.hasZauberzeichen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.spells)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.applicatus))
                        Switch(
                            checked = hasApplicatus,
                            onCheckedChange = { hasApplicatus = it }
                        )
                    }
                }
                
                if (hasApplicatus) {
                    item {
                        OutlinedTextField(
                            value = applicatusZfw,
                            onValueChange = { applicatusZfw = it },
                            label = { Text("${stringResource(R.string.applicatus)} ZfW") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = applicatusModifier,
                            onValueChange = { applicatusModifier = it },
                            label = { Text("${stringResource(R.string.applicatus)} Modifikator") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.odem_arcanum))
                        Switch(
                            checked = hasOdem,
                            onCheckedChange = { hasOdem = it }
                        )
                    }
                }
                
                if (hasOdem) {
                    item {
                        OutlinedTextField(
                            value = odemZfw,
                            onValueChange = { odemZfw = it },
                            label = { Text("${stringResource(R.string.odem_arcanum)} ZfW") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.analys_arkanstruktur))
                        Switch(
                            checked = hasAnalys,
                            onCheckedChange = { hasAnalys = it }
                        )
                    }
                }
                
                if (hasAnalys) {
                    item {
                        OutlinedTextField(
                            value = analysZfw,
                            onValueChange = { analysZfw = it },
                            label = { Text("${stringResource(R.string.analys_arkanstruktur)} ZfW") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                if (character.hasAe) {
                    item {
                        HorizontalDivider()
                    }
                    
                    item {
                        Text("Sonderfertigkeiten", style = MaterialTheme.typography.titleSmall)
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("Kraftkontrolle")
                            Switch(
                                checked = kraftkontrolle,
                                onCheckedChange = { kraftkontrolle = it }
                            )
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("Stab mit Kraftfokus")
                            Switch(
                                checked = hasStaffWithKraftfokus,
                                onCheckedChange = { hasStaffWithKraftfokus = it }
                            )
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("Zauberzeichen")
                            Switch(
                                checked = hasZauberzeichen,
                                onCheckedChange = { hasZauberzeichen = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        character.copy(
                            hasApplicatus = hasApplicatus,
                            applicatusZfw = if (hasApplicatus) applicatusZfw.toIntOrNull() ?: 0 else 0,
                            applicatusModifier = if (hasApplicatus) applicatusModifier.toIntOrNull() ?: 0 else 0,
                            hasOdem = hasOdem,
                            odemZfw = if (hasOdem) odemZfw.toIntOrNull() ?: 0 else 0,
                            hasAnalys = hasAnalys,
                            analysZfw = if (hasAnalys) analysZfw.toIntOrNull() ?: 0 else 0,
                            kraftkontrolle = if (character.hasAe) kraftkontrolle else false,
                            hasStaffWithKraftfokus = if (character.hasAe) hasStaffWithKraftfokus else false,
                            hasZauberzeichen = hasZauberzeichen
                        )
                    )
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
