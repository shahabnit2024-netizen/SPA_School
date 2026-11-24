package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppBranding
import com.example.ui.components.AppLogoImage
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily

private data class AboutFactItem(
    val icon: ImageVector,
    val labelBn: String,
    val labelEn: String,
    val valueBn: String,
    val valueEn: String
)

private val academyFacts = listOf(
    AboutFactItem(
        icon = Icons.Rounded.Badge,
        labelBn = "EIIN নম্বর",
        labelEn = "EIIN Number",
        valueBn = "১০৫৪৩৪ (105434)",
        valueEn = "105434 (১০৫৪৩৪)"
    ),
    AboutFactItem(
        icon = Icons.Rounded.CalendarMonth,
        labelBn = "প্রতিষ্ঠাকাল",
        labelEn = "Established",
        valueBn = "০১ জানুয়ারি, ১৯৪২",
        valueEn = "01 January, 1942"
    ),
    AboutFactItem(
        icon = Icons.Rounded.Verified,
        labelBn = "স্বীকৃতি ও তারিখ",
        labelEn = "Recognition",
        valueBn = "স্বীকৃত (০১ জানুয়ারি, ১৯৫১)",
        valueEn = "Recognized (01 January, 1951)"
    ),
    AboutFactItem(
        icon = Icons.Rounded.Category,
        labelBn = "স্তর ও শ্রেণি",
        labelEn = "Level & Type",
        valueBn = "মাধ্যমিক (Secondary)",
        valueEn = "Secondary School"
    ),
    AboutFactItem(
        icon = Icons.Rounded.AccountBalance,
        labelBn = "এমপিও (MPO) নিবন্ধন",
        labelEn = "MPO Status & Reg.",
        valueBn = "হ্যাঁ • ৮০৯১১১৩০১",
        valueEn = "Yes • 809111301"
    ),
    AboutFactItem(
        icon = Icons.Rounded.AccountBalance,
        labelBn = "শিক্ষা বোর্ড",
        labelEn = "Education Board",
        valueBn = "কুমিল্লা বোর্ড",
        valueEn = "Comilla Board"
    ),
    AboutFactItem(
        icon = Icons.Rounded.MenuBook,
        labelBn = "চালুকৃত বিভাগসমূহ",
        labelEn = "Academic Streams",
        valueBn = "বিজ্ঞান, মানবিক ও ব্যবসায় শিক্ষা",
        valueEn = "Science, Humanities & Business Studies"
    ),
    AboutFactItem(
        icon = Icons.Rounded.Diversity3,
        labelBn = "শিক্ষার্থীর ধরন",
        labelEn = "Gender / Mode",
        valueBn = "সম্মিলিত বা সহশিক্ষা (Combined)",
        valueEn = "Combined (Co-education)"
    ),
    AboutFactItem(
        icon = Icons.Rounded.Schedule,
        labelBn = "শিফট",
        labelEn = "Shift",
        valueBn = "দিবা (Day)",
        valueEn = "Day Shift"
    ),
    AboutFactItem(
        icon = Icons.Rounded.Groups,
        labelBn = "পরিচালনা পর্ষদ",
        labelEn = "Management",
        valueBn = "ম্যানেজিং কমিটি (Managing)",
        valueEn = "Managing Committee"
    ),
    AboutFactItem(
        icon = Icons.Rounded.LocationOn,
        labelBn = "অঞ্চল ও ভূপ্রকৃতি",
        labelEn = "Region & Terrain",
        valueBn = "গ্রামীণ (Grameen) • সমতল ভূমি (Plain Land)",
        valueEn = "Rural (Grameen) • Plain Land"
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    branding: AppBranding?,
    strings: AppStrings,
    onBack: () -> Unit
) {
    val isBn = strings.language == AppLanguage.BENGALI
    val resolvedSchoolName = if (isBn) {
        branding?.schoolNameBn?.ifBlank { strings.schoolFullName } ?: strings.schoolFullName
    } else {
        branding?.schoolNameEn?.ifBlank { strings.schoolFullName } ?: strings.schoolFullName
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text(strings.aboutTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("about_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(196.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_campus_banner),
                            contentDescription = resolvedSchoolName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.2f),
                                            Color(0xFF022C22).copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AppLogoImage(
                                    logoDataOrUrl = branding?.logoDataOrUrl,
                                    contentDescription = strings.appLogoLabel,
                                    size = 54.dp
                                )
                                StatusPill(
                                    text = "EIIN: 105434",
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = resolvedSchoolName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = strings.aboutCardSubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("about_narrative_card")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        if (isBn) {
                            Text(
                                text = "প্রাতিষ্ঠানিক পরিচিতি (বাংলা)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.aboutOfficialTextBn,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Institutional Profile (English)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.aboutEnglishSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Institutional Profile (English)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.aboutEnglishSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "প্রাতিষ্ঠানিক পরিচিতি (বাংলা)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.aboutOfficialTextBn,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.aboutKeyFactsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        academyFacts.forEachIndexed { index, fact ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = fact.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) fact.labelBn else fact.labelEn,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isBn) fact.valueBn else fact.valueEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (index < academyFacts.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusPill(
                        text = "EIIN: 105434",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    StatusPill(
                        text = "MPO: 809111301",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    StatusPill(
                        text = "EST: 1942",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("about_developer_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Code,
                                    contentDescription = "App Developer",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBn) "অ্যাপ ডেভেলপার (App Developer)" else "App Developer",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Md. Sahab Uddin SSC Batch of 2011",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
