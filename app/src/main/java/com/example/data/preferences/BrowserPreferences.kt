package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.privacy.DnsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ritam_preferences")

enum class ThemePalette(val displayName: String, val subtitle: String) {
    SERENE_SAND("Serene Sand", "Warm Paper"),
    ONYX_FOREST("Onyx Forest", "Deep Slate"),
    SOLARIZED_WARM("Solarized Warm", "Sepia Low-Blue"),
    SYSTEM_FOLLOW("System Follow", "Auto Dynamic")
}

enum class AccentTone(val displayName: String, val hexLabel: String) {
    SAGE("Sage", "#325346"),
    TERRACOTTA("Terracotta", "#8B4513"),
    INDIGO("Indigo", "#2C3E50"),
    SAND_MUTED("Sand Muted", "#8D8271")
}

enum class FontPairing(val displayName: String, val subtitle: String) {
    NEWSREADER("Newsreader", "Editorial Serif"),
    INTER("Inter Geo", "Modern Sans"),
    JETBRAINS_MONO("JB Mono", "Technical")
}

enum class CornerTopology(val displayName: String, val dpValue: Int) {
    SHARP("Sharp", 0),
    ROUNDED("Rounded", 8),
    ORGANIC("Organic", 16)
}

enum class OmniboxTopology(val displayName: String, val subtitle: String) {
    FLOATING_PILL("Floating Pill", "Thumb-island"),
    EDGE_DOCKED("Edge Docked", "Full width")
}

enum class LayoutDensity(val displayName: String) {
    COMPACT("Compact"),
    COMFORTABLE("Comfortable"),
    SPACIOUS("Spacious")
}

enum class SearchEngine(val displayName: String, val searchUrlTemplate: String, val host: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "duckduckgo.com"),
    GOOGLE("Google", "https://www.google.com/search?q=%s", "google.com"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s", "search.brave.com"),
    STARTPAGE("Startpage", "https://www.startpage.com/do/search?q=%s", "startpage.com"),
    WIKIPEDIA("Wikipedia", "https://en.wikipedia.org/wiki/Special:Search?search=%s", "en.wikipedia.org");

    fun buildQueryUrl(query: String): String {
        val encoded = android.net.Uri.encode(query)
        return searchUrlTemplate.replace("%s", encoded)
    }
}

enum class ProfilePersona(val displayName: String, val subtitle: String) {
    PERSONAL("Personal", "Default identity"),
    WORK("Work", "Isolated workspace"),
    ANONYMOUS("Anonymous", "Ephemeral session")
}

