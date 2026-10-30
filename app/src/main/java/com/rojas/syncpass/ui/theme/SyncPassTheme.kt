package com.rojas.syncpass.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = DeepPlum,
    onPrimary = OnBrand,
    primaryContainer = SoftLilac,
    onPrimaryContainer = Ink,
    secondary = Mauve,
    onSecondary = OnBrand,
    secondaryContainer = SoftPink,
    onSecondaryContainer = DeepPurple,
    tertiary = Pink,
    onTertiary = DeepPurple,
    tertiaryContainer = Color(0xFFFDD9EA),
    onTertiaryContainer = DeepPurple,
    background = AppBackground,
    onBackground = Ink,
    surface = AppSurface,
    onSurface = Ink,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = MutedInk,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF9FE),
    surfaceContainer = AppSurfaceVariant,
    surfaceContainerHigh = Color(0xFFF8F0FA),
    surfaceContainerHighest = Color(0xFFF3E8F6),
    outline = OutlineSoft,
    outlineVariant = Color(0xFFE8DCEB),
    error = ErrorRed,
    onError = OnBrand,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = DeepPurple,
    inverseOnSurface = OnBrand
)

private val DarkColors = darkColorScheme(
    primary = Pink,
    onPrimary = DeepPurple,
    primaryContainer = DeepPlum,
    onPrimaryContainer = SoftLilac,
    secondary = Lilac,
    onSecondary = DeepPurple,
    secondaryContainer = Color(0xFF4B2B57),
    onSecondaryContainer = Color(0xFFF8E7FC),
    tertiary = Mauve,
    onTertiary = DeepPurple,
    tertiaryContainer = Color(0xFF54263F),
    onTertiaryContainer = SoftPink,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD9C8E0),
    surfaceContainerLowest = Color(0xFF12081D),
    surfaceContainerLow = Color(0xFF1B0F29),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF2A1738),
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF4A3358),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val SyncPassShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SyncPassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SyncPassTypography,
        shapes = SyncPassShapes,
        content = content
    )
}
