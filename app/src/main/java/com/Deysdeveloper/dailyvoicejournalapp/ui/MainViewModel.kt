package com.Deysdeveloper.dailyvoicejournalapp.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Deysdeveloper.dailyvoicejournalapp.audio.AudioPlayer
import com.Deysdeveloper.dailyvoicejournalapp.audio.AudioRecorder
import com.Deysdeveloper.dailyvoicejournalapp.audio.SpeechToTextService
import com.Deysdeveloper.dailyvoicejournalapp.audio.WaveformExtractor
import com.Deysdeveloper.dailyvoicejournalapp.data.PreferencesManager
import com.Deysdeveloper.dailyvoicejournalapp.data.ThemeMode
import com.Deysdeveloper.dailyvoicejournalapp.data.UserPreferences
import com.Deysdeveloper.dailyvoicejournalapp.data.VoiceNote
import com.Deysdeveloper.dailyvoicejournalapp.notifications.ReminderScheduler
import com.Deysdeveloper.dailyvoicejournalapp.repository.VoiceNoteRepository
import com.Deysdeveloper.dailyvoicejournalapp.utils.StreakCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

sealed class TranscriptState {
    object Idle : TranscriptState()
    object Converting : TranscriptState()
    data class Success(val transcript: String) : TranscriptState()
    data class Error(val message: String) : TranscriptState()
}

data class GroupedVoiceNotes(
    val today: List<VoiceNote>,
    val yesterday: List<VoiceNote>,
    val olderByDate: List<DateGroup>,
    // Groups older notes by specific dates
)

data class DateGroup(
    val header: String, // e.g., "Monday, Feb 17"
    val notes: List<VoiceNote>
)

