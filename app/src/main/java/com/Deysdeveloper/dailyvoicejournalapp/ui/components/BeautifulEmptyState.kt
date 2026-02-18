package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.WarmTeal
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.SoftCoral
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.AccentGold

@Composable
fun BeautifulEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating circles animation
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background floating circles
            FloatingCircle(
                delayMillis = 0,
                size = 180.dp,
                color = WarmTeal.copy(alpha = 0.08f)
            )
            FloatingCircle(
                delayMillis = 400,
                size = 140.dp,
                color = SoftCoral.copy(alpha = 0.1f)
            )
            FloatingCircle(
                delayMillis = 800,
                size = 100.dp,
                color = AccentGold.copy(alpha = 0.12f)
            )

            // Breathing icon
            val infiniteTransition = rememberInfiniteTransition(label = "breathe")
            val iconScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "iconScale"
            )
            val iconAlpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "iconAlpha"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(iconScale)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(iconAlpha)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Elegant typography
        Text(
            text = "Your voice journal awaits",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap the beautiful button below\nto capture your first thought",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Subtle hint
        Text(
            text = "Every great journey begins with a single word",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FloatingCircle(
    delayMillis: Int,
    size: Dp,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floatingCircle$delayMillis")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatScale"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .offset(y = offsetY.dp)
            .background(
                color = color,
                shape = CircleShape
            )
    )
}
