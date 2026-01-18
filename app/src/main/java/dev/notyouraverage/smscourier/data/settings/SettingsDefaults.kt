package dev.notyouraverage.smscourier.data.settings

object SettingsDefaults {
    // Main Settings
    const val NOTIFICATION_PERSISTENCE = false
    const val DEFAULT_FORWARDING_DURATION = 15 // minutes
    val THEME = AppTheme.SYSTEM

    // Advanced/Security Settings (conservative defaults matching existing SecurityManager)
    const val LOCKOUT_DURATION = 15 // minutes
    const val MAX_FAILED_ATTEMPTS = 5
    const val CHALLENGE_EXPIRY = 2 // minutes
    const val MAX_PAIRING_RESEND_ATTEMPTS = 5
    const val PAIRING_RESEND_COOLDOWN = 1 // minute
    const val AUTH_REQUEST_TIMEOUT = 5 // minutes
}
