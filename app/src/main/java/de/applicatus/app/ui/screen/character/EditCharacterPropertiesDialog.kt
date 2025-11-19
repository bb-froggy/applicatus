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
fun EditCharacterPropertiesDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var mu by remember { mutableStateOf(character.mu.toString()) }
    var kl by remember { mutableStateOf(character.kl.toString()) }
    var inValue by remember { mutableStateOf(character.inValue.toString()) }
    var ch by remember { mutableStateOf(character.ch.toString()) }
    var ff by remember { mutableStateOf(character.ff.toString()) }
    var ge by remember { mutableStateOf(character.ge.toString()) }
    var ko by remember { mutableStateOf(character.ko.toString()) }
    var kk by remember { mutableStateOf(character.kk.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.properties)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mu,
                        onValueChange = { mu = it },
                        label = { Text("MU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = kl,
                        onValueChange = { kl = it },
                        label = { Text("KL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inValue,
                        onValueChange = { inValue = it },
                        label = { Text("IN") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ch,
                        onValueChange = { ch = it },
                        label = { Text("CH") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ff,
                        onValueChange = { ff = it },
                        label = { Text("FF") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ge,
                        onValueChange = { ge = it },
                        label = { Text("GE") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ko,
                        onValueChange = { ko = it },
                        label = { Text("KO") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = kk,
                        onValueChange = { kk = it },
                        label = { Text("KK") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        character.copy(
                            mu = mu.toIntOrNull() ?: character.mu,
                            kl = kl.toIntOrNull() ?: character.kl,
                            inValue = inValue.toIntOrNull() ?: character.inValue,
                            ch = ch.toIntOrNull() ?: character.ch,
                            ff = ff.toIntOrNull() ?: character.ff,
                            ge = ge.toIntOrNull() ?: character.ge,
                            ko = ko.toIntOrNull() ?: character.ko,
                            kk = kk.toIntOrNull() ?: character.kk
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
