package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SiteUsageEntity
import com.example.data.preferences.AccentTone
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.CornerTopology
import com.example.data.preferences.FontPairing
import com.example.data.preferences.LayoutDensity
import com.example.data.preferences.OmniboxTopology
import com.example.data.preferences.SearchEngine
import com.example.data.preferences.ThemePalette
import com.example.privacy.SessionShieldMetrics
import com.example.services.AppCacheStats
import com.example.services.TranslationHelper
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

enum class SettingsFocusSection(val label: String) {
    ALL("All"),
    ANALYTICS("Analytics"),
    APPEARANCE("Appearance"),
    BROWSER("Browser"),
    ABOUT("About")
}

@Composable
fun SettingsAnalyticsScreen(
    initialSection: SettingsFocusSection,
    preferences: BrowserPreferences,
    adsBlockedTotal: Long,
    trackersBlockedTotal: Long,
    totalBytesSaved: Long,
    sessionMetrics: SessionShieldMetrics,
    openTabsCount: Int,
    onBack: () -> Unit,
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
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onSaveHomepageUrl: (String) -> Unit,
    onToggleDefaultDesktop: (Boolean) -> Unit,
    onToggleLocalTelemetry: (Boolean) -> Unit,
    onOpenBookmarksManager: () -> Unit,
    onOpenClearData: () -> Unit,
    onToggleForceWebDark: (Boolean) -> Unit = {},
    onToggleBottomOmnibox: (Boolean) -> Unit = {},
    onToggleAutoHideToolbars: (Boolean) -> Unit = {},
    onToggleClearOnExit: (Boolean) -> Unit = {},
    onChangeAutoCloseDays: (Int) -> Unit = {},
    onSelectTranslateLanguage: (String) -> Unit = {},
    onExportConfigBackupUri: (android.net.Uri) -> Unit = {},
    onImportConfigBackupUri: (android.net.Uri) -> Unit = {},
    onOpenWatchlist: () -> Unit = {},
    onOpenFeeds: () -> Unit = {},
    cacheStats: AppCacheStats = AppCacheStats(),
    topSites: List<SiteUsageEntity> = emptyList(),
    onClearAppCache: () -> Unit = {},
    onClearSiteUsage: () -> Unit = {},
    onOpenSiteUrl: (String) -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    var activeSection by remember(initialSection) { mutableStateOf(initialSection) }
    var homepageDraft by remember(preferences.homepageUrl) { mutableStateOf(preferences.homepageUrl) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(onExportConfigBackupUri) }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportConfigBackupUri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).testTag("settings_back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = "Analytics & Personalization",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = NewsreaderFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Ritam Engine • Appearance, Browser & About",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SettingsFocusSection.entries.forEach { sec ->
                FilterChip(
                    selected = activeSection == sec,
                    onClick = { activeSection = sec },
                    label = { Text(sec.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("settings_chip_${sec.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (activeSection == SettingsFocusSection.ALL || activeSection == SettingsFocusSection.ANALYTICS) {
                item {
                    EngineAnalyticsSection(
                        adsBlockedTotal = adsBlockedTotal,
                        trackersBlockedTotal = trackersBlockedTotal,
                        totalBytesSaved = totalBytesSaved,
                        sessionMetrics = sessionMetrics,
                        openTabsCount = openTabsCount,
                        localTelemetryEnabled = preferences.localTelemetryEnabled,
                        onToggleLocalTelemetry = onToggleLocalTelemetry,
                        cacheStats = cacheStats,
                        topSites = topSites,
                        onClearAppCache = onClearAppCache,
                        onClearSiteUsage = onClearSiteUsage,
                        onOpenSiteUrl = onOpenSiteUrl
                    )
                }
            }

            if (activeSection == SettingsFocusSection.ALL || activeSection == SettingsFocusSection.APPEARANCE) {
                item {
                    PersonalizationSection(
                        preferences = preferences,
                        onSelectPalette = onSelectPalette,
                        onSelectAccent = onSelectAccent,
                        onToggleSurfaceBlur = onToggleSurfaceBlur,
                        onToggleOledBlack = onToggleOledBlack,
                        onSelectFont = onSelectFont,
                        onChangeFontScale = onChangeFontScale,
                        onToggleFluidTypography = onToggleFluidTypography,
                        onSelectCorner = onSelectCorner,
                        onSelectOmnibox = onSelectOmnibox,
                        onSelectDensity = onSelectDensity,
                        onResetAppearance = onResetAppearance,
                        onToggleForceWebDark = onToggleForceWebDark,
                        onToggleBottomOmnibox = onToggleBottomOmnibox,
                        onToggleAutoHideToolbars = onToggleAutoHideToolbars
                    )
                }
            }

            if (activeSection == SettingsFocusSection.ALL || activeSection == SettingsFocusSection.BROWSER) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeaderRow("SEARCH ENGINE & BROWSER", "General")

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ext.surfaceLow,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Default Search Engine", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                                SearchEngine.entries.forEach { engine ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = preferences.searchEngine == engine,
                                            onClick = { onSelectSearchEngine(engine) }
                                        )
                                        Column {
                                            Text(engine.displayName, style = MaterialTheme.typography.bodyMedium)
                                            Text(engine.host, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ext.surfaceLow,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Custom Homepage (Optional)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = homepageDraft,
                                        onValueChange = { homepageDraft = it },
                                        singleLine = true,
                                        placeholder = { Text("Leave blank for Sanctuary Home") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(onClick = { onSaveHomepageUrl(homepageDraft) }) {
                                        Text("Save")
                                    }
                                }
                            }
                        }

                        SettingToggleRow(
                            title = "Default Desktop Mode",
                            subtitle = "Request desktop sites for new tabs by default",
                            checked = preferences.defaultDesktopMode,
                            onCheckedChange = onToggleDefaultDesktop
                        )

                        SettingToggleRow(
                            title = "Wipe Browsing Data on Exit",
                            subtitle = "Automatically clear history, cache & cookies when exiting Ritam",
                            checked = preferences.clearOnExit,
                            onCheckedChange = onToggleClearOnExit
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ext.surfaceLow,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.AutoDelete, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Auto-Close Inactive Tabs", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(0 to "Never", 7 to "7 days", 14 to "14 days", 30 to "30 days").forEach { (days, label) ->
                                        FilterChip(
                                            selected = preferences.autoCloseTabsDays == days,
                                            onClick = { onChangeAutoCloseDays(days) },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ext.surfaceLow,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Default Translate Language", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TranslationHelper.supportedLanguages.forEach { lang ->
                                        FilterChip(
                                            selected = preferences.preferredTranslateLanguage.equals(lang.code, ignoreCase = true),
                                            onClick = { onSelectTranslateLanguage(lang.code) },
                                            label = { Text(lang.label) }
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ext.surfaceLow,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Backup & Power Library Tools", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    "Export or restore bookmarks, userscripts, watchlist & RSS feeds via portable JSON.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { exportBackupLauncher.launch("ritam_config_backup.json") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Export JSON")
                                    }
                                    OutlinedButton(
                                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Restore")
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onOpenWatchlist,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Watchlist")
                                    }
                                    OutlinedButton(
                                        onClick = onOpenFeeds,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.RssFeed, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("RSS Feeds")
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onOpenBookmarksManager,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Bookmarks")
                            }
                            Button(
                                onClick = onOpenClearData,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clear Data")
                            }
                        }
                    }
                }
            }

            if (activeSection == SettingsFocusSection.ALL || activeSection == SettingsFocusSection.ABOUT) {
                item {
                    AboutSection()
                }
            }
        }
    }
}
