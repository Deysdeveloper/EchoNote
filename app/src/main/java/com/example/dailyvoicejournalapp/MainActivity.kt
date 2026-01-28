package com.example.dailyvoicejournalapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.dailyvoicejournalapp.notifications.NotificationHelper
import com.example.dailyvoicejournalapp.notifications.ReminderScheduler
import com.example.dailyvoicejournalapp.ui.MainViewModel
import com.example.dailyvoicejournalapp.ui.screens.MainScreen
import com.example.dailyvoicejournalapp.ui.theme.DailyVoiceJournalAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var hasRecordPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    
    private val requestRecordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
    }
    
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check initial permissions
        hasRecordPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            // Request notification permission if not granted
            if (!hasNotificationPermission) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            hasNotificationPermission = true
        }
        
        // Initialize notification channel
        NotificationHelper(this)
        
        // Schedule daily reminder if enabled
        lifecycleScope.launch {
            val prefs = viewModel.userPreferences.first()
            if (prefs.notificationEnabled) {
                ReminderScheduler.scheduleDailyReminder(
                    this@MainActivity,
                    prefs.notificationHour,
                    prefs.notificationMinute
                )
            }
        }
        
        enableEdgeToEdge()
        setContent {
            DailyVoiceJournalAppTheme {
                MainScreen(
                    viewModel = viewModel,
                    hasRecordPermission = hasRecordPermission,
                    onRequestPermission = {
                        requestRecordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }
}