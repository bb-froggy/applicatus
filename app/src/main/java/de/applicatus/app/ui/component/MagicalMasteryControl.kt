package de.applicatus.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * Wiederverwendbare Komponente für Magisches Meisterhandwerk
 * 
 * @param skillValue Der aktuelle Talentwert
 * @param currentAsp Die aktuell verfügbaren AsP des Charakters
 * @param magicalMasteryAsp Der aktuelle Wert der eingesetzten AsP
 * @param onMagicalMasteryAspChange Callback wenn sich der Wert ändert
 * @param modifier Optional: Modifier für die äußere Column
 */
@Composable
fun MagicalMasteryControl(
    skillValue: Int,
    currentAsp: Int,
    magicalMasteryAsp: Int,
    onMagicalMasteryAspChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Maximal TaW/2 (aufgerundet) AsP können eingesetzt werden
    // da 1 AsP = +2 TaW und man den TaW maximal verdoppeln kann
    val maxMagicalMasteryAsp = ceil(skillValue / 2.0).toInt()
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Magisches Meisterhandwerk (+2 TaW pro AsP)",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Max $maxMagicalMasteryAsp AsP (TaW $skillValue → max ${skillValue * 2})",
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { 
                    if (magicalMasteryAsp > 0) {
                        onMagicalMasteryAspChange(magicalMasteryAsp - 1)
                    }
                },
                enabled = magicalMasteryAsp > 0
            ) {
                Text("-")
            }
            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$magicalMasteryAsp AsP",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "+${magicalMasteryAsp * 2} TaW",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = { onMagicalMasteryAspChange(magicalMasteryAsp + 1) },
                enabled = magicalMasteryAsp < maxMagicalMasteryAsp && magicalMasteryAsp < currentAsp
            ) {
                Text("+")
            }
        }
        Text(
            text = "Verfügbare AsP: $currentAsp",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
