package com.example.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.model.BloodRequest
import com.example.data.model.BloodRequestStatus
import com.example.data.model.UserProfile
import com.example.data.prefs.LanguagePreferences
import com.example.ui.i18n.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class BloodAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val currentUser = Firebase.auth.currentUser ?: return Result.success()
        val prefs = NotificationPreferences(applicationContext)
        val alertsEnabled = prefs.alertsEnabledFlow.first()
        if (!alertsEnabled || !BloodNotificationHelper.hasPostNotificationPermission(applicationContext)) {
            return Result.success()
        }

        return try {
            val db = FirebaseFirestore.getInstance(
                applicationContext.getString(R.string.firestore_database_id)
            )
            val profile = db.collection("users").document(currentUser.uid).get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.success()

            if (!profile.isApproved) return Result.success()

            val customGroups = prefs.customGroupsFlow.first()
            val targetGroups = customGroups.ifEmpty { setOf(profile.bloodGroup) }
            val baseline = prefs.getOrInitBaselineMillis()
            val notifiedIds = prefs.getNotifiedRequestIds()
            val isBengali = LanguagePreferences(applicationContext).languageFlow.first() == AppLanguage.BENGALI

            val openRequests = db.collection("blood_requests")
                .whereEqualTo("status", BloodRequestStatus.OPEN)
                .get()
                .await()
                .toObjects(BloodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            val newlyNotified = mutableListOf<String>()
            for (req in openRequests) {
                val createdAtMillis = req.createdAt?.toDate()?.time ?: 0L
                val isNew = createdAtMillis >= baseline && req.id.isNotBlank() && req.id !in notifiedIds
                val matchesGroup = req.effectiveTargetGroups.any { it in targetGroups }
                val notSelf = req.postedBy != currentUser.uid

                if (isNew && matchesGroup && notSelf) {
                    BloodNotificationHelper.showUrgentBloodNotification(
                        context = applicationContext,
                        requestId = req.id,
                        patientName = req.patientName,
                        bloodGroup = req.bloodGroup,
                        hospitalLocation = req.hospitalLocation,
                        contactPhone = req.contactPhone,
                        bagsNeeded = req.bagsNeeded,
                        isBengali = isBengali
                    )
                    newlyNotified.add(req.id)
                }
            }
            prefs.markRequestsNotified(newlyNotified)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "spa_blood_alert_periodic_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<BloodAlertWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }
}
