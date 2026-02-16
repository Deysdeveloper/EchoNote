package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TranscriptDialog(
    currentTranscript: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var transcript by remember { mutableStateOf(currentTranscript ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentTranscript.isNullOrBlank()) "Add Transcript" else "Edit Transcript",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add a transcript or notes for this voice recording. " +
                           "You can type what was said or add your own notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = transcript,
                    onValueChange = { 
                        transcript = it
                        isError = false
                    },
                    label = { Text("Transcript") },
                    placeholder = { Text("Type the transcript here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    maxLines = 6,
                    minLines = 4,
                    isError = isError,
                    supportingText = {
                        Text("${transcript.length} characters")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedTranscript = transcript.trim()
                    onSave(trimmedTranscript.takeIf { it.isNotEmpty() })
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
