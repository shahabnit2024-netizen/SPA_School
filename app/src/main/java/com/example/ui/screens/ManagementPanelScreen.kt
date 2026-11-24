package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppBranding
import com.example.data.model.AppUpdateInfo
import com.example.data.model.BatchGalleryPhoto
import com.example.data.model.FundCampaign
import com.example.data.model.NoticeCategories
import com.example.data.model.Teacher
import com.example.data.model.TeacherStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.UserRoles
import com.example.ui.components.DecodedOrRemoteImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FullScreenImageLightboxDialog
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.state.UiState
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementPanelScreen(
    branding: AppBranding?,
    appUpdate: AppUpdateInfo?,
    approvedAlumniState: UiState<List<UserProfile>>,
    fundsState: UiState<List<FundCampaign>>,
    teachersState: UiState<List<Teacher>>,
    pendingGalleryState: UiState<List<BatchGalleryPhoto>>,
    strings: AppStrings,
    onSaveBranding: (String, String, String, String, String?) -> Unit,
    onPushAppUpdate: (versionCode: Int, versionName: String, releaseNotes: String, downloadUrl: String, mandatory: Boolean) -> Unit,
    onClearAppUpdate: () -> Unit,
    onCreateNotice: (title: String, description: String, category: String, isPinned: Boolean) -> Unit,
    onCreateEvent: (title: String, date: String, location: String, fee: Double, description: String) -> Unit,
    onCreateCampaign: (title: String, targetAmount: Double) -> Unit,
    onAddTransaction: (campaignId: String, type: String, amount: Double, description: String, date: String) -> Unit,
    onCreateTeacher: (name: String, subject: String, tenure: String, phone: String, status: String) -> Unit,
    onUpdateTeacher: (teacherId: String, name: String, subject: String, tenure: String, phone: String, status: String) -> Unit,
    onDeleteTeacher: (String) -> Unit,
    onSetModerator: (UserProfile, Boolean) -> Unit,
    onApproveGalleryPhoto: (String) -> Unit,
    onRejectGalleryPhoto: (String) -> Unit,
    onBack: () -> Unit
) {
    val pendingGalleryCount = (pendingGalleryState as? UiState.Success)?.data?.size ?: 0
    val galleryTabTitle = if (pendingGalleryCount > 0) {
        "${strings.galleryApprovalsTab} ($pendingGalleryCount)"
    } else {
        strings.galleryApprovalsTab
    }
    val tabs = listOf(
        galleryTabTitle,
        strings.brandingTab,
        strings.appUpdateTab,
        strings.postNoticeTab,
        strings.eventsFundsTab,
        strings.teachersAdminTab,
        strings.moderatorsAdminTab
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.managementPanel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminGalleryApprovalsSection(
                    pendingGalleryState = pendingGalleryState,
                    strings = strings,
                    onApprove = onApproveGalleryPhoto,
                    onReject = onRejectGalleryPhoto
                )
                1 -> AdminBrandingSection(
                    branding = branding,
                    strings = strings,
                    onSaveBranding = onSaveBranding
                )
                2 -> AdminAppUpdateSection(
                    appUpdate = appUpdate,
                    strings = strings,
                    onPushAppUpdate = onPushAppUpdate,
                    onClearAppUpdate = onClearAppUpdate
                )
                3 -> AdminNoticeSection(strings = strings, onCreateNotice = onCreateNotice)
                4 -> AdminEventsFundsSection(
                    fundsState = fundsState,
                    strings = strings,
                    onCreateEvent = onCreateEvent,
                    onCreateCampaign = onCreateCampaign,
                    onAddTransaction = onAddTransaction
                )
                5 -> AdminTeachersSection(
                    teachersState = teachersState,
                    strings = strings,
                    onCreateTeacher = onCreateTeacher,
                    onUpdateTeacher = onUpdateTeacher,
                    onDeleteTeacher = onDeleteTeacher
                )
                6 -> AdminModeratorsSection(
                    approvedAlumniState = approvedAlumniState,
                    strings = strings,
                    onSetModerator = onSetModerator
                )
            }
        }
    }
}

