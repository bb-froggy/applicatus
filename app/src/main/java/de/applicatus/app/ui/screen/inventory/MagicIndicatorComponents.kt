package de.applicatus.app.ui.screen.inventory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.applicatus.app.data.model.inventory.MagicIndicator
import de.applicatus.app.data.model.inventory.MagicIndicatorType
import de.applicatus.app.data.model.magicsign.MagicSignEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Heptagramm (7-zackiger Stern) Symbol für Zauberzeichen.
 * Das Heptagramm ist ein klassisches magisches Symbol und passt besser zu DSA als eine Kristallkugel.
 */
@Composable
fun HeptagramIcon(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 1.5f
) {
    Canvas(modifier = modifier.size(size)) {
        val centerX = size.toPx() / 2
        val centerY = size.toPx() / 2
        val radius = (size.toPx() / 2) * 0.9f
        
        // Berechne die 7 Punkte des Heptagramms
        // Startwinkel: -90° (nach oben zeigend)
        val startAngle = -PI / 2
        val points = (0 until 7).map { i ->
            val angle = startAngle + (2 * PI * i / 7)
            Offset(
                x = centerX + (radius * cos(angle)).toFloat(),
                y = centerY + (radius * sin(angle)).toFloat()
            )
        }
        
        // Zeichne das Heptagramm mit Verbindungen (jeden 3. Punkt verbinden für {7/3} Stern)
        val path = Path()
        var currentIndex = 0
        path.moveTo(points[0].x, points[0].y)
        
        repeat(7) {
            currentIndex = (currentIndex + 3) % 7
            path.lineTo(points[currentIndex].x, points[currentIndex].y)
        }
        path.close()
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * Zeile mit Magic-Indikatoren (Symbole für Zauberzeichen, Applicatus, Langzauber).
 * Jedes Symbol ist antippbar und zeigt einen Detail-Dialog.
 */
@Composable
fun MagicIndicatorRow(
    indicators: List<MagicIndicator>,
    isGameMaster: Boolean,
    modifier: Modifier = Modifier,
    onIndicatorClick: (MagicIndicator) -> Unit = {}
) {
    if (indicators.isEmpty()) return
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        indicators.forEach { indicator ->
            MagicIndicatorChip(
                indicator = indicator,
                isGameMaster = isGameMaster,
                onClick = { onIndicatorClick(indicator) }
            )
        }
    }
}

/**
 * Einzelner Magic-Indikator als antippbares Chip
 */
@Composable
fun MagicIndicatorChip(
    indicator: MagicIndicator,
    isGameMaster: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        // Verdorben: Rot (nur für GM sichtbar)
        indicator.isBotched && isGameMaster -> MaterialTheme.colorScheme.errorContainer
        // Inaktiv: Grau
        !indicator.isActive -> MaterialTheme.colorScheme.surfaceVariant
        // Aktiv nach Typ
        else -> when (indicator.type) {
            MagicIndicatorType.MAGIC_SIGN -> MaterialTheme.colorScheme.primaryContainer
            MagicIndicatorType.APPLICATUS -> MaterialTheme.colorScheme.tertiaryContainer
            MagicIndicatorType.LONG_DURATION_SPELL -> MaterialTheme.colorScheme.secondaryContainer
        }
    }
    
    val contentColor = when {
        indicator.isBotched && isGameMaster -> MaterialTheme.colorScheme.onErrorContainer
        !indicator.isActive -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> when (indicator.type) {
            MagicIndicatorType.MAGIC_SIGN -> MaterialTheme.colorScheme.onPrimaryContainer
            MagicIndicatorType.APPLICATUS -> MaterialTheme.colorScheme.onTertiaryContainer
            MagicIndicatorType.LONG_DURATION_SPELL -> MaterialTheme.colorScheme.onSecondaryContainer
        }
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Symbol - Heptagramm für Zauberzeichen, Emoji für andere
            when (indicator.type) {
                MagicIndicatorType.MAGIC_SIGN -> {
                    HeptagramIcon(
                        size = 14.dp,
                        color = contentColor,
                        strokeWidth = 1.5f
                    )
                }
                else -> {
                    Text(
                        text = indicator.symbol,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Optional: Verdorben-Marker für GM
            if (indicator.isBotched && isGameMaster) {
                Text(
                    text = "⚠",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Dialog zur Anzeige von Magic-Indikator Details
 */
@Composable
fun MagicIndicatorDetailDialog(
    indicator: MagicIndicator,
    isGameMaster: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Symbol - Heptagramm für Zauberzeichen, Emoji für andere
                when (indicator.type) {
                    MagicIndicatorType.MAGIC_SIGN -> {
                        HeptagramIcon(
                            size = 24.dp,
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2f
                        )
                    }
                    else -> {
                        Text(
                            text = indicator.symbol,
                            fontSize = 24.sp
                        )
                    }
                }
                Column {
                    Text(
                        text = indicator.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = indicator.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status:",
                        fontWeight = FontWeight.Bold
                    )
                    
                    val statusText = when {
                        indicator.isBotched && isGameMaster -> "⚠️ Verdorben (nur GM sichtbar)"
                        indicator.isBotched -> "Aktiv" // Spieler sehen nicht, dass es verdorben ist
                        indicator.isActive -> "✓ Aktiv"
                        else -> "○ Inaktiv"
                    }
                    
                    val statusColor = when {
                        indicator.isBotched && isGameMaster -> MaterialTheme.colorScheme.error
                        indicator.isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    Text(
                        text = statusText,
                        color = statusColor
                    )
                }
                
                // Beschreibung
                if (indicator.description.isNotBlank()) {
                    Text(
                        text = indicator.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Typ-spezifische Informationen
                when (indicator.type) {
                    MagicIndicatorType.MAGIC_SIGN -> {
                        // Zauberzeichen-spezifisch
                        if (indicator.magicSignEffect != null && indicator.magicSignEffect != MagicSignEffect.NONE) {
                            Row {
                                Text("Effekt: ", fontWeight = FontWeight.Bold)
                                Text(indicator.magicSignEffect.displayName)
                            }
                        }
                        
                        indicator.activationModifier?.let { mod ->
                            Row {
                                Text("Aktivierungsmodifikator: ", fontWeight = FontWeight.Bold)
                                Text(if (mod >= 0) "+$mod" else "$mod")
                            }
                        }
                        
                        indicator.effectPoints?.let { rkpStar ->
                            Row {
                                Text("RkP*: ", fontWeight = FontWeight.Bold)
                                Text("$rkpStar")
                            }
                        }
                    }
                    
                    MagicIndicatorType.APPLICATUS, MagicIndicatorType.LONG_DURATION_SPELL -> {
                        // Zauber-spezifisch
                        indicator.variant?.takeIf { it.isNotBlank() }?.let { variant ->
                            Row {
                                Text("Variante: ", fontWeight = FontWeight.Bold)
                                Text(variant)
                            }
                        }
                        
                        indicator.effectPoints?.let { zfpStar ->
                            Row {
                                Text("ZfP*: ", fontWeight = FontWeight.Bold)
                                Text("$zfpStar")
                            }
                        }
                        
                        indicator.aspCost?.takeIf { it.isNotBlank() }?.let { cost ->
                            Row {
                                Text("ASP-Kosten: ", fontWeight = FontWeight.Bold)
                                Text(cost)
                            }
                        }
                    }
                }
                
                // Ablaufdatum
                indicator.expiryDate?.let { date ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row {
                        Text("Wirkt bis: ", fontWeight = FontWeight.Bold)
                        Text(date)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

/**
 * Kompakte Gewichtsanzeige mit Original- und reduziertem Gewicht
 */
@Composable
fun WeightWithReduction(
    weight: de.applicatus.app.data.model.inventory.Weight,
    modifier: Modifier = Modifier,
    originalWeight: de.applicatus.app.data.model.inventory.Weight? = null
) {
    if (originalWeight == null || originalWeight == weight) {
        // Keine Reduktion
        Text(
            text = weight.toDisplayString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    } else {
        // Mit Reduktion: Original → Reduziert
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = originalWeight.toDisplayString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = weight.toDisplayString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
