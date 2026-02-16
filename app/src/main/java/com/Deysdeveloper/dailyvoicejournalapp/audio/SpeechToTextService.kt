package com.Deysdeveloper.dailyvoicejournalapp.audio

import android.content.Context
import android.util.Log
import com.sun.jna.Native
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vosk-based Speech-to-Text Service for transcribing audio files.
 * Vosk is an offline speech recognition toolkit that runs entirely on-device.
 * 
 * Features:
 * - Completely offline - no internet required
 * - Supports multiple languages
 * - Small model size (~50MB)
 * - Real-time and batch transcription
 */
class SpeechToTextService(private val context: Context) {
    
    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()
    
    private var model: Model? = null
    private var isModelLoading = false
    
    // Model downloader for automatic model download
    val modelDownloader = ModelDownloader(context)
    
    // Audio converter for converting M4A to WAV
    private val audioConverter = AudioConverter(context)
    
    sealed class TranscriptionState {
        object Idle : TranscriptionState()
        object LoadingModel : TranscriptionState()
        object Converting : TranscriptionState()
        object Processing : TranscriptionState()
        data class Success(val transcript: String) : TranscriptionState()
        data class Error(val message: String) : TranscriptionState()
    }
    
    /**
     * Initialize the Vosk model. This should be called when the app starts
     * or before the first transcription.
     * @param autoDownload If true, automatically downloads the model if not available
     * @return true if model is ready, false if download is needed
     */
    suspend fun initializeModel(autoDownload: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (model != null) return@withContext true
        if (isModelLoading) {
            // Wait for loading to complete
            while (isModelLoading) {
                delay(100)
            }
            return@withContext model != null
        }
        
        isModelLoading = true
        _transcriptionState.value = TranscriptionState.LoadingModel
        
        try {
            // Check if model already exists in storage
            val modelPath = File(modelDownloader.getModelPath())
            
            if (modelDownloader.isModelAvailable()) {
                Log.d("SpeechToTextService", "Loading existing Vosk model from: ${modelPath.absolutePath}")
                
                // Initialize JNA before loading Vosk
                try {
                    Native.register("jnidispatch")
                    Log.d("SpeechToTextService", "JNA initialized successfully")
                } catch (e: Exception) {
                    Log.w("SpeechToTextService", "JNA pre-initialization failed, will try during model load", e)
                }
                
                model = Model(modelPath.absolutePath)
                Log.d("SpeechToTextService", "Vosk model loaded successfully")
                isModelLoading = false
                _transcriptionState.value = TranscriptionState.Idle
                return@withContext true
            }
            
            // Model doesn't exist
            Log.w("SpeechToTextService", "Vosk model not found. Searched at: ${File(context.filesDir, "vosk-model").absolutePath}")
            Log.w("SpeechToTextService", "Model path resolved to: ${modelDownloader.getModelPath()}")
            
            if (autoDownload) {
                // Download the model automatically
                Log.d("SpeechToTextService", "Auto-downloading model...")
                val downloaded = modelDownloader.downloadModel()
                if (downloaded && modelDownloader.isModelAvailable()) {
                    // Get the correct model path after download
                    val correctPath = File(modelDownloader.getModelPath())
                    // Try to load the model after download
                    model = Model(correctPath.absolutePath)
                    Log.d("SpeechToTextService", "Vosk model loaded after download from: ${correctPath.absolutePath}")
                    isModelLoading = false
                    _transcriptionState.value = TranscriptionState.Idle
                    return@withContext true
                } else {
                    _transcriptionState.value = TranscriptionState.Error("Failed to download speech model")
                    isModelLoading = false
                    return@withContext false
                }
            } else {
                _transcriptionState.value = TranscriptionState.Error("Speech model not found. Tap download to get it.")
                isModelLoading = false
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e("SpeechToTextService", "Failed to load Vosk model", e)
            _transcriptionState.value = TranscriptionState.Error("Failed to load speech model: ${e.message}")
            isModelLoading = false
            false
        }
    }
    
    /**
     * Transcribes an audio file to text using Vosk.
     * 
     * @param filePath Path to the audio file (supports WAV format best)
     * @return Result containing the transcript or error
     */
    suspend fun transcribeAudioFile(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        // Ensure model is loaded
        if (model == null) {
            val initialized = initializeModel()
            if (!initialized) {
                return@withContext Result.failure(
                    Exception("Speech model not available. Please download the Vosk model.")
                )
            }
        }
        
        _transcriptionState.value = TranscriptionState.Processing
        
        try {
            val file = File(filePath)
            if (!file.exists()) {
                _transcriptionState.value = TranscriptionState.Error("Audio file not found")
                return@withContext Result.failure(Exception("Audio file not found: $filePath"))
            }
            
            Log.d("SpeechToTextService", "Starting transcription of: ${file.absolutePath}")
            Log.d("SpeechToTextService", "File size: ${file.length()} bytes")
            
            // Check if file needs conversion
            val wavFilePath = if (filePath.endsWith(".wav", ignoreCase = true)) {
                filePath
            } else {
                // Convert to WAV format
                val tempWavPath = audioConverter.getTempWavPath(filePath)
                _transcriptionState.value = TranscriptionState.Converting
                
                try {
                    val converted = audioConverter.convertToWav(filePath, tempWavPath)
                    if (!converted) {
                        Log.w("SpeechToTextService", "Audio conversion failed, trying direct processing")
                        // If conversion fails, we'll try to process the original file anyway
                        // Vosk might still be able to handle it
                        _transcriptionState.value = TranscriptionState.Processing
                        filePath
                    } else {
                        tempWavPath
                    }
                } catch (e: Exception) {
                    Log.e("SpeechToTextService", "Audio conversion crashed", e)
                    _transcriptionState.value = TranscriptionState.Error("Audio conversion failed: ${e.message}")
                    return@withContext Result.failure(e)
                }
            }
            
            val transcript = try {
                processAudioFile(File(wavFilePath))
            } catch (e: Exception) {
                Log.e("SpeechToTextService", "Transcription processing failed", e)
                _transcriptionState.value = TranscriptionState.Error("Transcription failed: ${e.message}")
                return@withContext Result.failure(e)
            }
            
            // Cleanup temp file if it was created
            if (wavFilePath != filePath && File(wavFilePath).exists()) {
                File(wavFilePath).delete()
            }
            
            _transcriptionState.value = TranscriptionState.Success(transcript)
            Result.success(transcript)
            
        } catch (e: Exception) {
            Log.e("SpeechToTextService", "Transcription failed", e)
            _transcriptionState.value = TranscriptionState.Error("Transcription failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Process the audio file and extract text.
     * This is a basic implementation that assumes the audio is in a compatible format.
     * For production, you may need to convert the audio to 16kHz, 16-bit, mono WAV format.
     */
    private fun processAudioFile(file: File): String {
        Log.d("SpeechToTextService", "processAudioFile started: ${file.absolutePath}, size: ${file.length()}")
        val recognizer = Recognizer(model, 16000.0f)
        
        try {
            val bufferSize = 4096
            val buffer = ByteArray(bufferSize)
            val stringBuilder = StringBuilder()
            var totalBytesRead = 0
            var chunksProcessed = 0
            
            FileInputStream(file).use { inputStream ->
                // Skip WAV header (44 bytes) if it's a WAV file
                if (file.name.endsWith(".wav", ignoreCase = true)) {
                    inputStream.skip(44)
                    Log.d("SpeechToTextService", "Skipped WAV header")
                }
                
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                    totalBytesRead += bytesRead
                    chunksProcessed++
                    
                    // Convert bytes to short array (16-bit samples)
                    val shortBuffer = ByteBuffer.wrap(buffer, 0, bytesRead)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                    val shorts = ShortArray(bytesRead / 2)
                    shortBuffer.get(shorts)
                    
                    // Process with Vosk
                    if (recognizer.acceptWaveForm(shorts, shorts.size)) {
                        val result = recognizer.result
                        Log.d("SpeechToTextService", "Intermediate result: $result")
                        // Extract text from JSON result
                        val text = extractTextFromResult(result)
                        if (text.isNotBlank()) {
                            stringBuilder.append(text).append(" ")
                            Log.d("SpeechToTextService", "Added text: $text")
                        }
                    }
                }
            }
            
            Log.d("SpeechToTextService", "Processed $chunksProcessed chunks, $totalBytesRead bytes")
            
            // Get final result
            val finalResult = recognizer.finalResult
            Log.d("SpeechToTextService", "Final result: $finalResult")
            val finalText = extractTextFromResult(finalResult)
            if (finalText.isNotBlank()) {
                stringBuilder.append(finalText)
                Log.d("SpeechToTextService", "Added final text: $finalText")
            }
            
            val result = stringBuilder.toString().trim()
            Log.d("SpeechToTextService", "Transcription complete: '$result'")
            return result
            
        } finally {
            recognizer.close()
            Log.d("SpeechToTextService", "Recognizer closed")
        }
    }
    
    /**
     * Extract text from Vosk's JSON result.
     * Vosk returns results like: {"text": "hello world"} or {"text" : "hello world"}
     */
    private fun extractTextFromResult(jsonResult: String): String {
        return try {
            // Handle different JSON formats (with or without spaces)
            // Format 1: {"text": "hello"}  or  {"text" : "hello"}
            val regex = """"text"\s*:\s*"([^"]*)"""".toRegex()
            val match = regex.find(jsonResult)
            match?.groupValues?.get(1)?.trim() ?: ""
        } catch (e: Exception) {
            Log.e("SpeechToTextService", "Failed to parse result: $jsonResult", e)
            ""
        }
    }
    
    /**
     * Check if the model is available (either loaded or downloaded).
     */
    fun isModelAvailable(): Boolean {
        return model != null || modelDownloader.isModelAvailable()
    }
    
    /**
     * Get instructions for downloading the model (fallback if auto-download fails).
     */
    fun getModelDownloadInstructions(): String {
        return """
            Automatic model download failed.
            
            You can manually download the Vosk speech model:
            
            1. Download from: https://alphacephei.com/vosk/models
               Recommended: vosk-model-small-en-us-0.15 (~40MB)
            
            2. Extract the zip file
            
            3. Move the folder to: ${modelDownloader.getModelPath()}
            
            4. Restart the app
            
            Or try the automatic download again.
        """.trimIndent()
    }
    
    /**
     * Reset the transcription state.
     */
    fun resetState() {
        _transcriptionState.value = TranscriptionState.Idle
    }
    
    /**
     * Release resources.
     */
    fun destroy() {
        model?.close()
        model = null
    }
}
