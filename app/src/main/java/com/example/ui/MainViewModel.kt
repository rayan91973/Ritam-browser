package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.RitamApplication
import com.example.browser.ConsoleLogEntry
import com.example.browser.ExternalAppRequest
import com.example.browser.HighlightSelectionRequest
import com.example.browser.MaliciousWarningState
import com.example.browser.NetworkLogEntry
import com.example.browser.PageSourceDialogState
import com.example.browser.ReaderArticle
import com.example.browser.SanctuaryReaderSamples
import com.example.browser.SslWarningState
import com.example.browser.TabPrivacyReport
import com.example.browser.TabState
import com.example.browser.WebContextMenuRequest
import com.example.browser.WebPermissionPromptRequest
import com.example.browser.WebViewManager
import com.example.data.local.BookmarkEntity
import com.example.data.local.BookmarkFolderEntity
import com.example.data.local.CustomFilterRuleEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.FilterListEntity
import com.example.data.local.HighlightEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.QuickControlUsageEntity
import com.example.data.local.ReadingListItemEntity
import com.example.data.local.RssArticleEntity
import com.example.data.local.RssFeedEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.SiteUsageEntity
import com.example.data.local.UserscriptEntity
import com.example.data.local.VaultEntryEntity
import com.example.data.local.WatchlistItemEntity
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
import com.example.privacy.AdultContentPolicy
import com.example.privacy.BlockCategory
import com.example.privacy.DnsProvider
import com.example.privacy.DohResolutionResult
import com.example.privacy.HomographAndPhishingGuard
import com.example.privacy.HomographWarningState
import com.example.privacy.SessionShieldMetrics
import com.example.privacy.UrlCleaner
import com.example.services.AppCacheStats
import com.example.services.PdfDocumentHelper
import com.example.services.PdfViewerState
import com.example.services.PwaShortcutHelper
import com.example.services.ReadAloudState
import com.example.services.RitamCacheManager
import com.example.services.RssFeedHelper
import com.example.services.StorageStats
import com.example.services.StructuredDocType
import com.example.services.StructuredViewerHelper
import com.example.services.StructuredViewerState
import com.example.services.TranslationHelper
import com.example.services.UniversalFileType
import com.example.services.UniversalViewerHelper
import com.example.services.UniversalViewerState
import com.example.services.WatchlistAndSyncHelper
import com.example.widget.RitamSearchWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as RitamApplication).container
    val preferencesRepository = container.preferencesRepository
    val repository = container.browserRepository
    val shieldsEngine = container.shieldsEngine
    val webViewManager = container.webViewManager
    val downloadManager = container.downloadManager
    val notificationManager = container.notificationManager
    val readAloudController = container.readAloudController
    val dohResolver = container.dohResolver
    val mediaSessionHelper = container.mediaSessionHelper

    val preferences: StateFlow<BrowserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, BrowserPreferences())

    val homeBookmarks: StateFlow<List<BookmarkEntity>> = repository.homeBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkFolders: StateFlow<List<BookmarkFolderEntity>> = repository.bookmarkFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topSites: StateFlow<List<SiteUsageEntity>> = repository.topSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickControlUsage: StateFlow<List<QuickControlUsageEntity>> = repository.quickControlUsage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = repository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filterLists: StateFlow<List<FilterListEntity>> = repository.filterLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customFilterRules: StateFlow<List<CustomFilterRuleEntity>> = repository.customFilterRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val websiteSettings: StateFlow<List<WebsiteSettingsEntity>> = repository.websiteSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultEntries: StateFlow<List<VaultEntryEntity>> = repository.vaultEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<SearchHistoryEntity>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingList: StateFlow<List<ReadingListItemEntity>> = repository.readingList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userscripts: StateFlow<List<UserscriptEntity>> = repository.userscripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistItems: StateFlow<List<WatchlistItemEntity>> = repository.watchlistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highlights: StateFlow<List<HighlightEntity>> = repository.highlights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rssFeeds: StateFlow<List<RssFeedEntity>> = repository.rssFeeds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rssArticles: StateFlow<List<RssArticleEntity>> = repository.rssArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloadSpeeds: StateFlow<Map<Long, Long>> = downloadManager.activeSpeeds

    val adsBlockedTotal: StateFlow<Long> = repository.adsBlockedTotal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val trackersBlockedTotal: StateFlow<Long> = repository.trackersBlockedTotal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalBytesSaved: StateFlow<Long> = repository.totalBytesSaved
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val sessionMetrics: StateFlow<SessionShieldMetrics> = shieldsEngine.sessionMetrics

    private val initialTab = TabState()
    private val _tabs = MutableStateFlow(listOf(initialTab))
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<TabState> = combine(_tabs, _activeTabId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull() ?: TabState()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialTab)

    private val recentlyClosedStack = ArrayDeque<TabState>()
    private val _recentlyClosedCount = MutableStateFlow(0)
    val recentlyClosedCount: StateFlow<Int> = _recentlyClosedCount.asStateFlow()

    private val _sslWarning = MutableStateFlow<SslWarningState?>(null)
    val sslWarning: StateFlow<SslWarningState?> = _sslWarning.asStateFlow()

    private val _maliciousWarning = MutableStateFlow<MaliciousWarningState?>(null)
    val maliciousWarning: StateFlow<MaliciousWarningState?> = _maliciousWarning.asStateFlow()

    private val _adultBlockNotice = MutableStateFlow<AdultBlockNotice?>(null)
    val adultBlockNotice: StateFlow<AdultBlockNotice?> = _adultBlockNotice.asStateFlow()

    private val _findInPageActive = MutableStateFlow(false)
    val findInPageActive: StateFlow<Boolean> = _findInPageActive.asStateFlow()

    private val _findInPageQuery = MutableStateFlow("")
    val findInPageQuery: StateFlow<String> = _findInPageQuery.asStateFlow()

    private val _readerFontSizeSp = MutableStateFlow(17)
    val readerFontSizeSp: StateFlow<Int> = _readerFontSizeSp.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _storageStats = MutableStateFlow(downloadManager.getDeviceStorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _pageSourceState = MutableStateFlow<PageSourceDialogState?>(null)
    val pageSourceState: StateFlow<PageSourceDialogState?> = _pageSourceState.asStateFlow()

    val readAloudState: StateFlow<ReadAloudState> = readAloudController.state

    // 18-Feature StateFlows
    private val _isIncognitoLocked = MutableStateFlow(false)
    val isIncognitoLocked: StateFlow<Boolean> = _isIncognitoLocked.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _isToolbarHiddenOnScroll = MutableStateFlow(false)
    val isToolbarHiddenOnScroll: StateFlow<Boolean> = _isToolbarHiddenOnScroll.asStateFlow()

    private val _isBurningSession = MutableStateFlow(false)
    val isBurningSession: StateFlow<Boolean> = _isBurningSession.asStateFlow()

    private val _pdfViewerState = MutableStateFlow<PdfViewerState?>(null)
    val pdfViewerState: StateFlow<PdfViewerState?> = _pdfViewerState.asStateFlow()

    private val _universalViewerState = MutableStateFlow<UniversalViewerState?>(null)
    val universalViewerState: StateFlow<UniversalViewerState?> = _universalViewerState.asStateFlow()

    private val _cacheStats = MutableStateFlow(AppCacheStats())
    val cacheStats: StateFlow<AppCacheStats> = _cacheStats.asStateFlow()

    private val _fullscreenVideoView = MutableStateFlow<View?>(null)
    val fullscreenVideoView: StateFlow<View?> = _fullscreenVideoView.asStateFlow()
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val _devToolsVisible = MutableStateFlow(false)
    val devToolsVisible: StateFlow<Boolean> = _devToolsVisible.asStateFlow()

    private val _devToolsInitialTab = MutableStateFlow(1)
    val devToolsInitialTab: StateFlow<Int> = _devToolsInitialTab.asStateFlow()

    private val _devToolsPageSource = MutableStateFlow("")
    val devToolsPageSource: StateFlow<String> = _devToolsPageSource.asStateFlow()

    private val _cdpFormatState = MutableStateFlow<StructuredViewerState?>(null)
    val cdpFormatState: StateFlow<StructuredViewerState?> = _cdpFormatState.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<ConsoleLogEntry>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogEntry>> = _consoleLogs.asStateFlow()

    private val _networkLogs = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
    val networkLogs: StateFlow<List<NetworkLogEntry>> = _networkLogs.asStateFlow()

    private val _cookiesDump = MutableStateFlow("")
    val cookiesDump: StateFlow<String> = _cookiesDump.asStateFlow()

    private val _localStoragePairs = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val localStoragePairs: StateFlow<List<Pair<String, String>>> = _localStoragePairs.asStateFlow()

    private val _devEvalOutput = MutableStateFlow("")
    val devEvalOutput: StateFlow<String> = _devEvalOutput.asStateFlow()

    private val _externalAppRequest = MutableStateFlow<ExternalAppRequest?>(null)
    val externalAppRequest: StateFlow<ExternalAppRequest?> = _externalAppRequest.asStateFlow()

    private val _longPressContext = MutableStateFlow<WebContextMenuRequest?>(null)
    val longPressContext: StateFlow<WebContextMenuRequest?> = _longPressContext.asStateFlow()

    private val _qrShareUrl = MutableStateFlow<String?>(null)
    val qrShareUrl: StateFlow<String?> = _qrShareUrl.asStateFlow()

    private val _translateDialogOpen = MutableStateFlow(false)
    val translateDialogOpen: StateFlow<Boolean> = _translateDialogOpen.asStateFlow()

    private val _translateExcerpt = MutableStateFlow("")
    val translateExcerpt: StateFlow<String> = _translateExcerpt.asStateFlow()

    private val _dohTestResult = MutableStateFlow<DohResolutionResult?>(null)
    val dohTestResult: StateFlow<DohResolutionResult?> = _dohTestResult.asStateFlow()

    // 19-Feature Power StateFlows
    private val _splitTabId = MutableStateFlow<String?>(null)
    val splitTabId: StateFlow<String?> = _splitTabId.asStateFlow()

    val splitTab: StateFlow<TabState?> = combine(_tabs, _splitTabId) { list, id ->
        if (id == null) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _commandPaletteOpen = MutableStateFlow(false)
    val commandPaletteOpen: StateFlow<Boolean> = _commandPaletteOpen.asStateFlow()

    private val _elementPickerActiveTabId = MutableStateFlow<String?>(null)
    val elementPickerActiveTabId: StateFlow<String?> = _elementPickerActiveTabId.asStateFlow()

    private val _structuredViewerState = MutableStateFlow<StructuredViewerState?>(null)
    val structuredViewerState: StateFlow<StructuredViewerState?> = _structuredViewerState.asStateFlow()

    private val _pendingHighlightRequest = MutableStateFlow<HighlightSelectionRequest?>(null)
    val pendingHighlightRequest: StateFlow<HighlightSelectionRequest?> = _pendingHighlightRequest.asStateFlow()

    private val _userscriptsDialogOpen = MutableStateFlow(false)
    val userscriptsDialogOpen: StateFlow<Boolean> = _userscriptsDialogOpen.asStateFlow()

    private val _watchlistDialogOpen = MutableStateFlow(false)
    val watchlistDialogOpen: StateFlow<Boolean> = _watchlistDialogOpen.asStateFlow()

    // 9-Feature Privacy & Security StateFlows
    private val _homographWarning = MutableStateFlow<HomographWarningState?>(null)
    val homographWarning: StateFlow<HomographWarningState?> = _homographWarning.asStateFlow()

    private val _webPermissionPrompt = MutableStateFlow<WebPermissionPromptRequest?>(null)
    val webPermissionPrompt: StateFlow<WebPermissionPromptRequest?> = _webPermissionPrompt.asStateFlow()

    private val _trackerXRayOpen = MutableStateFlow(false)
    val trackerXRayOpen: StateFlow<Boolean> = _trackerXRayOpen.asStateFlow()

    private var clipboardClearJob: Job? = null
    private var activeMediaTabId: String? = null
    private val lastRecordedVisitByTab = HashMap<String, String>()

    init {
        refreshCacheStats()
        var appliedInitialDefaultDesktop = false
        viewModelScope.launch {
            preferences.collect { prefs ->
                shieldsEngine.updatePreferences(prefs)
                webViewManager.confirmExternalAppLaunch = prefs.confirmExternalAppLaunch
                webViewManager.fingerprintRandomizerEnabled = prefs.fingerprintRandomizerEnabled
                webViewManager.userscriptsEnabled = prefs.userscriptsEnabled
                webViewManager.unwrapRedirectLinksEnabled = prefs.unwrapRedirectLinksEnabled
                webViewManager.homographGuardEnabled = prefs.homographGuardEnabled
                webViewManager.strictReferrerPolicyEnabled = prefs.strictReferrerPolicyEnabled
                webViewManager.blockWebRtcLeaksEnabled = prefs.blockWebRtcLeaksEnabled
                webViewManager.lockdownCovertApisEnabled = prefs.lockdownCovertApisEnabled
                webViewManager.stripTrackingParamsEnabled = prefs.stripTrackingParams
                downloadManager.setDownloadNotificationsEnabled(
                    prefs.notificationsEnabled && prefs.downloadNotificationsEnabled
                )
                readAloudController.setNotificationsEnabled(prefs.notificationsEnabled)
                if (!prefs.autoHideToolbarsOnScroll) {
                    _isToolbarHiddenOnScroll.value = false
                }
                if (!appliedInitialDefaultDesktop) {
                    appliedInitialDefaultDesktop = true
                    if (prefs.biometricIncognitoLock) {
                        _isIncognitoLocked.value = true
                    }
                    if (prefs.autoCloseTabsDays > 0) {
                        closeInactiveTabs(silent = true)
                    }
                    val current = activeTab.value
                    if (current.isHome && current.id == initialTab.id && prefs.defaultDesktopMode != current.isDesktopMode) {
                        updateTab(current.id) { it.copy(isDesktopMode = prefs.defaultDesktopMode) }
                        webViewManager.setDesktopMode(current.id, prefs.defaultDesktopMode, reloadIfLoaded = false)
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.userscripts.collect { list ->
                webViewManager.activeUserscripts = list
            }
        }

        mediaSessionHelper.setHandlers(
            onToggle = {
                val targetId = activeMediaTabId
                    ?.takeIf { id -> _tabs.value.any { it.id == id && !it.isHome } }
                    ?: _activeTabId.value
                toggleTabMediaPlayback(targetId)
            },
            onStop = {
                webViewManager.pauseAllMedia()
                activeMediaTabId = null
                _tabs.update { list -> list.map { it.copy(isPlayingMedia = false) } }
                mediaSessionHelper.release()
            }
        )

        webViewManager.setCallbacks(object : WebViewManager.Callbacks {
            override fun onPageProgress(tabId: String, progress: Int) {
                updateTab(tabId) { it.copy(progress = progress, isLoading = progress < 100) }
            }

            override fun onPageStarted(tabId: String, url: String) {
                val host = extractHost(url)
                val siteZoom = shieldsEngine.getZoomForDomain(host)
                webViewManager.setTabZoom(tabId, siteZoom)
                if (tabId == _activeTabId.value) {
                    _isToolbarHiddenOnScroll.value = false
                }
                val activeGrants = webViewManager.getTemporaryTabPermissions(tabId)
                updateTab(tabId) { tab ->
                    val originChanged = tab.domain.isNotBlank() && host.isNotBlank() &&
                        !tab.domain.equals(host, ignoreCase = true)
                    tab.copy(
                        url = url,
                        domain = host,
                        isLoading = true,
                        progress = 15,
                        zoomPercent = siteZoom,
                        sslSecure = url.startsWith("https://", ignoreCase = true),
                        loadError = null,
                        snippet = url,
                        activeSensorGrants = activeGrants,
                        privacyReport = if (originChanged) TabPrivacyReport() else tab.privacyReport
                    )
                }
            }

            override fun onPageFinished(
                tabId: String,
                url: String,
                title: String,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                val host = extractHost(url)
                val cleanTitle = title.ifBlank { host.ifBlank { url } }
                val siteZoom = shieldsEngine.getZoomForDomain(host)
                webViewManager.setTabZoom(tabId, siteZoom)
                val currentTab = _tabs.value.firstOrNull { it.id == tabId }
                if (currentTab != null && (currentTab.mediaSpeed != 1.0f || currentTab.audioBoost != 1.0f)) {
                    webViewManager.setMediaSpeedAndBoost(tabId, currentTab.mediaSpeed, currentTab.audioBoost)
                }
                updateTab(tabId) {
                    it.copy(
                        url = url,
                        title = cleanTitle,
                        domain = host,
                        isLoading = false,
                        progress = 100,
                        zoomPercent = siteZoom,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        sslSecure = url.startsWith("https://", ignoreCase = true),
                        lastAccessedAt = System.currentTimeMillis()
                    )
                }
                if (currentTab != null && !currentTab.isPrivate &&
                    preferences.value.activeProfile != ProfilePersona.ANONYMOUS
                ) {
                    val isNewVisit = lastRecordedVisitByTab[tabId] != url
                    lastRecordedVisitByTab[tabId] = url
                    viewModelScope.launch {
                        repository.recordHistory(url = url, title = cleanTitle, domain = host)
                        if (isNewVisit && host.isNotBlank()) {
                            repository.recordSiteVisit(domain = host, title = cleanTitle, url = url)
                        }
                        refreshCacheStats()
                    }
                }
            }

            override fun onPageTitle(tabId: String, title: String) {
                updateTab(tabId) { it.copy(title = title) }
            }

            override fun onRequestBlocked(tabId: String, category: BlockCategory, blockedHost: String) {
                var newTotal = 0
                updateTab(tabId) { tab ->
                    val updatedDomains = if (blockedHost.isNotBlank() && !tab.blockedDomains.contains(blockedHost)) {
                        (listOf(blockedHost) + tab.blockedDomains).take(25)
                    } else {
                        tab.blockedDomains
                    }
                    val updatedCounts = if (blockedHost.isNotBlank()) {
                        val map = tab.privacyReport.blockedHostCounts.toMutableMap()
                        map[blockedHost] = (map[blockedHost] ?: 0) + 1
                        map
                    } else tab.privacyReport.blockedHostCounts
                    val updatedReport = tab.privacyReport.copy(blockedHostCounts = updatedCounts)
                    val updated = when (category) {
                        BlockCategory.AD -> tab.copy(
                            adsBlocked = tab.adsBlocked + 1,
                            blockedDomains = updatedDomains,
                            privacyReport = updatedReport
                        )
                        BlockCategory.TRACKER -> tab.copy(
                            trackersBlocked = tab.trackersBlocked + 1,
                            blockedDomains = updatedDomains,
                            privacyReport = updatedReport
                        )
                        else -> tab
                    }
                    newTotal = updated.totalBlocked
                    updated
                }
                val prefs = preferences.value
                if (prefs.notificationsEnabled && prefs.shieldAlertNotificationsEnabled &&
                    newTotal > 0 && newTotal % 10 == 0
                ) {
                    notificationManager.notifyShieldAlert(
                        title = "Ad-Blocker Protected Session",
                        body = "Blocked $newTotal ads & trackers on ${activeTab.value.domain.ifBlank { "this page" }}."
                    )
                }
            }

            override fun onFindMatchesChanged(tabId: String, activeOrdinal: Int, totalMatches: Int) {
                updateTab(tabId) {
                    it.copy(
                        findMatchCurrent = if (totalMatches > 0) activeOrdinal + 1 else 0,
                        findMatchTotal = totalMatches
                    )
                }
            }

            override fun onSslError(tabId: String, url: String, errorSummary: String) {
                updateTab(tabId) { it.copy(sslSecure = false, isLoading = false) }
                _sslWarning.value = SslWarningState(tabId, url, errorSummary)
            }

            override fun onLoadError(tabId: String, description: String) {
                updateTab(tabId) { it.copy(isLoading = false, loadError = description) }
            }

            override fun onDownloadRequested(
                url: String,
                userAgent: String,
                contentDisposition: String,
                mimeType: String,
                contentLength: Long
            ) {
                downloadManager.enqueueDownload(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength
                )
                refreshStorageStats()
                showMessage("Download queued")
            }

            override fun onMaliciousNavigationBlocked(tabId: String, url: String, domain: String) {
                updateTab(tabId) { it.copy(isLoading = false) }
                _maliciousWarning.value = MaliciousWarningState(tabId, url, domain)
            }

            override fun onScrollDelta(tabId: String, dy: Int, scrollY: Int) {
                if (tabId != _activeTabId.value) return
                val tab = activeTab.value
                if (!preferences.value.autoHideToolbarsOnScroll || tab.isHome || tab.isReaderMode || _findInPageActive.value) {
                    if (_isToolbarHiddenOnScroll.value) _isToolbarHiddenOnScroll.value = false
                    return
                }
                if (scrollY <= 40 || dy < -24) {
                    _isToolbarHiddenOnScroll.value = false
                } else if (dy > 28 && scrollY > 150) {
                    _isToolbarHiddenOnScroll.value = true
                }
            }

            override fun onConsoleMessage(tabId: String, entry: ConsoleLogEntry) {
                if (tabId == _activeTabId.value) {
                    _consoleLogs.update { (listOf(entry) + it).take(100) }
                }
            }

            override fun onNetworkRequest(tabId: String, entry: NetworkLogEntry) {
                if (tabId == _activeTabId.value) {
                    _networkLogs.update { (listOf(entry) + it).take(150) }
                }
            }

            override fun onExternalAppRequested(request: ExternalAppRequest) {
                _externalAppRequest.value = request
            }

            override fun onLongPressContext(request: WebContextMenuRequest) {
                _longPressContext.value = request
            }

            override fun onPdfUrlDetected(tabId: String, url: String) {
                openUrlInUniversalViewer(url, "application/pdf")
            }

            override fun onDocumentUrlDetected(tabId: String, url: String, mimeType: String) {
                openUrlInUniversalViewer(url, mimeType)
            }

            override fun onFullscreenVideoChanged(customView: View?, callback: WebChromeClient.CustomViewCallback?) {
                customViewCallback = callback
                _fullscreenVideoView.value = customView
            }

            override fun onMediaPlaybackDetected(tabId: String, hasMedia: Boolean, isPlaying: Boolean) {
                updateTab(tabId) { it.copy(hasActiveMedia = hasMedia, isPlayingMedia = isPlaying) }
                val updatedList = _tabs.value
                val playingTab = if (isPlaying) {
                    updatedList.firstOrNull { it.id == tabId }
                } else {
                    updatedList.firstOrNull { it.isPlayingMedia }
                }
                if (playingTab != null) {
                    activeMediaTabId = playingTab.id
                    mediaSessionHelper.updatePlaybackState(
                        title = playingTab.title,
                        domain = playingTab.domain,
                        isPlaying = true,
                        hasActiveMedia = true,
                        notificationsEnabled = preferences.value.notificationsEnabled
                    )
                } else if (tabId == activeMediaTabId || tabId == _activeTabId.value) {
                    val current = updatedList.firstOrNull { it.id == tabId }
                    if (current != null && current.hasActiveMedia) {
                        activeMediaTabId = current.id
                        mediaSessionHelper.updatePlaybackState(
                            title = current.title,
                            domain = current.domain,
                            isPlaying = false,
                            hasActiveMedia = true,
                            notificationsEnabled = preferences.value.notificationsEnabled
                        )
                    } else {
                        activeMediaTabId = null
                        mediaSessionHelper.release()
                    }
                }
            }

            override fun onElementPicked(tabId: String, domain: String, selector: String) {
                _elementPickerActiveTabId.value = null
                if (selector.isBlank()) {
                    showMessage("Element picker cancelled")
                    return
                }
                val cleanHost = domain.lowercase().removePrefix("www.")
                val rule = if (cleanHost.isNotBlank()) "$cleanHost##$selector" else "##$selector"
                viewModelScope.launch {
                    repository.addCustomFilterRule(rule)
                    webViewManager.reapplyCosmeticRulesNow(tabId, cleanHost)
                    showMessage("Blocked element: $selector")
                }
            }

            override fun onClipboardReadAttempt(tabId: String, host: String) {
                showMessage("Clipboard access guarded on ${host.ifBlank { "this page" }}")
            }

            override fun onFeedDiscovered(tabId: String, feedUrl: String?) {
                updateTab(tabId) { it.copy(discoveredFeedUrl = feedUrl) }
            }

            override fun onPrivacyApiBlocked(tabId: String, host: String, apiName: String) {
                updateTab(tabId) { tab ->
                    val map = tab.privacyReport.apiProbesBlocked.toMutableMap()
                    map[apiName] = (map[apiName] ?: 0) + 1
                    tab.copy(privacyReport = tab.privacyReport.copy(apiProbesBlocked = map))
                }
            }

            override fun onHomographDetected(warning: HomographWarningState) {
                updateTab(warning.tabId) { it.copy(isLoading = false) }
                _homographWarning.value = warning
            }

            override fun onRedirectUnwrapped(tabId: String, wrapperHost: String, unwrappedUrl: String) {
                updateTab(tabId) { tab ->
                    tab.copy(
                        privacyReport = tab.privacyReport.copy(
                            unwrappedRedirects = tab.privacyReport.unwrappedRedirects + 1
                        )
                    )
                }
                showMessage("Link Guard unwrapped tracker via $wrapperHost")
            }

            override fun onHttpsUpgraded(tabId: String, domain: String) {
                updateTab(tabId) { tab ->
                    tab.copy(
                        privacyReport = tab.privacyReport.copy(
                            httpsUpgrades = tab.privacyReport.httpsUpgrades + 1
                        )
                    )
                }
            }

            override fun onWebPermissionRequested(request: WebPermissionPromptRequest) {
                _webPermissionPrompt.value = request
            }

            override fun onAdultContentBlocked(tabId: String, url: String, matched: String) {
                webViewManager.stopLoading(tabId)
                _adultBlockNotice.value = AdultBlockNotice(matched, url)
            }

            override fun onTemporaryGrantsCleared(tabId: String) {
                updateTab(tabId) { it.copy(activeSensorGrants = emptySet()) }
            }
        })
    }

    fun navigateToInput(rawInput: String) {
        navigateToInputOnTab(activeTab.value.id, rawInput)
    }

    fun navigateToInputOnTab(
        tabId: String,
        rawInput: String,
        bypassHomographCheck: Boolean = false
    ) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        // Typed porn/nude queries never leave the device; explain the policy instead.
        AdultContentPolicy.checkInput(trimmed)?.let { verdict ->
            _adultBlockNotice.value = AdultBlockNotice(verdict.matched, trimmed)
            return
        }

        var resolvedUrl = resolveInputToUrl(trimmed)
        val targetTab = _tabs.value.firstOrNull { it.id == tabId } ?: activeTab.value
        var unwrappedCount = 0
        var strippedCount = 0
        if (!looksLikeUrl(trimmed)) {
            if (!targetTab.isPrivate && preferences.value.activeProfile != ProfilePersona.ANONYMOUS) {
                viewModelScope.launch {
                    repository.recordSearchQuery(trimmed)
                }
            }
        } else {
            if (preferences.value.unwrapRedirectLinksEnabled) {
                val unwrap = UrlCleaner.unwrapRedirectUrl(resolvedUrl)
                if (unwrap.wasUnwrapped) {
                    resolvedUrl = unwrap.unwrappedUrl
                    unwrappedCount = 1
                }
            }
            if (preferences.value.stripTrackingParams) {
                val clean = UrlCleaner.cleanUrl(resolvedUrl)
                resolvedUrl = clean.cleanedUrl
                strippedCount = clean.removedCount
            }
            if (preferences.value.homographGuardEnabled && !bypassHomographCheck) {
                val warning = HomographAndPhishingGuard.inspectUrl(resolvedUrl, targetTab.id)
                if (warning != null) {
                    _homographWarning.value = warning
                    return
                }
            }
        }

        if (shieldsEngine.isMaliciousUrl(resolvedUrl)) {
            _maliciousWarning.value = MaliciousWarningState(
                tabId = targetTab.id,
                url = resolvedUrl,
                domain = extractHost(resolvedUrl)
            )
            return
        }

        val cleanPath = runCatching { Uri.parse(resolvedUrl).path?.lowercase() }.getOrNull() ?: ""
        if (UniversalViewerHelper.shouldOpenDirectlyInViewer(resolvedUrl)) {
            openUrlInUniversalViewer(resolvedUrl)
            return
        }
        if (cleanPath.endsWith(".pdf")) {
            openUrlInUniversalViewer(resolvedUrl, "application/pdf")
            return
        }

        val currentId = targetTab.id
        val upgraded = shieldsEngine.maybeUpgradeToHttps(resolvedUrl, null)
        val didUpgradeHttps = upgraded != resolvedUrl
        val targetUrl = if (targetTab.isDesktopMode) webViewManager.normalizeDesktopUrl(upgraded) else upgraded
        val domain = extractHost(targetUrl)
        val zoomPercent = shieldsEngine.getZoomForDomain(domain)
        webViewManager.setDesktopMode(currentId, targetTab.isDesktopMode, reloadIfLoaded = false)
        webViewManager.setTabZoom(currentId, zoomPercent)
        _isToolbarHiddenOnScroll.value = false
        updateTab(currentId) {
            it.copy(
                url = targetUrl,
                domain = domain,
                title = domain.ifBlank { targetUrl },
                isReaderMode = false,
                readerArticle = null,
                isTranslated = false,
                originalUrlBeforeTranslate = "",
                discoveredFeedUrl = null,
                zoomPercent = zoomPercent,
                loadError = null,
                isLoading = true,
                progress = 10,
                sslSecure = targetUrl.startsWith("https://", ignoreCase = true),
                activeSensorGrants = webViewManager.getTemporaryTabPermissions(currentId),
                privacyReport = TabPrivacyReport(
                    unwrappedRedirects = unwrappedCount,
                    strippedParamsCount = strippedCount,
                    httpsUpgrades = if (didUpgradeHttps) 1 else 0
                )
            )
        }
        if (unwrappedCount > 0) {
            showMessage("Link Guard stripped redirect tracker")
        }
        webViewManager.loadUrl(currentId, targetUrl)
    }

    fun openSanctuaryEssay() {
        val id = activeTab.value.id
        val url = "https://en.wikipedia.org/wiki/Architecture"
        updateTab(id) {
            it.copy(
                url = url,
                domain = "en.wikipedia.org",
                title = SanctuaryReaderSamples.ARCHITECTURE_ESSAY.title,
                isReaderMode = true,
                readerArticle = SanctuaryReaderSamples.ARCHITECTURE_ESSAY,
                sslSecure = true,
                isLoading = false,
                progress = 100
            )
        }
    }

    fun openUrlInNewTab(url: String, isPrivate: Boolean = false, isStandalonePwa: Boolean = false) {
        AdultContentPolicy.checkUrl(url)?.let { verdict ->
            _adultBlockNotice.value = AdultBlockNotice(verdict.matched, url)
            return
        }
        val cleaned = if (preferences.value.stripTrackingParams) UrlCleaner.cleanUrl(url).cleanedUrl else url
        val upgraded = shieldsEngine.maybeUpgradeToHttps(cleaned, null)
        val isDesktop = preferences.value.defaultDesktopMode
        val targetUrl = if (isDesktop) webViewManager.normalizeDesktopUrl(upgraded) else upgraded
        val domain = extractHost(targetUrl)
        val zoomPercent = shieldsEngine.getZoomForDomain(domain)
        val newTab = TabState(
            url = targetUrl,
            domain = domain,
            title = domain.ifBlank { "Loading…" },
            isPrivate = isPrivate,
            isDesktopMode = isDesktop,
            zoomPercent = zoomPercent,
            sslSecure = targetUrl.startsWith("https://", ignoreCase = true),
            isLoading = true,
            progress = 10,
            isStandalonePwa = isStandalonePwa
        )
        webViewManager.setDesktopMode(newTab.id, isDesktop, reloadIfLoaded = false)
        webViewManager.setTabZoom(newTab.id, zoomPercent)
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
    }

    fun createNewTab(isPrivate: Boolean = false) {
        val effectivePrivate = isPrivate || preferences.value.activeProfile == ProfilePersona.ANONYMOUS
        val isDesktop = preferences.value.defaultDesktopMode
        val newTab = TabState(
            isPrivate = effectivePrivate,
            title = if (effectivePrivate) "Private Sanctuary" else "New Sanctuary",
            isDesktopMode = isDesktop
        )
        webViewManager.setDesktopMode(newTab.id, isDesktop, reloadIfLoaded = false)
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        _isToolbarHiddenOnScroll.value = false
    }

    fun switchTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            _findInPageActive.value = false
            _isToolbarHiddenOnScroll.value = false
            _elementPickerActiveTabId.value = null
            _consoleLogs.value = emptyList()
            _networkLogs.value = emptyList()
            updateTab(tabId) { it.copy(lastAccessedAt = System.currentTimeMillis()) }
            val currentTabs = _tabs.value
            val backgroundPlaying = currentTabs.firstOrNull { it.isPlayingMedia }
            val switched = currentTabs.firstOrNull { it.id == tabId }
            val sessionTab = backgroundPlaying ?: switched
            if (sessionTab != null && (sessionTab.isPlayingMedia || sessionTab.hasActiveMedia)) {
                activeMediaTabId = sessionTab.id
                mediaSessionHelper.updatePlaybackState(
                    title = sessionTab.title,
                    domain = sessionTab.domain,
                    isPlaying = sessionTab.isPlayingMedia,
                    hasActiveMedia = sessionTab.hasActiveMedia,
                    notificationsEnabled = preferences.value.notificationsEnabled
                )
            } else {
                activeMediaTabId = null
                mediaSessionHelper.release()
            }
        }
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        val closing = currentList.firstOrNull { it.id == tabId } ?: return
        if (_splitTabId.value == tabId) {
            _splitTabId.value = null
        }
        if (!closing.isPrivate && !closing.isHome) {
            if (recentlyClosedStack.size >= 10) recentlyClosedStack.removeFirst()
            recentlyClosedStack.addLast(closing)
            _recentlyClosedCount.value = recentlyClosedStack.size
        }
        webViewManager.closeTab(tabId, closing.isPrivate)
        val remaining = currentList.filterNot { it.id == tabId }
        if (!closing.isHome && closing.domain.isNotBlank() &&
            shieldsEngine.isEphemeralContainerForDomain(closing.domain) &&
            remaining.none { it.domain.equals(closing.domain, ignoreCase = true) }
        ) {
            webViewManager.clearOriginSiteData(closing.domain)
        }
        if (remaining.isEmpty()) {
            val fresh = TabState(isDesktopMode = preferences.value.defaultDesktopMode)
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
        } else {
            _tabs.value = remaining
            if (_activeTabId.value == tabId) {
                _activeTabId.value = remaining.last().id
            }
        }
        if (!closing.isPrivate && !closing.isHome) {
            showMessage("Tab closed")
        }
    }

    fun undoCloseTab() {
        val restored = recentlyClosedStack.removeLastOrNull() ?: return
        _recentlyClosedCount.value = recentlyClosedStack.size
        val revived = restored.copy(
            id = java.util.UUID.randomUUID().toString(),
            lastAccessedAt = System.currentTimeMillis()
        )
        webViewManager.setDesktopMode(revived.id, revived.isDesktopMode, reloadIfLoaded = false)
        webViewManager.setTabZoom(revived.id, revived.zoomPercent)
        _tabs.update { it + revived }
        _activeTabId.value = revived.id
        if (revived.url.isNotBlank()) {
            webViewManager.loadUrl(revived.id, revived.url)
        }
        showMessage("Reopened ${revived.title}")
    }

    fun togglePinTab(tabId: String) {
        val target = _tabs.value.firstOrNull { it.id == tabId } ?: return
        val nextPinned = !target.isPinned
        updateTab(tabId) { it.copy(isPinned = nextPinned) }
        webViewManager.syncPinnedTabs(_tabs.value.filter { it.isPinned }.map { it.id }.toSet())
        showMessage(if (nextPinned) "Tab pinned" else "Tab unpinned")
    }

    fun assignTabGroup(tabId: String, groupName: String?) {
        updateTab(tabId) { it.copy(groupName = groupName) }
        showMessage(if (groupName.isNullOrBlank()) "Removed from group" else "Grouped under $groupName")
    }

    fun closeInactiveTabs(silent: Boolean = false) {
        val days = preferences.value.autoCloseTabsDays.takeIf { it > 0 } ?: 7
        val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        val activeId = _activeTabId.value
        val stale = _tabs.value.filter {
            !it.isPinned && !it.isPrivate && it.id != activeId && it.lastAccessedAt < cutoff
        }
        if (stale.isEmpty()) return
        stale.forEach { tab ->
            if (!tab.isHome) {
                if (recentlyClosedStack.size >= 10) recentlyClosedStack.removeFirst()
                recentlyClosedStack.addLast(tab)
            }
            webViewManager.closeTab(tab.id, false)
        }
        _recentlyClosedCount.value = recentlyClosedStack.size
        val staleIds = stale.map { it.id }.toSet()
        _tabs.update { list -> list.filterNot { it.id in staleIds } }
        if (!silent) showMessage("Closed ${stale.size} inactive tab(s)")
    }

    fun closeAllRegularTabs() {
        val currentList = _tabs.value
        currentList.filter { !it.isPrivate }.forEach {
            webViewManager.closeTab(it.id, false)
        }
        val privateRemaining = currentList.filter { it.isPrivate }
        if (privateRemaining.isEmpty()) {
            val fresh = TabState(isDesktopMode = preferences.value.defaultDesktopMode)
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
        } else {
            _tabs.value = privateRemaining
            _activeTabId.value = privateRemaining.last().id
        }
        showMessage("Closed regular tabs")
    }

    fun goBack(): Boolean {
        val tab = activeTab.value
        if (tab.isReaderMode) {
            updateTab(tab.id) { it.copy(isReaderMode = false) }
            return true
        }
        if (tab.isHome) return false
        return if (webViewManager.goBack(tab.id)) {
            true
        } else {
            goHome()
            true
        }
    }

    fun goForward() {
        val tab = activeTab.value
        if (!tab.isHome) {
            webViewManager.goForward(tab.id)
        }
    }

    fun goHome() {
        val tab = activeTab.value
        _findInPageActive.value = false
        webViewManager.clearFindMatches(tab.id)
        updateTab(tab.id) {
            it.copy(
                url = "",
                domain = "",
                title = if (it.isPrivate) "Private Sanctuary" else "New Sanctuary",
                isReaderMode = false,
                readerArticle = null,
                isLoading = false,
                progress = 0,
                canGoBack = false,
                canGoForward = false,
                loadError = null
            )
        }
    }

    fun reloadOrStop() {
        val tab = activeTab.value
        if (tab.isHome) return
        if (tab.isLoading) {
            webViewManager.stopLoading(tab.id)
            updateTab(tab.id) { it.copy(isLoading = false) }
        } else {
            webViewManager.reload(tab.id)
        }
    }

    fun toggleDesktopMode() {
        val tab = activeTab.value
        val next = !tab.isDesktopMode
        val nextUrl = if (next && !tab.isHome) webViewManager.normalizeDesktopUrl(tab.url) else tab.url
        val nextDomain = if (nextUrl != tab.url) extractHost(nextUrl) else tab.domain
        webViewManager.setDesktopMode(
            tabId = tab.id,
            desktopMode = next,
            reloadIfLoaded = !tab.isHome
        )
        updateTab(tab.id) {
            it.copy(
                isDesktopMode = next,
                isReaderMode = if (next) false else it.isReaderMode,
                url = nextUrl,
                domain = nextDomain
            )
        }
        showMessage(if (next) "Desktop view enabled" else "Mobile view enabled")
    }

    fun toggleReaderMode() {
        val tab = activeTab.value
        if (tab.isHome) {
            openSanctuaryEssay()
            return
        }
        if (tab.isReaderMode) {
            updateTab(tab.id) { it.copy(isReaderMode = false) }
            return
        }
        webViewManager.extractReaderArticle(tab.id, tab.domain) { extracted ->
            val article = extracted ?: if (tab.domain.contains("wikipedia", ignoreCase = true)) {
                SanctuaryReaderSamples.ARCHITECTURE_ESSAY
            } else {
                ReaderArticle(
                    title = tab.title.ifBlank { tab.domain },
                    subtitle = tab.url,
                    category = "READER VIEW • ${tab.domain.uppercase()}",
                    siteName = tab.domain,
                    readTimeMinutes = 3,
                    paragraphs = listOf(
                        "Reader Mode extracted minimal text from ${tab.domain}. Some dynamic web apps render content after scrolling or require JavaScript interaction.",
                        "Tap the Reader toggle again to return to the full interactive webpage."
                    )
                )
            }
            updateTab(tab.id) { it.copy(isReaderMode = true, readerArticle = article) }
        }
    }

    fun adjustReaderFontSize(delta: Int) {
        _readerFontSizeSp.update { (it + delta).coerceIn(13, 24) }
    }

    fun setFindInPageActive(active: Boolean) {
        _findInPageActive.value = active
        if (!active) {
            _findInPageQuery.value = ""
            webViewManager.clearFindMatches(activeTab.value.id)
        }
    }

    fun updateFindInPageQuery(query: String) {
        _findInPageQuery.value = query
        webViewManager.findInPage(activeTab.value.id, query)
    }

    fun findNext(forward: Boolean) {
        webViewManager.findNext(activeTab.value.id, forward)
    }

    fun toggleShieldsForCurrentDomain() {
        val domain = activeTab.value.domain.lowercase().removePrefix("www.")
        if (domain.isBlank()) return
        viewModelScope.launch {
            val current = repository.getWebsiteSettings(domain)
            val nextEnabled = !(current?.shieldsEnabled ?: true)
            repository.upsertWebsiteSettings(
                (current ?: WebsiteSettingsEntity(domain = domain)).copy(
                    shieldsEnabled = nextEnabled,
                    updatedAt = System.currentTimeMillis()
                )
            )
            showMessage(
                if (nextEnabled) "Ritam Shields enabled for $domain"
                else "Ritam Shields paused for $domain"
            )
            webViewManager.reload(activeTab.value.id)
        }
    }

    fun isShieldsEnabledForDomain(domain: String): Boolean {
        if (domain.isBlank()) return true
        return shieldsEngine.isShieldsEnabledForDomain(domain.lowercase().removePrefix("www."))
    }

    fun dismissSslWarning() {
        _sslWarning.value = null
    }

    fun dismissMaliciousWarning() {
        _maliciousWarning.value = null
    }

    fun bookmarkCurrentPage(title: String, folderId: Long = 1L, isPinnedHome: Boolean = false) {
        val tab = activeTab.value
        if (tab.url.isBlank()) return
        viewModelScope.launch {
            repository.addBookmark(
                url = tab.url,
                title = title.ifBlank { tab.title },
                folderId = folderId,
                isPinnedHome = isPinnedHome
            )
            showMessage("Saved to bookmarks")
        }
    }

    fun addCustomBookmark(url: String, title: String, folderId: Long = 1L, isPinnedHome: Boolean = true) {
        val resolved = resolveInputToUrl(url)
        viewModelScope.launch {
            repository.addBookmark(url = resolved, title = title, folderId = folderId, isPinnedHome = isPinnedHome)
            showMessage("Bookmark added")
        }
    }

    fun addBookmarkFolder(name: String) {
        viewModelScope.launch {
            repository.addBookmarkFolder(name)
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch { repository.removeBookmarkById(id) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { repository.deleteHistoryItem(id) }
    }

    fun deleteHistoryItems(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteHistoryItems(ids)
            showMessage("Removed ${ids.size} entries")
        }
    }

    fun clearBrowsingData(clearHistory: Boolean, clearCache: Boolean, clearCookies: Boolean) {
        viewModelScope.launch {
            if (clearHistory) {
                lastRecordedVisitByTab.clear()
                repository.clearHistory()
                repository.clearSearchHistory()
            }
            if (clearCache) {
                RitamCacheManager.clearAppCache(getApplication())
            }
            webViewManager.clearAllWebData(clearCache = clearCache, clearCookies = clearCookies)
            refreshCacheStats()
            showMessage("Browsing data cleared")
        }
    }

    fun recordQuickControlUse(controlId: String) {
        viewModelScope.launch {
            repository.recordQuickControlUse(controlId)
        }
    }

    fun refreshCacheStats() {
        viewModelScope.launch {
            _cacheStats.value = withContext(Dispatchers.IO) {
                RitamCacheManager.computeCacheStats(getApplication())
            }
        }
    }

    fun clearAppCacheOnly() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                RitamCacheManager.clearAppCache(getApplication())
            }
            webViewManager.clearAllWebData(clearCache = true, clearCookies = false)
            refreshCacheStats()
            showMessage("Application cache cleared")
        }
    }

    fun clearSiteUsageAnalytics() {
        viewModelScope.launch {
            repository.clearSiteUsage()
            showMessage("Top sites analytics reset")
        }
    }

    fun shareCurrentPage(context: Context) {
        val tab = activeTab.value
        val shareText = if (tab.url.isBlank()) "Explore Ritam Browser — Fast, calm, private browsing." else "${tab.title}\n${tab.url}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "Copied",
        isSensitive: Boolean = false
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        clipboardClearJob?.cancel()
        val delaySec = preferences.value.clipboardAutoClearSeconds
        if (isSensitive && delaySec > 0) {
            showMessage("Copied (auto-clears in ${delaySec}s)")
            clipboardClearJob = viewModelScope.launch {
                delay(delaySec * 1000L)
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    } else {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            }
        } else {
            showMessage("Copied to clipboard")
        }
    }

    fun captureAndShareScreenshot(rootView: View, context: Context) {
        viewModelScope.launch(Dispatchers.Main) {
            runCatching {
                val bitmap = Bitmap.createBitmap(
                    rootView.width.coerceAtLeast(1),
                    rootView.height.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                rootView.draw(canvas)
                val file = File(context.cacheDir, "ritam_capture_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share Page Screenshot").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }.onFailure {
                showMessage("Unable to capture screen")
            }
        }
    }

    fun exportBookmarksToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val html = repository.exportBookmarksAsNetscapeHtml()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(html.toByteArray(Charsets.UTF_8))
                }
                showMessage("Bookmarks exported as Netscape HTML")
            }.onFailure {
                showMessage("Export failed")
            }
        }
    }

    fun importBookmarksFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                if (content.isBlank()) {
                    showMessage("That file is empty or couldn't be read")
                    return@launch
                }
                val summary = repository.importBrowserData(content)
                showMessage(
                    if (summary.bookmarksAdded == 0) {
                        "No bookmarks found. Choose a bookmarks HTML or JSON export from your other browser."
                    } else {
                        summary.toMessage()
                    }
                )
            }.onFailure {
                showMessage("Import failed. Make sure the file is a browser bookmarks export.")
            }
        }
    }

    fun exportHistoryToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val md = repository.exportHistoryAsMarkdown()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(md.toByteArray(Charsets.UTF_8))
                }
                showMessage("History exported as Markdown")
            }.onFailure {
                showMessage("History export failed")
            }
        }
    }

    fun pauseDownload(download: DownloadEntity) {
        downloadManager.pauseDownload(download)
        showMessage("Paused ${download.filename}")
    }

    fun resumeDownload(download: DownloadEntity) {
        downloadManager.resumeDownload(download)
        showMessage("Resuming ${download.filename}")
    }

    fun cancelDownload(download: DownloadEntity) {
        downloadManager.cancelDownload(download)
    }

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch { repository.deleteDownload(download.id) }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch { repository.clearCompletedDownloads() }
    }

    fun openDownload(download: DownloadEntity) {
        val file = File(download.filePath)
        val supported = UniversalViewerHelper.isSupportedFilename(download.filename, download.mimeType)
        val type = UniversalViewerHelper.classifyType(download.filename, download.mimeType)
        if (supported && file.exists()) {
            _universalViewerState.value = UniversalViewerState(
                title = download.filename,
                sourceUrl = download.url,
                localFile = file,
                fileType = type,
                mimeType = download.mimeType,
                isLoading = true
            )
            viewModelScope.launch {
                _universalViewerState.value = UniversalViewerHelper.openLocalFile(
                    context = getApplication(),
                    file = file,
                    displayTitle = download.filename,
                    mimeType = download.mimeType
                )
            }
            return
        }
        if (!downloadManager.openDownloadedFile(download)) {
            showMessage("No app available to open ${download.filename}")
        }
    }

    fun openUriInUniversalViewer(uri: Uri) {
        _universalViewerState.value = UniversalViewerState(
            title = "Opening file…",
            sourceUrl = uri.toString(),
            fileType = UniversalFileType.TEXT,
            isLoading = true
        )
        viewModelScope.launch {
            _universalViewerState.value = UniversalViewerHelper.openUri(getApplication(), uri)
        }
    }

    fun openUrlInUniversalViewer(url: String, mimeTypeHint: String = "") {
        val title = runCatching { Uri.parse(url).lastPathSegment }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: "Document"
        _universalViewerState.value = UniversalViewerState(
            title = title,
            sourceUrl = url,
            fileType = UniversalViewerHelper.classifyType(title, mimeTypeHint),
            mimeType = mimeTypeHint,
            isLoading = true
        )
        viewModelScope.launch {
            _universalViewerState.value = UniversalViewerHelper.openRemoteUrl(
                context = getApplication(),
                url = url,
                mimeTypeHint = mimeTypeHint
            )
            refreshCacheStats()
        }
    }

    fun openFileExternally(context: Context, file: File?, contentUri: Uri?, mimeType: String) {
        runCatching {
            val targetUri = when {
                file != null && file.exists() -> FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                contentUri != null -> contentUri
                else -> return
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            showMessage("No external app available for this file")
        }
    }

    fun dismissUniversalViewer() {
        _universalViewerState.value = null
    }

    fun shareDownload(download: DownloadEntity) {
        downloadManager.shareDownloadedFile(download)
    }

    fun refreshStorageStats() {
        _storageStats.value = downloadManager.getDeviceStorageStats()
        refreshCacheStats()
    }

    // Preference & Shields Updates
    fun updateThemePalette(palette: ThemePalette) = viewModelScope.launch {
        preferencesRepository.updateThemePalette(palette)
    }

    fun toggleDarkModeQuick() = viewModelScope.launch {
        val current = preferences.value.themePalette
        val next = if (current == ThemePalette.ONYX_FOREST) ThemePalette.SERENE_SAND else ThemePalette.ONYX_FOREST
        preferencesRepository.updateThemePalette(next)
    }

    fun updateAccentTone(tone: AccentTone) = viewModelScope.launch { preferencesRepository.updateAccentTone(tone) }
    fun updateOledPureBlack(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateOledPureBlack(enabled) }
    fun updateSurfaceBlur(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateSurfaceBlur(enabled) }
    fun updateFontPairing(pairing: FontPairing) = viewModelScope.launch { preferencesRepository.updateFontPairing(pairing) }
    fun updateFontScalePercent(percent: Int) = viewModelScope.launch { preferencesRepository.updateFontScalePercent(percent) }
    fun updateFluidTypography(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateFluidTypography(enabled) }
    fun updateCornerTopology(topology: CornerTopology) = viewModelScope.launch { preferencesRepository.updateCornerTopology(topology) }
    fun updateOmniboxTopology(topology: OmniboxTopology) = viewModelScope.launch { preferencesRepository.updateOmniboxTopology(topology) }
    fun updateLayoutDensity(density: LayoutDensity) = viewModelScope.launch { preferencesRepository.updateLayoutDensity(density) }
    fun updateSearchEngine(engine: SearchEngine) = viewModelScope.launch { preferencesRepository.updateSearchEngine(engine) }
    fun updateHomepageUrl(url: String) = viewModelScope.launch { preferencesRepository.updateHomepageUrl(url) }
    fun updateAdBlocking(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateAdBlocking(enabled) }
    fun updateTrackerBlocking(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateTrackerBlocking(enabled) }
    fun updateMaliciousProtection(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateMaliciousProtection(enabled) }
    fun updateHttpsOnly(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateHttpsOnly(enabled) }
    fun updateJavascriptEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateJavascriptEnabled(enabled) }
    fun updateBlockThirdPartyCookies(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateBlockThirdPartyCookies(enabled) }
    fun updateDefaultDesktopMode(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.updateDefaultDesktopMode(enabled)
        val current = activeTab.value
        if (current.isHome) {
            webViewManager.setDesktopMode(current.id, enabled, reloadIfLoaded = false)
            updateTab(current.id) { it.copy(isDesktopMode = enabled) }
        }
    }
    fun updateAskWhereToSave(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateAskWhereToSave(enabled) }
    fun updateLocalTelemetry(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateLocalTelemetry(enabled) }
    fun updateBiometricVaultLock(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateBiometricVaultLock(enabled) }
    fun updateAutofillEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateAutofillEnabled(enabled) }
    fun updateNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateNotificationsEnabled(enabled) }
    fun updateDownloadNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateDownloadNotificationsEnabled(enabled) }
    fun updateShieldAlertNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateShieldAlertNotificationsEnabled(enabled) }
    fun updateBlockSiteNotificationPrompts(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateBlockSiteNotificationPrompts(enabled) }
    fun updateGestureSwipeNav(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateGestureSwipeNav(enabled) }
    fun updateGestureSwipeTabs(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateGestureSwipeTabs(enabled) }
    fun updateGesturePullRefresh(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateGesturePullRefresh(enabled) }
    fun updateGestureDoubleTapTop(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateGestureDoubleTapTop(enabled) }
    fun updateCosmeticAdBlocking(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateCosmeticAdBlocking(enabled) }
    fun updateForceWebDarkMode(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.updateForceWebDarkMode(enabled)
        shieldsEngine.updatePreferences(preferences.value.copy(forceWebDarkMode = enabled))
        webViewManager.applyForceDarkToAllOpenTabs(_tabs.value)
    }
    fun updateBiometricIncognitoLock(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.updateBiometricIncognitoLock(enabled)
        _isIncognitoLocked.value = enabled
    }
    fun updateBottomOmnibox(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateBottomOmnibox(enabled) }
    fun updateBlockCookieBanners(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateBlockCookieBanners(enabled) }
    fun updateStripTrackingParams(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateStripTrackingParams(enabled) }
    fun updateAutoHideToolbarsOnScroll(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.updateAutoHideToolbarsOnScroll(enabled)
        if (!enabled) _isToolbarHiddenOnScroll.value = false
    }
    fun updateClearOnExit(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateClearOnExit(enabled) }
    fun updateDnsProvider(provider: DnsProvider) = viewModelScope.launch {
        preferencesRepository.updateDnsProvider(provider)
        dohResolver.clearCache()
    }
    fun updateCustomDohUrl(url: String) = viewModelScope.launch {
        preferencesRepository.updateCustomDohUrl(url)
        dohResolver.clearCache()
    }
    fun updateConfirmExternalAppLaunch(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.updateConfirmExternalAppLaunch(enabled)
    }
    fun updateAutoCloseTabsDays(days: Int) = viewModelScope.launch {
        preferencesRepository.updateAutoCloseTabsDays(days)
    }
    fun updatePreferredTranslateLanguage(langCode: String) = viewModelScope.launch {
        preferencesRepository.updatePreferredTranslateLanguage(langCode)
    }
    fun updateActiveProfile(profile: ProfilePersona) = viewModelScope.launch {
        preferencesRepository.updateActiveProfile(profile)
        showMessage("Switched to ${profile.displayName} profile")
    }
    fun updateFingerprintRandomizer(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateFingerprintRandomizer(enabled) }
    fun updateEphemeralContainerDefault(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateEphemeralContainerDefault(enabled) }
    fun updateClipboardAutoClearSeconds(seconds: Int) = viewModelScope.launch { preferencesRepository.updateClipboardAutoClearSeconds(seconds) }
    fun updateBionicReading(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateBionicReading(enabled) }
    fun updateReaderColumnWidthDp(dp: Int) = viewModelScope.launch { preferencesRepository.updateReaderColumnWidthDp(dp) }
    fun updateReaderLineHeightMultiplier(mult: Float) = viewModelScope.launch { preferencesRepository.updateReaderLineHeightMultiplier(mult) }
    fun updateUserscriptsEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateUserscriptsEnabled(enabled) }
    fun updateScreenSecurityPrivate(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateScreenSecurityPrivate(enabled) }
    fun updateHomographGuard(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateHomographGuard(enabled) }
    fun updateUnwrapRedirectLinks(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateUnwrapRedirectLinks(enabled) }
    fun updateStrictReferrerPolicy(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateStrictReferrerPolicy(enabled) }
    fun updateBlockWebRtcLeaks(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateBlockWebRtcLeaks(enabled) }
    fun updateLockdownCovertApis(enabled: Boolean) = viewModelScope.launch { preferencesRepository.updateLockdownCovertApis(enabled) }

    fun resetAppearanceDefaults() = viewModelScope.launch {
        preferencesRepository.resetAppearanceDefaults()
        showMessage("Appearance restored to defaults")
    }

    // --- Biometric Lock, Toolbar Reveal, Media & Burn Session ---
    fun markIncognitoUnlocked() {
        _isIncognitoLocked.value = false
    }

    fun markVaultUnlocked() {
        _isVaultUnlocked.value = true
    }

    fun relockSensitiveAreas() {
        if (preferences.value.biometricIncognitoLock) {
            _isIncognitoLocked.value = true
        }
        _isVaultUnlocked.value = false
    }

    fun revealToolbars() {
        _isToolbarHiddenOnScroll.value = false
    }

    fun toggleTabMediaPlayback(targetTabId: String = activeTab.value.id) {
        val tab = _tabs.value.firstOrNull { it.id == targetTabId } ?: return
        if (!tab.isHome) {
            val nextPlaying = !tab.isPlayingMedia
            webViewManager.toggleMediaPlayback(tab.id)
            updateTab(tab.id) { it.copy(isPlayingMedia = nextPlaying) }
            activeMediaTabId = tab.id
            mediaSessionHelper.updatePlaybackState(
                title = tab.title,
                domain = tab.domain,
                isPlaying = nextPlaying,
                hasActiveMedia = tab.hasActiveMedia || nextPlaying,
                notificationsEnabled = preferences.value.notificationsEnabled
            )
        }
    }

    fun onEnteringPipMode() {
        val tab = activeTab.value
        if (!tab.isHome) {
            webViewManager.onEnteringPipMode(tab.id)
        }
    }

    fun onEnteredPipMode(active: Boolean) {
        val tab = activeTab.value
        if (!tab.isHome) {
            webViewManager.setPipVideoFocusMode(tab.id, active)
        }
    }

    fun setTabMediaSpeedAndBoost(speed: Float, boost: Float) {
        val tab = activeTab.value
        if (tab.isHome) return
        val clampedSpeed = speed.coerceIn(0.5f, 3.0f)
        val clampedBoost = boost.coerceIn(1.0f, 2.5f)
        updateTab(tab.id) { it.copy(mediaSpeed = clampedSpeed, audioBoost = clampedBoost) }
        webViewManager.setMediaSpeedAndBoost(tab.id, clampedSpeed, clampedBoost)
        showMessage("Media: ${String.format("%.2gx", clampedSpeed)} • Boost ${String.format("%.1fx", clampedBoost)}")
    }

    fun exitFullscreenVideo() {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        _fullscreenVideoView.value = null
    }

    fun burnSession() {
        viewModelScope.launch {
            _isBurningSession.value = true
            delay(700L)
            readAloudController.stop()
            webViewManager.pauseAllMedia()
            webViewManager.clearBypassedHomographHosts()
            _tabs.value.forEach { webViewManager.closeTab(it.id, true) }
            recentlyClosedStack.clear()
            _recentlyClosedCount.value = 0
            lastRecordedVisitByTab.clear()
            repository.clearHistory()
            repository.clearSearchHistory()
            RitamCacheManager.clearAppCache(getApplication())
            webViewManager.clearAllWebData(clearCache = true, clearCookies = true)
            refreshCacheStats()
            val fresh = TabState(isDesktopMode = preferences.value.defaultDesktopMode)
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
            _consoleLogs.value = emptyList()
            _networkLogs.value = emptyList()
            _isBurningSession.value = false
            showMessage("Session burned cleanly")
        }
    }

    fun wipeOnExitIfEnabled() {
        if (!preferences.value.clearOnExit) return
        viewModelScope.launch {
            repository.clearHistory()
            repository.clearSearchHistory()
            RitamCacheManager.clearAppCache(getApplication())
            webViewManager.clearAllWebData(clearCache = true, clearCookies = true)
        }
    }

    // --- Page Zoom ---
    fun setTabZoom(zoomPercent: Int) {
        val tab = activeTab.value
        val clamped = zoomPercent.coerceIn(50, 200)
        webViewManager.setTabZoom(tab.id, clamped)
        updateTab(tab.id) { it.copy(zoomPercent = clamped) }
        val cleanDomain = tab.domain.lowercase().removePrefix("www.")
        if (cleanDomain.isNotBlank()) {
            viewModelScope.launch {
                val existing = repository.getWebsiteSettings(cleanDomain)
                repository.upsertWebsiteSettings(
                    (existing ?: WebsiteSettingsEntity(domain = cleanDomain)).copy(
                        zoomPercent = clamped,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // --- Built-in PDF Viewer ---
    fun openPdfFromUrl(url: String) {
        val title = runCatching { Uri.parse(url).lastPathSegment }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: "Document.pdf"
        _pdfViewerState.value = PdfViewerState(title = title, sourceUrl = url, isLoading = true)
        viewModelScope.launch {
            val file = PdfDocumentHelper.downloadRemotePdfToCache(getApplication(), url, title)
            if (file == null) {
                _pdfViewerState.value = PdfViewerState(
                    title = title,
                    sourceUrl = url,
                    isLoading = false,
                    error = "Could not download PDF from $url"
                )
            } else {
                val count = PdfDocumentHelper.getPageCount(file)
                _pdfViewerState.value = PdfViewerState(
                    title = title,
                    sourceUrl = url,
                    localFile = file,
                    pageCount = count,
                    isLoading = false,
                    error = if (count <= 0) "Invalid or password-protected PDF" else null
                )
            }
        }
    }

    fun openPdfFromFile(file: File, title: String) {
        _pdfViewerState.value = PdfViewerState(title = title, localFile = file, isLoading = true)
        viewModelScope.launch {
            val count = PdfDocumentHelper.getPageCount(file)
            _pdfViewerState.value = PdfViewerState(
                title = title,
                localFile = file,
                pageCount = count,
                isLoading = false,
                error = if (count <= 0) "Unable to parse PDF pages" else null
            )
        }
    }

    fun openPdfExternally(context: Context, file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            showMessage("No external PDF viewer available")
        }
    }

    fun dismissPdfViewer() {
        _pdfViewerState.value = null
    }

    // --- Translator ---
    fun openTranslateDialog() {
        val tab = activeTab.value
        _translateExcerpt.value = tab.title
        _translateDialogOpen.value = true
        if (!tab.isHome) {
            webViewManager.extractPageTextForTranslation(tab.id) { text ->
                if (text.isNotBlank()) _translateExcerpt.value = text
            }
        }
    }

    fun dismissTranslateDialog() {
        _translateDialogOpen.value = false
    }

    fun translateCurrentPage(langCode: String) {
        val tab = activeTab.value
        val sourceUrl = tab.originalUrlBeforeTranslate.ifBlank { tab.url }
        if (sourceUrl.isBlank()) return
        val translatedUrl = TranslationHelper.buildGoogleTranslateWebUrl(sourceUrl, langCode)
        updateTab(tab.id) {
            it.copy(
                isTranslated = true,
                originalUrlBeforeTranslate = sourceUrl,
                url = translatedUrl
            )
        }
        webViewManager.loadUrl(tab.id, translatedUrl)
    }

    fun restoreOriginalPage() {
        val tab = activeTab.value
        val orig = tab.originalUrlBeforeTranslate
        if (orig.isBlank()) return
        updateTab(tab.id) {
            it.copy(isTranslated = false, originalUrlBeforeTranslate = "", url = orig)
        }
        webViewManager.loadUrl(tab.id, orig)
    }

    // --- Reading List ("Read Later") ---
    fun addCurrentPageToReadingList() {
        val tab = activeTab.value
        if (tab.url.isBlank()) {
            showMessage("Open an article or webpage first")
            return
        }
        viewModelScope.launch {
            val clean = if (preferences.value.stripTrackingParams) UrlCleaner.cleanUrl(tab.url).cleanedUrl else tab.url
            repository.addToReadingList(
                url = clean,
                title = tab.title,
                domain = tab.domain,
                excerpt = tab.snippet
            )
            showMessage("Added to Reading List")
        }
    }

    fun addUrlToReadingList(url: String) {
        viewModelScope.launch {
            val clean = if (preferences.value.stripTrackingParams) UrlCleaner.cleanUrl(url).cleanedUrl else url
            val host = extractHost(clean)
            repository.addToReadingList(url = clean, title = host.ifBlank { clean }, domain = host)
            showMessage("Saved to Reading List")
        }
    }

    fun setReadingListRead(id: Long, isRead: Boolean) = viewModelScope.launch {
        repository.setReadingListRead(id, isRead)
    }

    fun deleteReadingListItem(id: Long) = viewModelScope.launch {
        repository.deleteReadingListItem(id)
    }

    fun clearReadReadingList() = viewModelScope.launch {
        repository.clearReadReadingList()
    }

    // --- QR Code & Context Menu & External App Guard ---
    fun showQrForCurrentOrUrl(url: String? = null) {
        val raw = url?.takeIf { it.isNotBlank() } ?: activeTab.value.url.ifBlank { "https://ritam.browser" }
        _qrShareUrl.value = raw
    }

    fun dismissQrDialog() {
        _qrShareUrl.value = null
    }

    fun dismissLongPressContext() {
        _longPressContext.value = null
    }

    fun confirmAllowExternalApp() {
        val req = _externalAppRequest.value ?: return
        _externalAppRequest.value = null
        webViewManager.launchExternalUri(req.targetUri, req.fallbackWebUrl)
    }

    fun loadFallbackWebUrl(url: String) {
        _externalAppRequest.value = null
        navigateToInput(url)
    }

    fun dismissExternalAppRequest() {
        _externalAppRequest.value = null
    }

    // --- Unified CDP Developer Inspector (Elements, Console, Network, Format, Storage/Cache) ---
    fun openDevTools(initialTab: Int = 1) {
        _devToolsInitialTab.value = initialTab.coerceIn(0, 4)
        _devToolsVisible.value = true
        refreshDevStorage()
        refreshCacheStats()
        if (initialTab == 0 || _devToolsPageSource.value.isBlank()) {
            refreshDevSource()
        }
        if (initialTab == 3) {
            refreshDevFormat()
        }
    }

    fun dismissDevTools() {
        _devToolsVisible.value = false
    }

    fun runDevJs(script: String) {
        val tab = activeTab.value
        webViewManager.evaluateDevJs(tab.id, script) { res ->
            _devEvalOutput.value = res
        }
    }

    fun refreshDevSource() {
        val tab = activeTab.value
        if (tab.isHome && tab.readerArticle == null) return
        webViewManager.extractPageSource(tab) { html ->
            if (html.isNotBlank()) {
                _devToolsPageSource.value = html
            }
        }
    }

    fun refreshDevFormat() {
        val tab = activeTab.value
        if (tab.url.isBlank() || tab.isHome) return
        _cdpFormatState.value = StructuredViewerState(
            title = tab.title.ifBlank { tab.domain },
            url = tab.url,
            docType = StructuredDocType.JSON,
            formattedText = "",
            rawText = "",
            isLoading = true
        )
        viewModelScope.launch {
            StructuredViewerHelper.fetchStructuredDocument(tab.url, getApplication())
                .onSuccess { state ->
                    _cdpFormatState.value = state
                    refreshCacheStats()
                }
                .onFailure {
                    webViewManager.extractPageTextForTranslation(tab.id) { text ->
                        val detected = StructuredViewerHelper.detectDocType(tab.url, text)
                        val formatted = if (detected == StructuredDocType.JSON) {
                            StructuredViewerHelper.formatPrettyJson(text) ?: text
                        } else text
                        _cdpFormatState.value = StructuredViewerState(
                            title = tab.title.ifBlank { tab.domain },
                            url = tab.url,
                            docType = detected,
                            formattedText = formatted,
                            rawText = text,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun refreshDevStorage() {
        val tab = activeTab.value
        webViewManager.inspectSiteStorage(tab.id, tab.url) { cookies, pairs ->
            _cookiesDump.value = cookies
            _localStoragePairs.value = pairs
        }
        refreshCacheStats()
    }

    fun clearDevLogs() {
        _consoleLogs.value = emptyList()
        _networkLogs.value = emptyList()
        _devEvalOutput.value = ""
    }

    // --- Encrypted DNS (DoH) Live Test ---
    fun runDohLatencyTest(hostname: String = "wikipedia.org") {
        viewModelScope.launch {
            val prefs = preferences.value
            val result = dohResolver.resolveHost(hostname, prefs.dnsProvider, prefs.customDohUrl)
            _dohTestResult.value = result
        }
    }

    fun printCurrentPage(activityContext: Context) {
        val tab = activeTab.value
        if (tab.isHome && tab.readerArticle == null) {
            showMessage("Open a webpage or article to print")
            return
        }
        val started = webViewManager.printCurrentPage(activityContext, tab)
        if (!started) {
            showMessage("Unable to open print preview for this page")
        }
    }

    fun openPageSource() {
        val tab = activeTab.value
        if (tab.isHome && tab.readerArticle == null) {
            showMessage("Open a webpage to inspect source")
            return
        }
        openDevTools(initialTab = 0)
    }

    fun dismissPageSource() {
        _pageSourceState.value = null
    }

    fun saveCurrentPageOffline() {
        val tab = activeTab.value
        if (tab.isHome && tab.readerArticle == null) {
            showMessage("Open a webpage to save offline")
            return
        }
        webViewManager.saveCurrentPage(tab) { filePath, filename, mimeType, sizeBytes ->
            if (filePath.isNullOrBlank()) {
                showMessage("Unable to save webpage offline")
            } else {
                val notify = preferences.value.notificationsEnabled && preferences.value.downloadNotificationsEnabled
                downloadManager.recordSavedPage(
                    url = tab.url.ifBlank { "ritam://saved/$filename" },
                    filename = filename,
                    filePath = filePath,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    notify = notify
                )
                refreshStorageStats()
                showMessage("Saved $filename to Downloads")
            }
        }
    }

    fun startReadAloud() {
        val tab = activeTab.value
        val existingArticle = tab.readerArticle
        if (existingArticle != null) {
            readAloudController.startReading(existingArticle.title, existingArticle.paragraphs)
            return
        }
        if (tab.isHome) {
            val essay = SanctuaryReaderSamples.ARCHITECTURE_ESSAY
            readAloudController.startReading(essay.title, essay.paragraphs)
            return
        }
        webViewManager.extractReaderArticle(tab.id, tab.domain) { extracted ->
            if (extracted != null && extracted.paragraphs.isNotEmpty()) {
                readAloudController.startReading(extracted.title, extracted.paragraphs)
            } else {
                readAloudController.startReading(
                    tab.title.ifBlank { tab.domain },
                    listOf("Reading ${tab.title.ifBlank { tab.domain }}. Unable to extract additional body paragraphs from this dynamic site.")
                )
            }
        }
    }

    fun toggleReadAloudPlayPause() = readAloudController.togglePlayPause()
    fun nextReadAloudParagraph() = readAloudController.nextParagraph()
    fun prevReadAloudParagraph() = readAloudController.prevParagraph()
    fun cycleReadAloudSpeed() = readAloudController.cycleSpeed()
    fun stopReadAloud() = readAloudController.stop()

    fun switchToAdjacentTab(delta: Int) {
        val list = _tabs.value
        if (list.size <= 1) return
        val currentIdx = list.indexOfFirst { it.id == _activeTabId.value }.coerceAtLeast(0)
        val nextIdx = (currentIdx + delta).mod(list.size)
        switchTab(list[nextIdx].id)
        showMessage("Tab ${nextIdx + 1} of ${list.size}")
    }

    fun scrollActivePageToTop() {
        val tab = activeTab.value
        if (!tab.isHome && !tab.isReaderMode) {
            webViewManager.scrollToTop(tab.id)
        }
    }

    fun pinHomeWidget(context: Context) {
        val pinned = RitamSearchWidgetProvider.requestPinWidget(context)
        if (pinned) {
            showMessage("Widget pin request sent to launcher")
        } else {
            showMessage("Long-press your Home Screen and choose Widgets → Ritam")
        }
    }

    fun sendTestNotification() {
        val sent = notificationManager.notifyShieldAlert(
            title = "Ritam Shields & Notifications Active",
            body = "Ad-Blocker, offline saves, and download alerts are ready."
        )
        if (sent) {
            showMessage("Test notification posted")
        } else {
            showMessage("Enable notification permission to receive alerts")
        }
    }

    // --- 19-Feature Power Capabilities ---

    fun setCommandPaletteOpen(open: Boolean) {
        _commandPaletteOpen.value = open
    }

    fun toggleSplitScreen(secondTabId: String? = null) {
        val activeId = _activeTabId.value
        if (_splitTabId.value != null && secondTabId == null) {
            _splitTabId.value = null
            showMessage("Closed split view")
            return
        }
        val candidate = if (secondTabId != null && secondTabId != activeId) {
            _tabs.value.firstOrNull { it.id == secondTabId }
        } else {
            _tabs.value.firstOrNull { it.id != activeId && !it.isPrivate }
                ?: _tabs.value.firstOrNull { it.id != activeId }
        }
        if (candidate != null) {
            _splitTabId.value = candidate.id
            showMessage("Parallel Split View: ${candidate.title}")
        } else {
            val activeTabVal = activeTab.value
            val initialUrl = if (activeTabVal.url.isNotBlank() && !activeTabVal.isHome) activeTabVal.url else "https://duckduckgo.com"
            val second = TabState(
                url = initialUrl,
                domain = extractHost(initialUrl),
                title = extractHost(initialUrl).ifBlank { "Parallel Tab" },
                isDesktopMode = preferences.value.defaultDesktopMode,
                sslSecure = initialUrl.startsWith("https://", ignoreCase = true),
                isLoading = true,
                progress = 10
            )
            webViewManager.setDesktopMode(second.id, second.isDesktopMode, reloadIfLoaded = false)
            webViewManager.setTabZoom(second.id, shieldsEngine.getZoomForDomain(second.domain))
            _tabs.update { it + second }
            _splitTabId.value = second.id
            webViewManager.loadUrl(second.id, initialUrl)
            showMessage("Opened Parallel Split View")
        }
    }

    fun swapSplitPanes() {
        val activeId = _activeTabId.value
        val secondaryId = _splitTabId.value ?: return
        _activeTabId.value = secondaryId
        _splitTabId.value = activeId
        showMessage("Swapped split panes")
    }

    fun closeSplitPane() {
        _splitTabId.value = null
    }

    fun toggleElementPicker() {
        val tab = activeTab.value
        if (tab.isHome || tab.isReaderMode) {
            showMessage("Open a webpage to pick an element to block")
            return
        }
        if (_elementPickerActiveTabId.value == tab.id) {
            _elementPickerActiveTabId.value = null
            webViewManager.reload(tab.id)
        } else {
            _elementPickerActiveTabId.value = tab.id
            webViewManager.enableElementPicker(tab.id)
            showMessage("Tap any ad, banner, or popup to hide it")
        }
    }

    fun captureFullPageScreenshot(fallbackRootView: View, context: Context) {
        val tab = activeTab.value
        if (tab.isHome || tab.isReaderMode) {
            captureAndShareScreenshot(fallbackRootView, context)
            return
        }
        webViewManager.captureFullPageBitmap(tab.id) { fullBmp ->
            if (fullBmp == null) {
                captureAndShareScreenshot(fallbackRootView, context)
                return@captureFullPageBitmap
            }
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val file = File(context.cacheDir, "ritam_fullpage_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        fullBmp.compress(Bitmap.CompressFormat.PNG, 92, out)
                    }
                    fullBmp.recycle()
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    withContext(Dispatchers.Main) {
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share Full-Page Screenshot").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        showMessage("Full-page capture exceeded memory; sharing viewport instead")
                        captureAndShareScreenshot(fallbackRootView, context)
                    }
                }
            }
        }
    }

    fun setUserscriptsDialogOpen(open: Boolean) {
        _userscriptsDialogOpen.value = open
    }

    fun addUserscript(name: String, urlPattern: String, code: String, isCss: Boolean) {
        viewModelScope.launch {
            repository.addUserscript(name, urlPattern, code, isCss)
            showMessage("Userscript \"$name\" saved")
        }
    }

    fun setUserscriptEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setUserscriptEnabled(id, enabled)
    }

    fun deleteUserscript(id: Long) = viewModelScope.launch {
        repository.deleteUserscript(id)
    }

    fun setWatchlistDialogOpen(open: Boolean) {
        _watchlistDialogOpen.value = open
    }

    fun addCurrentPageToWatchlist() {
        val tab = activeTab.value
        if (tab.url.isBlank() || tab.isHome) {
            showMessage("Open a webpage first to monitor changes")
            return
        }
        viewModelScope.launch {
            showMessage("Snapshotting ${tab.domain}...")
            val hash = WatchlistAndSyncHelper.computePageContentHash(tab.url).getOrElse {
                WatchlistAndSyncHelper.sha256Hex(tab.title + tab.url)
            }
            repository.upsertWatchlistItem(
                WatchlistItemEntity(
                    url = tab.url,
                    domain = tab.domain,
                    title = tab.title.ifBlank { tab.domain },
                    lastContentHash = hash,
                    lastCheckedAt = System.currentTimeMillis(),
                    hasUnreadChange = false
                )
            )
            showMessage("Added ${tab.domain} to Watchlist")
        }
    }

    fun checkWatchlistNow() {
        viewModelScope.launch {
            val items = repository.getWatchlistOnce()
            if (items.isEmpty()) {
                showMessage("Watchlist is empty")
                return@launch
            }
            var changedCount = 0
            items.forEach { item ->
                val result = WatchlistAndSyncHelper.computePageContentHash(item.url)
                result.onSuccess { newHash ->
                    val changed = item.lastContentHash.isNotBlank() && newHash != item.lastContentHash
                    if (changed) {
                        changedCount++
                        if (preferences.value.notificationsEnabled) {
                            notificationManager.notifyWatchlistChanged(item.title, item.domain)
                        }
                    }
                    repository.upsertWatchlistItem(
                        item.copy(
                            lastContentHash = newHash,
                            lastCheckedAt = System.currentTimeMillis(),
                            hasUnreadChange = item.hasUnreadChange || changed
                        )
                    )
                }
            }
            showMessage(
                if (changedCount > 0) "$changedCount watched page(s) updated!"
                else "All ${items.size} watched pages unchanged"
            )
        }
    }

    fun markWatchlistSeen(id: Long) = viewModelScope.launch {
        repository.markWatchlistSeen(id)
    }

    fun deleteWatchlistItem(id: Long) = viewModelScope.launch {
        repository.deleteWatchlistItem(id)
    }

    fun openHighlightCapture() {
        val tab = activeTab.value
        if (tab.isHome && tab.readerArticle == null) {
            showMessage("Open an article or webpage to highlight text")
            return
        }
        if (tab.isReaderMode && tab.readerArticle != null) {
            _pendingHighlightRequest.value = HighlightSelectionRequest(
                tabId = tab.id,
                url = tab.url.ifBlank { "ritam://sanctuary-essay" },
                domain = tab.domain.ifBlank { "ritam.local" },
                title = tab.readerArticle.title,
                quoteText = tab.readerArticle.quote ?: tab.readerArticle.paragraphs.firstOrNull().orEmpty().take(500)
            )
            return
        }
        webViewManager.extractSelectedText(tab.id) { selected ->
            _pendingHighlightRequest.value = HighlightSelectionRequest(
                tabId = tab.id,
                url = tab.url,
                domain = tab.domain,
                title = tab.title.ifBlank { tab.domain },
                quoteText = selected.ifBlank { tab.title }
            )
        }
    }

    fun dismissHighlightCapture() {
        _pendingHighlightRequest.value = null
    }

    fun saveHighlight(quoteText: String, note: String, colorHex: String) {
        val req = _pendingHighlightRequest.value ?: return
        _pendingHighlightRequest.value = null
        viewModelScope.launch {
            repository.addHighlight(
                url = req.url,
                domain = req.domain,
                title = req.title,
                quoteText = quoteText,
                note = note,
                colorHex = colorHex
            )
            showMessage("Highlight saved to Library")
        }
    }

    fun deleteHighlight(id: Long) = viewModelScope.launch {
        repository.deleteHighlight(id)
    }

    fun exportHighlightsToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val md = repository.exportHighlightsAsMarkdown()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(md.toByteArray(Charsets.UTF_8))
                }
                showMessage("Highlights exported as Markdown")
            }.onFailure {
                showMessage("Highlights export failed")
            }
        }
    }

    fun subscribeRssFeed(feedUrl: String) {
        val clean = feedUrl.trim()
        if (!clean.startsWith("http")) {
            showMessage("Enter a valid RSS or Atom feed URL")
            return
        }
        viewModelScope.launch {
            showMessage("Subscribing to feed...")
            RssFeedHelper.fetchAndParseFeed(clean)
                .onSuccess { parsed ->
                    repository.saveRssFeedAndArticles(
                        feedTitle = parsed.title,
                        feedUrl = parsed.feedUrl,
                        siteUrl = parsed.siteUrl,
                        articles = parsed.articles
                    )
                    showMessage("Subscribed to ${parsed.title} (${parsed.articles.size} articles)")
                }
                .onFailure {
                    showMessage("Could not load RSS/Atom feed")
                }
        }
    }

    fun refreshAllRssFeeds() {
        viewModelScope.launch {
            val feeds = repository.getRssFeedsOnce()
            if (feeds.isEmpty()) {
                showMessage("No subscribed feeds yet")
                return@launch
            }
            var updated = 0
            feeds.forEach { feed ->
                RssFeedHelper.fetchAndParseFeed(feed.feedUrl).onSuccess { parsed ->
                    repository.saveRssFeedAndArticles(
                        feedTitle = parsed.title,
                        feedUrl = feed.feedUrl,
                        siteUrl = parsed.siteUrl,
                        articles = parsed.articles
                    )
                    updated++
                }
            }
            showMessage("Refreshed $updated feed(s)")
        }
    }

    fun setRssArticleRead(id: Long, isRead: Boolean) = viewModelScope.launch {
        repository.setRssArticleRead(id, isRead)
    }

    fun deleteRssFeed(feedId: Long) = viewModelScope.launch {
        repository.deleteRssFeed(feedId)
        showMessage("Unsubscribed from feed")
    }

    fun installCurrentSiteAsPwa(context: Context) {
        val tab = activeTab.value
        if (tab.url.isBlank() || tab.isHome) {
            showMessage("Open a website first to install shortcut")
            return
        }
        val ok = PwaShortcutHelper.pinWebAppShortcut(
            context = context,
            url = tab.url,
            title = tab.title.ifBlank { tab.domain },
            standaloneWindow = true
        )
        showMessage(
            if (ok) "Pinned ${tab.domain} as Web App shortcut"
            else "Launcher doesn't support direct pin — use launcher Widgets/Shortcuts"
        )
    }

    fun openStructuredViewerForCurrentPage() {
        val tab = activeTab.value
        if (tab.url.isBlank() || tab.isHome) {
            showMessage("Open a JSON, Markdown, or XML URL first")
            return
        }
        openDevTools(initialTab = 3)
    }

    fun dismissStructuredViewer() {
        _structuredViewerState.value = null
    }

    fun syncAllFilterLists() {
        viewModelScope.launch {
            val lists = repository.getFilterListsOnce()
            val updated = shieldsEngine.refreshAdBlockLists(force = true)
            var synced = 0
            lists.filter { it.enabled }.forEach { list ->
                runCatching { shieldsEngine.adBlockRuleCountForList(list.name) }
                    .onSuccess { newCount ->
                        repository.updateFilterListStats(list.id, newCount, System.currentTimeMillis())
                        synced++
                    }
            }
            val (networkRules, cosmeticRules) = shieldsEngine.adBlockRuleCounts()
            val suffix = if (updated > 0) "$updated refreshed from upstream" else "using bundled copies"
            showMessage("Shields: $synced list(s), $networkRules network / $cosmeticRules cosmetic rules ($suffix)")
        }
    }

    fun exportConfigBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = repository.exportConfigBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                showMessage("Exported full Ritam config backup (JSON)")
            }.onFailure {
                showMessage("Config backup export failed")
            }
        }
    }

    fun importConfigBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val count = repository.importConfigBackupJson(json)
                showMessage("Restored $count items from Ritam backup")
            }.onFailure {
                showMessage("Invalid backup JSON file")
            }
        }
    }

    fun exportEncryptedBackupToUri(context: Context, uri: Uri, passphrase: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val envelope = repository.exportEncryptedVaultAndConfigJson(passphrase)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(envelope.toByteArray(Charsets.UTF_8))
                }
                showMessage("Exported AES-256 encrypted backup (.ritamvault)")
            }.onFailure { err ->
                showMessage(err.message ?: "Encrypted backup export failed")
            }
        }
    }

    fun importEncryptedBackupFromUri(context: Context, uri: Uri, passphrase: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val count = repository.importEncryptedBackup(content, passphrase)
                showMessage("Restored $count items & vault secrets securely")
            }.onFailure {
                showMessage("Wrong passphrase or invalid encrypted backup")
            }
        }
    }

    // --- 9-Feature Privacy & Security Controls ---

    fun dismissHomographWarning() {
        _homographWarning.value = null
    }

    fun bypassHomographAndLoad() {
        val warning = _homographWarning.value ?: return
        _homographWarning.value = null
        webViewManager.allowHomographHostOnce(warning.asciiHost)
        navigateToInputOnTab(warning.tabId, warning.url, bypassHomographCheck = true)
    }

    fun resolveWebPermissionPrompt(allow: Boolean, rememberForDomain: Boolean) {
        val req = _webPermissionPrompt.value ?: return
        _webPermissionPrompt.value = null

        if (allow) {
            req.rawPermissionRequest?.let { pr ->
                runCatching { pr.grant(pr.resources) }
            }
            req.geoCallback?.invoke(req.geoOrigin, true, rememberForDomain)
            if (!rememberForDomain) {
                webViewManager.grantTemporaryTabPermissions(req.tabId, req.permissionKeys)
                updateTab(req.tabId) {
                    it.copy(activeSensorGrants = webViewManager.getTemporaryTabPermissions(req.tabId))
                }
                showMessage("${req.resourceLabels.joinToString(" & ")} allowed for this tab only")
            } else {
                showMessage("Always allowed ${req.resourceLabels.joinToString(" & ")} on ${req.domain}")
            }
        } else {
            req.rawPermissionRequest?.let { pr ->
                runCatching { pr.deny() }
            }
            req.geoCallback?.invoke(req.geoOrigin, false, false)
            showMessage("Blocked ${req.resourceLabels.joinToString(" & ")} on ${req.domain}")
        }

        if (rememberForDomain && req.domain.isNotBlank()) {
            val cleanDomain = req.domain.lowercase().removePrefix("www.")
            val value = if (allow) 1 else -1
            viewModelScope.launch {
                val existing = repository.getWebsiteSettings(cleanDomain) ?: WebsiteSettingsEntity(domain = cleanDomain)
                var updated = existing
                if ("CAMERA" in req.permissionKeys) updated = updated.copy(cameraPermission = value)
                if ("MICROPHONE" in req.permissionKeys) updated = updated.copy(micPermission = value)
                if ("LOCATION" in req.permissionKeys) updated = updated.copy(locationPermission = value)
                repository.upsertWebsiteSettings(updated.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun setTrackerXRayOpen(open: Boolean) {
        _trackerXRayOpen.value = open
    }

    fun toggleRuleForXRayHost(host: String, block: Boolean) {
        val clean = host.trim().lowercase().removePrefix("www.")
        if (clean.isBlank()) return
        val rule = if (block) "||$clean^" else "@@||$clean^"
        viewModelScope.launch {
            repository.addCustomFilterRule(rule)
            showMessage(if (block) "Added block rule: $rule" else "Allowlisted host: $rule")
        }
    }

    override fun onCleared() {
        clipboardClearJob?.cancel()
        mediaSessionHelper.shutdown()
        readAloudController.shutdown()
        super.onCleared()
    }

    fun setFilterListEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setFilterListEnabled(id, enabled)
    }

    fun addCustomFilterRule(rule: String) = viewModelScope.launch {
        repository.addCustomFilterRule(rule)
        showMessage("Filter rule added")
    }

    fun setCustomRuleEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setCustomRuleEnabled(id, enabled)
    }

    fun deleteCustomRule(id: Long) = viewModelScope.launch {
        repository.deleteCustomRule(id)
    }

    fun upsertWebsiteSetting(
        domain: String,
        shieldsEnabled: Boolean,
        jsEnabled: Boolean,
        httpsOnly: Boolean,
        zoomPercent: Int = 100,
        forceDarkOverride: Int = -1,
        isEphemeralContainer: Boolean = false
    ) {
        viewModelScope.launch {
            repository.upsertWebsiteSettings(
                WebsiteSettingsEntity(
                    domain = domain,
                    shieldsEnabled = shieldsEnabled,
                    javascriptEnabled = jsEnabled,
                    httpsOnly = httpsOnly,
                    zoomPercent = zoomPercent.coerceIn(50, 200),
                    forceDarkOverride = forceDarkOverride,
                    isEphemeralContainer = isEphemeralContainer
                )
            )
            val tab = activeTab.value
            if (tab.domain.equals(domain, ignoreCase = true)) {
                webViewManager.setTabZoom(tab.id, zoomPercent)
                updateTab(tab.id) { it.copy(zoomPercent = zoomPercent.coerceIn(50, 200)) }
                webViewManager.applyForceDarkNow(tab.id, domain)
            }
        }
    }

    fun addVaultEntry(label: String, domain: String, username: String, secretValue: String, category: String) {
        viewModelScope.launch {
            repository.addVaultEntry(label, domain, username, secretValue, category)
            showMessage("Vault entry saved locally")
        }
    }

    fun deleteVaultEntry(id: Long) = viewModelScope.launch {
        repository.deleteVaultEntry(id)
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun consumeMessage() {
        _snackbarMessage.value = null
    }

    fun resolveInputToUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        return if (looksLikeUrl(trimmed)) {
            "https://$trimmed"
        } else {
            preferences.value.searchEngine.buildQueryUrl(trimmed)
        }
    }

    fun looksLikeUrl(input: String): Boolean {
        if (input.contains(" ")) return false
        if (input.startsWith("http://", ignoreCase = true) ||
            input.startsWith("https://", ignoreCase = true)
        ) return true
        return input.contains(".") && input.length > 3 && !input.endsWith(".")
    }

    private fun extractHost(url: String): String {
        return runCatching {
            Uri.parse(url).host?.removePrefix("www.") ?: ""
        }.getOrDefault("")
    }

    fun dismissAdultBlockNotice() {
        _adultBlockNotice.value = null
    }

    private fun updateTab(tabId: String, transform: (TabState) -> TabState) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                MainViewModel(app)
            }
        }
    }
}

/** Shown when a query/URL is refused by the adult-content policy. */
data class AdultBlockNotice(val matched: String, val attemptedInput: String)
