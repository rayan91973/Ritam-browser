package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.OmniboxTopology
import com.example.ui.theme.LocalRitamCornerRadius
import com.example.ui.theme.LocalRitamExtendedColors

@Composable
fun BottomNavDock(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isHome: Boolean,
    isPrivate: Boolean = false,
    gestureSwipeNavEnabled: Boolean = true,
    tabCount: Int,
    topology: OmniboxTopology,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenQuickControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalRitamExtendedColors.current
    val corner = LocalRitamCornerRadius.current
    val isFloating = topology == OmniboxTopology.FLOATING_PILL

    val dockContainerColor = if (isPrivate) IncognitoColors.Surface.copy(alpha = 0.96f) else ext.surfaceLow.copy(alpha = 0.96f)
    val dockBorderColor = if (isPrivate) IncognitoColors.Border else ext.cardBorder
    val activeIconTint = if (isPrivate) IncognitoColors.OnSurface else MaterialTheme.colorScheme.onSurface
    val disabledIconTint = if (isPrivate) IncognitoColors.OnSurfaceMuted.copy(alpha = 0.52f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
    val homeHighlightColor = if (isPrivate) IncognitoColors.Accent else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = if (isFloating) 12.dp else 0.dp,
                vertical = if (isFloating) 4.dp else 0.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .pointerInput(gestureSwipeNavEnabled, canGoBack, canGoForward) {
                    if (!gestureSwipeNavEnabled) return@pointerInput
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onHorizontalDrag = { _, delta -> dragX += delta },
                        onDragEnd = {
                            if (dragX > 90f && canGoBack) {
                                onBack()
                            } else if (dragX < -90f && canGoForward) {
                                onForward()
                            }
                        }
                    )
                },
            shape = if (isFloating) RoundedCornerShape((corner + 8.dp).coerceAtLeast(20.dp))
            else RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            color = dockContainerColor,
            tonalElevation = 4.dp,
            shadowElevation = if (isFloating) 6.dp else 3.dp,
            border = BorderStroke(1.dp, dockBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = if (canGoBack) activeIconTint else disabledIconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Navigate forward",
                        tint = if (canGoForward) activeIconTint else disabledIconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (isHome) homeHighlightColor.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        )
                        .clickable(onClick = onHome)
                        .testTag("nav_home_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPrivate) {
                        IncognitoSpectsEmblem(
                            size = 19.dp,
                            tint = if (isHome) homeHighlightColor else activeIconTint,
                            hatFill = IncognitoColors.SurfaceElevated
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Browser Home",
                            tint = if (isHome) homeHighlightColor else activeIconTint,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenTabs)
                        .testTag("nav_tabs_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = 1.5.dp,
                                color = activeIconTint,
                                shape = RoundedCornerShape(5.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tabCount > 99) "∞" else tabCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = activeIconTint
                        )
                    }
                }

                IconButton(
                    onClick = onOpenQuickControls,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("nav_quick_controls_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Quick Controls & Tools",
                        tint = activeIconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
