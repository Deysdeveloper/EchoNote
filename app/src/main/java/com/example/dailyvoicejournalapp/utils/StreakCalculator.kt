package com.example.dailyvoicejournalapp.utils

import java.text.SimpleDateFormat
import java.util.*

object StreakCalculator {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Get today's date as a string in yyyy-MM-dd format
     */
    fun getTodayDate(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * Get yesterday's date as a string in yyyy-MM-dd format
     */
    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return dateFormat.format(calendar.time)
    }
    
    /**
     * Calculate days between two date strings
     */
    private fun daysBetween(date1: String, date2: String): Int {
        return try {
            val d1 = dateFormat.parse(date1)
            val d2 = dateFormat.parse(date2)
            val diff = (d2?.time ?: 0) - (d1?.time ?: 0)
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Calculate updated streak based on last recording date
     * Returns Pair(newCurrentStreak, newLongestStreak)
     */
    fun calculateStreak(
        lastRecordingDate: String,
        currentStreak: Int,
        longestStreak: Int
    ): Pair<Int, Int> {
        val today = getTodayDate()
        val yesterday = getYesterdayDate()
        
        return when {
            // Already recorded today - keep streak
            lastRecordingDate == today -> {
                Pair(currentStreak, longestStreak)
            }
            // Recorded yesterday - increment streak
            lastRecordingDate == yesterday -> {
                val newStreak = currentStreak + 1
                val newLongest = maxOf(newStreak, longestStreak)
                Pair(newStreak, newLongest)
            }
            // First recording or streak broken
            lastRecordingDate.isEmpty() || daysBetween(lastRecordingDate, today) > 1 -> {
                val newLongest = maxOf(1, longestStreak)
                Pair(1, newLongest)
            }
            // Should not happen, but default to keep current
            else -> {
                Pair(currentStreak, longestStreak)
            }
        }
    }
    
    /**
     * Check if recording today should update the streak
     */
    fun shouldUpdateStreak(lastRecordingDate: String): Boolean {
        return lastRecordingDate != getTodayDate()
    }
}
