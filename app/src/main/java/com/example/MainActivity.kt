package com.example

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Rational
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.privacy.UrlCleaner
import com.example.services.MediaSessionHelper
import com.example.services.PwaShortcutHelper
import com.example.ui.MainViewModel
import com.example.ui.components.AddBookmarkDialog
import com.example.ui.components.BurnSessionOverlay
import com.example.ui.components.ClearBrowsingDataDialog
import com.example.ui.components.CommandPaletteDialog
import com.example.ui.components.DevToolsSheet
import com.example.ui.components.ExternalAppConfirmDialog
import com.example.ui.components.HighlightNoteDialog
import com.example.ui.components.HomographWarningDialog
import com.example.ui.components.LinkContextMenuDialog
import com.example.ui.components.MaliciousDomainDialog
import com.example.ui.components.PageSourceDialog
import com.example.ui.components.PdfViewerDialog
import com.example.ui.components.QrCodeShareDialog
import com.example.ui.components.QuickControlsSheet
import com.example.ui.components.SitePermissionsDialog
import com.example.ui.components.SslErrorDialog
import com.example.ui.components.StructuredViewerDialog
import com.example.ui.components.TabSwitcherOverlay
import com.example.ui.components.TranslateSheetDialog
import com.example.ui.components.UniversalViewerDialog
import com.example.ui.components.UserscriptsDialog
import com.example.ui.components.WatchlistDialog
import com.example.ui.components.WebPermissionPromptDialog
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
        WebView.enableSlowWholeDocumentDraw()
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
                    onSyncPipParams = { isPlaying, hasActiveMedia, isHome ->
                        syncPipParams(isPlaying, hasActiveMedia, isHome)
                    },
                    onLaunchVoiceSearch = { launchVoiceRecognition() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Returning to the foreground: stop auto-resuming media so the user's own pause wins.
        viewModel.webViewManager.setBackgroundMode(false)
    }

    override fun onResume() {
        super.onResume()
        viewModel.webViewManager.resumeAllLiveWebViews()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val tab = viewModel.activeTab.value
        if ((tab.isPlayingMedia || tab.hasActiveMedia) && !tab.isHome && !isFinishing) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        viewModel.onEnteredPipMode(isInPictureInPictureMode)
    }

    override fun onStop() {
        super.onStop()
        // Keep video/audio playing when the user switches apps. The foreground media service is
        // already running from the JS media-state bridge; this also installs the page watchdog.
        val tab = viewModel.activeTab.value
        if (tab.isPlayingMedia || tab.hasActiveMedia) {
            viewModel.webViewManager.setBackgroundMode(true)
        }
        viewModel.relockSensitiveAreas()
        if (isFinishing) {
            viewModel.wipeOnExitIfEnabled()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun buildPipParams(
        isPlaying: Boolean,
        hasActiveMedia: Boolean,
        isHome: Boolean
    ): PictureInPictureParams {
        val toggleIntent = Intent(MediaSessionHelper.ACTION_MEDIA_TOGGLE).setPackage(packageName)
        val togglePending = PendingIntent.getBroadcast(
            this,
            212,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleAction = RemoteAction(
            Icon.createWithResource(
                this,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            ),
            if (isPlaying) "Pause" else "Play",
            if (isPlaying) "Pause playback" else "Resume playback",
            togglePending
        )
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(toggleAction))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled((isPlaying || hasActiveMedia) && !isHome)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun syncPipParams(isPlaying: Boolean, hasActiveMedia: Boolean, isHome: Boolean) {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        runCatching {
            setPictureInPictureParams(buildPipParams(isPlaying, hasActiveMedia, isHome))
        }
    }

    private fun enterPipMode() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            viewModel.showMessage("Picture-in-Picture is not supported on this device")
            return
        }
        val tab = viewModel.activeTab.value
        viewModel.onEnteringPipMode()
        val params = buildPipParams(
            isPlaying = tab.isPlayingMedia || tab.hasActiveMedia,
            hasActiveMedia = tab.hasActiveMedia,
            isHome = tab.isHome
        )
        runCatching {
            enterPictureInPictureMode(params)
        }.onFailure {
            viewModel.showMessage("Open a video or media page to use Picture-in-Picture")
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val pwaUrl = intent?.getStringExtra(PwaShortcutHelper.EXTRA_PWA_URL)
        val isStandalonePwa = intent?.getBooleanExtra(PwaShortcutHelper.EXTRA_PWA_STANDALONE, false) == true
        if (!pwaUrl.isNullOrBlank()) {
            viewModel.openUrlInNewTab(pwaUrl, isStandalonePwa = isStandalonePwa)
            return
        }
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (!url.isNullOrBlank()) {
                viewModel.openUrlInNewTab(url, isStandalonePwa = isStandalonePwa)
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
    onSyncPipParams: (Boolean, Boolean, Boolean) -> Unit,
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
    val websiteSettings by viewModel.websiteSettings.collectAsStateWithLifecycle()
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
    val isToolbarHiddenOnScroll by viewModel.isToolbarHiddenOnScroll.collectAsStateWithLifecycle()
    val isBurningSession by viewModel.isBurningSession.collectAsStateWithLifecycle()
    val pdfViewerState by viewModel.pdfViewerState.collectAsStateWithLifecycle()
    val fullscreenVideoView by viewModel.fullscreenVideoView.collectAsStateWithLifecycle()
    val devToolsVisible by viewModel.devToolsVisible.collectAsStateWithLifecycle()
    val devToolsInitialTab by viewModel.devToolsInitialTab.collectAsStateWithLifecycle()
    val devToolsPageSource by viewModel.devToolsPageSource.collectAsStateWithLifecycle()
    val cdpFormatState by viewModel.cdpFormatState.collectAsStateWithLifecycle()
    val topSites by viewModel.topSites.collectAsStateWithLifecycle()
    val quickControlUsage by viewModel.quickControlUsage.collectAsStateWithLifecycle()
    val cacheStats by viewModel.cacheStats.collectAsStateWithLifecycle()
    val universalViewerState by viewModel.universalViewerState.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val networkLogs by viewModel.networkLogs.collectAsStateWithLifecycle()
    val cookiesDump by viewModel.cookiesDump.collectAsStateWithLifecycle()
    val localStoragePairs by viewModel.localStoragePairs.collectAsStateWithLifecycle()
    val devEvalOutput by viewModel.devEvalOutput.collectAsStateWithLifecycle()
    val externalAppRequest by viewModel.externalAppRequest.collectAsStateWithLifecycle()
    val adultBlockNotice by viewModel.adultBlockNotice.collectAsStateWithLifecycle()
    val longPressContext by viewModel.longPressContext.collectAsStateWithLifecycle()
    val qrShareUrl by viewModel.qrShareUrl.collectAsStateWithLifecycle()
    val translateDialogOpen by viewModel.translateDialogOpen.collectAsStateWithLifecycle()
    val translateExcerpt by viewModel.translateExcerpt.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val userscripts by viewModel.userscripts.collectAsStateWithLifecycle()
    val watchlistItems by viewModel.watchlistItems.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val rssFeeds by viewModel.rssFeeds.collectAsStateWithLifecycle()
    val rssArticles by viewModel.rssArticles.collectAsStateWithLifecycle()
    val activeDownloadSpeeds by viewModel.activeDownloadSpeeds.collectAsStateWithLifecycle()
    val splitTab by viewModel.splitTab.collectAsStateWithLifecycle()
    val commandPaletteOpen by viewModel.commandPaletteOpen.collectAsStateWithLifecycle()
    val structuredViewerState by viewModel.structuredViewerState.collectAsStateWithLifecycle()
    val pendingHighlightRequest by viewModel.pendingHighlightRequest.collectAsStateWithLifecycle()
    val userscriptsDialogOpen by viewModel.userscriptsDialogOpen.collectAsStateWithLifecycle()
    val watchlistDialogOpen by viewModel.watchlistDialogOpen.collectAsStateWithLifecycle()
    val homographWarning by viewModel.homographWarning.collectAsStateWithLifecycle()
    val webPermissionPrompt by viewModel.webPermissionPrompt.collectAsStateWithLifecycle()

    var showQuickControls by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var addBookmarkUrlEdit by remember { mutableStateOf<Boolean?>(null) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSiteInfoDialog by remember { mutableStateOf(false) }
    var pendingRememberDomainPerm by remember { mutableStateOf(false) }

    val openLocalFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.openUriInUniversalViewer(it) } }

    val webSensorPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val anyGranted = grants.values.any { it }
        viewModel.resolveWebPermissionPrompt(allow = anyGranted, rememberForDomain = pendingRememberDomainPerm)
        if (!anyGranted) {
            viewModel.showMessage("Android OS sensor permission denied")
        }
    }

    val isCurrentDomainEphemeral = remember(activeTab.domain, websiteSettings, preferences.ephemeralContainerDefault) {
        viewModel.shieldsEngine.isEphemeralContainerForDomain(activeTab.domain)
    }

    LaunchedEffect(preferences.screenSecurityPrivateEnabled, activeTab.isPrivate, isCurrentDomainEphemeral) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val shouldSecure = preferences.screenSecurityPrivateEnabled &&
            (activeTab.isPrivate || isCurrentDomainEphemeral)
        if (shouldSecure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

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

    LaunchedEffect(activeTab.isPlayingMedia, activeTab.hasActiveMedia, activeTab.isHome) {
        onSyncPipParams(activeTab.isPlayingMedia, activeTab.hasActiveMedia, activeTab.isHome)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: RitamRoutes.BROWSER

    LaunchedEffect(isInPipMode, currentRoute) {
        if (isInPipMode && currentRoute != RitamRoutes.BROWSER) {
            navController.popBackStack(RitamRoutes.BROWSER, inclusive = false)
        }
    }

    val isBrowserFocused = currentRoute == RitamRoutes.BROWSER &&
        !showTabSwitcher &&
        !showQuickControls &&
        !commandPaletteOpen &&
        !devToolsVisible &&
        !translateDialogOpen &&
        !userscriptsDialogOpen &&
        !watchlistDialogOpen &&
        addBookmarkUrlEdit == null &&
        !showClearDataDialog &&
        !showSiteInfoDialog &&
        pageSourceState == null &&
        pdfViewerState == null &&
        universalViewerState == null &&
        structuredViewerState == null &&
        pendingHighlightRequest == null &&
        homographWarning == null &&
        webPermissionPrompt == null &&
        sslWarning == null &&
        maliciousWarning == null &&
        qrShareUrl == null &&
        longPressContext == null &&
        externalAppRequest == null

    val backdropModifier = if (showTabSwitcher && preferences.surfaceBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(10.dp)
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().then(backdropModifier)) {
            BrowserScreen(
                activeTab = activeTab,
                tabs = tabs,
                tabsCount = tabs.size,
                isBrowserFocused = isBrowserFocused,
                preferences = preferences,
                homeBookmarks = homeBookmarks,
                topSites = topSites,
                allBookmarks = allBookmarks,
                recentSearches = recentSearches,
                webViewManager = viewModel.webViewManager,
                findInPageActive = findInPageActive,
                findInPageQuery = findInPageQuery,
                readerFontSizeSp = readerFontSizeSp,
                readAloudState = readAloudState,
                externalOpenOmniboxTrigger = externalOpenOmniboxTrigger,
                looksLikeUrl = { viewModel.looksLikeUrl(it) },
                onSelectSearchEngine = { viewModel.updateSearchEngine(it) },
                onCopyAddress = { url -> viewModel.copyToClipboard(context, url, "Address") },
                onNavigateInput = { viewModel.navigateToInput(it) },
                onNavigateTab = { tabId, url -> viewModel.navigateToInputOnTab(tabId, url) },
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
                onOpenSettings = {
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
                isInPipMode = isInPipMode,
                fullscreenVideoView = fullscreenVideoView,
                onExitFullscreenVideo = { viewModel.exitFullscreenVideo() },
                onRevealToolbars = { viewModel.revealToolbars() },
                onToggleTabMedia = { viewModel.toggleTabMediaPlayback() },
                onEnterPip = onEnterPipMode,
                onShareQrCode = { url -> viewModel.showQrForCurrentOrUrl(url) },
                onAddToReadingList = { viewModel.addCurrentPageToReadingList() },
                splitTab = splitTab,
                onSwapSplitPanes = { viewModel.swapSplitPanes() },
                onCloseSplitPane = { viewModel.closeSplitPane() },
                onSelectSplitPaneFocus = { viewModel.swapSplitPanes() },
                onChangeMediaSpeedBoost = { spd, boost -> viewModel.setTabMediaSpeedAndBoost(spd, boost) },
                onSubscribeDiscoveredFeed = { feedUrl -> viewModel.subscribeRssFeed(feedUrl) },
                onOpenCommandPalette = { viewModel.setCommandPaletteOpen(true) },
                onToggleBionicReading = { viewModel.updateBionicReading(!preferences.bionicReadingEnabled) },
                onAdjustReaderWidth = { viewModel.updateReaderColumnWidthDp(it) },
                onAdjustReaderLineHeight = { viewModel.updateReaderLineHeightMultiplier(it) },
                onSaveHighlight = { viewModel.openHighlightCapture() }
            )

            NavHost(
                navController = navController,
                startDestination = RitamRoutes.BROWSER,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(RitamRoutes.BROWSER) {
                    // Persistent BrowserScreen stays mounted underneath so background videos and tabs never pause
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
                    onClearReadLaterDone = { viewModel.clearReadReadingList() },
                    activeDownloadSpeeds = activeDownloadSpeeds,
                    onPauseDownload = { viewModel.pauseDownload(it) },
                    onResumeDownload = { viewModel.resumeDownload(it) },
                    rssFeeds = rssFeeds,
                    rssArticles = rssArticles,
                    onSubscribeFeed = { feedUrl -> viewModel.subscribeRssFeed(feedUrl) },
                    onRefreshFeeds = { viewModel.refreshAllRssFeeds() },
                    onToggleFeedArticleRead = { id, read -> viewModel.setRssArticleRead(id, read) },
                    onDeleteFeed = { id -> viewModel.deleteRssFeed(id) },
                    highlights = highlights,
                    onDeleteHighlight = { id -> viewModel.deleteHighlight(id) },
                    onExportHighlightsUri = { uri -> viewModel.exportHighlightsToUri(context, uri) },
                    onOpenLocalFile = { uri -> viewModel.openUriInUniversalViewer(uri) }
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
                    onToggleLocalTelemetry = { viewModel.updateLocalTelemetry(it) },
                    onOpenBookmarksManager = {
                        navController.navigate(RitamRoutes.library(LibraryTab.BOOKMARKS))
                    },
                    onOpenClearData = { showClearDataDialog = true },
                    onToggleForceWebDark = { viewModel.updateForceWebDarkMode(it) },
                    onToggleBottomOmnibox = { viewModel.updateBottomOmnibox(it) },
                    onToggleAutoHideToolbars = { viewModel.updateAutoHideToolbarsOnScroll(it) },
                    onToggleClearOnExit = { viewModel.updateClearOnExit(it) },
                    onChangeAutoCloseDays = { viewModel.updateAutoCloseTabsDays(it) },
                    onSelectTranslateLanguage = { viewModel.updatePreferredTranslateLanguage(it) },
                    onExportConfigBackupUri = { uri -> viewModel.exportConfigBackupToUri(context, uri) },
                    onImportConfigBackupUri = { uri -> viewModel.importConfigBackupFromUri(context, uri) },
                    onOpenWatchlist = { viewModel.setWatchlistDialogOpen(true) },
                    onOpenFeeds = { navController.navigate(RitamRoutes.library(LibraryTab.FEEDS)) },
                    cacheStats = cacheStats,
                    topSites = topSites,
                    onClearAppCache = { viewModel.clearAppCacheOnly() },
                    onClearSiteUsage = { viewModel.clearSiteUsageAnalytics() },
                    onOpenSiteUrl = { url ->
                        navController.popBackStack(RitamRoutes.BROWSER, inclusive = false)
                        viewModel.navigateToInput(url)
                    }
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
            val quickControlUsageMap = remember(quickControlUsage) {
                quickControlUsage.associateBy { it.controlId }
            }
            QuickControlsSheet(
                tab = activeTab,
                preferences = preferences,
                onDismiss = { showQuickControls = false },
                onOpenBookmarks = { navController.navigate(RitamRoutes.library(LibraryTab.BOOKMARKS)) },
                onOpenHistory = { navController.navigate(RitamRoutes.library(LibraryTab.HISTORY)) },
                onOpenDownloads = { navController.navigate(RitamRoutes.library(LibraryTab.DOWNLOADS)) },
                onOpenSettings = { section ->
                    val target = when (section) {
                        "analytics" -> SettingsFocusSection.ANALYTICS
                        else -> SettingsFocusSection.ALL
                    }
                    navController.navigate(RitamRoutes.settings(target))
                },
                onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                onToggleReaderMode = { viewModel.toggleReaderMode() },
                onToggleFindInPage = { viewModel.setFindInPageActive(true) },
                onSharePage = { viewModel.shareCurrentPage(context) },
                onToggleDarkMode = { viewModel.toggleDarkModeQuick() },
                onBookmarkCurrent = { addBookmarkUrlEdit = false },
                onScreenshot = { viewModel.captureAndShareScreenshot(rootView, context) },
                onPrintPage = { viewModel.printCurrentPage(context) },
                onViewSource = { viewModel.openPageSource() },
                onSavePage = { viewModel.saveCurrentPageOffline() },
                onReadAloud = { viewModel.startReadAloud() },
                onToggleForceWebDark = { viewModel.updateForceWebDarkMode(!preferences.forceWebDarkMode) },
                onOpenTranslate = { viewModel.openTranslateDialog() },
                onAddToReadingList = { viewModel.addCurrentPageToReadingList() },
                onShowQrCode = { viewModel.showQrForCurrentOrUrl() },
                onBurnSession = { viewModel.burnSession() },
                onEnterPip = onEnterPipMode,
                onOpenDevTools = { viewModel.openDevTools(initialTab = 1) },
                onToggleBottomOmnibox = { viewModel.updateBottomOmnibox(!preferences.bottomOmnibox) },
                onChangePageZoom = { zoom -> viewModel.setTabZoom(zoom) },
                splitActive = splitTab != null,
                onOpenCommandPalette = { viewModel.setCommandPaletteOpen(true) },
                onToggleSplitScreen = { viewModel.toggleSplitScreen() },
                onFullPageScreenshot = { viewModel.captureFullPageScreenshot(rootView, context) },
                onHighlightSelection = { viewModel.openHighlightCapture() },
                onOpenWatchlist = { viewModel.setWatchlistDialogOpen(true) },
                onOpenUserscripts = { viewModel.setUserscriptsDialogOpen(true) },
                onOpenStructuredViewer = { viewModel.openStructuredViewerForCurrentPage() },
                onInstallWebApp = { viewModel.installCurrentSiteAsPwa(context) },
                onOpenFeeds = { navController.navigate(RitamRoutes.library(LibraryTab.FEEDS)) },
                usageMap = quickControlUsageMap,
                onRecordActionUse = { id -> viewModel.recordQuickControlUse(id) },
                onOpenLocalFile = { openLocalFileLauncher.launch(arrayOf("*/*")) }
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
                jsEnabled = currentSiteSetting?.javascriptEnabled ?: preferences.javascriptEnabled,
                zoomPercent = currentSiteSetting?.zoomPercent ?: activeTab.zoomPercent,
                forceDarkOverride = currentSiteSetting?.forceDarkOverride ?: -1,
                onSave = { js, zoomPercent, forceDarkOverride ->
                    if (cleanDomain.isNotBlank()) {
                        viewModel.upsertWebsiteSetting(
                            domain = cleanDomain,
                            shieldsEnabled = true,
                            jsEnabled = js,
                            httpsOnly = true,
                            zoomPercent = zoomPercent,
                            forceDarkOverride = forceDarkOverride,
                            isEphemeralContainer = false
                        )
                        viewModel.reloadOrStop()
                    }
                    showSiteInfoDialog = false
                },
                onDismiss = { showSiteInfoDialog = false }
            )
        }

        pageSourceState?.let { srcState ->
            PageSourceDialog(
                state = srcState,
                onCopySource = { html -> viewModel.copyToClipboard(context, html, "Page HTML Source") },
                onDismiss = { viewModel.dismissPageSource() }
            )
        }

        pdfViewerState?.let { pdfState ->
            PdfViewerDialog(
                state = pdfState,
                onOpenExternal = { file -> viewModel.openPdfExternally(context, file) },
                onDismiss = { viewModel.dismissPdfViewer() }
            )
        }

        universalViewerState?.let { uvState ->
            UniversalViewerDialog(
                state = uvState,
                onCopyText = { txt -> viewModel.copyToClipboard(context, txt, "Document text") },
                onOpenExternal = { file -> viewModel.openFileExternally(context, file, null, uvState.mimeType) },
                onDismiss = { viewModel.dismissUniversalViewer() }
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
                onDismiss = { viewModel.dismissDevTools() },
                initialTab = devToolsInitialTab,
                pageUrl = activeTab.url,
                pageSourceHtml = devToolsPageSource,
                structuredState = cdpFormatState,
                cacheStats = cacheStats,
                onRefreshSource = { viewModel.refreshDevSource() },
                onRefreshFormat = { viewModel.refreshDevFormat() },
                onClearAppCache = { viewModel.clearAppCacheOnly() },
                onCopyText = { text, label -> viewModel.copyToClipboard(context, text, label) }
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

        adultBlockNotice?.let { notice ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissAdultBlockNotice() },
                title = { Text("Content not allowed") },
                text = {
                    Text(
                        "This content is not allowed on our platform.\n\n" +
                            "Pornographic and nude material is blocked for everyone's safety, and the " +
                            "request was stopped before anything was loaded.\n\n" +
                            "Try a different search if you were looking for something else."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissAdultBlockNotice() }) {
                        Text("OK")
                    }
                }
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

        if (commandPaletteOpen) {
            CommandPaletteDialog(
                tabs = tabs,
                bookmarks = allBookmarks,
                readingList = readingList,
                history = history,
                activeTab = activeTab,
                splitActive = splitTab != null,
                onSelectTab = { tabId ->
                    navController.popBackStack(RitamRoutes.BROWSER, inclusive = false)
                    viewModel.switchTab(tabId)
                },
                onOpenUrl = { input ->
                    navController.popBackStack(RitamRoutes.BROWSER, inclusive = false)
                    viewModel.navigateToInput(input)
                },
                onActionCommand = { actionId ->
                    when (actionId) {
                        "split_view" -> viewModel.toggleSplitScreen()
                        "full_screenshot" -> viewModel.captureFullPageScreenshot(rootView, context)
                        "highlight" -> viewModel.openHighlightCapture()
                        "pretty_viewer" -> viewModel.openStructuredViewerForCurrentPage()
                        "watchlist" -> viewModel.setWatchlistDialogOpen(true)
                        "userscripts" -> viewModel.setUserscriptsDialogOpen(true)
                        "pwa_install" -> viewModel.installCurrentSiteAsPwa(context)
                        "rss_feeds" -> navController.navigate(RitamRoutes.library(LibraryTab.FEEDS))
                        "toggle_dark" -> viewModel.toggleDarkModeQuick()
                        "toggle_desktop" -> viewModel.toggleDesktopMode()
                        "burn_session" -> viewModel.burnSession()
                        "open_file" -> openLocalFileLauncher.launch(arrayOf("*/*"))
                    }
                },
                onDismiss = { viewModel.setCommandPaletteOpen(false) }
            )
        }

        structuredViewerState?.let { viewerState ->
            StructuredViewerDialog(
                state = viewerState,
                onCopy = { formatted -> viewModel.copyToClipboard(context, formatted, "Formatted Output") },
                onDismiss = { viewModel.dismissStructuredViewer() }
            )
        }

        if (userscriptsDialogOpen) {
            UserscriptsDialog(
                scripts = userscripts,
                userscriptsEnabled = preferences.userscriptsEnabled,
                onToggleGlobal = { viewModel.updateUserscriptsEnabled(it) },
                onAddScript = { name, pattern, code, isCss -> viewModel.addUserscript(name, pattern, code, isCss) },
                onToggleScript = { id, enabled -> viewModel.setUserscriptEnabled(id, enabled) },
                onDeleteScript = { id -> viewModel.deleteUserscript(id) },
                onDismiss = { viewModel.setUserscriptsDialogOpen(false) }
            )
        }

        if (watchlistDialogOpen) {
            WatchlistDialog(
                items = watchlistItems,
                canAddCurrent = !activeTab.isHome && activeTab.url.isNotBlank(),
                onAddCurrent = { viewModel.addCurrentPageToWatchlist() },
                onCheckNow = { viewModel.checkWatchlistNow() },
                onOpenItem = { item ->
                    viewModel.markWatchlistSeen(item.id)
                    viewModel.setWatchlistDialogOpen(false)
                    navController.popBackStack(RitamRoutes.BROWSER, inclusive = false)
                    viewModel.navigateToInput(item.url)
                },
                onDeleteItem = { id -> viewModel.deleteWatchlistItem(id) },
                onDismiss = { viewModel.setWatchlistDialogOpen(false) }
            )
        }

        pendingHighlightRequest?.let { req ->
            HighlightNoteDialog(
                request = req,
                onSave = { quote, note, colorHex -> viewModel.saveHighlight(quote, note, colorHex) },
                onDismiss = { viewModel.dismissHighlightCapture() }
            )
        }

        homographWarning?.let { warning ->
            HomographWarningDialog(
                warning = warning,
                onGoBack = { viewModel.dismissHomographWarning() },
                onProceedAnyway = { viewModel.bypassHomographAndLoad() }
            )
        }

        webPermissionPrompt?.let { promptReq ->
            val requestOsAndResolve = { rememberDomain: Boolean ->
                val neededOs = promptReq.resourceLabels.flatMap { label ->
                    when (label) {
                        "CAMERA" -> listOf(Manifest.permission.CAMERA)
                        "MICROPHONE" -> listOf(Manifest.permission.RECORD_AUDIO)
                        "LOCATION" -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        else -> emptyList()
                    }
                }.distinct().filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (neededOs.isEmpty()) {
                    viewModel.resolveWebPermissionPrompt(allow = true, rememberForDomain = rememberDomain)
                } else {
                    pendingRememberDomainPerm = rememberDomain
                    webSensorPermissionLauncher.launch(neededOs.toTypedArray())
                }
            }
            WebPermissionPromptDialog(
                request = promptReq,
                onAllowOnceThisTab = { requestOsAndResolve(false) },
                onAlwaysAllow = { requestOsAndResolve(true) },
                onDeny = { viewModel.resolveWebPermissionPrompt(allow = false, rememberForDomain = false) }
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
