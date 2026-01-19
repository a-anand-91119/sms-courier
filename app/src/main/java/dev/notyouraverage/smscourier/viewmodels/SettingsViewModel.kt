package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import dev.notyouraverage.smscourier.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // Main Settings
    val theme: StateFlow<AppTheme> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val notificationPersistence: StateFlow<Boolean> = settingsRepository.notificationPersistence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultForwardingDurationMinutes: StateFlow<Int> = settingsRepository.defaultForwardingDurationMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

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

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository) as T
        }
    }
}
