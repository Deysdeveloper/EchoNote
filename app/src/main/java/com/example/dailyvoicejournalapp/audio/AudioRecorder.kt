package com.example.dailyvoicejournalapp.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime: Long = 0
    
    fun start(fileName: String): File {
        val directory = File(context.filesDir, "voice_notes")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        val file = File(directory, fileName)
        currentFile = file
        startTime = System.currentTimeMillis()
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        
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
