package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AppBranding
import com.example.ui.components.AppLogoImage
import com.example.ui.components.encodeLogoUriToDataUri
import com.example.ui.i18n.AppStrings

@Composable
fun AdminBrandingSection(
    branding: AppBranding?,
    strings: AppStrings,
    onSaveBranding: (
        schoolNameBn: String,
        schoolNameEn: String,
        subtitleBn: String,
        subtitleEn: String,
        logoDataOrUrl: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var schoolNameBn by remember(branding) {
        mutableStateOf(branding?.schoolNameBn?.ifBlank { "শ্রীপুর প্রসন্ন একাডেমি, কুমিল্লা" } ?: "শ্রীপুর প্রসন্ন একাডেমি, কুমিল্লা")
    }
    var schoolNameEn by remember(branding) {
        mutableStateOf(branding?.schoolNameEn?.ifBlank { "Sreepur Prasanna Academy, Comilla" } ?: "Sreepur Prasanna Academy, Comilla")
    }
    var subtitleBn by remember(branding) {
        mutableStateOf(branding?.subtitleBn?.ifBlank { "প্রাক্তন ছাত্র-ছাত্রী সংসদ" } ?: "প্রাক্তন ছাত্র-ছাত্রী সংসদ")
    }
    var subtitleEn by remember(branding) {
        mutableStateOf(branding?.subtitleEn?.ifBlank { "Alumni Association" } ?: "Alumni Association")
    }
    var logoDataOrUrl by remember(branding) {
        mutableStateOf(branding?.logoDataOrUrl.orEmpty())
    }
    var urlInput by remember(branding) {
        mutableStateOf(
            branding?.logoDataOrUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }.orEmpty()
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = encodeLogoUriToDataUri(context, uri)
            if (encoded != null) {
                logoDataOrUrl = encoded
                urlInput = ""
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                ) {
                    Text(
                        text = strings.appLogoLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    AppLogoImage(
                        logoDataOrUrl = logoDataOrUrl.ifBlank { null },
                        contentDescription = strings.appLogoLabel,
                        size = 96.dp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("pick_logo_button")
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.pickLogoFromGallery, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                logoDataOrUrl = ""
                                urlInput = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("reset_logo_button")
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.resetDefaultLogo, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            if (it.startsWith("http://") || it.startsWith("https://")) {
                                logoDataOrUrl = it
                            } else if (it.isBlank() && !logoDataOrUrl.startsWith("data:image/")) {
                                logoDataOrUrl = ""
                            }
                        },
                        label = { Text(strings.logoUrlLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = schoolNameBn,
                onValueChange = { schoolNameBn = it },
                label = { Text(strings.schoolNameBnLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = schoolNameEn,
                onValueChange = { schoolNameEn = it },
                label = { Text(strings.schoolNameEnLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = subtitleBn,
                onValueChange = { subtitleBn = it },
                label = { Text("উপশিরোনাম (বাংলা)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = subtitleEn,
                onValueChange = { subtitleEn = it },
                label = { Text("Subtitle (English)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = {
                    val effectiveLogo = when {
                        urlInput.trim().startsWith("http://") || urlInput.trim().startsWith("https://") -> urlInput.trim()
                        logoDataOrUrl.isNotBlank() -> logoDataOrUrl.trim()
                        else -> null
                    }
                    onSaveBranding(schoolNameBn, schoolNameEn, subtitleBn, subtitleEn, effectiveLogo)
                },
                enabled = schoolNameBn.trim().length >= 2 && schoolNameEn.trim().length >= 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_branding_button")
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.saveBranding)
            }
        }
    }
}
