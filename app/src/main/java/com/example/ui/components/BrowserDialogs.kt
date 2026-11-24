package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.browser.MaliciousWarningState
import com.example.browser.SslWarningState
import com.example.data.local.BookmarkFolderEntity

@Composable
fun SslErrorDialog(
    warning: SslWarningState,
    onDismissAndGoBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissAndGoBack,
        icon = {
            Icon(
                Icons.Rounded.GppBad,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Connection Not Private") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = warning.primaryError,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Ritam Browser blocked loading ${warning.url} to protect your security. Certificates that fail verification are never bypassed automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissAndGoBack,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Back to Safety")
            }
        }
    )
}

@Composable
fun MaliciousDomainDialog(
    warning: MaliciousWarningState,
    onBackToSafety: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onBackToSafety,
        icon = {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Malicious Domain Blocked") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ritam Shields intercepted navigation to ${warning.domain}.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "This host matches a known phishing or malware test signature. Navigation was stopped before any content loaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onBackToSafety,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Return Safely")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookmarkDialog(
    initialTitle: String,
    initialUrl: String,
    folders: List<BookmarkFolderEntity>,
    allowUrlEdit: Boolean,
    onSave: (title: String, url: String, folderId: Long, pinHome: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    var selectedFolderId by remember { mutableLongStateOf(folders.firstOrNull()?.id ?: 1L) }
    var pinHome by remember { mutableStateOf(allowUrlEdit) }
    var folderExpanded by remember { mutableStateOf(false) }

    val selectedFolderName = folders.firstOrNull { it.id == selectedFolderId }?.name ?: "Sanctuary"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (allowUrlEdit) "Add Bookmark" else "Save Page") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Address (URL)") },
                    enabled = allowUrlEdit,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (folders.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = folderExpanded,
                        onExpandedChange = { folderExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedFolderName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Folder") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = folderExpanded,
                            onDismissRequest = { folderExpanded = false }
                        ) {
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        selectedFolderId = folder.id
                                        folderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = pinHome, onCheckedChange = { pinHome = it })
                    Spacer(Modifier.width(4.dp))
                    Text("Pin to Browser Home grid", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onSave(title.ifBlank { url }, url, selectedFolderId, pinHome)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ClearBrowsingDataDialog(
    onConfirm: (history: Boolean, cache: Boolean, cookies: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var clearHistory by remember { mutableStateOf(true) }
    var clearCache by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear Browsing Data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Choose local data to wipe immediately from device storage:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = clearHistory, onCheckedChange = { clearHistory = it })
                    Text("Browsing & Search History")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = clearCache, onCheckedChange = { clearCache = it })
                    Text("Cached Images & Web Storage")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it })
                    Text("Site Cookies & Active Logins")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(clearHistory, clearCache, clearCookies) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clear Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SitePermissionsDialog(
    domain: String,
    sslSecure: Boolean,
    jsEnabled: Boolean,
    zoomPercent: Int = 100,
    forceDarkOverride: Int = -1,
    onSave: (js: Boolean, zoomPercent: Int, forceDarkOverride: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var js by remember { mutableStateOf(jsEnabled) }
    var zoom by remember { mutableIntStateOf(zoomPercent.coerceIn(50, 200)) }
    var darkMode by remember { mutableIntStateOf(forceDarkOverride) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (sslSecure) Icons.Rounded.Lock else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (sslSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        title = { Text(domain.ifBlank { "Site Information" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (sslSecure) "Connection is encrypted via HTTPS." else "Connection is not encrypted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sslSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("JavaScript", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = js, onCheckedChange = { js = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Site Text Zoom", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { zoom = (zoom - 15).coerceAtLeast(50) }) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease zoom")
                        }
                        Text("${zoom}%", style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { zoom = (zoom + 15).coerceAtMost(200) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Increase zoom")
                        }
                    }
                }
                Text("Webpage Dark Mode", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-1 to "Auto", 1 to "Dark", 0 to "Light").forEach { (value, label) ->
                        FilterChip(
                            selected = darkMode == value,
                            onClick = { darkMode = value },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(js, zoom, darkMode) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
