package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.ExternalAppRequest
import com.example.browser.WebContextMenuRequest
import com.example.services.QrCodeGenerator
import com.example.services.TranslationHelper
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily
import kotlinx.coroutines.launch

@Composable
fun QrCodeShareDialog(
    cleanUrl: String,
    removedTrackerCount: Int,
    onCopyCleanUrl: () -> Unit,
    onShareSystem: () -> Unit,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(cleanUrl) {
        QrCodeGenerator.generateBitmap(cleanUrl, pixelSize = 560)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Share via QR Code") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(4.dp)
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR code for $cleanUrl",
                            modifier = Modifier
                                .size(210.dp)
                                .padding(10.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("URL too long for compact QR", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(
                    text = cleanUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (removedTrackerCount > 0) {
                    Text(
                        text = "Stripped $removedTrackerCount tracking parameter(s) automatically.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCopyCleanUrl(); onDismiss() }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy Link")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onShareSystem(); onDismiss() }) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    )
}

@Composable
fun LinkContextMenuDialog(
    request: WebContextMenuRequest,
    cleanUrl: String,
    onOpenNewTab: (String) -> Unit,
    onOpenPrivateTab: (String) -> Unit,
    onAddReadLater: (String) -> Unit,
    onCopyClean: (String) -> Unit,
    onShareQr: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val target = request.linkUrl ?: request.imageUrl ?: ""
    if (target.isBlank()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (request.linkUrl != null) "Link Actions" else "Media Actions",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = cleanUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                ContextActionRow("Open in New Tab", Icons.Rounded.Tab) {
                    onOpenNewTab(target); onDismiss()
                }
                ContextActionRow("Open in Private Tab", Icons.Rounded.VisibilityOff) {
                    onOpenPrivateTab(target); onDismiss()
                }
                ContextActionRow("Save to Reading List", Icons.Rounded.BookmarkAdd) {
                    onAddReadLater(cleanUrl); onDismiss()
                }
                ContextActionRow("Copy Clean Link", Icons.Rounded.ContentCopy) {
                    onCopyClean(cleanUrl); onDismiss()
                }
                ContextActionRow("Show QR Code", Icons.Rounded.QrCode2) {
                    onShareQr(cleanUrl); onDismiss()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ContextActionRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = LocalRitamExtendedColors.current.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ExternalAppConfirmDialog(
    request: ExternalAppRequest,
    onAllowApp: () -> Unit,
    onOpenFallbackInBrowser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Open External App?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This page is requesting to leave Ritam Browser and open ${request.appLabel}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = request.targetUri,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(onClick = onAllowApp) {
                Text("Open App")
            }
        },
        dismissButton = {
            Row {
                if (!request.fallbackWebUrl.isNullOrBlank()) {
                    TextButton(onClick = { onOpenFallbackInBrowser(request.fallbackWebUrl) }) {
                        Text("Load Web Page")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Stay Here") }
            }
        }
    )
}

@Composable
fun TranslateSheetDialog(
    currentUrl: String,
    isHome: Boolean,
    isTranslated: Boolean,
    selectedLangCode: String,
    initialExcerpt: String,
    onSelectLang: (String) -> Unit,
    onTranslateFullPage: (String) -> Unit,
    onRestoreOriginal: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var snippetInput by remember(initialExcerpt) { mutableStateOf(initialExcerpt) }
    var translatedOutput by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Page & Text Translator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Target language", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TranslationHelper.supportedLanguages.forEach { lang ->
                        FilterChip(
                            selected = selectedLangCode.equals(lang.code, ignoreCase = true),
                            onClick = { onSelectLang(lang.code) },
                            label = { Text(lang.label) }
                        )
                    }
                }

                if (!isHome && currentUrl.startsWith("http")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onTranslateFullPage(selectedLangCode)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Translate Page")
                        }
                        if (isTranslated) {
                            OutlinedButton(
                                onClick = {
                                    onRestoreOriginal()
                                    onDismiss()
                                }
                            ) {
                                Text("Original")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = snippetInput,
                    onValueChange = { snippetInput = it },
                    label = { Text("Quick text snippet") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        enabled = !isTranslating && snippetInput.isNotBlank(),
                        onClick = {
                            isTranslating = true
                            errorMsg = null
                            scope.launch {
                                val res = TranslationHelper.translateText(snippetInput, selectedLangCode)
                                isTranslating = false
                                res.onSuccess { translatedOutput = it }
                                    .onFailure { errorMsg = it.localizedMessage ?: "Translation failed" }
                            }
                        }
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Translate Snippet")
                    }
                }

                if (translatedOutput.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LocalRitamExtendedColors.current.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = translatedOutput,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun BurnSessionOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(240))
    ) {
        val progress by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(durationMillis = 650, easing = LinearEasing),
            label = "burnProgress"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A0E0C), Color(0xFF0E1211))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x33FF6B35),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF7A45),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Text(
                    text = "Burning Session",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NewsreaderFamily,
                        color = Color.White
                    )
                )
                Text(
                    text = "Wiping tabs, cookies, web cache & session footprint…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBCC4BF),
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = { progress },
                    color = Color(0xFFFF7A45),
                    trackColor = Color(0x33FFFFFF),
                    modifier = Modifier
                        .width(200.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun BiometricLockScreenOverlay(
    onUnlock: () -> Unit,
    onExitPrivateMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IncognitoColors.Backdrop)
            .padding(24.dp)
            .testTag("incognito_biometric_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = IncognitoColors.Surface,
            border = BorderStroke(1.dp, IncognitoColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = IncognitoColors.BadgeBg,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Fingerprint,
                            contentDescription = "Biometric Incognito Lock",
                            tint = IncognitoColors.Accent,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Text(
                    text = "Incognito Tabs Locked",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = NewsreaderFamily),
                    color = IncognitoColors.OnSurface
                )
                Text(
                    text = "Verify your identity to view private browsing tabs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = IncognitoColors.OnSurfaceMuted,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onUnlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncognitoColors.Accent,
                        contentColor = IncognitoColors.Backdrop
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("unlock_incognito_button")
                ) {
                    Text("Unlock Incognito")
                }
                TextButton(onClick = onExitPrivateMode) {
                    Text("Switch to Regular Tab", color = IncognitoColors.OnSurfaceMuted)
                }
            }
        }
    }
}
