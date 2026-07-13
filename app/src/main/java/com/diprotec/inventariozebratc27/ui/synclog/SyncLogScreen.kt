package com.diprotec.inventariozebratc27.ui.synclog

import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.components.AppActionButton
import com.diprotec.inventariozebratc27.ui.components.AppButtonStyle
import com.diprotec.inventariozebratc27.ui.components.AppTopBar

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
                AppActionButton(
                    text = "Limpiar",
                    onClick = {
                        vm.clearAll()
                        showClearDialog = false
                    }
                )
            },
            dismissButton = {
                AppActionButton(
                    text = "Cancelar",
                    onClick = {
                        showClearDialog = false
                    },
                    style = AppButtonStyle.OUTLINE
                )
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
                    .widthIn(max = Dimens.listContentWidth)
                    .padding(horizontal = Dimens.space20, vertical = Dimens.space18)
            ) {
                if (!uiState.message.isNullOrBlank()) {
                    Text(
                        text = uiState.message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ButtonRedDark
                    )

                    Spacer(modifier = Modifier.height(Dimens.space12))
                }

                if (!uiState.canClearLogs) {
                    Text(
                        text = "La eliminación del historial no está permitida para este perfil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(Dimens.space12))
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
                        verticalArrangement = Arrangement.spacedBy(Dimens.space12)
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
    AppTopBar(
        title = "HISTORIAL DE ENVÍOS",
        actions = {
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
    )
}

@Composable
private fun SyncLogCard(
    item: SyncLogUiItem
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = White,
        tonalElevation = Dimens.borderWidth,
        shadowElevation = Dimens.borderWidth,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = Dimens.borderWidth,
                color = BorderGray,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space16)
        ) {
            Text(
                text = item.inventoryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.size(Dimens.space8))

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
                Spacer(modifier = Modifier.size(Dimens.space8))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }
}
