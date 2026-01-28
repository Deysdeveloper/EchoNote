package com.example.dailyvoicejournalapp.repository

import android.content.Context
import com.example.dailyvoicejournalapp.data.AppDatabase
import com.example.dailyvoicejournalapp.data.VoiceNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class VoiceNoteRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val voiceNoteDao = database.voiceNoteDao()
    
    fun getAllVoiceNotes(): Flow<List<VoiceNote>> {
        return voiceNoteDao.getAllVoiceNotes()
    }
    
    suspend fun insertVoiceNote(voiceNote: VoiceNote): Long {
        return voiceNoteDao.insert(voiceNote)
    }
    
    suspend fun deleteVoiceNote(id: Long) {
        val voiceNote = voiceNoteDao.getById(id)
        voiceNote?.let {
            // Delete the audio file
            val file = File(it.filePath)
            if (file.exists()) {
                file.delete()
            }
            // Delete the database entry
            voiceNoteDao.deleteById(id)
        }
    }
    
    suspend fun getVoiceNote(id: Long): VoiceNote? {
        return voiceNoteDao.getById(id)
    }
    
    fun getTotalRecordingsCount(): Flow<Int> {
        return voiceNoteDao.getTotalCount()
    }
    
    fun getTotalDuration(): Flow<Long> {
        return voiceNoteDao.getTotalDuration().map { it ?: 0L }
    }
}
