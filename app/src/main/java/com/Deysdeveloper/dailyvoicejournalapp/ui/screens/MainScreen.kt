package com.Deysdeveloper.dailyvoicejournalapp.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.Deysdeveloper.dailyvoicejournalapp.audio.ModelDownloader
import com.Deysdeveloper.dailyvoicejournalapp.ui.MainViewModel
import com.Deysdeveloper.dailyvoicejournalapp.ui.RecordingState
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.DateHeader
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.EmptyStateView
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.ModelDownloadDialog
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.RecordButton
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.RenameDialog
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.SettingsDialog
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.SpeechModelSetupDialog
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.StatisticsCard
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.TranscriptDialog
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.VoiceNoteItem
import com.Deysdeveloper.dailyvoicejournalapp.ui.components.WaveformVisualizerLive
import com.Deysdeveloper.dailyvoicejournalapp.data.VoiceNote
import com.Deysdeveloper.dailyvoicejournalapp.ui.TranscriptState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hasRecordPermission: Boolean,
    onRequestPermission: () -> Unit,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val groupedNotes by viewModel.groupedVoiceNotes.collectAsState()
    val recordingState = viewModel.recordingState
    val statistics by viewModel.statistics.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val transcriptStates by viewModel.transcriptStates.collectAsState()
    val modelDownloadState by viewModel.modelDownloadState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var noteToRename by remember { mutableStateOf<VoiceNote?>(null) }
    var noteToEditTranscript by remember { mutableStateOf<VoiceNote?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showModelSetupDialog by remember { mutableStateOf(false) }
    var showModelDownloadDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Initialize speech model on startup (check if exists, don't auto-download)
    LaunchedEffect(Unit) {
        viewModel.initializeSpeechModel(autoDownload = false)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Daily Voice Journal",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by title or transcript...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Statistics Card - shown when there are recordings
            if (statistics.totalCount > 0) {
                item {
                    StatisticsCard(
                        totalRecordings = statistics.totalCount,
                        totalDuration = statistics.totalDuration,
                        currentStreak = userPreferences.currentStreak,
                        longestStreak = userPreferences.longestStreak
                    )
                }
            }
            
            // Record button section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RecordButton(
                            isRecording = recordingState is RecordingState.Recording,
                            onRecordClick = {
                                if (!hasRecordPermission) {
                                    onRequestPermission()
                                } else {
                                    if (recordingState is RecordingState.Recording) {
                                        viewModel.stopRecording()
                                    } else {
                                        viewModel.startRecording()
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = when (recordingState) {
                                is RecordingState.Recording -> "Recording... ${viewModel.formatRecordingTime(viewModel.recordingElapsedTime)}"
                                is RecordingState.Playing -> "Playing..."
                                else -> "Tap to record"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        
                        // Show waveform visualizer when recording
                        if (recordingState is RecordingState.Recording) {
                            Spacer(modifier = Modifier.height(16.dp))
                            WaveformVisualizerLive(
                                amplitude = viewModel.currentAmplitude,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .padding(horizontal = 16.dp),
                                barColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Divider
            item {
                HorizontalDivider()
            }
            
            // Empty state or voice notes list
            if (groupedNotes.today.isEmpty() &&
                groupedNotes.yesterday.isEmpty() &&
                groupedNotes.olderByDate.isEmpty()) {
                item {
                    EmptyStateView()
                }
            } else {
                    // Today section
                    if (groupedNotes.today.isNotEmpty()) {
                        item {
                            DateHeader(text = "Today")
                        }
                        items(groupedNotes.today) { note ->
                            val isThisNotePlaying = recordingState is RecordingState.Playing && 
                                           (recordingState as RecordingState.Playing).noteId == note.id
                            val transcriptState = transcriptStates[note.id]
                            val isTranscribing = transcriptState is TranscriptState.Converting
                            val isConvertingAudio = false
                            VoiceNoteItem(
                                voiceNote = note,
                                isPlaying = isThisNotePlaying,
                                isAudioActuallyPlaying = if (isThisNotePlaying) viewModel.isAudioActuallyPlaying else false,
                                currentPosition = if (isThisNotePlaying) viewModel.currentPlaybackPosition else 0,
                                playbackDuration = if (isThisNotePlaying) viewModel.currentPlaybackDuration else 0,
                                isTranscribing = isTranscribing,
                                isConvertingAudio = isConvertingAudio,
                                canAutoTranscribe = viewModel.isSpeechModelAvailable(),
                                formatTime = viewModel::formatTime,
                                formatDuration = viewModel::formatDuration,
                                getDefaultTitle = viewModel::getDefaultTitle,
                                onPlayClick = { viewModel.playVoiceNote(note) },
                                onPauseResumeClick = {
                                    if (viewModel.isAudioActuallyPlaying) {
                                        viewModel.pausePlayback()
                                    } else {
                                        viewModel.resumePlayback()
                                    }
                                },
                                onSeek = { position -> viewModel.seekPlayback(position) },
                                onEditClick = { noteToRename = note },
                                onDeleteClick = { viewModel.deleteVoiceNote(note.id) },
                                onShareClick = { shareVoiceNote(context, note.filePath) },
                                onEditTranscriptClick = { noteToEditTranscript = note },
                                                                onAutoTranscribeClick = {
                                    if (viewModel.isSpeechModelAvailable()) {
                                        viewModel.convertSpeechToText(note)
                                    } else {
                                        showModelDownloadDialog = true
                                    }
                                }
                            )
                        }
                    }
                    
                    // Yesterday section
                    if (groupedNotes.yesterday.isNotEmpty()) {
                        item {
                            DateHeader(text = "Yesterday")
                        }
                        items(groupedNotes.yesterday) { note ->
                            val isThisNotePlaying = recordingState is RecordingState.Playing && 
                                           (recordingState as RecordingState.Playing).noteId == note.id
                            val transcriptState = transcriptStates[note.id]
                            val isTranscribing = transcriptState is TranscriptState.Converting
                            VoiceNoteItem(
                                voiceNote = note,
                                isPlaying = isThisNotePlaying,
                                isAudioActuallyPlaying = if (isThisNotePlaying) viewModel.isAudioActuallyPlaying else false,
                                currentPosition = if (isThisNotePlaying) viewModel.currentPlaybackPosition else 0,
                                playbackDuration = if (isThisNotePlaying) viewModel.currentPlaybackDuration else 0,
                                isTranscribing = isTranscribing,
                                canAutoTranscribe = viewModel.isSpeechModelAvailable(),
                                formatTime = viewModel::formatTime,
                                formatDuration = viewModel::formatDuration,
                                getDefaultTitle = viewModel::getDefaultTitle,
                                onPlayClick = { viewModel.playVoiceNote(note) },
                                onPauseResumeClick = {
                                    if (viewModel.isAudioActuallyPlaying) {
                                        viewModel.pausePlayback()
                                    } else {
                                        viewModel.resumePlayback()
                                    }
                                },
                                onSeek = { position -> viewModel.seekPlayback(position) },
                                onEditClick = { noteToRename = note },
                                onDeleteClick = { viewModel.deleteVoiceNote(note.id) },
                                onShareClick = { shareVoiceNote(context, note.filePath) },
                                onEditTranscriptClick = { noteToEditTranscript = note },
                                                                onAutoTranscribeClick = {
                                    if (viewModel.isSpeechModelAvailable()) {
                                        viewModel.convertSpeechToText(note)
                                    } else {
                                        showModelDownloadDialog = true
                                    }
                                }
                            )
                        }
                    }
                    
                    // Older notes grouped by date
                    groupedNotes.olderByDate.forEach { dateGroup ->
                        item {
                            DateHeader(text = dateGroup.header)
                        }
                        items(dateGroup.notes) { note ->
                            val isThisNotePlaying = recordingState is RecordingState.Playing &&
                                           (recordingState as RecordingState.Playing).noteId == note.id
                            val transcriptState = transcriptStates[note.id]
                            val isTranscribing = transcriptState is TranscriptState.Converting
                            VoiceNoteItem(
                                voiceNote = note,
                                isPlaying = isThisNotePlaying,
                                isAudioActuallyPlaying = if (isThisNotePlaying) viewModel.isAudioActuallyPlaying else false,
                                currentPosition = if (isThisNotePlaying) viewModel.currentPlaybackPosition else 0,
                                playbackDuration = if (isThisNotePlaying) viewModel.currentPlaybackDuration else 0,
                                isTranscribing = isTranscribing,
                                canAutoTranscribe = viewModel.isSpeechModelAvailable(),
                                formatTime = viewModel::formatTime,
                                formatDuration = viewModel::formatDuration,
                                getDefaultTitle = viewModel::getDefaultTitle,
                                onPlayClick = { viewModel.playVoiceNote(note) },
                                onPauseResumeClick = {
                                    if (viewModel.isAudioActuallyPlaying) {
                                        viewModel.pausePlayback()
                                    } else {
                                        viewModel.resumePlayback()
                                    }
                                },
                                onSeek = { position -> viewModel.seekPlayback(position) },
                                onEditClick = { noteToRename = note },
                                onDeleteClick = { viewModel.deleteVoiceNote(note.id) },
                                onShareClick = { shareVoiceNote(context, note.filePath) },
                                onEditTranscriptClick = { noteToEditTranscript = note },
                                onAutoTranscribeClick = {
                                    if (viewModel.isSpeechModelAvailable()) {
                                        viewModel.convertSpeechToText(note)
                                    } else {
                                        showModelDownloadDialog = true
                                    }
                                }
                            )
                        }
                    }
                    
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        }
    }
    
    // Rename dialog
    noteToRename?.let { note ->
        RenameDialog(
            currentTitle = note.title,
            onDismiss = { noteToRename = null },
            onConfirm = { newTitle ->
                viewModel.updateVoiceNoteTitle(note.id, newTitle)
                noteToRename = null
            }
        )
    }
    
    // Settings dialog
    if (showSettingsDialog) {
        SettingsDialog(
            notificationEnabled = userPreferences.notificationEnabled,
            notificationHour = userPreferences.notificationHour,
            notificationMinute = userPreferences.notificationMinute,
            lockEnabled = userPreferences.lockEnabled,
            onDismiss = { showSettingsDialog = false },
            onNotificationToggle = { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (hasNotificationPermission) {
                        viewModel.toggleNotifications(true)
                    } else {
                        onRequestNotificationPermission()
                    }
                } else {
                    viewModel.toggleNotifications(enabled)
                }
            },
            onTimeClick = {
                showTimePicker = true
            },
            onLockToggle = { enabled ->
                viewModel.toggleLock(enabled)
            }
        )
    }

    // Time Picker Dialog - Full screen style for better UX
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = userPreferences.notificationHour,
            initialMinute = userPreferences.notificationMinute,
            is24Hour = false
        )

        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Reminder Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TimePicker(
                        state = timePickerState,
                        layoutType = TimePickerLayoutType.Vertical
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.updateNotificationTime(timePickerState.hour, timePickerState.minute)
                                showTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
    
    // Transcript dialog
    noteToEditTranscript?.let { note ->
        TranscriptDialog(
            currentTranscript = note.transcript,
            onDismiss = { noteToEditTranscript = null },
            onSave = { transcript ->
                viewModel.setTranscriptManually(note.id, transcript ?: "")
                noteToEditTranscript = null
            }
        )
    }
    
    // Speech model setup dialog (fallback)
    if (showModelSetupDialog) {
        SpeechModelSetupDialog(
            instructions = viewModel.getSpeechModelInstructions(),
            onDismiss = { showModelSetupDialog = false }
        )
    }
    
    // Model download dialog
    if (showModelDownloadDialog || modelDownloadState is ModelDownloader.DownloadState.Downloading || modelDownloadState is ModelDownloader.DownloadState.Extracting) {
        ModelDownloadDialog(
            downloadState = modelDownloadState,
            onDismiss = { 
                showModelDownloadDialog = false
                if (modelDownloadState is ModelDownloader.DownloadState.Success) {
                    // Re-initialize the model after successful download
                    viewModel.initializeSpeechModel(autoDownload = false)
                }
            },
            onDownloadClick = {
                viewModel.downloadSpeechModel()
            },
            onRetryClick = {
                viewModel.downloadSpeechModel()
            }
        )
    }
}

private fun shareVoiceNote(context: Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) return
    
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"  // MIME type for M4A/AAC files
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Voice Recording")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share voice note").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
