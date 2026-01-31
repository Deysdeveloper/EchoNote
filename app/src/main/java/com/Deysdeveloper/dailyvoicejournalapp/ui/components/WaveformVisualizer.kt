package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 40
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    Canvas(modifier = modifier.height(80.dp).fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = (canvasWidth / barCount) * 0.7f
        val spacing = canvasWidth / barCount
        
        // Show the most recent amplitudes, filling from right to left
        val displayAmplitudes = if (amplitudes.size < barCount) {
            List(barCount - amplitudes.size) { 0f } + amplitudes
        } else {
            amplitudes.takeLast(barCount)
        }
        
        displayAmplitudes.forEachIndexed { index, amplitude ->
            val x = index * spacing + (spacing - barWidth) / 2
            
            // Normalize amplitude to 0-1 range with minimum height
            val normalizedAmplitude = max(amplitude, 0.1f)
            val barHeight = canvasHeight * normalizedAmplitude * 0.9f
            
            // Add slight animation variation based on position
            val animationOffset = kotlin.math.sin((index + animatedPhase * barCount) * 0.3f) * 0.1f
            val finalHeight = barHeight * (1f + animationOffset)
            
            val y = (canvasHeight - finalHeight) / 2
            
            // Draw rounded bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, finalHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

@Composable
fun WaveformVisualizerLive(
    amplitude: Float,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    var amplitudeHistory by remember { mutableStateOf(listOf<Float>()) }
    
    LaunchedEffect(amplitude) {
        amplitudeHistory = (amplitudeHistory + amplitude).takeLast(40)
    }
    
    WaveformVisualizer(
        amplitudes = amplitudeHistory,
        modifier = modifier,
        barColor = barColor
    )
}
