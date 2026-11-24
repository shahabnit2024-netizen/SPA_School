package com.example.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.bloodAlertDataStore by preferencesDataStore(name = "spa_blood_alerts")

class NotificationPreferences(private val context: Context) {

    private object Keys {
        val ALERTS_ENABLED = booleanPreferencesKey("blood_alerts_enabled")
        val CUSTOM_GROUPS = stringSetPreferencesKey("subscribed_blood_groups")
        val NOTIFIED_REQUEST_IDS = stringSetPreferencesKey("notified_blood_request_ids")
        val ALERT_BASELINE_MILLIS = longPreferencesKey("blood_alert_baseline_millis")
    }

    val alertsEnabledFlow: Flow<Boolean> = context.bloodAlertDataStore.data.map { prefs ->
        prefs[Keys.ALERTS_ENABLED] ?: true
    }

    val customGroupsFlow: Flow<Set<String>> = context.bloodAlertDataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_GROUPS] ?: emptySet()
    }

    suspend fun setAlertsEnabled(enabled: Boolean) {
        context.bloodAlertDataStore.edit { prefs ->
            prefs[Keys.ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setCustomGroups(groups: Set<String>) {
        context.bloodAlertDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_GROUPS] = groups
        }
    }

    suspend fun getOrInitBaselineMillis(): Long {
        val current = context.bloodAlertDataStore.data.first()[Keys.ALERT_BASELINE_MILLIS] ?: 0L
        if (current > 0L) return current
        val initialized = System.currentTimeMillis() - 60_000L
        context.bloodAlertDataStore.edit { prefs ->
            prefs[Keys.ALERT_BASELINE_MILLIS] = initialized
        }
        return initialized
    }

    suspend fun getNotifiedRequestIds(): Set<String> {
        return context.bloodAlertDataStore.data.first()[Keys.NOTIFIED_REQUEST_IDS] ?: emptySet()
    }

    suspend fun markRequestsNotified(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.bloodAlertDataStore.edit { prefs ->
            val existing = prefs[Keys.NOTIFIED_REQUEST_IDS]?.toMutableList() ?: mutableListOf()
            ids.forEach { id ->
                if (!existing.contains(id)) existing.add(id)
            }
            prefs[Keys.NOTIFIED_REQUEST_IDS] = existing.takeLast(100).toSet()
        }
    }
}
