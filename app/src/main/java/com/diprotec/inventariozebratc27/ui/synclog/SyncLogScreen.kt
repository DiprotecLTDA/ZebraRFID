package com.diprotec.inventariozebratc27.ui.synclog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.ButtonRedDark
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White

@Composable
fun SyncLogScreen(
    onBack: () -> Unit,
    vm: SyncLogViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val logs = uiState.logs

    var showClearDialog by remember {
        mutableStateOf(false)
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearDialog = false
            },
            title = {
                Text("Limpiar historial")
            },
            text = {
                Text(
                    text = "¿Desea limpiar todo el historial de envíos? Esta acción no elimina capturas del inventario."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearAll()
                        showClearDialog = false
                    }
                ) {
                    Text("Limpiar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        SyncLogTopBar(
            canClearLogs = uiState.canClearLogs,
            onBack = onBack,
            onClearClick = {
                if (logs.isNotEmpty()) {
                    showClearDialog = true
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .background(Background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                if (!uiState.message.isNullOrBlank()) {
                    Text(
                        text = uiState.message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ButtonRedDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!uiState.canClearLogs) {
                    Text(
                        text = "La eliminación del historial no está permitida para este perfil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = "No hay envíos registrados.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(logs) { item ->
                            SyncLogCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncLogTopBar(
    canClearLogs: Boolean,
    onBack: () -> Unit,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "HISTORIAL DE ENVÍOS",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (canClearLogs) {
            IconButton(
                onClick = onClearClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Limpiar historial",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SyncLogCard(
    item: SyncLogUiItem
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = White,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = BorderGray,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = item.inventoryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "Evento: ${item.eventType}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Capturas: ${item.captures}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Estado inventario: ${item.status}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Resultado: ${item.result}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Modo: ${item.mode}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Hora: ${item.sentAt}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            if (!item.message.isNullOrBlank()) {
                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }
}