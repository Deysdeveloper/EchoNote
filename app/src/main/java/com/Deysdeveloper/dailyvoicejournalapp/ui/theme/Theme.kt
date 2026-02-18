package com.Deysdeveloper.dailyvoicejournalapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.Deysdeveloper.dailyvoicejournalapp.data.ThemeMode

// Light theme - Original Material 3 purple theme
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple80.copy(alpha = 0.3f),
    onPrimaryContainer = Purple40,
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    secondaryContainer = PurpleGrey80.copy(alpha = 0.3f),
    onSecondaryContainer = PurpleGrey40,
    tertiary = Pink40,
    onTertiary = Color.White,
    tertiaryContainer = Pink80.copy(alpha = 0.3f),
    onTertiaryContainer = Pink40,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

// Dark theme with warm teal aesthetic
private val DarkColorScheme = darkColorScheme(
    primary = WarmTealLight,
    onPrimary = Charcoal,
    primaryContainer = DeepTeal,
    onPrimaryContainer = WarmTealLight,
    secondary = WarmTeal,
    onSecondary = Color.White,
    secondaryContainer = DeepTeal.copy(alpha = 0.7f),
    onSecondaryContainer = WarmTealLight,
    tertiary = AccentGold,
    onTertiary = Charcoal,
    tertiaryContainer = AccentGold.copy(alpha = 0.15f),
    onTertiaryContainer = SoftGold,
    background = DarkSlate,
    onBackground = Cream,
    surface = Color(0xFF252F3D),
    onSurface = OffWhite,
    surfaceVariant = Color(0xFF2D3A4A),
    onSurfaceVariant = LightGray.copy(alpha = 0.7f),
    outline = LightGray.copy(alpha = 0.2f),
    error = SoftCoral,
    onError = Color.White,
    errorContainer = SoftCoral.copy(alpha = 0.2f),
    onErrorContainer = SoftCoral
)

@Composable
fun DailyVoiceJournalAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to preserve our custom theme
    content: @Composable () -> Unit
) {
    // Determine dark theme based on mode
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}