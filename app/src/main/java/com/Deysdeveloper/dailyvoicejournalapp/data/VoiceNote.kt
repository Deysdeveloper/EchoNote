package com.Deysdeveloper.dailyvoicejournalapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val timestamp: Long,
    val duration: Long, // Duration in milliseconds
    val title: String? = null // Custom title for the note
)
