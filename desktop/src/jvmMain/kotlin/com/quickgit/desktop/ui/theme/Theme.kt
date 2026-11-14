package com.quickgit.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Platform-native desktop theming:
 * - Linux (.deb): Ubuntu Yaru / GNOME density
 * - Windows (.msi): Fluent Design–inspired (WinUI accent, Mica-like surfaces)
 * - Other (macOS, etc.): Yaru-style defaults
 */

// ── Semantic aliases used across the app ───────────────────────────────────
val UbuntuOrange = Color(0xFFE95420)
val UbuntuAubergine = Color(0xFF77216F)
val UbuntuWarmGrey = Color(0xFFAEA79F)
val UbuntuSuccess = Color(0xFF0E8420)
val UbuntuWarning = Color(0xFFE5A50A)
val UbuntuError = Color(0xFFC7162B)
val UbuntuInfo = Color(0xFF3584E4)

/** Windows Fluent system accent (default blue). */
val WindowsAccent = Color(0xFF0078D4)
val WindowsAccentLight = Color(0xFF4CC2FF)
val WindowsSuccess = Color(0xFF0F7B0F)
val WindowsWarning = Color(0xFF9D5D00)
val WindowsError = Color(0xFFC42B1C)
val WindowsInfo = Color(0xFF0078D4)

// Shared status aliases (UI primarily uses MaterialTheme / Platform chrome tokens).
val GitBlue = UbuntuInfo
val GitGreen = UbuntuSuccess
val GitRed = UbuntuError
val GitAmber = UbuntuOrange

enum class DesktopPlatformStyle {
    Ubuntu,
    Windows
}

fun detectDesktopPlatformStyle(): DesktopPlatformStyle {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return if (os.contains("win")) DesktopPlatformStyle.Windows else DesktopPlatformStyle.Ubuntu
}

@Immutable
data class PlatformChromeColors(
    val headerBar: Color,
    val headerBarOn: Color,
    val sidebar: Color,
    val sidebarOn: Color,
    val cardBorder: Color,
    val codeBackground: Color,
    val codeOn: Color,
    val codeGutter: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val selection: Color,
    val hover: Color,
    val inactive: Color
)

/** Back-compat name used by existing screens. */
typealias LinuxExtraColors = PlatformChromeColors

val LocalLinuxColors = staticCompositionLocalOf {
    PlatformChromeColors(
        headerBar = Color.Unspecified,
        headerBarOn = Color.Unspecified,
        sidebar = Color.Unspecified,
        sidebarOn = Color.Unspecified,
        cardBorder = Color.Unspecified,
        codeBackground = Color.Unspecified,
        codeOn = Color.Unspecified,
        codeGutter = Color.Unspecified,
        success = UbuntuSuccess,
        warning = UbuntuWarning,
        info = UbuntuInfo,
        selection = Color.Unspecified,
        hover = Color.Unspecified,
        inactive = Color.Unspecified
    )
}

// ── Ubuntu / Yaru ──────────────────────────────────────────────────────────

private val UbuntuLightChrome = PlatformChromeColors(
    headerBar = Color(0xFFFAFAFA),
    headerBarOn = Color(0xFF2C2C2C),
    sidebar = Color(0xFFF0F0F0),
    sidebarOn = Color(0xFF2C2C2C),
    cardBorder = Color(0xFFD9D9D9),
    codeBackground = Color(0xFFFCFCFC),
    codeOn = Color(0xFF2C2C2C),
    codeGutter = Color(0xFFF0F0F0),
    success = UbuntuSuccess,
    warning = Color(0xFFC64600),
    info = UbuntuInfo,
    selection = Color(0x33E95420),
    hover = Color(0x14000000),
    inactive = Color(0xFF8C8C8C)
)

private val UbuntuDarkChrome = PlatformChromeColors(
    headerBar = Color(0xFF2C2C2C),
    headerBarOn = Color(0xFFF2F2F2),
    sidebar = Color(0xFF252525),
    sidebarOn = Color(0xFFF2F2F2),
    cardBorder = Color(0xFF4A4A4A),
    codeBackground = Color(0xFF1E1E1E),
    codeOn = Color(0xFFE8E8E8),
    codeGutter = Color(0xFF252525),
    success = Color(0xFF57E389),
    warning = Color(0xFFFFBE6F),
    info = Color(0xFF62A0EA),
    selection = Color(0x44E95420),
    hover = Color(0x14FFFFFF),
    inactive = Color(0xFF9A9A9A)
)

