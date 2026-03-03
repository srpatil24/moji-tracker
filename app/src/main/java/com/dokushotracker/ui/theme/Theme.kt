package com.dokushotracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.dokushotracker.domain.model.AccentColorOption
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ThemeMode

private data class AccentPalette(
    val primaryLight: Color,
    val secondaryLight: Color,
    val secondaryContainerLight: Color,
    val tertiaryLight: Color,
    val tertiaryContainerLight: Color,
    val primaryContainerLight: Color,
    val backgroundLight: Color,
    val surfaceLight: Color,
    val primaryDark: Color,
    val secondaryDark: Color,
    val secondaryContainerDark: Color,
    val tertiaryDark: Color,
    val tertiaryContainerDark: Color,
    val primaryContainerDark: Color,
    val backgroundDark: Color,
    val surfaceDark: Color,
)

private fun paletteFor(accentColorOption: AccentColorOption): AccentPalette = when (accentColorOption) {
    AccentColorOption.SAGE -> AccentPalette(
        primaryLight = Color(0xFF4A6741),
        secondaryLight = Color(0xFF54634D),
        secondaryContainerLight = Color(0xFFD7E8CD),
        tertiaryLight = Color(0xFF386568),
        tertiaryContainerLight = Color(0xFFBBEBEE),
        primaryContainerLight = Color(0xFFCCE8C5),
        backgroundLight = Color(0xFFF8FAF5),
        surfaceLight = Color(0xFFF8FAF5),
        primaryDark = Color(0xFFB2D0A9),
        secondaryDark = Color(0xFFBCCCB2),
        secondaryContainerDark = Color(0xFF3D4B37),
        tertiaryDark = Color(0xFF9FCFD2),
        tertiaryContainerDark = Color(0xFF1E4D50),
        primaryContainerDark = Color(0xFF314A2A),
        backgroundDark = Color(0xFF1A1C19),
        surfaceDark = Color(0xFF1A1C19),
    )
    AccentColorOption.INDIGO -> AccentPalette(
        primaryLight = Color(0xFF3F6BA5),
        secondaryLight = Color(0xFF5A5E8F),
        secondaryContainerLight = Color(0xFFE0E2FF),
        tertiaryLight = Color(0xFF236A7F),
        tertiaryContainerLight = Color(0xFFCDEAF7),
        primaryContainerLight = Color(0xFFD2E4FF),
        backgroundLight = Color(0xFFF6F8FF),
        surfaceLight = Color(0xFFF6F8FF),
        primaryDark = Color(0xFFA9C8FF),
        secondaryDark = Color(0xFFC1C4FF),
        secondaryContainerDark = Color(0xFF3F436B),
        tertiaryDark = Color(0xFF97D1E2),
        tertiaryContainerDark = Color(0xFF004F60),
        primaryContainerDark = Color(0xFF1E4A7A),
        backgroundDark = Color(0xFF171C27),
        surfaceDark = Color(0xFF171C27),
    )
    AccentColorOption.EMERALD -> AccentPalette(
        primaryLight = Color(0xFF2E7D6F),
        secondaryLight = Color(0xFF236A60),
        secondaryContainerLight = Color(0xFFC5ECE3),
        tertiaryLight = Color(0xFF1A667E),
        tertiaryContainerLight = Color(0xFFCBEAF5),
        primaryContainerLight = Color(0xFFC7F0E8),
        backgroundLight = Color(0xFFF3FBF8),
        surfaceLight = Color(0xFFF3FBF8),
        primaryDark = Color(0xFF93E6D4),
        secondaryDark = Color(0xFF9BD7CD),
        secondaryContainerDark = Color(0xFF005148),
        tertiaryDark = Color(0xFF9ED2E4),
        tertiaryContainerDark = Color(0xFF005062),
        primaryContainerDark = Color(0xFF005048),
        backgroundDark = Color(0xFF13201C),
        surfaceDark = Color(0xFF13201C),
    )
    AccentColorOption.AMBER -> AccentPalette(
        primaryLight = Color(0xFFA67C2E),
        secondaryLight = Color(0xFF8E6A23),
        secondaryContainerLight = Color(0xFFF2E0BE),
        tertiaryLight = Color(0xFF7C5B14),
        tertiaryContainerLight = Color(0xFFF0DEB2),
        primaryContainerLight = Color(0xFFFFE4B2),
        backgroundLight = Color(0xFFFFFAF2),
        surfaceLight = Color(0xFFFFFAF2),
        primaryDark = Color(0xFFF0D48A),
        secondaryDark = Color(0xFFD4C091),
        secondaryContainerDark = Color(0xFF5E4920),
        tertiaryDark = Color(0xFFE8C783),
        tertiaryContainerDark = Color(0xFF674D15),
        primaryContainerDark = Color(0xFF6F5315),
        backgroundDark = Color(0xFF211A11),
        surfaceDark = Color(0xFF211A11),
    )
    AccentColorOption.ROSE -> AccentPalette(
        primaryLight = Color(0xFFB94A78),
        secondaryLight = Color(0xFFA35076),
        secondaryContainerLight = Color(0xFFF7D9E6),
        tertiaryLight = Color(0xFF8D4F9C),
        tertiaryContainerLight = Color(0xFFF0D6F5),
        primaryContainerLight = Color(0xFFFFD9E5),
        backgroundLight = Color(0xFFFFF7FA),
        surfaceLight = Color(0xFFFFF7FA),
        primaryDark = Color(0xFFFFB0CC),
        secondaryDark = Color(0xFFE1B6CB),
        secondaryContainerDark = Color(0xFF6E3750),
        tertiaryDark = Color(0xFFDFB8E8),
        tertiaryContainerDark = Color(0xFF64396F),
        primaryContainerDark = Color(0xFF7C2B4F),
        backgroundDark = Color(0xFF23161C),
        surfaceDark = Color(0xFF23161C),
    )
}

