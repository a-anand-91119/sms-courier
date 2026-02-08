package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for history cleanup operation.
 */
sealed class CleanupState {
    data object Idle : CleanupState()
    data object Loading : CleanupState()
    data class Success(val sessionsDeleted: Int, val messagesDeleted: Int) : CleanupState()
    data class Error(val message: String) : CleanupState()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val forwardingSessionRepository: ForwardingSessionRepository,
) : ViewModel() {

    // Main Settings
    val theme: StateFlow<AppTheme> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val notificationPersistence: StateFlow<Boolean> = settingsRepository.notificationPersistence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultForwardingDurationMinutes: StateFlow<Int> = settingsRepository.defaultForwardingDurationMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    // Data & Storage Settings
    val historyRetentionDays: StateFlow<Int> = settingsRepository.historyRetentionDays
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsDefaults.HISTORY_RETENTION_DAYS,
        )

    // Advanced/Security Settings
    val lockoutDurationMinutes: StateFlow<Int> = settingsRepository.lockoutDurationMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsDefaults.LOCKOUT_DURATION,
        )

    val maxFailedAttempts: StateFlow<Int> = settingsRepository.maxFailedAttempts
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsDefaults.MAX_FAILED_ATTEMPTS,
        )

    val challengeExpiryMinutes: StateFlow<Int> = settingsRepository.challengeExpiryMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsDefaults.CHALLENGE_EXPIRY,
        )

    val maxPairingResendAttempts: StateFlow<Int> = settingsRepository.maxPairingResendAttempts
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsDefaults.MAX_PAIRING_RESEND_ATTEMPTS,
        )

    val pairingResendCooldownMinutes: StateFlow<Int> = settingsRepository.pairingResendCooldownMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsDefaults.PAIRING_RESEND_COOLDOWN,
        )

    val authRequestTimeoutMinutes: StateFlow<Int> = settingsRepository.authRequestTimeoutMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsDefaults.AUTH_REQUEST_TIMEOUT,
        )

    // Error state for validation feedback
    private val _settingError = MutableStateFlow<String?>(null)
    val settingError: StateFlow<String?> = _settingError.asStateFlow()

    fun clearSettingError() {
        _settingError.value = null
    }

    // Cleanup state
    private val _cleanupState = MutableStateFlow<CleanupState>(CleanupState.Idle)
    val cleanupState: StateFlow<CleanupState> = _cleanupState.asStateFlow()

    fun resetCleanupState() {
        _cleanupState.value = CleanupState.Idle
    }

    // Main Settings Setters
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setNotificationPersistence(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationPersistence(enabled)
        }
    }

    fun setDefaultForwardingDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultForwardingDuration(minutes)
        }
    }

    // Advanced/Security Settings Setters
    fun setLockoutDuration(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setLockoutDuration(minutes)
                _settingError.value = null
            } catch (e: IllegalArgumentException) {
                _settingError.value = e.message
            }
        }
    }

    fun setMaxFailedAttempts(attempts: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setMaxFailedAttempts(attempts)
                _settingError.value = null
            } catch (e: IllegalArgumentException) {
                _settingError.value = e.message
            }
        }
    }

    fun setChallengeExpiry(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setChallengeExpiry(minutes)
                _settingError.value = null
            } catch (e: IllegalArgumentException) {
                _settingError.value = e.message
            }
        }
    }

    fun setMaxPairingResendAttempts(attempts: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setMaxPairingResendAttempts(attempts)
                _settingError.value = null
            } catch (e: IllegalArgumentException) {
                _settingError.value = e.message
            }
        }
    }

    fun setPairingResendCooldown(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setPairingResendCooldown(minutes)
                _settingError.value = null
            } catch (e: IllegalArgumentException) {
                _settingError.value = e.message
            }
        }
    }

    fun setAuthRequestTimeout(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setAuthRequestTimeout(minutes)
                _settingError.value = null
            } catch (e: IllegalArgumentException) {
                _settingError.value = e.message
            }
        }
    }

    // Data & Storage Settings Setters
    fun setHistoryRetentionDays(days: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setHistoryRetentionDays(days)
                _settingError.value = null
            } catch (e: IllegalArgumentException) {
                _settingError.value = e.message
            }
        }
    }

    fun cleanupOldHistory() {
        viewModelScope.launch {
            _cleanupState.value = CleanupState.Loading
            try {
                val retentionDays = historyRetentionDays.value
                if (retentionDays == 0) {
                    // Forever setting - show info dialog via Success with 0 counts
                    _cleanupState.value = CleanupState.Success(0, 0)
                } else {
                    val result = forwardingSessionRepository.cleanupOldSessions(retentionDays)
                    _cleanupState.value = CleanupState.Success(
                        sessionsDeleted = result.sessionCount,
                        messagesDeleted = result.messageCount,
                    )
                }
            } catch (e: Exception) {
                _cleanupState.value = CleanupState.Error(e.message ?: "Cleanup failed")
            }
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val forwardingSessionRepository: ForwardingSessionRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, forwardingSessionRepository) as T
        }
    }
}
