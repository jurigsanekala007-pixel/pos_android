package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PosPrimaryDark,
    onPrimary = PosOnPrimaryDark,
    primaryContainer = PosPrimaryContainerDark,
    onPrimaryContainer = PosOnPrimaryContainerDark,
    secondary = PosSecondaryDark,
    onSecondary = PosOnSecondaryDark,
    secondaryContainer = PosSecondaryContainerDark,
    onSecondaryContainer = PosOnSecondaryContainerDark,
    tertiary = PosTertiaryDark,
    onTertiary = PosOnTertiaryDark,
    tertiaryContainer = PosTertiaryContainerDark,
    onTertiaryContainer = PosOnTertiaryContainerDark,
    background = PosBackgroundDark,
    onBackground = PosOnBackgroundDark,
    surface = PosSurfaceDark,
    onSurface = PosOnSurfaceDark,
    surfaceVariant = PosSurfaceVariantDark,
    onSurfaceVariant = PosOnSurfaceVariantDark,
    outline = PosOutline,
    outlineVariant = PosOutlineVariant,
    error = PosError,
    errorContainer = PosErrorContainer,
    onErrorContainer = PosOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = PosPrimary,
    onPrimary = PosOnPrimary,
    primaryContainer = PosPrimaryContainer,
    onPrimaryContainer = PosOnPrimaryContainer,
    secondary = PosSecondary,
    onSecondary = PosOnSecondary,
    secondaryContainer = PosSecondaryContainer,
    onSecondaryContainer = PosOnSecondaryContainer,
    tertiary = PosTertiary,
    onTertiary = PosOnTertiary,
    tertiaryContainer = PosTertiaryContainer,
    onTertiaryContainer = PosOnTertiaryContainer,
    background = PosBackground,
    onBackground = PosOnBackground,
    surface = PosSurface,
    onSurface = PosOnSurface,
    surfaceVariant = PosSurfaceVariant,
    onSurfaceVariant = PosOnSurfaceVariant,
    outline = PosOutline,
    outlineVariant = PosOutlineVariant,
    error = PosError,
    errorContainer = PosErrorContainer,
    onErrorContainer = PosOnErrorContainer
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature theme for brand consistency
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
        shapes = AppShapes,
        content = content
    )
}

