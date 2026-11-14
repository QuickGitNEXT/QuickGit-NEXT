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
import java.io.File

/**
 * Platform-native desktop theming:
 * - GNOME/Ubuntu: Yaru
 * - KDE Plasma: Breeze
 * - Cinnamon (Mint): Mint-Y inspired
 * - MATE: traditional MATE palette
 * - Windows: Fluent
 * Override with -Dquickgitnext.theme=ubuntu|plasma|cinnamon|mate|windows
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

/** Breeze / Plasma accent (KDE system blue). */
val PlasmaAccent = Color(0xFF3DAEE9)
val PlasmaAccentDark = Color(0xFF2980B9)
val PlasmaSuccess = Color(0xFF27AE60)
val PlasmaWarning = Color(0xFFF67400)
val PlasmaError = Color(0xFFDA4453)
val PlasmaInfo = Color(0xFF3DAEE9)

/** Linux Mint Cinnamon / Mint-Y green. */
val CinnamonAccent = Color(0xFF35A854)
val CinnamonAccentDark = Color(0xFF2E7D32)
val CinnamonSuccess = Color(0xFF35A854)
val CinnamonWarning = Color(0xFFE5A50A)
val CinnamonError = Color(0xFFC7162B)
val CinnamonInfo = Color(0xFF2A7B9B)

/** MATE desktop traditional blue-green. */
val MateAccent = Color(0xFF5483B3)
val MateAccentDark = Color(0xFF3465A4)
val MateSuccess = Color(0xFF4E9A06)
val MateWarning = Color(0xFFC4A000)
val MateError = Color(0xFFCC0000)
val MateInfo = Color(0xFF3465A4)

enum class DesktopPlatformStyle {
    Ubuntu,
    Plasma,
    Cinnamon,
    Mate,
    Windows
}

