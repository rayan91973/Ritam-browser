package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val RITAM_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida/ALJJyrC2Nr-5dKmE3et5LKu0Fs09-WYVRnI3Ar4ztiP1DtfPn4s_BplnwYxH_Nft1A8ErMHgEd0Ig9i3jeKGRVXVCmTVoUrYoRd2JCl6RJQc0zSC5W9JuQQYKLrmh6SX0fPydYGcB7b7L6oPjnNCsCXkILF-zmwfY0xX2d83ccXKYxyKBTB5A6-ORmoRSQPFWuJcBumuVUPhPq02eDocl_-OeD_HWhDLGuQkdJO-vuGfZCuUkg"

@Composable
fun LotusEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(
            width = (w * 0.032f).coerceAtLeast(2.2f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Subtle botanical aura halo
        drawCircle(
            color = tint.copy(alpha = 0.08f),
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Center primary petal
        val centerPetal = Path().apply {
            moveTo(w * 0.5f, h * 0.16f)
            cubicTo(w * 0.64f, h * 0.34f, w * 0.62f, h * 0.62f, w * 0.5f, h * 0.78f)
            cubicTo(w * 0.38f, h * 0.62f, w * 0.36f, h * 0.34f, w * 0.5f, h * 0.16f)
            close()
        }
        drawPath(centerPetal, color = tint.copy(alpha = 0.14f))
        drawPath(centerPetal, color = tint, style = stroke)

        // Left inner petal
        val leftInner = Path().apply {
            moveTo(w * 0.48f, h * 0.77f)
            cubicTo(w * 0.31f, h * 0.68f, w * 0.24f, h * 0.46f, w * 0.28f, h * 0.28f)
            cubicTo(w * 0.39f, h * 0.36f, w * 0.45f, h * 0.53f, w * 0.48f, h * 0.77f)
            close()
        }
        drawPath(leftInner, color = tint.copy(alpha = 0.08f))
        drawPath(leftInner, color = tint, style = stroke)

        // Right inner petal
        val rightInner = Path().apply {
            moveTo(w * 0.52f, h * 0.77f)
            cubicTo(w * 0.69f, h * 0.68f, w * 0.76f, h * 0.46f, w * 0.72f, h * 0.28f)
            cubicTo(w * 0.61f, h * 0.36f, w * 0.55f, h * 0.53f, w * 0.52f, h * 0.77f)
            close()
        }
        drawPath(rightInner, color = tint.copy(alpha = 0.08f))
        drawPath(rightInner, color = tint, style = stroke)

        // Left outer petal
        val leftOuter = Path().apply {
            moveTo(w * 0.44f, h * 0.78f)
            cubicTo(w * 0.22f, h * 0.76f, w * 0.12f, h * 0.60f, w * 0.14f, h * 0.44f)
            cubicTo(w * 0.25f, h * 0.48f, w * 0.34f, h * 0.60f, w * 0.44f, h * 0.78f)
        }
        drawPath(leftOuter, color = tint, style = stroke)

        // Right outer petal
        val rightOuter = Path().apply {
            moveTo(w * 0.56f, h * 0.78f)
            cubicTo(w * 0.78f, h * 0.76f, w * 0.88f, h * 0.60f, w * 0.86f, h * 0.44f)
            cubicTo(w * 0.75f, h * 0.48f, w * 0.66f, h * 0.60f, w * 0.56f, h * 0.78f)
        }
        drawPath(rightOuter, color = tint, style = stroke)

        // Grounding serenity stem arc
        val baseArc = Path().apply {
            moveTo(w * 0.28f, h * 0.85f)
            quadraticTo(w * 0.5f, h * 0.90f, w * 0.72f, h * 0.85f)
        }
        drawPath(baseArc, color = tint.copy(alpha = 0.75f), style = stroke)
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val mins = diff / 60_000L
    val hours = mins / 60L
    val days = hours / 24L
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
        days == 1L -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(timestamp))
    }
}
