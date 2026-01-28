package com.example.dailyvoicejournalapp.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyvoicejournalapp.audio.AudioPlayer
import com.example.dailyvoicejournalapp.audio.AudioRecorder
import com.example.dailyvoicejournalapp.data.PreferencesManager
import com.example.dailyvoicejournalapp.data.UserPreferences
import com.example.dailyvoicejournalapp.data.VoiceNote
import com.example.dailyvoicejournalapp.notifications.ReminderScheduler
import com.example.dailyvoicejournalapp.repository.VoiceNoteRepository
import com.example.dailyvoicejournalapp.utils.StreakCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10

sealed class RecordingState {
    object Idle : RecordingState()
    object Recording : RecordingState()
    data class Playing(val noteId: Long) : RecordingState()
}

data class GroupedVoiceNotes(
    val today: List<VoiceNote>,
    val yesterday: List<VoiceNote>,
    val older: List<VoiceNote>
)

data class Statistics(
    val totalCount: Int = 0,
    val totalDuration: Long = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VoiceNoteRepository(application)
    private val preferencesManager = PreferencesManager(application)
    private val audioRecorder = AudioRecorder(application)
    val audioPlayer = AudioPlayer()
    
    var recordingState by mutableStateOf<RecordingState>(RecordingState.Idle)
        private set
    
    var currentAmplitude by mutableStateOf(0f)
        private set
    
    var currentPlaybackPosition by mutableStateOf(0)
        private set
    
    var currentPlaybackDuration by mutableStateOf(0)
        private set
    
    var isAudioActuallyPlaying by mutableStateOf(false)
        private set
    
    private var currentRecordingFile: File? = null
    private var amplitudeSamplingJob: Job? = null
    private var playbackProgressJob: Job? = null
    
    val groupedVoiceNotes: StateFlow<GroupedVoiceNotes> = repository.getAllVoiceNotes()
        .map { notes -> groupNotesByDate(notes) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GroupedVoiceNotes(emptyList(), emptyList(), emptyList())
        )
    
    val userPreferences: StateFlow<UserPreferences> = preferencesManager.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )
    
    val statistics: StateFlow<Statistics> = kotlinx.coroutines.flow.combine(
        repository.getTotalRecordingsCount(),
        repository.getTotalDuration()
    ) { count, duration ->
        Statistics(count, duration)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Statistics()
    )
    
    private fun groupNotesByDate(notes: List<VoiceNote>): GroupedVoiceNotes {
        val calendar = Calendar.getInstance()
        val today = calendar.apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val yesterday = calendar.apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }.timeInMillis
        
        return GroupedVoiceNotes(
            today = notes.filter { it.timestamp >= today },
            yesterday = notes.filter { it.timestamp >= yesterday && it.timestamp < today },
            older = notes.filter { it.timestamp < yesterday }
        )
    }
    
    fun startRecording() {
        if (recordingState != RecordingState.Idle) return
        
        stopPlayback()
        
        val timestamp = System.currentTimeMillis()
        val fileName = "voice_note_$timestamp.m4a"
        currentRecordingFile = audioRecorder.start(fileName)
        recordingState = RecordingState.Recording
        
        // Start sampling amplitude for waveform visualization
        startAmplitudeSampling()
    }
    
    fun stopRecording() {
        if (recordingState !is RecordingState.Recording) return
        
        // Stop amplitude sampling
        stopAmplitudeSampling()
        
        val duration = audioRecorder.stop()
        val file = currentRecordingFile
        
        if (file != null && file.exists()) {
            viewModelScope.launch {
                repository.insertVoiceNote(
                    VoiceNote(
                        filePath = file.absolutePath,
                        timestamp = System.currentTimeMillis(),
                        duration = duration
                    )
                )
                // Update streak after successful recording
                updateStreakIfNeeded()
            }
        }
        
        currentRecordingFile = null
        recordingState = RecordingState.Idle
        currentAmplitude = 0f
    }
    
    private suspend fun updateStreakIfNeeded() {
        val prefs = userPreferences.value
        val today = StreakCalculator.getTodayDate()
        
        if (StreakCalculator.shouldUpdateStreak(prefs.lastRecordingDate)) {
            val (newStreak, newLongest) = StreakCalculator.calculateStreak(
                prefs.lastRecordingDate,
                prefs.currentStreak,
                prefs.longestStreak
            )
            
            preferencesManager.updateStreak(newStreak, newLongest, today)
        }
    }
    
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val prefs = userPreferences.value
            preferencesManager.updateNotificationSettings(
                enabled,
                prefs.notificationHour,
                prefs.notificationMinute
            )
            
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(
                    getApplication(),
                    prefs.notificationHour,
                    prefs.notificationMinute
                )
            } else {
                ReminderScheduler.cancelDailyReminder(getApplication())
            }
        }
    }
    
    fun playVoiceNote(voiceNote: VoiceNote) {
        if (recordingState is RecordingState.Playing && 
            (recordingState as RecordingState.Playing).noteId == voiceNote.id) {
            stopPlayback()
            return
        }
        
        stopPlayback()
        
        val file = File(voiceNote.filePath)
        if (file.exists()) {
            recordingState = RecordingState.Playing(voiceNote.id)
            isAudioActuallyPlaying = true
            audioPlayer.play(file) {
                if (recordingState is RecordingState.Playing && 
                    (recordingState as RecordingState.Playing).noteId == voiceNote.id) {
                    stopPlaybackProgressTracking()
                    recordingState = RecordingState.Idle
                    isAudioActuallyPlaying = false
                    currentPlaybackPosition = 0
                    currentPlaybackDuration = 0
                }
            }
            startPlaybackProgressTracking()
        }
    }
    
    fun pausePlayback() {
        if (recordingState is RecordingState.Playing && audioPlayer.isPlaying()) {
            audioPlayer.pause()
            isAudioActuallyPlaying = false
        }
    }
    
    fun resumePlayback() {
        if (recordingState is RecordingState.Playing && audioPlayer.isPaused()) {
            audioPlayer.resume()
            isAudioActuallyPlaying = true
        }
    }
    
    fun seekPlayback(position: Int) {
        if (recordingState is RecordingState.Playing) {
            audioPlayer.seekTo(position)
            currentPlaybackPosition = position
        }
    }
    
    fun stopPlayback() {
        if (recordingState is RecordingState.Playing) {
            stopPlaybackProgressTracking()
            audioPlayer.stop()
            recordingState = RecordingState.Idle
            isAudioActuallyPlaying = false
            currentPlaybackPosition = 0
            currentPlaybackDuration = 0
        }
    }
    
    fun deleteVoiceNote(id: Long) {
        if (recordingState is RecordingState.Playing && 
            (recordingState as RecordingState.Playing).noteId == id) {
            stopPlayback()
        }
        
        viewModelScope.launch {
            repository.deleteVoiceNote(id)
        }
    }
    
    fun formatDuration(duration: Long): String {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun startAmplitudeSampling() {
        amplitudeSamplingJob?.cancel()
        amplitudeSamplingJob = viewModelScope.launch {
            while (recordingState is RecordingState.Recording) {
                val amplitude = audioRecorder.getMaxAmplitude()
                // Normalize amplitude to 0-1 range (logarithmic scale for better visualization)
                currentAmplitude = if (amplitude > 0) {
                    val db = 20 * log10(amplitude.toFloat() / 32767f)
                    // Convert dB (-90 to 0) to 0-1 range
                    ((db + 90f) / 90f).coerceIn(0f, 1f)
                } else {
                    0f
                }
                delay(50) // Sample every 50ms for smooth animation
            }
        }
    }
    
    private fun stopAmplitudeSampling() {
        amplitudeSamplingJob?.cancel()
        amplitudeSamplingJob = null
    }
    
    private fun startPlaybackProgressTracking() {
        playbackProgressJob?.cancel()
        playbackProgressJob = viewModelScope.launch {
            while (recordingState is RecordingState.Playing) {
                currentPlaybackPosition = audioPlayer.getCurrentPosition()
                currentPlaybackDuration = audioPlayer.getDuration()
                delay(100) // Update every 100ms for smooth slider movement
            }
        }
    }
    
    private fun stopPlaybackProgressTracking() {
        playbackProgressJob?.cancel()
        playbackProgressJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAmplitudeSampling()
        stopPlaybackProgressTracking()
        audioRecorder.release()
        audioPlayer.release()
    }
}
