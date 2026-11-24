package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ApprovalStatus
import com.example.data.model.BloodGroups
import com.example.data.model.UserProfile
import com.example.data.model.UserRoles
import com.example.data.model.maskPhoneNumber
import com.example.ui.components.AvatarCircle
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatusPill
import com.example.ui.components.launchPhoneDialer
import com.example.ui.components.launchWhatsApp
import com.example.ui.i18n.AppStrings
import com.example.ui.state.UiState
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun DirectoryScreen(
    currentUser: UserProfile,
    alumniState: UiState<List<UserProfile>>,
    strings: AppStrings,
    onAdminUpdateUser: (
        targetUid: String,
        name: String,
        phone: String,
        hidePhone: Boolean,
        sscBatch: String,
        bloodGroup: String,
        profession: String,
        city: String,
        country: String,
        role: String,
        status: String
    ) -> Unit,
    onAdminDeleteUser: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBatch by remember { mutableStateOf<String?>(null) }
    var editingMember by remember { mutableStateOf<UserProfile?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchDirectoryHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("directory_search_input")
            )
        }

        when (alumniState) {
            is UiState.Loading -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    ErrorBannerCard(message = alumniState.message)
                }
            }
            is UiState.Success -> {
                val allMembers = alumniState.data
                val availableBatches = remember(allMembers) {
                    allMembers.map { it.sscBatch }.filter { it.isNotBlank() }.distinct().sortedDescending()
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedBatch == null,
                            onClick = { selectedBatch = null },
                            label = { Text(strings.allBatches) }
                        )
                    }
                    items(availableBatches) { batch ->
                        FilterChip(
                            selected = selectedBatch == batch,
                            onClick = { selectedBatch = if (selectedBatch == batch) null else batch },
                            label = {
                                Text(
                                    "${strings.batchPrefix} $batch",
                                    fontFamily = JetBrainsMonoFamily
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val filtered = remember(allMembers, searchQuery, selectedBatch) {
                    val q = searchQuery.trim().lowercase()
                    allMembers.filter { m ->
                        val batchMatch = selectedBatch == null || m.sscBatch == selectedBatch
                        val queryMatch = q.isEmpty() ||
                            m.name.lowercase().contains(q) ||
                            m.profession.lowercase().contains(q) ||
                            m.city.lowercase().contains(q) ||
                            m.country.lowercase().contains(q)
                        batchMatch && queryMatch
                    }
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Rounded.Groups,
                            message = strings.noAlumniFound
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.uid }) { member ->
                            AlumniMemberCard(
                                member = member,
                                strings = strings,
                                canAdminEdit = currentUser.isSuperAdmin,
                                canAdminDelete = currentUser.isSuperAdmin && member.uid != currentUser.uid,
                                onEdit = { editingMember = member },
                                onDelete = { onAdminDeleteUser(member.uid) }
                            )
                        }
                    }
                }
            }
        }
    }

    editingMember?.let { member ->
        AdminEditMemberDialog(
            member = member,
            strings = strings,
            onDismiss = { editingMember = null },
            onSave = { name, phone, hide, batch, bg, prof, city, country, role, status ->
                onAdminUpdateUser(member.uid, name, phone, hide, batch, bg, prof, city, country, role, status)
                editingMember = null
            }
        )
    }
}

@Composable
private fun AlumniMemberCard(
    member: UserProfile,
    strings: AppStrings,
    canAdminEdit: Boolean,
    canAdminDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val phoneVisible = !member.hidePhone && member.phone.isNotBlank()
    val displayPhone = if (member.hidePhone) maskPhoneNumber(member.phone) else member.phone

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("directory_member_card_${member.uid}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarCircle(name = member.name, photoUrl = member.photoUrl, size = 50.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    StatusPill(
                        text = "${strings.batchPrefix} ${member.sscBatch}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (canAdminEdit) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                if (canAdminDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.deleteLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
                BloodGroupBadge(bloodGroup = member.bloodGroup)
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.BusinessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = member.profession,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${member.city}, ${member.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (member.hidePhone) Icons.Rounded.Lock else Icons.Rounded.Call,
                        contentDescription = null,
                        tint = if (member.hidePhone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (member.hidePhone) "${strings.phoneHiddenBadge} ($displayPhone)" else displayPhone,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { launchPhoneDialer(context, member.phone) },
                    enabled = phoneVisible,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("call_member_${member.uid}")
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.call, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { launchWhatsApp(context, member.phone) },
                    enabled = phoneVisible,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).testTag("whatsapp_member_${member.uid}")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.whatsapp, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminEditMemberDialog(
    member: UserProfile,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String, String, String, String, String, String, String) -> Unit
) {
    var name by remember(member) { mutableStateOf(member.name) }
    var phone by remember(member) { mutableStateOf(member.phone) }
    var hidePhone by remember(member) { mutableStateOf(member.hidePhone) }
    var sscBatch by remember(member) { mutableStateOf(member.sscBatch) }
    var bloodGroup by remember(member) { mutableStateOf(member.bloodGroup) }
    var profession by remember(member) { mutableStateOf(member.profession) }
    var city by remember(member) { mutableStateOf(member.city) }
    var country by remember(member) { mutableStateOf(member.country) }
    var role by remember(member) { mutableStateOf(member.role) }
    var status by remember(member) { mutableStateOf(member.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editLabel) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.fullNameLabel) }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.phoneLabel) }, singleLine = true)
                OutlinedTextField(
                    value = sscBatch,
                    onValueChange = { if (it.length <= 4) sscBatch = it.filter(Char::isDigit) },
                    label = { Text(strings.sscBatchLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BloodGroups.all.forEach { bg ->
                        FilterChip(selected = bloodGroup == bg, onClick = { bloodGroup = bg }, label = { Text(bg) })
                    }
                }
                OutlinedTextField(value = profession, onValueChange = { profession = it }, label = { Text(strings.professionLabel) }, singleLine = true)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text(strings.cityLabel) }, singleLine = true)
                OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text(strings.countryLabel) }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.hidePhoneLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = hidePhone, onCheckedChange = { hidePhone = it })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(UserRoles.MEMBER, UserRoles.MODERATOR, UserRoles.ADMIN).forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(ApprovalStatus.APPROVED, ApprovalStatus.PENDING, ApprovalStatus.REJECTED).forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, hidePhone, sscBatch, bloodGroup, profession, city, country, role, status) },
                enabled = name.trim().length >= 2 && sscBatch.matches(Regex("^[0-9]{4}$"))
            ) { Text(strings.saveChanges) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
