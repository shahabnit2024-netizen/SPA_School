package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FundCampaign
import com.example.data.model.FundTransaction
import com.example.data.model.Teacher
import com.example.data.model.TeacherStatus
import com.example.data.model.TransactionType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatusPill
import com.example.ui.components.launchPhoneDialer
import com.example.ui.i18n.AppStrings
import com.example.ui.state.UiState
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.CrimsonBloodContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnCrimsonBloodContainer
import com.example.ui.theme.OnEmeraldPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    campaign: FundCampaign?,
    isSuperAdmin: Boolean,
    transactionsState: UiState<List<FundTransaction>>,
    strings: AppStrings,
    onDeleteTransaction: (FundTransaction) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(campaign?.title ?: strings.ledgerTitle, maxLines = 1) },
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
                .padding(16.dp)
        ) {
            if (campaign != null) {
                val balance = (campaign.collectedAmount - campaign.spentAmount).coerceAtLeast(0.0)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column {
                            Text(strings.collectedLabel, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "৳${formatCurrency(campaign.collectedAmount)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = OnEmeraldPrimaryContainer
                            )
                        }
                        Column {
                            Text(strings.spentLabel, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "৳${formatCurrency(campaign.spentAmount)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = CrimsonBlood
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.balanceLabel, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "৳${formatCurrency(balance)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = OnEmeraldPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            Text(
                text = strings.ledgerTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            when (transactionsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> ErrorBannerCard(transactionsState.message)
                is UiState.Success -> {
                    if (transactionsState.data.isEmpty()) {
                        EmptyStateCard(Icons.AutoMirrored.Rounded.ReceiptLong, strings.noTransactions)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(transactionsState.data, key = { it.id }) { tx ->
                                val isIn = tx.type == TransactionType.IN
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isIn) EmeraldPrimaryContainer else CrimsonBloodContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isIn) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                                    contentDescription = if (isIn) strings.incomeIn else strings.expenseOut,
                                                    tint = if (isIn) OnEmeraldPrimaryContainer else OnCrimsonBloodContainer
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tx.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${tx.date} • ${tx.addedByName.ifBlank { "Admin" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${if (isIn) "+" else "-"}৳${formatCurrency(tx.amount)}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isIn) EmeraldPrimary else CrimsonBlood
                                        )
                                        if (isSuperAdmin) {
                                            IconButton(onClick = { onDeleteTransaction(tx) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.deleteLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(
    isSuperAdmin: Boolean,
    teachersState: UiState<List<Teacher>>,
    strings: AppStrings,
    onUpdateTeacher: (teacherId: String, name: String, subject: String, tenure: String, phone: String, status: String) -> Unit,
    onDeleteTeacher: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var editingTeacher by remember { mutableStateOf<Teacher?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.teachersTitle) },
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text(strings.allBatches.replace("ব্যাচ", "সকল").replace("Batches", "All")) }
                    )
                }
                item {
                    FilterChip(
                        selected = statusFilter == TeacherStatus.CURRENT,
                        onClick = { statusFilter = TeacherStatus.CURRENT },
                        label = { Text(strings.teacherCurrent) }
                    )
                }
                item {
                    FilterChip(
                        selected = statusFilter == TeacherStatus.RETIRED,
                        onClick = { statusFilter = TeacherStatus.RETIRED },
                        label = { Text(strings.teacherRetired) }
                    )
                }
                item {
                    FilterChip(
                        selected = statusFilter == TeacherStatus.LATE,
                        onClick = { statusFilter = TeacherStatus.LATE },
                        label = { Text(strings.teacherLate) }
                    )
                }
            }

            when (teachersState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorBannerCard(teachersState.message) }
                is UiState.Success -> {
                    val filtered = teachersState.data.filter { statusFilter == null || it.status == statusFilter }
                    if (filtered.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            EmptyStateCard(Icons.Rounded.School, strings.noNoticesYet)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { teacher ->
                                val statusLabel = when (teacher.status) {
                                    TeacherStatus.CURRENT -> strings.teacherCurrent
                                    TeacherStatus.RETIRED -> strings.teacherRetired
                                    else -> strings.teacherLate
                                }
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = teacher.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                StatusPill(
                                                    text = statusLabel,
                                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = teacher.subject,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = teacher.tenure,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (teacher.status != TeacherStatus.LATE && teacher.phone.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = { launchPhoneDialer(context, teacher.phone) },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Call, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(strings.call)
                                                }
                                            }
                                            if (isSuperAdmin) {
                                                IconButton(onClick = { editingTeacher = teacher }, modifier = Modifier.size(34.dp)) {
                                                    Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(onClick = { onDeleteTeacher(teacher.id) }, modifier = Modifier.size(34.dp)) {
                                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.deleteLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
            dismissButton = { TextButton(onClick = { editingTeacher = null }) { Text(strings.cancel) } }
        )
    }
}
