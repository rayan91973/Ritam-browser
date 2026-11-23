package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.browser.SanctuaryReaderSamples
import com.example.browser.TabState
import com.example.browser.WebViewManager
import com.example.data.local.BookmarkEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.SearchEngine
import com.example.services.ReadAloudState
import com.example.ui.components.ActiveOmniboxHeader
import com.example.ui.components.BiometricLockScreenOverlay
import com.example.ui.components.BottomNavDock
import com.example.ui.components.FindInPageBar
import com.example.ui.components.IncognitoColors
import com.example.ui.components.IncognitoSpectsEmblem
import com.example.ui.components.LotusEmblem
import com.example.ui.components.OmniboxSearchOverlay
import com.example.ui.components.RITAM_AVATAR_URL
import com.example.ui.components.ReadAloudFloatingBar
import com.example.ui.components.SearchEngineSelectorPillButton
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    activeTab: TabState,
    tabsCount: Int,
    preferences: BrowserPreferences,
    homeBookmarks: List<BookmarkEntity>,
    allBookmarks: List<BookmarkEntity>,
    recentSearches: List<SearchHistoryEntity>,
    webViewManager: WebViewManager,
    shieldsEnabledForSite: Boolean,
    sessionBlockedCount: Long,
    findInPageActive: Boolean,
    findInPageQuery: String,
    readerFontSizeSp: Int,
    readAloudState: ReadAloudState = ReadAloudState(),
    externalOpenOmniboxTrigger: Int = 0,
    looksLikeUrl: (String) -> Boolean,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onCopyAddress: (String) -> Unit,
    onNavigateInput: (String) -> Unit,
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
    onToggleSiteShields: () -> Unit,
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
    onOpenShieldSettings: () -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenAddBookmark: (allowUrlEdit: Boolean) -> Unit,
    onOpenBookmarksManager: () -> Unit,
    onOpenSiteInfo: () -> Unit,
    onVoiceSearch: () -> Unit,
    onScanQr: () -> Unit,
    isToolbarHiddenOnScroll: Boolean = false,
    isIncognitoLocked: Boolean = false,
    isInPipMode: Boolean = false,
    fullscreenVideoView: View? = null,
    onExitFullscreenVideo: () -> Unit = {},
    onUnlockIncognito: () -> Unit = {},
    onSwitchToRegularTab: () -> Unit = {},
    onRevealToolbars: () -> Unit = {},
    onToggleTabMedia: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onShareQrCode: (String) -> Unit = {},
    onAddToReadingList: () -> Unit = {}
) {
    var showOmniboxOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(externalOpenOmniboxTrigger) {
        if (externalOpenOmniboxTrigger > 0) {
            showOmniboxOverlay = true
        }
    }

    BackHandler(enabled = fullscreenVideoView != null || showOmniboxOverlay || findInPageActive || !activeTab.isHome || activeTab.isReaderMode) {
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
            if (activeTab.isHome) {
                BrowserHomeContent(
                    isPrivate = activeTab.isPrivate,
                    searchEngine = preferences.searchEngine,
                    sessionBlockedCount = sessionBlockedCount,
                    homeBookmarks = homeBookmarks,
                    onSelectSearchEngine = onSelectSearchEngine,
                    onTapOmnibox = { showOmniboxOverlay = true },
                    onTapBookmark = { url -> onNavigateInput(url) },
                    onAddHomeBookmark = { onOpenAddBookmark(true) },
                    onOpenSanctuaryEssay = onOpenSanctuaryEssay,
                    onOpenShields = onOpenShieldSettings,
                    onOpenProfile = onOpenProfileSettings,
                    onVoiceSearch = onVoiceSearch,
                    onScanQr = onScanQr,
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (!preferences.bottomOmnibox) {
                    AnimatedVisibility(
                        visible = showChrome,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        ActiveOmniboxHeader(
                            tab = activeTab,
                            shieldsEnabledForSite = shieldsEnabledForSite,
                            httpsOnlyEnabled = preferences.httpsOnlyEnabled,
                            gestureSwipeTabsEnabled = preferences.gestureSwipeTabsEnabled,
                            gesturePullRefreshEnabled = preferences.gesturePullRefreshEnabled,
                            gestureDoubleTapTopEnabled = preferences.gestureDoubleTapTopEnabled,
                            onTapAddress = { showOmniboxOverlay = true },
                            onDoubleTapAddress = onDoubleTapAddress,
                            onSwipeTab = onSwipeTab,
                            onTapSiteInfo = onOpenSiteInfo,
                            onToggleSiteShields = onToggleSiteShields,
                            onOpenShieldSettings = onOpenShieldSettings,
                            onToggleReaderMode = onToggleReaderMode,
                            onToggleDesktopMode = onToggleDesktopMode,
                            onReloadOrStop = onReloadOrStop,
                            isBottomDocked = false
                        )
                    }
                }

                if (findInPageActive && !isInPipMode) {
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
                            if ((preferences.bottomOmnibox || !showChrome) && !isInPipMode) {
                                Modifier.statusBarsPadding()
                            } else {
                                Modifier
                            }
                        )
                ) {
                    // Keep WebView alive behind Reader View so toggling back is instantaneous
                    key(activeTab.id) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = {
                                webViewManager.getOrCreateWebView(
                                    tab = activeTab,
                                    javascriptEnabled = preferences.javascriptEnabled,
                                    blockThirdPartyCookies = preferences.blockThirdPartyCookies
                                ).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    if (url.isNullOrBlank() && activeTab.url.isNotBlank()) {
                                        loadUrl(activeTab.url)
                                    }
                                }
                            },
                            update = { view: WebView ->
                                webViewManager.applyDynamicSettings(
                                    webView = view,
                                    tab = activeTab,
                                    javascriptEnabled = preferences.javascriptEnabled,
                                    blockThirdPartyCookies = preferences.blockThirdPartyCookies
                                )
                            }
                        )
                    }

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
                            onExitReader = onToggleReaderMode
                        )
                    }

                    // Active Media & PiP Quick Pill
                    if (activeTab.hasActiveMedia && !isInPipMode && !activeTab.isReaderMode) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                IconButton(onClick = onToggleTabMedia, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        imageVector = if (activeTab.isPlayingMedia) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (activeTab.isPlayingMedia) "Pause webpage media" else "Play webpage media",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = onEnterPip, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureInPictureAlt,
                                        contentDescription = "Enter Picture-in-Picture",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
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
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(14.dp)
                                .size(44.dp)
                                .testTag("reveal_toolbars_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Show address bar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (preferences.bottomOmnibox) {
                    AnimatedVisibility(
                        visible = showChrome,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        ActiveOmniboxHeader(
                            tab = activeTab,
                            shieldsEnabledForSite = shieldsEnabledForSite,
                            httpsOnlyEnabled = preferences.httpsOnlyEnabled,
                            gestureSwipeTabsEnabled = preferences.gestureSwipeTabsEnabled,
                            gesturePullRefreshEnabled = preferences.gesturePullRefreshEnabled,
                            gestureDoubleTapTopEnabled = preferences.gestureDoubleTapTopEnabled,
                            onTapAddress = { showOmniboxOverlay = true },
                            onDoubleTapAddress = onDoubleTapAddress,
                            onSwipeTab = onSwipeTab,
                            onTapSiteInfo = onOpenSiteInfo,
                            onToggleSiteShields = onToggleSiteShields,
                            onOpenShieldSettings = onOpenShieldSettings,
                            onToggleReaderMode = onToggleReaderMode,
                            onToggleDesktopMode = onToggleDesktopMode,
                            onReloadOrStop = onReloadOrStop,
                            isBottomDocked = true
                        )
                    }
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

        if (activeTab.isPrivate && isIncognitoLocked && preferences.biometricIncognitoLock && !isInPipMode) {
            BiometricLockScreenOverlay(
                onUnlock = onUnlockIncognito,
                onExitPrivateMode = onSwitchToRegularTab
            )
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
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { videoView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun BrowserHomeContent(
    isPrivate: Boolean,
    searchEngine: SearchEngine,
    sessionBlockedCount: Long,
    homeBookmarks: List<BookmarkEntity>,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onTapOmnibox: () -> Unit,
    onTapBookmark: (String) -> Unit,
    onAddHomeBookmark: () -> Unit,
    onOpenSanctuaryEssay: () -> Unit,
    onOpenShields: () -> Unit,
    onOpenProfile: () -> Unit,
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
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Ritam • BROWSER HOME / INCOGNITO + Shield + Profile Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ritam",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NewsreaderFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = primaryText
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                if (isPrivate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        IncognitoSpectsEmblem(
                            size = 16.dp,
                            tint = IncognitoColors.Accent,
                            hatFill = IncognitoColors.SurfaceElevated
                        )
                        Text(
                            text = "INCOGNITO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = IncognitoColors.Accent
                        )
                    }
                } else {
                    Text(
                        text = "BROWSER HOME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = mutedText
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onOpenShields,
                    shape = RoundedCornerShape(16.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("home_shield_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = "Ritam Shields",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        if (sessionBlockedCount > 0) {
                            Text(
                                text = sessionBlockedCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = primaryText
                            )
                        }
                    }
                }

                Surface(
                    onClick = onOpenProfile,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("home_profile_button")
                ) {
                    if (isPrivate) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(IncognitoColors.SurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            IncognitoSpectsEmblem(
                                size = 22.dp,
                                tint = IncognitoColors.Accent,
                                hatFill = IncognitoColors.Surface
                            )
                        }
                    } else {
                        AsyncImage(
                            model = RITAM_AVATAR_URL,
                            contentDescription = "Profile & Settings",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
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
                    modifier = Modifier.size(92.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IncognitoSpectsEmblem(
                            size = 64.dp,
                            tint = IncognitoColors.Accent,
                            hatFill = IncognitoColors.Surface
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "You've gone Incognito",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = NewsreaderFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal
                    ),
                    color = IncognitoColors.OnSurface
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IncognitoColors.Surface,
                    border = BorderStroke(1.dp, IncognitoColors.Border),
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• History, searches & cookies clear on tab close",
                            style = MaterialTheme.typography.bodySmall,
                            color = IncognitoColors.OnSurfaceMuted
                        )
                        Text(
                            text = "• Ritam Shields & HTTPS-Only stay fully active",
                            style = MaterialTheme.typography.bodySmall,
                            color = IncognitoColors.OnSurfaceMuted
                        )
                    }
                }
            } else {
                LotusEmblem(size = 82.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Breathe & Discover",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = NewsreaderFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal
                    ),
                    color = primaryText
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = onOpenSanctuaryEssay,
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.testTag("home_sanctuary_essay_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Sanctuary Essay • Reader Showcase",
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedText
                        )
                    }
                }
            }
        }

        // Thumb-anchored Search Pill with Search Engine Logo Switcher
        Surface(
            onClick = onTapOmnibox,
            shape = RoundedCornerShape(30.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(58.dp)
                .testTag("home_search_pill")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchEngineSelectorPillButton(
                    selectedEngine = searchEngine,
                    onSelectEngine = onSelectSearchEngine,
                    isPrivate = isPrivate
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isPrivate) "Search ${searchEngine.displayName} privately" else "Search or enter address",
                    style = MaterialTheme.typography.bodyLarge,
                    color = mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onVoiceSearch,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("home_voice_button")
                ) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = "Voice search",
                        tint = mutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onScanQr,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("home_qr_button")
                ) {
                    Icon(
                        Icons.Rounded.QrCodeScanner,
                        contentDescription = "Scan QR code",
                        tint = mutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // SANCTUARY BOOKMARKS 4-column grid
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SANCTUARY BOOKMARKS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = mutedText
            )
            Text(
                text = "${homeBookmarks.size} pinned",
                style = MaterialTheme.typography.labelSmall,
                color = mutedText
            )
        }

        Spacer(Modifier.height(10.dp))

        val visibleBookmarks = remember(homeBookmarks) { homeBookmarks.take(7) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(176.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false
        ) {
            items(visibleBookmarks, key = { it.id }) { bm ->
                HomeBookmarkTile(
                    title = bm.title,
                    monogram = bm.iconKey.take(2),
                    isPrivate = isPrivate,
                    onClick = { onTapBookmark(bm.url) }
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onAddHomeBookmark)
                        .padding(4.dp)
                        .testTag("home_add_bookmark_tile"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add Bookmark",
                                tint = accentColor
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelSmall,
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
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag("home_bookmark_${title.lowercase().replace(" ", "_")}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = monogram,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = NewsreaderFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accentColor
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
