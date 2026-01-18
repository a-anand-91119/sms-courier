package dev.notyouraverage.smscourier.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    // Main Settings
    val NOTIFICATION_PERSISTENCE = booleanPreferencesKey("notification_persistence")
    val DEFAULT_FORWARDING_DURATION = intPreferencesKey("default_forwarding_duration")
    val THEME = stringPreferencesKey("theme")

    // Advanced/Security Settings
    val LOCKOUT_DURATION = intPreferencesKey("lockout_duration")
    val MAX_FAILED_ATTEMPTS = intPreferencesKey("max_failed_attempts")
    val CHALLENGE_EXPIRY = intPreferencesKey("challenge_expiry")
    val MAX_PAIRING_RESEND_ATTEMPTS = intPreferencesKey("max_pairing_resend_attempts")
    val PAIRING_RESEND_COOLDOWN = intPreferencesKey("pairing_resend_cooldown")
    val AUTH_REQUEST_TIMEOUT = intPreferencesKey("auth_request_timeout")
}
