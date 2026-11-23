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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Https
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.browser.TabState
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
    totalBlockedSession: Long,
    shieldsEnabledForSite: Boolean,
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
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onToggleHttpsOnly: () -> Unit,
    onToggleSiteShields: () -> Unit,
    onBookmarkCurrent: () -> Unit,
    onScreenshot: () -> Unit,
    onPrintPage: () -> Unit,
    onViewSource: () -> Unit,
    onSavePage: () -> Unit,
    onReadAloud: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenNotificationsSettings: () -> Unit,
    onOpenGesturesSettings: () -> Unit,
    onClearData: () -> Unit,
    onSiteInfo: () -> Unit,
    onToggleForceWebDark: () -> Unit = {},
    onOpenTranslate: () -> Unit = {},
    onAddToReadingList: () -> Unit = {},
    onShowQrCode: () -> Unit = {},
    onBurnSession: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onOpenDevTools: () -> Unit = {},
    onToggleBottomOmnibox: () -> Unit = {},
    onChangePageZoom: (Int) -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { 3 })

    val isDarkPalette = preferences.themePalette == ThemePalette.ONYX_FOREST
    val hasActivePageOrEssay = !tab.isHome || tab.readerArticle != null

    val page1 = listOf(
        QuickControlAction("find", "Find", Icons.Rounded.FindInPage, enabled = !tab.isHome) {
            onDismiss(); onToggleFindInPage()
        },
        QuickControlAction("translate", "Translate", Icons.Rounded.Translate, isActive = tab.isTranslated, enabled = !tab.isHome) {
            onDismiss(); onOpenTranslate()
        },
        QuickControlAction("read_later", "Read Later", Icons.Rounded.Queue, enabled = !tab.isHome) {
            onDismiss(); onAddToReadingList()
        },
        QuickControlAction("reader", "Reader", Icons.AutoMirrored.Rounded.MenuBook, isActive = tab.isReaderMode) {
            onDismiss(); onToggleReaderMode()
        },
        QuickControlAction("desktop", "Desktop", Icons.Rounded.DesktopWindows, isActive = tab.isDesktopMode) {
            onToggleDesktopMode(); onDismiss()
        },
        QuickControlAction("web_dark", "Web Dark", Icons.Rounded.NightsStay, isActive = preferences.forceWebDarkMode) {
            onToggleForceWebDark()
        },
        QuickControlAction("adblock", "Shields", Icons.Rounded.Shield, isActive = shieldsEnabledForSite && preferences.adBlockingEnabled, badgeDot = shieldsEnabledForSite) {
            onToggleSiteShields()
        },
        QuickControlAction("bookmarks", "Bookmarks", Icons.Rounded.Bookmarks) {
            onDismiss(); onOpenBookmarks()
        },
        QuickControlAction("history", "History", Icons.Rounded.History) {
            onDismiss(); onOpenHistory()
        },
        QuickControlAction("downloads", "Downloads", Icons.Rounded.Download) {
            onDismiss(); onOpenDownloads()
        }
    )

    val page2 = listOf(
        QuickControlAction("read_aloud", "Read Aloud", Icons.Rounded.GraphicEq) {
            onDismiss(); onReadAloud()
        },
        QuickControlAction("qr_share", "QR Code", Icons.Rounded.QrCode2) {
            onDismiss(); onShowQrCode()
        },
        QuickControlAction("print", "Print", Icons.Rounded.Print, enabled = hasActivePageOrEssay) {
            onDismiss(); onPrintPage()
        },
        QuickControlAction("save_page", "Save Page", Icons.Rounded.SaveAlt, enabled = hasActivePageOrEssay) {
            onDismiss(); onSavePage()
        },
        QuickControlAction("view_source", "Source", Icons.Rounded.Code, enabled = hasActivePageOrEssay) {
            onDismiss(); onViewSource()
        },
        QuickControlAction("pip", "PiP Mode", Icons.Rounded.PictureInPictureAlt, enabled = !tab.isHome) {
            onDismiss(); onEnterPip()
        },
        QuickControlAction("devtools", "DevTools", Icons.Rounded.DeveloperMode, enabled = !tab.isHome) {
            onDismiss(); onOpenDevTools()
        },
        QuickControlAction("bottom_bar", "Bottom Bar", Icons.Rounded.Dock, isActive = preferences.bottomOmnibox) {
            onToggleBottomOmnibox()
        },
        QuickControlAction("share", "Share", Icons.Rounded.Share) {
            onDismiss(); onSharePage()
        },
        QuickControlAction("burn", "Burn", Icons.Rounded.LocalFireDepartment) {
            onDismiss(); onBurnSession()
        }
    )

    val page3 = listOf(
        QuickControlAction("dark", "App Theme", Icons.Rounded.DarkMode, isActive = isDarkPalette) {
            onToggleDarkMode()
        },
        QuickControlAction("new_tab", "New Tab", Icons.Rounded.Add) {
            onDismiss(); onNewTab()
        },
        QuickControlAction("private_tab", "Private", Icons.Rounded.VisibilityOff, isActive = tab.isPrivate) {
            onDismiss(); onNewPrivateTab()
        },
        QuickControlAction("https", "HTTPS-Only", Icons.Rounded.Https, isActive = preferences.httpsOnlyEnabled) {
            onToggleHttpsOnly()
        },
        QuickControlAction("site_info", "Site Info", Icons.Rounded.Info, enabled = !tab.isHome) {
            onDismiss(); onSiteInfo()
        },
        QuickControlAction("add_bm", "Bookmark", Icons.Rounded.BookmarkAdd, enabled = !tab.isHome) {
            onDismiss(); onBookmarkCurrent()
        },
        QuickControlAction("shot", "Capture", Icons.Rounded.CameraAlt) {
            onDismiss(); onScreenshot()
        },
        QuickControlAction("widgets", "Widgets", Icons.Rounded.Widgets) {
            onDismiss(); onOpenWidgets()
        },
        QuickControlAction("notifications", "Alerts", Icons.Rounded.NotificationsActive, isActive = preferences.notificationsEnabled) {
            onDismiss(); onOpenNotificationsSettings()
        },
        QuickControlAction("settings", "Settings", Icons.Rounded.Settings) {
            onDismiss(); onOpenSettings("all")
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ext.surfaceLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Quick Controls",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = preferences.themePalette.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ext.surfaceHigh
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ext.surfaceHigh)
                            .testTag("quick_controls_close")
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close Quick Controls",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Shields Status Banner
            Surface(
                onClick = { onDismiss(); onOpenSettings("shields") },
                shape = RoundedCornerShape(14.dp),
                color = ext.surfaceContainer,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_controls_shields_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (preferences.adBlockingEnabled || preferences.trackerBlockingEnabled)
                                "Ad-Blocker Active • $totalBlockedSession Blocked"
                            else "Ad-Blocker Paused",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3 Security/Profile quick chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickSecurityChip(
                    label = "Vault",
                    icon = Icons.Rounded.Lock,
                    modifier = Modifier.weight(1f),
                    onClick = { onDismiss(); onOpenSettings("vault") }
                )
                QuickSecurityChip(
                    label = preferences.activeProfile.displayName,
                    icon = Icons.Rounded.ManageAccounts,
                    modifier = Modifier.weight(1f),
                    onClick = { onDismiss(); onOpenSettings("vault") }
                )
                QuickSecurityChip(
                    label = "Security",
                    icon = Icons.Rounded.Security,
                    modifier = Modifier.weight(1f),
                    onClick = { onDismiss(); onOpenSettings("shields") }
                )
            }

            if (!tab.isHome) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.surfaceContainer,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Page Zoom",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onChangePageZoom((tab.zoomPercent - 15).coerceAtLeast(50)) },
                                enabled = tab.zoomPercent > 50,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease page zoom")
                            }
                            Text(
                                text = "${tab.zoomPercent}%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { onChangePageZoom((tab.zoomPercent + 15).coerceAtMost(200)) },
                                enabled = tab.zoomPercent < 200,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase page zoom")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3-Page 5x2 Grid
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val items = when (page) {
                    0 -> page1
                    1 -> page2
                    else -> page3
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

            Spacer(Modifier.height(16.dp))

            // Page indicators
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { idx ->
                        val selected = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (selected) 20.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (pagerState.currentPage) {
                        0 -> "SWIPE FOR QR, TRANSLATE, DEVTOOLS & BURN"
                        1 -> "SWIPE FOR THEME, PRIVACY & SITE CONTROLS"
                        else -> "SWIPE BACK"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickSecurityChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = ext.surfaceContainer,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    action: QuickControlAction,
    modifier: Modifier = Modifier
) {
    val ext = LocalRitamExtendedColors.current
    val alpha = if (action.enabled) 1f else 0.4f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = action.enabled, onClick = action.onClick)
            .padding(vertical = 4.dp)
            .testTag("quick_action_${action.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = CircleShape,
                color = if (action.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else ext.surfaceHigh,
                border = BorderStroke(
                    width = if (action.isActive) 1.5.dp else 1.dp,
                    color = if (action.isActive) MaterialTheme.colorScheme.primary else ext.cardBorder
                ),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (action.id == "private_tab") {
                        IncognitoSpectsEmblem(
                            size = 24.dp,
                            tint = if (action.isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                            hatFill = ext.surfaceHigh
                        )
                    } else {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = if (action.isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            if (action.badgeDot) {
                Box(
                    modifier = Modifier
                        .offset(x = (-2).dp, y = 2.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (action.isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
