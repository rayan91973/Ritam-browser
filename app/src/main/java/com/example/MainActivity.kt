package com.example

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.privacy.UrlCleaner
import com.example.services.BiometricAuthHelper
import com.example.ui.MainViewModel
import com.example.ui.components.AddBookmarkDialog
import com.example.ui.components.AddVaultEntryDialog
import com.example.ui.components.BurnSessionOverlay
import com.example.ui.components.ClearBrowsingDataDialog
import com.example.ui.components.DevToolsSheet
import com.example.ui.components.ExternalAppConfirmDialog
import com.example.ui.components.HomeWidgetPreviewDialog
import com.example.ui.components.LinkContextMenuDialog
import com.example.ui.components.MaliciousDomainDialog
import com.example.ui.components.PageSourceDialog
import com.example.ui.components.PdfViewerDialog
import com.example.ui.components.QrCodeShareDialog
import com.example.ui.components.QuickControlsSheet
import com.example.ui.components.SitePermissionsDialog
import com.example.ui.components.SslErrorDialog
import com.example.ui.components.TabSwitcherOverlay
import com.example.ui.components.TranslateSheetDialog
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LibraryTab
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.SettingsAnalyticsScreen
import com.example.ui.screens.SettingsFocusSection
import com.example.ui.theme.RitamTheme
import com.example.widget.RitamSearchWidgetProvider

object RitamRoutes {
    const val BROWSER = "browser"
    const val LIBRARY = "library/{tab}"
    const val SETTINGS = "settings/{section}"
    const val QR_SCANNER = "qr_scanner"

    fun library(tab: LibraryTab = LibraryTab.ALL): String = "library/${tab.name}"
    fun settings(section: SettingsFocusSection = SettingsFocusSection.ALL): String = "settings/${section.name}"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private var widgetOpenOmniboxTrigger by mutableIntStateOf(0)
    private var widgetVoiceSearchTrigger by mutableIntStateOf(0)
    private var isInPipMode by mutableStateOf(false)

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.navigateToInput(spoken)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            RitamTheme(preferences = preferences) {
                RitamAppRoot(
                    viewModel = viewModel,
                    externalOpenOmniboxTrigger = widgetOpenOmniboxTrigger,
                    externalVoiceTrigger = widgetVoiceSearchTrigger,
                    isInPipMode = isInPipMode,
                    onEnterPipMode = { enterPipMode() },
                    onLaunchVoiceSearch = { launchVoiceRecognition() }
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val tab = viewModel.activeTab.value
        if (tab.isPlayingMedia && !tab.isHome && !isFinishing) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        viewModel.relockSensitiveAreas()
        if (isFinishing) {
            viewModel.wipeOnExitIfEnabled()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun enterPipMode() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            viewModel.showMessage("Picture-in-Picture is not supported on this device")
            return
        }
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        runCatching {
            enterPictureInPictureMode(params)
        }.onFailure {
            viewModel.showMessage("Open a video or media page to use Picture-in-Picture")
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (!url.isNullOrBlank()) {
                viewModel.openUrlInNewTab(url)
            }
            return
        }
        when (intent?.getStringExtra(RitamSearchWidgetProvider.EXTRA_WIDGET_ACTION)) {
            RitamSearchWidgetProvider.ACTION_SEARCH -> widgetOpenOmniboxTrigger++
            RitamSearchWidgetProvider.ACTION_VOICE -> widgetVoiceSearchTrigger++
            RitamSearchWidgetProvider.ACTION_PRIVATE -> {
                viewModel.createNewTab(isPrivate = true)
                widgetOpenOmniboxTrigger++
            }
        }
    }

    private fun launchVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak search query or URL")
        }
        runCatching {
            voiceSearchLauncher.launch(intent)
        }.onFailure {
            viewModel.showMessage("Voice recognizer is unavailable on this device")
        }
    }
}

