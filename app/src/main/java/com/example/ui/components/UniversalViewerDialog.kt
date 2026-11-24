package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.services.ExcelSheet
import com.example.services.MarkdownBlock
import com.example.services.PdfDocumentHelper
import com.example.services.PptSlide
import com.example.services.UniversalFileType
import com.example.services.UniversalViewerState
import com.example.services.WordBlock
import com.example.services.WordBlockKind
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun UniversalViewerDialog(
    state: UniversalViewerState,
    onCopyText: (String) -> Unit,
    onOpenExternal: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("universal_viewer_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Unified Minimal Top Bar
                Surface(
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(44.dp).testTag("universal_viewer_close")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close viewer")
                        }
                        Icon(
                            imageVector = viewerIconFor(state.fileType),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.title.ifBlank { state.fileType.label },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = buildString {
                                append(state.fileType.label)
                                if (state.fileSizeBytes > 0L) append(" • ${formatBytes(state.fileSizeBytes)}")
                                when (state.fileType) {
                                    UniversalFileType.PDF -> if (state.pdfPageCount > 0) append(" • ${state.pdfPageCount}p")
                                    UniversalFileType.PPT -> if (state.pptSlides.isNotEmpty()) append(" • ${state.pptSlides.size} slides")
                                    UniversalFileType.EXCEL -> if (state.excelSheets.isNotEmpty()) append(" • ${state.excelSheets.size} sheet(s)")
                                    else -> Unit
                                }
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (state.rawText.isNotBlank()) {
                            IconButton(
                                onClick = { onCopyText(state.rawText) },
                                modifier = Modifier.size(42.dp).testTag("universal_viewer_copy")
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(18.dp))
                            }
                        }
                        state.localFile?.takeIf { it.exists() }?.let { file ->
                            IconButton(
                                onClick = { onOpenExternal(file) },
                                modifier = Modifier.size(42.dp).testTag("universal_viewer_external")
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = "Open externally", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Content Body
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Opening ${state.fileType.label}…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                        text = "Unable to open ${state.fileType.label}",
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

                        else -> when (state.fileType) {
                            UniversalFileType.IMAGE -> state.localFile?.let { BuiltInImageViewer(it, state.title) }
                            UniversalFileType.VIDEO -> state.localFile?.let { BuiltInVideoPlayer(it) }
                            UniversalFileType.AUDIO -> state.localFile?.let { BuiltInAudioPlayer(it, state.title) }
                            UniversalFileType.PDF -> state.localFile?.let { BuiltInPdfViewer(it, state.pdfPageCount) }
                            UniversalFileType.WORD -> BuiltInWordViewer(state.wordBlocks)
                            UniversalFileType.PPT -> BuiltInPptViewer(state.pptSlides)
                            UniversalFileType.EXCEL -> BuiltInExcelViewer(state.excelSheets)
                            UniversalFileType.MARKDOWN -> BuiltInMarkdownViewer(state.markdownBlocks, state.rawText)
                            UniversalFileType.TEXT -> BuiltInPlainTextViewer(state.rawText)
                        }
                    }
                }
            }
        }
    }
}

private fun viewerIconFor(type: UniversalFileType): ImageVector = when (type) {
    UniversalFileType.IMAGE -> Icons.Rounded.Image
    UniversalFileType.VIDEO -> Icons.Rounded.Movie
    UniversalFileType.AUDIO -> Icons.Rounded.AudioFile
    UniversalFileType.PDF -> Icons.Rounded.PictureAsPdf
    UniversalFileType.WORD -> Icons.Rounded.Description
    UniversalFileType.PPT -> Icons.Rounded.Slideshow
    UniversalFileType.EXCEL -> Icons.Rounded.TableChart
    UniversalFileType.MARKDOWN -> Icons.Rounded.Code
    UniversalFileType.TEXT -> Icons.Rounded.Description
}

// ---------------- 1. IMAGE VIEWER ----------------

