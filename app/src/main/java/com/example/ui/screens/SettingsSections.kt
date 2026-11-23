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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomFilterRuleEntity
import com.example.data.local.FilterListEntity
import com.example.data.local.VaultEntryEntity
import com.example.data.local.WebsiteSettingsEntity
import com.example.data.preferences.AccentTone
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.CornerTopology
import com.example.data.preferences.FontPairing
import com.example.data.preferences.LayoutDensity
import com.example.data.preferences.OmniboxTopology
import com.example.data.preferences.ProfilePersona
import com.example.data.preferences.ThemePalette
import com.example.privacy.DnsProvider
import com.example.privacy.DohResolutionResult
import com.example.privacy.SessionShieldMetrics
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
    onToggleLocalTelemetry: (Boolean) -> Unit
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
                caption = "LRU cap: 5 live WebViews",
                modifier = Modifier.weight(1f)
            )
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
fun ShieldsAndRulesSection(
    preferences: BrowserPreferences,
    filterLists: List<FilterListEntity>,
    customRules: List<CustomFilterRuleEntity>,
    websiteSettings: List<WebsiteSettingsEntity>,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleCosmeticAdBlock: (Boolean) -> Unit,
    onToggleTrackerBlock: (Boolean) -> Unit,
    onToggleMalicious: (Boolean) -> Unit,
    onToggleHttpsOnly: (Boolean) -> Unit,
    onToggleJs: (Boolean) -> Unit,
    onToggleThirdPartyCookies: (Boolean) -> Unit,
    onToggleFilterList: (Long, Boolean) -> Unit,
    onAddCustomRule: (String) -> Unit,
    onToggleCustomRule: (Long, Boolean) -> Unit,
    onDeleteCustomRule: (Long) -> Unit,
    dohTestResult: DohResolutionResult? = null,
    onToggleCookieBanners: (Boolean) -> Unit = {},
    onToggleStripTracking: (Boolean) -> Unit = {},
    onToggleConfirmExternalApp: (Boolean) -> Unit = {},
    onSelectDnsProvider: (DnsProvider) -> Unit = {},
    onSaveCustomDohUrl: (String) -> Unit = {},
    onTestDoh: () -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    var ruleInput by remember { mutableStateOf("") }
    var customDohInput by remember(preferences.customDohUrl) { mutableStateOf(preferences.customDohUrl) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderRow("RITAM SHIELDS & AD-BLOCKER ENGINE", "Active Protection")

        SettingToggleRow("Block Advertisements", "Strip ad networks, pop-unders & video ad calls", preferences.adBlockingEnabled, onToggleAdBlock)
        SettingToggleRow("Cosmetic Ad-Blocker (CSS)", "Collapse ad banners, sponsored cards & empty slots", preferences.cosmeticAdBlockingEnabled, onToggleCosmeticAdBlock)
        SettingToggleRow("Cookie & Consent Banner Blocker", "Hide CMP consent walls and auto-reject non-essential cookies", preferences.blockCookieBanners, onToggleCookieBanners)
        SettingToggleRow("Strip Tracking URL Parameters", "Clean utm_*, fbclid, gclid, igshid & msclkid from links", preferences.stripTrackingParams, onToggleStripTracking)
        SettingToggleRow("Block Trackers & Analytics", "Stop cross-site scripts & telemetry beacons", preferences.trackerBlockingEnabled, onToggleTrackerBlock)
        SettingToggleRow("Malicious Domain Guard", "Warn before loading known phishing hosts", preferences.maliciousProtectionEnabled, onToggleMalicious)
        SettingToggleRow("HTTPS-Only Upgrades", "Automatically upgrade insecure HTTP requests", preferences.httpsOnlyEnabled, onToggleHttpsOnly)
        SettingToggleRow("External App Launch Guard", "Confirm before websites launch other Android apps or intent:// links", preferences.confirmExternalAppLaunch, onToggleConfirmExternalApp)
        SettingToggleRow("JavaScript Engine", "Required by most interactive web apps", preferences.javascriptEnabled, onToggleJs)
        SettingToggleRow("Block Third-Party Cookies", "Isolate cross-origin tracking cookies", preferences.blockThirdPartyCookies, onToggleThirdPartyCookies)

        // Encrypted DNS (DoH)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Encrypted DNS (DoH)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            text = "${preferences.dnsProvider.displayName} • ${preferences.dnsProvider.subtitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DnsProvider.entries.forEach { provider ->
                        FilterChip(
                            selected = preferences.dnsProvider == provider,
                            onClick = { onSelectDnsProvider(provider) },
                            label = { Text(provider.displayName) }
                        )
                    }
                }
                if (preferences.dnsProvider == DnsProvider.CUSTOM) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customDohInput,
                            onValueChange = { customDohInput = it },
                            singleLine = true,
                            placeholder = { Text("https://dns.example.com/dns-query") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { onSaveCustomDohUrl(customDohInput) }) {
                            Text("Save")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onTestDoh) {
                        Text("Test DoH Lookup")
                    }
                    if (dohTestResult != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (dohTestResult.blockedByDns) "Blocked (${dohTestResult.latencyMs} ms)"
                                else if (dohTestResult.resolvedIps.isNotEmpty()) "${dohTestResult.resolvedIps.first()} (${dohTestResult.latencyMs} ms)"
                                else "${dohTestResult.error ?: "Checked"} (${dohTestResult.latencyMs} ms)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${dohTestResult.hostname} via ${preferences.dnsProvider.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Filter Lists
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Subscribed Filter Lists", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                filterLists.forEach { list ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(list.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("${list.description} • ${list.ruleCount} rules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = list.enabled, onCheckedChange = { onToggleFilterList(list.id, it) })
                    }
                }
            }
        }

        // Custom Filter Rules
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Custom Filter Rules", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text("Supports Adblock syntax like ||ads.example.com^ or @@||trusted.org^", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ruleInput,
                        onValueChange = { ruleInput = it },
                        singleLine = true,
                        placeholder = { Text("||tracker.example.com^") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (ruleInput.isNotBlank()) {
                                onAddCustomRule(ruleInput)
                                ruleInput = ""
                            }
                        }
                    ) { Text("Add") }
                }
                customRules.forEach { rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rule.rule, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = rule.enabled, onCheckedChange = { onToggleCustomRule(rule.id, it) })
                        IconButton(onClick = { onDeleteCustomRule(rule.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remove rule", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (websiteSettings.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Per-Site Exceptions (${websiteSettings.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    websiteSettings.forEach { site ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(site.domain, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (site.shieldsEnabled) "Shields On" else "Shields Paused",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (site.shieldsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilesAndVaultSection(
    preferences: BrowserPreferences,
    vaultEntries: List<VaultEntryEntity>,
    onSelectProfile: (ProfilePersona) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleAutofill: (Boolean) -> Unit,
    onAddVaultEntry: () -> Unit,
    onCopySecret: (String) -> Unit,
    onDeleteVaultEntry: (Long) -> Unit,
    isVaultUnlocked: Boolean = false,
    onUnlockVault: () -> Unit = {},
    onToggleBiometricIncognito: (Boolean) -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderRow("PROFILES & LOCAL VAULT", "Device-Only Enclave")

        // Profiles
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Switch Profile Persona", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfilePersona.entries.forEach { persona ->
                        val selected = preferences.activeProfile == persona
                        val icon = when (persona) {
                            ProfilePersona.PERSONAL -> Icons.Rounded.Person
                            ProfilePersona.WORK -> Icons.Rounded.Work
                            ProfilePersona.ANONYMOUS -> Icons.Rounded.VisibilityOff
                        }
                        Surface(
                            onClick = { onSelectProfile(persona) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceContainer,
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(persona.displayName, style = MaterialTheme.typography.labelLarge)
                                Text(persona.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        SettingToggleRow("Biometric Incognito Lock", "Require device fingerprint or PIN when resuming Private tabs", preferences.biometricIncognitoLock, onToggleBiometricIncognito)
        SettingToggleRow("Biometric Vault Lock", "Require device authentication before revealing secrets", preferences.biometricVaultLock, onToggleBiometric)
        SettingToggleRow("Inline Credential Auto-fill", "Offer saved local credentials on trusted HTTPS domains", preferences.autofillEnabled, onToggleAutofill)

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
                        Text("Local Passwords & API Keys (${vaultEntries.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text("Zero cloud sync • Stored exclusively in local Room DB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = onAddVaultEntry) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
                if (vaultEntries.isEmpty()) {
                    Text(
                        "No local vault credentials saved yet. Tap Add to store a login or developer token locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else if (preferences.biometricVaultLock && !isVaultUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ext.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Text(
                                text = "${vaultEntries.size} encrypted local entry(ies) locked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onUnlockVault) {
                                Text("Unlock Vault with Biometrics")
                            }
                        }
                    }
                } else {
                    vaultEntries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ext.surfaceContainer)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text(
                                    "${entry.domain.ifBlank { entry.category }} • ${entry.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onCopySecret(entry.secretValue) }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy secret", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDeleteVaultEntry(entry.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete entry", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsSettingsSection(
    preferences: BrowserPreferences,
    hasSystemPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggleNotificationsEnabled: (Boolean) -> Unit,
    onToggleDownloadNotifications: (Boolean) -> Unit,
    onToggleShieldAlerts: (Boolean) -> Unit,
    onToggleBlockSitePrompts: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderRow(
            "NOTIFICATIONS & ALERTS",
            if (hasSystemPermission) "Permitted" else "Action Needed"
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasSystemPermission) "System Notification Permission Granted" else "Allow System Notifications",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Receive alerts when pages are saved offline, downloads complete, or Read Aloud runs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasSystemPermission) {
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("notifications_grant_button")
                        ) {
                            Text("Grant Permission")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (!hasSystemPermission) onRequestPermission() else onSendTestNotification()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("notifications_test_button")
                    ) {
                        Text("Send Test Alert")
                    }
                }
            }
        }

        SettingToggleRow(
            title = "Enable Browser Notifications",
            subtitle = "Master switch for Ritam alerts and Read Aloud status",
            checked = preferences.notificationsEnabled,
            onCheckedChange = { enabled ->
                if (enabled && !hasSystemPermission) onRequestPermission()
                onToggleNotificationsEnabled(enabled)
            }
        )
        SettingToggleRow(
            title = "Downloads & Saved Pages Alerts",
            subtitle = "Notify when offline archives (.mht/.html) or downloads finish",
            checked = preferences.downloadNotificationsEnabled,
            onCheckedChange = onToggleDownloadNotifications
        )
        SettingToggleRow(
            title = "Ad-Blocker & Shield Alerts",
            subtitle = "Post milestone summary when heavy ad/tracker scripts are blocked",
            checked = preferences.shieldAlertNotificationsEnabled,
            onCheckedChange = onToggleShieldAlerts
        )
        SettingToggleRow(
            title = "Quiet Site Notification Prompts",
            subtitle = "Suppress intrusive permission popups from websites",
            checked = preferences.blockSiteNotificationPrompts,
            onCheckedChange = onToggleBlockSitePrompts
        )
    }
}

