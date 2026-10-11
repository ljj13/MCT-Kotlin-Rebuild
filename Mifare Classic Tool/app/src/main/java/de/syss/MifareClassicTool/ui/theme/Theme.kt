package de.syss.MifareClassicTool.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import de.syss.MifareClassicTool.Activities.Preferences
import de.syss.MifareClassicTool.Common

private val DarkColorScheme = darkColorScheme(
    primary = MctPrimaryDark,
    onPrimary = MctOnPrimaryDark,
    primaryContainer = MctPrimaryContainerDark,
    onPrimaryContainer = MctOnPrimaryContainerDark,
    secondary = MctSecondaryDark,
    onSecondary = MctOnSecondaryDark,
    secondaryContainer = MctSecondaryContainerDark,
    onSecondaryContainer = MctOnSecondaryContainerDark,
    background = MctBackgroundDark,
    onBackground = MctOnBackgroundDark,
    surface = MctSurfaceDark,
    onSurface = MctOnSurfaceDark,
    surfaceVariant = MctSurfaceVariantDark,
    onSurfaceVariant = MctOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = MctPrimaryLight,
    onPrimary = MctOnPrimaryLight,
    primaryContainer = MctPrimaryContainerLight,
    onPrimaryContainer = MctOnPrimaryContainerLight,
    secondary = MctSecondaryLight,
    onSecondary = MctOnSecondaryLight,
    secondaryContainer = MctSecondaryContainerLight,
    onSecondaryContainer = MctOnSecondaryContainerLight,
    background = MctBackgroundLight,
    onBackground = MctOnBackgroundLight,
    surface = MctSurfaceLight,
    onSurface = MctOnSurfaceLight,
    surfaceVariant = MctSurfaceVariantLight,
    onSurfaceVariant = MctOnSurfaceVariantLight
)

@Composable
fun isMctDarkTheme(): Boolean {
    val systemDark = isSystemInDarkTheme()
    return try {
        val pref = Common.getPreferences()
        val defaultTheme = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) 0 else 2
        val themeID = pref.getInt(Preferences.Preference.CustomAppTheme.toString(), defaultTheme)
        when (themeID) {
            0 -> true
            1 -> false
            else -> systemDark
        }
    } catch (e: Exception) {
        systemDark
    }
}

@Composable
fun MctTheme(
    darkTheme: Boolean = isMctDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
