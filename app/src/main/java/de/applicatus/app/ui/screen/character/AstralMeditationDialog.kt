package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character

@Composable
fun AstralMeditationDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var leToConvert by remember { mutableStateOf("1") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Astrale Meditation") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Wandle Lebensenergie in Astralenergie um.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    buildString {
                        append("Probe: IN/CH/KO\n")
                        val ritualBonus = (character.ritualKnowledgeValue + 1) / 2
                        val sfBonus = if (character.hasKonzentrationsstärke) 2 else 0
                        val totalFacilitation = ritualBonus + sfBonus
                        if (totalFacilitation > 0) {
                            append("Erleichterung: -$totalFacilitation")
                            if (ritualBonus > 0) append(" (RkW/2: $ritualBonus")
                            if (sfBonus > 0) {
                                if (ritualBonus > 0) append(", ")
                                append("SF Konzentrationsstärke: $sfBonus")
                            }
                            if (ritualBonus > 0 || sfBonus > 0) append(")")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                
                Divider()
                
                OutlinedTextField(
                    value = leToConvert,
                    onValueChange = { leToConvert = it.filter { c -> c.isDigit() } },
                    label = { Text("LE umwandeln") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    buildString {
                        val le = leToConvert.toIntOrNull() ?: 0
                        if (le > 0) {
                            append("Bei Erfolg:\n")
                            append("• +$le AE\n")
                            append("• -$le LE (umgewandelt)\n")
                            append("• -1 AsP (Kosten)\n")
                            append("• -1W3-1 LE (zusätzlich)")
                        } else {
                            append("Gib eine gültige LE-Anzahl ein.")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Warnungen
                val le = leToConvert.toIntOrNull() ?: 0
                if (le > 0) {
                    if (character.currentAe < 1) {
                        Text(
                            "⚠️ Nicht genug AsP!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (character.currentLe < le + 2) {
                        Text(
                            "⚠️ Möglicherweise nicht genug LE (mind. ${le + 2} empfohlen)!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (character.currentAe + le - 1 > character.maxAe) {
                        Text(
                            "⚠️ AE-Maximum würde überschritten!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val le = leToConvert.toIntOrNull() ?: 0
                    if (le > 0) {
                        onConfirm(le)
                    }
                },
                enabled = (leToConvert.toIntOrNull() ?: 0) > 0
            ) {
                Text("Meditieren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
