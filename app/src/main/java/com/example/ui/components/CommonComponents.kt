package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.UserRoles
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CrimsonBlood
import com.example.ui.theme.CrimsonBloodContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnCrimsonBloodContainer
import java.io.ByteArrayOutputStream

fun launchPhoneDialer(context: Context, phone: String) {
    val clean = phone.filter { it.isDigit() || it == '+' }
    if (clean.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
    } catch (_: Exception) {
        Toast.makeText(context, "No phone app found", Toast.LENGTH_SHORT).show()
    }
}

fun launchWhatsApp(context: Context, phone: String) {
    val digits = phone.filter { it.isDigit() }
    if (digits.isBlank()) return
    val normalized = if (digits.startsWith("0") && digits.length == 11) "88$digits" else digits
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized")))
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

fun launchExternalUrl(context: Context, url: String) {
    val clean = url.trim()
    if (!clean.startsWith("https://") && !clean.startsWith("http://")) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open update link", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun rememberDecodedImageBitmap(dataOrUrl: String?): ImageBitmap? {
    return remember(dataOrUrl) {
        if (dataOrUrl != null && dataOrUrl.startsWith("data:image/")) {
            try {
                val base64Part = dataOrUrl.substringAfter("base64,", "")
                if (base64Part.isNotEmpty()) {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }
}

@Composable
fun DecodedOrRemoteImage(
    imageDataOrUrl: String?,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = rememberDecodedImageBitmap(imageDataOrUrl)
    when {
        decodedBitmap != null -> {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        !imageDataOrUrl.isNullOrBlank() -> {
            AsyncImage(
                model = imageDataOrUrl,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    }
}

@Composable
fun AvatarCircle(
    name: String,
    photoUrl: String?,
    size: Dp = 48.dp,
    enableFullViewOnTap: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showFullPhoto by remember { mutableStateOf(false) }

    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "SA" }

    val decodedBitmap = rememberDecodedImageBitmap(photoUrl)
    val hasDisplayablePhoto = decodedBitmap != null ||
        (!photoUrl.isNullOrBlank() && (photoUrl.startsWith("https://") || photoUrl.startsWith("http://")))

    val interactiveModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(
            if (enableFullViewOnTap) {
                Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClickLabel = "$name profile photo") {
                        showFullPhoto = true
                    }
            } else {
                Modifier
            }
        )

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap,
            contentDescription = "$name avatar",
            contentScale = ContentScale.Crop,
            modifier = interactiveModifier.background(MaterialTheme.colorScheme.primaryContainer)
        )
    } else if (hasDisplayablePhoto && !photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "$name avatar",
            contentScale = ContentScale.Crop,
            modifier = interactiveModifier.background(MaterialTheme.colorScheme.primaryContainer)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = interactiveModifier.background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = initials,
                style = if (size >= 100.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    if (showFullPhoto) {
        FullScreenProfilePhotoDialog(
            name = name,
            initials = initials,
            photoUrl = photoUrl,
            onDismiss = { showFullPhoto = false }
        )
    }
}

@Composable
fun FullScreenProfilePhotoDialog(
    name: String,
    initials: String,
    photoUrl: String?,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        offset = if (nextScale > 1f) {
            offset + panChange
        } else {
            Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("full_profile_photo_viewer")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.2f
                                }
                            },
                            onTap = {
                                if (scale <= 1.05f) {
                                    onDismiss()
                                }
                            }
                        )
                    }
            ) {
                val decodedBitmap = rememberDecodedImageBitmap(photoUrl)
                val isValidRemote = !photoUrl.isNullOrBlank() &&
                    (photoUrl.startsWith("https://") || photoUrl.startsWith("http://"))
                if (decodedBitmap != null || isValidRemote) {
                    DecodedOrRemoteImage(
                        imageDataOrUrl = photoUrl,
                        contentDescription = "$name full profile photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 64.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(state = transformState)
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(state = transformState)
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_full_profile_photo")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenImageLightboxDialog(
    title: String,
    subtitle: String? = null,
    imageDataOrUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        offset = if (nextScale > 1f) offset + panChange else Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("full_gallery_photo_viewer")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.2f
                                }
                            },
                            onTap = {
                                if (scale <= 1.05f) onDismiss()
                            }
                        )
                    }
            ) {
                DecodedOrRemoteImage(
                    imageDataOrUrl = imageDataOrUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 72.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(state = transformState)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_full_gallery_photo")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageSelectorButtons(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
        modifier = modifier
    ) {
        LanguageOptionButton(
            label = "বাংলা",
            selected = currentLanguage == AppLanguage.BENGALI,
            compact = compact,
            onClick = { onSelectLanguage(AppLanguage.BENGALI) },
            modifier = Modifier.testTag("lang_btn_bn")
        )
        LanguageOptionButton(
            label = "English",
            selected = currentLanguage == AppLanguage.ENGLISH,
            compact = compact,
            onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
            modifier = Modifier.testTag("lang_btn_en")
        )
    }
}

