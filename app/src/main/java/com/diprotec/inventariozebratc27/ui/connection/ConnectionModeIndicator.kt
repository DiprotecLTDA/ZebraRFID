package com.diprotec.inventariozebratc27.ui.connection

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
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionModeIndicator(
    modifier: Modifier = Modifier,
    vm: ConnectionStatusViewModel? = null
) {
    val fallbackState = remember {
        mutableStateOf(AppConnectionMode.CHECKING)
    }

    val mode by if (vm != null) {
        vm.state.collectAsState()
    } else {
        fallbackState
    }

    val status = when (mode) {
        AppConnectionMode.ONLINE_API -> AppStatus.ONLINE
        AppConnectionMode.CHECKING -> AppStatus.WARNING
        AppConnectionMode.LOCAL_ROOM -> AppStatus.ERROR
    }

    val label = when (mode) {
        AppConnectionMode.ONLINE_API -> "Online"
        AppConnectionMode.CHECKING -> "Verificando"
        AppConnectionMode.LOCAL_ROOM -> "Room"
    }

    StatusChip(
        modifier = modifier
            .widthIn(min = Dimens.statusMinWidth),
        text = "Conexión: $label",
        status = status
    )
}
