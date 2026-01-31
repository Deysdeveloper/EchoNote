package com.Deysdeveloper.dailyvoicejournalapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val CURRENT_STREAK = intPreferencesKey("current_streak")
        private val LONGEST_STREAK = intPreferencesKey("longest_streak")
        private val LAST_RECORDING_DATE = stringPreferencesKey("last_recording_date")
        private val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        private val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
    }
    
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                currentStreak = preferences[CURRENT_STREAK] ?: 0,
                longestStreak = preferences[LONGEST_STREAK] ?: 0,
                lastRecordingDate = preferences[LAST_RECORDING_DATE] ?: "",
                notificationEnabled = preferences[NOTIFICATION_ENABLED] ?: true,
                notificationHour = preferences[NOTIFICATION_HOUR] ?: 21,
                notificationMinute = preferences[NOTIFICATION_MINUTE] ?: 0,
                lockEnabled = preferences[LOCK_ENABLED] ?: false
            )
        }
    
    suspend fun updateStreak(currentStreak: Int, longestStreak: Int, lastRecordingDate: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_STREAK] = currentStreak
            preferences[LONGEST_STREAK] = longestStreak
            preferences[LAST_RECORDING_DATE] = lastRecordingDate
        }
    }
    
    suspend fun updateNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED] = enabled
            preferences[NOTIFICATION_HOUR] = hour
            preferences[NOTIFICATION_MINUTE] = minute
        }
    }
    
    suspend fun updateLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCK_ENABLED] = enabled
        }
    }
}
