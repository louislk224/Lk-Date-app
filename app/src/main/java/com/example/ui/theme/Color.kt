package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Immersive UI Palette Colors
val LavenderPrimary = Color(0xFFD0BCFF)      // #D0BCFF
val DeepPurpleSecondary = Color(0xFF381E72)  // #381E72
val AccentPurpleTertiary = Color(0xFF49454F) // #49454F

val ImmersiveBackground = Color(0xFF1C1B1F)  // #1C1B1F
val ImmersiveSurface = Color(0xFF2B2930)     // #2B2930
val ImmersiveSurfaceVariant = Color(0xFF49454F) // #49454F

val ImmersiveOnText = Color(0xFFE6E1E5)      // #E6E1E5
val ImmersiveOnSubText = Color(0xFFCAC4D0)   // #CAC4D0

// Map existing variables for clean backwards compatibility while completely skinning the layout
val CrimsonPrimary = LavenderPrimary
val RoseSecondary = DeepPurpleSecondary
val SoftPinkTertiary = AccentPurpleTertiary

val SlateBackground = ImmersiveBackground
val SlateSurface = ImmersiveSurface
val SlateSurfaceVariant = ImmersiveSurfaceVariant

val OnSlateText = ImmersiveOnText
val OnSlateSubText = ImmersiveOnSubText

// Light theme counterparts mapping to premium dark palette to ensure consistent dark immersion
val LightPrimary = LavenderPrimary
val LightSecondary = DeepPurpleSecondary
val LightTertiary = AccentPurpleTertiary

