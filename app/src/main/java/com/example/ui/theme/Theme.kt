package com.example.ui.theme

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
    primary = AcademicBluePrimaryDark,
    onPrimary = AcademicBlueOnPrimaryDark,
    primaryContainer = AcademicBlueContainerDark,
    onPrimaryContainer = AcademicBlueOnContainerDark,
    secondary = AcademicTealSecondaryDark,
    onSecondary = AcademicTealOnSecondaryDark,
    secondaryContainer = AcademicTealContainerDark,
    onSecondaryContainer = AcademicTealOnContainerDark,
    tertiary = AcademicAmberTertiaryDark,
    onTertiary = AcademicAmberOnTertiaryDark,
    tertiaryContainer = AcademicAmberContainerDark,
    onTertiaryContainer = AcademicAmberOnContainerDark,
    background = NavyBackgroundDark,
    onBackground = NavyOnBackgroundDark,
    surface = NavySurfaceDark,
    onSurface = NavyOnSurfaceDark,
    surfaceVariant = NavySurfaceVariantDark,
    onSurfaceVariant = NavyOnSurfaceVariantDark,
    outline = NavyOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = AcademicBluePrimaryLight,
    onPrimary = AcademicBlueOnPrimaryLight,
    primaryContainer = AcademicBlueContainerLight,
    onPrimaryContainer = AcademicBlueOnContainerLight,
    secondary = AcademicTealSecondaryLight,
    onSecondary = AcademicTealOnSecondaryLight,
    secondaryContainer = AcademicTealContainerLight,
    onSecondaryContainer = AcademicTealOnContainerLight,
    tertiary = AcademicAmberTertiaryLight,
    onTertiary = AcademicAmberOnTertiaryLight,
    tertiaryContainer = AcademicAmberContainerLight,
    onTertiaryContainer = AcademicAmberOnContainerLight,
    background = SlateBackgroundLight,
    onBackground = SlateOnBackgroundLight,
    surface = SlateSurfaceLight,
    onSurface = SlateOnSurfaceLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
    outline = SlateOutlineLight
)

@Composable
fun MonClasseurBacTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use bespoke academic palette for binder identity
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
        typography = Typography,
        content = content
    )
}
