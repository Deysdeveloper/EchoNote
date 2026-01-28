package com.example.dailyvoicejournalapp.audio

import android.media.MediaPlayer
import java.io.File

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var onCompletionListener: (() -> Unit)? = null
    
    fun play(file: File, onCompletion: () -> Unit) {
        stop()
        onCompletionListener = onCompletion
        
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                onCompletionListener?.invoke()
                release()
                mediaPlayer = null
            }
            prepare()
            start()
        }
    }
    
    fun pause() {
        mediaPlayer?.pause()
    }
    
    fun resume() {
        mediaPlayer?.start()
    }
    
    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
    
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    
    fun isPaused(): Boolean {
        return mediaPlayer != null && !isPlaying()
    }
    
    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: IllegalStateException) {
            0
        }
    }
    
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: IllegalStateException) {
            0
        }
    }
    
    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: IllegalStateException) {
            // Handle error silently
        }
    }
    
    fun release() {
        stop()
    }
}
