package com.example.notifications

import com.example.data.prefs.LanguagePreferences
import com.example.data.repository.AlumniRepository
import com.example.ui.i18n.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpaFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val user = Firebase.auth.currentUser ?: return
        serviceScope.launch {
            val prefs = NotificationPreferences(applicationContext)
            val enabled = prefs.alertsEnabledFlow.first()
            val groups = prefs.customGroupsFlow.first()
            AlumniRepository(applicationContext).saveNotificationPreferences(
                alertsEnabled = enabled,
                subscribedGroups = groups.toList(),
                fcmToken = token
            )
            val _uid = user.uid
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val requestId = data["requestId"] ?: message.messageId ?: System.currentTimeMillis().toString()
        val bloodGroup = data["bloodGroup"] ?: return
        val patientName = data["patientName"] ?: message.notification?.title ?: "Urgent Patient"
        val hospitalLocation = data["hospitalLocation"] ?: message.notification?.body ?: ""
        val contactPhone = data["contactPhone"] ?: ""
        val bagsNeeded = data["bagsNeeded"]?.toLongOrNull() ?: 1L
        val postedBy = data["postedBy"].orEmpty()

        val currentUid = Firebase.auth.currentUser?.uid
        if (postedBy.isNotEmpty() && postedBy == currentUid) return

        serviceScope.launch {
            val prefs = NotificationPreferences(applicationContext)
            val enabled = prefs.alertsEnabledFlow.first()
            if (!enabled) return@launch
            val isBengali = LanguagePreferences(applicationContext).languageFlow.first() == AppLanguage.BENGALI
            BloodNotificationHelper.showUrgentBloodNotification(
                context = applicationContext,
                requestId = requestId,
                patientName = patientName,
                bloodGroup = bloodGroup,
                hospitalLocation = hospitalLocation,
                contactPhone = contactPhone,
                bagsNeeded = bagsNeeded,
                isBengali = isBengali
            )
            prefs.markRequestsNotified(listOf(requestId))
        }
    }
}
