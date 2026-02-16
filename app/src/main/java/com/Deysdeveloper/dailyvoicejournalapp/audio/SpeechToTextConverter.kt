package com.Deysdeveloper.dailyvoicejournalapp.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechToTextConverter(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    private val _transcriptState = MutableStateFlow<TranscriptState>(TranscriptState.Idle)
    val transcriptState: StateFlow<TranscriptState> = _transcriptState.asStateFlow()
    
    sealed class TranscriptState {
        object Idle : TranscriptState()
        object Listening : TranscriptState()
        data class Success(val transcript: String) : TranscriptState()
        data class Error(val message: String) : TranscriptState()
    }
    
    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        } else {
            Log.w("SpeechToText", "Speech recognition not available on this device")
        }
    }
    
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechToText", "Ready for speech")
                _transcriptState.value = TranscriptState.Listening
            }
            
            override fun onBeginningOfSpeech() {
                Log.d("SpeechToText", "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changes - can be used for UI feedback
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                Log.d("SpeechToText", "End of speech")
                isListening = false
            }
            
            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                Log.e("SpeechToText", "Error: $errorMessage (code: $error)")
                _transcriptState.value = TranscriptState.Error(errorMessage)
            }
            
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val transcript = matches[0]
                    Log.d("SpeechToText", "Transcript: $transcript")
                    _transcriptState.value = TranscriptState.Success(transcript)
                } else {
                    _transcriptState.value = TranscriptState.Error("No speech recognized")
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty()) {
                    Log.d("SpeechToText", "Partial transcript: ${partial[0]}")
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle events if needed
            }
        }
    }
    
    fun startListening() {
        if (speechRecognizer == null) {
            _transcriptState.value = TranscriptState.Error("Speech recognition not available")
            return
        }
        
        if (isListening) {
            stopListening()
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            isListening = true
            _transcriptState.value = TranscriptState.Listening
            speechRecognizer?.startListening(intent)
            Log.d("SpeechToText", "Started listening")
        } catch (e: Exception) {
            isListening = false
            Log.e("SpeechToText", "Failed to start listening", e)
            _transcriptState.value = TranscriptState.Error("Failed to start: ${e.message}")
        }
    }
    
    fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
                Log.d("SpeechToText", "Stopped listening")
            } catch (e: Exception) {
                Log.e("SpeechToText", "Error stopping listening", e)
            }
            isListening = false
        }
    }
    
    fun resetState() {
        _transcriptState.value = TranscriptState.Idle
    }
    
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
