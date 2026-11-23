package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.preferences.AccentTone
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.CornerTopology
import com.example.data.preferences.LayoutDensity
import com.example.data.preferences.ThemePalette

val LocalRitamExtendedColors = staticCompositionLocalOf {
    RitamExtendedColors(
        isDark = true,
        surfaceLowest = OnyxSurfaceContainerLowest,
        surfaceLow = OnyxSurfaceContainerLow,
        surfaceContainer = OnyxSurfaceContainer,
        surfaceHigh = OnyxSurfaceContainerHigh,
        surfaceHighest = OnyxSurfaceContainerHighest,
        primaryFixed = Color(0xFFACF2C9),
        onPrimaryFixed = Color(0xFF002112),
        secondaryMuted = OnyxOnSurfaceVariant,
        badgeGreen = SagePrimaryDark,
        cardBorder = OnyxOutlineVariant,
        privateBanner = Color(0xFF23322B),
        onPrivateBanner = Color(0xFFACF2C9)
    )
}

val LocalRitamCornerRadius = staticCompositionLocalOf { 12.dp }
val LocalRitamDensityPadding = staticCompositionLocalOf { 12.dp }

@Composable
fun RitamTheme(
    preferences: BrowserPreferences = BrowserPreferences(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (preferences.themePalette) {
        ThemePalette.ONYX_FOREST -> true
        ThemePalette.SERENE_SAND -> false
        ThemePalette.SOLARIZED_WARM -> false
        ThemePalette.SYSTEM_FOLLOW -> systemDark
    }

    val accentPair = resolveAccentColors(preferences.accentTone, isDark)

    val colorScheme = if (isDark) {
        val baseSurface = if (preferences.oledPureBlack) OnyxOledSurface else OnyxSurface
        val lowSurface = if (preferences.oledPureBlack) Color(0xFF0B0E0D) else OnyxSurfaceContainerLow
        darkColorScheme(
            primary = accentPair.primary,
            onPrimary = accentPair.onPrimary,
            primaryContainer = accentPair.primaryContainer,
            onPrimaryContainer = accentPair.onPrimaryContainer,
            secondary = SageSecondaryDark,
            onSecondary = Color(0xFF163627),
            secondaryContainer = SageSecondaryContainerDark,
            onSecondaryContainer = SageOnSecondaryContainerDark,
            tertiary = Color(0xFFB0CDBC),
            onTertiary = Color(0xFF1B352A),
            background = baseSurface,
            onBackground = OnyxOnSurface,
            surface = baseSurface,
            onSurface = OnyxOnSurface,
            surfaceVariant = OnyxSurfaceContainerHighest,
            onSurfaceVariant = OnyxOnSurfaceVariant,
            surfaceContainerLowest = OnyxSurfaceContainerLowest,
            surfaceContainerLow = lowSurface,
            surfaceContainer = OnyxSurfaceContainer,
            surfaceContainerHigh = OnyxSurfaceContainerHigh,
            surfaceContainerHighest = OnyxSurfaceContainerHighest,
            outline = OnyxOutline,
            outlineVariant = OnyxOutlineVariant,
            inverseSurface = OnyxInverseSurface,
            inverseOnSurface = OnyxInverseOnSurface,
            error = OnyxError,
            onError = OnyxOnError,
            errorContainer = OnyxErrorContainer,
            onErrorContainer = OnyxOnErrorContainer
        )
    } else if (preferences.themePalette == ThemePalette.SOLARIZED_WARM) {
        lightColorScheme(
            primary = accentPair.primary,
            onPrimary = accentPair.onPrimary,
            primaryContainer = accentPair.primaryContainer,
            onPrimaryContainer = accentPair.onPrimaryContainer,
            secondary = SolarOnSurfaceVariant,
            onSecondary = Color.White,
            secondaryContainer = SolarSurfaceContainerHigh,
            onSecondaryContainer = SolarOnSurface,
            tertiary = Color(0xFF47574B),
            onTertiary = Color.White,
            background = SolarSurface,
            onBackground = SolarOnSurface,
            surface = SolarSurface,
            onSurface = SolarOnSurface,
            surfaceVariant = SolarSurfaceContainerHighest,
            onSurfaceVariant = SolarOnSurfaceVariant,
            surfaceContainerLowest = SolarSurfaceContainerLowest,
            surfaceContainerLow = SolarSurfaceContainerLow,
            surfaceContainer = SolarSurfaceContainer,
            surfaceContainerHigh = SolarSurfaceContainerHigh,
            surfaceContainerHighest = SolarSurfaceContainerHighest,
            outline = SolarOutline,
            outlineVariant = SolarOutlineVariant,
            inverseSurface = SandInverseSurface,
            inverseOnSurface = SandInverseOnSurface,
            error = SandError,
            onError = SandOnError,
            errorContainer = SandErrorContainer,
            onErrorContainer = SandOnErrorContainer
        )
    } else {
        lightColorScheme(
            primary = accentPair.primary,
            onPrimary = accentPair.onPrimary,
            primaryContainer = accentPair.primaryContainer,
            onPrimaryContainer = accentPair.onPrimaryContainer,
            secondary = SageSecondaryLight,
            onSecondary = Color.White,
            secondaryContainer = SageSecondaryContainerLight,
            onSecondaryContainer = SageOnSecondaryContainerLight,
            tertiary = Color(0xFF293932),
            onTertiary = Color.White,
            background = SandSurface,
            onBackground = SandOnSurface,
            surface = SandSurface,
            onSurface = SandOnSurface,
            surfaceVariant = SandSurfaceContainerHighest,
            onSurfaceVariant = SandOnSurfaceVariant,
            surfaceContainerLowest = SandSurfaceContainerLowest,
            surfaceContainerLow = SandSurfaceContainerLow,
            surfaceContainer = SandSurfaceContainer,
            surfaceContainerHigh = SandSurfaceContainerHigh,
            surfaceContainerHighest = SandSurfaceContainerHighest,
            outline = SandOutline,
            outlineVariant = SandOutlineVariant,
            inverseSurface = SandInverseSurface,
            inverseOnSurface = SandInverseOnSurface,
            error = SandError,
            onError = SandOnError,
            errorContainer = SandErrorContainer,
            onErrorContainer = SandOnErrorContainer
        )
    }

    val extendedColors = if (isDark) {
        RitamExtendedColors(
            isDark = true,
            surfaceLowest = OnyxSurfaceContainerLowest,
            surfaceLow = if (preferences.oledPureBlack) Color(0xFF0A0D0C) else OnyxSurfaceContainerLow,
            surfaceContainer = OnyxSurfaceContainer,
            surfaceHigh = OnyxSurfaceContainerHigh,
            surfaceHighest = OnyxSurfaceContainerHighest,
            primaryFixed = accentPair.primaryFixed,
            onPrimaryFixed = accentPair.onPrimaryFixed,
            secondaryMuted = OnyxOnSurfaceVariant,
            badgeGreen = accentPair.primary,
            cardBorder = OnyxOutlineVariant.copy(alpha = 0.6f),
            privateBanner = Color(0xFF1F2B25),
            onPrivateBanner = accentPair.primaryFixed
        )
    } else if (preferences.themePalette == ThemePalette.SOLARIZED_WARM) {
        RitamExtendedColors(
            isDark = false,
            surfaceLowest = SolarSurfaceContainerLowest,
            surfaceLow = SolarSurfaceContainerLow,
            surfaceContainer = SolarSurfaceContainer,
            surfaceHigh = SolarSurfaceContainerHigh,
            surfaceHighest = SolarSurfaceContainerHighest,
            primaryFixed = accentPair.primaryFixed,
            onPrimaryFixed = accentPair.onPrimaryFixed,
            secondaryMuted = SolarOnSurfaceVariant,
            badgeGreen = accentPair.primary,
            cardBorder = SolarOutlineVariant.copy(alpha = 0.7f),
            privateBanner = Color(0xFF2D2A24),
            onPrivateBanner = Color(0xFFFDF6E2)
        )
    } else {
        RitamExtendedColors(
            isDark = false,
            surfaceLowest = SandSurfaceContainerLowest,
            surfaceLow = SandSurfaceContainerLow,
            surfaceContainer = SandSurfaceContainer,
            surfaceHigh = SandSurfaceContainerHigh,
            surfaceHighest = SandSurfaceContainerHighest,
            primaryFixed = accentPair.primaryFixed,
            onPrimaryFixed = accentPair.onPrimaryFixed,
            secondaryMuted = SandOnSurfaceVariant,
            badgeGreen = accentPair.primary,
            cardBorder = SandOutlineVariant.copy(alpha = 0.65f),
            privateBanner = Color(0xFF232826),
            onPrivateBanner = Color(0xFFFAF9F5)
        )
    }

    val cornerDp: Dp = when (preferences.cornerTopology) {
        CornerTopology.SHARP -> 2.dp
        CornerTopology.ROUNDED -> 12.dp
        CornerTopology.ORGANIC -> 20.dp
    }

    val densityPadding: Dp = when (preferences.layoutDensity) {
        LayoutDensity.COMPACT -> 8.dp
        LayoutDensity.COMFORTABLE -> 12.dp
        LayoutDensity.SPACIOUS -> 16.dp
    }

    val shapes = Shapes(
        small = RoundedCornerShape(if (preferences.cornerTopology == CornerTopology.SHARP) 0.dp else 8.dp),
        medium = RoundedCornerShape(cornerDp),
        large = RoundedCornerShape(if (preferences.cornerTopology == CornerTopology.SHARP) 4.dp else 16.dp),
        extraLarge = RoundedCornerShape(if (preferences.cornerTopology == CornerTopology.SHARP) 4.dp else 24.dp)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalRitamExtendedColors provides extendedColors,
        LocalRitamCornerRadius provides cornerDp,
        LocalRitamDensityPadding provides densityPadding
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildRitamTypography(preferences.fontPairing, preferences.fontScalePercent),
            shapes = shapes,
            content = content
        )
    }
}

