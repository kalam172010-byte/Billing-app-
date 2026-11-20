package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KalamLightColorScheme = lightColorScheme(
    primary = Slate900,
    onPrimary = PureWhite,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = Emerald600,
    onSecondary = PureWhite,
    secondaryContainer = Emerald50,
    onSecondaryContainer = Emerald700,
    tertiary = Blue600,
    onTertiary = PureWhite,
    tertiaryContainer = Blue50,
    onTertiaryContainer = Blue700,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate200,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Rose600,
    onError = PureWhite,
    errorContainer = Rose50,
    onErrorContainer = Rose700
)

val KalamShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun KalamBillingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KalamLightColorScheme,
        typography = KalamTypography,
        shapes = KalamShapes,
        content = content
    )
}
