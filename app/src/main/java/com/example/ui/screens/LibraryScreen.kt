package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookmarkEntity
import com.example.data.local.BookmarkFolderEntity
import com.example.data.local.DownloadCategory
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.data.local.HighlightEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.ReadingListItemEntity
import com.example.data.local.RssArticleEntity
import com.example.data.local.RssFeedEntity
import com.example.data.preferences.BrowserPreferences
import com.example.services.StorageStats
import com.example.services.UniversalFileType
import com.example.services.UniversalViewerHelper
import com.example.ui.components.formatBytes
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily
import java.util.Calendar

enum class LibraryTab(val label: String) {
    ALL("All"),
    HISTORY("History"),
    DOWNLOADS("Downloads"),
    BOOKMARKS("Bookmarks"),
    READING_LIST("Read Later"),
    FEEDS("Feeds"),
    HIGHLIGHTS("Highlights")
}

@Composable
fun LibraryScreen(
    initialTab: LibraryTab,
    history: List<HistoryEntity>,
    downloads: List<DownloadEntity>,
    bookmarks: List<BookmarkEntity>,
    folders: List<BookmarkFolderEntity>,
    storageStats: StorageStats,
    preferences: BrowserPreferences,
    isDesktopActive: Boolean,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onDeleteHistoryItems: (List<Long>) -> Unit,
    onOpenClearData: () -> Unit,
    onExportHistoryUri: (android.net.Uri) -> Unit,
    onCancelDownload: (DownloadEntity) -> Unit,
    onOpenDownload: (DownloadEntity) -> Unit,
    onShareDownload: (DownloadEntity) -> Unit,
    onDeleteDownload: (DownloadEntity) -> Unit,
    onClearCompletedDownloads: () -> Unit,
    onToggleAskWhereToSave: (Boolean) -> Unit,
    onAddBookmark: () -> Unit,
    onAddFolder: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onExportBookmarksUri: (android.net.Uri) -> Unit,
    onImportBookmarksUri: (android.net.Uri) -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onVoiceSearch: () -> Unit,
    onRefreshStorage: () -> Unit,
    readingList: List<ReadingListItemEntity> = emptyList(),
    onToggleReadLaterDone: (Long, Boolean) -> Unit = { _, _ -> },
    onDeleteReadLater: (Long) -> Unit = {},
    onClearReadLaterDone: () -> Unit = {},
    activeDownloadSpeeds: Map<Long, Long> = emptyMap(),
    onPauseDownload: (DownloadEntity) -> Unit = {},
    onResumeDownload: (DownloadEntity) -> Unit = {},
    rssFeeds: List<RssFeedEntity> = emptyList(),
    rssArticles: List<RssArticleEntity> = emptyList(),
    onSubscribeFeed: (String) -> Unit = {},
    onRefreshFeeds: () -> Unit = {},
    onToggleFeedArticleRead: (Long, Boolean) -> Unit = { _, _ -> },
    onDeleteFeed: (Long) -> Unit = {},
    highlights: List<HighlightEntity> = emptyList(),
    onDeleteHighlight: (Long) -> Unit = {},
    onExportHighlightsUri: (android.net.Uri) -> Unit = {},
    onOpenLocalFile: (android.net.Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val ext = LocalRitamExtendedColors.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.ordinal) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    val exportBookmarksLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri -> uri?.let(onExportBookmarksUri) }

    val importBookmarksLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportBookmarksUri) }

    val exportHistoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> uri?.let(onExportHistoryUri) }

    val exportHighlightsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> uri?.let(onExportHighlightsUri) }

    val openLocalFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onOpenLocalFile) }

    LaunchedEffect(Unit) { onRefreshStorage() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Compact Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).testTag("library_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Browser",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Library & Controls",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = NewsreaderFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showSearchBar = !showSearchBar },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "Filter library", modifier = Modifier.size(19.dp))
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Open Settings", modifier = Modifier.size(19.dp))
                }
            }
        }

        if (showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text("Search history, downloads & bookmarks…", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(17.dp))
                        }
                    } else {
                        IconButton(onClick = onVoiceSearch, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Mic, contentDescription = "Voice search", modifier = Modifier.size(17.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            LibraryTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (LibraryTab.entries[selectedTab]) {
            LibraryTab.ALL -> AllLibraryOverview(
                history = history.filter {
                    searchQuery.isBlank() || it.title.contains(searchQuery, true) || it.domain.contains(searchQuery, true)
                },
                downloads = downloads,
                bookmarks = bookmarks,
                preferences = preferences,
                isDesktopActive = isDesktopActive,
                onToggleDesktopMode = onToggleDesktopMode,
                onToggleDarkMode = onToggleDarkMode,
                onOpenUrl = onOpenUrl,
                onSwitchTab = { selectedTab = it.ordinal }
            )

            LibraryTab.HISTORY -> HistoryTabContent(
                history = history,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onVoiceSearch = onVoiceSearch,
                onOpenUrl = onOpenUrl,
                onCopyUrl = onCopyUrl,
                onDeleteItem = onDeleteHistoryItem,
                onDeleteItems = onDeleteHistoryItems,
                onExportHistory = { exportHistoryLauncher.launch("ritam_history.md") },
                onClearBrowsingData = onOpenClearData
            )

            LibraryTab.DOWNLOADS -> DownloadsTabContent(
                downloads = downloads.filter {
                    searchQuery.isBlank() || it.filename.contains(searchQuery, true)
                },
                activeDownloadSpeeds = activeDownloadSpeeds,
                storageStats = storageStats,
                askWhereToSave = preferences.askWhereToSaveDownloads,
                downloadDirectoryLabel = preferences.downloadDirectoryLabel,
                onToggleAskWhereToSave = onToggleAskWhereToSave,
                onPauseDownload = onPauseDownload,
                onResumeDownload = onResumeDownload,
                onCancelDownload = onCancelDownload,
                onOpenDownload = onOpenDownload,
                onShareDownload = onShareDownload,
                onDeleteDownload = onDeleteDownload,
                onClearCompleted = onClearCompletedDownloads,
                onPickLocalFile = { openLocalFileLauncher.launch(arrayOf("*/*")) }
            )

            LibraryTab.BOOKMARKS -> BookmarksTabContent(
                bookmarks = bookmarks.filter {
                    searchQuery.isBlank() || it.title.contains(searchQuery, true) || it.url.contains(searchQuery, true)
                },
                folders = folders,
                onOpenUrl = onOpenUrl,
                onAddBookmark = onAddBookmark,
                onAddFolder = onAddFolder,
                onDeleteBookmark = onDeleteBookmark,
                onExportHtml = { exportBookmarksLauncher.launch("ritam_bookmarks.html") },
                onImportHtml = {
                    importBookmarksLauncher.launch(
                        arrayOf("text/html", "application/json", "text/plain", "application/octet-stream")
                    )
                }
            )

            LibraryTab.READING_LIST -> ReadingListTabContent(
                items = readingList.filter {
                    searchQuery.isBlank() ||
                        it.title.contains(searchQuery, true) ||
                        it.domain.contains(searchQuery, true) ||
                        it.url.contains(searchQuery, true)
                },
                onOpenUrl = onOpenUrl,
                onToggleRead = onToggleReadLaterDone,
                onDelete = onDeleteReadLater,
                onClearRead = onClearReadLaterDone
            )

            LibraryTab.FEEDS -> FeedsTabContent(
                feeds = rssFeeds,
                articles = rssArticles.filter {
                    searchQuery.isBlank() ||
                        it.title.contains(searchQuery, true) ||
                        it.feedTitle.contains(searchQuery, true)
                },
                onSubscribeFeed = onSubscribeFeed,
                onRefreshFeeds = onRefreshFeeds,
                onOpenArticle = { article ->
                    onToggleFeedArticleRead(article.id, true)
                    onOpenUrl(article.link)
                },
                onToggleArticleRead = onToggleFeedArticleRead,
                onDeleteFeed = onDeleteFeed
            )

            LibraryTab.HIGHLIGHTS -> HighlightsTabContent(
                highlights = highlights.filter {
                    searchQuery.isBlank() ||
                        it.quoteText.contains(searchQuery, true) ||
                        it.note.contains(searchQuery, true) ||
                        it.title.contains(searchQuery, true)
                },
                onOpenUrl = onOpenUrl,
                onDeleteHighlight = onDeleteHighlight,
                onExportMarkdown = { exportHighlightsLauncher.launch("ritam_highlights.md") }
            )
        }
    }
}

@Composable
private fun AllLibraryOverview(
    history: List<HistoryEntity>,
    downloads: List<DownloadEntity>,
    bookmarks: List<BookmarkEntity>,
    preferences: BrowserPreferences,
    isDesktopActive: Boolean,
    onToggleDesktopMode: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onSwitchTab: (LibraryTab) -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "BROWSER ENVIRONMENT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onToggleDesktopMode,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDesktopActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceLow,
                    border = BorderStroke(1.dp, if (isDesktopActive) MaterialTheme.colorScheme.primary else ext.cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            Icons.Rounded.DesktopWindows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Desktop View", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            if (isDesktopActive) "Active UA" else "Mobile UA",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    onClick = onToggleDarkMode,
                    shape = RoundedCornerShape(16.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            Icons.Rounded.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Theme Mode", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            preferences.themePalette.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT HISTORY (${history.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { onSwitchTab(LibraryTab.HISTORY) }) { Text("View all") }
            }
            if (history.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No browsing history recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(history.take(4), key = { "all_h_${it.id}" }) { item ->
            Surface(
                onClick = { onOpenUrl(item.url) },
                shape = RoundedCornerShape(14.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${item.domain} • ${formatRelativeTime(item.visitedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DOWNLOADS (${downloads.size}) • BOOKMARKS (${bookmarks.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { onSwitchTab(LibraryTab.BOOKMARKS) }) { Text("Manage") }
            }
        }
    }
}

@Composable
private fun HistoryTabContent(
    history: List<HistoryEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onDeleteItems: (List<Long>) -> Unit,
    onExportHistory: () -> Unit,
    onClearBrowsingData: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var timeFilter by remember { mutableStateOf("All") }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }

    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
    }
    val startOfYesterday = startOfToday - 86_400_000L
    val sevenDaysAgo = startOfToday - 7 * 86_400_000L

    val filtered = remember(history, searchQuery, timeFilter) {
        history.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, true) ||
                item.url.contains(searchQuery, true)
            val matchesTime = when (timeFilter) {
                "Today" -> item.visitedAt >= startOfToday
                "Yesterday" -> item.visitedAt in startOfYesterday until startOfToday
                "7 Days" -> item.visitedAt >= sevenDaysAgo
                else -> true
            }
            matchesQuery && matchesTime
        }
    }

    val topDomains = remember(history) {
        history.map { it.domain }.filter { it.isNotBlank() }.distinct().take(3)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Local History", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    "Stored strictly on-device • Private tabs excluded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = ext.surfaceHigh) {
                            Text(
                                "${history.size} items",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    placeholder = { Text("Search history & domains…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = onVoiceSearch) {
                            Icon(Icons.Rounded.Mic, contentDescription = "Voice search")
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Today", "Yesterday", "7 Days").forEach { filter ->
                        FilterChip(
                            selected = timeFilter == filter,
                            onClick = { timeFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }

            if (filtered.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ext.surfaceLow,
                        border = BorderStroke(1.dp, ext.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text("Reading Reflection", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (topDomains.isNotEmpty())
                                        "Recent focus spans ${topDomains.joinToString(", ")} across ${history.size} recorded visits."
                                    else "Your reading flow remains calm and private.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No history entries match your filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    val checked = item.id in selectedIds
                    Surface(
                        onClick = {
                            if (selectMode) {
                                selectedIds = if (checked) selectedIds - item.id else selectedIds + item.id
                            } else {
                                onOpenUrl(item.url)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceLow,
                        border = BorderStroke(1.dp, ext.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectMode) {
                                Icon(
                                    imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = "Select entry",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ext.surfaceHigh,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (item.domain.firstOrNull() ?: 'W').uppercaseChar().toString(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.domain} • ${formatRelativeTime(item.visitedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!selectMode) {
                                IconButton(onClick = { onCopyUrl(item.url) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy URL", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteItem(item.id) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove entry", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom History Action Bar
        Surface(
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${history.map { it.domain }.distinct().size} origins visited",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onExportHistory) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export History")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (selectMode && selectedIds.isNotEmpty()) {
                                onDeleteItems(selectedIds.toList())
                                selectedIds = emptySet()
                                selectMode = false
                            } else {
                                selectMode = !selectMode
                                selectedIds = emptySet()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text(
                            if (selectMode && selectedIds.isNotEmpty()) "Delete (${selectedIds.size})"
                            else if (selectMode) "Done"
                            else "Select"
                        )
                    }
                    Button(
                        onClick = onClearBrowsingData,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1.3f).height(46.dp).testTag("clear_browsing_data_button")
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

@Composable
private fun DownloadsTabContent(
    downloads: List<DownloadEntity>,
    activeDownloadSpeeds: Map<Long, Long>,
    storageStats: StorageStats,
    askWhereToSave: Boolean,
    downloadDirectoryLabel: String,
    onToggleAskWhereToSave: (Boolean) -> Unit,
    onPauseDownload: (DownloadEntity) -> Unit,
    onResumeDownload: (DownloadEntity) -> Unit,
    onCancelDownload: (DownloadEntity) -> Unit,
    onOpenDownload: (DownloadEntity) -> Unit,
    onShareDownload: (DownloadEntity) -> Unit,
    onDeleteDownload: (DownloadEntity) -> Unit,
    onClearCompleted: () -> Unit,
    onPickLocalFile: () -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    var selectedCategory by remember { mutableStateOf("All") }

    val activeDownloads = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PAUSED }
    }
    val finishedDownloads = remember(downloads, selectedCategory) {
        downloads.filter {
            it.status != DownloadStatus.RUNNING && it.status != DownloadStatus.PAUSED &&
                (selectedCategory == "All" || it.category == selectedCategory)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Real StatFs Local Repository Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LOCAL REPOSITORY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${formatBytes(storageStats.usedBytes)} used of ${formatBytes(storageStats.totalBytes)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Icon(
                            Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { storageStats.usedPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = ext.surfaceHighest
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Free: ${formatBytes(storageStats.freeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(storageStats.usedPercent * 100).toInt()}% utilized",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickLocalFile,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("open_local_file_button")
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open File")
                        }
                        OutlinedButton(
                            onClick = onClearCompleted,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear Done")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Built-in viewer: Image, Video, Audio, PDF, Word (.doc/.docx), PPT, Excel (.xls/.xlsx/.csv), Markdown",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category Chips
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", DownloadCategory.DOCUMENTS, DownloadCategory.MEDIA, DownloadCategory.ARCHIVES, DownloadCategory.IMAGES).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        if (activeDownloads.isNotEmpty()) {
            item {
                Text(
                    text = "ACTIVE TRANSFERS (${activeDownloads.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(activeDownloads, key = { "act_${it.id}" }) { dl ->
                val progress = if (dl.totalBytes > 0) (dl.downloadedBytes.toFloat() / dl.totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                val speedBps = activeDownloadSpeeds[dl.id] ?: 0L
                val isPaused = dl.status == DownloadStatus.PAUSED
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                dl.filename,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (isPaused) "PAUSED" else if (speedBps > 0) "${formatBytes(speedBps)}/s" else "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${formatBytes(dl.downloadedBytes)} / ${if (dl.totalBytes > 0) formatBytes(dl.totalBytes) else "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            if (dl.systemDownloadId < 0) {
                                if (isPaused) {
                                    Button(onClick = { onResumeDownload(dl) }) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Resume")
                                    }
                                } else {
                                    OutlinedButton(onClick = { onPauseDownload(dl) }) {
                                        Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pause")
                                    }
                                }
                            }
                            OutlinedButton(onClick = { onCancelDownload(dl) }) { Text("Cancel") }
                        }
                    }
                }
            }
        }

        if (finishedDownloads.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No downloads in this category", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(finishedDownloads, key = { "fin_${it.id}" }) { dl ->
                val isViewerSupported = remember(dl.filename, dl.mimeType) {
                    UniversalViewerHelper.isSupportedFilename(dl.filename, dl.mimeType)
                }
                val detectedType = remember(dl.filename, dl.mimeType) {
                    UniversalViewerHelper.classifyType(dl.filename, dl.mimeType)
                }
                val fileIcon = if (!isViewerSupported) {
                    Icons.Rounded.FileDownload
                } else when (detectedType) {
                    UniversalFileType.IMAGE -> Icons.Rounded.Image
                    UniversalFileType.VIDEO -> Icons.Rounded.VideoFile
                    UniversalFileType.AUDIO -> Icons.Rounded.AudioFile
                    UniversalFileType.PDF -> Icons.Rounded.PictureAsPdf
                    UniversalFileType.PPT -> Icons.Rounded.Slideshow
                    UniversalFileType.EXCEL -> Icons.Rounded.TableChart
                    UniversalFileType.WORD, UniversalFileType.MARKDOWN, UniversalFileType.TEXT -> Icons.Rounded.Description
                }
                Surface(
                    onClick = { onOpenDownload(dl) },
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(fileIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dl.filename, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = buildString {
                                    append(dl.category)
                                    append(" • ")
                                    append(formatBytes(dl.totalBytes))
                                    if (isViewerSupported) {
                                        append(" • ${detectedType.label} Viewer")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onShareDownload(dl) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share file", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteDownload(dl) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remove record", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Destination Preferences Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("DESTINATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(downloadDirectoryLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ask where to save files", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = askWhereToSave, onCheckedChange = onToggleAskWhereToSave)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksTabContent(
    bookmarks: List<BookmarkEntity>,
    folders: List<BookmarkFolderEntity>,
    onOpenUrl: (String) -> Unit,
    onAddBookmark: () -> Unit,
    onAddFolder: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onExportHtml: () -> Unit,
    onImportHtml: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var selectedFolderId by remember { mutableLongStateOf(0L) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val visible = remember(bookmarks, selectedFolderId) {
        if (selectedFolderId == 0L) bookmarks else bookmarks.filter { it.folderId == selectedFolderId }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportHtml,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export HTML")
                }
                OutlinedButton(
                    onClick = onImportHtml,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import browser")
                }
            }
            Text(
                text = "Bookmarks from Chrome, Edge, Firefox, Safari, Opera, Brave or Vivaldi. " +
                    "Choose the .html or .json file they export; folders and duplicates are handled.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFolderId == 0L,
                    onClick = { selectedFolderId = 0L },
                    label = { Text("All (${bookmarks.size})") }
                )
                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(folder.name) }
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showNewFolderDialog = true },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Folder") }
                )
            }
        }

        item {
            Button(
                onClick = onAddBookmark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add New Bookmark")
            }
        }

        items(visible, key = { it.id }) { bm ->
            Surface(
                onClick = { onOpenUrl(bm.url) },
                shape = RoundedCornerShape(14.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bm.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(bm.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onOpenUrl(bm.url) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = "Open bookmark", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDeleteBookmark(bm.id) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete bookmark", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        onAddFolder(newFolderName)
                        newFolderName = ""
                        showNewFolderDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ReadingListTabContent(
    items: List<ReadingListItemEntity>,
    onOpenUrl: (String) -> Unit,
    onToggleRead: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onClearRead: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var filterMode by remember { mutableStateOf("UNREAD") }
    val visible = remember(items, filterMode) {
        when (filterMode) {
            "UNREAD" -> items.filter { !it.isRead }
            "READ" -> items.filter { it.isRead }
            else -> items
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterMode == "UNREAD",
                        onClick = { filterMode = "UNREAD" },
                        label = { Text("Unread (${items.count { !it.isRead }})") }
                    )
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { filterMode = "ALL" },
                        label = { Text("All (${items.size})") }
                    )
                    FilterChip(
                        selected = filterMode == "READ",
                        onClick = { filterMode = "READ" },
                        label = { Text("Done") }
                    )
                }
                if (items.any { it.isRead }) {
                    TextButton(onClick = onClearRead) {
                        Text("Clear Done")
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Queue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (filterMode == "UNREAD") "Reading queue is clear" else "No saved articles yet",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Tap the Read Later button in Quick Controls or the omnibox menu to queue articles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(visible, key = { "rl_${it.id}" }) { entry ->
                Surface(
                    onClick = { onOpenUrl(entry.url) },
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleRead(entry.id, !entry.isRead) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (entry.isRead) Icons.Rounded.DoneAll else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = if (entry.isRead) "Mark unread" else "Mark as read",
                                tint = if (entry.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (entry.isRead) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${entry.domain} • ${entry.readTimeMinutes} min read • ${formatRelativeTime(entry.savedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (entry.excerpt.isNotBlank()) {
                                Text(
                                    text = entry.excerpt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(entry.id) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete reading item", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedsTabContent(
    feeds: List<RssFeedEntity>,
    articles: List<RssArticleEntity>,
    onSubscribeFeed: (String) -> Unit,
    onRefreshFeeds: () -> Unit,
    onOpenArticle: (RssArticleEntity) -> Unit,
    onToggleArticleRead: (Long, Boolean) -> Unit,
    onDeleteFeed: (Long) -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var feedUrlInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.RssFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("RSS & Atom Reader", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        OutlinedButton(onClick = onRefreshFeeds, enabled = feeds.isNotEmpty()) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sync")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = feedUrlInput,
                            onValueChange = { feedUrlInput = it },
                            singleLine = true,
                            placeholder = { Text("https://example.com/feed.xml") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            if (feedUrlInput.isNotBlank()) {
                                onSubscribeFeed(feedUrlInput)
                                feedUrlInput = ""
                            }
                        }) {
                            Text("Follow")
                        }
                    }
                    if (feeds.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            feeds.forEach { feed ->
                                FilterChip(
                                    selected = true,
                                    onClick = { onDeleteFeed(feed.id) },
                                    label = { Text("${feed.title} ×") }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (articles.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.RssFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("No feed articles yet", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Paste any RSS/Atom URL above or tap Follow RSS when browsing a blog.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(articles, key = { "rss_${it.id}" }) { article ->
                Surface(
                    onClick = { onOpenArticle(article) },
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleArticleRead(article.id, !article.isRead) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (article.isRead) Icons.Rounded.DoneAll else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = if (article.isRead) "Mark unread" else "Mark read",
                                tint = if (article.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (article.isRead) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOf(article.feedTitle, article.publishedLabel).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (article.summary.isNotBlank()) {
                                Text(
                                    text = article.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightsTabContent(
    highlights: List<HighlightEntity>,
    onOpenUrl: (String) -> Unit,
    onDeleteHighlight: (Long) -> Unit,
    onExportMarkdown: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${highlights.size} SAVED HIGHLIGHTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onExportMarkdown, enabled = highlights.isNotEmpty()) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export Markdown")
                }
            }
        }

        if (highlights.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("No highlights saved yet", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Select text on any page or tap Note in Reader View to save quotes with private annotations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(highlights, key = { "hl_${it.id}" }) { hl ->
                val accent = remember(hl.colorHex) {
                    runCatching { Color(android.graphics.Color.parseColor(hl.colorHex)) }
                        .getOrDefault(Color(0xFF91D5AE))
                }
                Surface(
                    onClick = { if (hl.url.startsWith("http")) onOpenUrl(hl.url) },
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accent)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "“${hl.quoteText}”",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = NewsreaderFamily,
                                    fontStyle = FontStyle.Italic
                                )
                            )
                            if (hl.note.isNotBlank()) {
                                Text(
                                    text = "Note: ${hl.note}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${hl.title.ifBlank { hl.domain }} • ${formatRelativeTime(hl.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onDeleteHighlight(hl.id) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete highlight", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