@Composable
private fun LanguageOptionButton(
    label: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 6.dp else 8.dp
            )
        )
    }
}

@Composable
fun SplashOverlayScreen(
    imageResId: Int,
    contentDescription: String,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E1D3B))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onTap?.invoke() }
            )
    ) {
        val isPortraitAspect = maxHeight > maxWidth && (maxWidth / maxHeight) < 0.72f
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = contentDescription,
            contentScale = if (isPortraitAspect) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BloodGroupBadge(
    bloodGroup: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CrimsonBloodContainer,
        border = BorderStroke(1.dp, CrimsonBlood.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Text(
            text = bloodGroup,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = FontWeight.Bold,
            color = OnCrimsonBloodContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RoleBadge(role: String, strings: AppStrings) {
    val (label, bg, fg) = when (role) {
        UserRoles.ADMIN -> Triple(
            strings.roleAdmin,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        UserRoles.MODERATOR -> Triple(
            strings.roleModerator,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        else -> Triple(
            strings.roleMember,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
    StatusPill(text = label, containerColor = bg, contentColor = fg)
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorBannerCard(
    message: String,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (retryLabel != null && onRetry != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(retryLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun AppLogoImage(
    logoDataOrUrl: String?,
    contentDescription: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(logoDataOrUrl) {
        if (logoDataOrUrl != null && logoDataOrUrl.startsWith("data:image/")) {
            try {
                val base64Part = logoDataOrUrl.substringAfter("base64,", "")
                if (base64Part.isNotEmpty()) {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Surface(
        shape = CircleShape,
        color = if (decodedBitmap == null && logoDataOrUrl.isNullOrBlank()) Color.Transparent else Color.White,
        modifier = modifier.size(size)
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size).clip(CircleShape)
                )
            }
            !logoDataOrUrl.isNullOrBlank() && (logoDataOrUrl.startsWith("https://") || logoDataOrUrl.startsWith("http://")) -> {
                AsyncImage(
                    model = logoDataOrUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size).clip(CircleShape)
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_spa_crest),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size).clip(CircleShape)
                )
            }
        }
    }
}

fun encodeLogoUriToDataUri(context: Context, uri: Uri): String? {
    return encodeCompressedImageUriToDataUri(context = context, uri = uri, maxDim = 256, maxChars = 250_000)
}

fun encodeCompressedImageUriToDataUri(
    context: Context,
    uri: Uri,
    maxDim: Int,
    maxChars: Int
): String? {
    return try {
        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width.coerceAtLeast(1)
                val h = info.size.height.coerceAtLeast(1)
                val scale = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h, 1f)
                decoder.setTargetSize(
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1)
                )
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } ?: return null

        var workingDim = maxDim
        repeat(3) {
            val ratio = minOf(workingDim.toFloat() / rawBitmap.width, workingDim.toFloat() / rawBitmap.height, 1f)
            val targetW = (rawBitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (rawBitmap.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (targetW != rawBitmap.width || targetH != rawBitmap.height) {
                Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
            } else rawBitmap

            for (quality in listOf(84, 72, 60, 48)) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val dataUri = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (dataUri.length <= maxChars) {
                    return dataUri
                }
            }
            workingDim = (workingDim * 0.72f).toInt().coerceAtLeast(160)
        }
        null
    } catch (_: Exception) {
        null
    }
}

fun exitAndroidApp(context: Context) {
    (context as? Activity)?.finishAffinity()
}

@Composable
fun ExitAppConfirmationDialog(
    strings: AppStrings,
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.exitAppConfirmTitle) },
        text = { Text(strings.exitAppConfirmMessage) },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(strings.exitYes)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
