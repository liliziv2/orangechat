package me.rerere.rikkahub.ui.pages.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.security.SecurityAuditRepository

class SecurityAuditVM(
    private val repository: SecurityAuditRepository,
) : ViewModel() {
    val logs = repository.recentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