data class Statistics(
    val totalCount: Int = 0,
    val totalDuration: Long = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VoiceNoteRepository(application)
    private val preferencesManager = PreferencesManager(application)
    private val audioRecorder by lazy { AudioRecorder(application) }
    private val speechToTextService by lazy { SpeechToTextService(application) }
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
    
    // Search query as StateFlow so it properly triggers flow recombination
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Speech-to-text state
    private val _transcriptStates = MutableStateFlow<Map<Long, TranscriptState>>(emptyMap())
    val transcriptStates = _transcriptStates.asStateFlow()
    
    private var currentRecordingFile: File? = null
    private var amplitudeSamplingJob: Job? = null
    private var playbackProgressJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var recordingStartTime: Long = 0L
    private var currentTranscribingNoteId: Long? = null
    
    private val allVoiceNotes: StateFlow<List<VoiceNote>> = repository.getAllVoiceNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val groupedVoiceNotes: StateFlow<GroupedVoiceNotes> = 
        combine(allVoiceNotes, _searchQuery) { notes, query ->
            val filteredNotes = if (query.isBlank()) {
                notes
            } else {
                notes.filter { note ->
                    val title = note.title ?: getDefaultTitle(note.timestamp)
                    val transcript = note.transcript ?: ""
                    title.contains(query, ignoreCase = true) ||
                    transcript.contains(query, ignoreCase = true)
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
    
    // Statistics flow - using Eagerly to prevent reset to 0 during scroll/record
    val statistics: StateFlow<Statistics> = combine(
        repository.getTotalRecordingsCount(),
        repository.getTotalDuration()
    ) { count, duration ->
        Statistics(count, duration)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = Statistics(0, 0)
    )

    /**
     * Get activity data for the last 7 days.
     * Returns a list of 7 booleans indicating whether the user recorded on each day.
     * Index 0 = 6 days ago, Index 6 = today
     */
    fun getLastSevenDaysActivity(): List<Boolean> {
        val calendar = Calendar.getInstance()
        val activity = mutableListOf<Boolean>()

        // Go back 6 days from today
        calendar.add(Calendar.DAY_OF_MONTH, -6)

        repeat(7) {
            val dayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dayEnd = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // Check if there are any notes on this day
            val hasRecording = allVoiceNotes.value.any { note ->
                note.timestamp in dayStart..dayEnd
            }
            activity.add(hasRecording)

            // Move to next day
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return activity
    }

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

        val todayNotes = notes.filter { it.timestamp >= today }
        val yesterdayNotes = notes.filter { it.timestamp >= yesterday && it.timestamp < today }
        val olderNotes = notes.filter { it.timestamp < yesterday }

        // Group older notes by date
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val olderByDate = olderNotes
            .groupBy { note ->
                // Get just the date part (midnight timestamp)
                val noteCalendar = Calendar.getInstance().apply {
                    timeInMillis = note.timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                noteCalendar.timeInMillis
            }
            .toSortedMap(reverseOrder()) // Most recent first
            .map { (dateMillis, notesForDate) ->
                DateGroup(
                    header = dateFormat.format(Date(dateMillis)),
                    notes = notesForDate.sortedByDescending { it.timestamp }
                )
            }

        return GroupedVoiceNotes(
            today = todayNotes.sortedByDescending { it.timestamp },
            yesterday = yesterdayNotes.sortedByDescending { it.timestamp },
            olderByDate = olderByDate
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
                    val noteId = repository.insertVoiceNote(
                        VoiceNote(
                            filePath = file.absolutePath,
                            timestamp = System.currentTimeMillis(),
                            duration = duration
                        )
                    )
                    // Update streak after successful recording
                    updateStreakIfNeeded()

                    // Extract waveform data for playback visualization
                    extractAndSaveWaveform(noteId, file.absolutePath)
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to stop recording", e)
        } finally {
            currentRecordingFile = null
            recordingState = RecordingState.Idle
            currentAmplitude = 0f
            recordingElapsedTime = 0L
        }
    }

    private suspend fun extractAndSaveWaveform(noteId: Long, filePath: String) {
        try {
            val amplitudes = WaveformExtractor.extractWaveform(filePath)
            if (amplitudes.isNotEmpty()) {
                val waveformString = WaveformExtractor.serializeWaveform(amplitudes)
                repository.updateVoiceNoteWaveform(noteId, waveformString)
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to extract waveform", e)
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

    fun updateNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val prefs = userPreferences.value
            preferencesManager.updateNotificationSettings(
                prefs.notificationEnabled,
                hour,
                minute
            )

            if (prefs.notificationEnabled) {
                ReminderScheduler.scheduleDailyReminder(
                    getApplication(),
                    hour,
                    minute
                )
            }
        }
    }
    
    fun toggleLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateLockEnabled(enabled)
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.updateThemeMode(themeMode)
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
    
    fun updateVoiceNoteTranscript(id: Long, transcript: String?) {
        viewModelScope.launch {
            repository.updateVoiceNoteTranscript(id, transcript)
            _transcriptStates.value = _transcriptStates.value.toMutableMap().apply {
                remove(id)
            }
        }
    }
    
    /**
     * Converts speech in the audio file to text using Vosk.
     * This runs entirely on-device with no internet required.
     * 
     * Requires the Vosk model to be downloaded first.
     */
    fun convertSpeechToText(voiceNote: VoiceNote) {
        viewModelScope.launch {
            try {
                _transcriptStates.value = _transcriptStates.value.toMutableMap().apply {
                    put(voiceNote.id, TranscriptState.Converting)
                }
                currentTranscribingNoteId = voiceNote.id

                // Use Vosk for transcription
                val result = speechToTextService.transcribeAudioFile(voiceNote.filePath)

                result.fold(
                    onSuccess = { transcript ->
                        if (transcript.isNotBlank()) {
                            updateVoiceNoteTranscript(voiceNote.id, transcript)
                            _transcriptStates.value = _transcriptStates.value.toMutableMap().apply {
                                put(voiceNote.id, TranscriptState.Success(transcript))
                            }
                        } else {
                            _transcriptStates.value = _transcriptStates.value.toMutableMap().apply {
                                put(voiceNote.id, TranscriptState.Error("No speech recognized. The audio may be too short or unclear."))
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e("MainViewModel", "Transcription failed", error)
                        _transcriptStates.value = _transcriptStates.value.toMutableMap().apply {
                            put(voiceNote.id, TranscriptState.Error(error.message ?: "Transcription failed. Please try again or add transcript manually."))
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Unexpected error during transcription", e)
                _transcriptStates.value = _transcriptStates.value.toMutableMap().apply {
                    put(voiceNote.id, TranscriptState.Error("Unexpected error: ${e.message}"))
                }
            } finally {
                currentTranscribingNoteId = null
            }
        }
    }
    
    /**
     * Check if the Vosk speech model is available.
     */
    fun isSpeechModelAvailable(): Boolean {
        return speechToTextService.isModelAvailable()
    }
    
    /**
     * Get instructions for downloading the speech model.
     */
    fun getSpeechModelInstructions(): String {
        return speechToTextService.getModelDownloadInstructions()
    }
    
    /**
     * Initialize the speech model. Call this on app startup.
     * @param autoDownload If true, will automatically download the model if not present
     */
    fun initializeSpeechModel(autoDownload: Boolean = false) {
        viewModelScope.launch {
            speechToTextService.initializeModel(autoDownload)
        }
    }
    
    /**
     * Download the Vosk speech model automatically.
     */
    fun downloadSpeechModel() {
        viewModelScope.launch {
            speechToTextService.modelDownloader.downloadModel()
        }
    }
    
    /**
     * Get the model download state flow.
     */
    val modelDownloadState = speechToTextService.modelDownloader.downloadState
    
    fun setTranscriptManually(id: Long, transcript: String) {
        updateVoiceNoteTranscript(id, transcript)
    }
    
    fun clearTranscriptError(id: Long) {
        _transcriptStates.value = _transcriptStates.value.toMutableMap().apply {
            remove(id)
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
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
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
        speechToTextService.destroy()
    }
}
