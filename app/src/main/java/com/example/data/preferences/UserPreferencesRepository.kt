package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class AppUserPreferences(
    val language: String = "fr", // "fr" or "ar"
    val themeMode: String = "system", // "system", "light", "dark"
    val viewMode: String = "grid", // "grid" or "list"
    val sortOrder: String = "name", // "name", "updated", "count"
    val onboardingCompleted: Boolean = false,
    val geminiApiKey: String = "AIzaSyAykWKD5d9qxr6veTHajsDY8JkIfO1YwPA"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    val userPreferencesFlow: Flow<AppUserPreferences> = context.dataStore.data.map { preferences ->
        val rawKey = preferences[PreferencesKeys.GEMINI_API_KEY]
        val validKey = if (!rawKey.isNullOrBlank()) rawKey else "AIzaSyAykWKD5d9qxr6veTHajsDY8JkIfO1YwPA"
        AppUserPreferences(
            language = preferences[PreferencesKeys.LANGUAGE] ?: "fr",
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "system",
            viewMode = preferences[PreferencesKeys.VIEW_MODE] ?: "grid",
            sortOrder = preferences[PreferencesKeys.SORT_ORDER] ?: "name",
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            geminiApiKey = validKey
        )
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = lang
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setViewMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIEW_MODE] = mode
        }
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = order
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey.trim()
        }
    }

    suspend fun resetPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
            preferences[PreferencesKeys.LANGUAGE] = "fr"
            preferences[PreferencesKeys.THEME_MODE] = "system"
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }
}
