package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Deysdeveloper.dailyvoicejournalapp.audio.WaveformExtractor
import com.Deysdeveloper.dailyvoicejournalapp.data.VoiceNote

@Composable
fun VoiceNoteItem(
    voiceNote: VoiceNote,
    isPlaying: Boolean,
    isAudioActuallyPlaying: Boolean = false,
    currentPosition: Int = 0,
    playbackDuration: Int = 0,
    isTranscribing: Boolean = false,
    isConvertingAudio: Boolean = false,
    canAutoTranscribe: Boolean = false,
    formatTime: (Long) -> String,
    formatDuration: (Long) -> String,
    getDefaultTitle: (Long) -> String,
    onPlayClick: () -> Unit,
    onPauseResumeClick: () -> Unit = {},
    onSeek: (Int) -> Unit = {},
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditTranscriptClick: () -> Unit = {},
    onAutoTranscribeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val amplitudes = remember(voiceNote.waveformData) {
        voiceNote.waveformData?.let { WaveformExtractor.deserializeWaveform(it) } ?: emptyList()
    }

    val progress = if (playbackDuration > 0) {
        currentPosition.toFloat() / playbackDuration.toFloat()
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Title and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = voiceNote.title ?: getDefaultTitle(voiceNote.timestamp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatTime(voiceNote.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Edit title button
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit title",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Stop button
                    IconButton(onClick = onPlayClick) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Duration info (when not playing)
                    if (!isPlaying) {
                        Text(
                            text = formatDuration(voiceNote.duration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    // Edit transcript button
                    IconButton(onClick = onEditTranscriptClick) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = if (voiceNote.transcript.isNullOrBlank()) "Add transcript" else "Edit transcript",
                            tint = if (voiceNote.transcript.isNullOrBlank()) 
                                MaterialTheme.colorScheme.outline 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Auto-transcribe button (show if no transcript and not currently transcribing)
                    if (voiceNote.transcript.isNullOrBlank() && !isTranscribing) {
                        IconButton(onClick = onAutoTranscribeClick) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Auto-transcribe",
                                tint = if (canAutoTranscribe) 
                                    MaterialTheme.colorScheme.secondary 
                                else 
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    
                    // Share button
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Delete button
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Show waveform visualization
            if (amplitudes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (isPlaying && playbackDuration > 0) {
                    // Interactive waveform during playback
                    PlaybackWaveform(
                        amplitudes = amplitudes,
                        progress = progress,
                        onSeek = { seekProgress ->
                            val seekPosition = (seekProgress * playbackDuration).toInt()
                            onSeek(seekPosition)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Static waveform when not playing
                    StaticWaveform(
                        amplitudes = amplitudes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Show audio controller when playing (legacy, can be removed if waveform is preferred)
            if (isPlaying && playbackDuration > 0 && amplitudes.isEmpty()) {
                AudioController(
                    isPlaying = isAudioActuallyPlaying,
                    currentPosition = currentPosition,
                    duration = playbackDuration,
                    onPlayPauseClick = onPauseResumeClick,
                    onSeek = onSeek,
                    formatTime = formatTime,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Show transcript if available
            if (!voiceNote.transcript.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Transcript",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transcript",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = voiceNote.transcript,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Transcribing indicator
            if (isTranscribing || isConvertingAudio) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConvertingAudio) "Converting audio format..." else "Transcribing speech...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
