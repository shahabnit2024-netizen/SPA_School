package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.AppUpdateInfo
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun AdminAppUpdateSection(
    appUpdate: AppUpdateInfo?,
    strings: AppStrings,
    onPushAppUpdate: (
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String,
        mandatory: Boolean
    ) -> Unit,
    onClearAppUpdate: () -> Unit
) {
    var versionCodeText by remember(appUpdate) {
        mutableStateOf((appUpdate?.versionCode ?: (BuildConfig.VERSION_CODE + 1L)).toString())
    }
    var versionName by remember(appUpdate) {
        mutableStateOf(appUpdate?.versionName?.ifBlank { "1.1.0" } ?: "1.1.0")
    }
    var releaseNotes by remember(appUpdate) {
        mutableStateOf(appUpdate?.releaseNotes.orEmpty())
    }
    var downloadUrl by remember(appUpdate) {
        mutableStateOf(appUpdate?.downloadUrl.orEmpty())
    }
    var mandatory by remember(appUpdate) {
        mutableStateOf(appUpdate?.mandatory ?: false)
    }

    val parsedCode = versionCodeText.toIntOrNull()
    val trimmedUrl = downloadUrl.trim()
    val urlValid = trimmedUrl.isEmpty() ||
        (trimmedUrl.length <= 500 && (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")))
    val canPush = parsedCode != null &&
        parsedCode in 1..1_000_000 &&
        versionName.trim().length in 1..40 &&
        releaseNotes.trim().length in 2..2000 &&
        urlValid

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.availableUpdatesTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${strings.currentVersionLabel}: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (appUpdate != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(
                                text = "v${appUpdate.versionName} (${appUpdate.versionCode})",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (appUpdate.mandatory) {
                                StatusPill(
                                    text = strings.mandatoryBadge,
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Text(
                            text = appUpdate.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (appUpdate.updatedAt != null || appUpdate.pushedByName.isNotBlank()) {
                            Text(
                                text = listOfNotNull(
                                    appUpdate.pushedByName.takeIf { it.isNotBlank() }?.let { "${strings.pushedByLabel}: $it" },
                                    appUpdate.updatedAt?.let { formatTimestamp(it) }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onClearAppUpdate,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("clear_app_update_button")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.clearAppUpdate)
                        }
                    } else {
                        Text(
                            text = strings.noUpdatesAvailable,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = versionCodeText,
                    onValueChange = { versionCodeText = it.filter(Char::isDigit).take(7) },
                    label = { Text(strings.versionCodeLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("update_version_code_input")
                )
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it.take(40) },
                    label = { Text(strings.versionNameLabel) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("update_version_name_input")
                )
            }
        }

        item {
            OutlinedTextField(
                value = releaseNotes,
                onValueChange = { releaseNotes = it.take(2000) },
                label = { Text(strings.releaseNotesLabel) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("update_release_notes_input")
            )
        }

        item {
            OutlinedTextField(
                value = downloadUrl,
                onValueChange = { downloadUrl = it.take(500) },
                label = { Text(strings.downloadUrlLabel) },
                isError = !urlValid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("update_download_url_input")
            )
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.mandatoryUpdateLabel, style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = mandatory,
                    onCheckedChange = { mandatory = it },
                    modifier = Modifier.testTag("update_mandatory_switch")
                )
            }
        }

        item {
            Button(
                onClick = {
                    val code = parsedCode ?: return@Button
                    onPushAppUpdate(code, versionName.trim(), releaseNotes.trim(), trimmedUrl, mandatory)
                },
                enabled = canPush,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("push_app_update_button")
            ) {
                Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.pushAppUpdate)
            }
        }
    }
}
