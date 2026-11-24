package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BloodGroups
import com.example.data.model.BloodRequest
import com.example.data.model.BloodRequestStatus
import com.example.data.model.UserProfile
import com.example.data.model.compatibleDonorGroups
import com.example.data.model.daysUntilEligible
import com.example.data.model.isEligibleBloodDonor
import com.example.notifications.BloodNotificationHelper
import com.example.ui.components.AvatarCircle
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatusPill
import com.example.ui.components.launchPhoneDialer
import com.example.ui.i18n.AppStrings
import com.example.ui.state.UiState
import com.example.ui.theme.AmberWarningContainer
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnAmberWarningContainer
import com.example.ui.theme.OnEmeraldPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodBankScreen(
    currentUser: UserProfile,
    donorsState: UiState<List<UserProfile>>,
    requestsState: UiState<List<BloodRequest>>,
    strings: AppStrings,
    bloodAlertsEnabled: Boolean,
    subscribedAlertGroups: Set<String>,
    openRequestFormInitially: Boolean = false,
    openRequestsTabInitially: Boolean = false,
    onToggleBloodAlerts: (Boolean) -> Unit,
    onCreateRequest: (
        patientName: String,
        bloodGroup: String,
        targetBloodGroups: List<String>,
        hospitalLocation: String,
        contactPhone: String,
        bagsNeeded: Int
    ) -> Unit,
    onMarkFulfilled: (String) -> Unit,
    onUpdateRequest: (
        reqId: String,
        patientName: String,
        bloodGroup: String,
        hospitalLocation: String,
        contactPhone: String,
        bagsNeeded: Int,
        status: String
    ) -> Unit,
    onDeleteRequest: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember(openRequestFormInitially, openRequestsTabInitially) {
        mutableStateOf(if (openRequestFormInitially || openRequestsTabInitially) 1 else 0)
    }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var cityFilter by remember { mutableStateOf("") }
    var showRequestSheet by remember(openRequestFormInitially) { mutableStateOf(openRequestFormInitially) }
    var editingRequest by remember { mutableStateOf<BloodRequest?>(null) }
    var hasNotifPermission by remember {
        mutableStateOf(BloodNotificationHelper.hasPostNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted || BloodNotificationHelper.hasPostNotificationPermission(context)
        if (granted) onToggleBloodAlerts(true)
    }

    val activeAlertGroups = subscribedAlertGroups.ifEmpty { setOf(currentUser.bloodGroup) }
    val donorsList = (donorsState as? UiState.Success)?.data.orEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (bloodAlertsEnabled && hasNotifPermission) {
                        EmeraldPrimaryContainer.copy(alpha = 0.55f)
                    } else {
                        AmberWarningContainer.copy(alpha = 0.65f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (bloodAlertsEnabled && hasNotifPermission) {
                            Icons.Rounded.NotificationsActive
                        } else {
                            Icons.Rounded.NotificationsOff
                        },
                        contentDescription = null,
                        tint = if (bloodAlertsEnabled && hasNotifPermission) {
                            OnEmeraldPrimaryContainer
                        } else {
                            OnAmberWarningContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.bloodAlertsTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!hasNotifPermission) {
                                strings.notificationsPermissionMissing
                            } else {
                                "${strings.subscribedGroupsLabel}: ${activeAlertGroups.joinToString(", ")}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        TextButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.testTag("grant_blood_notif_permission_button")
                        ) {
                            Text(strings.enableNotificationsButton)
                        }
                    } else if (!bloodAlertsEnabled) {
                        TextButton(onClick = { onToggleBloodAlerts(true) }) {
                            Text(strings.enableNotificationsButton)
                        }
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(strings.donorsTab)
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(strings.requestsTab)
                }
            }

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = cityFilter,
                    onValueChange = { cityFilter = it },
                    placeholder = { Text(strings.filterByCityHint) },
                    leadingIcon = { Icon(Icons.Rounded.LocationCity, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedGroup == null,
                            onClick = { selectedGroup = null },
                            label = { Text(strings.allGroups) }
                        )
                    }
                    items(BloodGroups.all) { bg ->
                        FilterChip(
                            selected = selectedGroup == bg,
                            onClick = { selectedGroup = if (selectedGroup == bg) null else bg },
                            label = { Text(bg, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                when (donorsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorBannerCard(donorsState.message) }
                    is UiState.Success -> {
                        val q = cityFilter.trim().lowercase()
                        val donors = donorsState.data.filter { d ->
                            val bgOk = selectedGroup == null || d.bloodGroup == selectedGroup
                            val cityOk = q.isEmpty() || d.city.lowercase().contains(q)
                            bgOk && cityOk
                        }.sortedByDescending { isEligibleBloodDonor(it.lastBloodDonateDate) }

                        if (donors.isEmpty()) {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(Icons.Rounded.Bloodtype, strings.noAlumniFound)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(donors, key = { it.uid }) { donor ->
                                    DonorCard(donor = donor, strings = strings)
                                }
                            }
                        }
                    }
                }
            } else {
                when (requestsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorBannerCard(requestsState.message) }
                    is UiState.Success -> {
                        if (requestsState.data.isEmpty()) {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(Icons.Rounded.Bloodtype, strings.noNoticesYet)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(requestsState.data, key = { it.id }) { req ->
                                    BloodRequestCard(
                                        request = req,
                                        canMarkFulfilled = req.status == BloodRequestStatus.OPEN &&
                                            (req.postedBy == currentUser.uid || currentUser.isSuperAdmin),
                                        canAdminEdit = currentUser.isSuperAdmin,
                                        strings = strings,
                                        onMarkFulfilled = { onMarkFulfilled(req.id) },
                                        onEdit = { editingRequest = req },
                                        onDelete = { onDeleteRequest(req.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showRequestSheet = true },
            containerColor = CrimsonBlood,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("urgent_blood_fab")
        ) {
            Icon(Icons.Rounded.AddAlert, contentDescription = strings.urgentBloodRequest)
        }
    }

    if (showRequestSheet) {
        UrgentBloodRequestSheet(
            donors = donorsList,
            strings = strings,
            onDismiss = { showRequestSheet = false },
            onSubmit = { patient, bg, targetGroups, hospital, phone, bags ->
                onCreateRequest(patient, bg, targetGroups, hospital, phone, bags)
                showRequestSheet = false
                selectedTab = 1
            }
        )
    }

    editingRequest?.let { req ->
        EditBloodRequestDialog(
            request = req,
            strings = strings,
            onDismiss = { editingRequest = null },
            onSave = { patient, bg, hospital, phone, bags, status ->
                onUpdateRequest(req.id, patient, bg, hospital, phone, bags, status)
                editingRequest = null
            }
        )
    }
}

@Composable
private fun DonorCard(donor: UserProfile, strings: AppStrings) {
    val context = LocalContext.current
    val eligible = isEligibleBloodDonor(donor.lastBloodDonateDate)
    val daysRemaining = daysUntilEligible(donor.lastBloodDonateDate)
    val canCall = !donor.hidePhone && donor.phone.isNotBlank()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            AvatarCircle(name = donor.name, photoUrl = donor.photoUrl, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = donor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${donor.city} • ${strings.batchPrefix} ${donor.sscBatch}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                StatusPill(
                    text = if (eligible) strings.availableDonor else "${strings.cooldownDonorPrefix} $daysRemaining ${strings.daysSuffix}",
                    containerColor = if (eligible) EmeraldPrimaryContainer else AmberWarningContainer,
                    contentColor = if (eligible) OnEmeraldPrimaryContainer else OnAmberWarningContainer
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                BloodGroupBadge(bloodGroup = donor.bloodGroup)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { launchPhoneDialer(context, donor.phone) },
                    enabled = canCall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (canCall) Icons.Rounded.Call else Icons.Rounded.Lock,
                        contentDescription = strings.call,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (canCall) strings.call else strings.phoneHiddenBadge, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun BloodRequestCard(
    request: BloodRequest,
    canMarkFulfilled: Boolean,
    canAdminEdit: Boolean,
    strings: AppStrings,
    onMarkFulfilled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isOpen = request.status == BloodRequestStatus.OPEN

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isOpen) CrimsonBlood.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodGroupBadge(bloodGroup = request.bloodGroup)
                    Spacer(Modifier.width(8.dp))
                    StatusPill(
                        text = if (isOpen) strings.statusOpen else strings.statusFulfilled,
                        containerColor = if (isOpen) AmberWarningContainer else EmeraldPrimaryContainer,
                        contentColor = if (isOpen) OnAmberWarningContainer else OnEmeraldPrimaryContainer
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${request.bagsNeeded} Bags",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                        fontWeight = FontWeight.Bold,
                        color = CrimsonBlood
                    )
                    if (canAdminEdit) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.deleteLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = request.patientName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalHospital, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = request.hospitalLocation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${strings.notifiedGroupsPrefix} ${request.effectiveTargetGroups.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { launchPhoneDialer(context, request.contactPhone) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonBlood),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Call, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${strings.call} (${request.contactPhone})", maxLines = 1)
                }
                if (canMarkFulfilled) {
                    OutlinedButton(
                        onClick = onMarkFulfilled,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.markFulfilled, maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun UrgentBloodRequestSheet(
    donors: List<UserProfile>,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSubmit: (
        patientName: String,
        bloodGroup: String,
        targetBloodGroups: List<String>,
        hospital: String,
        phone: String,
        bags: Int
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var patientName by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var targetGroups by remember { mutableStateOf(setOf("O+")) }
    var hospital by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bagsText by remember { mutableStateOf("1") }

    val matchingAlumniCount = donors.count { it.bloodGroup in targetGroups }
    val matchingEligibleCount = donors.count { it.bloodGroup in targetGroups && isEligibleBloodDonor(it.lastBloodDonateDate) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = strings.urgentBloodRequest,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BloodGroups.all.forEach { bg ->
                    FilterChip(
                        selected = bloodGroup == bg,
                        onClick = {
                            bloodGroup = bg
                            targetGroups = setOf(bg)
                        },
                        label = { Text(bg, fontWeight = FontWeight.Bold) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = strings.notifyTargetGroupsLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = targetGroups == setOf(bloodGroup),
                    onClick = { targetGroups = setOf(bloodGroup) },
                    label = { Text(strings.exactGroupOnly) }
                )
                FilterChip(
                    selected = targetGroups == compatibleDonorGroups(bloodGroup).toSet(),
                    onClick = { targetGroups = compatibleDonorGroups(bloodGroup).toSet() },
                    label = { Text(strings.compatibleDonorsOption) }
                )
            }
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BloodGroups.all.forEach { bg ->
                    FilterChip(
                        selected = bg in targetGroups,
                        onClick = {
                            val next = targetGroups.toMutableSet()
                            if (bg in next) {
                                if (next.size > 1) next.remove(bg)
                            } else {
                                next.add(bg)
                            }
                            targetGroups = next
                        },
                        label = { Text(bg) }
                    )
                }
            }
            Text(
                text = "${strings.notifyPreviewPrefix} $matchingAlumniCount (${matchingEligibleCount} ${strings.eligibleSuffix})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text(strings.patientNameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = hospital,
                onValueChange = { hospital = it },
                label = { Text(strings.hospitalLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phoneLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = bagsText,
                    onValueChange = { bagsText = it.filter { c -> c.isDigit() } },
                    label = { Text(strings.bagsNeededLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))
            val bags = bagsText.toIntOrNull() ?: 0
            val valid = patientName.trim().length >= 2 &&
                hospital.trim().length >= 2 &&
                phone.trim().length >= 6 &&
                bags in 1..20 &&
                targetGroups.isNotEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = { onSubmit(patientName, bloodGroup, targetGroups.toList(), hospital, phone, bags) },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonBlood),
                    modifier = Modifier.weight(1f).testTag("submit_blood_request_button")
                ) {
                    Text(strings.postRequest)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditBloodRequestDialog(
    request: BloodRequest,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int, String) -> Unit
) {
    var patient by remember(request) { mutableStateOf(request.patientName) }
    var bloodGroup by remember(request) { mutableStateOf(request.bloodGroup) }
    var hospital by remember(request) { mutableStateOf(request.hospitalLocation) }
    var phone by remember(request) { mutableStateOf(request.contactPhone) }
    var bagsText by remember(request) { mutableStateOf(request.bagsNeeded.toString()) }
    var status by remember(request) { mutableStateOf(request.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = patient, onValueChange = { patient = it }, label = { Text(strings.patientNameLabel) }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BloodGroups.all.forEach { bg ->
                        FilterChip(selected = bloodGroup == bg, onClick = { bloodGroup = bg }, label = { Text(bg) })
                    }
                }
                OutlinedTextField(value = hospital, onValueChange = { hospital = it }, label = { Text(strings.hospitalLabel) }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.phoneLabel) }, singleLine = true)
                OutlinedTextField(
                    value = bagsText,
                    onValueChange = { bagsText = it.filter(Char::isDigit) },
                    label = { Text(strings.bagsNeededLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = status == BloodRequestStatus.OPEN, onClick = { status = BloodRequestStatus.OPEN }, label = { Text(strings.statusOpen) })
                    FilterChip(selected = status == BloodRequestStatus.FULFILLED, onClick = { status = BloodRequestStatus.FULFILLED }, label = { Text(strings.statusFulfilled) })
                }
            }
        },
        confirmButton = {
            val bags = bagsText.toIntOrNull() ?: 0
            Button(
                onClick = { onSave(patient, bloodGroup, hospital, phone, bags, status) },
                enabled = patient.trim().length >= 2 && hospital.trim().length >= 2 && phone.trim().length >= 6 && bags in 1..20
            ) { Text(strings.saveChanges) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
