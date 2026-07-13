package com.diprotec.inventariozebratc27.ui.syncstatus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkerStatusViewModel(
    context: Context
) : ViewModel() {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(WorkerSyncState.STOPPED)
    val state: StateFlow<WorkerSyncState> = _state.asStateFlow()

    private val workNames = listOf(
        "catalogSync_periodic",
        "catalogSync_once",
        "pendingInventorySync_periodic",
        "pendingInventorySync_once"
    )

    init {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(2_000L)
            }
        }
    }

    private suspend fun refresh() {
        val workInfos = withContext(Dispatchers.IO) {
            val wm = WorkManager.getInstance(appContext)

            workNames.flatMap { name ->
                runCatching {
                    wm.getWorkInfosForUniqueWork(name).get()
                }.getOrDefault(emptyList())
            }
        }

        _state.value = when {
            workInfos.any { it.state == WorkInfo.State.RUNNING } -> {
                WorkerSyncState.SYNCING
            }

            workInfos.any {
                it.state == WorkInfo.State.ENQUEUED ||
                        it.state == WorkInfo.State.BLOCKED
            } -> {
                WorkerSyncState.WAITING
            }

            else -> {
                WorkerSyncState.STOPPED
            }
        }
    }
}