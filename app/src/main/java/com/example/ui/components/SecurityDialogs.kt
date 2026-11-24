package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.browser.TabState
import com.example.browser.WebPermissionPromptRequest
import com.example.data.preferences.BrowserPreferences
import com.example.privacy.HomographWarningState
import com.example.ui.theme.LocalRitamExtendedColors

@Composable
fun HomographWarningDialog(
    warning: HomographWarningState,
    onGoBack: () -> Unit,
    onProceedAnyway: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onGoBack,
        icon = {
            Icon(
                Icons.Rounded.GppMaybe,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Lookalike / Phishing Domain Warning",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = warning.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Displayed host: ${warning.displayHost}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "True ASCII / Punycode: ${warning.asciiHost}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGoBack,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("homograph_go_back_button")
            ) {
                Text("Back to Safety")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onProceedAnyway,
                modifier = Modifier.testTag("homograph_proceed_button")
            ) {
                Text("Open Anyway", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun WebPermissionPromptDialog(
    request: WebPermissionPromptRequest,
    onAllowOnceThisTab: () -> Unit,
    onAlwaysAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    val resourcesJoined = request.resourceLabels.joinToString(" & ")
    AlertDialog(
        onDismissRequest = onDeny,
        icon = {
            Icon(
                Icons.Rounded.PrivacyTip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = "Site Permission Request",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${request.domain} wants access to: $resourcesJoined",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.surfaceContainer,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"Allow This Tab Only\" automatically revokes sensor access as soon as you switch domains or close this tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAllowOnceThisTab,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("web_perm_allow_once")
                    ) {
                        Text("Allow This Tab Only (Auto-Revoke)")
                    }
                    OutlinedButton(
                        onClick = onAlwaysAllow,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("web_perm_always_allow")
                    ) {
                        Text("Always Allow on ${request.domain}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDeny,
                modifier = Modifier.testTag("web_perm_deny")
            ) {
                Text("Block", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackerXRayDialog(
    tab: TabState,
    preferences: BrowserPreferences,
    shieldsEnabledForSite: Boolean,
    isEphemeralSite: Boolean,
    onBlockHostRule: (String) -> Unit,
    onAllowHostRule: (String) -> Unit,
    onOpenShieldSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    val activeShieldsCount = listOf(
        preferences.strictReferrerPolicyEnabled,
        preferences.blockWebRtcLeaksEnabled,
        preferences.lockdownCovertApisEnabled,
        preferences.fingerprintRandomizerEnabled,
        preferences.homographGuardEnabled,
        preferences.unwrapRedirectLinksEnabled
    ).count { it }
    val grade = tab.privacyReport.computeGrade(
        sslSecure = tab.sslSecure,
        shieldsEnabled = shieldsEnabledForSite && preferences.adBlockingEnabled,
        strictLockdownsActive = activeShieldsCount
    )
    val gradeColor = when {
        grade.startsWith("A") -> MaterialTheme.colorScheme.primary
        grade == "B" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 640.dp)
                .testTag("tracker_xray_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = gradeColor.copy(alpha = 0.16f),
                            border = BorderStroke(2.dp, gradeColor),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = grade,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = gradeColor
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Privacy Grade & Tracker X-Ray",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = tab.domain.ifBlank { "Current Sanctuary Session" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProtectionPill("Referrer Cloak", preferences.strictReferrerPolicyEnabled)
                        ProtectionPill("WebRTC IP Guard", preferences.blockWebRtcLeaksEnabled)
                        ProtectionPill("API Lockdown", preferences.lockdownCovertApisEnabled)
                        ProtectionPill("Fingerprint Noise", preferences.fingerprintRandomizerEnabled)
                        ProtectionPill("Homograph Guard", preferences.homographGuardEnabled)
                        ProtectionPill("Ephemeral", isEphemeralSite || tab.isPrivate)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        XRayMetricBox("Trackers/Ads", tab.totalBlocked.toString(), Modifier.weight(1f))
                        XRayMetricBox("API Probes", tab.privacyReport.totalApiBlocks.toString(), Modifier.weight(1f))
                        XRayMetricBox("Params Clean", tab.privacyReport.strippedParamsCount.toString(), Modifier.weight(1f))
                        XRayMetricBox("Links Unwrapped", tab.privacyReport.unwrappedRedirects.toString(), Modifier.weight(1f))
                    }

                    if (tab.privacyReport.apiProbesBlocked.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ext.surfaceContainer,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Neutralized Covert APIs",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                tab.privacyReport.apiProbesBlocked.forEach { (api, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(api, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${count}x blocked",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ext.surfaceContainer,
                        border = BorderStroke(1.dp, ext.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Blocked Third-Party Hosts (${tab.privacyReport.blockedHostCounts.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            val hosts = tab.privacyReport.blockedHostCounts.entries
                                .sortedByDescending { it.value }
                            if (hosts.isEmpty()) {
                                Text(
                                    text = "No third-party trackers or ad hosts detected on this page yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                hosts.take(15).forEach { (host, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = host,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$count blocked request(s)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(onClick = { onAllowHostRule(host) }) {
                                                Text("Allow")
                                            }
                                            TextButton(onClick = { onBlockHostRule(host) }) {
                                                Text("Strict")
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenShieldSettings()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Configure Global Privacy & Shield Rules")
                }
            }
        }
    }
}

@Composable
private fun ProtectionPill(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (active) Icons.Rounded.VerifiedUser else Icons.Rounded.Block,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun XRayMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    val ext = LocalRitamExtendedColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ext.surfaceContainer,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EncryptedBackupPassphraseDialog(
    isExport: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }

    val valid = if (isExport) {
        passphrase.length >= 6 && passphrase == confirmPassphrase
    } else {
        passphrase.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.EnhancedEncryption,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (isExport) "Encrypt Vault & Backup (.ritamvault)" else "Decrypt Encrypted Backup",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isExport) {
                        "Your bookmarks, filter rules, userscripts, highlights, and decrypted vault passwords will be sealed with PBKDF2-SHA256 + AES-256-GCM."
                    } else {
                        "Enter the passphrase used when creating this .ritamvault archive."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(if (isExport) "Passphrase (min 6 chars)" else "Backup Passphrase") },
                    singleLine = true,
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { reveal = !reveal }) {
                            Icon(
                                if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle passphrase visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_input")
                )
                if (isExport) {
                    OutlinedTextField(
                        value = confirmPassphrase,
                        onValueChange = { confirmPassphrase = it },
                        label = { Text("Confirm Passphrase") },
                        singleLine = true,
                        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = confirmPassphrase.isNotEmpty() && confirmPassphrase != passphrase,
                        modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_confirm")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(passphrase) },
                enabled = valid,
                modifier = Modifier.testTag("backup_passphrase_confirm_button")
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isExport) "Encrypt & Save" else "Decrypt & Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
