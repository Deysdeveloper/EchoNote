package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Deysdeveloper.dailyvoicejournalapp.audio.WaveformExtractor
import com.Deysdeveloper.dailyvoicejournalapp.data.VoiceNote
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.WarmTeal
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.SoftCoral
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.CardGradientLight
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.CardGradientDark

@Composable
fun BeautifulVoiceNoteItem(
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

    // Glassmorphism card
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        // Main card with glass effect
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header with title and actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Title with custom font weight
                            Text(
                                text = voiceNote.title ?: getDefaultTitle(voiceNote.timestamp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Time with subtle styling
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatTime(voiceNote.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Duration pill
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = formatDuration(voiceNote.duration),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Action buttons row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Transcript button
                            IconButton(
                                onClick = onEditTranscriptClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Transcript",
                                    tint = if (voiceNote.transcript.isNullOrBlank())
                                        MaterialTheme.colorScheme.outline
                                    else
                                        WarmTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Auto-transcribe button
                            if (voiceNote.transcript.isNullOrBlank() && !isTranscribing) {
                                IconButton(
                                    onClick = onAutoTranscribeClick,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = "Auto-transcribe",
                                        tint = if (canAutoTranscribe) SoftCoral else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Edit button
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Share button
                            IconButton(
                                onClick = onShareClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Waveform visualization
                    if (amplitudes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Play button with gradient background
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        brush = if (isPlaying) {
                                            Brush.radialGradient(
                                                colors = listOf(SoftCoral, SoftCoral.copy(alpha = 0.8f))
                                            )
                                        } else {
                                            Brush.radialGradient(
                                                colors = listOf(WarmTeal, WarmTeal.copy(alpha = 0.8f))
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(onClick = onPlayClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Waveform
                            Box(modifier = Modifier.weight(1f)) {
                                if (isPlaying && playbackDuration > 0) {
                                    PlaybackWaveform(
                                        amplitudes = amplitudes,
                                        progress = progress,
                                        onSeek = { seekProgress ->
                                            val seekPosition = (seekProgress * playbackDuration).toInt()
                                            onSeek(seekPosition)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        barCount = 50,
                                        barWidth = 3.dp,
                                        barHeight = 36.dp
                                    )
                                } else {
                                    StaticWaveform(
                                        amplitudes = amplitudes,
                                        modifier = Modifier.fillMaxWidth(),
                                        barCount = 50,
                                        barWidth = 3.dp,
                                        barHeight = 36.dp,
                                        barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Simple play button row when no waveform
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = onPlayClick,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isPlaying) SoftCoral else WarmTeal
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop" else "Play"
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = formatDuration(voiceNote.duration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Transcript section
                    if (!voiceNote.transcript.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Transcript",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = voiceNote.transcript,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Transcribing indicator
                    if (isTranscribing || isConvertingAudio) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isConvertingAudio) "Converting audio..." else "Transcribing speech...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
