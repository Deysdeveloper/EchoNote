package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.Deysdeveloper.dailyvoicejournalapp.data.ThemeMode
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.*
import java.util.Locale

@Composable
fun SettingsDialog(
    notificationEnabled: Boolean,
    notificationHour: Int,
    notificationMinute: Int,
    lockEnabled: Boolean,
    themeMode: ThemeMode,
    onDismiss: () -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
    onLockToggle: (Boolean) -> Unit,
    onThemeChange: (ThemeMode) -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = backgroundColor.luminance() < 0.5f

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isDark) WarmTeal.copy(alpha = 0.08f) else WarmTeal.copy(alpha = 0.04f),
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
                    // Header
                    SettingsHeader()

                    // Notifications Section
                    SettingsSection(
                        icon = Icons.Outlined.Notifications,
                        title = "Daily Reminder",
                        color = WarmTeal
                    ) {
                        // Enable/Disable toggle
                        SettingToggleRow(
                            checked = notificationEnabled,
                            onCheckedChange = onNotificationToggle
                        )

                        // Time selector (only when enabled)
                        AnimatedVisibility(
                            visible = notificationEnabled,
                            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                                animationSpec = tween(300),
                                initialOffsetY = { -20 }
                            )
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Time selector card
                                TimeSelectorCard(
                                    hour = notificationHour,
                                    minute = notificationMinute,
                                    onClick = onTimeClick
                                )
                            }
                        }
                    }

                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )

                    // Appearance Section
                    SettingsSection(
                        icon = Icons.Outlined.Palette,
                        title = "Appearance",
                        color = AccentGold
                    ) {
                        // Theme selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ThemeOptionCompact(
                                icon = Icons.Outlined.LightMode,
                                label = "Light",
                                isSelected = themeMode == ThemeMode.LIGHT,
                                color = AccentGold,
                                onClick = { onThemeChange(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeOptionCompact(
                                icon = Icons.Outlined.DarkMode,
                                label = "Dark",
                                isSelected = themeMode == ThemeMode.DARK,
                                color = WarmTeal,
                                onClick = { onThemeChange(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeOptionCompact(
                                icon = Icons.Outlined.SettingsSuggest,
                                label = "System",
                                isSelected = themeMode == ThemeMode.SYSTEM,
                                color = SoftCoral,
                                onClick = { onThemeChange(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )

                    // Privacy Section
                    SettingsSection(
                        icon = Icons.Outlined.Lock,
                        title = "Privacy",
                        color = SoftCoral
                    ) {
                        SettingToggleRowWithSubtitle(
                            checked = lockEnabled,
                            onCheckedChange = onLockToggle
                        )
                    }

                    // Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(WarmTeal.copy(alpha = 0.2f), WarmTealLight.copy(alpha = 0.1f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = WarmTeal,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Column {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = color
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content
        content()
    }
}

@Composable
private fun SettingToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Enable daily reminder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmTeal,
                checkedTrackColor = WarmTeal.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SettingToggleRowWithSubtitle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Lock app with biometric",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Require authentication to open",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SoftCoral,
                checkedTrackColor = SoftCoral.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun TimeSelectorCard(
    hour: Int,
    minute: Int,
    onClick: () -> Unit
) {
    val timeText = remember(hour, minute) {
        String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        WarmTeal.copy(alpha = 0.1f),
                        WarmTealLight.copy(alpha = 0.05f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = WarmTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Change time",
                tint = WarmTeal,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ThemeOptionCompact(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = if (isSelected) {
        Brush.linearGradient(
            listOf(
                color.copy(alpha = 0.2f),
                color.copy(alpha = 0.1f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
    }

    val iconBgColor = if (isSelected) {
        color.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundBrush)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
