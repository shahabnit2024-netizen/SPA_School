package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WorkOutline
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppBranding
import com.example.data.model.Notice
import com.example.data.model.NoticeCategories
import com.example.data.model.UserProfile
import com.example.ui.components.AppLogoImage
import com.example.ui.components.AvatarCircle
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.state.UiState
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.CrimsonBloodContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.GoldTertiaryContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnCrimsonBloodContainer
import com.example.ui.theme.OnEmeraldPrimaryContainer
import com.example.ui.theme.OnGoldTertiaryContainer
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: UserProfile,
    branding: AppBranding?,
    noticesState: UiState<List<Notice>>,
    strings: AppStrings,
    onNavigateUrgentBlood: () -> Unit,
    onNavigateDonation: () -> Unit,
    onNavigateTeachers: () -> Unit,
    onNavigateGallery: () -> Unit,
    onNavigateAbout: () -> Unit,
    onShareFeedPost: (title: String, description: String, category: String) -> Unit,
    onUpdateNotice: (noticeId: String, title: String, description: String, category: String, isPinned: Boolean) -> Unit,
    onDeleteNotice: (String) -> Unit
) {
    var showShareSheet by remember { mutableStateOf(false) }
    var editingNotice by remember { mutableStateOf<Notice?>(null) }

    val resolvedSchoolName = if (strings.language == AppLanguage.BENGALI) {
        branding?.schoolNameBn?.ifBlank { strings.schoolFullName } ?: strings.schoolFullName
    } else {
        branding?.schoolNameEn?.ifBlank { strings.schoolFullName } ?: strings.schoolFullName
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Campus Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 356.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_campus_banner),
                        contentDescription = strings.schoolFullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.15f),
                                        Color(0xFF022C22).copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 356.dp)
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppLogoImage(
                                logoDataOrUrl = branding?.logoDataOrUrl,
                                contentDescription = strings.appLogoLabel,
                                size = 154.dp,
                                modifier = Modifier.testTag("home_hero_school_logo")
                            )
                            RoleBadge(role = currentUser.role, strings = strings)
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "$resolvedSchoolName • ${strings.batchPrefix} ${currentUser.sscBatch}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(2.5.dp, Color.White.copy(alpha = 0.88f)),
                                shadowElevation = 6.dp
                            ) {
                                AvatarCircle(
                                    name = currentUser.name,
                                    photoUrl = currentUser.photoUrl,
                                    size = 164.dp,
                                    modifier = Modifier.testTag("home_user_profile_avatar")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Actions Grid (2x2)
        item {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        title = strings.urgentBlood,
                        icon = Icons.Rounded.Bloodtype,
                        containerColor = CrimsonBloodContainer,
                        contentColor = OnCrimsonBloodContainer,
                        iconTint = CrimsonBlood,
                        onClick = onNavigateUrgentBlood,
                        modifier = Modifier.weight(1f).testTag("quick_urgent_blood")
                    )
                    QuickActionCard(
                        title = strings.donation,
                        icon = Icons.Rounded.VolunteerActivism,
                        containerColor = EmeraldPrimaryContainer,
                        contentColor = OnEmeraldPrimaryContainer,
                        iconTint = EmeraldPrimary,
                        onClick = onNavigateDonation,
                        modifier = Modifier.weight(1f).testTag("quick_donation")
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        title = strings.teachersDirectory,
                        icon = Icons.Rounded.School,
                        containerColor = GoldTertiaryContainer,
                        contentColor = OnGoldTertiaryContainer,
                        iconTint = GoldTertiary,
                        onClick = onNavigateTeachers,
                        modifier = Modifier.weight(1f).testTag("quick_teachers")
                    )
                    QuickActionCard(
                        title = strings.batchGalleryTitle,
                        icon = Icons.Rounded.PhotoLibrary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateGallery,
                        modifier = Modifier.weight(1f).testTag("quick_batch_gallery")
                    )
                }
                QuickActionCard(
                    title = strings.aboutActionLabel,
                    icon = Icons.Rounded.Info,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateAbout,
                    modifier = Modifier.fillMaxWidth().testTag("quick_about_school")
                )
            }
        }

        // 3. Notices & Community Feed Content
        when (noticesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    ErrorBannerCard(message = noticesState.message)
                }
            }
            is UiState.Success -> {
                val allNotices = noticesState.data
                val pinned = allNotices.filter { it.isPinned }
                val feedItems = allNotices.filter { !it.isPinned || it.category != NoticeCategories.NOTICE }

                if (pinned.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.pinnedNotices,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        val pagerState = rememberPagerState(pageCount = { pinned.size })
                        HorizontalPager(
                            state = pagerState,
                            pageSpacing = 12.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            PinnedNoticeCard(notice = pinned[page], strings = strings)
                        }
                        if (pinned.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(pinned.size) { idx ->
                                    val selected = pagerState.currentPage == idx
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .size(if (selected) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.communityFeed,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { showShareSheet = true },
                            modifier = Modifier.testTag("share_feed_post_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.shareFeedPost)
                        }
                    }
                }

                if (feedItems.isEmpty() && pinned.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Campaign,
                            message = strings.noNoticesYet
                        )
                    }
                } else {
                    items(allNotices, key = { it.id }) { notice ->
                        FeedNoticeCard(
                            notice = notice,
                            strings = strings,
                            canEdit = currentUser.isSuperAdmin,
                            canDelete = currentUser.isSuperAdmin || notice.postedBy == currentUser.uid,
                            onEdit = { editingNotice = notice },
                            onDelete = { onDeleteNotice(notice.id) }
                        )
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        ShareFeedSheet(
            strings = strings,
            onDismiss = { showShareSheet = false },
            onSubmit = { title, desc, cat ->
                onShareFeedPost(title, desc, cat)
                showShareSheet = false
            }
        )
    }

    editingNotice?.let { notice ->
        EditNoticeDialog(
            notice = notice,
            strings = strings,
            onDismiss = { editingNotice = null },
            onSave = { t, d, c, pin ->
                onUpdateNotice(notice.id, t, d, c, pin)
                editingNotice = null
            }
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.height(98.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PinnedNoticeCard(notice: Notice, strings: AppStrings) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusPill(
                    text = strings.categoryNotice,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = Color.White
                )
                Text(
                    text = formatTimestamp(notice.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = notice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeedNoticeCard(
    notice: Notice,
    strings: AppStrings,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (catLabel, catBg, catFg, icon) = when (notice.category) {
        NoticeCategories.JOB -> Quad(
            strings.categoryJob,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Rounded.WorkOutline
        )
        NoticeCategories.NEWS -> Quad(
            strings.categoryNews,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Rounded.Newspaper
        )
        else -> Quad(
            strings.categoryNotice,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Rounded.Campaign
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
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
                    Icon(icon, contentDescription = null, tint = catFg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    StatusPill(text = catLabel, containerColor = catBg, contentColor = catFg)
                    if (notice.isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canEdit) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = strings.editLabel,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = strings.deleteLabel,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = notice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notice.postedByName.ifBlank { "SPA Alumni" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTimestamp(notice.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareFeedSheet(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String, category: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(NoticeCategories.JOB) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = strings.shareFeedPost,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = category == NoticeCategories.JOB,
                    onClick = { category = NoticeCategories.JOB },
                    label = { Text(strings.categoryJob) }
                )
                FilterChip(
                    selected = category == NoticeCategories.NEWS,
                    onClick = { category = NoticeCategories.NEWS },
                    label = { Text(strings.categoryNews) }
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Details / Referral Info") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        if (title.trim().length >= 2 && description.trim().length >= 2) {
                            onSubmit(title, description, category)
                        }
                    },
                    enabled = title.trim().length >= 2 && description.trim().length >= 2,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.publishNotice)
                }
            }
        }
    }
}

@Composable
private fun EditNoticeDialog(
    notice: Notice,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, category: String, isPinned: Boolean) -> Unit
) {
    var title by remember(notice) { mutableStateOf(notice.title) }
    var description by remember(notice) { mutableStateOf(notice.description) }
    var category by remember(notice) { mutableStateOf(notice.category) }
    var isPinned by remember(notice) { mutableStateOf(notice.isPinned) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = category == NoticeCategories.NOTICE, onClick = { category = NoticeCategories.NOTICE }, label = { Text(strings.categoryNotice) })
                    FilterChip(selected = category == NoticeCategories.JOB, onClick = { category = NoticeCategories.JOB }, label = { Text(strings.categoryJob) })
                    FilterChip(selected = category == NoticeCategories.NEWS, onClick = { category = NoticeCategories.NEWS }, label = { Text(strings.categoryNews) })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.pinNoticeToggle, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, category, isPinned) },
                enabled = title.trim().length >= 2 && description.trim().length >= 2
            ) { Text(strings.saveChanges) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

fun formatTimestamp(ts: com.google.firebase.Timestamp?): String {
    if (ts == null) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(ts.toDate())
}