private val UbuntuLightColors: ColorScheme = lightColorScheme(
    primary = UbuntuOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = UbuntuAubergine,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5D9F0),
    onSecondaryContainer = Color(0xFF2D0A2A),
    tertiary = UbuntuSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB6F2C0),
    onTertiaryContainer = Color(0xFF00210A),
    error = UbuntuError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF2C2C2C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFEBEBEB),
    surfaceContainerHighest = Color(0xFFE6E6E6),
    inverseSurface = Color(0xFF2C2C2C),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFFFB59A),
    outline = Color(0xFFC0C0C0),
    outlineVariant = Color(0xFFD9D9D9),
    scrim = Color(0x99000000),
    surfaceTint = UbuntuOrange
)

private val UbuntuDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF7B5A),
    onPrimary = Color(0xFF3B0A00),
    primaryContainer = Color(0xFFB33A14),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFE0A8D8),
    onSecondary = Color(0xFF3B0A36),
    secondaryContainer = Color(0xFF5A184F),
    onSecondaryContainer = Color(0xFFF5D9F0),
    tertiary = Color(0xFF57E389),
    onTertiary = Color(0xFF003912),
    tertiaryContainer = Color(0xFF0B6B1C),
    onTertiaryContainer = Color(0xFFB6F2C0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF3D3D3D),
    onSurfaceVariant = Color(0xFFC8C8C8),
    surfaceContainerLowest = Color(0xFF1A1A1A),
    surfaceContainerLow = Color(0xFF252525),
    surfaceContainer = Color(0xFF2C2C2C),
    surfaceContainerHigh = Color(0xFF363636),
    surfaceContainerHighest = Color(0xFF3D3D3D),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF2C2C2C),
    inversePrimary = UbuntuOrange,
    outline = Color(0xFF6C6C6C),
    outlineVariant = Color(0xFF4A4A4A),
    scrim = Color(0xCC000000),
    surfaceTint = Color(0xFFFF7B5A)
)

private val UbuntuShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

// ── Windows Fluent ─────────────────────────────────────────────────────────

private val WindowsLightChrome = PlatformChromeColors(
    headerBar = Color(0xFFF3F3F3),
    headerBarOn = Color(0xFF1A1A1A),
    sidebar = Color(0xFFECECEC),
    sidebarOn = Color(0xFF1A1A1A),
    cardBorder = Color(0xFFE0E0E0),
    codeBackground = Color(0xFFFFFFFF),
    codeOn = Color(0xFF1A1A1A),
    codeGutter = Color(0xFFF5F5F5),
    success = WindowsSuccess,
    warning = WindowsWarning,
    info = WindowsInfo,
    selection = Color(0x330078D4),
    hover = Color(0x0A000000),
    inactive = Color(0xFF8A8A8A)
)

private val WindowsDarkChrome = PlatformChromeColors(
    headerBar = Color(0xFF2C2C2C),
    headerBarOn = Color(0xFFFFFFFF),
    sidebar = Color(0xFF202020),
    sidebarOn = Color(0xFFFFFFFF),
    cardBorder = Color(0xFF3F3F3F),
    codeBackground = Color(0xFF1E1E1E),
    codeOn = Color(0xFFE6E6E6),
    codeGutter = Color(0xFF252525),
    success = Color(0xFF6CCB5F),
    warning = Color(0xFFFCE100),
    info = WindowsAccentLight,
    selection = Color(0x440078D4),
    hover = Color(0x14FFFFFF),
    inactive = Color(0xFF9A9A9A)
)

/**
 * Fluent light — white/smoke surfaces, system blue accent (#0078D4).
 */
