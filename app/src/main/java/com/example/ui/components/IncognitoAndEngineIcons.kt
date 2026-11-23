package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.SearchEngine
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

object IncognitoColors {
    val Backdrop = Color(0xFF111318)
    val Surface = Color(0xFF1B1E26)
    val SurfaceElevated = Color(0xFF262A35)
    val Border = Color(0xFF393F4F)
    val OnSurface = Color(0xFFEDEFF5)
    val OnSurfaceMuted = Color(0xFFA0A7BA)
    val Accent = Color(0xFFC5CEF2)
    val BadgeBg = Color(0xFF2B3040)
}

@Composable
fun IncognitoSpectsEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    tint: Color = IncognitoColors.OnSurface,
    hatFill: Color = IncognitoColors.SurfaceElevated
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = (w * 0.055f).coerceAtLeast(2.2f)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Subtle aura behind spectacles
        drawCircle(
            color = tint.copy(alpha = 0.07f),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        // Fedora hat crown
        val crownPath = Path().apply {
            moveTo(w * 0.27f, h * 0.44f)
            lineTo(w * 0.35f, h * 0.21f)
            quadraticTo(w * 0.50f, h * 0.26f, w * 0.65f, h * 0.21f)
            lineTo(w * 0.73f, h * 0.44f)
            close()
        }
        drawPath(crownPath, color = hatFill)
        drawPath(crownPath, color = tint, style = stroke)

        // Fedora hat brim
        drawLine(
            color = tint,
            start = Offset(w * 0.14f, h * 0.45f),
            end = Offset(w * 0.86f, h * 0.45f),
            strokeWidth = strokeWidth * 1.15f,
            cap = StrokeCap.Round
        )

        // Left spectacle lens
        val lensRadius = w * 0.145f
        val leftCenter = Offset(w * 0.32f, h * 0.67f)
        val rightCenter = Offset(w * 0.68f, h * 0.67f)

        drawCircle(
            color = tint.copy(alpha = 0.14f),
            radius = lensRadius,
            center = leftCenter
        )
        drawCircle(
            color = tint,
            radius = lensRadius,
            center = leftCenter,
            style = stroke
        )

        // Right spectacle lens
        drawCircle(
            color = tint.copy(alpha = 0.14f),
            radius = lensRadius,
            center = rightCenter
        )
        drawCircle(
            color = tint,
            radius = lensRadius,
            center = rightCenter,
            style = stroke
        )

        // Nose bridge connecting spectacles
        val bridgePath = Path().apply {
            moveTo(leftCenter.x + lensRadius * 0.92f, leftCenter.y - lensRadius * 0.12f)
            quadraticTo(w * 0.50f, h * 0.60f, rightCenter.x - lensRadius * 0.92f, rightCenter.y - lensRadius * 0.12f)
        }
        drawPath(bridgePath, color = tint, style = stroke)
    }
}

@Composable
fun SearchEngineLogo(
    engine: SearchEngine,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    when (engine) {
        SearchEngine.GOOGLE -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.72f)) {
                    val stroke = Stroke(width = this.size.width * 0.22f, cap = StrokeCap.Butt)
                    val pad = this.size.width * 0.14f
                    val arcSize = Size(this.size.width - pad * 2, this.size.height - pad * 2)
                    val topLeft = Offset(pad, pad)
                    drawArc(Color(0xFFEA4335), startAngle = -145f, sweepAngle = 100f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                    drawArc(Color(0xFFFBBC05), startAngle = 135f, sweepAngle = 80f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                    drawArc(Color(0xFF34A853), startAngle = 40f, sweepAngle = 95f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                    drawArc(Color(0xFF4285F4), startAngle = -10f, sweepAngle = 55f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                    drawLine(
                        color = Color(0xFF4285F4),
                        start = Offset(this.size.width * 0.50f, this.size.height * 0.50f),
                        end = Offset(this.size.width * 0.88f, this.size.height * 0.50f),
                        strokeWidth = this.size.width * 0.20f
                    )
                }
            }
        }

        SearchEngine.DUCKDUCKGO -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFFDE5833)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.72f)) {
                    val w = this.size.width
                    val h = this.size.height
                    // Clean white duck head + orange bill silhouette
                    drawCircle(color = Color.White, radius = w * 0.30f, center = Offset(w * 0.45f, h * 0.48f))
                    // Neck
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.30f, h * 0.50f),
                        size = Size(w * 0.28f, h * 0.38f)
                    )
                    // Duck bill
                    val bill = Path().apply {
                        moveTo(w * 0.58f, h * 0.43f)
                        quadraticTo(w * 0.90f, h * 0.46f, w * 0.86f, h * 0.58f)
                        quadraticTo(w * 0.72f, h * 0.62f, w * 0.56f, h * 0.56f)
                        close()
                    }
                    drawPath(bill, color = Color(0xFFFFCC4D))
                    // Eye
                    drawCircle(color = Color(0xFF222222), radius = w * 0.065f, center = Offset(w * 0.50f, h * 0.42f))
                }
            }
        }

        SearchEngine.BRAVE -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFFFB542B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "B",
                    color = Color.White,
                    fontSize = (size.value * 0.54f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        SearchEngine.STARTPAGE -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF6573FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontSize = (size.value * 0.52f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SearchEngine.WIKIPEDIA -> {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF3F3F1),
                border = BorderStroke(1.dp, Color(0xFF9EA29D)),
                modifier = modifier.size(size)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "W",
                        fontFamily = NewsreaderFamily,
                        color = Color(0xFF1D1E1C),
                        fontSize = (size.value * 0.52f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SearchEngineSelectorPillButton(
    selectedEngine: SearchEngine,
    onSelectEngine: (SearchEngine) -> Unit,
    isPrivate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ext = LocalRitamExtendedColors.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isPrivate) IncognitoColors.SurfaceElevated
                    else ext.surfaceHigh.copy(alpha = 0.75f)
                )
                .clickable { expanded = true }
                .padding(horizontal = 6.dp, vertical = 5.dp)
                .testTag("search_engine_selector_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SearchEngineLogo(engine = selectedEngine, size = 24.dp)
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Change search engine (${selectedEngine.displayName})",
                tint = if (isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = if (isPrivate) IncognitoColors.Surface else ext.surfaceLow
        ) {
            Text(
                text = "SEARCH ENGINE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                color = if (isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
            SearchEngine.entries.forEach { engine ->
                val isSelected = engine == selectedEngine
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SearchEngineLogo(engine = engine, size = 22.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = engine.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = engine.host,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPrivate) IncognitoColors.OnSurfaceMuted else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = if (isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectEngine(engine)
                    },
                    modifier = Modifier.testTag("engine_option_${engine.name.lowercase()}")
                )
            }
        }
    }
}
