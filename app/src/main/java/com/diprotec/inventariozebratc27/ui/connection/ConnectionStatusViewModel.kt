package com.diprotec.inventariozebratc27.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.core.network.AppConnectionMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionStatusViewModel(
    private val monitor: AppConnectionMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(AppConnectionMode.CHECKING)
    val state: StateFlow<AppConnectionMode> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitor.observeMode().collect { mode ->
                _state.value = mode
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = AppConnectionMode.CHECKING
            _state.value = monitor.checkMode()
        }
    }
}