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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.ConsoleLogEntry
import com.example.browser.NetworkLogEntry
import com.example.services.AppCacheStats
import com.example.services.StructuredDocType
import com.example.services.StructuredViewerState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRitamExtendedColors

private data class CdpDomMetrics(
    val totalLines: Int,
    val elementsCount: Int,
    val scriptsCount: Int,
    val imagesCount: Int,
    val linksCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsSheet(
    domain: String,
    consoleLogs: List<ConsoleLogEntry>,
    networkLogs: List<NetworkLogEntry>,
    cookiesDump: String,
    localStoragePairs: List<Pair<String, String>>,
    evalOutput: String,
    onRunJs: (String) -> Unit,
    onRefreshStorage: () -> Unit,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit,
    initialTab: Int = 1,
    pageUrl: String = "",
    pageSourceHtml: String = "",
    structuredState: StructuredViewerState? = null,
    cacheStats: AppCacheStats = AppCacheStats(),
    onRefreshSource: () -> Unit = {},
    onRefreshFormat: () -> Unit = {},
    onClearAppCache: () -> Unit = {},
    onCopyText: (String, String) -> Unit = { _, _ -> }
) {
    val ext = LocalRitamExtendedColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 4)) }

    var jsInput by remember { mutableStateOf("document.title") }
    var consoleFilter by remember { mutableStateOf("ALL") }
    var networkFilter by remember { mutableStateOf("ALL") }
    var sourceQuery by remember { mutableStateOf("") }
    var formatPretty by remember { mutableStateOf(true) }

    val cdpTabs = listOf(
        "Elements",
        "Console",
        "Network",
        "Format",
        "Storage"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ext.surfaceLow,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .testTag("devtools_sheet")
        ) {
            // Minimal CDP Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Icon(
                        Icons.Rounded.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CDP Inspector",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = pageUrl.ifBlank { domain.ifBlank { "about:blank" } },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            when (selectedTab) {
                                0 -> onRefreshSource()
                                3 -> onRefreshFormat()
                                4 -> onRefreshStorage()
                                else -> onClearLogs()
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTab in listOf(0, 3, 4)) Icons.Rounded.Refresh else Icons.Rounded.DeleteSweep,
                            contentDescription = "Action",
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close CDP Inspector",
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Minimal CDP Tab Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cdpTabs.forEachIndexed { idx, label ->
                    val countSuffix = when (idx) {
                        1 -> if (consoleLogs.isNotEmpty()) " (${consoleLogs.size})" else ""
                        2 -> if (networkLogs.isNotEmpty()) " (${networkLogs.size})" else ""
                        else -> ""
                    }
                    val active = selectedTab == idx
                    Surface(
                        onClick = {
                            selectedTab = idx
                            when (idx) {
                                0 -> if (pageSourceHtml.isBlank()) onRefreshSource()
                                3 -> if (structuredState == null) onRefreshFormat()
                                4 -> onRefreshStorage()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else ext.surfaceContainer,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (active) MaterialTheme.colorScheme.primary else ext.cardBorder
                        )
                    ) {
                        Text(
                            text = "$label$countSuffix",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                0 -> CdpElementsPane(
                    htmlSource = pageSourceHtml,
                    query = sourceQuery,
                    onQueryChange = { sourceQuery = it },
                    onRefresh = onRefreshSource,
                    onCopy = { onCopyText(pageSourceHtml, "HTML Source") }
                )

                1 -> CdpConsolePane(
                    logs = consoleLogs,
                    filter = consoleFilter,
                    onFilterChange = { consoleFilter = it },
                    jsInput = jsInput,
                    onJsInputChange = { jsInput = it },
                    evalOutput = evalOutput,
                    onRunJs = onRunJs
                )

                2 -> CdpNetworkPane(
                    logs = networkLogs,
                    filter = networkFilter,
                    onFilterChange = { networkFilter = it }
                )

                3 -> CdpFormatPane(
                    state = structuredState,
                    prettyMode = formatPretty,
                    onToggleMode = { formatPretty = it },
                    onRefresh = onRefreshFormat,
                    onCopy = { text -> onCopyText(text, "Formatted Document") }
                )

                4 -> CdpStorageAndCachePane(
                    cookiesDump = cookiesDump,
                    localStoragePairs = localStoragePairs,
                    cacheStats = cacheStats,
                    onRefresh = onRefreshStorage,
                    onClearCache = onClearAppCache,
                    onCopyCookie = { text -> onCopyText(text, "Cookie") }
                )
            }
        }
    }
}

