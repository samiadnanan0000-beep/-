package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val angle: Double
)

@Composable
fun ConfettiCanvas(
    modifier: Modifier = Modifier,
    durationMs: Int = 3000
) {
    val progress = remember { Animatable(0f) }

    val particles = remember {
        val colors = listOf(
            Color(0xFFE5484D), // Coral red
            Color(0xFF30A46C), // Olive green
            Color(0xFF0C66E4), // One UI blue
            Color(0xFFF5A623), // Gold
            Color(0xFF9333EA), // Purple
            Color(0xFF06B6D4)  // Cyan
        )
        List(85) {
            ConfettiParticle(
                x = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.4f,
                speed = 0.6f + Random.nextFloat() * 0.8f,
                size = 14f + Random.nextFloat() * 16f,
                color = colors[Random.nextInt(colors.size)],
                angle = Random.nextDouble(0.0, 360.0)
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
        )
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            particles.forEach { p ->
                val currentY = (p.initialY + (progress.value * p.speed)) * height
                val currentX = (p.x * width) + (kotlin.math.sin(progress.value * 10 + p.angle) * 35).toFloat()
                if (currentY in 0f..height) {
                    drawRect(
                        color = p.color.copy(alpha = 1f - (progress.value * 0.7f)),
                        topLeft = Offset(currentX, currentY),
                        size = Size(p.size, p.size * 0.6f)
                    )
                }
            }
        }
    }
}