fun detectDesktopPlatformStyle(): DesktopPlatformStyle {
    val forced = System.getProperty("quickgitnext.theme")
        ?.trim()
        ?.lowercase()
        .orEmpty()
    when (forced) {
        "ubuntu", "yaru", "gnome" -> return DesktopPlatformStyle.Ubuntu
        "plasma", "kde", "breeze" -> return DesktopPlatformStyle.Plasma
        "cinnamon", "mint", "mint-y" -> return DesktopPlatformStyle.Cinnamon
        "mate" -> return DesktopPlatformStyle.Mate
        "windows", "fluent", "win" -> return DesktopPlatformStyle.Windows
    }
    val os = System.getProperty("os.name").orEmpty().lowercase()
    if (os.contains("win")) return DesktopPlatformStyle.Windows
    val desktop = (
        System.getenv("XDG_CURRENT_DESKTOP").orEmpty() + ":" +
            System.getenv("DESKTOP_SESSION").orEmpty() + ":" +
            System.getenv("XDG_SESSION_DESKTOP").orEmpty() + ":" +
            System.getenv("GDMSESSION").orEmpty()
        ).lowercase()
    when {
        "cinnamon" in desktop || "x-cinnamon" in desktop -> return DesktopPlatformStyle.Cinnamon
        "mate" in desktop -> return DesktopPlatformStyle.Mate
        "kde" in desktop || "plasma" in desktop -> return DesktopPlatformStyle.Plasma
        else -> return DesktopPlatformStyle.Ubuntu
    }
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
    headerBar = Color(0xFF1E1E1E),
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


// ── KDE Plasma / Breeze ────────────────────────────────────────────────────

private val PlasmaLightChrome = PlatformChromeColors(
    headerBar = Color(0xFFEFF0F1),
    headerBarOn = Color(0xFF232629),
    sidebar = Color(0xFFE8E8E9),
    sidebarOn = Color(0xFF232629),
    cardBorder = Color(0xFFBDC3C7),
    codeBackground = Color(0xFFFCFCFC),
    codeOn = Color(0xFF232629),
    codeGutter = Color(0xFFEFF0F1),
    success = PlasmaSuccess,
    warning = PlasmaWarning,
    info = PlasmaInfo,
    selection = Color(0x333DAEE9),
    hover = Color(0x14000000),
    inactive = Color(0xFF7F8C8D)
)

private val PlasmaDarkChrome = PlatformChromeColors(
    headerBar = Color(0xFF2A2E32),
    headerBarOn = Color(0xFFEFF0F1),
    sidebar = Color(0xFF1B1E20),
    sidebarOn = Color(0xFFEFF0F1),
    cardBorder = Color(0xFF4D4D4D),
    codeBackground = Color(0xFF121517),
    codeOn = Color(0xFFEFF0F1),
    codeGutter = Color(0xFF1B1E20),
    success = Color(0xFF2ECC71),
    warning = Color(0xFFF39C12),
    info = PlasmaAccent,
    selection = Color(0x443DAEE9),
    hover = Color(0x14FFFFFF),
    inactive = Color(0xFF7F8C8D)
)

/** Breeze light — soft grey chrome, Plasma blue accent. */
private val PlasmaLightColors: ColorScheme = lightColorScheme(
    primary = PlasmaAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EFFF),
    onPrimaryContainer = Color(0xFF00344D),
    secondary = Color(0xFF7F8C8D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E9),
    onSecondaryContainer = Color(0xFF232629),
    tertiary = PlasmaSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F5E3),
    onTertiaryContainer = Color(0xFF0E3D24),
    error = PlasmaError,
    onError = Color.White,
    errorContainer = Color(0xFFFADBD8),
    onErrorContainer = Color(0xFF5B1A20),
    background = Color(0xFFEFF0F1),
    onBackground = Color(0xFF232629),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF232629),
    surfaceVariant = Color(0xFFE8E8E9),
    onSurfaceVariant = Color(0xFF4D4D4D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7F8),
    surfaceContainer = Color(0xFFEFF0F1),
    surfaceContainerHigh = Color(0xFFE8E8E9),
    surfaceContainerHighest = Color(0xFFDEDEDF),
    inverseSurface = Color(0xFF31363B),
    inverseOnSurface = Color(0xFFEFF0F1),
    inversePrimary = Color(0xFF7FD4F7),
    outline = Color(0xFFBDC3C7),
    outlineVariant = Color(0xFFD5D6D7),
    scrim = Color(0x99000000),
    surfaceTint = PlasmaAccent
)

/** Breeze dark — #31363B surfaces, bright Plasma blue. */
private val PlasmaDarkColors: ColorScheme = darkColorScheme(
    primary = PlasmaAccent,
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF1A6A8A),
    onPrimaryContainer = Color(0xFFD6EFFF),
    secondary = Color(0xFFB0B3B5),
    onSecondary = Color(0xFF2A2E32),
    secondaryContainer = Color(0xFF3B4045),
    onSecondaryContainer = Color(0xFFEFF0F1),
    tertiary = Color(0xFF2ECC71),
    onTertiary = Color(0xFF0E3D24),
    tertiaryContainer = Color(0xFF1E8449),
    onTertiaryContainer = Color(0xFFD5F5E3),
    error = Color(0xFFF5B7B1),
    onError = Color(0xFF5B1A20),
    errorContainer = Color(0xFF922B34),
    onErrorContainer = Color(0xFFFADBD8),
    background = Color(0xFF1B1E20),
    onBackground = Color(0xFFEFF0F1),
    surface = Color(0xFF2A2E32),
    onSurface = Color(0xFFEFF0F1),
    surfaceVariant = Color(0xFF31363B),
    onSurfaceVariant = Color(0xFFB0B3B5),
    surfaceContainerLowest = Color(0xFF121517),
    surfaceContainerLow = Color(0xFF1B1E20),
    surfaceContainer = Color(0xFF2A2E32),
    surfaceContainerHigh = Color(0xFF3B4045),
    surfaceContainerHighest = Color(0xFF454A4F),
    inverseSurface = Color(0xFFEFF0F1),
    inverseOnSurface = Color(0xFF2A2E32),
    inversePrimary = PlasmaAccentDark,
    outline = Color(0xFF5A5F64),
    outlineVariant = Color(0xFF3B4045),
    scrim = Color(0xCC000000),
    surfaceTint = PlasmaAccent
)

