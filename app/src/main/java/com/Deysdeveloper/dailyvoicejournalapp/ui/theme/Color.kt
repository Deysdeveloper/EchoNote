package com.Deysdeveloper.dailyvoicejournalapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary palette - Warm, calming, journal-like
val WarmTeal = Color(0xFF2E7D6F)
val WarmTealLight = Color(0xFF4A9B8C)
val WarmTealDark = Color(0xFF1B5E56)

// Accent colors
val SoftCoral = Color(0xFFFF6B6B)
val CoralDark = Color(0xFFE85555)
val AccentGold = Color(0xFFFFB347)
val SoftGold = Color(0xFFFFD89B)

// Neutral colors
val Cream = Color(0xFFF5F5F0)
val OffWhite = Color(0xFFFAFAF8)
val Charcoal = Color(0xFF2C3E50)
val SlateGray = Color(0xFF64748B)
val LightGray = Color(0xFFE2E8F0)

// Dark theme colors
val DeepTeal = Color(0xFF1A3D36)
val DarkSlate = Color(0xFF1E293B)
val SoftCreamDark = Color(0xFF2D2D2A)

// Glassmorphism colors
val GlassLight = Color(0x80FFFFFF)
val GlassDark = Color(0x801E293B)
val GlassBorderLight = Color(0x40FFFFFF)
val GlassBorderDark = Color(0x40FFFFFF)

// Gradients
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(WarmTeal, WarmTealLight)
)

val RecordingGradient = Brush.radialGradient(
    colors = listOf(SoftCoral, CoralDark)
)

val GoldGradient = Brush.linearGradient(
    colors = listOf(AccentGold, SoftGold)
)

val SunriseGradient = Brush.linearGradient(
    colors = listOf(SoftCoral, AccentGold, SoftGold)
)

val OceanGradient = Brush.linearGradient(
    colors = listOf(WarmTealDark, WarmTeal, WarmTealLight)
)

val CardGradientLight = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF0FDFA),
        Color(0xFFE0F2FE)
    )
)

val CardGradientDark = Brush.linearGradient(
    colors = listOf(
        Color(0xFF134E4A),
        Color(0xFF0F766E)
    )
)

// Legacy colors for backward compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