@Composable
private fun BuiltInImageViewer(file: File, title: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1110))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 4.5f)
                    scale = nextScale
                    offset = if (nextScale > 1.01f) {
                        Offset(offset.x + pan.x, offset.y + pan.y)
                    } else Offset.Zero
                }
            }
    ) {
        AsyncImage(
            model = file,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation.toFloat()
                    translationX = offset.x
                    translationY = offset.y
                }
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xCC181C1A),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = {
                        scale = (scale - 0.5f).coerceAtLeast(1f)
                        if (scale <= 1f) offset = Offset.Zero
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "${(scale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                IconButton(
                    onClick = { scale = (scale + 0.5f).coerceAtMost(4.5f) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { rotation = (rotation + 90) % 360 },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90 degrees", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                if (scale > 1f || rotation != 0) {
                    IconButton(
                        onClick = {
                            scale = 1f
                            rotation = 0
                            offset = Offset.Zero
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reset view", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ---------------- 2. VIDEO PLAYER ----------------

@Composable
private fun BuiltInVideoPlayer(file: File) {
    val ext = LocalRitamExtendedColors.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(videoViewRef, isPlaying) {
        while (true) {
            val vv = videoViewRef
            if (vv != null) {
                runCatching {
                    if (vv.duration > 0) durationMs = vv.duration
                    positionMs = vv.currentPosition
                    isPlaying = vv.isPlaying
                }
            }
            delay(350L)
        }
    }

    DisposableEffect(file.absolutePath) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.fromFile(file))
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            durationMs = mp.duration
                            mp.isLooping = false
                            start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Surface(
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Slider(
                    value = if (durationMs > 0) positionMs.toFloat().coerceIn(0f, durationMs.toFloat()) else 0f,
                    onValueChange = { target ->
                        positionMs = target.toInt()
                        videoViewRef?.seekTo(positionMs)
                    },
                    valueRange = 0f..(durationMs.coerceAtLeast(1).toFloat())
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatDurationMs(positionMs)} / ${formatDurationMs(durationMs)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val next = (positionMs - 10_000).coerceAtLeast(0)
                                videoViewRef?.seekTo(next)
                                positionMs = next
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "Back 10s")
                        }
                        Surface(
                            onClick = {
                                val vv = videoViewRef ?: return@Surface
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val next = (positionMs + 10_000).coerceAtMost(durationMs.coerceAtLeast(0))
                                videoViewRef?.seekTo(next)
                                positionMs = next
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s")
                        }
                    }
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            val vol = if (isMuted) 0f else 1f
                            runCatching { mediaPlayerRef?.setVolume(vol, vol) }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute"
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 3. AUDIO PLAYER ----------------

@Composable
private fun BuiltInAudioPlayer(file: File, title: String) {
    val ext = LocalRitamExtendedColors.current
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }
    var speed by remember { mutableFloatStateOf(1f) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val player = remember(file.absolutePath) {
        MediaPlayer().apply {
            runCatching {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                durationMs = duration.coerceAtLeast(0)
                start()
                isPlaying = true
                setOnCompletionListener { isPlaying = false }
            }.onFailure {
                loadError = "Unable to decode audio stream"
            }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            runCatching {
                if (player.isPlaying) {
                    positionMs = player.currentPosition
                }
                isPlaying = player.isPlaying
            }
            delay(300L)
        }
    }

    DisposableEffect(player) {
        onDispose {
            runCatching {
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.extension.uppercase().ifBlank { "AUDIO" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                val errorText = loadError
                if (errorText != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    Spacer(Modifier.height(18.dp))
                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1).toFloat()),
                        onValueChange = { target ->
                            positionMs = target.toInt()
                            runCatching { player.seekTo(positionMs) }
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1).toFloat()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatDurationMs(positionMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatDurationMs(durationMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = speed != 1f,
                            onClick = {
                                speed = when (speed) {
                                    1f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2f
                                    else -> 1f
                                }
                                runCatching {
                                    player.playbackParams = player.playbackParams.setSpeed(speed)
                                }
                            },
                            label = { Text("${speed}x") }
                        )
                        IconButton(onClick = {
                            val next = (positionMs - 10_000).coerceAtLeast(0)
                            runCatching { player.seekTo(next) }
                            positionMs = next
                        }) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s")
                        }
                        Surface(
                            onClick = {
                                runCatching {
                                    if (player.isPlaying) {
                                        player.pause()
                                        isPlaying = false
                                    } else {
                                        player.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        IconButton(onClick = {
                            val next = (positionMs + 10_000).coerceAtMost(durationMs)
                            runCatching { player.seekTo(next) }
                            positionMs = next
                        }) {
                            Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s")
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 4. PDF VIEWER ----------------

@Composable
private fun BuiltInPdfViewer(file: File, pageCount: Int) {
    val ext = LocalRitamExtendedColors.current
    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var invertColors by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpTarget by remember { mutableFloatStateOf(1f) }
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ext.surfaceLow)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Page $currentPage of $pageCount • ${(zoomFactor * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { zoomFactor = (zoomFactor - 0.25f).coerceAtLeast(1f) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out PDF", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { zoomFactor = (zoomFactor + 0.25f).coerceAtMost(2.5f) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in PDF", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { invertColors = !invertColors }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.Contrast,
                        contentDescription = "Invert contrast",
                        tint = if (invertColors) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        val pageIndices = remember(pageCount) { (0 until pageCount).toList() }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                    var bmp by remember(file.absolutePath, pageIdx, zoomFactor) { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(file.absolutePath, pageIdx, zoomFactor) {
                        bmp = PdfDocumentHelper.renderPage(file, pageIdx, (1080 * zoomFactor).roundToInt())
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            val renderedPage = bmp
                            if (renderedPage == null) {
                                Box(modifier = Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(26.dp))
                                }
                            } else {
                                Image(
                                    bitmap = renderedPage.asImageBitmap(),
                                    contentDescription = "PDF Page ${pageIdx + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    colorFilter = if (invertColors) invertColorFilter else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(
                                text = "Page ${pageIdx + 1} / $pageCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4F5552),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

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
                                listState.animateScrollToItem(currentPage.coerceAtMost(pageCount - 1))
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

// ---------------- 5. WORD (.doc / .docx) VIEWER ----------------

@Composable
private fun BuiltInWordViewer(blocks: List<WordBlock>) {
    val ext = LocalRitamExtendedColors.current
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(blocks, searchQuery) {
        if (searchQuery.isBlank()) blocks
        else blocks.filter { b ->
            b.text.contains(searchQuery, true) ||
                b.tableRows.any { row -> row.any { cell -> cell.contains(searchQuery, true) } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            placeholder = { Text("Search inside document…", style = MaterialTheme.typography.bodySmall) },
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
        if (searchQuery.isNotBlank()) {
            Text(
                text = if (filtered.isEmpty()) "No matches in this document"
                else "${filtered.size} matching block${if (filtered.size == 1) "" else "s"} of ${blocks.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 4.dp)
            )
        }
        SelectionContainer {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { block ->
                    when (block.kind) {
                        WordBlockKind.HEADING1 -> Text(
                            text = block.text,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = NewsreaderFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        WordBlockKind.HEADING2 -> Text(
                            text = block.text,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        WordBlockKind.BULLET -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        WordBlockKind.PARAGRAPH -> Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        WordBlockKind.TABLE -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ext.surfaceLow,
                                border = BorderStroke(1.dp, ext.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    block.tableRows.forEachIndexed { rIdx, row ->
                                        Row(
                                            modifier = Modifier.background(
                                                if (rIdx == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                else Color.Transparent
                                            )
                                        ) {
                                            row.forEach { cell ->
                                                Text(
                                                    text = cell,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (rIdx == 0) FontWeight.SemiBold else FontWeight.Normal
                                                    ),
                                                    modifier = Modifier
                                                        .width(130.dp)
                                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                        if (rIdx < block.tableRows.lastIndex) {
                                            HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
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
}

// ---------------- 6. POWERPOINT (.ppt / .pptx) VIEWER ----------------

@Composable
private fun BuiltInPptViewer(slides: List<PptSlide>) {
    val ext = LocalRitamExtendedColors.current
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    if (slides.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ext.surfaceLowest)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Slide Navigator Strip
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(slides) { idx, _ ->
                FilterChip(
                    selected = pagerState.currentPage == idx,
                    onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                    label = { Text("Slide ${idx + 1}", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { idx ->
            val slide = slides[idx]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f, matchHeightConstraintsFirst = false)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SLIDE ${slide.index}",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${idx + 1} / ${slides.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NewsreaderFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = ext.cardBorder)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            slide.bullets.forEach { bullet ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = bullet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (slide.notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ext.surfaceContainer,
                        border = BorderStroke(1.dp, ext.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "SPEAKER NOTES",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = slide.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Prev / Next Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                    scope.launch { pagerState.animateScrollToPage(prev) }
                },
                enabled = pagerState.currentPage > 0
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous slide")
            }
            Text(
                text = "Slide ${pagerState.currentPage + 1} of ${slides.size}",
                style = MaterialTheme.typography.labelMedium
            )
            IconButton(
                onClick = {
                    val next = (pagerState.currentPage + 1).coerceAtMost(slides.lastIndex)
                    scope.launch { pagerState.animateScrollToPage(next) }
                },
                enabled = pagerState.currentPage < slides.lastIndex
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next slide")
            }
        }
    }
}

// ---------------- 7. EXCEL (.xls / .xlsx / .csv) VIEWER ----------------

@Composable
private fun BuiltInExcelViewer(sheets: List<ExcelSheet>) {
    val ext = LocalRitamExtendedColors.current
    if (sheets.isEmpty()) return

    var activeSheetIdx by remember(sheets) { mutableIntStateOf(0) }
    val sheet = sheets.getOrElse(activeSheetIdx) { sheets.first() }
    var selectedCellRef by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val selectedValue = selectedCellRef?.let { (r, c) ->
        sheet.rows.getOrNull(r)?.getOrNull(c).orEmpty()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (sheets.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sheets.forEachIndexed { idx, s ->
                    FilterChip(
                        selected = activeSheetIdx == idx,
                        onClick = {
                            activeSheetIdx = idx
                            selectedCellRef = null
                        },
                        label = { Text(s.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Formula/Cell Inspector Bar
        Surface(
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cellTag = selectedCellRef?.let { (r, c) -> "${colLetter(c)}${r + 1}" } ?: "Cell"
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = cellTag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = selectedValue?.ifBlank { "(empty)" } ?: "Tap any cell to inspect value",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val hScroll = rememberScrollState()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(hScroll)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Column header bar (A, B, C...)
            item {
                Row(
                    modifier = Modifier.background(ext.surfaceHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    repeat(sheet.maxCols) { cIdx ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = colLetter(cIdx),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                HorizontalDivider(color = ext.cardBorder)
            }

            itemsIndexed(sheet.rows) { rIdx, row ->
                val isSelectedRow = selectedCellRef?.first == rIdx
                Row(
                    modifier = Modifier.background(
                        when {
                            rIdx == 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            rIdx % 2 == 1 -> ext.surfaceLow
                            else -> Color.Transparent
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .background(if (isSelectedRow) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else ext.surfaceLow)
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${rIdx + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    row.forEachIndexed { cIdx, cell ->
                        val isCellSelected = selectedCellRef == (rIdx to cIdx)
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .background(
                                    if (isCellSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { selectedCellRef = rIdx to cIdx }
                                .padding(horizontal = 8.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (rIdx == 0) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.45f))
            }
        }
    }
}

private fun colLetter(index: Int): String {
    var n = index + 1
    val sb = StringBuilder()
    while (n > 0) {
        n--
        sb.insert(0, ('A' + (n % 26)))
        n /= 26
    }
    return sb.toString()
}

// ---------------- 8. MARKDOWN (.md) VIEWER ----------------

@Composable
private fun BuiltInMarkdownViewer(blocks: List<MarkdownBlock>, rawText: String) {
    val ext = LocalRitamExtendedColors.current
    var showRaw by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ext.surfaceLow)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !showRaw,
                onClick = { showRaw = false },
                label = { Text("Rendered") }
            )
            FilterChip(
                selected = showRaw,
                onClick = { showRaw = true },
                label = { Text("Raw Source") }
            )
        }

        if (showRaw) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BuiltInPlainTextViewer(rawText)
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(blocks) { block ->
                        when (block) {
                            is MarkdownBlock.Heading -> {
                                val style = when (block.level) {
                                    1 -> MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = NewsreaderFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    2 -> MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = NewsreaderFamily,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = renderInlineMarkdown(block.text, MaterialTheme.colorScheme.primary),
                                    style = style,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            is MarkdownBlock.Paragraph -> Text(
                                text = renderInlineMarkdown(block.text, MaterialTheme.colorScheme.primary),
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            is MarkdownBlock.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = block.ordinalLabel ?: "•",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = renderInlineMarkdown(block.text, MaterialTheme.colorScheme.primary),
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            is MarkdownBlock.Quote -> Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = renderInlineMarkdown(block.text, MaterialTheme.colorScheme.primary),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            is MarkdownBlock.CodeBlock -> Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ext.surfaceLowest,
                                border = BorderStroke(1.dp, ext.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (block.language.isNotBlank()) {
                                        Text(
                                            text = block.language.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                        Text(
                                            text = block.code,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                lineHeight = 17.sp
                                            )
                                        )
                                    }
                                }
                            }
                            is MarkdownBlock.Table -> Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ext.surfaceLow,
                                border = BorderStroke(1.dp, ext.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    Row(modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))) {
                                        block.headers.forEach { h ->
                                            Text(
                                                text = h,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.width(125.dp).padding(10.dp)
                                            )
                                        }
                                    }
                                    block.rows.forEach { r ->
                                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                        Row {
                                            r.forEach { cell ->
                                                Text(
                                                    text = cell,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.width(125.dp).padding(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            MarkdownBlock.Divider -> HorizontalDivider(color = ext.cardBorder)
                        }
                    }
                }
            }
        }
    }
}

private fun renderInlineMarkdown(text: String, accentColor: Color): AnnotatedString {
    val tokenRegex = Regex("""(\*\*[^*]+\*\*|`[^`]+`|\*[^*]+\*)""")
    return buildAnnotatedString {
        var last = 0
        tokenRegex.findAll(text).forEach { m ->
            if (m.range.first > last) append(text.substring(last, m.range.first))
            val tok = m.value
            when {
                tok.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(tok.removeSurrounding("**"))
                }
                tok.startsWith("`") -> withStyle(
                    SpanStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = accentColor,
                        background = accentColor.copy(alpha = 0.12f)
                    )
                ) {
                    append(" ${tok.removeSurrounding("`")} ")
                }
                tok.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(tok.removeSurrounding("*"))
                }
                else -> append(tok)
            }
            last = m.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
}

// ---------------- 9. PLAIN TEXT VIEWER ----------------

@Composable
private fun BuiltInPlainTextViewer(rawText: String) {
    val ext = LocalRitamExtendedColors.current
    var fontSizeSp by remember { mutableIntStateOf(12) }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ext.surfaceLow)
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TEXT SIZE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { fontSizeSp = (fontSizeSp - 1).coerceAtLeast(9) },
                enabled = fontSizeSp > 9,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Rounded.ZoomOut, contentDescription = "Smaller text", modifier = Modifier.size(18.dp))
            }
            Text(
                text = "$fontSizeSp sp",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { fontSizeSp = (fontSizeSp + 1).coerceAtMost(24) },
                enabled = fontSizeSp < 24,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Rounded.ZoomIn, contentDescription = "Larger text", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { fontSizeSp = 12 },
                enabled = fontSizeSp != 12,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset text size", modifier = Modifier.size(18.dp))
            }
        }
        SelectionContainer {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(vScroll)
                    .horizontalScroll(hScroll)
                    .padding(14.dp)
            ) {
                Text(
                    text = rawText.ifBlank { "(empty file)" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp + 6).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatDurationMs(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%d:%02d".format(mins, secs)
}
