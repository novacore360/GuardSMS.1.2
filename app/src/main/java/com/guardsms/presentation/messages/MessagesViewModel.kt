package com.guardsms.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardsms.data.repository.GuardRepository
import com.guardsms.domain.model.SmsMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesState(
    val messages: List<SmsMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val filterTab: FilterTab = FilterTab.ALL
)

enum class FilterTab { ALL, FLAGGED, SAFE }

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repository: GuardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MessagesState())
    val state: StateFlow<MessagesState> = _state.asStateFlow()

    private val allMessages = mutableListOf<SmsMessage>()

    init {
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.getMessages().onSuccess { msgs ->
                allMessages.clear()
                allMessages.addAll(msgs)
                applyFilter()
                _state.value = _state.value.copy(isLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load messages"
                )
            }
        }
    }

    fun setFilter(tab: FilterTab) {
        _state.value = _state.value.copy(filterTab = tab)
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (_state.value.filterTab) {
            FilterTab.ALL -> allMessages.toList()
            FilterTab.FLAGGED -> allMessages.filter { it.isRedflagged }
            FilterTab.SAFE -> allMessages.filter { !it.isRedflagged }
        }
        _state.value = _state.value.copy(messages = filtered)
    }

    fun redFlagMessage(message: SmsMessage) {
        viewModelScope.launch {
            repository.redFlagMessage(message.id).onSuccess {
                // Also report the message domains
                message.extractedDomains.forEach { domain ->
                    repository.reportDomain(domain, "user_report", "Flagged from message")
                }
                repository.reportRawMessage(message.body, "user_report", "Flagged by user")
                loadMessages()
                _state.value = _state.value.copy(successMessage = "Message flagged and reported")
            }.onFailure {
                _state.value = _state.value.copy(errorMessage = "Failed to flag message")
            }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }
}
