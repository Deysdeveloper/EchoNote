package com.Deysdeveloper.dailyvoicejournalapp.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime: Long = 0
    
    fun start(fileName: String): File {
        android.util.Log.d("AudioRecorder", "start() called with fileName: $fileName")
        
        val directory = File(context.filesDir, "voice_notes")
        if (!directory.exists()) {
            android.util.Log.d("AudioRecorder", "Creating directory: ${directory.absolutePath}")
            directory.mkdirs()
        }
        
        val file = File(directory, fileName)
        currentFile = file
        startTime = System.currentTimeMillis()
        
        android.util.Log.d("AudioRecorder", "Creating MediaRecorder...")
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            android.util.Log.d("AudioRecorder", "Setting audio source...")
            setAudioSource(MediaRecorder.AudioSource.MIC)
            android.util.Log.d("AudioRecorder", "Setting output format...")
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            android.util.Log.d("AudioRecorder", "Setting audio encoder...")
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            android.util.Log.d("AudioRecorder", "Setting output file: ${file.absolutePath}")
            setOutputFile(file.absolutePath)
            android.util.Log.d("AudioRecorder", "Preparing...")
            prepare()
            android.util.Log.d("AudioRecorder", "Starting recording...")
            start()
        }
        
        android.util.Log.d("AudioRecorder", "Recording started successfully!")
        return file
    }
    
    fun stop(): Long {
        val duration = System.currentTimeMillis() - startTime
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        currentFile = null
        return duration
    }
    
    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
    
    fun isRecording(): Boolean = mediaRecorder != null
    
    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: IllegalStateException) {
            0
        }
    }
}
