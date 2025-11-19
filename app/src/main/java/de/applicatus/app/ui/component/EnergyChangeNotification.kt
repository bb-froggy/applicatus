package de.applicatus.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Zeigt kumulative Energie-Änderungen an (z.B. "-4 LE, +12 AE").
 * Bleibt 4 Sekunden sichtbar und verblasst dann über 1 Sekunde.
 * Wird während des Fade-outs eine neue Änderung registriert, wird das Faden rückgängig gemacht.
 * 
 * @param changes Map von Energietyp zu Änderungswert (z.B. "LE" to -4)
 * @param modifier Modifier für die Komponente
 * @param onAnimationEnd Callback wenn Animation endet
 */
@Composable
fun EnergyChangeNotification(
    changes: Map<String, Int>,
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }
    val alpha = remember { Animatable(1f) }
    
    // Wenn sich changes ändert, Animation zurücksetzen
    LaunchedEffect(changes) {
        // Alpha zurück auf 1.0 setzen (Fade-out unterbrechen)
        alpha.snapTo(1f)
        
        // Sichtbar für 4 Sekunden
        delay(4000)
        
        // Fade-out über 1 Sekunde
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1000)
        )
        
        isVisible = false
        onAnimationEnd()
    }
    
    if (isVisible) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alpha.value),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                changes.entries.forEachIndexed { index, (energyType, change) ->
                    if (index > 0) {
                        Text(
                            text = ", ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha.value)
                        )
                    }
                    
                    val changeText = if (change > 0) "+$change" else "$change"
                    val color = when {
                        change > 0 -> Color(0xFF4CAF50) // Grün
                        change < 0 -> Color(0xFFF44336) // Rot
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                    
                    Text(
                        text = "$changeText $energyType",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color.copy(alpha = alpha.value)
                    )
                }
            }
        }
    }
}
