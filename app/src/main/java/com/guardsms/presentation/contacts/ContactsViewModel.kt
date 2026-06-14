package com.guardsms.presentation.contacts

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardsms.data.repository.GuardRepository
import com.guardsms.domain.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ContactsState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val totalContacts: Int = 0
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: GuardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state.asStateFlow()

    init { loadContacts() }

    fun loadContacts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.getContacts().onSuccess { contacts ->
                _state.value = ContactsState(
                    contacts = contacts,
                    totalContacts = contacts.size,
                    isLoading = false
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load contacts"
                )
            }
        }
    }

    fun syncDeviceContacts(contentResolver: ContentResolver) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true)
            val contacts = withContext(Dispatchers.IO) {
                readDeviceContacts(contentResolver)
            }
            repository.syncContacts(contacts).onSuccess {
                loadContacts()
                _state.value = _state.value.copy(
                    isSyncing = false,
                    successMessage = "${contacts.size} contacts synced"
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    isSyncing = false,
                    errorMessage = "Sync failed. Check your connection."
                )
            }
        }
    }

    private fun readDeviceContacts(contentResolver: ContentResolver): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val seen = mutableSetOf<String>()

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx)?.trim() ?: continue
                val phone = it.getString(numberIdx)?.trim() ?: continue
                val normalized = repository.normalizePhone(phone)
                if (seen.add(normalized)) {
                    contacts.add(Contact(name = name, phone = phone, phoneNormalized = normalized))
                }
            }
        }
        return contacts
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }
}
