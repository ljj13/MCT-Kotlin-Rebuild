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
fun MctTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
