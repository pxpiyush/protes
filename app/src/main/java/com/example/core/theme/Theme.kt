package com.example.core.theme

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Custom Spacing System
data class ProtesSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val doubleExtraLarge: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { ProtesSpacing() }

// Extension to access spacing via MaterialTheme.spacing
val MaterialTheme.spacing: ProtesSpacing
    @Composable
    get() = LocalSpacing.current

private val LightColorScheme = lightColorScheme(
    primary = EditorialInk,
    onPrimary = EditorialPaper,
    primaryContainer = EditorialBeige,
    onPrimaryContainer = EditorialInk,
    secondary = EditorialRust,
    onSecondary = EditorialWhite,
    background = EditorialPaper,
    onBackground = EditorialInk,
    surface = EditorialSurface,
    onSurface = EditorialInk,
    surfaceVariant = EditorialBeige,
    onSurfaceVariant = EditorialInkLight,
    outline = EditorialInkLight.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = EditorialSand,
    onPrimary = EditorialCharcoal,
    primaryContainer = EditorialCharcoalVariant,
    onPrimaryContainer = EditorialSand,
    secondary = EditorialRustLight,
    onSecondary = EditorialWhite,
    background = EditorialCharcoal,
    onBackground = EditorialSand,
    surface = EditorialCharcoalSurface,
    onSurface = EditorialSand,
    surfaceVariant = EditorialCharcoalVariant,
    onSurfaceVariant = EditorialSandDark,
    outline = EditorialSandDark.copy(alpha = 0.5f)
)

@Composable
fun ProtesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val rawColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // Smooth, premium color interpolation during theme transitions
    val animatedColorScheme = androidx.compose.material3.ColorScheme(
        primary = androidx.compose.animation.animateColorAsState(rawColorScheme.primary, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "primary").value,
        onPrimary = androidx.compose.animation.animateColorAsState(rawColorScheme.onPrimary, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "onPrimary").value,
        primaryContainer = androidx.compose.animation.animateColorAsState(rawColorScheme.primaryContainer, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "primaryContainer").value,
        onPrimaryContainer = androidx.compose.animation.animateColorAsState(rawColorScheme.onPrimaryContainer, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "onPrimaryContainer").value,
        inversePrimary = rawColorScheme.inversePrimary,
        secondary = androidx.compose.animation.animateColorAsState(rawColorScheme.secondary, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "secondary").value,
        onSecondary = androidx.compose.animation.animateColorAsState(rawColorScheme.onSecondary, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "onSecondary").value,
        secondaryContainer = rawColorScheme.secondaryContainer,
        onSecondaryContainer = rawColorScheme.onSecondaryContainer,
        tertiary = rawColorScheme.tertiary,
        onTertiary = rawColorScheme.onTertiary,
        tertiaryContainer = rawColorScheme.tertiaryContainer,
        onTertiaryContainer = rawColorScheme.onTertiaryContainer,
        background = androidx.compose.animation.animateColorAsState(rawColorScheme.background, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "background").value,
        onBackground = androidx.compose.animation.animateColorAsState(rawColorScheme.onBackground, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "onBackground").value,
        surface = androidx.compose.animation.animateColorAsState(rawColorScheme.surface, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "surface").value,
        onSurface = androidx.compose.animation.animateColorAsState(rawColorScheme.onSurface, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "onSurface").value,
        surfaceVariant = androidx.compose.animation.animateColorAsState(rawColorScheme.surfaceVariant, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "surfaceVariant").value,
        onSurfaceVariant = androidx.compose.animation.animateColorAsState(rawColorScheme.onSurfaceVariant, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "onSurfaceVariant").value,
        surfaceTint = rawColorScheme.surfaceTint,
        inverseOnSurface = rawColorScheme.inverseOnSurface,
        inverseSurface = rawColorScheme.inverseSurface,
        outline = androidx.compose.animation.animateColorAsState(rawColorScheme.outline, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "outline").value,
        outlineVariant = rawColorScheme.outlineVariant,
        scrim = rawColorScheme.scrim,
        error = rawColorScheme.error,
        onError = rawColorScheme.onError,
        errorContainer = rawColorScheme.errorContainer,
        onErrorContainer = rawColorScheme.onErrorContainer
    )

    CompositionLocalProvider(LocalSpacing provides ProtesSpacing()) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = ProtesTypography,
            content = content
        )
    }
}