@Composable
fun GesturesSettingsSection(
    preferences: BrowserPreferences,
    onToggleSwipeNav: (Boolean) -> Unit,
    onToggleSwipeTabs: (Boolean) -> Unit,
    onTogglePullRefresh: (Boolean) -> Unit,
    onToggleDoubleTapTop: (Boolean) -> Unit,
    onOpenWidgetPreview: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeaderRow("GESTURES & HOME WIDGETS", "Touch Controls")

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Rounded.Gesture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Swipe horizontally across the bottom dock to go back/forward, swipe the omnibox pill to switch tabs, pull down to refresh, or double-tap to scroll to top.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingToggleRow(
            title = "Swipe Bottom Bar Back / Forward",
            subtitle = "Drag left or right on the bottom dock for page history",
            checked = preferences.gestureSwipeNavEnabled,
            onCheckedChange = onToggleSwipeNav
        )
        SettingToggleRow(
            title = "Swipe Address Bar to Switch Tabs",
            subtitle = "Swipe horizontally on the omnibox pill to jump between open tabs",
            checked = preferences.gestureSwipeTabsEnabled,
            onCheckedChange = onToggleSwipeTabs
        )
        SettingToggleRow(
            title = "Pull Down Omnibox to Reload",
            subtitle = "Drag downward on the address bar pill to refresh active webpage",
            checked = preferences.gesturePullRefreshEnabled,
            onCheckedChange = onTogglePullRefresh
        )
        SettingToggleRow(
            title = "Double-Tap Address Bar to Scroll Top",
            subtitle = "Quickly jump back to top of long articles",
            checked = preferences.gestureDoubleTapTopEnabled,
            onCheckedChange = onToggleDoubleTapTop
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Home Screen Search Widget",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Pin Search, Voice & Incognito shortcut bar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onOpenWidgetPreview,
                    modifier = Modifier.height(48.dp).testTag("settings_add_widget_button")
                ) {
                    Text("Widget")
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
