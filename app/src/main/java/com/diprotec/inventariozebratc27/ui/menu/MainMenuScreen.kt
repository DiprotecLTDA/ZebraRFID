package com.diprotec.inventariozebratc27.ui.menu

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
                .padding(horizontal = 20.dp, vertical = 14.dp),
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

                    Spacer(modifier = Modifier.size(6.dp))

                    SessionTimeIndicator(
                        value = sessionRemainingText
                    )
                }

                if (showWorkerTrafficLight) {
                    Spacer(modifier = Modifier.size(8.dp))

                    WorkerTrafficLight()
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
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
                    TextButton(
                        onClick = {
                            showLogoutDialog = false

                            viewModel.logout(
                                onDone = onSalir
                            )
                        }
                    ) {
                        Text("Sí")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                        }
                    ) {
                        Text("No")
                    }
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
        AppConnectionMode.CHECKING -> Color(0xFFFFA000)
        AppConnectionMode.ONLINE_API -> Color(0xFF2E7D32)
        AppConnectionMode.LOCAL_ROOM -> Color(0xFFC63428)
    }

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
                    .background(Color(0xFFC63428), CircleShape)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "Sesión:",
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

@Composable
private fun BlockingPendingSyncOverlay() {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
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
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Actualizando lista",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(12.dp))

                Text(
                    text = "Sincronizando inventarios pendientes...",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.size(18.dp))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(12.dp))

                Text(
                    text = "Por favor espera",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}