data class BrowserPreferences(
    val themePalette: ThemePalette = ThemePalette.ONYX_FOREST,
    val accentTone: AccentTone = AccentTone.SAGE,
    val oledPureBlack: Boolean = false,
    val surfaceBlur: Boolean = true,
    val fontPairing: FontPairing = FontPairing.NEWSREADER,
    val fontScalePercent: Int = 100,
    val fluidTypography: Boolean = true,
    val cornerTopology: CornerTopology = CornerTopology.ROUNDED,
    val omniboxTopology: OmniboxTopology = OmniboxTopology.FLOATING_PILL,
    val layoutDensity: LayoutDensity = LayoutDensity.COMFORTABLE,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val homepageUrl: String = "",
    val adBlockingEnabled: Boolean = true,
    val trackerBlockingEnabled: Boolean = true,
    val maliciousProtectionEnabled: Boolean = true,
    val httpsOnlyEnabled: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val defaultDesktopMode: Boolean = false,
    val restoreTabsOnStartup: Boolean = true,
    val askWhereToSaveDownloads: Boolean = false,
    val downloadDirectoryLabel: String = "/Internal Storage/Download/Ritam",
    val localTelemetryEnabled: Boolean = true,
    val biometricVaultLock: Boolean = true,
    val autofillEnabled: Boolean = true,
    val activeProfile: ProfilePersona = ProfilePersona.PERSONAL,
    val notificationsEnabled: Boolean = true,
    val downloadNotificationsEnabled: Boolean = true,
    val shieldAlertNotificationsEnabled: Boolean = false,
    val blockSiteNotificationPrompts: Boolean = true,
    val gestureSwipeNavEnabled: Boolean = true,
    val gestureSwipeTabsEnabled: Boolean = true,
    val gesturePullRefreshEnabled: Boolean = true,
    val gestureDoubleTapTopEnabled: Boolean = true,
    val cosmeticAdBlockingEnabled: Boolean = true,
    // 18-Feature Expansion
    val forceWebDarkMode: Boolean = false,
    val biometricIncognitoLock: Boolean = false,
    val bottomOmnibox: Boolean = false,
    val blockCookieBanners: Boolean = true,
    val stripTrackingParams: Boolean = true,
    val autoHideToolbarsOnScroll: Boolean = true,
    val clearOnExit: Boolean = false,
    val dnsProvider: DnsProvider = DnsProvider.SYSTEM,
    val customDohUrl: String = "",
    val confirmExternalAppLaunch: Boolean = true,
    val autoCloseTabsDays: Int = 0, // 0 = Never, 7, 14, 30
    val preferredTranslateLanguage: String = "es"
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val ACCENT_TONE = stringPreferencesKey("accent_tone")
        val OLED_PURE_BLACK = booleanPreferencesKey("oled_pure_black")
        val SURFACE_BLUR = booleanPreferencesKey("surface_blur")
        val FONT_PAIRING = stringPreferencesKey("font_pairing")
        val FONT_SCALE_PERCENT = intPreferencesKey("font_scale_percent")
        val FLUID_TYPOGRAPHY = booleanPreferencesKey("fluid_typography")
        val CORNER_TOPOLOGY = stringPreferencesKey("corner_topology")
        val OMNIBOX_TOPOLOGY = stringPreferencesKey("omnibox_topology")
        val LAYOUT_DENSITY = stringPreferencesKey("layout_density")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val HOMEPAGE_URL = stringPreferencesKey("homepage_url")
        val AD_BLOCKING = booleanPreferencesKey("ad_blocking")
        val TRACKER_BLOCKING = booleanPreferencesKey("tracker_blocking")
        val MALICIOUS_PROTECTION = booleanPreferencesKey("malicious_protection")
        val HTTPS_ONLY = booleanPreferencesKey("https_only")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val DEFAULT_DESKTOP_MODE = booleanPreferencesKey("default_desktop_mode")
        val RESTORE_TABS = booleanPreferencesKey("restore_tabs")
        val ASK_WHERE_TO_SAVE = booleanPreferencesKey("ask_where_to_save")
        val LOCAL_TELEMETRY = booleanPreferencesKey("local_telemetry")
        val BIOMETRIC_VAULT_LOCK = booleanPreferencesKey("biometric_vault_lock")
        val AUTOFILL_ENABLED = booleanPreferencesKey("autofill_enabled")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DOWNLOAD_NOTIFICATIONS = booleanPreferencesKey("download_notifications")
        val SHIELD_ALERT_NOTIFICATIONS = booleanPreferencesKey("shield_alert_notifications")
        val BLOCK_SITE_NOTIFICATION_PROMPTS = booleanPreferencesKey("block_site_notification_prompts")
        val GESTURE_SWIPE_NAV = booleanPreferencesKey("gesture_swipe_nav")
        val GESTURE_SWIPE_TABS = booleanPreferencesKey("gesture_swipe_tabs")
        val GESTURE_PULL_REFRESH = booleanPreferencesKey("gesture_pull_refresh")
        val GESTURE_DOUBLE_TAP_TOP = booleanPreferencesKey("gesture_double_tap_top")
        val COSMETIC_AD_BLOCKING = booleanPreferencesKey("cosmetic_ad_blocking")
        val FORCE_WEB_DARK_MODE = booleanPreferencesKey("force_web_dark_mode")
        val BIOMETRIC_INCOGNITO_LOCK = booleanPreferencesKey("biometric_incognito_lock")
        val BOTTOM_OMNIBOX = booleanPreferencesKey("bottom_omnibox")
        val BLOCK_COOKIE_BANNERS = booleanPreferencesKey("block_cookie_banners")
        val STRIP_TRACKING_PARAMS = booleanPreferencesKey("strip_tracking_params")
        val AUTO_HIDE_TOOLBARS = booleanPreferencesKey("auto_hide_toolbars")
        val CLEAR_ON_EXIT = booleanPreferencesKey("clear_on_exit")
        val DNS_PROVIDER = stringPreferencesKey("dns_provider")
        val CUSTOM_DOH_URL = stringPreferencesKey("custom_doh_url")
        val CONFIRM_EXTERNAL_APP_LAUNCH = booleanPreferencesKey("confirm_external_app_launch")
        val AUTO_CLOSE_TABS_DAYS = intPreferencesKey("auto_close_tabs_days")
        val PREFERRED_TRANSLATE_LANGUAGE = stringPreferencesKey("preferred_translate_language")
    }

    val preferences: Flow<BrowserPreferences> = context.dataStore.data.map { prefs ->
        BrowserPreferences(
            themePalette = prefs[Keys.THEME_PALETTE]?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() }
                ?: ThemePalette.ONYX_FOREST,
            accentTone = prefs[Keys.ACCENT_TONE]?.let { runCatching { AccentTone.valueOf(it) }.getOrNull() }
                ?: AccentTone.SAGE,
            oledPureBlack = prefs[Keys.OLED_PURE_BLACK] ?: false,
            surfaceBlur = prefs[Keys.SURFACE_BLUR] ?: true,
            fontPairing = prefs[Keys.FONT_PAIRING]?.let { runCatching { FontPairing.valueOf(it) }.getOrNull() }
                ?: FontPairing.NEWSREADER,
            fontScalePercent = prefs[Keys.FONT_SCALE_PERCENT] ?: 100,
            fluidTypography = prefs[Keys.FLUID_TYPOGRAPHY] ?: true,
            cornerTopology = prefs[Keys.CORNER_TOPOLOGY]?.let { runCatching { CornerTopology.valueOf(it) }.getOrNull() }
                ?: CornerTopology.ROUNDED,
            omniboxTopology = prefs[Keys.OMNIBOX_TOPOLOGY]?.let { runCatching { OmniboxTopology.valueOf(it) }.getOrNull() }
                ?: OmniboxTopology.FLOATING_PILL,
            layoutDensity = prefs[Keys.LAYOUT_DENSITY]?.let { runCatching { LayoutDensity.valueOf(it) }.getOrNull() }
                ?: LayoutDensity.COMFORTABLE,
            searchEngine = prefs[Keys.SEARCH_ENGINE]?.let { runCatching { SearchEngine.valueOf(it) }.getOrNull() }
                ?: SearchEngine.DUCKDUCKGO,
            homepageUrl = prefs[Keys.HOMEPAGE_URL] ?: "",
            adBlockingEnabled = prefs[Keys.AD_BLOCKING] ?: true,
            trackerBlockingEnabled = prefs[Keys.TRACKER_BLOCKING] ?: true,
            maliciousProtectionEnabled = prefs[Keys.MALICIOUS_PROTECTION] ?: true,
            httpsOnlyEnabled = prefs[Keys.HTTPS_ONLY] ?: true,
            javascriptEnabled = prefs[Keys.JAVASCRIPT_ENABLED] ?: true,
            blockThirdPartyCookies = prefs[Keys.BLOCK_THIRD_PARTY_COOKIES] ?: true,
            defaultDesktopMode = prefs[Keys.DEFAULT_DESKTOP_MODE] ?: false,
            restoreTabsOnStartup = prefs[Keys.RESTORE_TABS] ?: true,
            askWhereToSaveDownloads = prefs[Keys.ASK_WHERE_TO_SAVE] ?: false,
            localTelemetryEnabled = prefs[Keys.LOCAL_TELEMETRY] ?: true,
            biometricVaultLock = prefs[Keys.BIOMETRIC_VAULT_LOCK] ?: true,
            autofillEnabled = prefs[Keys.AUTOFILL_ENABLED] ?: true,
            activeProfile = prefs[Keys.ACTIVE_PROFILE]?.let { runCatching { ProfilePersona.valueOf(it) }.getOrNull() }
                ?: ProfilePersona.PERSONAL,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            downloadNotificationsEnabled = prefs[Keys.DOWNLOAD_NOTIFICATIONS] ?: true,
            shieldAlertNotificationsEnabled = prefs[Keys.SHIELD_ALERT_NOTIFICATIONS] ?: false,
            blockSiteNotificationPrompts = prefs[Keys.BLOCK_SITE_NOTIFICATION_PROMPTS] ?: true,
            gestureSwipeNavEnabled = prefs[Keys.GESTURE_SWIPE_NAV] ?: true,
            gestureSwipeTabsEnabled = prefs[Keys.GESTURE_SWIPE_TABS] ?: true,
            gesturePullRefreshEnabled = prefs[Keys.GESTURE_PULL_REFRESH] ?: true,
            gestureDoubleTapTopEnabled = prefs[Keys.GESTURE_DOUBLE_TAP_TOP] ?: true,
            cosmeticAdBlockingEnabled = prefs[Keys.COSMETIC_AD_BLOCKING] ?: true,
            forceWebDarkMode = prefs[Keys.FORCE_WEB_DARK_MODE] ?: false,
            biometricIncognitoLock = prefs[Keys.BIOMETRIC_INCOGNITO_LOCK] ?: false,
            bottomOmnibox = prefs[Keys.BOTTOM_OMNIBOX] ?: false,
            blockCookieBanners = prefs[Keys.BLOCK_COOKIE_BANNERS] ?: true,
            stripTrackingParams = prefs[Keys.STRIP_TRACKING_PARAMS] ?: true,
            autoHideToolbarsOnScroll = prefs[Keys.AUTO_HIDE_TOOLBARS] ?: true,
            clearOnExit = prefs[Keys.CLEAR_ON_EXIT] ?: false,
            dnsProvider = prefs[Keys.DNS_PROVIDER]?.let { runCatching { DnsProvider.valueOf(it) }.getOrNull() }
                ?: DnsProvider.SYSTEM,
            customDohUrl = prefs[Keys.CUSTOM_DOH_URL] ?: "",
            confirmExternalAppLaunch = prefs[Keys.CONFIRM_EXTERNAL_APP_LAUNCH] ?: true,
            autoCloseTabsDays = prefs[Keys.AUTO_CLOSE_TABS_DAYS] ?: 0,
            preferredTranslateLanguage = prefs[Keys.PREFERRED_TRANSLATE_LANGUAGE] ?: "es"
        )
    }

    suspend fun updateThemePalette(palette: ThemePalette) = edit { it[Keys.THEME_PALETTE] = palette.name }
    suspend fun updateAccentTone(tone: AccentTone) = edit { it[Keys.ACCENT_TONE] = tone.name }
    suspend fun updateOledPureBlack(enabled: Boolean) = edit { it[Keys.OLED_PURE_BLACK] = enabled }
    suspend fun updateSurfaceBlur(enabled: Boolean) = edit { it[Keys.SURFACE_BLUR] = enabled }
    suspend fun updateFontPairing(pairing: FontPairing) = edit { it[Keys.FONT_PAIRING] = pairing.name }
    suspend fun updateFontScalePercent(percent: Int) = edit { it[Keys.FONT_SCALE_PERCENT] = percent.coerceIn(85, 130) }
    suspend fun updateFluidTypography(enabled: Boolean) = edit { it[Keys.FLUID_TYPOGRAPHY] = enabled }
    suspend fun updateCornerTopology(topology: CornerTopology) = edit { it[Keys.CORNER_TOPOLOGY] = topology.name }
    suspend fun updateOmniboxTopology(topology: OmniboxTopology) = edit { it[Keys.OMNIBOX_TOPOLOGY] = topology.name }
    suspend fun updateLayoutDensity(density: LayoutDensity) = edit { it[Keys.LAYOUT_DENSITY] = density.name }
    suspend fun updateSearchEngine(engine: SearchEngine) = edit { it[Keys.SEARCH_ENGINE] = engine.name }
    suspend fun updateHomepageUrl(url: String) = edit { it[Keys.HOMEPAGE_URL] = url }
    suspend fun updateAdBlocking(enabled: Boolean) = edit { it[Keys.AD_BLOCKING] = enabled }
    suspend fun updateTrackerBlocking(enabled: Boolean) = edit { it[Keys.TRACKER_BLOCKING] = enabled }
    suspend fun updateMaliciousProtection(enabled: Boolean) = edit { it[Keys.MALICIOUS_PROTECTION] = enabled }
    suspend fun updateHttpsOnly(enabled: Boolean) = edit { it[Keys.HTTPS_ONLY] = enabled }
    suspend fun updateJavascriptEnabled(enabled: Boolean) = edit { it[Keys.JAVASCRIPT_ENABLED] = enabled }
    suspend fun updateBlockThirdPartyCookies(enabled: Boolean) = edit { it[Keys.BLOCK_THIRD_PARTY_COOKIES] = enabled }
    suspend fun updateDefaultDesktopMode(enabled: Boolean) = edit { it[Keys.DEFAULT_DESKTOP_MODE] = enabled }
    suspend fun updateAskWhereToSave(enabled: Boolean) = edit { it[Keys.ASK_WHERE_TO_SAVE] = enabled }
    suspend fun updateLocalTelemetry(enabled: Boolean) = edit { it[Keys.LOCAL_TELEMETRY] = enabled }
    suspend fun updateBiometricVaultLock(enabled: Boolean) = edit { it[Keys.BIOMETRIC_VAULT_LOCK] = enabled }
    suspend fun updateAutofillEnabled(enabled: Boolean) = edit { it[Keys.AUTOFILL_ENABLED] = enabled }
    suspend fun updateActiveProfile(profile: ProfilePersona) = edit { it[Keys.ACTIVE_PROFILE] = profile.name }
    suspend fun updateNotificationsEnabled(enabled: Boolean) = edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    suspend fun updateDownloadNotificationsEnabled(enabled: Boolean) = edit { it[Keys.DOWNLOAD_NOTIFICATIONS] = enabled }
    suspend fun updateShieldAlertNotificationsEnabled(enabled: Boolean) = edit { it[Keys.SHIELD_ALERT_NOTIFICATIONS] = enabled }
    suspend fun updateBlockSiteNotificationPrompts(enabled: Boolean) = edit { it[Keys.BLOCK_SITE_NOTIFICATION_PROMPTS] = enabled }
    suspend fun updateGestureSwipeNav(enabled: Boolean) = edit { it[Keys.GESTURE_SWIPE_NAV] = enabled }
    suspend fun updateGestureSwipeTabs(enabled: Boolean) = edit { it[Keys.GESTURE_SWIPE_TABS] = enabled }
    suspend fun updateGesturePullRefresh(enabled: Boolean) = edit { it[Keys.GESTURE_PULL_REFRESH] = enabled }
    suspend fun updateGestureDoubleTapTop(enabled: Boolean) = edit { it[Keys.GESTURE_DOUBLE_TAP_TOP] = enabled }
    suspend fun updateCosmeticAdBlocking(enabled: Boolean) = edit { it[Keys.COSMETIC_AD_BLOCKING] = enabled }
    suspend fun updateForceWebDarkMode(enabled: Boolean) = edit { it[Keys.FORCE_WEB_DARK_MODE] = enabled }
    suspend fun updateBiometricIncognitoLock(enabled: Boolean) = edit { it[Keys.BIOMETRIC_INCOGNITO_LOCK] = enabled }
    suspend fun updateBottomOmnibox(enabled: Boolean) = edit { it[Keys.BOTTOM_OMNIBOX] = enabled }
    suspend fun updateBlockCookieBanners(enabled: Boolean) = edit { it[Keys.BLOCK_COOKIE_BANNERS] = enabled }
    suspend fun updateStripTrackingParams(enabled: Boolean) = edit { it[Keys.STRIP_TRACKING_PARAMS] = enabled }
    suspend fun updateAutoHideToolbarsOnScroll(enabled: Boolean) = edit { it[Keys.AUTO_HIDE_TOOLBARS] = enabled }
    suspend fun updateClearOnExit(enabled: Boolean) = edit { it[Keys.CLEAR_ON_EXIT] = enabled }
    suspend fun updateDnsProvider(provider: DnsProvider) = edit { it[Keys.DNS_PROVIDER] = provider.name }
    suspend fun updateCustomDohUrl(url: String) = edit { it[Keys.CUSTOM_DOH_URL] = url }
    suspend fun updateConfirmExternalAppLaunch(enabled: Boolean) = edit { it[Keys.CONFIRM_EXTERNAL_APP_LAUNCH] = enabled }
    suspend fun updateAutoCloseTabsDays(days: Int) = edit { it[Keys.AUTO_CLOSE_TABS_DAYS] = days }
    suspend fun updatePreferredTranslateLanguage(langCode: String) = edit { it[Keys.PREFERRED_TRANSLATE_LANGUAGE] = langCode }

    suspend fun resetAppearanceDefaults() {
        context.dataStore.edit {
            it[Keys.THEME_PALETTE] = ThemePalette.ONYX_FOREST.name
            it[Keys.ACCENT_TONE] = AccentTone.SAGE.name
            it[Keys.OLED_PURE_BLACK] = false
            it[Keys.SURFACE_BLUR] = true
            it[Keys.FONT_PAIRING] = FontPairing.NEWSREADER.name
            it[Keys.FONT_SCALE_PERCENT] = 100
            it[Keys.FLUID_TYPOGRAPHY] = true
            it[Keys.CORNER_TOPOLOGY] = CornerTopology.ROUNDED.name
            it[Keys.OMNIBOX_TOPOLOGY] = OmniboxTopology.FLOATING_PILL.name
            it[Keys.LAYOUT_DENSITY] = LayoutDensity.COMFORTABLE.name
            it[Keys.FORCE_WEB_DARK_MODE] = false
            it[Keys.BOTTOM_OMNIBOX] = false
            it[Keys.AUTO_HIDE_TOOLBARS] = true
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
