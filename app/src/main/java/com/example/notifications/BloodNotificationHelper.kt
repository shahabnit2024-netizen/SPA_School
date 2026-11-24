package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BloodGroups
import com.example.data.model.bloodGroupTopicName
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging

object BloodNotificationHelper {
    const val CHANNEL_ID = "urgent_blood_requests"
    const val EXTRA_OPEN_BLOOD_REQUESTS = "extra_open_blood_requests"
    const val EXTRA_BLOOD_REQUEST_ID = "extra_blood_request_id"
    private const val TAG = "BloodNotifyHelper"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Urgent Blood Donation Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts alumni with matching blood groups when urgent blood requests are posted."
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showUrgentBloodNotification(
        context: Context,
        requestId: String,
        patientName: String,
        bloodGroup: String,
        hospitalLocation: String,
        contactPhone: String,
        bagsNeeded: Long,
        isBengali: Boolean
    ) {
        if (!hasPostNotificationPermission(context)) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_BLOOD_REQUESTS, true)
            putExtra(EXTRA_BLOOD_REQUEST_ID, requestId)
        }
        val requestCode = requestId.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBengali) {
            "জরুরি রক্তের প্রয়োজন: $bloodGroup ($bagsNeeded ব্যাগ)"
        } else {
            "Urgent Blood Needed: $bloodGroup ($bagsNeeded bag${if (bagsNeeded > 1L) "s" else ""})"
        }
        val body = if (isBengali) {
            "রোগী: $patientName • $hospitalLocation • কল: $contactPhone"
        } else {
            "Patient: $patientName • $hospitalLocation • Call: $contactPhone"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_spa_crest)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(requestCode, notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission", se)
        }
    }

    fun syncBloodGroupTopics(activeGroups: Set<String>, alertsEnabled: Boolean) {
        try {
            val messaging = Firebase.messaging
            BloodGroups.all.forEach { bg ->
                val topic = bloodGroupTopicName(bg)
                if (alertsEnabled && bg in activeGroups) {
                    messaging.subscribeToTopic(topic)
                } else {
                    messaging.unsubscribeFromTopic(topic)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to sync FCM blood topics", e)
        }
    }
}
