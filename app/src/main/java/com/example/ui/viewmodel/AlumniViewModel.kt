package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlumniEvent
import com.example.data.model.AppBranding
import com.example.data.model.AppUpdateInfo
import com.example.data.model.ApprovalStatus
import com.example.data.model.BatchGalleryPhoto
import com.example.data.model.BloodRequest
import com.example.data.model.BloodRequestStatus
import com.example.data.model.FundCampaign
import com.example.data.model.FundTransaction
import com.example.data.model.GalleryStatus
import com.example.data.model.Notice
import com.example.data.model.Teacher
import com.example.data.model.UserProfile
import com.example.data.prefs.LanguagePreferences
import com.example.data.repository.AlumniRepository
import com.example.notifications.BloodAlertWorker
import com.example.notifications.BloodNotificationHelper
import com.example.notifications.NotificationPreferences
import com.example.ui.i18n.AppLanguage
import com.example.ui.state.UiState
import com.example.ui.state.unwrapFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AlumniViewModel(
    private val appContext: Context,
    private val repository: AlumniRepository,
    private val languagePreferences: LanguagePreferences,
    private val notificationPreferences: NotificationPreferences,
    private val currentUserId: String
) : ViewModel() {

    val language: StateFlow<AppLanguage> = languagePreferences.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppLanguage.BENGALI
        )

    val bloodAlertsEnabled: StateFlow<Boolean> = notificationPreferences.alertsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = true
        )

    val customAlertGroups: StateFlow<Set<String>> = notificationPreferences.customGroupsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            languagePreferences.setLanguage(lang)
        }
    }

    fun toggleLanguage() {
        val next = if (language.value == AppLanguage.BENGALI) AppLanguage.ENGLISH else AppLanguage.BENGALI
        setLanguage(next)
    }

    fun setBloodAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setAlertsEnabled(enabled)
        }
    }

    fun toggleAlertBloodGroup(group: String, fallbackProfileGroup: String) {
        viewModelScope.launch {
            val current = customAlertGroups.value.ifEmpty { setOf(fallbackProfileGroup) }.toMutableSet()
            if (current.contains(group)) {
                if (current.size > 1) current.remove(group)
            } else {
                current.add(group)
            }
            notificationPreferences.setCustomGroups(current)
        }
    }

    val ownProfileState: StateFlow<UiState<UserProfile?>> = repository.observeOwnProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing own profile", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val ownPrivatePhone: StateFlow<String?> = repository.observeOwnPrivatePhone(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    private inline fun <T> approvedStream(
        crossinline block: () -> Flow<List<T>>,
        label: String
    ): StateFlow<UiState<List<T>>> {
        return ownProfileState.flatMapLatest { profileState ->
            val profile = (profileState as? UiState.Success)?.data
            if (profile != null && profile.isApproved) {
                block()
                    .map<List<T>, UiState<List<T>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error)
                        Log.w(TAG, "Error observing $label", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load $label"))
                    }
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )
    }

    val approvedAlumniState: StateFlow<UiState<List<UserProfile>>> =
        approvedStream({ repository.observeApprovedAlumni() }, "alumni")

    val noticesState: StateFlow<UiState<List<Notice>>> =
        approvedStream({ repository.observeNotices() }, "notices")

    val bloodRequestsState: StateFlow<UiState<List<BloodRequest>>> =
        approvedStream({ repository.observeBloodRequests() }, "blood_requests")

    val eventsState: StateFlow<UiState<List<AlumniEvent>>> =
        approvedStream({ repository.observeEvents() }, "events")

    val fundsState: StateFlow<UiState<List<FundCampaign>>> =
        approvedStream({ repository.observeFunds() }, "funds")

    val teachersState: StateFlow<UiState<List<Teacher>>> =
        approvedStream({ repository.observeTeachers() }, "teachers")

    val approvedGalleryState: StateFlow<UiState<List<BatchGalleryPhoto>>> =
        approvedStream({ repository.observeApprovedBatchGallery() }, "batch_gallery")

    val myGalleryUploadsState: StateFlow<UiState<List<BatchGalleryPhoto>>> =
        approvedStream({ repository.observeMyBatchGalleryUploads() }, "my_batch_gallery")

    val pendingGalleryState: StateFlow<UiState<List<BatchGalleryPhoto>>> =
        ownProfileState.flatMapLatest { profileState ->
            val profile = (profileState as? UiState.Success)?.data
            if (profile != null && profile.isSuperAdmin) {
                repository.observePendingBatchGallery()
                    .map<List<BatchGalleryPhoto>, UiState<List<BatchGalleryPhoto>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error)
                        Log.w(TAG, "Error observing pending batch gallery", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load pending gallery photos"))
                    }
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val brandingState: StateFlow<AppBranding?> = repository.observeAppBranding()
        .catch { error ->
            unwrapFirestoreError(error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val appUpdateState: StateFlow<AppUpdateInfo?> = repository.observeAppUpdateInfo()
        .catch { error ->
            unwrapFirestoreError(error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val pendingApprovalsState: StateFlow<UiState<List<UserProfile>>> =
        ownProfileState.flatMapLatest { profileState ->
            val profile = (profileState as? UiState.Success)?.data
            if (profile != null && profile.canManagePending) {
                repository.observePendingApprovals(profile)
                    .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error)
                        Log.w(TAG, "Error observing pending approvals", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load pending approvals"))
                    }
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedCampaignId = MutableStateFlow<String?>(null)
    val ledgerTransactionsState: StateFlow<UiState<List<FundTransaction>>> =
        _selectedCampaignId.flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeFundTransactions(id)
                    .map<List<FundTransaction>, UiState<List<FundTransaction>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error)
                        Log.w(TAG, "Error observing ledger", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load ledger"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun selectCampaignForLedger(campaignId: String) {
        _selectedCampaignId.value = campaignId
    }

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(ownProfileState, bloodAlertsEnabled, customAlertGroups) { profileState, enabled, customGroups ->
                val profile = (profileState as? UiState.Success)?.data
                Triple(profile, enabled, customGroups)
            }.collect { (profile, enabled, customGroups) ->
                if (profile != null && profile.isApproved) {
                    val activeGroups = customGroups.ifEmpty { setOf(profile.bloodGroup) }
                    BloodNotificationHelper.ensureChannel(appContext)
                    BloodNotificationHelper.syncBloodGroupTopics(activeGroups, enabled)
                    if (enabled) {
                        BloodAlertWorker.schedulePeriodicSync(appContext)
                    } else {
                        BloodAlertWorker.cancelSync(appContext)
                    }
                    try {
                        Firebase.messaging.token.addOnSuccessListener { token ->
                            viewModelScope.launch {
                                repository.saveNotificationPreferences(
                                    alertsEnabled = enabled,
                                    subscribedGroups = activeGroups.toList(),
                                    fcmToken = token
                                )
                            }
                        }.addOnFailureListener {
                            viewModelScope.launch {
                                repository.saveNotificationPreferences(
                                    alertsEnabled = enabled,
                                    subscribedGroups = activeGroups.toList(),
                                    fcmToken = null
                                )
                            }
                        }
                    } catch (_: Exception) {
                        repository.saveNotificationPreferences(
                            alertsEnabled = enabled,
                            subscribedGroups = activeGroups.toList(),
                            fcmToken = null
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                ownProfileState,
                bloodRequestsState,
                bloodAlertsEnabled,
                customAlertGroups,
                language
            ) { profileState, requestsState, enabled, customGroups, lang ->
                val profile = (profileState as? UiState.Success)?.data
                val requests = (requestsState as? UiState.Success)?.data.orEmpty()
                listOf(profile, requests, enabled, customGroups, lang)
            }.collect { values ->
                val profile = values[0] as? UserProfile ?: return@collect
                @Suppress("UNCHECKED_CAST")
                val requests = values[1] as List<BloodRequest>
                val enabled = values[2] as Boolean
                @Suppress("UNCHECKED_CAST")
                val customGroups = values[3] as Set<String>
                val lang = values[4] as AppLanguage

                if (!profile.isApproved || !enabled || requests.isEmpty()) return@collect

                val activeGroups = customGroups.ifEmpty { setOf(profile.bloodGroup) }
                val baseline = notificationPreferences.getOrInitBaselineMillis()
                val notifiedIds = notificationPreferences.getNotifiedRequestIds()
                val newlyNotified = mutableListOf<String>()

                for (req in requests) {
                    val createdMillis = req.createdAt?.toDate()?.time ?: 0L
                    val isNew = req.status == BloodRequestStatus.OPEN &&
                        req.id.isNotBlank() &&
                        req.id !in notifiedIds &&
                        createdMillis >= baseline &&
                        req.postedBy != currentUserId
                    val matchesGroup = req.effectiveTargetGroups.any { it in activeGroups }

                    if (isNew && matchesGroup) {
                        BloodNotificationHelper.showUrgentBloodNotification(
                            context = appContext,
                            requestId = req.id,
                            patientName = req.patientName,
                            bloodGroup = req.bloodGroup,
                            hospitalLocation = req.hospitalLocation,
                            contactPhone = req.contactPhone,
                            bagsNeeded = req.bagsNeeded,
                            isBengali = lang == AppLanguage.BENGALI
                        )
                        newlyNotified.add(req.id)
                    }
                }
                notificationPreferences.markRequestsNotified(newlyNotified)
            }
        }
    }

    private fun runMutation(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = block()
            _isMutating.value = false
            result.onSuccess {
                if (successMessage.isNotBlank()) _messages.emit(successMessage)
            }.onFailure { error ->
                _messages.emit(error.localizedMessage ?: "Operation failed")
            }
        }
    }

    fun registerProfile(
        name: String,
        phone: String,
        hidePhone: Boolean,
        sscBatch: String,
        bloodGroup: String,
        profession: String,
        city: String,
        country: String
    ) = runMutation("Registration completed") {
        repository.registerProfile(name, phone, hidePhone, sscBatch, bloodGroup, profession, city, country)
    }

    fun activatePendingOwnProfile() {
        viewModelScope.launch {
            repository.activatePendingOwnProfile()
        }
    }

    fun updateProfile(
        existing: UserProfile,
        name: String,
        rawPhone: String,
        hidePhone: Boolean,
        sscBatch: String,
        bloodGroup: String,
        profession: String,
        city: String,
        country: String,
        resubmitIfRejected: Boolean = false
    ) = runMutation("Profile saved") {
        repository.updateOwnProfile(
            existing, name, rawPhone, hidePhone, sscBatch, bloodGroup, profession, city, country, resubmitIfRejected
        )
    }

    fun updatePrivacyAndDonation(
        rawPhone: String?,
        hidePhone: Boolean,
        lastBloodDonateDate: Timestamp?
    ) = runMutation("Preferences updated") {
        repository.updatePrivacyAndDonation(rawPhone, hidePhone, lastBloodDonateDate)
    }

    fun approveUser(targetUid: String) = runMutation("Member approved") {
        repository.updateApprovalStatus(targetUid, ApprovalStatus.APPROVED)
    }

    fun rejectUser(targetUid: String) = runMutation("Registration rejected") {
        repository.updateApprovalStatus(targetUid, ApprovalStatus.REJECTED)
    }

    fun setBatchModerator(targetUser: UserProfile, makeModerator: Boolean) = runMutation(
        if (makeModerator) "Assigned as Batch Moderator" else "Moderator role removed"
    ) {
        repository.assignBatchModerator(targetUser, makeModerator)
    }

    fun createNotice(
        title: String,
        description: String,
        category: String,
        isPinned: Boolean,
        postedByName: String
    ) = runMutation("Published successfully") {
        repository.createNotice(title, description, category, isPinned, postedByName)
    }

    fun deleteNotice(noticeId: String) = runMutation("Notice deleted") {
        repository.deleteNotice(noticeId)
    }

    fun createBloodRequest(
        patientName: String,
        bloodGroup: String,
        targetBloodGroups: List<String>,
        hospitalLocation: String,
        contactPhone: String,
        bagsNeeded: Int,
        postedByName: String
    ) = runMutation(
        "Urgent blood request posted • Alerts sent to ${targetBloodGroups.ifEmpty { listOf(bloodGroup) }.joinToString(", ")}"
    ) {
        repository.createBloodRequest(
            patientName, bloodGroup, targetBloodGroups, hospitalLocation, contactPhone, bagsNeeded, postedByName
        )
    }

    fun markBloodRequestFulfilled(reqId: String) = runMutation("Marked as fulfilled") {
        repository.markBloodRequestFulfilled(reqId)
    }

    fun createEvent(
        title: String,
        date: String,
        location: String,
        fee: Double,
        description: String
    ) = runMutation("Event created") {
        repository.createEvent(title, date, location, fee, description)
    }

    fun toggleEventRegistration(event: AlumniEvent) = runMutation("") {
        val going = event.registeredUsers.contains(currentUserId)
        repository.toggleEventRegistration(event.id, going)
    }

    fun createFundCampaign(title: String, targetAmount: Double) = runMutation("Fund campaign created") {
        repository.createFundCampaign(title, targetAmount)
    }

    fun addFundTransaction(
        campaignId: String,
        type: String,
        amount: Double,
        description: String,
        date: String,
        addedByName: String
    ) = runMutation("Ledger transaction added") {
        repository.addFundTransaction(campaignId, type, amount, description, date, addedByName)
    }

    fun createTeacher(
        name: String,
        subject: String,
        tenure: String,
        phone: String,
        status: String
    ) = runMutation("Teacher added") {
        repository.createTeacher(name, subject, tenure, phone, status)
    }

    fun deleteTeacher(teacherId: String) = runMutation("Teacher removed") {
        repository.deleteTeacher(teacherId)
    }

    fun updateTeacher(
        teacherId: String,
        name: String,
        subject: String,
        tenure: String,
        phone: String,
        status: String
    ) = runMutation("Teacher updated") {
        repository.updateTeacher(teacherId, name, subject, tenure, phone, status)
    }

    fun updateAppBranding(
        schoolNameBn: String,
        schoolNameEn: String,
        subtitleBn: String,
        subtitleEn: String,
        logoDataOrUrl: String?
    ) = runMutation("Branding & logo saved") {
        repository.updateAppBranding(schoolNameBn, schoolNameEn, subtitleBn, subtitleEn, logoDataOrUrl)
    }

    fun pushAppUpdate(
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String,
        mandatory: Boolean,
        pushedByName: String
    ) = runMutation("App update notification pushed") {
        repository.pushAppUpdate(versionCode, versionName, releaseNotes, downloadUrl, mandatory, pushedByName)
    }

    fun clearAppUpdate() = runMutation("App update notice cleared") {
        repository.clearAppUpdate()
    }

    fun adminUpdateUserProfile(
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
    ) = runMutation("Member updated") {
        repository.adminUpdateUserProfile(
            targetUid, name, phone, hidePhone, sscBatch, bloodGroup, profession, city, country, role, status
        )
    }

    fun deleteUser(targetUid: String) = runMutation("Member removed") {
        repository.deleteUser(targetUid)
    }

    fun updateNotice(
        noticeId: String,
        title: String,
        description: String,
        category: String,
        isPinned: Boolean
    ) = runMutation("Post updated") {
        repository.updateNotice(noticeId, title, description, category, isPinned)
    }

    fun updateBloodRequest(
        reqId: String,
        patientName: String,
        bloodGroup: String,
        hospitalLocation: String,
        contactPhone: String,
        bagsNeeded: Int,
        status: String
    ) = runMutation("Blood request updated") {
        repository.updateBloodRequest(reqId, patientName, bloodGroup, hospitalLocation, contactPhone, bagsNeeded, status)
    }

    fun deleteBloodRequest(reqId: String) = runMutation("Blood request deleted") {
        repository.deleteBloodRequest(reqId)
    }

    fun updateEvent(
        eventId: String,
        title: String,
        date: String,
        location: String,
        fee: Double,
        description: String
    ) = runMutation("Event updated") {
        repository.updateEvent(eventId, title, date, location, fee, description)
    }

    fun deleteEvent(eventId: String) = runMutation("Event deleted") {
        repository.deleteEvent(eventId)
    }

    fun updateFundCampaign(
        campaignId: String,
        title: String,
        targetAmount: Double,
        status: String
    ) = runMutation("Campaign updated") {
        repository.updateFundCampaign(campaignId, title, targetAmount, status)
    }

    fun deleteFundCampaign(campaignId: String) = runMutation("Campaign deleted") {
        repository.deleteFundCampaign(campaignId)
    }

    fun deleteFundTransaction(campaignId: String, tx: FundTransaction) = runMutation("Transaction deleted") {
        repository.deleteFundTransaction(campaignId, tx)
    }

    fun updateOwnProfilePhoto(photoDataOrUrl: String) = runMutation("Profile photo updated") {
        repository.updateOwnProfilePhoto(photoDataOrUrl)
    }

    fun uploadBatchGalleryPhoto(
        sscBatch: String,
        caption: String,
        imageDataOrUrl: String,
        uploadedByName: String,
        isSuperAdmin: Boolean
    ) = runMutation(
        if (isSuperAdmin) "Batch photo published to gallery"
        else "Submitted! Visible after admin approval"
    ) {
        repository.uploadBatchGalleryPhoto(
            sscBatch = sscBatch,
            caption = caption,
            imageDataOrUrl = imageDataOrUrl,
            uploadedByName = uploadedByName,
            autoApproveIfAdmin = isSuperAdmin
        )
    }

    fun approveBatchGalleryPhoto(photoId: String) = runMutation("Batch photo approved") {
        repository.reviewBatchGalleryPhoto(photoId, GalleryStatus.APPROVED)
    }

    fun rejectBatchGalleryPhoto(photoId: String) = runMutation("Batch photo rejected") {
        repository.reviewBatchGalleryPhoto(photoId, GalleryStatus.REJECTED)
    }

    fun deleteBatchGalleryPhoto(photoId: String) = runMutation("Photo deleted") {
        repository.deleteBatchGalleryPhoto(photoId)
    }

    private companion object {
        const val TAG = "AlumniVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
