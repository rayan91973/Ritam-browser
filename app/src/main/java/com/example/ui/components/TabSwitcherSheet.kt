package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TabUnselected
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import com.example.browser.TabState
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

@Composable
fun TabSwitcherOverlay(
    tabs: List<TabState>,
    activeTabId: String,
    recentlyClosedCount: Int = 0,
    autoCloseDays: Int = 0,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onTogglePinTab: (String) -> Unit = {},
    onAssignTabGroup: (String, String?) -> Unit = { _, _ -> },
    onUndoCloseTab: () -> Unit = {},
    onCloseInactiveTabs: () -> Unit = {},
    onNewTab: (Boolean) -> Unit,
    onCloseAllRegular: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val ext = LocalRitamExtendedColors.current
    var showSearch by remember { mutableStateOf(false) }
    var showPrivateOnly by remember { mutableStateOf(false) }
    var selectedGroupFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var groupDialogTab by remember { mutableStateOf<TabState?>(null) }

    val availableGroups = remember(tabs) {
        tabs.mapNotNull { it.groupName?.takeIf { g -> g.isNotBlank() } }.distinct().sorted()
    }

    val staleCutoffMs = remember(autoCloseDays) {
        val thresholdDays = if (autoCloseDays > 0) autoCloseDays else 7
        System.currentTimeMillis() - thresholdDays * 24L * 60L * 60L * 1000L
    }

    val inactiveCount = remember(tabs, staleCutoffMs, activeTabId) {
        tabs.count { !it.isPinned && !it.isPrivate && it.id != activeTabId && it.lastAccessedAt < staleCutoffMs }
    }

    val filteredTabs = remember(tabs, showPrivateOnly, selectedGroupFilter, searchQuery) {
        tabs.filter { tab ->
            val matchesScope = !showPrivateOnly || tab.isPrivate
            val matchesGroup = when (selectedGroupFilter) {
                "ALL" -> true
                "PINNED" -> tab.isPinned
                else -> tab.groupName.equals(selectedGroupFilter, ignoreCase = true)
            }
            val matchesQuery = searchQuery.isBlank() ||
                tab.title.contains(searchQuery, ignoreCase = true) ||
                tab.url.contains(searchQuery, ignoreCase = true) ||
                tab.domain.contains(searchQuery, ignoreCase = true)
            matchesScope && matchesGroup && matchesQuery
        }.sortedByDescending { it.isPinned }
    }

    val hasPrivateTabs = remember(tabs) { tabs.any { it.isPrivate } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ext.surfaceLowest.copy(alpha = if (ext.isDark) 0.78f else 0.56f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = ext.surfaceLow,
            tonalElevation = 6.dp,
            shadowElevation = 18.dp,
            border = BorderStroke(1.dp, ext.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 10.dp)
            ) {
                // Pull Pill Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(ext.surfaceHighest)
                    )
                }

                // Compact Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Open Tabs",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NewsreaderFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = tabs.size.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (hasPrivateTabs || showPrivateOnly) {
                            Surface(
                                onClick = { showPrivateOnly = !showPrivateOnly },
                                shape = RoundedCornerShape(14.dp),
                                color = if (showPrivateOnly) IncognitoColors.SurfaceElevated else ext.surfaceContainer,
                                border = BorderStroke(1.dp, if (showPrivateOnly) IncognitoColors.Accent.copy(alpha = 0.6f) else ext.cardBorder),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    IncognitoSpectsEmblem(
                                        size = 16.dp,
                                        tint = if (showPrivateOnly) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        hatFill = IncognitoColors.Surface
                                    )
                                    Text(
                                        text = "Incognito",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (showPrivateOnly) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showSearch = !showSearch },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = "Search open tabs",
                                tint = if (showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("close_tab_drawer_button")
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Collapse tabs drawer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Filter open tabs…", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Tab Groups & Pinned Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedGroupFilter == "ALL",
                        onClick = { selectedGroupFilter = "ALL" },
                        label = { Text("All (${tabs.size})") }
                    )
                    if (tabs.any { it.isPinned }) {
                        FilterChip(
                            selected = selectedGroupFilter == "PINNED",
                            onClick = { selectedGroupFilter = "PINNED" },
                            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Pinned") }
                        )
                    }
                    availableGroups.forEach { grp ->
                        FilterChip(
                            selected = selectedGroupFilter == grp,
                            onClick = { selectedGroupFilter = grp },
                            leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text(grp) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Compact Horizontal Tab Rows List
                if (filteredTabs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.TabUnselected,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (showPrivateOnly) "No incognito tabs open" else "No matching open tabs",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Begin a quiet, mindful session",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredTabs, key = { it.id }) { tab ->
                            CompactTabDrawerRow(
                                tab = tab,
                                isActive = tab.id == activeTabId,
                                isStale = !tab.isPinned && !tab.isPrivate && tab.id != activeTabId && tab.lastAccessedAt < staleCutoffMs,
                                onSelect = {
                                    onSelectTab(tab.id)
                                    onDismiss()
                                },
                                onTogglePin = { onTogglePinTab(tab.id) },
                                onEditGroup = { groupDialogTab = tab },
                                onClose = { onCloseTab(tab.id) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Quick Bottom Operations Strip (+ New Tab | Private)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onNewTab(false)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("new_regular_tab_button")
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "New Tab",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Surface(
                            onClick = {
                                onNewTab(true)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = IncognitoColors.SurfaceElevated,
                            border = BorderStroke(1.dp, IncognitoColors.Border),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("new_private_tab_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IncognitoSpectsEmblem(
                                    size = 20.dp,
                                    tint = IncognitoColors.Accent,
                                    hatFill = IncognitoColors.Surface
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = "Private",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IncognitoColors.OnSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasPrivateTabs) IncognitoColors.Accent
                                            else IncognitoColors.OnSurfaceMuted.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (recentlyClosedCount > 0) {
                                TextButton(
                                    onClick = onUndoCloseTab,
                                    modifier = Modifier.testTag("undo_close_tab_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reopen ($recentlyClosedCount)", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (inactiveCount > 0) {
                                TextButton(onClick = onCloseInactiveTabs) {
                                    Text("Sweep Inactive ($inactiveCount)", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        TextButton(
                            onClick = onCloseAllRegular,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("close_all_tabs_button")
                        ) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Close All Tabs",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }

    groupDialogTab?.let { target ->
        var customGroup by remember(target.id) { mutableStateOf(target.groupName ?: "") }
        AlertDialog(
            onDismissRequest = { groupDialogTab = null },
            title = { Text("Assign Tab Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Work", "Research", "Reading", "Shopping").forEach { preset ->
                            FilterChip(
                                selected = customGroup.equals(preset, ignoreCase = true),
                                onClick = { customGroup = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customGroup,
                        onValueChange = { customGroup = it },
                        label = { Text("Group Name (empty to clear)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAssignTabGroup(target.id, customGroup.trim().ifBlank { null })
                    groupDialogTab = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { groupDialogTab = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CompactTabDrawerRow(
    tab: TabState,
    isActive: Boolean,
    isStale: Boolean = false,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit = {},
    onEditGroup: () -> Unit = {},
    onClose: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current

    val rowBackground = when {
        tab.isPrivate -> if (isActive) IncognitoColors.SurfaceElevated else IncognitoColors.Surface
        isActive -> ext.surfaceContainer
        else -> ext.surfaceLowest.copy(alpha = 0.85f)
    }
    val primaryText = if (tab.isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface
    val secondaryText = if (tab.isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant

    val leadingIcon = when {
        tab.isReaderMode -> Icons.AutoMirrored.Rounded.MenuBook
        tab.isHome -> Icons.Rounded.Spa
        tab.domain.contains("github", true) || tab.domain.contains("aistudio", true) -> Icons.Rounded.Terminal
        else -> Icons.Rounded.Public
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(14.dp),
        color = rowBackground,
        border = BorderStroke(
            width = if (isActive) 1.2.dp else 0.8.dp,
            color = when {
                tab.isPrivate && isActive -> IncognitoColors.Accent.copy(alpha = 0.7f)
                tab.isPrivate -> IncognitoColors.Border
                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else -> ext.cardBorder.copy(alpha = 0.5f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tab_card_${tab.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon tile with active dot
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        tab.isPrivate -> IncognitoColors.BadgeBg
                        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else -> ext.surfaceHigh
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (tab.isPrivate) {
                            IncognitoSpectsEmblem(
                                size = 20.dp,
                                tint = IncognitoColors.Accent,
                                hatFill = IncognitoColors.Surface
                            )
                        } else {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (tab.isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (tab.isPrivate) IncognitoColors.Accent.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (tab.isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (tab.isPrivate) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IncognitoColors.BadgeBg
                        ) {
                            Text(
                                text = "INCOGNITO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = IncognitoColors.Accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (!tab.groupName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = tab.groupName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (isStale) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "INACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = tab.domain.ifBlank { if (tab.isPrivate) "ritam://incognito" else "ritam://new-tab" },
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onEditGroup,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = "Assign tab group",
                    tint = if (!tab.groupName.isNullOrBlank()) MaterialTheme.colorScheme.primary else secondaryText.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = if (tab.isPinned) "Unpin tab" else "Pin tab",
                    tint = if (tab.isPinned) MaterialTheme.colorScheme.primary else secondaryText.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("close_tab_${tab.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close tab ${tab.title}",
                    tint = secondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
