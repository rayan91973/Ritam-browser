package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.QuickControlUsageSorter
import com.example.browser.TabState
import com.example.data.local.QuickControlUsageEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.ThemePalette
import com.example.ui.theme.LocalRitamExtendedColors

data class QuickControlAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isActive: Boolean = false,
    val badgeDot: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickControlsSheet(
    tab: TabState,
    preferences: BrowserPreferences,
    onDismiss: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: (String) -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleReaderMode: () -> Unit,
    onToggleFindInPage: () -> Unit,
    onSharePage: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onBookmarkCurrent: () -> Unit,
    onScreenshot: () -> Unit,
    onPrintPage: () -> Unit,
    onViewSource: () -> Unit,
    onSavePage: () -> Unit,
    onReadAloud: () -> Unit,
    onToggleForceWebDark: () -> Unit = {},
    onOpenTranslate: () -> Unit = {},
    onAddToReadingList: () -> Unit = {},
    onShowQrCode: () -> Unit = {},
    onBurnSession: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onOpenDevTools: () -> Unit = {},
    onToggleBottomOmnibox: () -> Unit = {},
    onChangePageZoom: (Int) -> Unit = {},
    splitActive: Boolean = false,
    onOpenCommandPalette: () -> Unit = {},
    onToggleSplitScreen: () -> Unit = {},
    onFullPageScreenshot: () -> Unit = {},
    onHighlightSelection: () -> Unit = {},
    onOpenWatchlist: () -> Unit = {},
    onOpenUserscripts: () -> Unit = {},
    onOpenStructuredViewer: () -> Unit = {},
    onInstallWebApp: () -> Unit = {},
    onOpenFeeds: () -> Unit = {},
    usageMap: Map<String, QuickControlUsageEntity> = emptyMap(),
    onRecordActionUse: (String) -> Unit = {},
    onOpenLocalFile: () -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isDarkPalette = preferences.themePalette == ThemePalette.ONYX_FOREST
    val hasActivePageOrEssay = !tab.isHome || tab.readerArticle != null

    val rawActions = listOf(
        QuickControlAction("find", "Find", Icons.Rounded.FindInPage, enabled = !tab.isHome) {
            onRecordActionUse("find"); onDismiss(); onToggleFindInPage()
        },
        QuickControlAction("translate", "Translate", Icons.Rounded.Translate, isActive = tab.isTranslated, enabled = !tab.isHome) {
            onRecordActionUse("translate"); onDismiss(); onOpenTranslate()
        },
        QuickControlAction("read_later", "Read Later", Icons.Rounded.Queue, enabled = !tab.isHome) {
            onRecordActionUse("read_later"); onDismiss(); onAddToReadingList()
        },
        QuickControlAction("reader", "Reader", Icons.AutoMirrored.Rounded.MenuBook, isActive = tab.isReaderMode) {
            onRecordActionUse("reader"); onDismiss(); onToggleReaderMode()
        },
        QuickControlAction("desktop", "Desktop", Icons.Rounded.DesktopWindows, isActive = tab.isDesktopMode) {
            onRecordActionUse("desktop"); onToggleDesktopMode(); onDismiss()
        },
        QuickControlAction("web_dark", "Web Dark", Icons.Rounded.NightsStay, isActive = preferences.forceWebDarkMode) {
            onRecordActionUse("web_dark"); onToggleForceWebDark()
        },
        QuickControlAction("bookmarks", "Bookmarks", Icons.Rounded.Bookmarks) {
            onRecordActionUse("bookmarks"); onDismiss(); onOpenBookmarks()
        },
        QuickControlAction("history", "History", Icons.Rounded.History) {
            onRecordActionUse("history"); onDismiss(); onOpenHistory()
        },
        QuickControlAction("downloads", "Downloads", Icons.Rounded.Download) {
            onRecordActionUse("downloads"); onDismiss(); onOpenDownloads()
        },
        QuickControlAction("open_file", "Open File", Icons.Rounded.FolderOpen) {
            onRecordActionUse("open_file"); onDismiss(); onOpenLocalFile()
        },
        QuickControlAction("read_aloud", "Read Aloud", Icons.Rounded.GraphicEq) {
            onRecordActionUse("read_aloud"); onDismiss(); onReadAloud()
        },
        QuickControlAction("qr_share", "QR Code", Icons.Rounded.QrCode2) {
            onRecordActionUse("qr_share"); onDismiss(); onShowQrCode()
        },
        QuickControlAction("print", "Print", Icons.Rounded.Print, enabled = hasActivePageOrEssay) {
            onRecordActionUse("print"); onDismiss(); onPrintPage()
        },
        QuickControlAction("save_page", "Save Page", Icons.Rounded.SaveAlt, enabled = hasActivePageOrEssay) {
            onRecordActionUse("save_page"); onDismiss(); onSavePage()
        },
        QuickControlAction("view_source", "Source", Icons.Rounded.Code, enabled = hasActivePageOrEssay) {
            onRecordActionUse("view_source"); onDismiss(); onViewSource()
        },
        QuickControlAction("pip", "PiP Mode", Icons.Rounded.PictureInPictureAlt, enabled = !tab.isHome) {
            onRecordActionUse("pip"); onDismiss(); onEnterPip()
        },
        QuickControlAction("devtools", "DevTools", Icons.Rounded.DeveloperMode, enabled = !tab.isHome) {
            onRecordActionUse("devtools"); onDismiss(); onOpenDevTools()
        },
        QuickControlAction("bottom_bar", "Bottom Bar", Icons.Rounded.Dock, isActive = preferences.bottomOmnibox) {
            onRecordActionUse("bottom_bar"); onToggleBottomOmnibox()
        },
        QuickControlAction("share", "Share", Icons.Rounded.Share) {
            onRecordActionUse("share"); onDismiss(); onSharePage()
        },
        QuickControlAction("burn", "Burn", Icons.Rounded.LocalFireDepartment) {
            onRecordActionUse("burn"); onDismiss(); onBurnSession()
        },
        QuickControlAction("dark", "App Theme", Icons.Rounded.DarkMode, isActive = isDarkPalette) {
            onRecordActionUse("dark"); onToggleDarkMode()
        },
        QuickControlAction("add_bm", "Bookmark", Icons.Rounded.BookmarkAdd, enabled = !tab.isHome) {
            onRecordActionUse("add_bm"); onDismiss(); onBookmarkCurrent()
        },
        QuickControlAction("shot", "Capture", Icons.Rounded.CameraAlt) {
            onRecordActionUse("shot"); onDismiss(); onScreenshot()
        },
        QuickControlAction("settings", "Settings", Icons.Rounded.Settings) {
            onRecordActionUse("settings"); onDismiss(); onOpenSettings("all")
        },
        QuickControlAction("cmd_palette", "Jump", Icons.Rounded.Search) {
            onRecordActionUse("cmd_palette"); onDismiss(); onOpenCommandPalette()
        },
        QuickControlAction("split_view", "Split", Icons.Rounded.Splitscreen, isActive = splitActive) {
            onRecordActionUse("split_view"); onDismiss(); onToggleSplitScreen()
        },
        QuickControlAction("full_shot", "Full Shot", Icons.Rounded.PhotoSizeSelectLarge) {
            onRecordActionUse("full_shot"); onDismiss(); onFullPageScreenshot()
        },
        QuickControlAction("highlight", "Highlight", Icons.Rounded.EditNote, enabled = hasActivePageOrEssay) {
            onRecordActionUse("highlight"); onDismiss(); onHighlightSelection()
        },
        QuickControlAction("watchlist", "Watchlist", Icons.Rounded.Visibility) {
            onRecordActionUse("watchlist"); onDismiss(); onOpenWatchlist()
        },
        QuickControlAction("scripts", "Scripts", Icons.Rounded.Terminal, isActive = preferences.userscriptsEnabled) {
            onRecordActionUse("scripts"); onDismiss(); onOpenUserscripts()
        },
        QuickControlAction("format_view", "Format", Icons.Rounded.DataObject, enabled = !tab.isHome) {
            onRecordActionUse("format_view"); onDismiss(); onOpenStructuredViewer()
        },
        QuickControlAction("install_pwa", "Install App", Icons.Rounded.InstallMobile, enabled = !tab.isHome) {
            onRecordActionUse("install_pwa"); onDismiss(); onInstallWebApp()
        },
        QuickControlAction("feeds", "Feeds", Icons.Rounded.RssFeed, badgeDot = !tab.discoveredFeedUrl.isNullOrBlank()) {
            onRecordActionUse("feeds"); onDismiss(); onOpenFeeds()
        }
    )

    val allActions = remember(rawActions, usageMap) {
        QuickControlUsageSorter.sortByUsage(
            items = rawActions,
            idOf = { it.id },
            usageMap = usageMap
        )
    }

    val pages = remember(allActions) { allActions.chunked(10) }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ext.surfaceLow,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
                .padding(bottom = 12.dp)
        ) {
            // Compact Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick Controls",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (usageMap.isNotEmpty()) "TOP USED FIRST" else preferences.themePalette.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ext.surfaceHigh
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${pages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(ext.surfaceHigh)
                            .testTag("quick_controls_close")
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close Quick Controls",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!tab.isHome) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.surfaceContainer,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Page Zoom",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onChangePageZoom((tab.zoomPercent - 15).coerceAtLeast(50)) },
                                enabled = tab.zoomPercent > 50,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Remove,
                                    contentDescription = "Decrease page zoom",
                                    tint = if (tab.zoomPercent > 50) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${tab.zoomPercent}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { onChangePageZoom((tab.zoomPercent + 15).coerceAtMost(200)) },
                                enabled = tab.zoomPercent < 200,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Increase page zoom",
                                    tint = if (tab.zoomPercent < 200) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Compact 5x2 Grid Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val items = pages[page]
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val row1 = items.take(5)
                        row1.forEach { action ->
                            QuickActionTile(action = action, modifier = Modifier.weight(1f))
                        }
                        repeat(5 - row1.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val row2 = items.drop(5).take(5)
                        row2.forEach { action ->
                            QuickActionTile(action = action, modifier = Modifier.weight(1f))
                        }
                        repeat(5 - row2.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Page indicators
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(pages.size) { idx ->
                        val selected = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (selected) 16.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (pagerState.currentPage < pages.lastIndex) "SWIPE FOR MORE TOOLS" else "SWIPE BACK",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp, fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    action: QuickControlAction,
    modifier: Modifier = Modifier
) {
    val ext = LocalRitamExtendedColors.current
    val alpha = if (action.enabled) 1f else 0.65f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = action.enabled, onClick = action.onClick)
            .padding(vertical = 2.dp)
            .testTag("quick_action_${action.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = CircleShape,
                color = if (action.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else ext.surfaceHigh,
                border = BorderStroke(
                    width = if (action.isActive) 1.5.dp else 1.dp,
                    color = if (action.isActive) MaterialTheme.colorScheme.primary else ext.cardBorder
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = if (action.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (action.badgeDot) {
                Box(
                    modifier = Modifier
                        .offset(x = (-1).dp, y = 1.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (action.isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

