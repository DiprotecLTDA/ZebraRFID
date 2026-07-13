package com.diprotec.inventariozebratc27.ui.connection

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

    val color = when (mode) {
        AppConnectionMode.ONLINE_API -> Color(0xFF2E7D32)
        AppConnectionMode.CHECKING -> Color(0xFFF9A825)
        AppConnectionMode.LOCAL_ROOM -> Color(0xFFC62828)
    }

    val label = when (mode) {
        AppConnectionMode.ONLINE_API -> "Online"
        AppConnectionMode.CHECKING -> "Verificando"
        AppConnectionMode.LOCAL_ROOM -> "Room"
    }

    StatusChip(
        modifier = modifier,
        dotColor = color,
        title = "Conexión",
        value = label
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}