private data class AccentColorSet(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryFixed: Color,
    val onPrimaryFixed: Color
)

private fun resolveAccentColors(tone: AccentTone, isDark: Boolean): AccentColorSet {
    return if (isDark) {
        when (tone) {
            AccentTone.SAGE -> AccentColorSet(
                primary = SagePrimaryDark,
                onPrimary = SageOnPrimaryDark,
                primaryContainer = SagePrimaryContainerDark,
                onPrimaryContainer = SageOnPrimaryContainerDark,
                primaryFixed = Color(0xFFACF2C9),
                onPrimaryFixed = Color(0xFF002112)
            )
            AccentTone.TERRACOTTA -> AccentColorSet(
                primary = TerraPrimaryDark,
                onPrimary = Color(0xFF3E1800),
                primaryContainer = TerraPrimaryContainerDark,
                onPrimaryContainer = Color(0xFFFFDBC9),
                primaryFixed = Color(0xFFFFDBC9),
                onPrimaryFixed = Color(0xFF331200)
            )
            AccentTone.INDIGO -> AccentColorSet(
                primary = IndigoPrimaryDark,
                onPrimary = Color(0xFF0C2438),
                primaryContainer = IndigoPrimaryContainerDark,
                onPrimaryContainer = Color(0xFFD2E4F8),
                primaryFixed = Color(0xFFD2E4F8),
                onPrimaryFixed = Color(0xFF0C2438)
            )
            AccentTone.SAND_MUTED -> AccentColorSet(
                primary = MutedSandPrimaryDark,
                onPrimary = Color(0xFF2B2216),
                primaryContainer = MutedSandPrimaryContainerDark,
                onPrimaryContainer = Color(0xFFEDE2D3),
                primaryFixed = Color(0xFFEDE2D3),
                onPrimaryFixed = Color(0xFF2B2216)
            )
        }
    } else {
        when (tone) {
            AccentTone.SAGE -> AccentColorSet(
                primary = SagePrimaryLight,
                onPrimary = Color.White,
                primaryContainer = SagePrimaryContainerLight,
                onPrimaryContainer = Color(0xFFA1C5B5),
                primaryFixed = SagePrimaryFixedLight,
                onPrimaryFixed = SageOnPrimaryFixedLight
            )
            AccentTone.TERRACOTTA -> AccentColorSet(
                primary = TerraPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = TerraPrimaryContainerLight,
                onPrimaryContainer = Color(0xFFFFDBC9),
                primaryFixed = TerraPrimaryFixedLight,
                onPrimaryFixed = Color(0xFF331200)
            )
            AccentTone.INDIGO -> AccentColorSet(
                primary = IndigoPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = IndigoPrimaryContainerLight,
                onPrimaryContainer = Color(0xFFD2E4F8),
                primaryFixed = IndigoPrimaryFixedLight,
                onPrimaryFixed = Color(0xFF0C2438)
            )
            AccentTone.SAND_MUTED -> AccentColorSet(
                primary = MutedSandPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = MutedSandPrimaryContainerLight,
                onPrimaryContainer = Color(0xFFEDE2D3),
                primaryFixed = MutedSandPrimaryFixedLight,
                onPrimaryFixed = Color(0xFF2B2216)
            )
        }
    }
}
