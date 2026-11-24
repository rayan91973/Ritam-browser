package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.browser.ReaderArticle
import com.example.services.StructuredViewerHelper
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

@Composable
fun ReaderModeView(
    article: ReaderArticle,
    fontSizeSp: Int,
    isBookmarked: Boolean,
    onAdjustFontSize: (Int) -> Unit,
    onBookmarkArticle: () -> Unit,
    onOrganizeBookmarks: () -> Unit,
    onExitReader: () -> Unit,
    modifier: Modifier = Modifier,
    bionicReadingEnabled: Boolean = false,
    columnWidthDp: Int = 640,
    lineHeightMultiplier: Float = 1.62f,
    onToggleBionicReading: () -> Unit = {},
    onAdjustColumnWidth: (Int) -> Unit = {},
    onAdjustLineHeight: (Float) -> Unit = {},
    onSaveHighlight: () -> Unit = {}
) {
    val ext = LocalRitamExtendedColors.current
    val scrollState = rememberScrollState()
    val readProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue <= 0) 0f
            else (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle editorial toolbar + scroll progress
        LinearProgressIndicator(
            progress = { readProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = ext.surfaceHigh
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                Surface(
                    onClick = { onAdjustFontSize(-1) },
                    shape = RoundedCornerShape(8.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text("A-", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(
                    text = "${fontSizeSp}pt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    onClick = { onAdjustFontSize(1) },
                    shape = RoundedCornerShape(8.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text("A+", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Surface(
                    onClick = onToggleBionicReading,
                    shape = RoundedCornerShape(8.dp),
                    color = if (bionicReadingEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else ext.surfaceLow,
                    border = BorderStroke(1.dp, if (bionicReadingEnabled) MaterialTheme.colorScheme.primary else ext.cardBorder),
                    modifier = Modifier.height(34.dp).testTag("reader_focus_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CenterFocusStrong,
                            contentDescription = "Focus Fixation Reading",
                            tint = if (bionicReadingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Focus",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bionicReadingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                val nextWidth = when {
                    columnWidthDp <= 540 -> 640
                    columnWidthDp <= 680 -> 760
                    else -> 520
                }
                val widthLabel = when {
                    columnWidthDp <= 540 -> "Narrow"
                    columnWidthDp <= 680 -> "Medium"
                    else -> "Wide"
                }
                Surface(
                    onClick = { onAdjustColumnWidth(nextWidth) },
                    shape = RoundedCornerShape(8.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(Modifier.padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
                        Text("W: $widthLabel", style = MaterialTheme.typography.labelSmall)
                    }
                }
                val nextLine = when {
                    lineHeightMultiplier < 1.52f -> 1.62f
                    lineHeightMultiplier < 1.75f -> 1.85f
                    else -> 1.45f
                }
                Surface(
                    onClick = { onAdjustLineHeight(nextLine) },
                    shape = RoundedCornerShape(8.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(Modifier.padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
                        Text("${String.format("%.2f", lineHeightMultiplier)}x", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(
                    onClick = onSaveHighlight,
                    shape = RoundedCornerShape(8.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = "Save highlight", modifier = Modifier.size(14.dp))
                        Text("Note", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            Surface(
                onClick = onExitReader,
                shape = RoundedCornerShape(16.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("reader_exit_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Exit", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = columnWidthDp.coerceIn(480, 820).dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Category & Read Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${article.readTimeMinutes} min read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Title
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = NewsreaderFamily,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 38.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (article.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = article.subtitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NewsreaderFamily,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 25.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Author Row
                if (article.author.isNotBlank() || article.siteName.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ext.surfaceLow,
                        border = BorderStroke(1.dp, ext.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!article.authorAvatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = article.authorAvatarUrl,
                                    contentDescription = article.author,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ext.surfaceHigh)
                                )
                            }
                            Column {
                                if (article.author.isNotBlank()) {
                                    Text(
                                        text = article.author,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                Text(
                                    text = article.siteName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Hero Image
                if (!article.heroImageUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ext.surfaceContainer)
                    ) {
                        AsyncImage(
                            model = article.heroImageUrl,
                            contentDescription = article.heroCaption ?: article.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (!article.heroBadge.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = article.heroBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    if (!article.heroCaption.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = article.heroCaption,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = NewsreaderFamily,
                                fontStyle = FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Paragraphs with Drop Cap or Focus Fixation Reading
                article.paragraphs.forEachIndexed { index, para ->
                    val effectiveLineHeight = (fontSizeSp * lineHeightMultiplier.coerceIn(1.35f, 2.0f)).sp
                    if (bionicReadingEnabled) {
                        val fixation = remember(para) {
                            StructuredViewerHelper.buildFixationAnnotatedString(para)
                        }
                        Text(
                            text = fixation,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = NewsreaderFamily,
                                fontSize = fontSizeSp.sp,
                                lineHeight = effectiveLineHeight
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (index == 0 && para.length > 1) {
                        val annotated = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontFamily = NewsreaderFamily,
                                    fontSize = (fontSizeSp + 16).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append(para.first().toString())
                            }
                            append(para.drop(1))
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = NewsreaderFamily,
                                fontSize = fontSizeSp.sp,
                                lineHeight = effectiveLineHeight
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = para,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = NewsreaderFamily,
                                fontSize = fontSizeSp.sp,
                                lineHeight = effectiveLineHeight
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    // Quote after first paragraph
                    if (index == 0 && !article.quote.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ext.surfaceLow,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = article.quote,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NewsreaderFamily,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 27.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Secondary Editorial Images Grid
                if (article.secondaryImages.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        article.secondaryImages.take(2).forEach { (caption, imageUrl) ->
                            Column(modifier = Modifier.weight(1f)) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(132.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(ext.surfaceHigh)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = caption,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                // Save / Collection Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ext.surfaceLow,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isBookmarked) "Saved to Sanctuary Collection" else "Save for Calm Reading",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Stored locally in your browser library",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = if (isBookmarked) onOrganizeBookmarks else onBookmarkArticle,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (isBookmarked) "Organize" else "Save")
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
