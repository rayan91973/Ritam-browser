package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.services.PdfDocumentHelper
import com.example.services.PdfViewerState
import com.example.ui.theme.LocalRitamExtendedColors
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PdfViewerDialog(
    state: PdfViewerState,
    onOpenExternal: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var invertColors by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpTarget by remember { mutableFloatStateOf(1f) }
    val pageCount = state.pageCount
    val currentPage by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + 1).coerceIn(1, pageCount.coerceAtLeast(1))
        }
    }

    val invertColorFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("pdf_viewer_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = ext.surfaceLow,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close PDF viewer")
                        }
                        Icon(
                            Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (pageCount > 0) {
                                    "Page $currentPage of $pageCount • ${(zoomFactor * 100).roundToInt()}%"
                                } else {
                                    "Built-in PDF Reader"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { zoomFactor = (zoomFactor - 0.25f).coerceAtLeast(1f) }) {
                            Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out PDF")
                        }
                        IconButton(onClick = { zoomFactor = (zoomFactor + 0.25f).coerceAtMost(2.5f) }) {
                            Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in PDF")
                        }
                        IconButton(onClick = { invertColors = !invertColors }) {
                            Icon(
                                Icons.Rounded.Contrast,
                                contentDescription = "Toggle night contrast",
                                tint = if (invertColors) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        state.localFile?.let { file ->
                            IconButton(onClick = { onOpenExternal(file) }) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = "Open externally")
                            }
                        }
                    }
                }

                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("Loading PDF document…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    state.error != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Unable to open PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = state.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    state.localFile != null && state.pageCount > 0 -> {
                        val pageIndices = remember(pageCount) { (0 until pageCount).toList() }
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ext.surfaceLowest)
                                    .padding(horizontal = 12.dp),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 84.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(pageIndices, key = { _, idx -> idx }) { _, pageIdx ->
                                    PdfPageCard(
                                        file = state.localFile,
                                        pageIndex = pageIdx,
                                        totalPages = pageCount,
                                        widthPx = (1080 * zoomFactor).roundToInt(),
                                        colorFilter = if (invertColors) invertColorFilter else null
                                    )
                                }
                            }

                            // Page navigator: jumps straight to a page instead of scrolling a long PDF.
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = ext.surfaceHigh,
                                border = BorderStroke(1.dp, ext.cardBorder),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                listState.animateScrollToItem((currentPage - 2).coerceAtLeast(0))
                                            }
                                        },
                                        enabled = currentPage > 1
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous page")
                                    }
                                    Text(
                                        text = "$currentPage / $pageCount",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier
                                            .clickable {
                                                jumpTarget = currentPage.toFloat()
                                                showJumpDialog = true
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                listState.animateScrollToItem((currentPage).coerceAtMost(pageCount - 1))
                                            }
                                        },
                                        enabled = currentPage < pageCount
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next page")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJumpDialog && pageCount > 0) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Go to page") },
            text = {
                Column {
                    Text(
                        text = "Page ${jumpTarget.roundToInt()} of $pageCount",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = jumpTarget,
                        onValueChange = { jumpTarget = it },
                        valueRange = 1f..pageCount.toFloat(),
                        steps = (pageCount - 2).coerceAtLeast(0)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = jumpTarget.roundToInt().coerceIn(1, pageCount) - 1
                    scope.launch { listState.animateScrollToItem(target) }
                    showJumpDialog = false
                }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PdfPageCard(
    file: File,
    pageIndex: Int,
    totalPages: Int,
    widthPx: Int,
    colorFilter: ColorFilter?
) {
    var bitmap by remember(file.absolutePath, pageIndex, widthPx) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.absolutePath, pageIndex, widthPx) {
        bitmap = PdfDocumentHelper.renderPage(file, pageIndex, widthPx)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            val bmp = bitmap
            if (bmp == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1} of $totalPages",
                    contentScale = ContentScale.FillWidth,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = "Page ${pageIndex + 1} / $totalPages",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4F5552),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
