package com.Deysdeveloper.dailyvoicejournalapp.data

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
    
    @Query("UPDATE voice_notes SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String?)
    
    @Query("SELECT * FROM voice_notes WHERE title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchByTitle(query: String): Flow<List<VoiceNote>>
    
    @Query("SELECT COUNT(*) FROM voice_notes")
    fun getTotalCount(): Flow<Int>
    
    @Query("SELECT SUM(duration) FROM voice_notes")
    fun getTotalDuration(): Flow<Long?>
    
    @Query("UPDATE voice_notes SET transcript = :transcript WHERE id = :id")
    suspend fun updateTranscript(id: Long, transcript: String?)

    @Query("UPDATE voice_notes SET waveformData = :waveformData WHERE id = :id")
    suspend fun updateWaveformData(id: Long, waveformData: String?)

    @Query("SELECT * FROM voice_notes WHERE transcript IS NULL AND duration > 5000 ORDER BY timestamp DESC")
    fun getVoiceNotesWithoutTranscript(): Flow<List<VoiceNote>>
}
