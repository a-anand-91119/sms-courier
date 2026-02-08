package dev.notyouraverage.smscourier.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.data.settings.PreferenceKeys
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.settingsDataStore

    // Main Settings - Read Flows
    val notificationPersistence: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_PERSISTENCE]
                ?: SettingsDefaults.NOTIFICATION_PERSISTENCE
        }

    val defaultForwardingDurationMinutes: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.DEFAULT_FORWARDING_DURATION]
                ?: SettingsDefaults.DEFAULT_FORWARDING_DURATION
        }

    val theme: Flow<AppTheme> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferenceKeys.THEME] ?: SettingsDefaults.THEME.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                SettingsDefaults.THEME
            }
        }

    // Advanced/Security Settings - Read Flows
    val lockoutDurationMinutes: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.LOCKOUT_DURATION]
                ?: SettingsDefaults.LOCKOUT_DURATION
        }

    val maxFailedAttempts: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.MAX_FAILED_ATTEMPTS]
                ?: SettingsDefaults.MAX_FAILED_ATTEMPTS
        }

    val challengeExpiryMinutes: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.CHALLENGE_EXPIRY]
                ?: SettingsDefaults.CHALLENGE_EXPIRY
        }

    val maxPairingResendAttempts: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.MAX_PAIRING_RESEND_ATTEMPTS]
                ?: SettingsDefaults.MAX_PAIRING_RESEND_ATTEMPTS
        }

    val pairingResendCooldownMinutes: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.PAIRING_RESEND_COOLDOWN]
                ?: SettingsDefaults.PAIRING_RESEND_COOLDOWN
        }

    val authRequestTimeoutMinutes: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.AUTH_REQUEST_TIMEOUT]
                ?: SettingsDefaults.AUTH_REQUEST_TIMEOUT
        }

    // Data & Storage Settings - Read Flows
    val historyRetentionDays: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.HISTORY_RETENTION_DAYS]
                ?: SettingsDefaults.HISTORY_RETENTION_DAYS
        }

    // Main Settings - Write Methods
    suspend fun setNotificationPersistence(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_PERSISTENCE] = enabled
        }
    }

    suspend fun setDefaultForwardingDuration(minutes: Int) {
        require(minutes in 1..60) { "Duration must be between 1 and 60 minutes" }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_FORWARDING_DURATION] = minutes
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME] = theme.name
        }
    }

    // Advanced/Security Settings - Write Methods
    suspend fun setLockoutDuration(minutes: Int) {
        require(minutes in 1..60) { "Lockout duration must be between 1 and 60 minutes" }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LOCKOUT_DURATION] = minutes
        }
    }

    suspend fun setMaxFailedAttempts(attempts: Int) {
        require(attempts in 1..10) { "Max attempts must be between 1 and 10" }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.MAX_FAILED_ATTEMPTS] = attempts
        }
    }

    suspend fun setChallengeExpiry(minutes: Int) {
        require(minutes in 1..10) { "Challenge expiry must be between 1 and 10 minutes" }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.CHALLENGE_EXPIRY] = minutes
        }
    }

    suspend fun setMaxPairingResendAttempts(attempts: Int) {
        require(attempts in 1..10) { "Max pairing resend attempts must be between 1 and 10" }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.MAX_PAIRING_RESEND_ATTEMPTS] = attempts
        }
    }

    suspend fun setPairingResendCooldown(minutes: Int) {
        require(minutes in 1..10) { "Pairing resend cooldown must be between 1 and 10 minutes" }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PAIRING_RESEND_COOLDOWN] = minutes
        }
    }

    suspend fun setAuthRequestTimeout(minutes: Int) {
        require(minutes in 1..30) { "Auth request timeout must be between 1 and 30 minutes" }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTH_REQUEST_TIMEOUT] = minutes
        }
    }

    // Data & Storage Settings - Write Methods
    suspend fun setHistoryRetentionDays(days: Int) {
        require(days == 0 || days in listOf(7, 30, 90)) {
            "Retention must be 0 (forever) or one of: 7, 30, 90 days"
        }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HISTORY_RETENTION_DAYS] = days
        }
    }

    /**
     * Clears all settings, resetting them to defaults.
     * Useful for testing and "Reset to defaults" functionality.
     */
    suspend fun clearAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
