package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun BeautifulStatisticsCard(
    totalRecordings: Int,
    totalDuration: Long,
    currentStreak: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier,
    lastSevenDaysActivity: List<Boolean> = emptyList()
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = backgroundColor.luminance() < 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) WarmTeal.copy(alpha = 0.12f) else WarmTeal.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header - My Journey
                JourneyHeader(
                    currentStreak = currentStreak,
                    delayMillis = 0
                )

                // Weekly Activity Strip
                if (lastSevenDaysActivity.isNotEmpty()) {
                    WeeklyActivityStrip(
                        activity = lastSevenDaysActivity,
                        delayMillis = 100
                    )
                }

                // Main Stats Row - Streak (Hero) + Two side stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hero Streak Card (Left - takes more space)
                    StreakHeroCard(
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        isActive = currentStreak > 0,
                        modifier = Modifier.weight(1.2f),
                        delayMillis = 200
                    )

                    // Side Stats Column (Right)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCardCompact(
                            icon = Icons.Outlined.Mic,
                            value = totalRecordings.toString(),
                            label = "Recordings",
                            color = WarmTeal,
                            delayMillis = 300
                        )

                        StatCardCompact(
                            icon = Icons.Outlined.Timer,
                            value = formatDurationCompact(totalDuration),
                            label = "Total Time",
                            color = WarmTealLight,
                            delayMillis = 400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyHeader(
    currentStreak: Int,
    delayMillis: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "headerAlpha"
    )

    val translateY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -20f,
        animationSpec = tween(500),
        label = "headerTranslate"
    )

    val (motivationalText, iconColor) = when {
        currentStreak >= 7 -> "Incredible consistency! You're on fire!" to SoftCoral
        currentStreak >= 3 -> "Great momentum! Keep it going!" to AccentGold
        currentStreak > 0 -> "Good start! Build that habit!" to WarmTealLight
        else -> "Start your voice journaling journey today!" to WarmTeal
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .offset(y = translateY.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Journey Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(iconColor.copy(alpha = 0.2f), iconColor.copy(alpha = 0.1f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Column {
            Text(
                text = "My Journey",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = motivationalText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyActivityStrip(
    activity: List<Boolean>,
    delayMillis: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        isVisible = true
    }

    // Calculate actual day labels based on current day
    val calendar = Calendar.getInstance()
    val dayLabels = mutableListOf<String>()
    val todayIndex = 6 // Last item is today

    // Go back 6 days and get the day labels
    calendar.add(Calendar.DAY_OF_MONTH, -6)
    repeat(7) {
                        dayLabels.add(
                    when (calendar[Calendar.DAY_OF_WEEK]) {
                Calendar.MONDAY -> "M"
                Calendar.TUESDAY -> "T"
                Calendar.WEDNESDAY -> "W"
                Calendar.THURSDAY -> "T"
                Calendar.FRIDAY -> "F"
                Calendar.SATURDAY -> "S"
                Calendar.SUNDAY -> "S"
                else -> "?"
            }
        )
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            activity.takeLast(7).forEachIndexed { index, hasActivity ->
                val dayDelay = delayMillis + (index * 50)
                var dayVisible by remember { mutableStateOf(false) }

                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        delay(dayDelay.toLong())
                        dayVisible = true
                    }
                }

                val dayAlpha by animateFloatAsState(
                    targetValue = if (dayVisible) 1f else 0f,
                    animationSpec = tween(300),
                    label = "dayAlpha"
                )

                val dayScale by animateFloatAsState(
                    targetValue = if (dayVisible) 1f else 0.5f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dayScale"
                )

                val isToday = index == todayIndex

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(dayAlpha)
                ) {
                    // Activity dot
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .scale(dayScale)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isToday && hasActivity -> AccentGold
                                    hasActivity -> WarmTeal
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasActivity) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Day label
                    Text(
                        text = dayLabels.getOrNull(index) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakHeroCard(
    currentStreak: Int,
    longestStreak: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        isVisible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "firePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "cardAlpha"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .height(140.dp)
            .scale(if (isActive) pulseScale else 1f)
            .alpha(cardAlpha)
            .scale(cardScale)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = if (isActive) {
                    Brush.linearGradient(
                        listOf(
                            AccentGold.copy(alpha = 0.3f),
                            SoftGold.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            LightGray.copy(alpha = 0.6f),
                            LightGray.copy(alpha = 0.3f)
                        )
                    )
                }
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top row: Fire icon (left) + Best badge (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Fire icon with background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isActive) Color.White.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (isActive) SoftCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Best streak badge (top-right)
                if (longestStreak > 0 && longestStreak > currentStreak) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isActive) SoftCoral.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = if (isActive) SoftCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Best: $longestStreak",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isActive) SoftCoral else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom: Streak number and label
            Spacer(modifier = Modifier.weight(1f))

            Column {
                Text(
                    text = if (isActive) "$currentStreak" else "—",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isActive) SoftCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = if (currentStreak == 1) "Day Streak" else "Day Streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCardCompact(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    delayMillis: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "alpha"
    )

    val translateX by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(400),
        label = "translateX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .alpha(alpha)
            .offset(x = translateX.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        color.copy(alpha = 0.12f),
                        color.copy(alpha = 0.04f)
                    )
                )
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Value and label
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDurationCompact(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}