/** Breeze-like control radii (slightly softer than Fluent, tighter than Material). */
private val PlasmaShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)


// ── Cinnamon / Mint-Y ──────────────────────────────────────────────────────

private val CinnamonLightChrome = PlatformChromeColors(
    headerBar = Color(0xFFF6F6F6),
    headerBarOn = Color(0xFF2E3436),
    sidebar = Color(0xFFEEEEEE),
    sidebarOn = Color(0xFF2E3436),
    cardBorder = Color(0xFFD5D5D5),
    codeBackground = Color(0xFFFFFFFF),
    codeOn = Color(0xFF2E3436),
    codeGutter = Color(0xFFF6F6F6),
    success = CinnamonSuccess,
    warning = CinnamonWarning,
    info = CinnamonInfo,
    selection = Color(0x3335A854),
    hover = Color(0x14000000),
    inactive = Color(0xFF888888)
)

private val CinnamonDarkChrome = PlatformChromeColors(
    headerBar = Color(0xFF2E3436),
    headerBarOn = Color(0xFFEEEEEE),
    sidebar = Color(0xFF25292B),
    sidebarOn = Color(0xFFEEEEEE),
    cardBorder = Color(0xFF4A4A4A),
    codeBackground = Color(0xFF1E1E1E),
    codeOn = Color(0xFFEEEEEE),
    codeGutter = Color(0xFF25292B),
    success = Color(0xFF56C26A),
    warning = Color(0xFFE5A50A),
    info = Color(0xFF4CB0D4),
    selection = Color(0x4435A854),
    hover = Color(0x14FFFFFF),
    inactive = Color(0xFF9A9A9A)
)

private val CinnamonLightColors: ColorScheme = lightColorScheme(
    primary = CinnamonAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F0DD),
    onPrimaryContainer = Color(0xFF0D3B1A),
    secondary = Color(0xFF6B6B6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF2E3436),
    tertiary = CinnamonInfo,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0EAF2),
    onTertiaryContainer = Color(0xFF0A3444),
    error = CinnamonError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F6F6),
    onBackground = Color(0xFF2E3436),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2E3436),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9F9),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    inverseSurface = Color(0xFF2E3436),
    inverseOnSurface = Color(0xFFF6F6F6),
    inversePrimary = Color(0xFF8AD49A),
    outline = Color(0xFFC8C8C8),
    outlineVariant = Color(0xFFDCDCDC),
    scrim = Color(0x99000000),
    surfaceTint = CinnamonAccent
)

private val CinnamonDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF56C26A),
    onPrimary = Color(0xFF0D3B1A),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFD6F0DD),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF2E3436),
    secondaryContainer = Color(0xFF3D4345),
    onSecondaryContainer = Color(0xFFEEEEEE),
    tertiary = Color(0xFF4CB0D4),
    onTertiary = Color(0xFF0A3444),
    tertiaryContainer = Color(0xFF1A5368),
    onTertiaryContainer = Color(0xFFD0EAF2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF25292B),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF2E3436),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF3D4345),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceContainerLowest = Color(0xFF1A1D1E),
    surfaceContainerLow = Color(0xFF25292B),
    surfaceContainer = Color(0xFF2E3436),
    surfaceContainerHigh = Color(0xFF383E40),
    surfaceContainerHighest = Color(0xFF43484A),
    inverseSurface = Color(0xFFEEEEEE),
    inverseOnSurface = Color(0xFF2E3436),
    inversePrimary = CinnamonAccentDark,
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF3D4345),
    scrim = Color(0xCC000000),
    surfaceTint = Color(0xFF56C26A)
)

