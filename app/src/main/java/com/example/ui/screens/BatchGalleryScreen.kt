package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BatchGalleryPhoto
import com.example.data.model.GalleryStatus
import com.example.data.model.UserProfile
import com.example.ui.components.DecodedOrRemoteImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FullScreenImageLightboxDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.encodeCompressedImageUriToDataUri
import com.example.ui.i18n.AppStrings
import com.example.ui.state.UiState
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchGalleryScreen(
    currentUser: UserProfile,
    approvedGalleryState: UiState<List<BatchGalleryPhoto>>,
    myUploadsState: UiState<List<BatchGalleryPhoto>>,
    strings: AppStrings,
    onUploadPhoto: (sscBatch: String, caption: String, imageDataOrUrl: String) -> Unit,
    onDeletePhoto: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedBatchFilter by remember { mutableStateOf("") }
    var showUploadSheet by remember { mutableStateOf(false) }
    var lightboxPhoto by remember { mutableStateOf<BatchGalleryPhoto?>(null) }

    val approvedPhotos = (approvedGalleryState as? UiState.Success)?.data.orEmpty()
    val myPendingOrRejected = (myUploadsState as? UiState.Success)?.data.orEmpty()
        .filter { it.status != GalleryStatus.APPROVED }

    val distinctBatches = remember(approvedPhotos) {
        approvedPhotos.map { it.sscBatch }.filter { it.isNotBlank() }.distinct().sortedDescending()
    }
    val filteredPhotos = remember(approvedPhotos, selectedBatchFilter) {
        if (selectedBatchFilter.isBlank()) approvedPhotos
        else approvedPhotos.filter { it.sscBatch == selectedBatchFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.batchGalleryTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("gallery_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showUploadSheet = true },
                icon = { Icon(Icons.Rounded.AddAPhoto, contentDescription = strings.uploadGroupPhoto) },
                text = { Text(strings.uploadGroupPhoto) },
                modifier = Modifier.testTag("gallery_upload_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 92.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (myPendingOrRejected.isNotEmpty()) {
                item {
                    Text(
                        text = strings.mySubmissionsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(myPendingOrRejected, key = { "mine_${it.id}" }) { photo ->
                    MySubmissionGalleryCard(
                        photo = photo,
                        strings = strings,
                        onOpenFull = { lightboxPhoto = photo },
                        onDelete = { onDeletePhoto(photo.id) }
                    )
                }
            }

            if (distinctBatches.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedBatchFilter.isBlank(),
                                onClick = { selectedBatchFilter = "" },
                                label = { Text(strings.allBatches) },
                                modifier = Modifier.testTag("gallery_filter_all")
                            )
                        }
                        items(distinctBatches, key = { it }) { batch ->
                            FilterChip(
                                selected = selectedBatchFilter == batch,
                                onClick = {
                                    selectedBatchFilter = if (selectedBatchFilter == batch) "" else batch
                                },
                                label = { Text("${strings.batchPrefix} $batch") },
                                modifier = Modifier.testTag("gallery_filter_$batch")
                            )
                        }
                    }
                }
            }

            when (approvedGalleryState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        ErrorBannerCard(message = approvedGalleryState.message)
                    }
                }
                is UiState.Success -> {
                    if (filteredPhotos.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.PhotoLibrary,
                                message = strings.galleryEmptyState
                            )
                        }
                    } else {
                        items(filteredPhotos, key = { it.id }) { photo ->
                            ApprovedGalleryCard(
                                photo = photo,
                                strings = strings,
                                canDelete = currentUser.isSuperAdmin || photo.uploadedBy == currentUser.uid,
                                onOpenFull = { lightboxPhoto = photo },
                                onDelete = { onDeletePhoto(photo.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUploadSheet) {
        UploadBatchPhotoSheet(
            defaultBatch = currentUser.sscBatch,
            isSuperAdmin = currentUser.isSuperAdmin,
            strings = strings,
            onDismiss = { showUploadSheet = false },
            onSubmit = { batch, caption, imageData ->
                onUploadPhoto(batch, caption, imageData)
                showUploadSheet = false
            }
        )
    }

    lightboxPhoto?.let { photo ->
        FullScreenImageLightboxDialog(
            title = "${strings.batchPrefix} ${photo.sscBatch}",
            subtitle = photo.caption,
            imageDataOrUrl = photo.imageDataOrUrl,
            onDismiss = { lightboxPhoto = null }
        )
    }
}

@Composable
private fun MySubmissionGalleryCard(
    photo: BatchGalleryPhoto,
    strings: AppStrings,
    onOpenFull: () -> Unit,
    onDelete: () -> Unit
) {
    val isRejected = photo.status == GalleryStatus.REJECTED
    val statusText = if (isRejected) strings.rejectedBadge else strings.pendingApprovalBadge
    val statusBg = if (isRejected) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val statusFg = if (isRejected) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("my_gallery_submission_${photo.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            DecodedOrRemoteImage(
                imageDataOrUrl = photo.imageDataOrUrl,
                contentDescription = photo.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClickLabel = photo.caption, onClick = onOpenFull)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusPill(
                    text = statusText,
                    containerColor = statusBg,
                    contentColor = statusFg
                )
                Text(
                    text = "${strings.batchPrefix} ${photo.sscBatch} • ${photo.caption}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = strings.deleteLabel,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ApprovedGalleryCard(
    photo: BatchGalleryPhoto,
    strings: AppStrings,
    canDelete: Boolean,
    onOpenFull: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("gallery_photo_card_${photo.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClickLabel = photo.caption, onClick = onOpenFull)
            ) {
                DecodedOrRemoteImage(
                    imageDataOrUrl = photo.imageDataOrUrl,
                    contentDescription = photo.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.56f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${strings.batchPrefix} ${photo.sscBatch}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.50f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = "Zoom photo",
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp).size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = photo.caption,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (photo.uploadedByName.isNotBlank()) {
                            Text(
                                text = "${strings.uploadedByLabel}: ${photo.uploadedByName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val dateStr = formatTimestamp(photo.createdAt)
                        if (dateStr.isNotBlank()) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.deleteLabel,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadBatchPhotoSheet(
    defaultBatch: String,
    isSuperAdmin: Boolean,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSubmit: (sscBatch: String, caption: String, imageDataOrUrl: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sscBatch by remember { mutableStateOf(defaultBatch) }
    var caption by remember { mutableStateOf("") }
    var encodedPhoto by remember { mutableStateOf<String?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingPhoto = true
            scope.launch {
                val dataUri = withContext(Dispatchers.IO) {
                    encodeCompressedImageUriToDataUri(
                        context = context,
                        uri = uri,
                        maxDim = 960,
                        maxChars = 380_000
                    )
                }
                isProcessingPhoto = false
                if (dataUri != null) {
                    encodedPhoto = dataUri
                } else {
                    Toast.makeText(context, strings.photoProcessingError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = strings.uploadGroupPhoto,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (!isSuperAdmin) {
                Text(
                    text = strings.galleryApprovalHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (encodedPhoto != null) {
                DecodedOrRemoteImage(
                    imageDataOrUrl = encodedPhoto,
                    contentDescription = strings.uploadGroupPhoto,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isProcessingPhoto,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pick_batch_group_photo_button")
            ) {
                if (isProcessingPhoto) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(strings.pickGroupPhotoButton)
            }

            OutlinedTextField(
                value = sscBatch,
                onValueChange = { if (it.length <= 4) sscBatch = it.filter(Char::isDigit) },
                label = { Text(strings.sscBatchLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("batch_gallery_year_input")
            )

            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 300) caption = it },
                label = { Text(strings.photoCaptionLabel) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("batch_gallery_caption_input")
            )

            val isValid = sscBatch.matches(Regex("^[0-9]{4}$")) &&
                caption.trim().length in 2..300 &&
                !encodedPhoto.isNullOrBlank() &&
                !isProcessingPhoto

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        encodedPhoto?.let { img ->
                            onSubmit(sscBatch, caption, img)
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("submit_batch_photo_button")
                ) {
                    Text(strings.submitForApproval, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
