package com.guardsms.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardsms.data.repository.GuardRepository
import com.guardsms.domain.model.FlaggedDomain
import com.guardsms.domain.model.SmsMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val totalMessages: Int = 0,
    val flaggedCount: Int = 0,
    val safeCount: Int = 0,
    val blockedCount: Int = 0,
    val recentMessages: List<SmsMessage> = emptyList(),
    val topFlaggedDomains: List<FlaggedDomain> = emptyList(),
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: GuardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val messages = repository.getMessages(100).getOrDefault(emptyList())
            val flaggedDomains = repository.getFlaggedDomains().getOrDefault(emptyList())

            _state.value = DashboardState(
                totalMessages = messages.size,
                flaggedCount = messages.count { it.isRedflagged },
                safeCount = messages.count { !it.isRedflagged },
                blockedCount = messages.count { it.status == "BLOCKED" },
                recentMessages = messages.take(5),
                topFlaggedDomains = flaggedDomains.take(5),
                isLoading = false,
                isLoggedIn = repository.getCurrentUserId() != null
            )
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.signOut()
            onComplete()
        }
    }
}
