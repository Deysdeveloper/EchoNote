package com.Deysdeveloper.dailyvoicejournalapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.Deysdeveloper.dailyvoicejournalapp.notifications.NotificationHelper
import com.Deysdeveloper.dailyvoicejournalapp.notifications.ReminderScheduler
import com.Deysdeveloper.dailyvoicejournalapp.ui.MainViewModel
import com.Deysdeveloper.dailyvoicejournalapp.ui.screens.LockScreen
import com.Deysdeveloper.dailyvoicejournalapp.ui.screens.MainScreen
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.DailyVoiceJournalAppTheme
import com.Deysdeveloper.dailyvoicejournalapp.utils.BiometricHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var hasRecordPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var isUnlocked by mutableStateOf(false)
    private lateinit var biometricHelper: BiometricHelper
    
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
        
        // Initialize biometric helper
        biometricHelper = BiometricHelper(this)
        
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
                val userPreferences by viewModel.userPreferences.collectAsState()
                
                // Check if lock is enabled and app is not unlocked
                val shouldShowLockScreen = userPreferences.lockEnabled && !isUnlocked
                
                if (shouldShowLockScreen) {
                    LockScreen(
                        onUnlockRequest = {
                            if (biometricHelper.isBiometricAvailable()) {
                                biometricHelper.authenticate(
                                    title = "Unlock Daily Voice Journal",
                                    subtitle = "Authenticate to access your private journal",
                                    onSuccess = {
                                        isUnlocked = true
                                    },
                                    onError = { error ->
                                        // If user cancels or fails authentication, we stay on lock screen
                                        // They can try again by tapping the unlock button
                                    }
                                )
                            } else {
                                // If biometric is not available, unlock automatically
                                isUnlocked = true
                            }
                        }
                    )
                } else {
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
    
    override fun onResume() {
        super.onResume()
        // Lock the app again when returning to it
        lifecycleScope.launch {
            val prefs = viewModel.userPreferences.first()
            if (prefs.lockEnabled) {
                isUnlocked = false
            }
        }
    }
}