package com.diprotec.inventariozebratc27.ui.syncstatus

import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.theme.StatusError
import com.diprotec.inventariozebratc27.ui.theme.StatusOnline
import com.diprotec.inventariozebratc27.ui.theme.StatusWarning
import com.diprotec.inventariozebratc27.ui.components.AppStatus
import com.diprotec.inventariozebratc27.ui.components.StatusChip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WorkerTrafficLight(
    modifier: Modifier = Modifier,
    vm: WorkerStatusViewModel? = null
) {
    val fallbackState = remember {
        mutableStateOf(WorkerSyncState.STOPPED)
    }

    val state by if (vm != null) {
        vm.state.collectAsState()
    } else {
        fallbackState
    }

    val status = when (state) {
        WorkerSyncState.SYNCING -> AppStatus.ONLINE
        WorkerSyncState.WAITING -> AppStatus.WARNING
        WorkerSyncState.STOPPED -> AppStatus.ERROR
    }

    val shortValue = when (state) {
        WorkerSyncState.SYNCING -> "SYNC"
        WorkerSyncState.WAITING -> "WAIT"
        WorkerSyncState.STOPPED -> "OFF"
    }

    StatusChip(
        modifier = modifier
            .widthIn(min = Dimens.statusMinWidth),
        text = "Sincronización: $shortValue",
        status = status
    )
}
