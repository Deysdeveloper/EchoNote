package com.Deysdeveloper.dailyvoicejournalapp.data

data class UserPreferences(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastRecordingDate: String = "", // Format: yyyy-MM-dd
    val notificationEnabled: Boolean = true,
    val notificationHour: Int = 21, // 9 PM default
    val notificationMinute: Int = 0,
    val lockEnabled: Boolean = false
)