@Composable
private fun AdminGalleryApprovalsSection(
    pendingGalleryState: UiState<List<BatchGalleryPhoto>>,
    strings: AppStrings,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var previewPhoto by remember { mutableStateOf<BatchGalleryPhoto?>(null) }

    when (pendingGalleryState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorBannerCard(pendingGalleryState.message) }
        is UiState.Success -> {
            val pending = pendingGalleryState.data
            if (pending.isEmpty()) {
                Box(Modifier.padding(16.dp)) {
                    EmptyStateCard(Icons.Rounded.VerifiedUser, strings.noPendingGalleryPhotos)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pending, key = { it.id }) { photo ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().testTag("admin_pending_gallery_${photo.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                DecodedOrRemoteImage(
                                    imageDataOrUrl = photo.imageDataOrUrl,
                                    contentDescription = photo.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable(onClickLabel = photo.caption) { previewPhoto = photo }
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(
                                        text = "${strings.batchPrefix} ${photo.sscBatch}",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (photo.uploadedByName.isNotBlank()) {
                                        Text(
                                            text = photo.uploadedByName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = photo.caption,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { onReject(photo.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("reject_gallery_${photo.id}")
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.reject)
                                    }
                                    Button(
                                        onClick = { onApprove(photo.id) },
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("approve_gallery_${photo.id}")
                                    ) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.approve)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewPhoto?.let { photo ->
        FullScreenImageLightboxDialog(
            title = "${strings.batchPrefix} ${photo.sscBatch}",
            subtitle = photo.caption,
            imageDataOrUrl = photo.imageDataOrUrl,
            onDismiss = { previewPhoto = null }
        )
    }
}

@Composable
private fun AdminNoticeSection(
    strings: AppStrings,
    onCreateNotice: (String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(NoticeCategories.NOTICE) }
    var isPinned by remember { mutableStateOf(true) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notice Title") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == NoticeCategories.NOTICE, onClick = { category = NoticeCategories.NOTICE }, label = { Text(strings.categoryNotice) })
                FilterChip(selected = category == NoticeCategories.JOB, onClick = { category = NoticeCategories.JOB }, label = { Text(strings.categoryJob) })
                FilterChip(selected = category == NoticeCategories.NEWS, onClick = { category = NoticeCategories.NEWS }, label = { Text(strings.categoryNews) })
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(strings.pinNoticeToggle, style = MaterialTheme.typography.titleSmall)
                Switch(checked = isPinned, onCheckedChange = { isPinned = it })
            }
        }
        item {
            Button(
                onClick = {
                    onCreateNotice(title, description, category, isPinned)
                    title = ""
                    description = ""
                },
                enabled = title.trim().length >= 2 && description.trim().length >= 2,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(strings.publishNotice)
            }
        }
    }
}

@Composable
private fun AdminEventsFundsSection(
    fundsState: UiState<List<FundCampaign>>,
    strings: AppStrings,
    onCreateEvent: (String, String, String, Double, String) -> Unit,
    onCreateCampaign: (String, Double) -> Unit,
    onAddTransaction: (String, String, Double, String, String) -> Unit
) {
    var evTitle by remember { mutableStateOf("") }
    var evDate by remember { mutableStateOf("") }
    var evLocation by remember { mutableStateOf("") }
    var evFee by remember { mutableStateOf("0") }
    var evDesc by remember { mutableStateOf("") }

    var fundTitle by remember { mutableStateOf("") }
    var fundTarget by remember { mutableStateOf("") }

    val campaigns = (fundsState as? UiState.Success)?.data.orEmpty()
    var selectedCampaignId by remember(campaigns) { mutableStateOf(campaigns.firstOrNull()?.id ?: "") }
    var txType by remember { mutableStateOf(TransactionType.IN) }
    var txAmount by remember { mutableStateOf("") }
    var txDesc by remember { mutableStateOf("") }
    var txDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(strings.createEvent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(value = evTitle, onValueChange = { evTitle = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = evDate, onValueChange = { evDate = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = evFee,
                    onValueChange = { evFee = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Fee (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { OutlinedTextField(value = evLocation, onValueChange = { evLocation = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = evDesc, onValueChange = { evDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Button(
                onClick = {
                    onCreateEvent(evTitle, evDate, evLocation, evFee.toDoubleOrNull() ?: 0.0, evDesc)
                    evTitle = ""; evDate = ""; evLocation = ""; evDesc = ""
                },
                enabled = evTitle.trim().length >= 2 && evDate.trim().length >= 2 && evLocation.trim().length >= 2 && evDesc.trim().length >= 2,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.createEvent) }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        item { Text(strings.createCampaign, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(value = fundTitle, onValueChange = { fundTitle = it }, label = { Text("Campaign Title") }, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedTextField(
                value = fundTarget,
                onValueChange = { fundTarget = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Target Amount (৳)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = {
                    val t = fundTarget.toDoubleOrNull() ?: 0.0
                    if (t > 0) {
                        onCreateCampaign(fundTitle, t)
                        fundTitle = ""; fundTarget = ""
                    }
                },
                enabled = fundTitle.trim().length >= 2 && (fundTarget.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.createCampaign) }
        }

        if (campaigns.isNotEmpty()) {
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { Text(strings.addTransaction, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    campaigns.forEach { c ->
                        FilterChip(
                            selected = selectedCampaignId == c.id,
                            onClick = { selectedCampaignId = c.id },
                            label = { Text(c.title) }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = txType == TransactionType.IN, onClick = { txType = TransactionType.IN }, label = { Text(strings.incomeIn) })
                    FilterChip(selected = txType == TransactionType.OUT, onClick = { txType = TransactionType.OUT }, label = { Text(strings.expenseOut) })
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = txAmount,
                        onValueChange = { txAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(value = txDate, onValueChange = { txDate = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
                }
            }
            item { OutlinedTextField(value = txDesc, onValueChange = { txDesc = it }, label = { Text("Transaction Description") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    onClick = {
                        val amt = txAmount.toDoubleOrNull() ?: 0.0
                        if (selectedCampaignId.isNotBlank() && amt > 0) {
                            onAddTransaction(selectedCampaignId, txType, amt, txDesc, txDate)
                            txAmount = ""; txDesc = ""
                        }
                    },
                    enabled = selectedCampaignId.isNotBlank() && (txAmount.toDoubleOrNull() ?: 0.0) > 0 && txDesc.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.addTransaction) }
            }
        }
    }
}

@Composable
private fun AdminTeachersSection(
    teachersState: UiState<List<Teacher>>,
    strings: AppStrings,
    onCreateTeacher: (String, String, String, String, String) -> Unit,
    onUpdateTeacher: (String, String, String, String, String, String) -> Unit,
    onDeleteTeacher: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var tenure by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(TeacherStatus.CURRENT) }
    var editingTeacher by remember { mutableStateOf<Teacher?>(null) }

    val teachers = (teachersState as? UiState.Success)?.data.orEmpty()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Teacher Name") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = tenure, onValueChange = { tenure = it }, label = { Text("Tenure (e.g. 1998-2024)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.phoneLabel) }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = status == TeacherStatus.CURRENT, onClick = { status = TeacherStatus.CURRENT }, label = { Text(strings.teacherCurrent) })
                FilterChip(selected = status == TeacherStatus.RETIRED, onClick = { status = TeacherStatus.RETIRED }, label = { Text(strings.teacherRetired) })
                FilterChip(selected = status == TeacherStatus.LATE, onClick = { status = TeacherStatus.LATE }, label = { Text(strings.teacherLate) })
            }
        }
        item {
            Button(
                onClick = {
                    onCreateTeacher(name, subject, tenure, phone, status)
                    name = ""; subject = ""; tenure = ""; phone = ""
                },
                enabled = name.trim().length >= 2 && subject.trim().length >= 2 && tenure.trim().length >= 2 && phone.trim().length >= 4,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.addTeacher) }
        }
        items(teachers, key = { it.id }) { t ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.Bold)
                        Text("${t.subject} • ${t.tenure}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { editingTeacher = t }) {
                        Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeleteTeacher(t.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.deleteLabel, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editingTeacher?.let { t ->
        var eName by remember(t) { mutableStateOf(t.name) }
        var eSubject by remember(t) { mutableStateOf(t.subject) }
        var eTenure by remember(t) { mutableStateOf(t.tenure) }
        var ePhone by remember(t) { mutableStateOf(t.phone) }
        var eStatus by remember(t) { mutableStateOf(t.status) }
        AlertDialog(
            onDismissRequest = { editingTeacher = null },
            title = { Text(strings.editLabel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = eName, onValueChange = { eName = it }, label = { Text("Teacher Name") }, singleLine = true)
                    OutlinedTextField(value = eSubject, onValueChange = { eSubject = it }, label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(value = eTenure, onValueChange = { eTenure = it }, label = { Text("Tenure") }, singleLine = true)
                    OutlinedTextField(value = ePhone, onValueChange = { ePhone = it }, label = { Text(strings.phoneLabel) }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = eStatus == TeacherStatus.CURRENT, onClick = { eStatus = TeacherStatus.CURRENT }, label = { Text(strings.teacherCurrent) })
                        FilterChip(selected = eStatus == TeacherStatus.RETIRED, onClick = { eStatus = TeacherStatus.RETIRED }, label = { Text(strings.teacherRetired) })
                        FilterChip(selected = eStatus == TeacherStatus.LATE, onClick = { eStatus = TeacherStatus.LATE }, label = { Text(strings.teacherLate) })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTeacher(t.id, eName, eSubject, eTenure, ePhone, eStatus)
                        editingTeacher = null
                    },
                    enabled = eName.trim().length >= 2 && eSubject.trim().length >= 2
                ) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { editingTeacher = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun AdminModeratorsSection(
    approvedAlumniState: UiState<List<UserProfile>>,
    strings: AppStrings,
    onSetModerator: (UserProfile, Boolean) -> Unit
) {
    when (approvedAlumniState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorBannerCard(approvedAlumniState.message) }
        is UiState.Success -> {
            val candidates = approvedAlumniState.data.filter { it.role != UserRoles.ADMIN }
            if (candidates.isEmpty()) {
                Box(Modifier.padding(16.dp)) { EmptyStateCard(Icons.Rounded.VerifiedUser, strings.noAlumniFound) }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(candidates, key = { it.uid }) { user ->
                        val isMod = user.role == UserRoles.MODERATOR
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${strings.batchPrefix} ${user.sscBatch}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(4.dp))
                                    RoleBadge(role = user.role, strings = strings)
                                }
                                OutlinedButton(onClick = { onSetModerator(user, !isMod) }) {
                                    Text(if (isMod) strings.removeModerator else strings.makeModerator)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
