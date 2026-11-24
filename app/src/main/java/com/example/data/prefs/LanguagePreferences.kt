package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "spa_alumni_prefs")

class LanguagePreferences(private val context: Context) {
    private val langKey = stringPreferencesKey("app_language")

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[langKey]) {
            AppLanguage.ENGLISH.name -> AppLanguage.ENGLISH
            else -> AppLanguage.BENGALI
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[langKey] = language.name
        }
    }
}
