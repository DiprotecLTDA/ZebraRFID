package com.diprotec.inventariozebratc27.ui.syncstatus

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

    val color = when (state) {
        WorkerSyncState.SYNCING -> Color(0xFF2E7D32)
        WorkerSyncState.WAITING -> Color(0xFFF9A825)
        WorkerSyncState.STOPPED -> Color(0xFFC62828)
    }

    val shortValue = when (state) {
        WorkerSyncState.SYNCING -> "SYNC"
        WorkerSyncState.WAITING -> "WAIT"
        WorkerSyncState.STOPPED -> "OFF"
    }

    StatusChip(
        modifier = modifier,
        dotColor = color,
        title = "Sincronización",
        value = shortValue
    )
}

@Composable
private fun StatusChip(
    modifier: Modifier = Modifier,
    dotColor: Color,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier
            .widthIn(min = 150.dp)
            .heightIn(min = 40.dp),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(dotColor, CircleShape)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "$title:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.size(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}