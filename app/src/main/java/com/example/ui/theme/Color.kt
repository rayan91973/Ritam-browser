package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// --- Serene Sand (Light) ---
val SandSurface = Color(0xFFFAF9F5)
val SandSurfaceDim = Color(0xFFDCDAD6)
val SandSurfaceContainerLowest = Color(0xFFFFFFFF)
val SandSurfaceContainerLow = Color(0xFFF4F4F0)
val SandSurfaceContainer = Color(0xFFEFEEEA)
val SandSurfaceContainerHigh = Color(0xFFE9E8E4)
val SandSurfaceContainerHighest = Color(0xFFE3E2DF)
val SandOnSurface = Color(0xFF1B1C1A)
val SandOnSurfaceVariant = Color(0xFF414845)
val SandOutline = Color(0xFF717974)
val SandOutlineVariant = Color(0xFFC1C8C3)
val SandInverseSurface = Color(0xFF2F312E)
val SandInverseOnSurface = Color(0xFFF2F1ED)
val SandError = Color(0xFFBA1A1A)
val SandOnError = Color(0xFFFFFFFF)
val SandErrorContainer = Color(0xFFFFDAD6)
val SandOnErrorContainer = Color(0xFF93000A)

// --- Onyx Forest (Dark Slate Green, High Legibility) ---
val OnyxSurface = Color(0xFF1F2824)
val OnyxOledSurface = Color(0xFF1B231F)
val OnyxSurfaceContainerLowest = Color(0xFF1B231F)
val OnyxSurfaceContainerLow = Color(0xFF26312C)
val OnyxSurfaceContainer = Color(0xFF2D3A34)
val OnyxSurfaceContainerHigh = Color(0xFF36453E)
val OnyxSurfaceContainerHighest = Color(0xFF42534B)
val OnyxOnSurface = Color(0xFFF3F7F4)
val OnyxOnSurfaceVariant = Color(0xFFD2DDD5)
val OnyxOutline = Color(0xFF9FB0A6)
val OnyxOutlineVariant = Color(0xFF5A6B62)
val OnyxInverseSurface = Color(0xFFE6ECE8)
val OnyxInverseOnSurface = Color(0xFF25302B)
val OnyxError = Color(0xFFFFB4AB)
val OnyxOnError = Color(0xFF690005)
val OnyxErrorContainer = Color(0xFF93000A)
val OnyxOnErrorContainer = Color(0xFFFFDAD6)

// --- Solarized Warm (Sepia Low-Blue) ---
val SolarSurface = Color(0xFFFDF6E2)
val SolarSurfaceContainerLowest = Color(0xFFFFFBF0)
val SolarSurfaceContainerLow = Color(0xFFF6EDD4)
val SolarSurfaceContainer = Color(0xFFEEE4C8)
val SolarSurfaceContainerHigh = Color(0xFFE6DAB9)
val SolarSurfaceContainerHighest = Color(0xFFDDD0AA)
val SolarOnSurface = Color(0xFF28231A)
val SolarOnSurfaceVariant = Color(0xFF5D5342)
val SolarOutline = Color(0xFF8C7E65)
val SolarOutlineVariant = Color(0xFFD2C5A4)

// --- Accent Matrices ---
// 1. Sage (Default Botanical)
val SagePrimaryLight = Color(0xFF1B3C30)
val SagePrimaryContainerLight = Color(0xFF325346)
val SagePrimaryFixedLight = Color(0xFFC6EBD9)
val SageOnPrimaryFixedLight = Color(0xFF002116)
val SageSecondaryLight = Color(0xFF526069)
val SageSecondaryContainerLight = Color(0xFFD3E2EC)
val SageOnSecondaryContainerLight = Color(0xFF3B4951)

val SagePrimaryDark = Color(0xFF91D5AE)
val SageOnPrimaryDark = Color(0xFF003822)
val SagePrimaryContainerDark = Color(0xFF5C9E7A)
val SageOnPrimaryContainerDark = Color(0xFF00311D)
val SageSecondaryDark = Color(0xFFABCFB9)
val SageSecondaryContainerDark = Color(0xFF2F503F)
val SageOnSecondaryContainerDark = Color(0xFFC6EBD4)

// 2. Terracotta
val TerraPrimaryLight = Color(0xFF8B4513)
val TerraPrimaryContainerLight = Color(0xFFA65A28)
val TerraPrimaryFixedLight = Color(0xFFFFDBC9)
val TerraPrimaryDark = Color(0xFFFFB68E)
val TerraPrimaryContainerDark = Color(0xFF70340A)

// 3. Indigo
val IndigoPrimaryLight = Color(0xFF2C3E50)
val IndigoPrimaryContainerLight = Color(0xFF425970)
val IndigoPrimaryFixedLight = Color(0xFFD2E4F8)
val IndigoPrimaryDark = Color(0xFFA6C8EA)
val IndigoPrimaryContainerDark = Color(0xFF23394D)

// 4. Sand Muted
val MutedSandPrimaryLight = Color(0xFF6E6251)
val MutedSandPrimaryContainerLight = Color(0xFF8D8271)
val MutedSandPrimaryFixedLight = Color(0xFFEDE2D3)
val MutedSandPrimaryDark = Color(0xFFD8C8B4)
val MutedSandPrimaryContainerDark = Color(0xFF554A3B)

@Immutable
data class RitamExtendedColors(
    val isDark: Boolean,
    val surfaceLowest: Color,
    val surfaceLow: Color,
    val surfaceContainer: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val primaryFixed: Color,
    val onPrimaryFixed: Color,
    val secondaryMuted: Color,
    val badgeGreen: Color,
    val cardBorder: Color,
    val privateBanner: Color,
    val onPrivateBanner: Color
)
