package de.applicatus.app.ui.component.spell

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Zeigt eine kurze Sternchen-Animation (ca. 1 Sekunde)
 */
@Composable
fun SpellCastAnimation(
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }
    
    // Animation läuft 1000ms
    LaunchedEffect(Unit) {
        delay(1000)
        isVisible = false
        onAnimationEnd()
    }
    
    if (isVisible) {
        Box(modifier = modifier) {
            // Erzeuge mehrere Sternchen an zufälligen Positionen
            repeat(8) { index ->
                AnimatedStar(
                    index = index,
                    totalStars = 8
                )
            }
        }
    }
}

@Composable
private fun AnimatedStar(
    index: Int,
    totalStars: Int
) {
    // Zufällige Start-Position (innerhalb eines Radius)
    val angle = remember { Random.nextFloat() * 2f * Math.PI.toFloat() }
    val startRadius = remember { 20f + Random.nextFloat() * 30f }
    val endRadius = remember { startRadius + 40f + Random.nextFloat() * 20f }
    
    val startX = remember { (startRadius * kotlin.math.cos(angle.toDouble())).toFloat() }
    val startY = remember { (startRadius * kotlin.math.sin(angle.toDouble())).toFloat() }
    val endX = remember { (endRadius * kotlin.math.cos(angle.toDouble())).toFloat() }
    val endY = remember { (endRadius * kotlin.math.sin(angle.toDouble())).toFloat() }
    
    // Animations-Fortschritt
    val infiniteTransition = rememberInfiniteTransition(label = "star_$index")
    
    // Einmal-Animation für Position und Alpha
    var animProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val duration = 1000L
        
        while (System.currentTimeMillis() - startTime < duration) {
            animProgress = ((System.currentTimeMillis() - startTime).toFloat() / duration)
            delay(16) // ~60 FPS
        }
        animProgress = 1f
    }
    
    // Interpolierte Werte
    val currentX = startX + (endX - startX) * animProgress
    val currentY = startY + (endY - startY) * animProgress
    val alpha = 1f - animProgress // Fade out
    
    // Rotation (kontinuierlich)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_$index"
    )
    
    // Pulsieren (kontinuierlich)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_$index"
    )
    
    Text(
        text = "✨",
        fontSize = 24.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .offset(x = currentX.dp, y = currentY.dp)
            .alpha(alpha)
            .scale(scale)
    )
}
