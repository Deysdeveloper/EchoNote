package com.deysdeveloper.echonote.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deysdeveloper.echonote.audio.AudioPlayer
import com.deysdeveloper.echonote.audio.AudioRecorder
import com.deysdeveloper.echonote.data.PreferencesManager
import com.deysdeveloper.echonote.data.UserPreferences
import com.deysdeveloper.echonote.data.VoiceNote
import com.deysdeveloper.echonote.notifications.ReminderScheduler
import com.deysdeveloper.echonote.repository.VoiceNoteRepository
import com.deysdeveloper.echonote.utils.StreakCalculator
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
    private val audioRecorder by lazy { AudioRecorder(application) }
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
    
    var recordingElapsedTime by mutableStateOf(0L)
        private set
    
    var searchQuery by mutableStateOf("")
        private set
    
    private var currentRecordingFile: File? = null
    private var amplitudeSamplingJob: Job? = null
    private var playbackProgressJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var recordingStartTime: Long = 0L
    
    private val allVoiceNotes: StateFlow<List<VoiceNote>> = repository.getAllVoiceNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val groupedVoiceNotes: StateFlow<GroupedVoiceNotes> = allVoiceNotes
        .map { notes ->
            val filteredNotes = if (searchQuery.isBlank()) {
                notes
            } else {
                notes.filter { note ->
                    val title = note.title ?: getDefaultTitle(note.timestamp)
                    title.contains(searchQuery, ignoreCase = true)
                }
            }
            groupNotesByDate(filteredNotes)
        }
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
        
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "voice_note_$timestamp.m4a"
            currentRecordingFile = audioRecorder.start(fileName)
            recordingState = RecordingState.Recording
            recordingStartTime = System.currentTimeMillis()
            recordingElapsedTime = 0L
            
            // Start sampling amplitude for waveform visualization
            startAmplitudeSampling()
            // Start recording timer
            startRecordingTimer()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to start recording", e)
            // Reset state if recording fails
            recordingState = RecordingState.Idle
            currentRecordingFile = null
        }
    }
    
    fun stopRecording() {
        if (recordingState !is RecordingState.Recording) return
        
        // Stop amplitude sampling and timer
        stopAmplitudeSampling()
        stopRecordingTimer()
        
        try {
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
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to stop recording", e)
        } finally {
            currentRecordingFile = null
            recordingState = RecordingState.Idle
            currentAmplitude = 0f
            recordingElapsedTime = 0L
        }
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
    
    fun toggleLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateLockEnabled(enabled)
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
    
    fun updateVoiceNoteTitle(id: Long, title: String?) {
        viewModelScope.launch {
            repository.updateVoiceNoteTitle(id, title)
        }
    }
    
    fun getDefaultTitle(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 5..11 -> "Morning Thought"
            in 12..16 -> "Afternoon Note"
            in 17..20 -> "Evening Reflection"
            else -> "Night Journal"
        }
    }
    
    fun updateSearchQuery(query: String) {
        searchQuery = query
    }
    
    fun clearSearch() {
        searchQuery = ""
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
                try {
                    val amplitude = audioRecorder.getMaxAmplitude()
                    // Normalize amplitude to 0-1 range (logarithmic scale for better visualization)
                    val normalizedAmplitude = if (amplitude > 0) {
                        val db = 20 * log10(amplitude.toFloat() / 32767f)
                        // Convert dB (-90 to 0) to 0-1 range
                        ((db + 90f) / 90f).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    currentAmplitude = normalizedAmplitude
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error sampling amplitude", e)
                    currentAmplitude = 0f
                }
                // Increased from 50ms to 100ms for better performance on tablets
                delay(100)
            }
        }
    }
    
    private fun stopAmplitudeSampling() {
        amplitudeSamplingJob?.cancel()
        amplitudeSamplingJob = null
    }
    
    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (recordingState is RecordingState.Recording) {
                recordingElapsedTime = System.currentTimeMillis() - recordingStartTime
                delay(100) // Update every 100ms for smooth timer
            }
        }
    }
    
    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }
    
    fun formatRecordingTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
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
        stopRecordingTimer()
        stopPlaybackProgressTracking()
        audioRecorder.release()
        audioPlayer.release()
    }
}
