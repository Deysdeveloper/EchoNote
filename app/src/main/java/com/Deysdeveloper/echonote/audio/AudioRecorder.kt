package com.deysdeveloper.echonote.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime: Long = 0
    private var isInitialized = false
    private val isTablet: Boolean

    init {
        // Detect if device is a tablet
        isTablet = isTabletDevice()
        
        // Log device info for debugging tablet issues
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val apiLevel = Build.VERSION.SDK_INT
        Log.i("AudioRecorder", "Device: $manufacturer $model, Android API: $apiLevel, Is Tablet: $isTablet")
    }

    private fun isTabletDevice(): Boolean {
        return try {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            // Tablet detection based on screen size
            val widthDp = displayMetrics.widthPixels / displayMetrics.density
            val heightDp = displayMetrics.heightPixels / displayMetrics.density
            val screenSmallestWidthDp = widthDp.coerceAtMost(heightDp)
            
            // Consider device a tablet if smallest side is at least 600dp (7-inch tablet)
            screenSmallestWidthDp >= 600
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Could not detect tablet, assuming phone", e)
            false
        }
    }
    
    fun start(fileName: String): File {
        Log.d("AudioRecorder", "start() called with fileName: $fileName")
        
        // Check if device has audio recording capability
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            Log.e("AudioRecorder", "AudioManager not available")
            throw RuntimeException("Audio system not available on this device")
        }
        
        // Release any previous media recorder
        release()
        
        try {
            val directory = File(context.filesDir, "voice_notes")
            if (!directory.exists()) {
                Log.d("AudioRecorder", "Creating directory: ${directory.absolutePath}")
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            currentFile = file
            startTime = System.currentTimeMillis()
            
            Log.d("AudioRecorder", "Creating MediaRecorder...")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                try {
                    Log.d("AudioRecorder", "Setting audio source...")
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    Log.d("AudioRecorder", "Setting output format...")
                    
                    // Try MPEG_4 first, fallback to THREE_GPP for tablets
                    var useMpeg4 = true
                    try {
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        Log.d("AudioRecorder", "Using MPEG_4 format")
                    } catch (e: Exception) {
                        Log.w("AudioRecorder", "MPEG_4 not supported, trying THREE_GPP", e)
                        useMpeg4 = false
                        // Use THREE_GPP as fallback for tablet compatibility
                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                        Log.d("AudioRecorder", "Using THREE_GPP format (tablet compatible)")
                    }
                    
                    Log.d("AudioRecorder", "Setting audio encoder...")
                    // Use AAC for MPEG_4, AMR_NB for THREE_GPP
                    if (useMpeg4) {
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    } else {
                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    }
                    
                    // Use more conservative settings that are widely supported on tablets
                    setAudioEncodingBitRate(64000)  // Reduced for tablet compatibility
                    setAudioSamplingRate(16000)      // Reduced for tablet compatibility
                    Log.d("AudioRecorder", "Setting output file: ${file.absolutePath}")
                    setOutputFile(file.absolutePath)
                    Log.d("AudioRecorder", "Preparing...")
                    prepare()
                    Log.d("AudioRecorder", "Starting recording...")
                    start()
                    isInitialized = true
                    Log.d("AudioRecorder", "Recording started successfully!")
                } catch (e: IOException) {
                    Log.e("AudioRecorder", "IOException during recording setup", e)
                    release()
                    throw RuntimeException("Failed to setup audio recording: ${e.message}", e)
                } catch (e: IllegalStateException) {
                    Log.e("AudioRecorder", "IllegalStateException during recording setup", e)
                    release()
                    throw RuntimeException("MediaRecorder in invalid state: ${e.message}", e)
                } catch (e: RuntimeException) {
                    Log.e("AudioRecorder", "RuntimeException during recording setup", e)
                    release()
                    throw RuntimeException("Failed to start recording: ${e.message}", e)
                }
            }
            
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            currentFile = null
            isInitialized = false
            throw e
        }
    }
    
    fun stop(): Long {
        if (!isInitialized || mediaRecorder == null) {
            Log.w("AudioRecorder", "stop() called but recording not initialized")
            return 0L
        }
        
        val duration = System.currentTimeMillis() - startTime
        Log.d("AudioRecorder", "Stopping recording, duration: ${duration}ms")
        
        try {
            mediaRecorder?.apply {
                if (isInitialized) {
                    stop()
                    release()
                }
            }
        } catch (e: RuntimeException) {
            Log.e("AudioRecorder", "Error stopping MediaRecorder", e)
            // Still release to clean up resources
            mediaRecorder?.release()
        } finally {
            mediaRecorder = null
            currentFile = null
            isInitialized = false
        }
        
        return duration
    }
    
    fun release() {
        Log.d("AudioRecorder", "release() called")
        mediaRecorder?.apply {
            try {
                if (isInitialized) {
                    stop()
                }
            } catch (e: Exception) {
                Log.w("AudioRecorder", "Error during release", e)
            } finally {
                try {
                    release()
                } catch (e: Exception) {
                    Log.w("AudioRecorder", "Error releasing MediaRecorder", e)
                }
            }
        }
        mediaRecorder = null
        currentFile = null
        isInitialized = false
    }
    
    fun isRecording(): Boolean = isInitialized && mediaRecorder != null
    
    fun getMaxAmplitude(): Int {
        return try {
            if (isInitialized) {
                mediaRecorder?.maxAmplitude ?: 0
            } else {
                0
            }
        } catch (e: IllegalStateException) {
            Log.w("AudioRecorder", "Error getting max amplitude", e)
            0
        } catch (e: RuntimeException) {
            Log.w("AudioRecorder", "Error getting max amplitude", e)
            0
        }
    }
}
