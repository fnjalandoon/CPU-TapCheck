package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = NavyContainer,
    onPrimaryContainer = OnNavyContainer,
    secondary = GoldWarm,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = AlreadyBlue,
    onTertiary = Color.White,
    background = IvoryPaper,
    onBackground = TextInk,
    surface = IvoryCard,
    onSurface = TextInk,
    surfaceVariant = IvoryMuted,
    onSurfaceVariant = TextMuted,
    outline = SlateOutline,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = GoldBright,
    onPrimary = NavyDarker,
    primaryContainer = MidnightElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondary = GoldWarm,
    onSecondary = NavyDarker,
    secondaryContainer = MidnightCard,
    onSecondaryContainer = GoldBright,
    tertiary = AlreadyContainer,
    onTertiary = OnAlreadyContainer,
    background = MidnightSurface,
    onBackground = DarkTextPrimary,
    surface = MidnightCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = MidnightElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = MidnightOutline,
    error = ErrorContainer,
    onError = OnErrorContainer,
    errorContainer = ErrorCrimson,
    onErrorContainer = Color.White
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CpuTapCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
