package com.example.dailyvoicejournalapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNote>>
    
    @Insert
    suspend fun insert(voiceNote: VoiceNote): Long
    
    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getById(id: Long): VoiceNote?
    
    @Query("SELECT COUNT(*) FROM voice_notes")
    fun getTotalCount(): Flow<Int>
    
    @Query("SELECT SUM(duration) FROM voice_notes")
    fun getTotalDuration(): Flow<Long?>
}
