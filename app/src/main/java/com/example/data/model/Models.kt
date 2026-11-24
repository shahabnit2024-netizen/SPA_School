package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.concurrent.TimeUnit

object BloodGroups {
    val all = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
}

fun compatibleDonorGroups(recipientGroup: String): List<String> = when (recipientGroup) {
    "O-" -> listOf("O-")
    "O+" -> listOf("O+", "O-")
    "A-" -> listOf("A-", "O-")
    "A+" -> listOf("A+", "A-", "O+", "O-")
    "B-" -> listOf("B-", "O-")
    "B+" -> listOf("B+", "B-", "O+", "O-")
    "AB-" -> listOf("AB-", "A-", "B-", "O-")
    "AB+" -> BloodGroups.all
    else -> listOf(recipientGroup)
}

fun bloodGroupTopicName(bloodGroup: String): String {
    val normalized = bloodGroup.trim()
        .lowercase()
        .replace("+", "_pos")
        .replace("-", "_neg")
    return "spa_blood_$normalized"
}

object UserRoles {
    const val ADMIN = "admin"
    const val MODERATOR = "moderator"
    const val MEMBER = "member"
}

object ApprovalStatus {
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
}

object NoticeCategories {
    const val NOTICE = "notice"
    const val JOB = "job"
    const val NEWS = "news"
}

object TeacherStatus {
    const val CURRENT = "current"
    const val RETIRED = "retired"
    const val LATE = "late"
}

object FundStatus {
    const val ACTIVE = "active"
    const val COMPLETED = "completed"
}

object TransactionType {
    const val IN = "in"
    const val OUT = "out"
}

object BloodRequestStatus {
    const val OPEN = "open"
    const val FULFILLED = "fulfilled"
}

const val MASKED_PHONE_LABEL = "Hidden (Privacy On)"

fun maskPhoneNumber(phone: String): String {
    val trimmed = phone.trim()
    if (trimmed.length <= 4) return "••••"
    val suffix = trimmed.takeLast(3)
    return "••••••$suffix"
}

/**
 * Automatic 90-day blood donation eligibility calculation.
 * Returns true if lastBloodDonateDate is null OR strictly more than 90 days ago.
 */
fun isEligibleBloodDonor(lastDonateDate: Timestamp?, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (lastDonateDate == null) return true
    val diffMillis = nowMillis - lastDonateDate.toDate().time
    val daysSince = TimeUnit.MILLISECONDS.toDays(diffMillis)
    return daysSince > 90L
}

fun daysUntilEligible(lastDonateDate: Timestamp?, nowMillis: Long = System.currentTimeMillis()): Long {
    if (lastDonateDate == null) return 0L
    val diffMillis = nowMillis - lastDonateDate.toDate().time
    val daysSince = TimeUnit.MILLISECONDS.toDays(diffMillis)
    return (91L - daysSince).coerceAtLeast(0L)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val hidePhone: Boolean = false,
    val sscBatch: String = "",
    val bloodGroup: String = "O+",
    val lastBloodDonateDate: Timestamp? = null,
    val profession: String = "",
    val city: String = "",
    val country: String = "Bangladesh",
    val photoUrl: String? = null,
    val role: String = UserRoles.MEMBER,
    val status: String = ApprovalStatus.PENDING,
    val visibleToUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isSuperAdmin: Boolean get() = role == UserRoles.ADMIN && status == ApprovalStatus.APPROVED
    val isBatchModerator: Boolean get() = role == UserRoles.MODERATOR && status == ApprovalStatus.APPROVED
    val isApproved: Boolean get() = status == ApprovalStatus.APPROVED
    val isPending: Boolean get() = status == ApprovalStatus.PENDING
    val isRejected: Boolean get() = status == ApprovalStatus.REJECTED
    val canManagePending: Boolean get() = isSuperAdmin || isBatchModerator
}

@Keep
@IgnoreExtraProperties
data class BatchMetadata(
    @DocumentId val id: String = "",
    val batchYear: String = "",
    val moderatorUids: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Notice(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = NoticeCategories.NOTICE,
    @get:PropertyName("isPinned") @PropertyName("isPinned")
    val isPinned: Boolean = false,
    val audience: String = "all_members",
    val postedBy: String = "",
    val postedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BloodRequest(
    @DocumentId val id: String = "",
    val patientName: String = "",
    val bloodGroup: String = "O+",
    val targetBloodGroups: List<String> = emptyList(),
    val hospitalLocation: String = "",
    val contactPhone: String = "",
    val bagsNeeded: Long = 1L,
    val status: String = BloodRequestStatus.OPEN,
    val postedBy: String = "",
    val postedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val effectiveTargetGroups: List<String>
        get() = targetBloodGroups.ifEmpty { listOf(bloodGroup) }
}

@Keep
@IgnoreExtraProperties
data class AlumniEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val date: String = "",
    val location: String = "",
    val fee: Double = 0.0,
    val description: String = "",
    val visibility: String = "alumni",
    val registeredUsers: List<String> = emptyList(),
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FundCampaign(
    @DocumentId val id: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val collectedAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val status: String = FundStatus.ACTIVE,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FundTransaction(
    @DocumentId val id: String = "",
    val type: String = TransactionType.IN,
    val amount: Double = 0.0,
    val description: String = "",
    val date: String = "",
    val addedBy: String = "",
    val addedByName: String = "",
    val campaignVisibility: String = "alumni",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Teacher(
    @DocumentId val id: String = "",
    val name: String = "",
    val subject: String = "",
    val tenure: String = "",
    val phone: String = "",
    val status: String = TeacherStatus.CURRENT,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppBranding(
    @DocumentId val id: String = "",
    val schoolNameBn: String = "",
    val schoolNameEn: String = "",
    val subtitleBn: String = "",
    val subtitleEn: String = "",
    val logoDataOrUrl: String? = null,
    val visibility: String = "alumni",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppUpdateInfo(
    @DocumentId val id: String = "",
    val versionCode: Long = 1L,
    val versionName: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val mandatory: Boolean = false,
    val visibility: String = "alumni",
    val pushedBy: String = "",
    val pushedByName: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object GalleryStatus {
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
}

@Keep
@IgnoreExtraProperties
data class BatchGalleryPhoto(
    @DocumentId val id: String = "",
    val sscBatch: String = "",
    val caption: String = "",
    val imageDataOrUrl: String = "",
    val status: String = GalleryStatus.PENDING,
    val uploadedBy: String = "",
    val uploadedByName: String = "",
    val reviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