private val WindowsLightColors: ColorScheme = lightColorScheme(
    primary = WindowsAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE6F7),
    onPrimaryContainer = Color(0xFF003A5D),
    secondary = Color(0xFF5B5B5B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = WindowsSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F0D4),
    onTertiaryContainer = Color(0xFF0A3D0A),
    error = WindowsError,
    onError = Color.White,
    errorContainer = Color(0xFFF9D2CE),
    onErrorContainer = Color(0xFF5C0E08),
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    inverseSurface = Color(0xFF2C2C2C),
    inverseOnSurface = Color(0xFFF3F3F3),
    inversePrimary = WindowsAccentLight,
    outline = Color(0xFFD1D1D1),
    outlineVariant = Color(0xFFE5E5E5),
    scrim = Color(0x99000000),
    surfaceTint = WindowsAccent
)

/**
 * Fluent dark — charcoal / layer surfaces, lighter accent for contrast.
 */
private val WindowsDarkColors: ColorScheme = darkColorScheme(
    primary = WindowsAccentLight,
    onPrimary = Color(0xFF003A5D),
    primaryContainer = Color(0xFF005A9E),
    onPrimaryContainer = Color(0xFFCDE6F7),
    secondary = Color(0xFFC2C2C2),
    onSecondary = Color(0xFF2C2C2C),
    secondaryContainer = Color(0xFF3D3D3D),
    onSecondaryContainer = Color(0xFFE8E8E8),
    tertiary = Color(0xFF6CCB5F),
    onTertiary = Color(0xFF0A3D0A),
    tertiaryContainer = Color(0xFF0F7B0F),
    onTertiaryContainer = Color(0xFFD4F0D4),
    error = Color(0xFFFF99A4),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF202020),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = Color(0xFFC8C8C8),
    surfaceContainerLowest = Color(0xFF1C1C1C),
    surfaceContainerLow = Color(0xFF252525),
    surfaceContainer = Color(0xFF2C2C2C),
    surfaceContainerHigh = Color(0xFF373737),
    surfaceContainerHighest = Color(0xFF424242),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF2C2C2C),
    inversePrimary = WindowsAccent,
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF3F3F3F),
    scrim = Color(0xCC000000),
    surfaceTint = WindowsAccentLight
)

/** Fluent control corners — slightly tighter than Material, closer to WinUI. */
private val WindowsShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

// ── Typography (platform UI font mapping via SansSerif) ────────────────────

private fun desktopTypography(compact: Boolean): Typography {
    // Windows: slightly denser control text; Linux: Yaru-like body sizes
    val body = if (compact) 13.sp else 14.sp
    val bodySmall = if (compact) 12.sp else 12.sp
    return Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-0.25).sp),
        displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
        displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = body, lineHeight = 20.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = bodySmall + 1.sp, lineHeight = 18.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = bodySmall, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
    )
}

@Composable
fun QuickGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    platform: DesktopPlatformStyle = detectDesktopPlatformStyle(),
    content: @Composable () -> Unit
) {
    val colors: ColorScheme
    val chrome: PlatformChromeColors
    val shapes: Shapes
    val typography: Typography
    when (platform) {
        DesktopPlatformStyle.Windows -> {
            colors = if (darkTheme) WindowsDarkColors else WindowsLightColors
            chrome = if (darkTheme) WindowsDarkChrome else WindowsLightChrome
            shapes = WindowsShapes
            typography = desktopTypography(compact = true)
        }
        DesktopPlatformStyle.Ubuntu -> {
            colors = if (darkTheme) UbuntuDarkColors else UbuntuLightColors
            chrome = if (darkTheme) UbuntuDarkChrome else UbuntuLightChrome
            shapes = UbuntuShapes
            typography = desktopTypography(compact = false)
        }
    }
    CompositionLocalProvider(LocalLinuxColors provides chrome) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

/** Chrome tokens (header, sidebar, code editor) — Ubuntu or Fluent depending on OS. */
object LinuxTheme {
    val colors: PlatformChromeColors
        @Composable get() = LocalLinuxColors.current
}

object PlatformTheme {
    val colors: PlatformChromeColors
        @Composable get() = LocalLinuxColors.current
    val style: DesktopPlatformStyle
        get() = detectDesktopPlatformStyle()
}
