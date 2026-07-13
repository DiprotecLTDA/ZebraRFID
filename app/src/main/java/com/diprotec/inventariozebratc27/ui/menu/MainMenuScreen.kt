package com.diprotec.inventariozebratc27.ui.menu

import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.theme.StatusError
import com.diprotec.inventariozebratc27.ui.theme.StatusOnline
import com.diprotec.inventariozebratc27.ui.theme.StatusWarning
import com.diprotec.inventariozebratc27.ui.theme.OverlayScrim
import com.diprotec.inventariozebratc27.ui.components.AppActionButton
import com.diprotec.inventariozebratc27.ui.components.AppButtonStyle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.ui.common.AppFloatingMessage
import com.diprotec.inventariozebratc27.ui.connection.AppConnectionMode
import com.diprotec.inventariozebratc27.ui.syncstatus.WorkerTrafficLight
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.InventoryMenuButton
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White

@Composable
fun MainMenuScreen(
    onRealizarInventario: () -> Unit,
    onContinuarInventario: () -> Unit,
    onHistorialEnvios: () -> Unit,
    onConsumoDatos: () -> Unit,
    onBuscarRfid: () -> Unit,
    onAcercaDe: () -> Unit,
    onSalir: () -> Unit,
    viewModel: MainMenuViewModel = hiltViewModel()
) {
    val currentUsername by viewModel.currentUsername.collectAsState()
    val pendingSyncState by viewModel.pendingSyncState
    val sessionRemainingText by viewModel.sessionRemainingText

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    val showDataUsage = currentUsername == "76001910-0"

    val showWorkerTrafficLight =
        pendingSyncState.syncing ||
                (
                        pendingSyncState.connectionMode != AppConnectionMode.ONLINE_API &&
                                pendingSyncState.connectionMode != AppConnectionMode.CHECKING
                        )

    BackHandler {
        showLogoutDialog = true
    }

    LaunchedEffect(pendingSyncState.message) {
        pendingSyncState.message?.let { message ->
            AppFloatingMessage.info(message)
            viewModel.clearPendingSyncMessage()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.space20, vertical = Dimens.space14),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    MainMenuConnectionIndicator(
                        mode = pendingSyncState.connectionMode
                    )

                    Spacer(modifier = Modifier.size(Dimens.space6))

                    SessionTimeIndicator(
                        value = sessionRemainingText
                    )
                }

                if (showWorkerTrafficLight) {
                    Spacer(modifier = Modifier.size(Dimens.space8))

                    WorkerTrafficLight()
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space18))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = Dimens.formContentWidth),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space18),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        InventoryMenuButton(
                            text = "Realizar inventario",
                            icon = Icons.Default.Inventory2,
                            onClick = onRealizarInventario,
                            modifier = Modifier.weight(1f)
                        )

                        InventoryMenuButton(
                            text = "Continuar inventario",
                            icon = Icons.Default.QrCodeScanner,
                            onClick = onContinuarInventario,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        InventoryMenuButton(
                            text = "Sincronizar pendientes",
                            icon = Icons.Default.PlaylistAddCheckCircle,
                            onClick = {
                                viewModel.syncPendingInventories()
                            },
                            enabled = pendingSyncState.canSyncPending,
                            loading = pendingSyncState.syncing,
                            modifier = Modifier.weight(1f)
                        )

                        InventoryMenuButton(
                            text = "Acerca de",
                            icon = Icons.Default.Help,
                            onClick = onAcercaDe,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        InventoryMenuButton(
                            text = "Historial de envíos",
                            icon = Icons.Default.History,
                            onClick = onHistorialEnvios,
                            modifier = Modifier.weight(1f)
                        )

                        InventoryMenuButton(
                            text = "Buscar RFID",
                            icon = Icons.Default.QrCodeScanner,
                            onClick = onBuscarRfid,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        InventoryMenuButton(
                            text = "Salir",
                            icon = Icons.Default.Logout,
                            onClick = {
                                showLogoutDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (showDataUsage) {
                            InventoryMenuButton(
                                text = "Consumo de datos",
                                icon = Icons.Default.DataUsage,
                                onClick = onConsumoDatos,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (pendingSyncState.syncing) {
            BlockingPendingSyncOverlay()
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = {
                    showLogoutDialog = false
                },
                title = {
                    Text("Cerrar sesión")
                },
                text = {
                    Text("¿Desea cerrar sesión?")
                },
                confirmButton = {
                    AppActionButton(
                        text = "Sí",
                        onClick = {
                            showLogoutDialog = false

                            viewModel.logout(
                                onDone = onSalir
                            )
                        }
                    )
                },
                dismissButton = {
                    AppActionButton(
                        text = "No",
                        onClick = {
                            showLogoutDialog = false
                        },
                        style = AppButtonStyle.OUTLINE
                    )
                }
            )
        }
    }
}

@Composable
private fun MainMenuConnectionIndicator(
    mode: AppConnectionMode,
    modifier: Modifier = Modifier
) {
    val label = when (mode) {
        AppConnectionMode.CHECKING -> "Verificando conexión"
        AppConnectionMode.ONLINE_API -> "Con conexión"
        AppConnectionMode.LOCAL_ROOM -> "Sin conexión"
    }

    val dotColor = when (mode) {
        AppConnectionMode.CHECKING -> StatusWarning
        AppConnectionMode.ONLINE_API -> StatusOnline
        AppConnectionMode.LOCAL_ROOM -> StatusError
    }

    Surface(
        modifier = modifier
            .widthIn(min = Dimens.statusMinWidth)
            .heightIn(min = Dimens.statusHeight),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = Dimens.borderWidth,
        shadowElevation = Dimens.borderWidth
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.space12)
                    .background(dotColor, CircleShape)
            )

            Spacer(modifier = Modifier.size(Dimens.space8))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SessionTimeIndicator(
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(min = Dimens.statusMinWidth)
            .heightIn(min = Dimens.statusHeight),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = Dimens.borderWidth,
        shadowElevation = Dimens.borderWidth
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.space12)
                    .background(StatusError, CircleShape)
            )

            Spacer(modifier = Modifier.size(Dimens.space8))

            Text(
                text = "Sesión:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.size(Dimens.space6))

            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BlockingPendingSyncOverlay() {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayScrim)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.space32),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = Dimens.space8
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.space22),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Actualizando lista",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(Dimens.space12))

                Text(
                    text = "Sincronizando inventarios pendientes...",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.size(Dimens.space18))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(Dimens.space12))

                Text(
                    text = "Por favor espera",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
