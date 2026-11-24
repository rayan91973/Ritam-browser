package com.example.ui.screens

import android.os.Debug
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SiteUsageEntity
import com.example.data.preferences.AccentTone
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.CornerTopology
import com.example.data.preferences.FontPairing
import com.example.data.preferences.LayoutDensity
import com.example.data.preferences.OmniboxTopology
import com.example.data.preferences.ThemePalette
import com.example.privacy.SessionShieldMetrics
import com.example.services.AppCacheStats
import com.example.ui.components.formatBytes
import com.example.ui.theme.IndigoPrimaryLight
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.MutedSandPrimaryLight
import com.example.ui.theme.SagePrimaryLight
import com.example.ui.theme.TerraPrimaryLight
import kotlin.math.roundToInt

@Composable
fun EngineAnalyticsSection(
    adsBlockedTotal: Long,
    trackersBlockedTotal: Long,
    totalBytesSaved: Long,
    sessionMetrics: SessionShieldMetrics,
    openTabsCount: Int,
    localTelemetryEnabled: Boolean,
    onToggleLocalTelemetry: (Boolean) -> Unit,
    cacheStats: AppCacheStats = AppCacheStats(),
    topSites: List<SiteUsageEntity> = emptyList(),
    onClearAppCache: () -> Unit = {},
    onClearSiteUsage: () -> Unit = {},
    onOpenSiteUrl: (String) -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    val runtime = remember { Runtime.getRuntime() }
    val jvmUsedBytes = remember(openTabsCount, sessionMetrics) {
        (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
    }
    val nativeBytes = remember(openTabsCount, sessionMetrics) {
        Debug.getNativeHeapAllocatedSize().coerceAtLeast(0L)
    }
    val totalBudget = remember { runtime.maxMemory().coerceAtLeast(1L) }
    val combinedUsed = jvmUsedBytes + nativeBytes
    val usedRatio = (combinedUsed.toFloat() / totalBudget.toFloat()).coerceIn(0.05f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderRow("ENGINE & PRIVACY ANALYTICS", "On-Device Live")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricKpiCard(
                label = "Data Saved",
                value = formatBytes(totalBytesSaved + sessionMetrics.bytesSaved),
                caption = "Blocked payloads",
                modifier = Modifier.weight(1f)
            )
            MetricKpiCard(
                label = "Trackers Stopped",
                value = (trackersBlockedTotal + sessionMetrics.trackersBlocked).toString(),
                caption = "Session: ${sessionMetrics.trackersBlocked}",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricKpiCard(
                label = "Ads Blocked",
                value = (adsBlockedTotal + sessionMetrics.adsBlocked).toString(),
                caption = "Session: ${sessionMetrics.adsBlocked}",
                modifier = Modifier.weight(1f)
            )
            MetricKpiCard(
                label = "Open Tabs",
                value = openTabsCount.toString(),
                caption = "Up to 8 live background WebViews",
                modifier = Modifier.weight(1f)
            )
        }

        // Application Cache Card (HTTP disk + Docs/Media preview + WebView cache)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Application Cache", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            text = "${cacheStats.cachedFileCount} cached files • Offline fallback ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatBytes(cacheStats.totalBytes),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("HTTP ${formatBytes(cacheStats.httpCacheBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Docs ${formatBytes(cacheStats.previewCacheBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("WebView ${formatBytes(cacheStats.webViewCacheBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = onClearAppCache,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clear_app_cache_button")
                ) {
                    Text("Clear Application Cache")
                }
            }
        }

        // Most Visited Sites Analytics Card (drives Pinned & Top Sites grid)
        if (topSites.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Most Visited Sites", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                "Auto-ranked on Home shortcuts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onClearSiteUsage,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    topSites.take(6).forEachIndexed { idx, site ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenSiteUrl(site.url) }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = site.title.ifBlank { site.domain },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = site.domain,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "${site.visitCount} visits",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real JVM / Native Heap Memory Breakdown
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Memory Segment Breakdown", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(formatBytes(combinedUsed), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { usedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = ext.surfaceHighest
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ART Heap ${formatBytes(jvmUsedBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Native ${formatBytes(nativeBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Cap ${formatBytes(totalBudget)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SettingToggleRow(
            title = "Local Privacy Telemetry",
            subtitle = "Track data & trackers blocked locally in Room DB",
            checked = localTelemetryEnabled,
            onCheckedChange = onToggleLocalTelemetry
        )
    }
}

@Composable
fun PersonalizationSection(
    preferences: BrowserPreferences,
    onSelectPalette: (ThemePalette) -> Unit,
    onSelectAccent: (AccentTone) -> Unit,
    onToggleSurfaceBlur: (Boolean) -> Unit,
    onToggleOledBlack: (Boolean) -> Unit,
    onSelectFont: (FontPairing) -> Unit,
    onChangeFontScale: (Int) -> Unit,
    onToggleFluidTypography: (Boolean) -> Unit,
    onSelectCorner: (CornerTopology) -> Unit,
    onSelectOmnibox: (OmniboxTopology) -> Unit,
    onSelectDensity: (LayoutDensity) -> Unit,
    onResetAppearance: () -> Unit,
    onToggleForceWebDark: (Boolean) -> Unit = {},
    onToggleBottomOmnibox: (Boolean) -> Unit = {},
    onToggleAutoHideToolbars: (Boolean) -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderRow("PERSONALIZATION, THEMING & TOPOLOGY", "Active Skin")

        // 4 Theme Palettes
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Aesthetic Palette", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                val palettes = ThemePalette.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    palettes.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { palette ->
                                val selected = preferences.themePalette == palette
                                Surface(
                                    onClick = { onSelectPalette(palette) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceContainer,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("palette_${palette.name.lowercase()}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            palette.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            palette.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = ext.cardBorder)

                Text("Accent Color Matrix", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccentTone.entries.forEach { tone ->
                        val selected = preferences.accentTone == tone
                        val swatch = when (tone) {
                            AccentTone.SAGE -> SagePrimaryLight
                            AccentTone.TERRACOTTA -> TerraPrimaryLight
                            AccentTone.INDIGO -> IndigoPrimaryLight
                            AccentTone.SAND_MUTED -> MutedSandPrimaryLight
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectAccent(tone) }
                                .padding(vertical = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = swatch,
                                border = BorderStroke(if (selected) 2.5.dp else 1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.8f else 0.2f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                if (selected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(tone.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }

        SettingToggleRow(
            title = "Surface Blur & Translucent Chrome",
            subtitle = "Soft layered surfaces on sheets and omnibox",
            checked = preferences.surfaceBlur,
            onCheckedChange = onToggleSurfaceBlur
        )

        SettingToggleRow(
            title = "OLED Pure Black Mode",
            subtitle = "True #000000 backdrop when dark theme is active",
            checked = preferences.oledPureBlack,
            onCheckedChange = onToggleOledBlack
        )

        SettingToggleRow(
            title = "Force Dark Mode for Webpages",
            subtitle = "Render light websites in dark ink globally unless overridden per site",
            checked = preferences.forceWebDarkMode,
            onCheckedChange = onToggleForceWebDark
        )

        SettingToggleRow(
            title = "Bottom Address Bar",
            subtitle = "Dock the omnibox above the bottom bar for one-handed reach",
            checked = preferences.bottomOmnibox,
            onCheckedChange = onToggleBottomOmnibox
        )

        SettingToggleRow(
            title = "Auto-Hide Toolbars on Scroll",
            subtitle = "Collapse omnibox and bottom bar while scrolling articles",
            checked = preferences.autoHideToolbarsOnScroll,
            onCheckedChange = onToggleAutoHideToolbars
        )

        // Typography Engine
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Typography Engine", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontPairing.entries.forEach { pairing ->
                        val selected = preferences.fontPairing == pairing
                        Surface(
                            onClick = { onSelectFont(pairing) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceContainer,
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Ag", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Text(pairing.displayName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(pairing.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Optical Text Scale", style = MaterialTheme.typography.bodyMedium)
                    Text("${preferences.fontScalePercent}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = preferences.fontScalePercent.toFloat(),
                    onValueChange = { onChangeFontScale(it.roundToInt()) },
                    valueRange = 85f..130f,
                    steps = 8
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fluid Micro-Typography", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = preferences.fluidTypography, onCheckedChange = onToggleFluidTypography)
                }
            }
        }

        // Geometry & Omnibox Topology
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Geometry & Omnibox Topology", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CornerTopology.entries.forEach { corner ->
                        val selected = preferences.cornerTopology == corner
                        Surface(
                            onClick = { onSelectCorner(corner) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceContainer,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(corner.displayName, style = MaterialTheme.typography.labelMedium)
                                Text("${corner.dpValue}dp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OmniboxTopology.entries.forEach { topo ->
                        val selected = preferences.omniboxTopology == topo
                        Surface(
                            onClick = { onSelectOmnibox(topo) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceContainer,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(topo.displayName, style = MaterialTheme.typography.labelMedium)
                                Text(topo.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LayoutDensity.entries.forEach { density ->
                        val selected = preferences.layoutDensity == density
                        Surface(
                            onClick = { onSelectDensity(density) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceContainer,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(density.displayName, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = onResetAppearance,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset Appearance Defaults")
                }
            }
        }
    }
}


@Composable
fun SectionHeaderRow(title: String, badge: String) {
    val ext = LocalRitamExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(shape = RoundedCornerShape(10.dp), color = ext.surfaceHigh) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun MetricKpiCard(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalRitamExtendedColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ext.surfaceLow,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(2.dp))
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ext.surfaceLow,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
