package de.applicatus.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Zeigt eine kurze Blubberblasen-Animation (ca. 1.5 Sekunden) beim Brauen eines Tranks
 */
@Composable
fun PotionBrewAnimation(
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }
    
    // Animation läuft 1500ms
    LaunchedEffect(Unit) {
        delay(1500)
        isVisible = false
        onAnimationEnd()
    }
    
    if (isVisible) {
        Box(modifier = modifier) {
            // Erzeuge mehrere Blasen an zufälligen Positionen
            repeat(12) { index ->
                AnimatedBubble(
                    index = index,
                    totalBubbles = 12
                )
            }
        }
    }
}

@Composable
private fun AnimatedBubble(
    index: Int,
    totalBubbles: Int
) {
    // Zufällige Start-Position (unten im Kessel)
    val startX = remember { -40f + Random.nextFloat() * 80f }
    val startY = remember { 30f + Random.nextFloat() * 20f }
    
    // Blasen steigen nach oben
    val endX = remember { startX + (-10f + Random.nextFloat() * 20f) } // leichte seitliche Bewegung
    val endY = remember { -60f - Random.nextFloat() * 40f } // nach oben
    
    // Zufällige Verzögerung für gestaffeltes Erscheinen
    val delay = remember { (index * 100L) % 800 }
    
    // Zufällige bunte Farben (Grün, Blau, Lila, Pink)
    val bubbleColor = remember {
        val colors = listOf(
            Color(0xFF4CAF50), // Grün
            Color(0xFF2196F3), // Blau
            Color(0xFF9C27B0), // Lila
            Color(0xFFE91E63), // Pink
            Color(0xFF00BCD4), // Cyan
            Color(0xFFFF9800)  // Orange
        )
        colors[index % colors.size]
    }
    
    // Animations-Fortschritt
    var animProgress by remember { mutableStateOf(0f) }
    var started by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delay)
        started = true
        val startTime = System.currentTimeMillis()
        val duration = 1500L - delay
        
        while (System.currentTimeMillis() - startTime < duration) {
            animProgress = ((System.currentTimeMillis() - startTime).toFloat() / duration)
            delay(16) // ~60 FPS
        }
        animProgress = 1f
    }
    
    if (started) {
        // Interpolierte Werte
        val currentX = startX + (endX - startX) * animProgress
        val currentY = startY + (endY - startY) * animProgress
        
        // Fade in am Anfang, fade out am Ende
        val alpha = when {
            animProgress < 0.2f -> animProgress / 0.2f // Fade in
            animProgress > 0.8f -> (1f - animProgress) / 0.2f // Fade out
            else -> 1f
        }
        
        // Pulsieren während der Animation
        val infiniteTransition = rememberInfiniteTransition(label = "bubble_$index")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale_$index"
        )
        
        // Größe der Blase variiert leicht
        val baseSize = remember { 8 + Random.nextInt(8) } // 8-16 dp
        
        Box(
            modifier = Modifier
                .offset(x = currentX.dp, y = currentY.dp)
                .alpha(alpha)
                .scale(scale)
                .size(baseSize.dp)
                .background(
                    color = bubbleColor.copy(alpha = 0.7f),
                    shape = CircleShape
                )
        )
    }
}
