package com.quickgit.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ubuntu / Yaru-inspired palette for the Linux .deb desktop client.
 * Semantic git status colors stay available for diffs and CI badges.
 */
val UbuntuOrange = Color(0xFFE95420)
val UbuntuAubergine = Color(0xFF77216F)
val UbuntuWarmGrey = Color(0xFFAEA79F)

// Kept for git status / CI (not the chrome primary)
val GitBlue = Color(0xFF0E8420) // reuse slot for positive actions where blue was used
val GitGreen = Color(0xFF0E8420) // Yaru-ish success green
val GitRed = Color(0xFFC7162B)
val GitAmber = Color(0xFFE95420)

private val LightColors = lightColorScheme(
    primary = UbuntuOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = UbuntuAubergine,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5D9F0),
    onSecondaryContainer = Color(0xFF2D0A2A),
    tertiary = Color(0xFF0E8420),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB6F2C0),
    onTertiaryContainer = Color(0xFF00210A),
    error = GitRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF2C2C2C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainerHighest = Color(0xFFEBEBEB),
    outline = UbuntuWarmGrey,
    outlineVariant = Color(0xFFD9D9D9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7B5A),
    onPrimary = Color(0xFF3B0A00),
    primaryContainer = Color(0xFFB33A14),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFE0A8D8),
    onSecondary = Color(0xFF3B0A36),
    secondaryContainer = Color(0xFF5A184F),
    onSecondaryContainer = Color(0xFFF5D9F0),
    tertiary = Color(0xFF5BDD75),
    onTertiary = Color(0xFF003912),
    tertiaryContainer = Color(0xFF0B6B1C),
    onTertiaryContainer = Color(0xFFB6F2C0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // Yaru dark chrome
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF3D3D3D),
    onSurfaceVariant = Color(0xFFC8C8C8),
    surfaceContainerLow = Color(0xFF252525),
    surfaceContainerHighest = Color(0xFF3D3D3D),
    outline = Color(0xFF8C8C8C),
    outlineVariant = Color(0xFF4A4A4A)
)

/** Modest radii in the spirit of Yaru / libadwaita desktop widgets. */
private val UbuntuShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

private val UbuntuTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 48.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
fun QuickGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = UbuntuTypography,
        shapes = UbuntuShapes,
        content = content
    )
}
