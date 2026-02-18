package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * A waveform visualization component for audio playback similar to WhatsApp voice messages.
 * Shows amplitude bars with a progress indicator that tracks playback position.
 */
@Composable
fun PlaybackWaveform(
    amplitudes: List<Float>,
    progress: Float, // 0.0 to 1.0
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 60,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    playedColor: Color = MaterialTheme.colorScheme.primary,
    unplayedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    barHeight: Dp = 40.dp
) {
    val normalizedAmplitudes = normalizeAmplitudes(amplitudes, barCount)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val seekProgress = (offset.x / width).coerceIn(0f, 1f)
                    onSeek(seekProgress)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val totalBarWidth = barWidth.toPx() + barSpacing.toPx()
            val availableBars = min(barCount, (canvasWidth / totalBarWidth).toInt())
            val startX = (canvasWidth - (availableBars * totalBarWidth - barSpacing.toPx())) / 2f
            val progressX = startX + (canvasWidth - 2 * startX) * progress

            for (i in 0 until availableBars) {
                val amplitude = normalizedAmplitudes.getOrNull(i) ?: 0.1f
                val barHeightPx = max(canvasHeight * amplitude * 0.9f, 4.dp.toPx())
                val x = startX + i * totalBarWidth
                val y = (canvasHeight - barHeightPx) / 2f

                // Determine if this bar is before or after the progress indicator
                val isPlayed = x + barWidth.toPx() / 2 <= progressX
                val barColor = if (isPlayed) playedColor else unplayedColor

                // Draw rounded bar
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth.toPx(), barHeightPx),
                    cornerRadius = CornerRadius(barWidth.toPx() / 2, barWidth.toPx() / 2)
                )
            }

            // Draw progress indicator line
            if (progress > 0f && progress < 1f) {
                drawLine(
                    color = playedColor,
                    start = Offset(progressX, 0f),
                    end = Offset(progressX, canvasHeight),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

/**
 * Normalizes and resizes the amplitude list to match the bar count.
 * If amplitudes is empty, returns a list of minimum values.
 */
private fun normalizeAmplitudes(amplitudes: List<Float>, barCount: Int): List<Float> {
    if (amplitudes.isEmpty()) {
        return List(barCount) { 0.1f }
    }

    // Normalize to 0.1 - 1.0 range
    val maxAmplitude = amplitudes.maxOrNull() ?: 1f
    val normalized = amplitudes.map {
        max(0.1f, min(1f, it / maxAmplitude))
    }

    // Resample to match bar count
    return if (normalized.size == barCount) {
        normalized
    } else if (normalized.size < barCount) {
        // Pad with minimum values
        normalized + List(barCount - normalized.size) { 0.1f }
    } else {
        // Downsample by taking max of chunks
        val chunkSize = normalized.size / barCount
        List(barCount) { index ->
            val start = index * chunkSize
            val end = min(start + chunkSize, normalized.size)
            normalized.subList(start, end).maxOrNull() ?: 0.1f
        }
    }
}

/**
 * Simplified version without seek functionality for static display
 */
@Composable
fun StaticWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barCount: Int = 60,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    barColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    barHeight: Dp = 40.dp
) {
    val normalizedAmplitudes = normalizeAmplitudes(amplitudes, barCount)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val totalBarWidth = barWidth.toPx() + barSpacing.toPx()
            val availableBars = min(barCount, (canvasWidth / totalBarWidth).toInt())
            val startX = (canvasWidth - (availableBars * totalBarWidth - barSpacing.toPx())) / 2f

            for (i in 0 until availableBars) {
                val amplitude = normalizedAmplitudes.getOrNull(i) ?: 0.1f
                val barHeightPx = max(canvasHeight * amplitude * 0.9f, 4.dp.toPx())
                val x = startX + i * totalBarWidth
                val y = (canvasHeight - barHeightPx) / 2f

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth.toPx(), barHeightPx),
                    cornerRadius = CornerRadius(barWidth.toPx() / 2, barWidth.toPx() / 2)
                )
            }
        }
    }
}
