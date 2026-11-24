package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.AppUpdateInfo
import com.example.data.model.BloodGroups
import com.example.data.model.UserProfile
import com.example.data.model.isEligibleBloodDonor
import com.example.notifications.BloodNotificationHelper
import com.example.ui.components.AvatarCircle
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.ExitAppConfirmationDialog
import com.example.ui.components.LanguageSelectorButtons
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.encodeCompressedImageUriToDataUri
import com.example.ui.components.launchExternalUrl
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    privatePhone: String?,
    appUpdate: AppUpdateInfo?,
    bloodAlertsEnabled: Boolean,
    subscribedAlertGroups: Set<String>,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleBloodAlerts: (Boolean) -> Unit,
    onToggleAlertBloodGroup: (String) -> Unit,
    onToggleHidePhone: (Boolean) -> Unit,
    onUpdateDonationDate: (Timestamp) -> Unit,
    onSaveProfile: (
        name: String,
        phone: String,
        hidePhone: Boolean,
        sscBatch: String,
        bloodGroup: String,
        profession: String,
        city: String,
        country: String
    ) -> Unit,
    onUpdateProfilePhoto: (String) -> Unit,
    onOpenManagementPanel: () -> Unit,
    onOpenAbout: () -> Unit,
    onRequestExit: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isUpdatingPhoto by remember { mutableStateOf(false) }
    var hasNotifPermission by remember {
        mutableStateOf(BloodNotificationHelper.hasPostNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted || BloodNotificationHelper.hasPostNotificationPermission(context)
        if (granted) onToggleBloodAlerts(true)
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUpdatingPhoto = true
            scope.launch {
                val dataUri = withContext(Dispatchers.IO) {
                    encodeCompressedImageUriToDataUri(
                        context = context,
                        uri = uri,
                        maxDim = 400,
                        maxChars = 140_000
                    )
                }
                isUpdatingPhoto = false
                if (dataUri != null) {
                    onUpdateProfilePhoto(dataUri)
                } else {
                    Toast.makeText(context, strings.photoProcessingError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val resolvedPhone = privatePhone ?: profile.phone
    val activeAlertGroups = subscribedAlertGroups.ifEmpty { setOf(profile.bloodGroup) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarCircle(
                            name = profile.name,
                            photoUrl = profile.photoUrl,
                            size = 124.dp,
                            modifier = Modifier.testTag("profile_photo_avatar")
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${strings.batchPrefix} ${profile.sscBatch} • ${profile.profession}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RoleBadge(role = profile.role, strings = strings)
                                BloodGroupBadge(bloodGroup = profile.bloodGroup)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "${profile.city}, ${profile.country}  •  $resolvedPhone",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isUpdatingPhoto,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("change_profile_photo_button")
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.changeProfilePhoto, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.editProfile, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (profile.isSuperAdmin) {
            item {
                Button(
                    onClick = onOpenManagementPanel,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("open_management_panel_button")
                ) {
                    Icon(Icons.Rounded.AdminPanelSettings, null)
                    Spacer(Modifier.width(10.dp))
                    Text(strings.managementPanel, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        item {
            val hasNewUpdate = appUpdate != null && appUpdate.versionCode > BuildConfig.VERSION_CODE
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasNewUpdate) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (hasNewUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth().testTag("profile_app_update_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (hasNewUpdate) Icons.Rounded.SystemUpdate else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.availableUpdatesTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${strings.currentVersionLabel}: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (hasNewUpdate) {
                            StatusPill(
                                text = strings.updateAvailableBadge,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    if (hasNewUpdate && appUpdate != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${strings.newVersionLabel}: v${appUpdate.versionName} (${appUpdate.versionCode})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
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
                        if (appUpdate.downloadUrl.startsWith("http://") || appUpdate.downloadUrl.startsWith("https://")) {
                            Button(
                                onClick = { launchExternalUrl(context, appUpdate.downloadUrl) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("download_app_update_button")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.downloadUpdateButton)
                            }
                        }
                    } else {
                        StatusPill(
                            text = strings.upToDateStatus,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (appUpdate != null && appUpdate.releaseNotes.isNotBlank()) {
                            Text(
                                text = "v${appUpdate.versionName}: ${appUpdate.releaseNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Bloodtype, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(strings.lastDonationDate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (profile.lastBloodDonateDate != null) {
                                        "${formatTimestamp(profile.lastBloodDonateDate)} (${if (isEligibleBloodDonor(profile.lastBloodDonateDate)) strings.availableDonor else "Cooldown"})"
                                    } else {
                                        strings.notRecordedYet
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Rounded.CalendarToday, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.updateDonationDate)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.bloodAlertsTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(strings.bloodAlertsSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = bloodAlertsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleBloodAlerts(enabled)
                                }
                            },
                            modifier = Modifier.testTag("profile_blood_alerts_switch")
                        )
                    }

                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.enableNotificationsButton)
                        }
                    }

                    if (bloodAlertsEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = strings.subscribedGroupsLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BloodGroups.all.forEach { bg ->
                                    FilterChip(
                                        selected = bg in activeAlertGroups,
                                        onClick = { onToggleAlertBloodGroup(bg) },
                                        label = { Text(bg) }
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.hidePhoneLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(strings.hidePhoneDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = profile.hidePhone,
                            onCheckedChange = onToggleHidePhone,
                            modifier = Modifier.testTag("profile_hide_phone_switch")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(strings.languageSetting, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        LanguageSelectorButtons(
                            currentLanguage = currentLanguage,
                            onSelectLanguage = onSelectLanguage
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("profile_about_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.aboutTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = strings.aboutCardSubtitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (currentLanguage == AppLanguage.BENGALI) {
                        Text(
                            text = "প্রাতিষ্ঠানিক পরিচিতি (বাংলা)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.aboutOfficialTextBn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Institutional Profile (English)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.aboutEnglishSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Institutional Profile (English)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.aboutEnglishSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "প্রাতিষ্ঠানিক পরিচিতি (বাংলা)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.aboutOfficialTextBn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = "App Developer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentLanguage == AppLanguage.BENGALI) "অ্যাপ ডেভেলপার (App Developer)" else "App Developer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Md. Sahab Uddin SSC Batch of 2011",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenAbout,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_open_about_button")
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.aboutReadMore)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.signOut)
            }
        }

        item {
            OutlinedButton(
                onClick = { showExitDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_exit_app_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ExitToApp, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.exitApp)
            }
        }
    }

    if (showExitDialog) {
        ExitAppConfirmationDialog(
            strings = strings,
            onConfirmExit = {
                showExitDialog = false
                onRequestExit()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = profile.lastBloodDonateDate?.toDate()?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val safeMillis = minOf(millis, System.currentTimeMillis() - 1000L)
                        onUpdateDonationDate(Timestamp(Date(safeMillis)))
                    }
                    showDatePicker = false
                }) {
                    Text(strings.saveChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            initialPhone = resolvedPhone,
            strings = strings,
            onDismiss = { showEditDialog = false },
            onSave = { n, p, h, b, bg, prof, c, country ->
                onSaveProfile(n, p, h, b, bg, prof, c, country)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    initialPhone: String,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var phone by remember { mutableStateOf(initialPhone) }
    var sscBatch by remember { mutableStateOf(profile.sscBatch) }
    var bloodGroup by remember { mutableStateOf(profile.bloodGroup) }
    var profession by remember { mutableStateOf(profile.profession) }
    var city by remember { mutableStateOf(profile.city) }
    var country by remember { mutableStateOf(profile.country) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editProfile) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.fullNameLabel) }, singleLine = true)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phoneLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sscBatch,
                    onValueChange = { if (it.length <= 4) sscBatch = it.filter(Char::isDigit) },
                    label = { Text(strings.sscBatchLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BloodGroups.all.forEach { bg ->
                        FilterChip(
                            selected = bloodGroup == bg,
                            onClick = { bloodGroup = bg },
                            label = { Text(bg) }
                        )
                    }
                }
                OutlinedTextField(value = profession, onValueChange = { profession = it }, label = { Text(strings.professionLabel) }, singleLine = true)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text(strings.cityLabel) }, singleLine = true)
                OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text(strings.countryLabel) }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, profile.hidePhone, sscBatch, bloodGroup, profession, city, country) },
                enabled = name.trim().length >= 2 && phone.trim().length >= 6 && sscBatch.matches(Regex("^[0-9]{4}$"))
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