private fun contentColor(backgroundColor: Color): Color = if (backgroundColor.luminance() > 0.45f) {
    Color(0xFF1C1B1F)
} else {
    Color(0xFFF4F4F4)
}

private fun accentVariant(base: Color, accent: Color, amount: Float): Color = lerp(base, accent, amount)

private fun lightColorsFor(accentColorOption: AccentColorOption): ColorScheme {
    val palette = paletteFor(accentColorOption)
    val surfaceVariant = accentVariant(palette.surfaceLight, palette.primaryLight, 0.11f)
    val outline = accentVariant(Color(0xFF7A7A7A), palette.primaryLight, 0.18f)
    return lightColorScheme(
        primary = palette.primaryLight,
        onPrimary = contentColor(palette.primaryLight),
        primaryContainer = palette.primaryContainerLight,
        onPrimaryContainer = contentColor(palette.primaryContainerLight),
        secondary = palette.secondaryLight,
        onSecondary = contentColor(palette.secondaryLight),
        secondaryContainer = palette.secondaryContainerLight,
        onSecondaryContainer = contentColor(palette.secondaryContainerLight),
        tertiary = palette.tertiaryLight,
        onTertiary = contentColor(palette.tertiaryLight),
        tertiaryContainer = palette.tertiaryContainerLight,
        onTertiaryContainer = contentColor(palette.tertiaryContainerLight),
        background = palette.backgroundLight,
        onBackground = Color(0xFF1A1B1D),
        surface = palette.surfaceLight,
        onSurface = Color(0xFF1A1B1D),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = accentVariant(Color(0xFF45474A), palette.primaryLight, 0.26f),
        outline = outline,
        outlineVariant = accentVariant(surfaceVariant, outline, 0.7f),
        surfaceTint = palette.primaryLight,
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )
}

private fun darkColorsFor(accentColorOption: AccentColorOption, pureBlackDarkMode: Boolean): ColorScheme {
    val palette = paletteFor(accentColorOption)
    val surfaceVariant = accentVariant(palette.surfaceDark, palette.primaryDark, 0.2f)
    val outline = accentVariant(Color(0xFF8A8D90), palette.primaryDark, 0.2f)
    val base = darkColorScheme(
        primary = palette.primaryDark,
        onPrimary = contentColor(palette.primaryDark),
        primaryContainer = palette.primaryContainerDark,
        onPrimaryContainer = contentColor(palette.primaryContainerDark),
        secondary = palette.secondaryDark,
        onSecondary = contentColor(palette.secondaryDark),
        secondaryContainer = palette.secondaryContainerDark,
        onSecondaryContainer = contentColor(palette.secondaryContainerDark),
        tertiary = palette.tertiaryDark,
        onTertiary = contentColor(palette.tertiaryDark),
        tertiaryContainer = palette.tertiaryContainerDark,
        onTertiaryContainer = contentColor(palette.tertiaryContainerDark),
        background = palette.backgroundDark,
        onBackground = Color(0xFFE9E9EC),
        surface = palette.surfaceDark,
        onSurface = Color(0xFFE9E9EC),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = accentVariant(Color(0xFFCBCDD2), palette.primaryDark, 0.22f),
        outline = outline,
        outlineVariant = accentVariant(surfaceVariant, outline, 0.65f),
        surfaceTint = palette.primaryDark,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
    if (!pureBlackDarkMode) {
        return base
    }
    return base.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF111111),
        onBackground = Color(0xFFF2F2F2),
        onSurface = Color(0xFFF2F2F2),
        outline = Color(0xFF2B2B2B),
        outlineVariant = Color(0xFF1E1E1E),
    )
}

@Composable
fun DokushoTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (useDark) {
        darkColorsFor(settings.accentColor, settings.pureBlackDarkMode)
    } else {
        lightColorsFor(settings.accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
