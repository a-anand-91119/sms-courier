package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val theme: StateFlow<AppTheme> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val notificationPersistence: StateFlow<Boolean> = settingsRepository.notificationPersistence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultForwardingDurationMinutes: StateFlow<Int> = settingsRepository.defaultForwardingDurationMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

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

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository) as T
        }
    }
}
