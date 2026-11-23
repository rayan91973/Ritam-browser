package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import com.example.data.preferences.SearchEngine
import com.example.data.preferences.ThemePalette
import com.example.privacy.DnsProvider
import com.example.privacy.DohResolutionResult
import com.example.privacy.SessionShieldMetrics
import com.example.services.TranslationHelper
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

enum class SettingsFocusSection(val label: String) {
    ALL("All"),
    ANALYTICS("Analytics"),
    APPEARANCE("Appearance"),
    SHIELDS("Shields"),
    NOTIFICATIONS("Notifications"),
    GESTURES("Gestures"),
    VAULT("Vault & Profiles"),
    BROWSER("Browser")
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
    filterLists: List<FilterListEntity>,
    customRules: List<CustomFilterRuleEntity>,
    websiteSettings: List<WebsiteSettingsEntity>,
    vaultEntries: List<VaultEntryEntity>,
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
    onToggleLocalTelemetry: (Boolean) -> Unit,
    onToggleNotificationsEnabled: (Boolean) -> Unit,
    onToggleDownloadNotifications: (Boolean) -> Unit,
    onToggleShieldAlerts: (Boolean) -> Unit,
    onToggleBlockSitePrompts: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onToggleSwipeNav: (Boolean) -> Unit,
    onToggleSwipeTabs: (Boolean) -> Unit,
    onTogglePullRefresh: (Boolean) -> Unit,
    onToggleDoubleTapTop: (Boolean) -> Unit,
    onOpenWidgetPreview: () -> Unit,
    onSelectProfile: (ProfilePersona) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleAutofill: (Boolean) -> Unit,
    onAddVaultEntry: () -> Unit,
    onCopySecret: (String) -> Unit,
    onDeleteVaultEntry: (Long) -> Unit,
    onOpenBookmarksManager: () -> Unit,
    onOpenClearData: () -> Unit,
    dohTestResult: DohResolutionResult? = null,
    isVaultUnlocked: Boolean = false,
    onUnlockVault: () -> Unit = {},
    onToggleForceWebDark: (Boolean) -> Unit = {},
    onToggleBottomOmnibox: (Boolean) -> Unit = {},
    onToggleAutoHideToolbars: (Boolean) -> Unit = {},
    onToggleCookieBanners: (Boolean) -> Unit = {},
    onToggleStripTracking: (Boolean) -> Unit = {},
    onToggleConfirmExternalApp: (Boolean) -> Unit = {},
    onSelectDnsProvider: (DnsProvider) -> Unit = {},
    onSaveCustomDohUrl: (String) -> Unit = {},
    onTestDoh: () -> Unit = {},
    onToggleBiometricIncognito: (Boolean) -> Unit = {},
    onToggleClearOnExit: (Boolean) -> Unit = {},
    onChangeAutoCloseDays: (Int) -> Unit = {},
    onSelectTranslateLanguage: (String) -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    val context = LocalContext.current
    var activeSection by remember(initialSection) { mutableStateOf(initialSection) }
    var homepageDraft by remember(preferences.homepageUrl) { mutableStateOf(preferences.homepageUrl) }

    var hasSystemNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSystemNotificationPermission = granted
        if (granted) {
            onToggleNotificationsEnabled(true)
        }
    }

    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasSystemNotificationPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).testTag("settings_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = "Analytics & Personalization",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = NewsreaderFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Ritam Engine • Privacy, Topology & Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsFocusSection.entries.forEach { sec ->
                FilterChip(
                    selected = activeSection == sec,
                    onClick = { activeSection = sec },
                    label = { Text(sec.label) },
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
                        onToggleLocalTelemetry = onToggleLocalTelemetry
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

            if (activeSection == SettingsFocusSection.ALL || activeSection == SettingsFocusSection.SHIELDS) {
                item {
                    ShieldsAndRulesSection(
                        preferences = preferences,
                        filterLists = filterLists,
                        customRules = customRules,
                        websiteSettings = websiteSettings,
                        onToggleAdBlock = onToggleAdBlock,
                        onToggleCosmeticAdBlock = onToggleCosmeticAdBlock,
                        onToggleTrackerBlock = onToggleTrackerBlock,
                        onToggleMalicious = onToggleMalicious,
                        onToggleHttpsOnly = onToggleHttpsOnly,
                        onToggleJs = onToggleJs,
                        onToggleThirdPartyCookies = onToggleThirdPartyCookies,
                        onToggleFilterList = onToggleFilterList,
                        onAddCustomRule = onAddCustomRule,
                        onToggleCustomRule = onToggleCustomRule,
                        onDeleteCustomRule = onDeleteCustomRule,
                        dohTestResult = dohTestResult,
                        onToggleCookieBanners = onToggleCookieBanners,
                        onToggleStripTracking = onToggleStripTracking,
                        onToggleConfirmExternalApp = onToggleConfirmExternalApp,
                        onSelectDnsProvider = onSelectDnsProvider,
                        onSaveCustomDohUrl = onSaveCustomDohUrl,
                        onTestDoh = onTestDoh
                    )
                }
            }

            if (activeSection == SettingsFocusSection.ALL || activeSection == SettingsFocusSection.NOTIFICATIONS) {
                item {
                    NotificationsSettingsSection(
                        preferences = preferences,
                        hasSystemPermission = hasSystemNotificationPermission,
                        onRequestPermission = requestNotificationPermission,
                        onToggleNotificationsEnabled = onToggleNotificationsEnabled,
                        onToggleDownloadNotifications = onToggleDownloadNotifications,
                        onToggleShieldAlerts = onToggleShieldAlerts,
                        onToggleBlockSitePrompts = onToggleBlockSitePrompts,
                        onSendTestNotification = onSendTestNotification
                    )
                }
            }

            if (activeSection == SettingsFocusSection.ALL || activeSection == SettingsFocusSection.GESTURES) {
                item {
                    GesturesSettingsSection(
                        preferences = preferences,
                        onToggleSwipeNav = onToggleSwipeNav,
                        onToggleSwipeTabs = onToggleSwipeTabs,
                        onTogglePullRefresh = onTogglePullRefresh,
                        onToggleDoubleTapTop = onToggleDoubleTapTop,
                        onOpenWidgetPreview = onOpenWidgetPreview
                    )
                }
            }

            if (activeSection == SettingsFocusSection.ALL || activeSection == SettingsFocusSection.VAULT) {
                item {
                    ProfilesAndVaultSection(
                        preferences = preferences,
                        vaultEntries = vaultEntries,
                        onSelectProfile = onSelectProfile,
                        onToggleBiometric = onToggleBiometric,
                        onToggleAutofill = onToggleAutofill,
                        onAddVaultEntry = onAddVaultEntry,
                        onCopySecret = onCopySecret,
                        onDeleteVaultEntry = onDeleteVaultEntry,
                        isVaultUnlocked = isVaultUnlocked,
                        onUnlockVault = onUnlockVault,
                        onToggleBiometricIncognito = onToggleBiometricIncognito
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

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onOpenBookmarksManager,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Bookmarks Vault")
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
        }
    }
}
