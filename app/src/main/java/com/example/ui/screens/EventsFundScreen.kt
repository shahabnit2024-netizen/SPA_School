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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AlumniEvent
import com.example.data.model.FundCampaign
import com.example.data.model.FundStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.state.UiState
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnEmeraldPrimaryContainer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EventsFundScreen(
    currentUserId: String,
    isSuperAdmin: Boolean,
    eventsState: UiState<List<AlumniEvent>>,
    fundsState: UiState<List<FundCampaign>>,
    strings: AppStrings,
    openFundsTabInitially: Boolean = false,
    onToggleGoing: (AlumniEvent) -> Unit,
    onOpenLedger: (FundCampaign) -> Unit,
    onUpdateEvent: (eventId: String, title: String, date: String, location: String, fee: Double, description: String) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onUpdateCampaign: (campaignId: String, title: String, targetAmount: Double, status: String) -> Unit,
    onDeleteCampaign: (String) -> Unit
) {
    var selectedTab by remember(openFundsTabInitially) {
        mutableStateOf(if (openFundsTabInitially) 1 else 0)
    }
    var editingEvent by remember { mutableStateOf<AlumniEvent?>(null) }
    var editingCampaign by remember { mutableStateOf<FundCampaign?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(strings.eventsTab)
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(strings.fundsTab)
            }
        }

        if (selectedTab == 0) {
            when (eventsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorBannerCard(eventsState.message) }
                is UiState.Success -> {
                    if (eventsState.data.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            EmptyStateCard(Icons.Rounded.Event, strings.noNoticesYet)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(eventsState.data, key = { it.id }) { event ->
                                EventCard(
                                    event = event,
                                    isGoing = event.registeredUsers.contains(currentUserId),
                                    isSuperAdmin = isSuperAdmin,
                                    strings = strings,
                                    onToggleGoing = { onToggleGoing(event) },
                                    onEdit = { editingEvent = event },
                                    onDelete = { onDeleteEvent(event.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            when (fundsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorBannerCard(fundsState.message) }
                is UiState.Success -> {
                    if (fundsState.data.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            EmptyStateCard(Icons.Rounded.AccountBalanceWallet, strings.noNoticesYet)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(fundsState.data, key = { it.id }) { campaign ->
                                FundCampaignCard(
                                    campaign = campaign,
                                    isSuperAdmin = isSuperAdmin,
                                    strings = strings,
                                    onOpenLedger = { onOpenLedger(campaign) },
                                    onEdit = { editingCampaign = campaign },
                                    onDelete = { onDeleteCampaign(campaign.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingEvent?.let { ev ->
        EditEventDialog(
            event = ev,
            strings = strings,
            onDismiss = { editingEvent = null },
            onSave = { title, date, loc, fee, desc ->
                onUpdateEvent(ev.id, title, date, loc, fee, desc)
                editingEvent = null
            }
        )
    }

    editingCampaign?.let { camp ->
        EditCampaignDialog(
            campaign = camp,
            strings = strings,
            onDismiss = { editingCampaign = null },
            onSave = { title, target, status ->
                onUpdateCampaign(camp.id, title, target, status)
                editingCampaign = null
            }
        )
    }
}

@Composable
private fun EventCard(
    event: AlumniEvent,
    isGoing: Boolean,
    isSuperAdmin: Boolean,
    strings: AppStrings,
    onToggleGoing: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = event.date,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        text = if (event.fee <= 0.0) strings.freeLabel else "৳${formatCurrency(event.fee)}",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (isSuperAdmin) {
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
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${event.registeredUsers.size} ${strings.attendeesSuffix}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily)
                    )
                }
                if (isGoing) {
                    Button(
                        onClick = onToggleGoing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimaryContainer,
                            contentColor = OnEmeraldPrimaryContainer
                        ),
                        modifier = Modifier.testTag("event_toggle_${event.id}")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.goingToggle)
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleGoing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("event_toggle_${event.id}")
                    ) {
                        Text(strings.registerToggle)
                    }
                }
            }
        }
    }
}

@Composable
private fun FundCampaignCard(
    campaign: FundCampaign,
    isSuperAdmin: Boolean,
    strings: AppStrings,
    onOpenLedger: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (campaign.targetAmount > 0) {
        (campaign.collectedAmount / campaign.targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    val balance = (campaign.collectedAmount - campaign.spentAmount).coerceAtLeast(0.0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = campaign.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusPill(
                    text = campaign.status.uppercase(),
                    containerColor = if (campaign.status == FundStatus.ACTIVE) EmeraldPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (campaign.status == FundStatus.ACTIVE) OnEmeraldPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSuperAdmin) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.deleteLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
            )
            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                FundMetric(label = strings.collectedLabel, amount = campaign.collectedAmount)
                FundMetric(label = strings.spentLabel, amount = campaign.spentAmount)
                FundMetric(label = strings.balanceLabel, amount = balance)
                FundMetric(label = strings.targetLabel, amount = campaign.targetAmount)
            }

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onOpenLedger,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("view_ledger_${campaign.id}")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.viewLedger)
            }
        }
    }
}

@Composable
private fun FundMetric(label: String, amount: Double) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "৳${formatCurrency(amount)}",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

@Composable
private fun EditEventDialog(
    event: AlumniEvent,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, String) -> Unit
) {
    var title by remember(event) { mutableStateOf(event.title) }
    var date by remember(event) { mutableStateOf(event.date) }
    var location by remember(event) { mutableStateOf(event.location) }
    var feeText by remember(event) { mutableStateOf(event.fee.toInt().toString()) }
    var desc by remember(event) { mutableStateOf(event.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, singleLine = true)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, singleLine = true)
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, singleLine = true)
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Fee (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, date, location, feeText.toDoubleOrNull() ?: 0.0, desc) },
                enabled = title.trim().length >= 2 && date.trim().length >= 2 && location.trim().length >= 2
            ) { Text(strings.saveChanges) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun EditCampaignDialog(
    campaign: FundCampaign,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    var title by remember(campaign) { mutableStateOf(campaign.title) }
    var targetText by remember(campaign) { mutableStateOf(campaign.targetAmount.toInt().toString()) }
    var status by remember(campaign) { mutableStateOf(campaign.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Campaign Title") }, singleLine = true)
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = status == FundStatus.ACTIVE, onClick = { status = FundStatus.ACTIVE }, label = { Text("ACTIVE") })
                    FilterChip(selected = status == FundStatus.COMPLETED, onClick = { status = FundStatus.COMPLETED }, label = { Text("COMPLETED") })
                }
            }
        },
        confirmButton = {
            val target = targetText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onSave(title, target, status) },
                enabled = title.trim().length >= 2 && target > 0.0
            ) { Text(strings.saveChanges) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
