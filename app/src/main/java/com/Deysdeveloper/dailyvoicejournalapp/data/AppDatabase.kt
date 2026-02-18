package com.Deysdeveloper.dailyvoicejournalapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [VoiceNote::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceNoteDao(): VoiceNoteDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add title column to voice_notes table
                db.execSQL("ALTER TABLE voice_notes ADD COLUMN title TEXT")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add transcript column to voice_notes table
                db.execSQL("ALTER TABLE voice_notes ADD COLUMN transcript TEXT")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add waveformData column to voice_notes table for playback visualization
                db.execSQL("ALTER TABLE voice_notes ADD COLUMN waveformData TEXT")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voice_journal_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
