package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.browser.HighlightSelectionRequest
import com.example.browser.TabState
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.ReadingListItemEntity
import com.example.data.local.UserscriptEntity
import com.example.data.local.WatchlistItemEntity
import com.example.services.StructuredViewerState
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

private data class PaletteAction(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector
)

private data class PaletteSearchResult(
    val key: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun CommandPaletteDialog(
    tabs: List<TabState>,
    bookmarks: List<BookmarkEntity>,
    readingList: List<ReadingListItemEntity>,
    history: List<HistoryEntity>,
    activeTab: TabState,
    splitActive: Boolean,
    onSelectTab: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onActionCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var query by remember { mutableStateOf("") }

    val quickActions = remember(activeTab.isHome, splitActive) {
        listOf(
            PaletteAction(
                id = "split_view",
                label = if (splitActive) "Close Split-Screen" else "Toggle Split-Screen",
                subtitle = "Side-by-side dual pane browsing",
                icon = Icons.Rounded.Splitscreen
            ),
            PaletteAction(
                id = "full_screenshot",
                label = "Full-Page Screenshot",
                subtitle = "Capture & share full scrolling page PNG",
                icon = Icons.Rounded.PhotoSizeSelectLarge
            ),
            PaletteAction(
                id = "highlight",
                label = "Highlight & Add Note",
                subtitle = "Save quote to Highlights",
                icon = Icons.Rounded.EditNote
            ),
            PaletteAction(
                id = "pretty_viewer",
                label = "JSON / Markdown Viewer",
                subtitle = "Format JSON, Markdown, or XML cleanly",
                icon = Icons.Rounded.DataObject
            ),
            PaletteAction(
                id = "watchlist",
                label = "Page Change Watchlist",
                subtitle = "Monitor page updates & snapshots",
                icon = Icons.Rounded.NotificationsActive
            ),
            PaletteAction(
                id = "userscripts",
                label = "Custom Userscripts & CSS",
                subtitle = "Per-site JS & styling automation",
                icon = Icons.Rounded.Terminal
            ),
            PaletteAction(
                id = "pwa_install",
                label = "Install Site as Web App",
                subtitle = "Pin standalone launcher shortcut",
                icon = Icons.Rounded.InstallMobile
            ),
            PaletteAction(
                id = "rss_feeds",
                label = "RSS & Atom Feed Reader",
                subtitle = "Follow blogs without algorithms",
                icon = Icons.Rounded.RssFeed
            ),
            PaletteAction(
                id = "toggle_dark",
                label = "Switch Light / Dark Theme",
                subtitle = "Toggle Onyx Forest / Serene Sand",
                icon = Icons.Rounded.NightsStay
            ),
            PaletteAction(
                id = "toggle_desktop",
                label = "Toggle Desktop Mode",
                subtitle = "Switch Desktop / Mobile user agent",
                icon = Icons.Rounded.DesktopWindows
            ),
            PaletteAction(
                id = "burn_session",
                label = "Burn Session Data",
                subtitle = "Wipe tabs, cookies, cache & history now",
                icon = Icons.Rounded.LocalFireDepartment
            ),
            PaletteAction(
                id = "open_file",
                label = "Open Document or Media File",
                subtitle = "View Image, Video, Audio, PDF, Word, PPT, Excel, MD",
                icon = Icons.Rounded.DataObject
            )
        )
    }

    val results = remember(query, tabs, bookmarks, readingList, history, quickActions) {
        val q = query.trim()
        val list = mutableListOf<PaletteSearchResult>()

        quickActions.filter {
            q.isBlank() || it.label.contains(q, true) || it.subtitle.contains(q, true)
        }.take(if (q.isBlank()) 4 else 8).forEach { act ->
            list.add(
                PaletteSearchResult(
                    key = "act_${act.id}",
                    title = act.label,
                    subtitle = act.subtitle,
                    badge = "ACTION",
                    icon = act.icon,
                    onClick = {
                        onDismiss()
                        onActionCommand(act.id)
                    }
                )
            )
        }

        tabs.filter {
            q.isBlank() || it.title.contains(q, true) || it.url.contains(q, true) || it.domain.contains(q, true)
        }.take(6).forEach { tab ->
            list.add(
                PaletteSearchResult(
                    key = "tab_${tab.id}",
                    title = tab.title.ifBlank { tab.domain.ifBlank { "New Tab" } },
                    subtitle = tab.url.ifBlank { "Sanctuary Home" },
                    badge = if (tab.id == activeTab.id) "ACTIVE TAB" else "TAB",
                    icon = Icons.Rounded.Tab,
                    onClick = {
                        onDismiss()
                        onSelectTab(tab.id)
                    }
                )
            )
        }

        bookmarks.filter {
            q.isNotBlank() && (it.title.contains(q, true) || it.url.contains(q, true))
        }.take(6).forEach { bm ->
            list.add(
                PaletteSearchResult(
                    key = "bm_${bm.id}",
                    title = bm.title,
                    subtitle = bm.url,
                    badge = "BOOKMARK",
                    icon = Icons.Rounded.Bookmark,
                    onClick = {
                        onDismiss()
                        onOpenUrl(bm.url)
                    }
                )
            )
        }

        readingList.filter {
            q.isNotBlank() && (it.title.contains(q, true) || it.domain.contains(q, true))
        }.take(4).forEach { rl ->
            list.add(
                PaletteSearchResult(
                    key = "rl_${rl.id}",
                    title = rl.title,
                    subtitle = rl.url,
                    badge = "READ LATER",
                    icon = Icons.Rounded.Queue,
                    onClick = {
                        onDismiss()
                        onOpenUrl(rl.url)
                    }
                )
            )
        }

        history.filter {
            q.isNotBlank() && (it.title.contains(q, true) || it.url.contains(q, true) || it.domain.contains(q, true))
        }.take(6).forEach { h ->
            list.add(
                PaletteSearchResult(
                    key = "his_${h.id}",
                    title = h.title,
                    subtitle = h.url,
                    badge = "HISTORY",
                    icon = Icons.Rounded.History,
                    onClick = {
                        onDismiss()
                        onOpenUrl(h.url)
                    }
                )
            )
        }

        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 28.dp)
                .heightIn(max = 600.dp)
                .testTag("command_palette_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Quick Command Palette",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NewsreaderFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Command Palette")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Jump to tab, bookmark, history, or action…") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("command_palette_input")
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(results, key = { it.key }) { item ->
                        Surface(
                            onClick = item.onClick,
                            shape = RoundedCornerShape(14.dp),
                            color = ext.surfaceContainer,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = item.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (query.isNotBlank()) {
                        item(key = "palette_open_direct") {
                            Surface(
                                onClick = {
                                    onDismiss()
                                    onOpenUrl(query)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Search or open \"$query\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StructuredViewerDialog(
    state: StructuredViewerState,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var wrapLines by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .testTag("structured_viewer_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = state.docType.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = state.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = wrapLines,
                            onClick = { wrapLines = !wrapLines },
                            label = { Text("Wrap") }
                        )
                        IconButton(onClick = { onCopy(state.formattedText) }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy formatted output")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close viewer")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text("Fetching & pretty-formatting document…", style = MaterialTheme.typography.bodySmall)
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ext.surfaceContainer,
                        border = BorderStroke(1.dp, ext.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val vScroll = rememberScrollState()
                        val hScroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(vScroll)
                                .then(if (!wrapLines) Modifier.horizontalScroll(hScroll) else Modifier)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.formattedText.ifBlank { state.rawText.ifBlank { state.error ?: "Empty document" } },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserscriptsDialog(
    scripts: List<UserscriptEntity>,
    userscriptsEnabled: Boolean,
    onToggleGlobal: (Boolean) -> Unit,
    onAddScript: (name: String, urlPattern: String, code: String, isCss: Boolean) -> Unit,
    onToggleScript: (Long, Boolean) -> Unit,
    onDeleteScript: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var name by remember { mutableStateOf("") }
    var urlPattern by remember { mutableStateOf("*") }
    var code by remember { mutableStateOf("") }
    var isCss by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .heightIn(max = 640.dp)
                .testTag("userscripts_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Userscripts & Custom CSS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NewsreaderFamily,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Inject custom CSS or JS per host pattern",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Userscripts")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Userscripts Engine", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = userscriptsEnabled, onCheckedChange = onToggleGlobal)
                }

                HorizontalDivider(color = ext.cardBorder)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ext.surfaceContainer,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Add New Rule / Script", style = MaterialTheme.typography.labelLarge)
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Script Name (e.g. Wide Wikipedia)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = urlPattern,
                                    onValueChange = { urlPattern = it },
                                    label = { Text("URL / Host Pattern (* or wikipedia.org)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = !isCss,
                                        onClick = { isCss = false },
                                        leadingIcon = { Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text("JavaScript") }
                                    )
                                    FilterChip(
                                        selected = isCss,
                                        onClick = { isCss = true },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text("Custom CSS") }
                                    )
                                }
                                OutlinedTextField(
                                    value = code,
                                    onValueChange = { code = it },
                                    label = { Text(if (isCss) "CSS rules…" else "JavaScript code…") },
                                    minLines = 3,
                                    maxLines = 5,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (name.isNotBlank() && code.isNotBlank()) {
                                            onAddScript(name.trim(), urlPattern.trim().ifBlank { "*" }, code.trim(), isCss)
                                            name = ""
                                            urlPattern = "*"
                                            code = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save Userscript")
                                }
                            }
                        }
                    }

                    if (scripts.isEmpty()) {
                        item {
                            Text(
                                text = "No userscripts installed yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(scripts, key = { it.id }) { script ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ext.surfaceContainer,
                                border = BorderStroke(1.dp, ext.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = script.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = if (script.isCss) "CSS" else "JS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Matches: ${script.urlPattern}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = script.enabled,
                                        onCheckedChange = { onToggleScript(script.id, it) }
                                    )
                                    IconButton(onClick = { onDeleteScript(script.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete script")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistDialog(
    items: List<WatchlistItemEntity>,
    canAddCurrent: Boolean,
    onAddCurrent: () -> Unit,
    onCheckNow: () -> Unit,
    onOpenItem: (WatchlistItemEntity) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .heightIn(max = 580.dp)
                .testTag("watchlist_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Page Change Watchlist",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NewsreaderFamily,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Detect updates on release notes, prices, or blogs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Watchlist")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onAddCurrent,
                        enabled = canAddCurrent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Watch Page")
                    }
                    OutlinedButton(
                        onClick = onCheckNow,
                        enabled = items.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Check Now")
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (items.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ext.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No watched pages yet. Open a site and tap Watch Page to save its SHA-256 content snapshot.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            Surface(
                                onClick = { onOpenItem(item) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (item.hasUnreadChange) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else ext.surfaceContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (item.hasUnreadChange) MaterialTheme.colorScheme.primary else ext.cardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.hasUnreadChange) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                ) {
                                                    Text(
                                                        text = "UPDATED",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${item.domain} • Checked ${formatRelativeTime(item.lastCheckedAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDeleteItem(item.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove watchlist item")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightNoteDialog(
    request: HighlightSelectionRequest,
    onSave: (quote: String, note: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var quote by remember(request) { mutableStateOf(request.quoteText) }
    var note by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#91D5AE") }

    val swatches = listOf(
        "#91D5AE" to Color(0xFF91D5AE),
        "#F2C94C" to Color(0xFFF2C94C),
        "#EB8A64" to Color(0xFFEB8A64),
        "#8BB8E8" to Color(0xFF8BB8E8)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Highlight & Private Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = request.domain.ifBlank { request.title },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = quote,
                    onValueChange = { quote = it },
                    label = { Text("Highlighted Quote") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Private Margin Note (Optional)") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Highlight Ink", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    swatches.forEach { (hex, color) ->
                        val active = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25302B).copy(alpha = 0.85f))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quote.isNotBlank()) {
                        onSave(quote.trim(), note.trim(), selectedColor)
                    }
                }
            ) {
                Text("Save Highlight")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
