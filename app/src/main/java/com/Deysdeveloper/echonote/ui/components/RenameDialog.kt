package com.deysdeveloper.echonote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RenameDialog(
    currentTitle: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var titleText by remember { mutableStateOf(currentTitle ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Rename Voice Note")
        },
        text = {
            Column {
                Text(
                    text = "Give your voice note a custom title",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., Morning Reflection") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(if (titleText.isBlank()) null else titleText.trim())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
