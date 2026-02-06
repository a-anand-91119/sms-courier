package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.repository.ForwardedMessageRepository
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionHistoryViewModel(
    private val sessionRepository: ForwardingSessionRepository,
    private val messageRepository: ForwardedMessageRepository,
    val phoneNumber: String,
    val deviceRole: String,
) : ViewModel() {

    // Tab state - per CONTEXT.md: "Always opens to Sessions view by default, no persistence of toggle state"
    private val _selectedTab = MutableStateFlow(Tab.SESSIONS)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()

    // Paged sessions flow - cachedIn survives configuration changes
    val sessions: Flow<PagingData<ForwardingSession>> =
        sessionRepository.getSessionsForDevicePaged(phoneNumber)
            .cachedIn(viewModelScope)

    // Paged contacts flow (distinct senders)
    val contacts: Flow<PagingData<String>> =
        messageRepository.getDistinctSendersForDevice(phoneNumber)
            .cachedIn(viewModelScope)

    // Selected session for bottom sheet
    private val _selectedSession = MutableStateFlow<ForwardingSession?>(null)
    val selectedSession: StateFlow<ForwardingSession?> = _selectedSession.asStateFlow()

    // Contact message counts (loaded on demand)
    private val _contactMessageCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val contactMessageCounts: StateFlow<Map<String, Int>> = _contactMessageCounts.asStateFlow()

    fun selectTab(tab: Tab) {
        _selectedTab.value = tab
    }

    fun selectSession(session: ForwardingSession?) {
        _selectedSession.value = session
    }

    fun loadContactMessageCount(senderNumber: String) {
        if (_contactMessageCounts.value.containsKey(senderNumber)) return
        viewModelScope.launch {
            val count = messageRepository.getMessageCountForSender(phoneNumber, senderNumber)
            _contactMessageCounts.value = _contactMessageCounts.value + (senderNumber to count)
        }
    }

    enum class Tab {
        SESSIONS, CONTACTS
    }

    class Factory(
        private val sessionRepository: ForwardingSessionRepository,
        private val messageRepository: ForwardedMessageRepository,
        private val phoneNumber: String,
        private val deviceRole: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SessionHistoryViewModel(
                sessionRepository,
                messageRepository,
                phoneNumber,
                deviceRole,
            ) as T
        }
    }
}
