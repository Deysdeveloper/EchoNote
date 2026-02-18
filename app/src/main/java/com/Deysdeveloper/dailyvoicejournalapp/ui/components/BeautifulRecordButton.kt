package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BeautifulRecordButton(
    isRecording: Boolean,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    
    // Create dynamic gradients based on theme
    val primaryGradient = Brush.linearGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.8f),
            primaryColor
        )
    )
    val recordingGradient = Brush.radialGradient(
        colors = listOf(
            errorColor.copy(alpha = 0.9f),
            errorColor
        )
    )
    
    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Expanding rings animation when recording
        if (isRecording) {
            repeat(3) { index ->
                val delay = index * 300
                ExpandingRing(
                    delayMillis = delay,
                    color = errorColor.copy(alpha = 0.3f)
                )
            }
        }

        // Main button with gradient
        val buttonScale by animateFloatAsState(
            targetValue = if (isRecording) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "buttonScale"
        )

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(buttonScale)
                .shadow(
                    elevation = if (isRecording) 24.dp else 16.dp,
                    shape = CircleShape,
                    spotColor = if (isRecording) errorColor else primaryColor
                )
                .background(
                    brush = if (isRecording) recordingGradient else primaryGradient,
                    shape = CircleShape
                )
                .clickable(onClick = onRecordClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                tint = Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .scale(if (isRecording) 1f else 1.1f)
            )
        }

        // Inner glow ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun ExpandingRing(
    delayMillis: Int,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expandingRing$delayMillis")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(scale)
            .background(
                color = color.copy(alpha = alpha),
                shape = CircleShape
            )
    )
}