private val CinnamonShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(5.dp),
    medium = RoundedCornerShape(7.dp),
    large = RoundedCornerShape(9.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

// ── MATE ───────────────────────────────────────────────────────────────────

private val MateLightChrome = PlatformChromeColors(
    headerBar = Color(0xFFECECEC),
    headerBarOn = Color(0xFF2E3436),
    sidebar = Color(0xFFE0E0E0),
    sidebarOn = Color(0xFF2E3436),
    cardBorder = Color(0xFFC0C0C0),
    codeBackground = Color(0xFFFFFFFF),
    codeOn = Color(0xFF2E3436),
    codeGutter = Color(0xFFECECEC),
    success = MateSuccess,
    warning = MateWarning,
    info = MateInfo,
    selection = Color(0x335483B3),
    hover = Color(0x14000000),
    inactive = Color(0xFF888A85)
)

private val MateDarkChrome = PlatformChromeColors(
    headerBar = Color(0xFF2E3436),
    headerBarOn = Color(0xFFEEEEEC),
    sidebar = Color(0xFF242729),
    sidebarOn = Color(0xFFEEEEEC),
    cardBorder = Color(0xFF555753),
    codeBackground = Color(0xFF1E1E1E),
    codeOn = Color(0xFFEEEEEC),
    codeGutter = Color(0xFF242729),
    success = Color(0xFF8AE234),
    warning = Color(0xFFFCE94F),
    info = Color(0xFF729FCF),
    selection = Color(0x445483B3),
    hover = Color(0x14FFFFFF),
    inactive = Color(0xFF888A85)
)

private val MateLightColors: ColorScheme = lightColorScheme(
    primary = MateAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E3F4),
    onPrimaryContainer = Color(0xFF0D2B4A),
    secondary = Color(0xFF888A85),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E6),
    onSecondaryContainer = Color(0xFF2E3436),
    tertiary = MateSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8F0C0),
    onTertiaryContainer = Color(0xFF1E3A00),
    error = MateError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFECECEC),
    onBackground = Color(0xFF2E3436),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF2E3436),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF555753),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFECECEC),
    surfaceContainerHigh = Color(0xFFE4E4E4),
    surfaceContainerHighest = Color(0xFFDCDCDC),
    inverseSurface = Color(0xFF2E3436),
    inverseOnSurface = Color(0xFFECECEC),
    inversePrimary = Color(0xFF9AB8D9),
    outline = Color(0xFFB0B0B0),
    outlineVariant = Color(0xFFD0D0D0),
    scrim = Color(0x99000000),
    surfaceTint = MateAccent
)

private val MateDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF729FCF),
    onPrimary = Color(0xFF0D2B4A),
    primaryContainer = Color(0xFF3465A4),
    onPrimaryContainer = Color(0xFFD4E3F4),
    secondary = Color(0xFFB8B8B4),
    onSecondary = Color(0xFF2E3436),
    secondaryContainer = Color(0xFF3D4240),
    onSecondaryContainer = Color(0xFFEEEEEC),
    tertiary = Color(0xFF8AE234),
    onTertiary = Color(0xFF1E3A00),
    tertiaryContainer = Color(0xFF4E9A06),
    onTertiaryContainer = Color(0xFFD8F0C0),
    error = Color(0xFFEF2929),
    onError = Color.White,
    errorContainer = Color(0xFF8A0000),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF242729),
    onBackground = Color(0xFFEEEEEC),
    surface = Color(0xFF2E3436),
    onSurface = Color(0xFFEEEEEC),
    surfaceVariant = Color(0xFF3D4240),
    onSurfaceVariant = Color(0xFFB8B8B4),
    surfaceContainerLowest = Color(0xFF1A1C1D),
    surfaceContainerLow = Color(0xFF242729),
    surfaceContainer = Color(0xFF2E3436),
    surfaceContainerHigh = Color(0xFF383C3E),
    surfaceContainerHighest = Color(0xFF43474A),
    inverseSurface = Color(0xFFEEEEEC),
    inverseOnSurface = Color(0xFF2E3436),
    inversePrimary = MateAccentDark,
    outline = Color(0xFF555753),
    outlineVariant = Color(0xFF3D4240),
    scrim = Color(0xCC000000),
    surfaceTint = Color(0xFF729FCF)
)

private val MateShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp)
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
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = bodySmall, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
    )
}

