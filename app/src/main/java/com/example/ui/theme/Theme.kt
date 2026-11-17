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
    primary = RoyalBlue,
    onPrimary = PureWhite,
    primaryContainer = RoyalBlueSoft,
    onPrimaryContainer = DeepNightBlue,
    secondary = Turquoise,
    onSecondary = PureWhite,
    secondaryContainer = TurquoiseSoft,
    onSecondaryContainer = DeepNightBlue,
    tertiary = SubtleGold,
    onTertiary = DeepNightBlue,
    tertiaryContainer = SubtleGoldSoft,
    onTertiaryContainer = DeepNightBlue,
    background = OffWhite,
    onBackground = SlateDarkText,
    surface = PureWhite,
    onSurface = SlateDarkText,
    surfaceVariant = PearlGray,
    onSurfaceVariant = SlateMuted,
    outline = Color(0xFFCBD5E1),
    outlineVariant = PearlGray,
    error = CrimsonError,
    onError = PureWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlueBright,
    onPrimary = PureWhite,
    primaryContainer = RoyalBlue,
    onPrimaryContainer = PureWhite,
    secondary = TurquoiseLight,
    onSecondary = DeepNightBlue,
    secondaryContainer = Turquoise,
    onSecondaryContainer = PureWhite,
    tertiary = SubtleGoldDark,
    onTertiary = DeepNightBlue,
    tertiaryContainer = Color(0xFF422006),
    onTertiaryContainer = SubtleGoldSoft,
    background = DeepNightBlue,
    onBackground = OffWhite,
    surface = DeepNightSurface,
    onSurface = OffWhite,
    surfaceVariant = DeepNightCard,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = DeepNightBlue
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AdrielSchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    customPrimaryColor: Color? = null,
    customSecondaryColor: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (customPrimaryColor != null || customSecondaryColor != null) {
        baseScheme.copy(
            primary = customPrimaryColor ?: baseScheme.primary,
            secondary = customSecondaryColor ?: baseScheme.secondary
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
