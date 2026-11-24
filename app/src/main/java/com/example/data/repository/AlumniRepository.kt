package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.AlumniEvent
import com.example.data.model.AppBranding
import com.example.data.model.AppUpdateInfo
import com.example.data.model.ApprovalStatus
import com.example.data.model.BatchGalleryPhoto
import com.example.data.model.BatchMetadata
import com.example.data.model.BloodGroups
import com.example.data.model.BloodRequest
import com.example.data.model.BloodRequestStatus
import com.example.data.model.FundCampaign
import com.example.data.model.FundStatus
import com.example.data.model.FundTransaction
import com.example.data.model.GalleryStatus
import com.example.data.model.Notice
import com.example.data.model.NoticeCategories
import com.example.data.model.Teacher
import com.example.data.model.TeacherStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.UserRoles
import com.example.data.model.maskPhoneNumber
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val BOOTSTRAP_ADMIN_EMAILS = setOf(
    "sahabuddinsagor@gmail.com",
    "shahab.nit2024@gmail.com"
)

class AlumniRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun isBootstrapAdmin(): Boolean {
        val user = auth.currentUser
        val email = user?.email?.lowercase()?.trim()
        return user != null && user.isEmailVerified && email in BOOTSTRAP_ADMIN_EMAILS
    }

    private fun <T> Flow<T>.logStreamErrors(op: OperationType, path: String, uid: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    fun observeOwnProfile(uid: String): Flow<UserProfile?> = flow {
        if (uid.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logStreamErrors(OperationType.GET, path, uid)
        )
    }

    fun observeOwnPrivatePhone(uid: String): Flow<String?> = flow {
        if (uid.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$uid/private/contact"
        emitAll(
            db.collection("users").document(uid).collection("private").document("contact")
                .snapshots()
                .map { snapshot -> snapshot.getString("phone") }
                .logStreamErrors(OperationType.GET, path, uid)
        )
    }

    fun observeAppBranding(): Flow<AppBranding?> = flow {
        val uid = requireUserId()
        val path = "app_config/branding"
        emitAll(
            db.collection("app_config").document("branding")
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(AppBranding::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logStreamErrors(OperationType.GET, path, uid)
        )
    }

    fun observeAppUpdateInfo(): Flow<AppUpdateInfo?> = flow {
        val uid = requireUserId()
        val path = "app_config/update_info"
        emitAll(
            db.collection("app_config").document("update_info")
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(AppUpdateInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logStreamErrors(OperationType.GET, path, uid)
        )
    }

    fun observeApprovedAlumni(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .whereEqualTo("status", ApprovalStatus.APPROVED)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<UserProfile> { it.sscBatch }.thenBy { it.name })
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePendingApprovals(viewerProfile: UserProfile): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        val query = if (isBootstrapAdmin()) {
            db.collection("users").whereEqualTo("status", ApprovalStatus.PENDING)
        } else {
            db.collection("users")
                .whereEqualTo("sscBatch", viewerProfile.sscBatch)
                .whereEqualTo("status", ApprovalStatus.PENDING)
                .whereArrayContains("visibleToUids", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeNotices(): Flow<List<Notice>> = flow {
        val uid = requireUserId()
        val path = "notices"
        emitAll(
            db.collection("notices")
                .whereEqualTo("audience", "all_members")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Notice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeBloodRequests(): Flow<List<BloodRequest>> = flow {
        val uid = requireUserId()
        val path = "blood_requests"
        emitAll(
            db.collection("blood_requests")
                .whereIn("status", listOf(BloodRequestStatus.OPEN, BloodRequestStatus.FULFILLED))
                .snapshots()
                .map { snap ->
                    snap.toObjects(BloodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<BloodRequest> { if (it.status == BloodRequestStatus.OPEN) 0 else 1 }
                                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeEvents(): Flow<List<AlumniEvent>> = flow {
        val uid = requireUserId()
        val path = "events"
        emitAll(
            db.collection("events")
                .whereEqualTo("visibility", "alumni")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AlumniEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeFunds(): Flow<List<FundCampaign>> = flow {
        val uid = requireUserId()
        val path = "funds"
        emitAll(
            db.collection("funds")
                .whereIn("status", listOf(FundStatus.ACTIVE, FundStatus.COMPLETED))
                .snapshots()
                .map { snap ->
                    snap.toObjects(FundCampaign::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeFundTransactions(campaignId: String): Flow<List<FundTransaction>> = flow {
        val uid = requireUserId()
        require(campaignId.isNotBlank()) { "Campaign ID must not be blank" }
        val path = "funds/$campaignId/transactions"
        emitAll(
            db.collection("funds").document(campaignId).collection("transactions")
                .whereEqualTo("campaignVisibility", "alumni")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FundTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeTeachers(): Flow<List<Teacher>> = flow {
        val uid = requireUserId()
        val path = "teachers"
        emitAll(
            db.collection("teachers")
                .whereIn("status", listOf(TeacherStatus.CURRENT, TeacherStatus.RETIRED, TeacherStatus.LATE))
                .snapshots()
                .map { snap ->
                    snap.toObjects(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<Teacher> {
                                when (it.status) {
                                    TeacherStatus.CURRENT -> 0
                                    TeacherStatus.RETIRED -> 1
                                    else -> 2
                                }
                            }.thenBy { it.name }
                        )
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun registerProfile(
        name: String,
        phone: String,
        hidePhone: Boolean,
        sscBatch: String,
        bloodGroup: String,
        profession: String,
        city: String,
        country: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("User must be signed in with Google before registering.")
            val uid = user.uid
            path = "users/$uid"
            val trimmedName = name.trim()
            val trimmedPhone = phone.trim()
            val trimmedBatch = sscBatch.trim()
            val trimmedProfession = profession.trim()
            val trimmedCity = city.trim()
            val trimmedCountry = country.trim()

            require(trimmedName.length in 2..80) { "Full name must be 2 to 80 characters" }
            require(trimmedPhone.length in 6..30) { "Phone number must be 6 to 30 characters" }
            require(trimmedBatch.matches(Regex("^[0-9]{4}$"))) { "SSC batch must be a 4-digit year" }
            require(trimmedProfession.length in 2..80) { "Profession must be 2 to 80 characters" }
            require(trimmedCity.length in 2..60) { "City must be 2 to 60 characters" }
            require(trimmedCountry.length in 2..60) { "Country must be 2 to 60 characters" }

            val batchDoc = try {
                db.collection("batches").document(trimmedBatch).get().await()
                    .toObject(BatchMetadata::class.java)
            } catch (_: Exception) {
                null
            }
            val visibleUids = (listOf(uid) + (batchDoc?.moderatorUids ?: emptyList())).distinct().take(20)

            val isBootstrap = isBootstrapAdmin()
            val assignedRole = if (isBootstrap) UserRoles.ADMIN else UserRoles.MEMBER
            val assignedStatus = ApprovalStatus.APPROVED
            val publicPhone = if (hidePhone) maskPhoneNumber(trimmedPhone) else trimmedPhone

            val payload = mapOf(
                "uid" to uid,
                "email" to (user.email ?: ""),
                "name" to trimmedName,
                "phone" to publicPhone,
                "hidePhone" to hidePhone,
                "sscBatch" to trimmedBatch,
                "bloodGroup" to bloodGroup,
                "profession" to trimmedProfession,
                "city" to trimmedCity,
                "country" to trimmedCountry,
                "photoUrl" to user.photoUrl?.toString(),
                "role" to assignedRole,
                "status" to assignedStatus,
                "visibleToUids" to visibleUids,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val privatePayload = mapOf(
                "uid" to uid,
                "phone" to trimmedPhone,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(db.collection("users").document(uid), payload)
            batch.set(db.collection("users").document(uid).collection("private").document("contact"), privatePayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOwnProfile(
        existingProfile: UserProfile,
        name: String,
        rawPhone: String,
        hidePhone: Boolean,
        sscBatch: String,
        bloodGroup: String,
        profession: String,
        city: String,
        country: String,
        resubmitIfRejected: Boolean = false
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val trimmedName = name.trim()
            val trimmedPhone = rawPhone.trim()
            val trimmedBatch = sscBatch.trim()
            val trimmedProfession = profession.trim()
            val trimmedCity = city.trim()
            val trimmedCountry = country.trim()

            require(trimmedName.length in 2..80) { "Full name must be 2 to 80 characters" }
            require(trimmedPhone.length in 6..30) { "Phone number must be 6 to 30 characters" }
            require(trimmedBatch.matches(Regex("^[0-9]{4}$"))) { "SSC batch must be a 4-digit year" }
            require(trimmedProfession.length in 2..80) { "Profession must be 2 to 80 characters" }
            require(trimmedCity.length in 2..60) { "City must be 2 to 60 characters" }
            require(trimmedCountry.length in 2..60) { "Country must be 2 to 60 characters" }

            val publicPhone = if (hidePhone) maskPhoneNumber(trimmedPhone) else trimmedPhone
            val batchDoc = try {
                db.collection("batches").document(trimmedBatch).get().await()
                    .toObject(BatchMetadata::class.java)
            } catch (_: Exception) {
                null
            }
            val visibleUids = (listOf(uid) + (batchDoc?.moderatorUids ?: emptyList())).distinct().take(20)

            val updates = mutableMapOf<String, Any>(
                "name" to trimmedName,
                "phone" to publicPhone,
                "hidePhone" to hidePhone,
                "sscBatch" to trimmedBatch,
                "bloodGroup" to bloodGroup,
                "profession" to trimmedProfession,
                "city" to trimmedCity,
                "country" to trimmedCountry,
                "visibleToUids" to visibleUids,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (resubmitIfRejected && existingProfile.isRejected) {
                updates["status"] = ApprovalStatus.APPROVED
            }

            val privatePayload = mapOf(
                "uid" to uid,
                "phone" to trimmedPhone,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val writeBatch = db.batch()
            writeBatch.update(db.collection("users").document(uid), updates)
            writeBatch.set(db.collection("users").document(uid).collection("private").document("contact"), privatePayload)
            writeBatch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun activatePendingOwnProfile(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "status" to ApprovalStatus.APPROVED,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacyAndDonation(
        rawPhone: String?,
        hidePhone: Boolean,
        lastBloodDonateDate: Timestamp?
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val updates = mutableMapOf<String, Any>(
                "hidePhone" to hidePhone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!rawPhone.isNullOrBlank()) {
                updates["phone"] = if (hidePhone) maskPhoneNumber(rawPhone) else rawPhone.trim()
            }
            if (lastBloodDonateDate != null) {
                require(lastBloodDonateDate.toDate().time <= System.currentTimeMillis() + 60_000L) {
                    "Donation date cannot be in the future"
                }
                updates["lastBloodDonateDate"] = lastBloodDonateDate
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateApprovalStatus(targetUid: String, newStatus: String): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank()) { "Target user ID required" }
            require(newStatus in listOf(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED)) {
                "Invalid status"
            }
            db.collection("users").document(targetUid).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun assignBatchModerator(targetUser: UserProfile, makeModerator: Boolean): Result<Unit> {
        val path = "users/${targetUser.uid}"
        return try {
            requireUserId()
            require(targetUser.uid.isNotBlank()) { "Target UID required" }
            val batchYear = targetUser.sscBatch
            val newRole = if (makeModerator) UserRoles.MODERATOR else UserRoles.MEMBER

            val batchDocRef = db.collection("batches").document(batchYear)
            val existingBatch = batchDocRef.get().await().toObject(BatchMetadata::class.java)
            val currentMods = existingBatch?.moderatorUids?.toMutableList() ?: mutableListOf()
            if (makeModerator) {
                if (!currentMods.contains(targetUser.uid)) currentMods.add(targetUser.uid)
            } else {
                currentMods.remove(targetUser.uid)
            }

            val writeBatch = db.batch()
            writeBatch.update(
                db.collection("users").document(targetUser.uid),
                mapOf(
                    "role" to newRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            writeBatch.set(
                batchDocRef,
                mapOf(
                    "batchYear" to batchYear,
                    "moderatorUids" to currentMods.take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            writeBatch.commit().await()

            if (makeModerator) {
                val pendingInBatch = db.collection("users")
                    .whereEqualTo("sscBatch", batchYear)
                    .whereEqualTo("status", ApprovalStatus.PENDING)
                    .get()
                    .await()
                if (!pendingInBatch.isEmpty) {
                    val syncBatch = db.batch()
                    for (doc in pendingInBatch.documents) {
                        syncBatch.update(
                            doc.reference,
                            mapOf(
                                "visibleToUids" to FieldValue.arrayUnion(targetUser.uid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    syncBatch.commit().await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createNotice(
        title: String,
        description: String,
        category: String,
        isPinned: Boolean,
        postedByName: String
    ): Result<Unit> {
        val path = "notices"
        return try {
            val uid = requireUserId()
            require(title.trim().length in 2..140) { "Title must be 2 to 140 characters" }
            require(description.trim().length in 2..3000) { "Description must be 2 to 3000 characters" }
            require(category in listOf(NoticeCategories.NOTICE, NoticeCategories.JOB, NoticeCategories.NEWS))
            val docRef = db.collection("notices").document()
            docRef.set(
                mapOf(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "category" to category,
                    "isPinned" to isPinned,
                    "audience" to "all_members",
                    "postedBy" to uid,
                    "postedByName" to postedByName.take(80),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotice(noticeId: String): Result<Unit> {
        val path = "notices/$noticeId"
        return try {
            requireUserId()
            require(noticeId.isNotBlank())
            db.collection("notices").document(noticeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createBloodRequest(
        patientName: String,
        bloodGroup: String,
        targetBloodGroups: List<String>,
        hospitalLocation: String,
        contactPhone: String,
        bagsNeeded: Int,
        postedByName: String
    ): Result<Unit> {
        val path = "blood_requests"
        return try {
            val uid = requireUserId()
            require(patientName.trim().length in 2..80) { "Patient name must be 2 to 80 characters" }
            require(hospitalLocation.trim().length in 2..160) { "Hospital location must be 2 to 160 characters" }
            require(contactPhone.trim().length in 6..30) { "Contact phone must be 6 to 30 characters" }
            require(bagsNeeded in 1..20) { "Bags needed must be between 1 and 20" }
            val resolvedTargets = targetBloodGroups
                .filter { it in BloodGroups.all }
                .distinct()
                .ifEmpty { listOf(bloodGroup) }
                .take(8)
            val docRef = db.collection("blood_requests").document()
            docRef.set(
                mapOf(
                    "patientName" to patientName.trim(),
                    "bloodGroup" to bloodGroup,
                    "targetBloodGroups" to resolvedTargets,
                    "hospitalLocation" to hospitalLocation.trim(),
                    "contactPhone" to contactPhone.trim(),
                    "bagsNeeded" to bagsNeeded,
                    "status" to BloodRequestStatus.OPEN,
                    "postedBy" to uid,
                    "postedByName" to postedByName.take(80),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markBloodRequestFulfilled(reqId: String): Result<Unit> {
        val path = "blood_requests/$reqId"
        return try {
            requireUserId()
            require(reqId.isNotBlank())
            db.collection("blood_requests").document(reqId)
                .update("status", BloodRequestStatus.FULFILLED)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createEvent(
        title: String,
        date: String,
        location: String,
        fee: Double,
        description: String
    ): Result<Unit> {
        val path = "events"
        return try {
            val uid = requireUserId()
            require(title.trim().length in 2..140)
            require(date.trim().length in 2..60)
            require(location.trim().length in 2..160)
            require(fee >= 0.0 && fee <= 1_000_000.0)
            require(description.trim().length in 2..3000)
            val docRef = db.collection("events").document()
            docRef.set(
                mapOf(
                    "title" to title.trim(),
                    "date" to date.trim(),
                    "location" to location.trim(),
                    "fee" to fee,
                    "description" to description.trim(),
                    "visibility" to "alumni",
                    "registeredUsers" to emptyList<String>(),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleEventRegistration(eventId: String, currentlyGoing: Boolean): Result<Unit> {
        val path = "events/$eventId"
        return try {
            val uid = requireUserId()
            require(eventId.isNotBlank())
            val updateValue = if (currentlyGoing) {
                FieldValue.arrayRemove(uid)
            } else {
                FieldValue.arrayUnion(uid)
            }
            db.collection("events").document(eventId)
                .update("registeredUsers", updateValue)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createFundCampaign(title: String, targetAmount: Double): Result<Unit> {
        val path = "funds"
        return try {
            val uid = requireUserId()
            require(title.trim().length in 2..140)
            require(targetAmount > 0.0 && targetAmount <= 100_000_000.0)
            val docRef = db.collection("funds").document()
            docRef.set(
                mapOf(
                    "title" to title.trim(),
                    "targetAmount" to targetAmount,
                    "collectedAmount" to 0.0,
                    "spentAmount" to 0.0,
                    "status" to FundStatus.ACTIVE,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addFundTransaction(
        campaignId: String,
        type: String,
        amount: Double,
        description: String,
        date: String,
        addedByName: String
    ): Result<Unit> {
        val path = "funds/$campaignId/transactions"
        return try {
            val uid = requireUserId()
            require(campaignId.isNotBlank())
            require(type in listOf(TransactionType.IN, TransactionType.OUT))
            require(amount > 0.0 && amount <= 10_000_000.0)
            require(description.trim().length in 2..300)
            require(date.trim().length in 2..60)

            val fundRef = db.collection("funds").document(campaignId)
            val txRef = fundRef.collection("transactions").document()
            val batch = db.batch()
            batch.set(
                txRef,
                mapOf(
                    "type" to type,
                    "amount" to amount,
                    "description" to description.trim(),
                    "date" to date.trim(),
                    "addedBy" to uid,
                    "addedByName" to addedByName.take(80),
                    "campaignVisibility" to "alumni",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            val counterField = if (type == TransactionType.IN) "collectedAmount" else "spentAmount"
            batch.update(fundRef, counterField, FieldValue.increment(amount))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createTeacher(
        name: String,
        subject: String,
        tenure: String,
        phone: String,
        status: String
    ): Result<Unit> {
        val path = "teachers"
        return try {
            requireUserId()
            require(name.trim().length in 2..80)
            require(subject.trim().length in 2..80)
            require(tenure.trim().length in 2..60)
            require(phone.trim().length in 4..30)
            require(status in listOf(TeacherStatus.CURRENT, TeacherStatus.RETIRED, TeacherStatus.LATE))
            db.collection("teachers").document().set(
                mapOf(
                    "name" to name.trim(),
                    "subject" to subject.trim(),
                    "tenure" to tenure.trim(),
                    "phone" to phone.trim(),
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTeacher(teacherId: String): Result<Unit> {
        val path = "teachers/$teacherId"
        return try {
            requireUserId()
            require(teacherId.isNotBlank())
            db.collection("teachers").document(teacherId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAppBranding(
        schoolNameBn: String,
        schoolNameEn: String,
        subtitleBn: String,
        subtitleEn: String,
        logoDataOrUrl: String?
    ): Result<Unit> {
        val path = "app_config/branding"
        return try {
            val uid = requireUserId()
            require(schoolNameBn.trim().length in 2..120)
            require(schoolNameEn.trim().length in 2..120)
            val cleanLogo = logoDataOrUrl?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanLogo != null) {
                require(cleanLogo.length <= 300_000) { "Logo image is too large" }
            }
            val payload = mapOf(
                "schoolNameBn" to schoolNameBn.trim(),
                "schoolNameEn" to schoolNameEn.trim(),
                "subtitleBn" to subtitleBn.trim().take(120),
                "subtitleEn" to subtitleEn.trim().take(120),
                "logoDataOrUrl" to cleanLogo,
                "visibility" to "alumni",
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("app_config").document("branding").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserProfile(
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
    ): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank())
            require(name.trim().length in 2..80)
            require(phone.trim().length in 4..40)
            require(sscBatch.trim().matches(Regex("^[0-9]{4}$")))
            val batchDocRef = db.collection("batches").document(sscBatch.trim())
            val existingBatch = try {
                batchDocRef.get().await().toObject(BatchMetadata::class.java)
            } catch (_: Exception) {
                null
            }
            val currentMods = existingBatch?.moderatorUids?.toMutableList() ?: mutableListOf()
            if (role == UserRoles.MODERATOR) {
                if (!currentMods.contains(targetUid)) currentMods.add(targetUid)
            } else {
                currentMods.remove(targetUid)
            }

            val writeBatch = db.batch()
            writeBatch.update(
                db.collection("users").document(targetUid),
                mapOf(
                    "name" to name.trim(),
                    "phone" to if (hidePhone) maskPhoneNumber(phone) else phone.trim(),
                    "hidePhone" to hidePhone,
                    "sscBatch" to sscBatch.trim(),
                    "bloodGroup" to bloodGroup,
                    "profession" to profession.trim(),
                    "city" to city.trim(),
                    "country" to country.trim(),
                    "role" to role,
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            writeBatch.set(
                batchDocRef,
                mapOf(
                    "batchYear" to sscBatch.trim(),
                    "moderatorUids" to currentMods.take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            writeBatch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUser(targetUid: String): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank())
            db.collection("users").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateNotice(
        noticeId: String,
        title: String,
        description: String,
        category: String,
        isPinned: Boolean
    ): Result<Unit> {
        val path = "notices/$noticeId"
        return try {
            requireUserId()
            require(noticeId.isNotBlank())
            require(title.trim().length in 2..140)
            require(description.trim().length in 2..3000)
            db.collection("notices").document(noticeId).update(
                mapOf(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "category" to category,
                    "isPinned" to isPinned
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBloodRequest(
        reqId: String,
        patientName: String,
        bloodGroup: String,
        hospitalLocation: String,
        contactPhone: String,
        bagsNeeded: Int,
        status: String
    ): Result<Unit> {
        val path = "blood_requests/$reqId"
        return try {
            requireUserId()
            require(reqId.isNotBlank())
            require(patientName.trim().length in 2..80)
            require(hospitalLocation.trim().length in 2..160)
            require(contactPhone.trim().length in 6..30)
            require(bagsNeeded in 1..20)
            db.collection("blood_requests").document(reqId).update(
                mapOf(
                    "patientName" to patientName.trim(),
                    "bloodGroup" to bloodGroup,
                    "hospitalLocation" to hospitalLocation.trim(),
                    "contactPhone" to contactPhone.trim(),
                    "bagsNeeded" to bagsNeeded,
                    "status" to status
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBloodRequest(reqId: String): Result<Unit> {
        val path = "blood_requests/$reqId"
        return try {
            requireUserId()
            require(reqId.isNotBlank())
            db.collection("blood_requests").document(reqId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateEvent(
        eventId: String,
        title: String,
        date: String,
        location: String,
        fee: Double,
        description: String
    ): Result<Unit> {
        val path = "events/$eventId"
        return try {
            requireUserId()
            require(eventId.isNotBlank())
            require(title.trim().length in 2..140)
            db.collection("events").document(eventId).update(
                mapOf(
                    "title" to title.trim(),
                    "date" to date.trim(),
                    "location" to location.trim(),
                    "fee" to fee,
                    "description" to description.trim()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        val path = "events/$eventId"
        return try {
            requireUserId()
            require(eventId.isNotBlank())
            db.collection("events").document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFundCampaign(
        campaignId: String,
        title: String,
        targetAmount: Double,
        status: String
    ): Result<Unit> {
        val path = "funds/$campaignId"
        return try {
            requireUserId()
            require(campaignId.isNotBlank())
            require(title.trim().length in 2..140)
            require(targetAmount > 0.0)
            db.collection("funds").document(campaignId).update(
                mapOf(
                    "title" to title.trim(),
                    "targetAmount" to targetAmount,
                    "status" to status
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFundCampaign(campaignId: String): Result<Unit> {
        val path = "funds/$campaignId"
        return try {
            requireUserId()
            require(campaignId.isNotBlank())
            db.collection("funds").document(campaignId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFundTransaction(campaignId: String, tx: FundTransaction): Result<Unit> {
        val path = "funds/$campaignId/transactions/${tx.id}"
        return try {
            requireUserId()
            require(campaignId.isNotBlank() && tx.id.isNotBlank())
            val fundRef = db.collection("funds").document(campaignId)
            val txRef = fundRef.collection("transactions").document(tx.id)
            val counterField = if (tx.type == TransactionType.IN) "collectedAmount" else "spentAmount"
            val batch = db.batch()
            batch.delete(txRef)
            batch.update(fundRef, counterField, FieldValue.increment(-tx.amount))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTeacher(
        teacherId: String,
        name: String,
        subject: String,
        tenure: String,
        phone: String,
        status: String
    ): Result<Unit> {
        val path = "teachers/$teacherId"
        return try {
            requireUserId()
            require(teacherId.isNotBlank())
            db.collection("teachers").document(teacherId).update(
                mapOf(
                    "name" to name.trim(),
                    "subject" to subject.trim(),
                    "tenure" to tenure.trim(),
                    "phone" to phone.trim(),
                    "status" to status
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun pushAppUpdate(
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String,
        mandatory: Boolean,
        pushedByName: String
    ): Result<Unit> {
        val path = "app_config/update_info"
        return try {
            val uid = requireUserId()
            val cleanVersionName = versionName.trim()
            val cleanNotes = releaseNotes.trim()
            val cleanUrl = downloadUrl.trim()
            val cleanPusher = pushedByName.trim().take(80).ifBlank { null }

            require(versionCode in 1..1_000_000) { "Version code must be between 1 and 1,000,000" }
            require(cleanVersionName.length in 1..40) { "Version name must be 1 to 40 characters" }
            require(cleanNotes.length in 2..2000) { "Release notes must be 2 to 2000 characters" }
            if (cleanUrl.isNotEmpty()) {
                require(cleanUrl.length <= 500) { "Download URL must be at most 500 characters" }
                require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                    "Download URL must start with http:// or https://"
                }
            }

            val payload = mapOf(
                "versionCode" to versionCode,
                "versionName" to cleanVersionName,
                "releaseNotes" to cleanNotes,
                "downloadUrl" to cleanUrl.takeIf { it.isNotEmpty() },
                "mandatory" to mandatory,
                "visibility" to "alumni",
                "pushedBy" to uid,
                "pushedByName" to cleanPusher,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("app_config").document("update_info").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun clearAppUpdate(): Result<Unit> {
        val path = "app_config/update_info"
        return try {
            requireUserId()
            db.collection("app_config").document("update_info").delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveNotificationPreferences(
        alertsEnabled: Boolean,
        subscribedGroups: List<String>,
        fcmToken: String? = null
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/private/notifications"
            val cleanGroups = subscribedGroups.filter { it in BloodGroups.all }.distinct().take(8)
            val cleanToken = fcmToken?.trim()?.takeIf { it.isNotEmpty() }?.take(512)
            val payload = mapOf(
                "uid" to uid,
                "alertsEnabled" to alertsEnabled,
                "subscribedGroups" to cleanGroups,
                "fcmToken" to cleanToken,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("users").document(uid)
                .collection("private").document("notifications")
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOwnProfilePhoto(photoDataOrUrl: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val cleanPhoto = photoDataOrUrl.trim()
            require(cleanPhoto.isNotEmpty() && cleanPhoto.length <= 150_000) {
                "Profile photo must not be empty or exceed 150 KB"
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "photoUrl" to cleanPhoto,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeApprovedBatchGallery(): Flow<List<BatchGalleryPhoto>> = flow {
        val uid = requireUserId()
        val path = "batch_gallery"
        emitAll(
            db.collection("batch_gallery")
                .whereEqualTo("status", GalleryStatus.APPROVED)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BatchGalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyBatchGalleryUploads(): Flow<List<BatchGalleryPhoto>> = flow {
        val uid = requireUserId()
        val path = "batch_gallery"
        emitAll(
            db.collection("batch_gallery")
                .whereEqualTo("uploadedBy", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BatchGalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePendingBatchGallery(): Flow<List<BatchGalleryPhoto>> = flow {
        val uid = requireUserId()
        val path = "batch_gallery"
        emitAll(
            db.collection("batch_gallery")
                .whereEqualTo("status", GalleryStatus.PENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BatchGalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logStreamErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun uploadBatchGalleryPhoto(
        sscBatch: String,
        caption: String,
        imageDataOrUrl: String,
        uploadedByName: String,
        autoApproveIfAdmin: Boolean = false
    ): Result<Unit> {
        val path = "batch_gallery"
        return try {
            val uid = requireUserId()
            val cleanBatch = sscBatch.trim()
            val cleanCaption = caption.trim()
            val cleanImage = imageDataOrUrl.trim()
            require(cleanBatch.matches(Regex("^[0-9]{4}$"))) { "SSC batch must be a 4-digit year" }
            require(cleanCaption.length in 2..300) { "Caption must be 2 to 300 characters" }
            require(cleanImage.length in 10..400_000) { "Group photo is invalid or too large" }

            val initialStatus = if (autoApproveIfAdmin) GalleryStatus.APPROVED else GalleryStatus.PENDING
            val payload = mutableMapOf<String, Any>(
                "sscBatch" to cleanBatch,
                "caption" to cleanCaption,
                "imageDataOrUrl" to cleanImage,
                "status" to initialStatus,
                "uploadedBy" to uid,
                "uploadedByName" to uploadedByName.trim().take(80),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (autoApproveIfAdmin) {
                payload["reviewedBy"] = uid
            }
            db.collection("batch_gallery").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewBatchGalleryPhoto(photoId: String, newStatus: String): Result<Unit> {
        val path = "batch_gallery/$photoId"
        return try {
            val uid = requireUserId()
            require(photoId.isNotBlank())
            require(newStatus in listOf(GalleryStatus.APPROVED, GalleryStatus.REJECTED))
            db.collection("batch_gallery").document(photoId).update(
                mapOf(
                    "status" to newStatus,
                    "reviewedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBatchGalleryPhoto(photoId: String): Result<Unit> {
        val path = "batch_gallery/$photoId"
        return try {
            requireUserId()
            require(photoId.isNotBlank())
            db.collection("batch_gallery").document(photoId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
