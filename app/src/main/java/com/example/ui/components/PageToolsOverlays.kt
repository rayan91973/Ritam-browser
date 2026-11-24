package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.browser.PageSourceDialogState
import com.example.services.ReadAloudState
import com.example.ui.theme.LocalRitamExtendedColors

@Composable
fun ReadAloudFloatingBar(
    state: ReadAloudState,
    isPrivate: Boolean = false,
    onTogglePlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCycleSpeed: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isActive) return
    val ext = LocalRitamExtendedColors.current
    val bgColor = if (isPrivate) IncognitoColors.SurfaceElevated else ext.surfaceLow
    val borderColor = if (isPrivate) IncognitoColors.Border else ext.cardBorder
    val primaryText = if (isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface
    val mutedText = if (isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant
    val accent = if (isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("read_aloud_bar"),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Read Aloud active",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title.ifBlank { "Read Aloud" },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.statusNote ?: "Paragraph ${state.currentIndex + 1} of ${state.totalCount.coerceAtLeast(1)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedText,
                        maxLines = 1
                    )
                }

                Surface(
                    onClick = onCycleSpeed,
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.testTag("read_aloud_speed_button")
                ) {
                    Text(
                        text = "${state.speechRate}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(34.dp).testTag("read_aloud_prev_button")
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous paragraph",
                        tint = primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier.size(34.dp).testTag("read_aloud_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause reading" else "Resume reading",
                            tint = if (isPrivate) IncognitoColors.Backdrop else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(34.dp).testTag("read_aloud_next_button")
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next paragraph",
                        tint = primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(34.dp).testTag("read_aloud_close_button")
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Stop Read Aloud",
                        tint = mutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accent,
                trackColor = borderColor.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun PageSourceDialog(
    state: PageSourceDialogState,
    onCopySource: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var filterQuery by remember { mutableStateOf("") }
    val lines = remember(state.htmlSource) { state.htmlSource.lines() }
    val displayedSource = remember(state.htmlSource, filterQuery) {
        if (filterQuery.isBlank()) {
            state.htmlSource.take(45_000)
        } else {
            lines.mapIndexedNotNull { idx, line ->
                if (line.contains(filterQuery, ignoreCase = true)) {
                    "${idx + 1}: $line"
                } else null
            }.joinToString("\n").ifBlank { "No matching source lines for \"$filterQuery\"" }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .testTag("page_source_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, ext.cardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "View Page Source",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${lines.size} lines • ${state.url}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onCopySource(state.htmlSource) },
                            modifier = Modifier.size(48.dp).testTag("source_copy_button")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy HTML source")
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp).testTag("source_close_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close source viewer")
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    singleLine = true,
                    placeholder = { Text("Filter HTML tags, scripts, meta…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    val vScroll = rememberScrollState()
                    val hScroll = rememberScrollState()
                    SelectionContainer {
                        Text(
                            text = displayedSource,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(vScroll)
                                .horizontalScroll(hScroll)
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeWidgetPreviewDialog(
    onPinWidget: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .testTag("home_widget_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ritam Home Widget",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Launch private search, voice lookup, or a dark Incognito tab directly from your Android home screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                // Live miniature widget preview
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF232B27),
                    border = BorderStroke(1.dp, Color(0xFF56655D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF313D37))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Color(0xFF91D5AE),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Search with Ritam…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF3F7F4),
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF313D37)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = Color(0xFF91D5AE),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF313D37)),
                            contentAlignment = Alignment.Center
                        ) {
                            IncognitoSpectsEmblem(
                                size = 18.dp,
                                tint = IncognitoColors.Accent,
                                hatFill = IncognitoColors.Surface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            onPinWidget()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("pin_widget_confirm_button")
                    ) {
                        Text("Add Widget")
                    }
                }
            }
        }
    }
}