@Composable
private fun RitamAppRoot(
    viewModel: MainViewModel,
    externalOpenOmniboxTrigger: Int,
    externalVoiceTrigger: Int,
    isInPipMode: Boolean,
    onEnterPipMode: () -> Unit,
    onLaunchVoiceSearch: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val rootView = LocalView.current

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val homeBookmarks by viewModel.homeBookmarks.collectAsStateWithLifecycle()
    val allBookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val bookmarkFolders by viewModel.bookmarkFolders.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val readingList by viewModel.readingList.collectAsStateWithLifecycle()
    val filterLists by viewModel.filterLists.collectAsStateWithLifecycle()
    val customRules by viewModel.customFilterRules.collectAsStateWithLifecycle()
    val websiteSettings by viewModel.websiteSettings.collectAsStateWithLifecycle()
    val vaultEntries by viewModel.vaultEntries.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val adsBlockedTotal by viewModel.adsBlockedTotal.collectAsStateWithLifecycle()
    val trackersBlockedTotal by viewModel.trackersBlockedTotal.collectAsStateWithLifecycle()
    val totalBytesSaved by viewModel.totalBytesSaved.collectAsStateWithLifecycle()
    val sessionMetrics by viewModel.sessionMetrics.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val sslWarning by viewModel.sslWarning.collectAsStateWithLifecycle()
    val maliciousWarning by viewModel.maliciousWarning.collectAsStateWithLifecycle()
    val findInPageActive by viewModel.findInPageActive.collectAsStateWithLifecycle()
    val findInPageQuery by viewModel.findInPageQuery.collectAsStateWithLifecycle()
    val readerFontSizeSp by viewModel.readerFontSizeSp.collectAsStateWithLifecycle()
    val pageSourceState by viewModel.pageSourceState.collectAsStateWithLifecycle()
    val readAloudState by viewModel.readAloudState.collectAsStateWithLifecycle()
    val recentlyClosedCount by viewModel.recentlyClosedCount.collectAsStateWithLifecycle()
    val isIncognitoLocked by viewModel.isIncognitoLocked.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val isToolbarHiddenOnScroll by viewModel.isToolbarHiddenOnScroll.collectAsStateWithLifecycle()
    val isBurningSession by viewModel.isBurningSession.collectAsStateWithLifecycle()
    val pdfViewerState by viewModel.pdfViewerState.collectAsStateWithLifecycle()
    val fullscreenVideoView by viewModel.fullscreenVideoView.collectAsStateWithLifecycle()
    val devToolsVisible by viewModel.devToolsVisible.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val networkLogs by viewModel.networkLogs.collectAsStateWithLifecycle()
    val cookiesDump by viewModel.cookiesDump.collectAsStateWithLifecycle()
    val localStoragePairs by viewModel.localStoragePairs.collectAsStateWithLifecycle()
    val devEvalOutput by viewModel.devEvalOutput.collectAsStateWithLifecycle()
    val externalAppRequest by viewModel.externalAppRequest.collectAsStateWithLifecycle()
    val longPressContext by viewModel.longPressContext.collectAsStateWithLifecycle()
    val qrShareUrl by viewModel.qrShareUrl.collectAsStateWithLifecycle()
    val translateDialogOpen by viewModel.translateDialogOpen.collectAsStateWithLifecycle()
    val translateExcerpt by viewModel.translateExcerpt.collectAsStateWithLifecycle()
    val dohTestResult by viewModel.dohTestResult.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showQuickControls by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var addBookmarkUrlEdit by remember { mutableStateOf<Boolean?>(null) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSiteInfoDialog by remember { mutableStateOf(false) }
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        val actionLabel = if (msg == "Tab closed" && recentlyClosedCount > 0) "Undo" else null
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && msg == "Tab closed") {
            viewModel.undoCloseTab()
        }
        viewModel.consumeMessage()
    }
    LaunchedEffect(externalVoiceTrigger) {
        if (externalVoiceTrigger > 0) {
            onLaunchVoiceSearch()
        }
    }

    val shieldsEnabledForSite = remember(activeTab.domain, websiteSettings) {
        val cleanDomain = activeTab.domain.lowercase().removePrefix("www.")
        websiteSettings.firstOrNull { it.domain == cleanDomain }?.shieldsEnabled ?: true
    }

    val backdropModifier = if (showTabSwitcher && preferences.surfaceBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(10.dp)
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = RitamRoutes.BROWSER,
            modifier = backdropModifier
        ) {
            composable(RitamRoutes.BROWSER) {
                BrowserScreen(
                    activeTab = activeTab,
                    tabsCount = tabs.size,
                    preferences = preferences,
                    homeBookmarks = homeBookmarks,
                    allBookmarks = allBookmarks,
                    recentSearches = recentSearches,
                    webViewManager = viewModel.webViewManager,
                    shieldsEnabledForSite = shieldsEnabledForSite,
                    sessionBlockedCount = sessionMetrics.adsBlocked + sessionMetrics.trackersBlocked,
                    findInPageActive = findInPageActive,
                    findInPageQuery = findInPageQuery,
                    readerFontSizeSp = readerFontSizeSp,
                    readAloudState = readAloudState,
                    externalOpenOmniboxTrigger = externalOpenOmniboxTrigger,
                    looksLikeUrl = { viewModel.looksLikeUrl(it) },
                    onSelectSearchEngine = { viewModel.updateSearchEngine(it) },
                    onCopyAddress = { url -> viewModel.copyToClipboard(context, url, "Address") },
                    onNavigateInput = { viewModel.navigateToInput(it) },
                    onOpenSanctuaryEssay = { viewModel.openSanctuaryEssay() },
                    onBack = { viewModel.goBack() },
                    onForward = { viewModel.goForward() },
                    onHome = { viewModel.goHome() },
                    onReloadOrStop = { viewModel.reloadOrStop() },
                    onSwipeTab = { delta -> viewModel.switchToAdjacentTab(delta) },
                    onDoubleTapAddress = { viewModel.scrollActivePageToTop() },
                    onToggleReaderMode = { viewModel.toggleReaderMode() },
                    onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                    onAdjustReaderFontSize = { viewModel.adjustReaderFontSize(it) },
                    onToggleSiteShields = { viewModel.toggleShieldsForCurrentDomain() },
                    onFindQueryChange = { viewModel.updateFindInPageQuery(it) },
                    onFindNext = { viewModel.findNext(it) },
                    onCloseFind = { viewModel.setFindInPageActive(false) },
                    onToggleReadAloudPlayPause = { viewModel.toggleReadAloudPlayPause() },
                    onPrevReadAloud = { viewModel.prevReadAloudParagraph() },
                    onNextReadAloud = { viewModel.nextReadAloudParagraph() },
                    onCycleReadAloudSpeed = { viewModel.cycleReadAloudSpeed() },
                    onStopReadAloud = { viewModel.stopReadAloud() },
                    onOpenTabSwitcher = { showTabSwitcher = true },
                    onOpenQuickControls = { showQuickControls = true },
                    onOpenShieldSettings = {
                        navController.navigate(RitamRoutes.settings(SettingsFocusSection.SHIELDS))
                    },
                    onOpenProfileSettings = {
                        navController.navigate(RitamRoutes.settings(SettingsFocusSection.ALL))
                    },
                    onOpenAddBookmark = { allowEdit -> addBookmarkUrlEdit = allowEdit },
                    onOpenBookmarksManager = {
                        navController.navigate(RitamRoutes.library(LibraryTab.BOOKMARKS))
                    },
                    onOpenSiteInfo = { showSiteInfoDialog = true },
                    onVoiceSearch = onLaunchVoiceSearch,
                    onScanQr = { navController.navigate(RitamRoutes.QR_SCANNER) },
                    isToolbarHiddenOnScroll = isToolbarHiddenOnScroll,
                    isIncognitoLocked = isIncognitoLocked && preferences.biometricIncognitoLock,
                    isInPipMode = isInPipMode,
                    fullscreenVideoView = fullscreenVideoView,
                    onExitFullscreenVideo = { viewModel.exitFullscreenVideo() },
                    onUnlockIncognito = {
                        BiometricAuthHelper.authenticate(
                            activityContext = context,
                            title = "Unlock Incognito",
                            subtitle = "Verify your identity to view private tabs",
                            onSuccess = { viewModel.markIncognitoUnlocked() },
                            onError = { msg -> viewModel.showMessage(msg) }
                        )
                    },
                    onSwitchToRegularTab = {
                        val regular = tabs.firstOrNull { !it.isPrivate }
                        if (regular != null) {
                            viewModel.switchTab(regular.id)
                        } else {
                            viewModel.createNewTab(isPrivate = false)
                        }
                    },
                    onRevealToolbars = { viewModel.revealToolbars() },
                    onToggleTabMedia = { viewModel.toggleTabMediaPlayback() },
                    onEnterPip = onEnterPipMode,
                    onShareQrCode = { url -> viewModel.showQrForCurrentOrUrl(url) },
                    onAddToReadingList = { viewModel.addCurrentPageToReadingList() }
                )
            }

            composable(
                route = RitamRoutes.LIBRARY,
                arguments = listOf(navArgument("tab") { type = NavType.StringType })
            ) { backStackEntry ->
                val tabArg = backStackEntry.arguments?.getString("tab")
                val initialTab = runCatching { LibraryTab.valueOf(tabArg ?: "") }.getOrDefault(LibraryTab.ALL)
                LibraryScreen(
                    initialTab = initialTab,
                    history = history,
                    downloads = downloads,
                    bookmarks = allBookmarks,
                    folders = bookmarkFolders,
                    storageStats = storageStats,
                    preferences = preferences,
                    isDesktopActive = activeTab.isDesktopMode,
                    onBack = { navController.popBackStack() },
                    onOpenUrl = { url ->
                        navController.popBackStack(RitamRoutes.BROWSER, inclusive = false)
                        viewModel.navigateToInput(url)
                    },
                    onCopyUrl = { url -> viewModel.copyToClipboard(context, url) },
                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                    onDeleteHistoryItems = { viewModel.deleteHistoryItems(it) },
                    onOpenClearData = { showClearDataDialog = true },
                    onExportHistoryUri = { uri -> viewModel.exportHistoryToUri(context, uri) },
                    onCancelDownload = { viewModel.cancelDownload(it) },
                    onOpenDownload = { viewModel.openDownload(it) },
                    onShareDownload = { viewModel.shareDownload(it) },
                    onDeleteDownload = { viewModel.deleteDownload(it) },
                    onClearCompletedDownloads = { viewModel.clearCompletedDownloads() },
                    onToggleAskWhereToSave = { viewModel.updateAskWhereToSave(it) },
                    onAddBookmark = { addBookmarkUrlEdit = true },
                    onAddFolder = { viewModel.addBookmarkFolder(it) },
                    onDeleteBookmark = { viewModel.removeBookmark(it) },
                    onExportBookmarksUri = { uri -> viewModel.exportBookmarksToUri(context, uri) },
                    onImportBookmarksUri = { uri -> viewModel.importBookmarksFromUri(context, uri) },
                    onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                    onToggleDarkMode = { viewModel.toggleDarkModeQuick() },
                    onOpenSettings = { navController.navigate(RitamRoutes.settings(SettingsFocusSection.ALL)) },
                    onVoiceSearch = onLaunchVoiceSearch,
                    onRefreshStorage = { viewModel.refreshStorageStats() },
                    readingList = readingList,
                    onToggleReadLaterDone = { id, read -> viewModel.setReadingListRead(id, read) },
                    onDeleteReadLater = { viewModel.deleteReadingListItem(it) },
                    onClearReadLaterDone = { viewModel.clearReadReadingList() }
                )
            }

            composable(
                route = RitamRoutes.SETTINGS,
                arguments = listOf(navArgument("section") { type = NavType.StringType })
            ) { backStackEntry ->
                val sectionArg = backStackEntry.arguments?.getString("section")
                val initialSection = runCatching {
                    SettingsFocusSection.valueOf(sectionArg ?: "")
                }.getOrDefault(SettingsFocusSection.ALL)

                SettingsAnalyticsScreen(
                    initialSection = initialSection,
                    preferences = preferences,
                    adsBlockedTotal = adsBlockedTotal,
                    trackersBlockedTotal = trackersBlockedTotal,
                    totalBytesSaved = totalBytesSaved,
                    sessionMetrics = sessionMetrics,
                    openTabsCount = tabs.size,
                    filterLists = filterLists,
                    customRules = customRules,
                    websiteSettings = websiteSettings,
                    vaultEntries = vaultEntries,
                    onBack = { navController.popBackStack() },
                    onSelectPalette = { viewModel.updateThemePalette(it) },
                    onSelectAccent = { viewModel.updateAccentTone(it) },
                    onToggleSurfaceBlur = { viewModel.updateSurfaceBlur(it) },
                    onToggleOledBlack = { viewModel.updateOledPureBlack(it) },
                    onSelectFont = { viewModel.updateFontPairing(it) },
                    onChangeFontScale = { viewModel.updateFontScalePercent(it) },
                    onToggleFluidTypography = { viewModel.updateFluidTypography(it) },
                    onSelectCorner = { viewModel.updateCornerTopology(it) },
                    onSelectOmnibox = { viewModel.updateOmniboxTopology(it) },
                    onSelectDensity = { viewModel.updateLayoutDensity(it) },
                    onResetAppearance = { viewModel.resetAppearanceDefaults() },
                    onSelectSearchEngine = { viewModel.updateSearchEngine(it) },
                    onSaveHomepageUrl = { viewModel.updateHomepageUrl(it) },
                    onToggleDefaultDesktop = { viewModel.updateDefaultDesktopMode(it) },
                    onToggleAdBlock = { viewModel.updateAdBlocking(it) },
                    onToggleCosmeticAdBlock = { viewModel.updateCosmeticAdBlocking(it) },
                    onToggleTrackerBlock = { viewModel.updateTrackerBlocking(it) },
                    onToggleMalicious = { viewModel.updateMaliciousProtection(it) },
                    onToggleHttpsOnly = { viewModel.updateHttpsOnly(it) },
                    onToggleJs = { viewModel.updateJavascriptEnabled(it) },
                    onToggleThirdPartyCookies = { viewModel.updateBlockThirdPartyCookies(it) },
                    onToggleFilterList = { id, enabled -> viewModel.setFilterListEnabled(id, enabled) },
                    onAddCustomRule = { viewModel.addCustomFilterRule(it) },
                    onToggleCustomRule = { id, enabled -> viewModel.setCustomRuleEnabled(id, enabled) },
                    onDeleteCustomRule = { viewModel.deleteCustomRule(it) },
                    onToggleLocalTelemetry = { viewModel.updateLocalTelemetry(it) },
                    onToggleNotificationsEnabled = { viewModel.updateNotificationsEnabled(it) },
                    onToggleDownloadNotifications = { viewModel.updateDownloadNotificationsEnabled(it) },
                    onToggleShieldAlerts = { viewModel.updateShieldAlertNotificationsEnabled(it) },
                    onToggleBlockSitePrompts = { viewModel.updateBlockSiteNotificationPrompts(it) },
                    onSendTestNotification = { viewModel.sendTestNotification() },
                    onToggleSwipeNav = { viewModel.updateGestureSwipeNav(it) },
                    onToggleSwipeTabs = { viewModel.updateGestureSwipeTabs(it) },
                    onTogglePullRefresh = { viewModel.updateGesturePullRefresh(it) },
                    onToggleDoubleTapTop = { viewModel.updateGestureDoubleTapTop(it) },
                    onOpenWidgetPreview = { showWidgetDialog = true },
                    onSelectProfile = { viewModel.updateActiveProfile(it) },
                    onToggleBiometric = { viewModel.updateBiometricVaultLock(it) },
                    onToggleAutofill = { viewModel.updateAutofillEnabled(it) },
                    onAddVaultEntry = { showAddVaultDialog = true },
                    onCopySecret = { secret -> viewModel.copyToClipboard(context, secret, "Vault Secret") },
                    onDeleteVaultEntry = { viewModel.deleteVaultEntry(it) },
                    onOpenBookmarksManager = {
                        navController.navigate(RitamRoutes.library(LibraryTab.BOOKMARKS))
                    },
                    onOpenClearData = { showClearDataDialog = true },
                    dohTestResult = dohTestResult,
                    isVaultUnlocked = isVaultUnlocked,
                    onUnlockVault = {
                        BiometricAuthHelper.authenticate(
                            activityContext = context,
                            title = "Unlock Vault",
                            subtitle = "Authenticate to view local passwords and keys",
                            onSuccess = { viewModel.markVaultUnlocked() },
                            onError = { msg -> viewModel.showMessage(msg) }
                        )
                    },
                    onToggleForceWebDark = { viewModel.updateForceWebDarkMode(it) },
                    onToggleBottomOmnibox = { viewModel.updateBottomOmnibox(it) },
                    onToggleAutoHideToolbars = { viewModel.updateAutoHideToolbarsOnScroll(it) },
                    onToggleCookieBanners = { viewModel.updateBlockCookieBanners(it) },
                    onToggleStripTracking = { viewModel.updateStripTrackingParams(it) },
                    onToggleConfirmExternalApp = { viewModel.updateConfirmExternalAppLaunch(it) },
                    onSelectDnsProvider = { viewModel.updateDnsProvider(it) },
                    onSaveCustomDohUrl = { viewModel.updateCustomDohUrl(it) },
                    onTestDoh = { viewModel.runDohLatencyTest() },
                    onToggleBiometricIncognito = { viewModel.updateBiometricIncognitoLock(it) },
                    onToggleClearOnExit = { viewModel.updateClearOnExit(it) },
                    onChangeAutoCloseDays = { viewModel.updateAutoCloseTabsDays(it) },
                    onSelectTranslateLanguage = { viewModel.updatePreferredTranslateLanguage(it) }
                )
            }

            composable(RitamRoutes.QR_SCANNER) {
                QrScannerScreen(
                    onCodeScanned = { code ->
                        navController.popBackStack()
                        viewModel.navigateToInput(code)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showTabSwitcher) {
            TabSwitcherOverlay(
                tabs = tabs,
                activeTabId = activeTab.id,
                recentlyClosedCount = recentlyClosedCount,
                autoCloseDays = preferences.autoCloseTabsDays,
                onSelectTab = { viewModel.switchTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onTogglePinTab = { viewModel.togglePinTab(it) },
                onAssignTabGroup = { id, group -> viewModel.assignTabGroup(id, group) },
                onUndoCloseTab = { viewModel.undoCloseTab() },
                onCloseInactiveTabs = { viewModel.closeInactiveTabs() },
                onNewTab = { isPrivate -> viewModel.createNewTab(isPrivate) },
                onCloseAllRegular = { viewModel.closeAllRegularTabs() },
                onDismiss = { showTabSwitcher = false }
            )
        }

        if (showQuickControls) {
            QuickControlsSheet(
                tab = activeTab,
                preferences = preferences,
                totalBlockedSession = sessionMetrics.adsBlocked + sessionMetrics.trackersBlocked,
                shieldsEnabledForSite = shieldsEnabledForSite,
                onDismiss = { showQuickControls = false },
                onOpenBookmarks = { navController.navigate(RitamRoutes.library(LibraryTab.BOOKMARKS)) },
                onOpenHistory = { navController.navigate(RitamRoutes.library(LibraryTab.HISTORY)) },
                onOpenDownloads = { navController.navigate(RitamRoutes.library(LibraryTab.DOWNLOADS)) },
                onOpenSettings = { section ->
                    val target = when (section) {
                        "analytics" -> SettingsFocusSection.ANALYTICS
                        "shields" -> SettingsFocusSection.SHIELDS
                        "vault" -> SettingsFocusSection.VAULT
                        else -> SettingsFocusSection.ALL
                    }
                    navController.navigate(RitamRoutes.settings(target))
                },
                onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                onToggleReaderMode = { viewModel.toggleReaderMode() },
                onToggleFindInPage = { viewModel.setFindInPageActive(true) },
                onSharePage = { viewModel.shareCurrentPage(context) },
                onToggleDarkMode = { viewModel.toggleDarkModeQuick() },
                onNewTab = { viewModel.createNewTab(false) },
                onNewPrivateTab = { viewModel.createNewTab(true) },
                onToggleHttpsOnly = { viewModel.updateHttpsOnly(!preferences.httpsOnlyEnabled) },
                onToggleSiteShields = { viewModel.toggleShieldsForCurrentDomain() },
                onBookmarkCurrent = { addBookmarkUrlEdit = false },
                onScreenshot = { viewModel.captureAndShareScreenshot(rootView, context) },
                onPrintPage = { viewModel.printCurrentPage(context) },
                onViewSource = { viewModel.openPageSource() },
                onSavePage = { viewModel.saveCurrentPageOffline() },
                onReadAloud = { viewModel.startReadAloud() },
                onOpenWidgets = { showWidgetDialog = true },
                onOpenNotificationsSettings = {
                    navController.navigate(RitamRoutes.settings(SettingsFocusSection.NOTIFICATIONS))
                },
                onOpenGesturesSettings = {
                    navController.navigate(RitamRoutes.settings(SettingsFocusSection.GESTURES))
                },
                onClearData = { showClearDataDialog = true },
                onSiteInfo = { showSiteInfoDialog = true },
                onToggleForceWebDark = { viewModel.updateForceWebDarkMode(!preferences.forceWebDarkMode) },
                onOpenTranslate = { viewModel.openTranslateDialog() },
                onAddToReadingList = { viewModel.addCurrentPageToReadingList() },
                onShowQrCode = { viewModel.showQrForCurrentOrUrl() },
                onBurnSession = { viewModel.burnSession() },
                onEnterPip = onEnterPipMode,
                onOpenDevTools = { viewModel.openDevTools() },
                onToggleBottomOmnibox = { viewModel.updateBottomOmnibox(!preferences.bottomOmnibox) },
                onChangePageZoom = { zoom -> viewModel.setTabZoom(zoom) }
            )
        }

        sslWarning?.let { warning ->
            SslErrorDialog(
                warning = warning,
                onDismissAndGoBack = {
                    viewModel.dismissSslWarning()
                    viewModel.goBack()
                }
            )
        }

        maliciousWarning?.let { warning ->
            MaliciousDomainDialog(
                warning = warning,
                onBackToSafety = { viewModel.dismissMaliciousWarning() }
            )
        }

        addBookmarkUrlEdit?.let { allowEdit ->
            AddBookmarkDialog(
                initialTitle = if (allowEdit) "" else activeTab.title,
                initialUrl = if (allowEdit) "" else activeTab.url,
                folders = bookmarkFolders,
                allowUrlEdit = allowEdit,
                onSave = { title, url, folderId, pinHome ->
                    if (allowEdit) {
                        viewModel.addCustomBookmark(url, title, folderId, pinHome)
                    } else {
                        viewModel.bookmarkCurrentPage(title, folderId, pinHome)
                    }
                    addBookmarkUrlEdit = null
                },
                onDismiss = { addBookmarkUrlEdit = null }
            )
        }

        if (showClearDataDialog) {
            ClearBrowsingDataDialog(
                onConfirm = { h, c, ck ->
                    viewModel.clearBrowsingData(h, c, ck)
                    showClearDataDialog = false
                },
                onDismiss = { showClearDataDialog = false }
            )
        }

        if (showSiteInfoDialog) {
            val cleanDomain = activeTab.domain.lowercase().removePrefix("www.")
            val currentSiteSetting = websiteSettings.firstOrNull { it.domain == cleanDomain }
            SitePermissionsDialog(
                domain = cleanDomain,
                sslSecure = activeTab.sslSecure,
                shieldsEnabled = currentSiteSetting?.shieldsEnabled ?: true,
                jsEnabled = currentSiteSetting?.javascriptEnabled ?: preferences.javascriptEnabled,
                httpsOnly = currentSiteSetting?.httpsOnly ?: preferences.httpsOnlyEnabled,
                zoomPercent = currentSiteSetting?.zoomPercent ?: activeTab.zoomPercent,
                forceDarkOverride = currentSiteSetting?.forceDarkOverride ?: -1,
                onSave = { shields, js, https, zoomPercent, forceDarkOverride ->
                    if (cleanDomain.isNotBlank()) {
                        viewModel.upsertWebsiteSetting(
                            domain = cleanDomain,
                            shieldsEnabled = shields,
                            jsEnabled = js,
                            httpsOnly = https,
                            zoomPercent = zoomPercent,
                            forceDarkOverride = forceDarkOverride
                        )
                        viewModel.reloadOrStop()
                    }
                    showSiteInfoDialog = false
                },
                onDismiss = { showSiteInfoDialog = false }
            )
        }

        if (showAddVaultDialog) {
            AddVaultEntryDialog(
                onSave = { label, domain, user, secret, cat ->
                    viewModel.addVaultEntry(label, domain, user, secret, cat)
                    showAddVaultDialog = false
                },
                onDismiss = { showAddVaultDialog = false }
            )
        }

        pageSourceState?.let { srcState ->
            PageSourceDialog(
                state = srcState,
                onCopySource = { html -> viewModel.copyToClipboard(context, html, "Page HTML Source") },
                onDismiss = { viewModel.dismissPageSource() }
            )
        }

        if (showWidgetDialog) {
            HomeWidgetPreviewDialog(
                onPinWidget = { viewModel.pinHomeWidget(context) },
                onDismiss = { showWidgetDialog = false }
            )
        }

        pdfViewerState?.let { pdfState ->
            PdfViewerDialog(
                state = pdfState,
                onOpenExternal = { file -> viewModel.openPdfExternally(context, file) },
                onDismiss = { viewModel.dismissPdfViewer() }
            )
        }

        if (devToolsVisible) {
            DevToolsSheet(
                domain = activeTab.domain,
                consoleLogs = consoleLogs,
                networkLogs = networkLogs,
                cookiesDump = cookiesDump,
                localStoragePairs = localStoragePairs,
                evalOutput = devEvalOutput,
                onRunJs = { viewModel.runDevJs(it) },
                onRefreshStorage = { viewModel.refreshDevStorage() },
                onClearLogs = { viewModel.clearDevLogs() },
                onDismiss = { viewModel.dismissDevTools() }
            )
        }

        qrShareUrl?.let { rawUrl ->
            val cleaned = remember(rawUrl) { UrlCleaner.cleanUrl(rawUrl) }
            QrCodeShareDialog(
                cleanUrl = cleaned.cleanedUrl,
                removedTrackerCount = cleaned.removedCount,
                onCopyCleanUrl = { viewModel.copyToClipboard(context, cleaned.cleanedUrl, "Clean URL") },
                onShareSystem = { viewModel.shareCurrentPage(context) },
                onDismiss = { viewModel.dismissQrDialog() }
            )
        }

        longPressContext?.let { req ->
            val rawTarget = req.linkUrl ?: req.imageUrl ?: ""
            val cleaned = remember(rawTarget) { UrlCleaner.cleanUrl(rawTarget) }
            LinkContextMenuDialog(
                request = req,
                cleanUrl = cleaned.cleanedUrl,
                onOpenNewTab = { url -> viewModel.openUrlInNewTab(url, isPrivate = false) },
                onOpenPrivateTab = { url -> viewModel.openUrlInNewTab(url, isPrivate = true) },
                onAddReadLater = { url -> viewModel.addUrlToReadingList(url) },
                onCopyClean = { url -> viewModel.copyToClipboard(context, url, "Clean URL") },
                onShareQr = { url -> viewModel.showQrForCurrentOrUrl(url) },
                onDismiss = { viewModel.dismissLongPressContext() }
            )
        }

        externalAppRequest?.let { req ->
            ExternalAppConfirmDialog(
                request = req,
                onAllowApp = { viewModel.confirmAllowExternalApp() },
                onOpenFallbackInBrowser = { fallback -> viewModel.loadFallbackWebUrl(fallback) },
                onDismiss = { viewModel.dismissExternalAppRequest() }
            )
        }

        if (translateDialogOpen) {
            TranslateSheetDialog(
                currentUrl = activeTab.url,
                isHome = activeTab.isHome,
                isTranslated = activeTab.isTranslated,
                selectedLangCode = preferences.preferredTranslateLanguage,
                initialExcerpt = translateExcerpt,
                onSelectLang = { viewModel.updatePreferredTranslateLanguage(it) },
                onTranslateFullPage = { lang -> viewModel.translateCurrentPage(lang) },
                onRestoreOriginal = { viewModel.restoreOriginalPage() },
                onDismiss = { viewModel.dismissTranslateDialog() }
            )
        }

        BurnSessionOverlay(visible = isBurningSession)

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
        )
    }
}