/**
 * Prefer real Linux desktop settings over Swing's isSystemInDarkTheme(),
 * which often stays light on GNOME/KDE.
 */
fun detectSystemDarkTheme(): Boolean {
    // Explicit override for testing / packaging
    when (System.getProperty("quickgitnext.dark")?.trim()?.lowercase()) {
        "1", "true", "dark", "yes" -> return true
        "0", "false", "light", "no" -> return false
    }
    val os = System.getProperty("os.name").orEmpty().lowercase()
    if (!os.contains("linux")) {
        return swingPanelIsDark()
    }

    // 1) Freedesktop Settings portal (GNOME, KDE Plasma 5/6, etc.)
    //    color-scheme: 0 = no preference, 1 = prefer dark, 2 = prefer light
    readPortalColorScheme()?.let { return it == 1 }

    // 2) KDE: real window background luminance (most reliable for Breeze / BreezeDark)
    readKdeWindowBackgroundDark()?.let { return it }

    // 3) Cinnamon
    readProcessOutput("gsettings", "get", "org.cinnamon.desktop.interface", "gtk-theme")
        ?.lowercase()
        ?.let { out ->
            if ("dark" in out) return true
            if ("mint-y" in out && "dark" !in out) return false
        }

    // 4) MATE
    readProcessOutput("gsettings", "get", "org.mate.interface", "gtk-theme")
        ?.lowercase()
        ?.let { out ->
            if ("dark" in out || "black" in out) return true
        }
    readProcessOutput("gsettings", "get", "org.mate.Marco.general", "theme")
        ?.lowercase()
        ?.let { out ->
            if ("dark" in out || "black" in out) return true
        }

    // 5) GNOME
    readProcessOutput("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
        ?.lowercase()
        ?.let { out ->
            if ("prefer-dark" in out) return true
            if ("prefer-light" in out) return false
        }
    readProcessOutput("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
        ?.lowercase()
        ?.let { out ->
            if ("dark" in out) return true
        }

    System.getenv("GTK_THEME")?.lowercase()?.let {
        if ("dark" in it) return true
    }
    System.getenv("GTK_APPLICATION_PREFER_DARK_THEME")?.let {
        if (it == "1" || it.equals("true", ignoreCase = true)) return true
    }

    return swingPanelIsDark()
}

/** Portal color-scheme: 1 dark, 2 light, null unknown. */
private fun readPortalColorScheme(): Int? {
    // gdbus: (<<uint32 1>>,)
    val out = readProcessOutput(
        "gdbus", "call", "--session",
        "--dest", "org.freedesktop.portal.Desktop",
        "--object-path", "/org/freedesktop/portal/desktop",
        "--method", "org.freedesktop.portal.Settings.Read",
        "org.freedesktop.appearance", "color-scheme"
    ) ?: return null
    val m = Regex("""uint32\s+(\d+)""").find(out)
    return m?.groupValues?.getOrNull(1)?.toIntOrNull()
}

/**
 * true = dark, false = light, null = could not determine.
 * Reads Colors:Window BackgroundNormal from live KDE config.
 */
private fun readKdeWindowBackgroundDark(): Boolean? {
    fun rgbLum(r: Int, g: Int, b: Int) = 0.299 * r + 0.587 * g + 0.114 * b
    fun parseRgb(raw: String): Triple<Int, Int, Int>? {
        val parts = raw.trim()
            .removePrefix("'").removeSuffix("'")
            .removePrefix("\"").removeSuffix("\"")
            .split(Regex("""[,\s]+"""))
            .mapNotNull { it.trim().toIntOrNull() }
        return if (parts.size >= 3) Triple(parts[0], parts[1], parts[2]) else null
    }
    fun schemeNameIsDark(name: String): Boolean? {
        val n = name.lowercase()
        if (n.isBlank()) return null
        if ("dark" in n || n == "breezedark" || "breeze-dark" in n) return true
        if ("light" in n) return false
        return null
    }

    for (tool in listOf("kreadconfig6", "kreadconfig5")) {
        parseRgb(
            readProcessOutput(
                tool, "--file", "kdeglobals", "--group", "Colors:Window", "--key", "BackgroundNormal"
            ).orEmpty()
        )?.let { (r, g, b) -> return rgbLum(r, g, b) < 140.0 }

        schemeNameIsDark(
            readProcessOutput(
                tool, "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme"
            ).orEmpty()
        )?.let { return it }
    }

    val home = System.getProperty("user.home") ?: return null
    // Plasma 6 may also write defaults under kdedefaults
    for (rel in listOf(".config/kdeglobals", ".config/kdedefaults/kdeglobals")) {
        val globals = File(home, rel)
        if (!globals.isFile) continue
        val text = runCatching { globals.readText() }.getOrNull() ?: continue

        val winSection = Regex("""(?is)\[Colors:Window\](.*?)(\n\[|\z)""")
            .find(text)?.groupValues?.getOrNull(1).orEmpty()
        Regex("""(?im)^BackgroundNormal\s*=\s*([0-9]+)\s*,\s*([0-9]+)\s*,\s*([0-9]+)""")
            .find(winSection)
            ?.groupValues
            ?.let { gv ->
                return rgbLum(gv[1].toInt(), gv[2].toInt(), gv[3].toInt()) < 140.0
            }

        // Sometimes only View background is populated
        val viewSection = Regex("""(?is)\[Colors:View\](.*?)(\n\[|\z)""")
            .find(text)?.groupValues?.getOrNull(1).orEmpty()
        Regex("""(?im)^BackgroundNormal\s*=\s*([0-9]+)\s*,\s*([0-9]+)\s*,\s*([0-9]+)""")
            .find(viewSection)
            ?.groupValues
            ?.let { gv ->
                return rgbLum(gv[1].toInt(), gv[2].toInt(), gv[3].toInt()) < 140.0
            }

        schemeNameIsDark(
            Regex("""(?im)^ColorScheme\s*=\s*(.+)$""").find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        )?.let { return it }

        val look = Regex("""(?im)^LookAndFeelPackage\s*=\s*(.+)$""")
            .find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (look.contains("dark", ignoreCase = true)) return true
    }
    return null
}

private fun swingPanelIsDark(): Boolean = runCatching {
    val bg = javax.swing.UIManager.getLookAndFeel()?.defaults?.getColor("Panel.background")
        ?: return@runCatching false
    (0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue) < 128
}.getOrDefault(false)

private fun readProcessOutput(vararg cmd: String): String? = runCatching {
    val p = ProcessBuilder(*cmd)
        .redirectErrorStream(true)
        .start()
    val out = p.inputStream.bufferedReader().readText().trim()
    p.waitFor()
    out.takeIf { it.isNotEmpty() }
}.getOrNull()

@Composable
fun isAppInDarkTheme(): Boolean {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    // On Linux Swing often lies; use desktop settings. Elsewhere trust Compose.
    return if (os.contains("linux")) detectSystemDarkTheme() else isSystemInDarkTheme()
}

@Composable
fun QuickGitTheme(
    darkTheme: Boolean = isAppInDarkTheme(),
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
        DesktopPlatformStyle.Plasma -> {
            colors = if (darkTheme) PlasmaDarkColors else PlasmaLightColors
            chrome = if (darkTheme) PlasmaDarkChrome else PlasmaLightChrome
            shapes = PlasmaShapes
            typography = desktopTypography(compact = true)
        }
        DesktopPlatformStyle.Cinnamon -> {
            colors = if (darkTheme) CinnamonDarkColors else CinnamonLightColors
            chrome = if (darkTheme) CinnamonDarkChrome else CinnamonLightChrome
            shapes = CinnamonShapes
            typography = desktopTypography(compact = false)
        }
        DesktopPlatformStyle.Mate -> {
            colors = if (darkTheme) MateDarkColors else MateLightColors
            chrome = if (darkTheme) MateDarkChrome else MateLightChrome
            shapes = MateShapes
            typography = desktopTypography(compact = false)
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

/** Chrome tokens (header, sidebar, code editor) — Yaru, Breeze, or Fluent. */
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
