package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.notifications.BloodNotificationHelper
import com.example.ui.AppRoot

class MainActivity : ComponentActivity() {
    private var openBloodRequestsFromNotification by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            AppRoot(
                openBloodRequestsFromNotification = openBloodRequestsFromNotification,
                onConsumedBloodNotificationNav = { openBloodRequestsFromNotification = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(BloodNotificationHelper.EXTRA_OPEN_BLOOD_REQUESTS, false) == true) {
            openBloodRequestsFromNotification = true
        }
    }
}
