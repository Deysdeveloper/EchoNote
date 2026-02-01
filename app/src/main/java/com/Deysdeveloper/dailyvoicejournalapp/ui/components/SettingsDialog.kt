package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    notificationEnabled: Boolean,
    notificationHour: Int,
    notificationMinute: Int,
    lockEnabled: Boolean,
    onDismiss: () -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onNotificationTimeChange: (Int, Int) -> Unit,
    onLockToggle: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Settings")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Notification settings section
                Text(
                    text = "Daily Reminder",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable daily reminder",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = onNotificationToggle
                    )
                }
                
                if (notificationEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Time: ${String.format("%02d:%02d", notificationHour, notificationMinute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Notification: \"Take 1 minute to record today's thoughts.\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lock settings section
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Require authentication to open",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = lockEnabled,
                        onCheckedChange = onLockToggle
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
