package dev.notyouraverage.smscourier.data.settings

object SettingsDefaults {
    // Main Settings
    const val NOTIFICATION_PERSISTENCE = true
    const val DEFAULT_FORWARDING_DURATION = 5 // minutes
    const val MAX_FORWARDING_DURATION = 30 // minutes
    val THEME = AppTheme.SYSTEM

    // Advanced/Security Settings (conservative defaults matching existing SecurityManager)
    const val LOCKOUT_DURATION = 15 // minutes
    const val MAX_FAILED_ATTEMPTS = 5
    const val CHALLENGE_EXPIRY = 2 // minutes
    const val MAX_PAIRING_RESEND_ATTEMPTS = 5
    const val PAIRING_RESEND_COOLDOWN = 1 // minute
    const val AUTH_REQUEST_TIMEOUT = 5 // minutes

    // Data & Storage Settings
    const val HISTORY_RETENTION_DAYS = 30 // days (0 = forever)
    const val AUTO_CLEANUP_ENABLED = true // ON by default for new installations
    const val LAST_CLEANUP_TIMESTAMP = 0L // 0 = never run
}
