package de.syss.MifareClassicTool.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// KernelSU Style UI 2.0 Color Palette
// Clean tech blue/cyan accents, deep charcoal/slate dark theme surfaces,
// crisp light theme contrast, and razor-sharp border outlines.
// =========================================================================

// Light Theme Palette (Cool off-white + pure white cards + crisp outline)
val MctPrimaryLight = Color(0xFF0284C7)           // Tech Blue 600
val MctOnPrimaryLight = Color(0xFFFFFFFF)
val MctPrimaryContainerLight = Color(0xFFE0F2FE)  // Sky 100
val MctOnPrimaryContainerLight = Color(0xFF0369A1) // Sky 700

val MctSecondaryLight = Color(0xFF059669)         // Emerald 600
val MctOnSecondaryLight = Color(0xFFFFFFFF)
val MctSecondaryContainerLight = Color(0xFFD1FAE5)// Emerald 100
val MctOnSecondaryContainerLight = Color(0xFF047857)

val MctBackgroundLight = Color(0xFFF6F8FA)        // Clean Cool Off-White
val MctOnBackgroundLight = Color(0xFF1F2328)
val MctSurfaceLight = Color(0xFFFFFFFF)           // Pure White Card Container
val MctOnSurfaceLight = Color(0xFF1F2328)
val MctSurfaceVariantLight = Color(0xFFF0F3F6)    // Elevated Surface Container High
val MctOnSurfaceVariantLight = Color(0xFF656D76)
val MctOutlineLight = Color(0xFFD0D7DE)           // Subtle Soft Border (GitHub/KernelSU style)

val MctErrorLight = Color(0xFFDC2626)             // Red 600
val MctOnErrorLight = Color(0xFFFFFFFF)
val MctErrorContainerLight = Color(0xFFFEE2E2)
val MctOnErrorContainerLight = Color(0xFF991B1B)

// Dark Theme Palette (Deep Slate / Charcoal + Elevated Card + 1dp Outline)
val MctPrimaryDark = Color(0xFF38BDF8)            // Tech Cyan / Sky 400
val MctOnPrimaryDark = Color(0xFF082F49)          // Sky 950
val MctPrimaryContainerDark = Color(0xFF0369A1)   // Sky 700
val MctOnPrimaryContainerDark = Color(0xFFE0F2FE)

val MctSecondaryDark = Color(0xFF34D399)          // Mint / Emerald 400
val MctOnSecondaryDark = Color(0xFF064E3B)
val MctSecondaryContainerDark = Color(0xFF065F46)
val MctOnSecondaryContainerDark = Color(0xFFA7F3D0)

val MctBackgroundDark = Color(0xFF0D1117)         // Deep Charcoal / Slate Background
val MctOnBackgroundDark = Color(0xFFF0F6FC)
val MctSurfaceDark = Color(0xFF161B22)            // Elevated Card Surface
val MctOnSurfaceDark = Color(0xFFF0F6FC)
val MctSurfaceVariantDark = Color(0xFF21262D)     // Surface Container High / Dialogs
val MctOnSurfaceVariantDark = Color(0xFF8B949E)   // Muted Subtitle / Parameter Text
val MctOutlineDark = Color(0xFF30363D)            // Razor-Sharp Border Outline

val MctErrorDark = Color(0xFFF87171)              // Red 400
val MctOnErrorDark = Color(0xFF450A0A)
val MctErrorContainerDark = Color(0xFF991B1B)
val MctOnErrorContainerDark = Color(0xFFFEE2E2)

// MCT Specific RFID Highlights (strictly preserved for block data syntax highlighting)
val MctLightGreen = Color(0xFF66FF66)
val MctDarkGreen = Color(0xFF338800)
val MctOrange = Color(0xFFFF6600)
val MctPurple = Color(0xFF9933FF)
val MctYellow = Color(0xFFCCCC00)
val MctRed = Color(0xFFFF0000)
