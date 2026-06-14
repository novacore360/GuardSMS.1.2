package com.guardsms.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardsms.data.repository.GuardRepository
import com.guardsms.domain.model.FlaggedDomain
import com.guardsms.domain.model.UserReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsState(
    val flaggedDomains: List<FlaggedDomain> = emptyList(),
    val userReports: List<UserReport> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val activeTab: ReportTab = ReportTab.COMMUNITY
)

enum class ReportTab { COMMUNITY, MY_REPORTS }

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: GuardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val domains = repository.getFlaggedDomains().getOrDefault(emptyList())
            val reports = repository.getUserReports().getOrDefault(emptyList())
            _state.value = _state.value.copy(
                flaggedDomains = domains,
                userReports = reports,
                isLoading = false
            )
        }
    }

    fun setTab(tab: ReportTab) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    fun reportDomain(domain: String, threatType: String, description: String?) {
        if (domain.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Enter a domain name")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true)
            repository.reportDomain(domain, threatType, description)
                .onSuccess {
                    repository.submitReport("domain", domain, threatType, description)
                    loadAll()
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        successMessage = "Domain reported successfully"
                    )
                }.onFailure {
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to report domain"
                    )
                }
        }
    }

    fun reportUrl(url: String, threatType: String, description: String?) {
        if (url.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Enter a URL")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true)
            repository.reportUrl(url, threatType, description)
                .onSuccess {
                    repository.submitReport("url", url, threatType, description)
                    loadAll()
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        successMessage = "URL reported successfully"
                    )
                }.onFailure {
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to report URL"
                    )
                }
        }
    }

    fun reportMessage(message: String, threatType: String, description: String?) {
        if (message.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Paste the suspicious message")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true)
            repository.reportRawMessage(message, threatType, description)
                .onSuccess {
                    repository.submitReport("message", message, threatType, description)
                    loadAll()
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        successMessage = "Message reported successfully"
                    )
                }.onFailure {
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to report message"
                    )
                }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }
}
