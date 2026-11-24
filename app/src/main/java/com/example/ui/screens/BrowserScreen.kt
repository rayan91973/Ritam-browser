package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.SanctuaryReaderSamples
import com.example.browser.SiteShortcutPlanner
import com.example.browser.TabState
import com.example.browser.WebViewManager
import com.example.data.local.BookmarkEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.SiteUsageEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.SearchEngine
import com.example.services.ReadAloudState
import com.example.ui.components.ActiveOmniboxHeader
import com.example.ui.components.BottomNavDock
import com.example.ui.components.FindInPageBar
import com.example.ui.components.IncognitoColors
import com.example.ui.components.IncognitoSpectsEmblem
import com.example.ui.components.LotusEmblem
import com.example.ui.components.OmniboxSearchOverlay
import com.example.ui.components.ReadAloudFloatingBar
import com.example.ui.components.SearchEngineSelectorPillButton
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    activeTab: TabState,
    tabs: List<TabState> = listOf(activeTab),
    tabsCount: Int = tabs.size,
    isBrowserFocused: Boolean = true,
    preferences: BrowserPreferences,
    homeBookmarks: List<BookmarkEntity>,
    topSites: List<SiteUsageEntity> = emptyList(),
    allBookmarks: List<BookmarkEntity>,
    recentSearches: List<SearchHistoryEntity>,
    webViewManager: WebViewManager,
    findInPageActive: Boolean,
    findInPageQuery: String,
    readerFontSizeSp: Int,
    readAloudState: ReadAloudState = ReadAloudState(),
    externalOpenOmniboxTrigger: Int = 0,
    looksLikeUrl: (String) -> Boolean,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onCopyAddress: (String) -> Unit,
    onNavigateInput: (String) -> Unit,
    onNavigateTab: (String, String) -> Unit = { _, _ -> },
    onOpenSanctuaryEssay: () -> Unit,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onReloadOrStop: () -> Unit,
    onSwipeTab: (Int) -> Unit = {},
    onDoubleTapAddress: () -> Unit = {},
    onToggleReaderMode: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onAdjustReaderFontSize: (Int) -> Unit,
    onFindQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onCloseFind: () -> Unit,
    onToggleReadAloudPlayPause: () -> Unit = {},
    onPrevReadAloud: () -> Unit = {},
    onNextReadAloud: () -> Unit = {},
    onCycleReadAloudSpeed: () -> Unit = {},
    onStopReadAloud: () -> Unit = {},
    onOpenTabSwitcher: () -> Unit,
    onOpenQuickControls: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAddBookmark: (allowUrlEdit: Boolean) -> Unit,
    onOpenBookmarksManager: () -> Unit,
    onOpenSiteInfo: () -> Unit,
    onVoiceSearch: () -> Unit,
    onScanQr: () -> Unit,
    isToolbarHiddenOnScroll: Boolean = false,
    isInPipMode: Boolean = false,
    fullscreenVideoView: View? = null,
    onExitFullscreenVideo: () -> Unit = {},
    onRevealToolbars: () -> Unit = {},
    onToggleTabMedia: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onShareQrCode: (String) -> Unit = {},
    onAddToReadingList: () -> Unit = {},
    splitTab: TabState? = null,
    onSwapSplitPanes: () -> Unit = {},
    onCloseSplitPane: () -> Unit = {},
    onSelectSplitPaneFocus: () -> Unit = {},
    onChangeMediaSpeedBoost: (Float, Float) -> Unit = { _, _ -> },
    onSubscribeDiscoveredFeed: (String) -> Unit = {},
    onOpenCommandPalette: () -> Unit = {},
    onToggleBionicReading: () -> Unit = {},
    onAdjustReaderWidth: (Int) -> Unit = {},
    onAdjustReaderLineHeight: (Float) -> Unit = {},
    onSaveHighlight: () -> Unit = {}
) {
    var showOmniboxOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(externalOpenOmniboxTrigger) {
        if (externalOpenOmniboxTrigger > 0) {
            showOmniboxOverlay = true
        }
    }

    BackHandler(
        enabled = isBrowserFocused &&
            (fullscreenVideoView != null || showOmniboxOverlay || findInPageActive || !activeTab.isHome || activeTab.isReaderMode)
    ) {
        when {
            fullscreenVideoView != null -> onExitFullscreenVideo()
            showOmniboxOverlay -> showOmniboxOverlay = false
            findInPageActive -> onCloseFind()
            else -> onBack()
        }
    }

    val isCurrentBookmarked = remember(activeTab.url, allBookmarks) {
        allBookmarks.any { it.url.equals(activeTab.url, ignoreCase = true) }
    }

    val showChrome = !isInPipMode && (!isToolbarHiddenOnScroll || activeTab.isHome || activeTab.isReaderMode || findInPageActive)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (activeTab.isPrivate) IncognitoColors.Backdrop else MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!activeTab.isHome && !preferences.bottomOmnibox && !activeTab.isStandalonePwa) {
                AnimatedVisibility(
                    visible = showChrome,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    ActiveOmniboxHeader(
                        tab = activeTab,
                        gestureSwipeTabsEnabled = preferences.gestureSwipeTabsEnabled,
                        gesturePullRefreshEnabled = preferences.gesturePullRefreshEnabled,
                        gestureDoubleTapTopEnabled = preferences.gestureDoubleTapTopEnabled,
                        onTapAddress = { showOmniboxOverlay = true },
                        onDoubleTapAddress = onDoubleTapAddress,
                        onSwipeTab = onSwipeTab,
                        onTapSiteInfo = onOpenSiteInfo,
                        onToggleReaderMode = onToggleReaderMode,
                        onToggleDesktopMode = onToggleDesktopMode,
                        onReloadOrStop = onReloadOrStop,
                        isBottomDocked = false
                    )
                }
            }

            if (!activeTab.isHome && findInPageActive && !isInPipMode) {
                FindInPageBar(
                    query = findInPageQuery,
                    currentMatch = activeTab.findMatchCurrent,
                    totalMatches = activeTab.findMatchTotal,
                    isPrivate = activeTab.isPrivate,
                    onQueryChange = onFindQueryChange,
                    onPrev = { onFindNext(false) },
                    onNext = { onFindNext(true) },
                    onClose = onCloseFind
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (!activeTab.isHome && (preferences.bottomOmnibox || !showChrome || activeTab.isStandalonePwa) && !isInPipMode) {
                            Modifier.statusBarsPadding()
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (splitTab != null && !activeTab.isHome && !activeTab.isReaderMode) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val sideBySide = maxWidth >= 600.dp
                        if (sideBySide) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    PersistentMultiTabWebViewStage(
                                        tabs = tabs,
                                        activeTabId = activeTab.id,
                                        excludedTabIds = setOf(splitTab.id),
                                        touchesEnabled = isBrowserFocused,
                                        preferences = preferences,
                                        webViewManager = webViewManager
                                    )
                                }
                                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    SplitSecondaryPane(
                                        splitTab = splitTab,
                                        preferences = preferences,
                                        webViewManager = webViewManager,
                                        onFocusPane = onSelectSplitPaneFocus,
                                        onSwap = onSwapSplitPanes,
                                        onClose = onCloseSplitPane,
                                        onNavigateUrl = onNavigateTab,
                                        onGoBack = { webViewManager.goBack(it) },
                                        onGoForward = { webViewManager.goForward(it) },
                                        onReload = { webViewManager.reload(it) }
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    PersistentMultiTabWebViewStage(
                                        tabs = tabs,
                                        activeTabId = activeTab.id,
                                        excludedTabIds = setOf(splitTab.id),
                                        touchesEnabled = isBrowserFocused,
                                        preferences = preferences,
                                        webViewManager = webViewManager
                                    )
                                }
                                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    SplitSecondaryPane(
                                        splitTab = splitTab,
                                        preferences = preferences,
                                        webViewManager = webViewManager,
                                        onFocusPane = onSelectSplitPaneFocus,
                                        onSwap = onSwapSplitPanes,
                                        onClose = onCloseSplitPane,
                                        onNavigateUrl = onNavigateTab,
                                        onGoBack = { webViewManager.goBack(it) },
                                        onGoForward = { webViewManager.goForward(it) },
                                        onReload = { webViewManager.reload(it) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    PersistentMultiTabWebViewStage(
                        tabs = tabs,
                        activeTabId = if (!activeTab.isHome && !activeTab.isReaderMode) activeTab.id else null,
                        excludedTabIds = emptySet(),
                        touchesEnabled = isBrowserFocused && !activeTab.isHome && !activeTab.isReaderMode,
                        preferences = preferences,
                        webViewManager = webViewManager
                    )
                }

                if (activeTab.isHome) {
                    BrowserHomeContent(
                        isPrivate = activeTab.isPrivate,
                        searchEngine = preferences.searchEngine,
                        homeBookmarks = homeBookmarks,
                        topSites = topSites,
                        onSelectSearchEngine = onSelectSearchEngine,
                        onTapOmnibox = { showOmniboxOverlay = true },
                        onTapBookmark = { url -> onNavigateInput(url) },
                        onAddHomeBookmark = { onOpenAddBookmark(true) },
                        onOpenSanctuaryEssay = onOpenSanctuaryEssay,
                        onOpenSettings = onOpenSettings,
                        onOpenCommandPalette = onOpenCommandPalette,
                        onVoiceSearch = onVoiceSearch,
                        onScanQr = onScanQr,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (activeTab.isPrivate) IncognitoColors.Backdrop else MaterialTheme.colorScheme.background)
                    )
                } else {
                    if (activeTab.loadError != null && !activeTab.isReaderMode) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(28.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Unable to Reach Site",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = activeTab.loadError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(onClick = onHome) { Text("Home") }
                                    Button(onClick = onReloadOrStop) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Try Again")
                                    }
                                }
                            }
                        }
                    }

                    if (activeTab.isReaderMode) {
                        val article = activeTab.readerArticle ?: SanctuaryReaderSamples.ARCHITECTURE_ESSAY
                        ReaderModeView(
                            article = article,
                            fontSizeSp = readerFontSizeSp,
                            isBookmarked = isCurrentBookmarked,
                            onAdjustFontSize = onAdjustReaderFontSize,
                            onBookmarkArticle = { onOpenAddBookmark(false) },
                            onOrganizeBookmarks = onOpenBookmarksManager,
                            onExitReader = onToggleReaderMode,
                            bionicReadingEnabled = preferences.bionicReadingEnabled,
                            columnWidthDp = preferences.readerColumnWidthDp,
                            lineHeightMultiplier = preferences.readerLineHeightMultiplier,
                            onToggleBionicReading = onToggleBionicReading,
                            onAdjustColumnWidth = onAdjustReaderWidth,
                            onAdjustLineHeight = onAdjustReaderLineHeight,
                            onSaveHighlight = onSaveHighlight
                        )
                    }

                    // Active Temporary Sensor Grant Badge (Camera / Mic / Location for this tab only)
                    if (activeTab.activeSensorGrants.isNotEmpty() && !activeTab.isReaderMode && !isInPipMode) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("temporary_sensor_grant_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Temp access: ${activeTab.activeSensorGrants.joinToString(", ")} • Auto-revokes",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Active Media, Speed, Boost & PiP Quick Pill
                    if (activeTab.hasActiveMedia && !isInPipMode && !activeTab.isReaderMode) {
                        val speedSteps = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
                        val nextSpeed = speedSteps.getOrElse(
                            (speedSteps.indexOfFirst { kotlin.math.abs(it - activeTab.mediaSpeed) < 0.05f } + 1)
                        ) { 1.0f }
                        val nextBoost = if (activeTab.audioBoost > 1.2f) 1.0f else 2.0f

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(onClick = onToggleTabMedia, modifier = Modifier.size(34.dp)) {
                                    Icon(
                                        imageVector = if (activeTab.isPlayingMedia) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (activeTab.isPlayingMedia) "Pause webpage media" else "Play webpage media",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Surface(
                                    onClick = { onChangeMediaSpeedBoost(nextSpeed, activeTab.audioBoost) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "${String.format("%.2g", activeTab.mediaSpeed)}x",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    onClick = { onChangeMediaSpeedBoost(activeTab.mediaSpeed, nextBoost) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (activeTab.audioBoost > 1.2f) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.VolumeUp,
                                            contentDescription = "Audio Boost",
                                            tint = if (activeTab.audioBoost > 1.2f) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (activeTab.audioBoost > 1.2f) "2x" else "Boost",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (activeTab.audioBoost > 1.2f) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = onEnterPip, modifier = Modifier.size(34.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureInPictureAlt,
                                        contentDescription = "Enter Picture-in-Picture",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Discovered RSS / Atom Feed Chip
                    val discoveredFeed = activeTab.discoveredFeedUrl
                    if (!discoveredFeed.isNullOrBlank() && !isInPipMode && !activeTab.isReaderMode) {
                        Surface(
                            onClick = { onSubscribeDiscoveredFeed(discoveredFeed) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 10.dp, bottom = if (!showChrome) 56.dp else 10.dp)
                                .testTag("discovered_rss_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.RssFeed,
                                    contentDescription = "Subscribe to RSS Feed",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Follow RSS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Reveal Toolbar Pill when auto-hidden on scroll
                    if (!showChrome && !isInPipMode) {
                        Surface(
                            onClick = onRevealToolbars,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(10.dp)
                                .size(38.dp)
                                .testTag("reveal_toolbars_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Show address bar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!activeTab.isHome && preferences.bottomOmnibox && !activeTab.isStandalonePwa) {
                AnimatedVisibility(
                    visible = showChrome,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    ActiveOmniboxHeader(
                        tab = activeTab,
                        gestureSwipeTabsEnabled = preferences.gestureSwipeTabsEnabled,
                        gesturePullRefreshEnabled = preferences.gesturePullRefreshEnabled,
                        gestureDoubleTapTopEnabled = preferences.gestureDoubleTapTopEnabled,
                        onTapAddress = { showOmniboxOverlay = true },
                        onDoubleTapAddress = onDoubleTapAddress,
                        onSwipeTab = onSwipeTab,
                        onTapSiteInfo = onOpenSiteInfo,
                        onToggleReaderMode = onToggleReaderMode,
                        onToggleDesktopMode = onToggleDesktopMode,
                        onReloadOrStop = onReloadOrStop,
                        isBottomDocked = true
                    )
                }
            }

            if (!isInPipMode) {
                ReadAloudFloatingBar(
                    state = readAloudState,
                    isPrivate = activeTab.isPrivate,
                    onTogglePlayPause = onToggleReadAloudPlayPause,
                    onPrev = onPrevReadAloud,
                    onNext = onNextReadAloud,
                    onCycleSpeed = onCycleReadAloudSpeed,
                    onStop = onStopReadAloud
                )
            }

            AnimatedVisibility(
                visible = showChrome,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                BottomNavDock(
                    canGoBack = !activeTab.isHome || activeTab.canGoBack || activeTab.isReaderMode,
                    canGoForward = activeTab.canGoForward && !activeTab.isHome,
                    isHome = activeTab.isHome,
                    isPrivate = activeTab.isPrivate,
                    gestureSwipeNavEnabled = preferences.gestureSwipeNavEnabled,
                    tabCount = tabsCount,
                    topology = preferences.omniboxTopology,
                    onBack = { onBack() },
                    onForward = onForward,
                    onHome = onHome,
                    onOpenTabs = onOpenTabSwitcher,
                    onOpenQuickControls = onOpenQuickControls
                )
            }
        }

        if (showOmniboxOverlay) {
            OmniboxSearchOverlay(
                initialQuery = activeTab.url,
                currentPageTitle = activeTab.title,
                isPrivate = activeTab.isPrivate,
                stripTrackingParams = preferences.stripTrackingParams,
                searchEngine = preferences.searchEngine,
                recentSearches = recentSearches,
                bookmarks = allBookmarks,
                looksLikeUrl = looksLikeUrl,
                onSelectSearchEngine = onSelectSearchEngine,
                onCopyAddress = onCopyAddress,
                onShareQrCode = { url ->
                    showOmniboxOverlay = false
                    onShareQrCode(url)
                },
                onAddToReadingList = onAddToReadingList,
                onSubmitQuery = { q ->
                    showOmniboxOverlay = false
                    onNavigateInput(q)
                },
                onVoiceSearch = {
                    showOmniboxOverlay = false
                    onVoiceSearch()
                },
                onScanQr = {
                    showOmniboxOverlay = false
                    onScanQr()
                },
                onDismiss = { showOmniboxOverlay = false }
            )
        }

        fullscreenVideoView?.let { videoView ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF141917))
            ) {
                key(videoView) {
                    AndroidView(
                        factory = {
                            (videoView.parent as? ViewGroup)?.removeView(videoView)
                            videoView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private class MultiTabWebViewHostLayout(context: Context) : FrameLayout(context) {
    var activeTabId: String? = null
    var touchesEnabled: Boolean = true

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!touchesEnabled) return false
        val targetId = activeTabId ?: return false
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child.tag == targetId && child.visibility == View.VISIBLE && child.alpha > 0.1f) {
                return child.dispatchTouchEvent(ev)
            }
        }
        return false
    }
}

@Composable
private fun PersistentMultiTabWebViewStage(
    tabs: List<TabState>,
    activeTabId: String?,
    excludedTabIds: Set<String>,
    touchesEnabled: Boolean,
    preferences: BrowserPreferences,
    webViewManager: WebViewManager
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> MultiTabWebViewHostLayout(ctx) },
        update = { host: MultiTabWebViewHostLayout ->
            host.activeTabId = activeTabId
            host.touchesEnabled = touchesEnabled

            val liveTabs = tabs.filter { tab ->
                !tab.isHome &&
                    tab.id !in excludedTabIds &&
                    (tab.id == activeTabId || tab.isPlayingMedia || tab.hasActiveMedia || webViewManager.hasLiveWebView(tab.id))
            }
            val desiredIds = liveTabs.map { it.id }.toSet()

            // Remove closed or excluded WebViews from this host
            for (i in host.childCount - 1 downTo 0) {
                val child = host.getChildAt(i)
                val tagId = child.tag as? String
                if (tagId == null || tagId !in desiredIds) {
                    host.removeViewAt(i)
                }
            }

            // Attach & update each live tab WebView without detaching background tabs
            liveTabs.forEach { tab ->
                val webView = webViewManager.getOrCreateWebView(
                    tab = tab,
                    javascriptEnabled = preferences.javascriptEnabled,
                    blockThirdPartyCookies = preferences.blockThirdPartyCookies
                )
                webView.tag = tab.id
                if (webView.parent !== host) {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    host.addView(
                        webView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
                val isFocused = tab.id == activeTabId
                webView.visibility = View.VISIBLE
                webView.alpha = if (isFocused) 1f else 0f
                webView.translationZ = if (isFocused) 1f else 0f
                webView.isClickable = isFocused && touchesEnabled
                webView.isFocusable = isFocused && touchesEnabled
                webView.isFocusableInTouchMode = isFocused && touchesEnabled
                if (isFocused) {
                    webView.bringToFront()
                }
                if (webView.url.isNullOrBlank() && tab.url.isNotBlank()) {
                    webView.loadUrl(tab.url)
                }
            }
        }
    )
}

@Composable
private fun WebViewPaneContent(
    tab: TabState,
    preferences: BrowserPreferences,
    webViewManager: WebViewManager
) {
    key(tab.id) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                webViewManager.getOrCreateWebView(
                    tab = tab,
                    javascriptEnabled = preferences.javascriptEnabled,
                    blockThirdPartyCookies = preferences.blockThirdPartyCookies
                ).apply {
                    (parent as? ViewGroup)?.removeView(this)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    alpha = 1f
                    visibility = View.VISIBLE
                    if (url.isNullOrBlank() && tab.url.isNotBlank()) {
                        loadUrl(tab.url)
                    }
                }
            },
            update = { view: WebView ->
                webViewManager.applyDynamicSettings(
                    webView = view,
                    tab = tab,
                    javascriptEnabled = preferences.javascriptEnabled,
                    blockThirdPartyCookies = preferences.blockThirdPartyCookies
                )
            }
        )
    }
}

@Composable
private fun SplitSecondaryPane(
    splitTab: TabState,
    preferences: BrowserPreferences,
    webViewManager: WebViewManager,
    onFocusPane: () -> Unit,
    onSwap: () -> Unit,
    onClose: () -> Unit,
    onNavigateUrl: (String, String) -> Unit,
    onGoBack: (String) -> Unit,
    onGoForward: (String) -> Unit,
    onReload: (String) -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var urlInput by remember(splitTab.url) { mutableStateOf(splitTab.url) }
    var isEditingUrl by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = ext.surfaceLow,
            border = BorderStroke(0.5.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(
                            onClick = { onGoBack(splitTab.id) },
                            enabled = splitTab.canGoBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onGoForward(splitTab.id) },
                            enabled = splitTab.canGoForward,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onReload(splitTab.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reload", modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(2.dp))
                        if (isEditingUrl) {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 2.dp),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (urlInput.isNotBlank()) {
                                                onNavigateUrl(splitTab.id, urlInput)
                                                isEditingUrl = false
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = "Go", modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        } else {
                            Surface(
                                onClick = { isEditingUrl = true },
                                shape = RoundedCornerShape(8.dp),
                                color = ext.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = splitTab.domain.ifBlank { splitTab.title.ifBlank { splitTab.url } },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSwap, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap panes", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close split view", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            WebViewPaneContent(
                tab = splitTab,
                preferences = preferences,
                webViewManager = webViewManager
            )
        }
    }
}

@Composable
private fun BrowserHomeContent(
    isPrivate: Boolean,
    searchEngine: SearchEngine,
    homeBookmarks: List<BookmarkEntity>,
    topSites: List<SiteUsageEntity> = emptyList(),
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onTapOmnibox: () -> Unit,
    onTapBookmark: (String) -> Unit,
    onAddHomeBookmark: () -> Unit,
    onOpenSanctuaryEssay: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCommandPalette: () -> Unit = {},
    onVoiceSearch: () -> Unit,
    onScanQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalRitamExtendedColors.current
    val surfaceColor = if (isPrivate) IncognitoColors.Surface else ext.surfaceLow
    val borderColor = if (isPrivate) IncognitoColors.Border else ext.cardBorder
    val primaryText = if (isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface
    val mutedText = if (isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Ritam • BROWSER HOME / INCOGNITO + Command Palette + Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Ritam",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = NewsreaderFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = primaryText
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                if (isPrivate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IncognitoSpectsEmblem(
                            size = 14.dp,
                            tint = IncognitoColors.Accent,
                            hatFill = IncognitoColors.SurfaceElevated
                        )
                        Text(
                            text = "INCOGNITO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = IncognitoColors.Accent
                        )
                    }
                } else {
                    Text(
                        text = "BROWSER HOME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = mutedText
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onOpenCommandPalette,
                    shape = CircleShape,
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("home_command_palette_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = "Quick Command Palette",
                            tint = mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    onClick = onOpenSettings,
                    shape = CircleShape,
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("home_settings_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Centerpiece: Incognito Spectacles (Private) OR Lotus + Breathe & Discover (Regular)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPrivate) {
                Surface(
                    shape = CircleShape,
                    color = IncognitoColors.SurfaceElevated,
                    border = BorderStroke(1.dp, IncognitoColors.Border),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IncognitoSpectsEmblem(
                            size = 44.dp,
                            tint = IncognitoColors.Accent,
                            hatFill = IncognitoColors.Surface
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "You've gone Incognito",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NewsreaderFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal
                    ),
                    color = IncognitoColors.OnSurface
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = IncognitoColors.Surface,
                    border = BorderStroke(1.dp, IncognitoColors.Border),
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "• History, searches & cookies clear on tab close",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncognitoColors.OnSurfaceMuted
                        )
                        Text(
                            text = "• System protections & encrypted connections stay active",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncognitoColors.OnSurfaceMuted
                        )
                    }
                }
            } else {
                LotusEmblem(size = 58.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Breathe & Discover",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NewsreaderFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal
                    ),
                    color = primaryText
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = onOpenSanctuaryEssay,
                    shape = RoundedCornerShape(16.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.testTag("home_sanctuary_essay_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Sanctuary Essay • Reader Showcase",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedText
                        )
                    }
                }
            }
        }

        // Compact Search Pill with Search Engine Logo Switcher
        Surface(
            onClick = onTapOmnibox,
            shape = RoundedCornerShape(22.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 3.dp,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(44.dp)
                .testTag("home_search_pill")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchEngineSelectorPillButton(
                    selectedEngine = searchEngine,
                    onSelectEngine = onSelectSearchEngine,
                    isPrivate = isPrivate
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isPrivate) "Search ${searchEngine.displayName} privately" else "Search or enter address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onVoiceSearch,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("home_voice_button")
                ) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = "Voice search",
                        tint = mutedText,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onScanQr,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("home_qr_button")
                ) {
                    Icon(
                        Icons.Rounded.QrCodeScanner,
                        contentDescription = "Scan QR code",
                        tint = mutedText,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // PINNED & TOP SITES 4-column grid ranked by analytics + pinned bookmarks
        val homeShortcuts = remember(homeBookmarks, topSites) {
            SiteShortcutPlanner.mergePinnedAndTopSites(homeBookmarks, topSites, maxSlots = 7)
        }
        val topAnalyticsCount = remember(homeShortcuts) { homeShortcuts.count { it.isAnalyticsTop } }
        Row(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (topAnalyticsCount > 0) "PINNED & MOST VISITED" else "PINNED SITES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = mutedText
            )
            Text(
                text = if (topAnalyticsCount > 0) "${homeShortcuts.size} shortcuts • $topAnalyticsCount top" else "${homeShortcuts.size} pinned",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = mutedText
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(142.dp),
            contentPadding = PaddingValues(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(homeShortcuts, key = { it.key }) { shortcut ->
                HomeBookmarkTile(
                    title = shortcut.title,
                    monogram = shortcut.iconKey,
                    visitCount = shortcut.visitCount,
                    isPrivate = isPrivate,
                    onClick = { onTapBookmark(shortcut.url) }
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddHomeBookmark)
                        .padding(3.dp)
                        .testTag("home_add_bookmark_tile"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add Bookmark",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = mutedText,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBookmarkTile(
    title: String,
    monogram: String,
    visitCount: Long = 0L,
    isPrivate: Boolean = false,
    onClick: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    val surfaceColor = if (isPrivate) IncognitoColors.Surface else ext.surfaceLow
    val borderColor = if (isPrivate) IncognitoColors.Border else ext.cardBorder
    val accentColor = if (isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary
    val textColor = if (isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(3.dp)
            .testTag("home_bookmark_${title.lowercase().replace(" ", "_")}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = monogram,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = NewsreaderFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor
                    )
                }
            }
            if (visitCount > 0L) {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
