package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.auth.onGoogleSignInClicked
import com.example.data.model.BloodGroups
import com.example.data.model.UserProfile
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.ExitAppConfirmationDialog
import com.example.ui.components.LanguageSelectorButtons
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun AuthScreen(
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onRequestExit: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                LanguageSelectorButtons(
                    currentLanguage = currentLanguage,
                    onSelectLanguage = onSelectLanguage
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D1943),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(224.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_spa_crest),
                        contentDescription = strings.schoolFullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = strings.schoolFullName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.schoolSubtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_campus_banner),
                            contentDescription = "Sreepur Prasanna Academy campus",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = strings.signInTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = strings.signInSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showExitDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_exit_app_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.exitApp)
                }
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    initialName: String,
    existingRejectedProfile: UserProfile? = null,
    existingPrivatePhone: String? = null,
    isSubmitting: Boolean,
    onSubmit: (
        name: String,
        phone: String,
        hidePhone: Boolean,
        sscBatch: String,
        bloodGroup: String,
        profession: String,
        city: String,
        country: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var name by remember(existingRejectedProfile) {
        mutableStateOf(existingRejectedProfile?.name?.ifBlank { initialName } ?: initialName)
    }
    var phone by remember(existingRejectedProfile, existingPrivatePhone) {
        mutableStateOf(existingPrivatePhone ?: existingRejectedProfile?.phone.orEmpty())
    }
    var sscBatch by remember(existingRejectedProfile) {
        mutableStateOf(existingRejectedProfile?.sscBatch ?: "2015")
    }
    var bloodGroup by remember(existingRejectedProfile) {
        mutableStateOf(existingRejectedProfile?.bloodGroup ?: "O+")
    }
    var profession by remember(existingRejectedProfile) {
        mutableStateOf(existingRejectedProfile?.profession.orEmpty())
    }
    var city by remember(existingRejectedProfile) {
        mutableStateOf(existingRejectedProfile?.city ?: "Comilla")
    }
    var country by remember(existingRejectedProfile) {
        mutableStateOf(existingRejectedProfile?.country ?: "Bangladesh")
    }
    var hidePhone by remember(existingRejectedProfile) {
        mutableStateOf(existingRejectedProfile?.hidePhone ?: false)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LanguageSelectorButtons(
                    currentLanguage = currentLanguage,
                    onSelectLanguage = onSelectLanguage
                )
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.signOut)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = strings.registrationTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = strings.registrationSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.fullNameLabel) },
                leadingIcon = { Icon(Icons.Rounded.AccountCircle, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(strings.phoneLabel) },
                leadingIcon = { Icon(Icons.Rounded.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("reg_phone_input")
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = sscBatch,
                onValueChange = { input -> if (input.length <= 4) sscBatch = input.filter { it.isDigit() } },
                label = { Text(strings.sscBatchLabel) },
                leadingIcon = { Icon(Icons.Rounded.School, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("reg_batch_input")
            )
            Spacer(Modifier.height(14.dp))

            Text(
                text = strings.bloodGroupLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BloodGroups.all.forEach { bg ->
                    FilterChip(
                        selected = bloodGroup == bg,
                        onClick = { bloodGroup = bg },
                        label = { Text(bg, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = profession,
                onValueChange = { profession = it },
                label = { Text(strings.professionLabel) },
                leadingIcon = { Icon(Icons.Rounded.BusinessCenter, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("reg_profession_input")
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(strings.cityLabel) },
                    leadingIcon = { Icon(Icons.Rounded.LocationCity, null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("reg_city_input")
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text(strings.countryLabel) },
                    leadingIcon = { Icon(Icons.Rounded.Public, null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("reg_country_input")
                )
            }

            Spacer(Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.hidePhoneLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.hidePhoneDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = hidePhone,
                        onCheckedChange = { hidePhone = it },
                        modifier = Modifier.testTag("reg_hide_phone_switch")
                    )
                }
            }

            if (validationError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    when {
                        name.trim().length < 2 -> validationError = "Enter your full name (min 2 chars)"
                        phone.trim().length < 6 -> validationError = "Enter a valid phone number"
                        !sscBatch.trim().matches(Regex("^[0-9]{4}$")) -> validationError = "Enter 4-digit SSC batch year (e.g. 2015)"
                        profession.trim().length < 2 -> validationError = "Enter your profession"
                        city.trim().length < 2 -> validationError = "Enter your current city"
                        country.trim().length < 2 -> validationError = "Enter your country"
                        else -> {
                            validationError = null
                            onSubmit(name, phone, hidePhone, sscBatch, bloodGroup, profession, city, country)
                        }
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_registration_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (existingRejectedProfile != null) strings.resubmitProfile else strings.submitRegistration,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun PendingApprovalScreen(
    profile: UserProfile,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onEditRejected: () -> Unit,
    onRequestExit: () -> Unit,
    onSignOut: () -> Unit
) {
    val isRejected = profile.isRejected
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                LanguageSelectorButtons(
                    currentLanguage = currentLanguage,
                    onSelectLanguage = onSelectLanguage
                )
            }

            Spacer(Modifier.weight(1f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRejected) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            ) {
                Icon(
                    imageVector = if (isRejected) Icons.Rounded.ReportProblem else Icons.Rounded.HourglassTop,
                    contentDescription = null,
                    tint = if (isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = if (isRejected) strings.rejectedTitle else strings.pendingTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (isRejected) strings.rejectedDescription else strings.pendingDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${strings.batchPrefix} ${profile.sscBatch} • ${profile.profession}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BloodGroupBadge(bloodGroup = profile.bloodGroup)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${profile.city}, ${profile.country}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (isRejected) {
                Button(
                    onClick = onEditRejected,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(strings.resubmitProfile)
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pending_sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.signOut)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showExitDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pending_exit_app_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
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
}
