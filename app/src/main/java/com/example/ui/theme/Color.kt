package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Emerald Theme Colors
val Emerald950 = Color(0xFF022C22)
val Emerald900 = Color(0xFF064E3B)
val Emerald850 = Color(0xFF065440)
val Emerald800 = Color(0xFF065F46)
val Emerald700 = Color(0xFF047857)
val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald300 = Color(0xFF6EE7B7)
val Emerald200 = Color(0xFFA7F3D0)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald50 = Color(0xFFECFDF5)

// Teal & Mint Accents
val Teal700 = Color(0xFF0F766E)
val Teal600 = Color(0xFF0D9488)
val Teal500 = Color(0xFF14B8A6)
val Teal100 = Color(0xFFCCFBF1)

// Gold / Warm accents
val Gold600 = Color(0xFFD97706)
val Gold500 = Color(0xFFF59E0B)
val Gold400 = Color(0xFFFBBF24)
val Gold100 = Color(0xFFFEF3C7)
val Amber700 = Color(0xFFB45309)
val Amber600 = Gold600
val Amber500 = Gold500
val Amber100 = Gold100

// Modern Fintech Pastels & Accents (Mobbin & Dribbble inspired)
val FintechIndigo = Color(0xFF6366F1)
val FintechIndigoLight = Color(0xFFEEF2FF)
val FintechRose = Color(0xFFF43F5E)
val FintechRoseLight = Color(0xFFFFF1F2)
val FintechCyan = Color(0xFF06B6D4)
val FintechCyanLight = Color(0xFFECFEFF)
val FintechPurple = Color(0xFF8B5CF6)
val FintechPurpleLight = Color(0xFFF5F3FF)
val FintechLime = Color(0xFF84CC16)
val FintechLimeLight = Color(0xFFF7FEE7)

// Slate & Backgrounds
val Slate950 = Color(0xFF020617)
val Slate900 = Color(0xFF0F172A)
val Slate850 = Color(0xFF172033)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

// Modern Surface & Border Tints
val SurfaceBorderLight = Color(0xFFE2E8F0)
val SurfaceBorderDark = Color(0xFF1E3A2F)
val CardBackgroundLight = Color(0xFFFFFFFF)
val CardBackgroundDark = Color(0xFF0F241D)

// Status colors
val CrimsonError = Color(0xFFEF4444)
val CrimsonContainer = Color(0xFFFEE2E2)
val WarningOrange = Color(0xFFF97316)
val WarningContainer = Color(0xFFFFEDD5)
val SuccessGreen = Color(0xFF10B981)

// Neo-Fintech Luxury Gradients & FinTech Palettes (Inspired by modern digital banking)
val FintechNavyHero = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0F172A),
        Color(0xFF1E293B),
        Color(0xFF1E1B4B)
    )
)

val FintechCardMint = Brush.linearGradient(
    colors = listOf(
        Color(0xFFD1FAE5),
        Color(0xFFA7F3D0),
        Color(0xFF6EE7B7)
    )
)

val FintechCardIndigo = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4338CA),
        Color(0xFF6366F1),
        Color(0xFF818CF8)
    )
)

val FintechCardSky = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0284C7),
        Color(0xFF38BDF8)
    )
)

val HeroEmeraldGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF022C22), // Deep Forest
        Color(0xFF064E3B), // Emerald 900
        Color(0xFF065F46), // Emerald 800
        Color(0xFF047857)  // Emerald 700
    )
)

val GlowPillGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF10B981),
        Color(0xFF059669)
    )
)

val PrimaryEmeraldGradient = Brush.linearGradient(
    colors = listOf(
        Emerald600,
        Emerald700
    )
)

val SoftMintGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFECFDF5),
        Color(0xFFD1FAE5)
    )
)

val GlassBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.08f)
    )
)

val SubtleCardBorderLight = Color(0xFFE2E8F0).copy(alpha = 0.8f)
val SubtleCardBorderDark = Color(0xFF1E3A2F).copy(alpha = 0.8f)

