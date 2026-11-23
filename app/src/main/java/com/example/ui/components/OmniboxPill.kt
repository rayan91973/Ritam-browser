package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import com.example.privacy.UrlCleaner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.TabState
import com.example.data.local.BookmarkEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.preferences.SearchEngine
import com.example.ui.theme.LocalRitamExtendedColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActiveOmniboxHeader(
    tab: TabState,
    shieldsEnabledForSite: Boolean,
    httpsOnlyEnabled: Boolean,
    gestureSwipeTabsEnabled: Boolean = true,
    gesturePullRefreshEnabled: Boolean = true,
    gestureDoubleTapTopEnabled: Boolean = true,
    onTapAddress: () -> Unit,
    onDoubleTapAddress: () -> Unit = {},
    onSwipeTab: (Int) -> Unit = {},
    onTapSiteInfo: () -> Unit,
    onToggleSiteShields: () -> Unit,
    onOpenShieldSettings: () -> Unit,
    onToggleReaderMode: () -> Unit,
    onToggleDesktopMode: () -> Unit = {},
    onReloadOrStop: () -> Unit,
    isBottomDocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ext = LocalRitamExtendedColors.current
    var showShieldFlyout by remember { mutableStateOf(false) }

    val pillSurfaceColor = if (tab.isPrivate) IncognitoColors.Surface else ext.surfaceLow
    val pillBorderColor = if (tab.isPrivate) IncognitoColors.Border else ext.cardBorder
    val primaryTextColor = if (tab.isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface
    val mutedTextColor = if (tab.isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (tab.isPrivate) Modifier.background(IncognitoColors.Backdrop)
                else Modifier
            )
            .then(if (!isBottomDocked) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(gestureSwipeTabsEnabled, gesturePullRefreshEnabled) {
                    if (!gestureSwipeTabsEnabled && !gesturePullRefreshEnabled) return@pointerInput
                    var dragX = 0f
                    var dragY = 0f
                    detectDragGestures(
                        onDragStart = {
                            dragX = 0f
                            dragY = 0f
                        },
                        onDrag = { _, dragAmount ->
                            dragX += dragAmount.x
                            dragY += dragAmount.y
                        },
                        onDragEnd = {
                            if (gestureSwipeTabsEnabled && abs(dragX) > 96f && abs(dragX) > abs(dragY) * 1.3f) {
                                if (dragX < 0f) onSwipeTab(1) else onSwipeTab(-1)
                            } else if (gesturePullRefreshEnabled && dragY > 88f && abs(dragY) > abs(dragX) * 1.4f) {
                                onReloadOrStop()
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(28.dp),
            color = pillSurfaceColor,
            border = BorderStroke(1.dp, pillBorderColor),
            tonalElevation = 2.dp,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab.isPrivate) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp, end = 2.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(IncognitoColors.BadgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        IncognitoSpectsEmblem(
                            size = 18.dp,
                            tint = IncognitoColors.Accent,
                            hatFill = IncognitoColors.Surface
                        )
                    }
                }

                IconButton(
                    onClick = onTapSiteInfo,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("omnibox_ssl_button")
                ) {
                    Icon(
                        imageVector = if (tab.sslSecure) Icons.Rounded.Lock else Icons.Rounded.Warning,
                        contentDescription = if (tab.sslSecure) "Secure HTTPS connection" else "Insecure connection",
                        tint = if (tab.sslSecure) {
                            if (tab.isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Domain + path click to edit URL (double-tap scrolls to top)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .combinedClickable(
                            onClick = onTapAddress,
                            onDoubleClick = {
                                if (gestureDoubleTapTopEnabled) onDoubleTapAddress()
                            }
                        )
                        .padding(horizontal = 4.dp)
                        .testTag("omnibox_address_display"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayDomain = tab.domain.ifBlank { tab.url }
                    val pathSuffix = remember(tab.url) {
                        val idx = tab.url.indexOf("://")
                        val afterScheme = if (idx >= 0) tab.url.substring(idx + 3) else tab.url
                        val slashIdx = afterScheme.indexOf('/')
                        if (slashIdx >= 0) afterScheme.substring(slashIdx) else ""
                    }
                    Text(
                        text = displayDomain,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (pathSuffix.isNotEmpty() && pathSuffix != "/") {
                        Text(
                            text = pathSuffix,
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // Ritam Shield badge
                val pillControlBg = if (tab.isPrivate) IncognitoColors.SurfaceElevated else ext.surfaceHigh
                val pillControlBorder = if (tab.isPrivate) IncognitoColors.Border else ext.cardBorder
                val pillAccent = if (tab.isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary

                Surface(
                    onClick = { showShieldFlyout = !showShieldFlyout },
                    shape = RoundedCornerShape(16.dp),
                    color = if (shieldsEnabledForSite) pillControlBg else MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, pillControlBorder),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("omnibox_shield_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "Ritam Shields status",
                            tint = if (shieldsEnabledForSite) pillAccent else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = tab.totalBlocked.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = primaryTextColor
                        )
                    }
                }

                if (tab.isDesktopMode) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        onClick = onToggleDesktopMode,
                        shape = CircleShape,
                        color = pillAccent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, pillAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("omnibox_desktop_badge")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.DesktopWindows,
                                contentDescription = "Desktop view active (tap to switch)",
                                tint = pillAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Reader Mode pill
                Surface(
                    onClick = onToggleReaderMode,
                    shape = CircleShape,
                    color = if (tab.isReaderMode) pillAccent else pillControlBg,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("omnibox_reader_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = "Toggle Reader Mode",
                            tint = if (tab.isReaderMode) {
                                if (tab.isPrivate) IncognitoColors.Backdrop else MaterialTheme.colorScheme.onPrimary
                            } else primaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onReloadOrStop,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("omnibox_refresh_button")
                ) {
                    Icon(
                        imageVector = if (tab.isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                        contentDescription = if (tab.isLoading) "Stop loading" else "Reload page",
                        tint = mutedTextColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        if (tab.isLoading && tab.progress in 1..99) {
            LinearProgressIndicator(
                progress = { tab.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (tab.isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary,
                trackColor = if (tab.isPrivate) IncognitoColors.SurfaceElevated else ext.surfaceHighest
            )
        }

        AnimatedVisibility(visible = showShieldFlyout) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(18.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (shieldsEnabledForSite) "Ad-Blocker & Shield Active" else "Ad-Blocker Paused for Site",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = tab.domain.ifBlank { "Current Session" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = shieldsEnabledForSite,
                            onCheckedChange = { onToggleSiteShields() },
                            modifier = Modifier.testTag("flyout_shield_toggle")
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ShieldStatMini("Ads Blocked", tab.adsBlocked.toString())
                        ShieldStatMini("Trackers", tab.trackersBlocked.toString())
                        ShieldStatMini("HTTPS-Only", if (httpsOnlyEnabled) "Enforced" else "Off")
                    }
                    if (tab.blockedDomains.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Blocked hosts: ${tab.blockedDomains.take(4).joinToString(", ")}" +
                                if (tab.blockedDomains.size > 4) " +${tab.blockedDomains.size - 4} more" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showShieldFlyout = false }) {
                            Text("Close")
                        }
                        TextButton(onClick = {
                            showShieldFlyout = false
                            onOpenShieldSettings()
                        }) {
                            Text("Ad-Blocker Rules")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShieldStatMini(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OmniboxSearchOverlay(
    initialQuery: String,
    currentPageTitle: String = "",
    isPrivate: Boolean = false,
    stripTrackingParams: Boolean = true,
    searchEngine: SearchEngine,
    recentSearches: List<SearchHistoryEntity>,
    bookmarks: List<BookmarkEntity>,
    looksLikeUrl: (String) -> Boolean,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onCopyAddress: (String) -> Unit,
    onShareQrCode: (String) -> Unit = {},
    onAddToReadingList: () -> Unit = {},
    onSubmitQuery: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onScanQr: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var queryState by remember { mutableStateOf(TextFieldValue("")) }
    var copiedAddress by remember { mutableStateOf(false) }
    val query = queryState.text
    val focusRequester = remember { FocusRequester() }

    val overlayBackdrop = if (isPrivate) IncognitoColors.Backdrop else MaterialTheme.colorScheme.background
    val overlaySurface = if (isPrivate) IncognitoColors.Surface else ext.surfaceLow
    val overlayElevated = if (isPrivate) IncognitoColors.SurfaceElevated else ext.surfaceHigh
    val overlayBorder = if (isPrivate) IncognitoColors.Border else ext.cardBorder
    val overlayText = if (isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface
    val overlayMuted = if (isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant
    val overlayAccent = if (isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val matchingBookmarks = remember(query, bookmarks) {
        if (query.isBlank()) bookmarks.take(4)
        else bookmarks.filter {
            it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }.take(4)
    }

    val matchingSearches = remember(query, recentSearches) {
        if (query.isBlank()) recentSearches.take(5)
        else recentSearches.filter { it.query.contains(query, ignoreCase = true) }.take(5)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = overlayBackdrop
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (isPrivate) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IncognitoColors.BadgeBg,
                    border = BorderStroke(1.dp, IncognitoColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IncognitoSpectsEmblem(
                            size = 18.dp,
                            tint = IncognitoColors.Accent,
                            hatFill = IncognitoColors.Surface
                        )
                        Text(
                            text = "Incognito Mode • Searches & sites won't be saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncognitoColors.OnSurface
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = overlaySurface,
                border = BorderStroke(1.5.dp, overlayAccent.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Cancel address input",
                            tint = overlayText
                        )
                    }

                    SearchEngineSelectorPillButton(
                        selectedEngine = searchEngine,
                        onSelectEngine = onSelectSearchEngine,
                        isPrivate = isPrivate,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    BasicTextField(
                        value = queryState,
                        onValueChange = { queryState = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = overlayText
                        ),
                        cursorBrush = SolidColor(overlayAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (query.isNotBlank()) {
                                    onSubmitQuery(query)
                                }
                            }
                        ),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search ${searchEngine.displayName} or enter URL",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = overlayMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("omnibox_text_field")
                    )
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { queryState = TextFieldValue("") },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear text", tint = overlayMuted)
                        }
                    } else {
                        IconButton(onClick = onVoiceSearch, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Rounded.Mic, contentDescription = "Voice search", tint = overlayMuted)
                        }
                        IconButton(onClick = onScanQr, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR code", tint = overlayMuted)
                        }
                    }
                }
            }

            if (initialQuery.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = overlaySurface,
                    border = BorderStroke(1.dp, overlayBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("omnibox_current_address_card")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = overlayElevated,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = overlayAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                if (currentPageTitle.isNotBlank()) {
                                    Text(
                                        text = currentPageTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = overlayText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = initialQuery,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = overlayMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val cleanResult = remember(initialQuery, stripTrackingParams) {
                            if (stripTrackingParams) UrlCleaner.cleanUrl(initialQuery)
                            else UrlCleaner.CleanResult(initialQuery, 0)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    copiedAddress = true
                                    onCopyAddress(cleanResult.cleanedUrl)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (copiedAddress) overlayAccent.copy(alpha = 0.2f) else overlayElevated,
                                border = BorderStroke(1.dp, if (copiedAddress) overlayAccent else overlayBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("omnibox_copy_address_button")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (copiedAddress) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy clean address",
                                        tint = if (copiedAddress) overlayAccent else overlayText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            copiedAddress -> "Copied"
                                            cleanResult.removedCount > 0 -> "Clean Copy (-${cleanResult.removedCount})"
                                            else -> "Copy address"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (copiedAddress) overlayAccent else overlayText,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    queryState = TextFieldValue(
                                        text = initialQuery,
                                        selection = TextRange(initialQuery.length)
                                    )
                                    focusRequester.requestFocus()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = overlayElevated,
                                border = BorderStroke(1.dp, overlayBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("omnibox_edit_address_button")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit address",
                                        tint = overlayAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Edit address",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = overlayText
                                    )
                                }
                            }

                            Surface(
                                onClick = { onShareQrCode(cleanResult.cleanedUrl) },
                                shape = RoundedCornerShape(12.dp),
                                color = overlayElevated,
                                border = BorderStroke(1.dp, overlayBorder),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode2,
                                        contentDescription = "Share QR Code",
                                        tint = overlayAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { onAddToReadingList(); onDismiss() },
                                shape = RoundedCornerShape(12.dp),
                                color = overlayElevated,
                                border = BorderStroke(1.dp, overlayBorder),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Queue,
                                        contentDescription = "Add to Reading List",
                                        tint = overlayText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (query.isNotBlank()) {
                    item {
                        val isUrl = looksLikeUrl(query.trim())
                        Surface(
                            onClick = { onSubmitQuery(query) },
                            shape = RoundedCornerShape(14.dp),
                            color = overlayAccent.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("omnibox_submit_row")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isUrl) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = overlayAccent
                                    )
                                } else {
                                    SearchEngineLogo(engine = searchEngine, size = 22.dp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = query.trim(),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = overlayText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isUrl) "Navigate directly via HTTPS" else "Search with ${searchEngine.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = overlayMuted
                                    )
                                }
                            }
                        }
                    }
                }

                if (matchingBookmarks.isNotEmpty()) {
                    item {
                        Text(
                            text = "BOOKMARKS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            color = overlayMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    items(matchingBookmarks, key = { "bm_${it.id}" }) { bm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSubmitQuery(bm.url) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = overlayAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bm.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = overlayText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bm.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = overlayMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (matchingSearches.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = if (isPrivate) IncognitoColors.Border else ext.cardBorder
                        )
                        Text(
                            text = "RECENT SEARCHES",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            color = overlayMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    items(matchingSearches, key = { "sh_${it.id}" }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSubmitQuery(item.query) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = null,
                                tint = overlayMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = item.query,
                                style = MaterialTheme.typography.bodyMedium,
                                color = overlayText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FindInPageBar(
    query: String,
    currentMatch: Int = 0,
    totalMatches: Int = 0,
    isPrivate: Boolean = false,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalRitamExtendedColors.current
    val barBg = if (isPrivate) IncognitoColors.SurfaceElevated else ext.surfaceHigh
    val barBorder = if (isPrivate) IncognitoColors.Border else ext.cardBorder
    val barText = if (isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface
    val barMuted = if (isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant
    val barAccent = if (isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("find_in_page_bar"),
        shape = RoundedCornerShape(16.dp),
        color = barBg,
        border = BorderStroke(1.dp, barBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = barMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = barText
                ),
                cursorBrush = SolidColor(barAccent),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Find in page…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = barMuted
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("find_in_page_input")
            )
            if (query.isNotEmpty()) {
                Text(
                    text = if (totalMatches > 0) "$currentMatch/$totalMatches" else "0/0",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (totalMatches > 0) barAccent else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            IconButton(onClick = onPrev, enabled = totalMatches > 0, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Previous match",
                    tint = if (totalMatches > 0) barText else barMuted.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = onNext, enabled = totalMatches > 0, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Next match",
                    tint = if (totalMatches > 0) barText else barMuted.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close find bar", tint = barMuted)
            }
        }
    }
}