@Composable
private fun CdpElementsPane(
    htmlSource: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCopy: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    val lines = remember(htmlSource) { htmlSource.lines() }
    val metrics = remember(htmlSource) {
        if (htmlSource.isBlank()) {
            CdpDomMetrics(0, 0, 0, 0, 0)
        } else {
            CdpDomMetrics(
                totalLines = lines.size,
                elementsCount = Regex("<[a-zA-Z]").findAll(htmlSource).count(),
                scriptsCount = Regex("<script\\b", RegexOption.IGNORE_CASE).findAll(htmlSource).count(),
                imagesCount = Regex("<img\\b", RegexOption.IGNORE_CASE).findAll(htmlSource).count(),
                linksCount = Regex("<a\\b", RegexOption.IGNORE_CASE).findAll(htmlSource).count()
            )
        }
    }
    val filteredLines = remember(lines, query) {
        val indexed = lines.mapIndexed { i, line -> (i + 1) to line }
        if (query.isBlank()) indexed.take(500)
        else indexed.filter { (_, line) -> line.contains(query, ignoreCase = true) }.take(300)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DOM ${metrics.elementsCount} • JS ${metrics.scriptsCount} • IMG ${metrics.imagesCount} • A ${metrics.linksCount}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(6.dp),
                    color = ext.surfaceContainer,
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Text(
                        text = "Pull DOM",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (htmlSource.isNotBlank()) {
                    Surface(
                        onClick = onCopy,
                        shape = RoundedCornerShape(6.dp),
                        color = ext.surfaceContainer,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Filter DOM nodes, tags, IDs…",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp),
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ext.surfaceLowest,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 310.dp)
        ) {
            if (htmlSource.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tap 'Pull DOM' on an active webpage to inspect HTML elements.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredLines, key = { it.first }) { (lineNo, line) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = lineNo.toString().padStart(3, ' '),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                            Text(
                                text = line.trimStart().ifBlank { " " },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                ),
                                color = if (line.contains("<script", true) || line.contains("<link", true)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
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

@Composable
private fun CdpConsolePane(
    logs: List<ConsoleLogEntry>,
    filter: String,
    onFilterChange: (String) -> Unit,
    jsInput: String,
    onJsInputChange: (String) -> Unit,
    evalOutput: String,
    onRunJs: (String) -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    val filtered = remember(logs, filter) {
        when (filter) {
            "ERR" -> logs.filter { it.level.contains("ERROR", true) }
            "WRN" -> logs.filter { it.level.contains("WARN", true) }
            else -> logs
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "ERR", "WRN").forEach { key ->
                CdpMiniPill(
                    label = key,
                    selected = filter == key,
                    onClick = { onFilterChange(key) }
                )
            }
            listOf(
                "title" to "document.title",
                "links" to "document.links.length",
                "imgs" to "document.images.length",
                "size" to "innerWidth + 'x' + innerHeight"
            ).forEach { (label, expr) ->
                CdpMiniPill(
                    label = label,
                    selected = jsInput == expr,
                    onClick = {
                        onJsInputChange(expr)
                        onRunJs(expr)
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = jsInput,
                onValueChange = onJsInputChange,
                prefix = {
                    Text(
                        "> ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                placeholder = { Text("Eval JS") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp),
                modifier = Modifier.weight(1f)
            )
            Surface(
                onClick = { if (jsInput.isNotBlank()) onRunJs(jsInput) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Run JS", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        if (evalOutput.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ext.surfaceLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "<- $evalOutput",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ext.surfaceLowest,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp, max = 250.dp)
        ) {
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Console clean • 0 entries matching $filter",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(filtered) { idx, log ->
                        val isError = log.level.contains("ERROR", true)
                        val isWarn = log.level.contains("WARN", true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when {
                                    isError -> "ERR"
                                    isWarn -> "WRN"
                                    else -> "LOG"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = when {
                                    isError -> MaterialTheme.colorScheme.error
                                    isWarn -> Color(0xFFD99B26)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ":${log.lineNumber}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (idx < filtered.lastIndex) {
                            HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CdpNetworkPane(
    logs: List<NetworkLogEntry>,
    filter: String,
    onFilterChange: (String) -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    val blockedCount = remember(logs) { logs.count { it.isBlocked } }
    val filtered = remember(logs, filter) {
        when (filter) {
            "BLK" -> logs.filter { it.isBlocked }
            "OK" -> logs.filter { !it.isBlocked }
            else -> logs
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CdpMiniPill("All (${logs.size})", filter == "ALL") { onFilterChange("ALL") }
                CdpMiniPill("Blocked ($blockedCount)", filter == "BLK") { onFilterChange("BLK") }
                CdpMiniPill("Allowed (${logs.size - blockedCount})", filter == "OK") { onFilterChange("OK") }
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ext.surfaceLowest,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 300.dp)
        ) {
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No network requests captured yet.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(filtered) { idx, req ->
                        val shortPath = remember(req.url) {
                            req.url.substringAfter(req.host, req.url)
                                .ifBlank { "/" }
                                .take(90)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (req.isBlocked) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (req.isBlocked) req.category.take(3).uppercase() else "200",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (req.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.host.ifBlank { "resource" },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = shortPath,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (idx < filtered.lastIndex) {
                            HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CdpFormatPane(
    state: StructuredViewerState?,
    prettyMode: Boolean,
    onToggleMode: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onCopy: (String) -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    val shownText = remember(state, prettyMode) {
        if (state == null) ""
        else if (prettyMode) state.formattedText.ifBlank { state.rawText }
        else state.rawText.ifBlank { state.formattedText }
    }
    val keyColor = MaterialTheme.colorScheme.primary
    val strColor = MaterialTheme.colorScheme.tertiary
    val highlighted: AnnotatedString? = remember(shownText, prettyMode, state?.docType) {
        if (prettyMode && state?.docType == StructuredDocType.JSON && shownText.length <= 24_000) {
            highlightCdpJson(shownText, keyColor, strColor)
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CdpMiniPill("Pretty", prettyMode) { onToggleMode(true) }
                CdpMiniPill("Raw", !prettyMode) { onToggleMode(false) }
                state?.let {
                    CdpMiniPill(it.docType.label, true) {}
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(6.dp),
                    color = ext.surfaceContainer,
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Text(
                        "Format Page",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (shownText.isNotBlank()) {
                    Surface(
                        onClick = { onCopy(shownText) },
                        shape = RoundedCornerShape(6.dp),
                        color = ext.surfaceContainer,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Text(
                            "Copy",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ext.surfaceLowest,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 310.dp)
        ) {
            when {
                state == null -> Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Tap 'Format Page' to inspect pretty JSON, XML, Markdown, or page structure.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Formatting payload…",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    if (highlighted != null) {
                        Text(
                            text = highlighted,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        )
                    } else {
                        Text(
                            text = shownText.ifBlank { "(empty payload)" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CdpStorageAndCachePane(
    cookiesDump: String,
    localStoragePairs: List<Pair<String, String>>,
    cacheStats: AppCacheStats,
    onRefresh: () -> Unit,
    onClearCache: () -> Unit,
    onCopyCookie: (String) -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    val parsedCookies = remember(cookiesDump) {
        cookiesDump.split(";")
            .mapNotNull { part ->
                val trimmed = part.trim()
                if (trimmed.isBlank()) null
                else {
                    val eq = trimmed.indexOf('=')
                    if (eq > 0) trimmed.substring(0, eq).trim() to trimmed.substring(eq + 1).trim()
                    else trimmed to ""
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Application Cache Breakdown Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ext.surfaceContainer,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "APPLICATION CACHE • ${formatBytes(cacheStats.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            onClick = onClearCache,
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Clear Cache",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "HTTP ${formatBytes(cacheStats.httpCacheBytes)} • Docs ${formatBytes(cacheStats.previewCacheBytes)} • WebView ${formatBytes(cacheStats.webViewCacheBytes)} • ${cacheStats.cachedFileCount} cached files",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    text = "COOKIES (${parsedCookies.size}) & LOCAL STORAGE (${localStoragePairs.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh storage", modifier = Modifier.size(15.dp))
                }
            }
        }

        if (parsedCookies.isEmpty()) {
            item {
                Text(
                    text = "No script-accessible cookies for this origin.",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(parsedCookies) { (name, value) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ext.surfaceLowest,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.4f)
                        )
                        Text(
                            text = value.ifBlank { "(empty)" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.6f)
                        )
                        IconButton(onClick = { onCopyCookie("$name=$value") }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy cookie", modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }

        if (localStoragePairs.isNotEmpty()) {
            items(localStoragePairs) { (k, v) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ext.surfaceLowest,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = k,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.4f)
                        )
                        Text(
                            text = v,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.5.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CdpMiniPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else ext.surfaceContainer,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun highlightCdpJson(raw: String, keyColor: Color, stringColor: Color): AnnotatedString {
    val tokenRegex = Regex("""("[^"\\]*(?:\\.[^"\\]*)*"\s*:)|("[^"\\]*(?:\\.[^"\\]*)*")""")
    return buildAnnotatedString {
        var cursor = 0
        tokenRegex.findAll(raw).forEach { m ->
            if (m.range.first > cursor) {
                append(raw.substring(cursor, m.range.first))
            }
            val color = if (m.groupValues[1].isNotEmpty()) keyColor else stringColor
            withStyle(SpanStyle(color = color)) {
                append(m.value)
            }
            cursor = m.range.last + 1
        }
        if (cursor < raw.length) {
            append(raw.substring(cursor))
        }
    }
}
