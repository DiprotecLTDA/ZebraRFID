package com.diprotec.inventariozebratc27.ui.inventory.rfid

import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.components.AppTextField

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.remember
import com.diprotec.inventariozebratc27.ui.theme.OverlayScrim
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.rfid.RfidConnectionState
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.ButtonRedDark
import com.diprotec.inventariozebratc27.ui.theme.InventoryMenuButton
import com.diprotec.inventariozebratc27.ui.theme.LabelGray
import com.diprotec.inventariozebratc27.ui.theme.TealPrimary
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryRfidScreen(
    inventoryId: Long,
    onBack: () -> Unit,
    onViewList: () -> Unit,
    onLeavePending: () -> Unit,
    onFinishInventory: () -> Unit,
    viewModel: InventoryRfidViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var expandedMedida by rememberSaveable {
        mutableStateOf(false)
    }

    var expandedUbicacion by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(inventoryId) {
        viewModel.loadInventory(inventoryId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.formContentWidth)
                .fillMaxSize()
                .padding(horizontal = Dimens.space20, vertical = Dimens.space12)
                .testTag("inventory_rfid_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedUbicacion,
                onExpandedChange = {
                    expandedUbicacion = !expandedUbicacion
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                FloatingLabelContainer(
                    label = "Seleccione ubicación",
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                ) {
                    AppTextField(
                        value = uiState.selectedUbicacionName,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedUbicacion
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.buttonHeight)
                            .testTag("input_rfid_ubicacion")
                    )
                }

                DropdownMenu(
                    expanded = expandedUbicacion,
                    onDismissRequest = {
                        expandedUbicacion = false
                    }
                ) {
                    uiState.ubicaciones.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Text(item.nombre)
                            },
                            onClick = {
                                viewModel.onUbicacionSelected(item.id)
                                expandedUbicacion = false
                            }
                        )
                    }
                }
            }

            FieldSpacer()

            UnitDropdownField(
                expanded = expandedMedida,
                onExpandedChange = {
                    expandedMedida = it
                },
                selectedUnitName = uiState.selectedUnitName,
                units = uiState.units,
                onUnitSelected = { unitId ->
                    viewModel.onUnitSelected(unitId)
                    expandedMedida = false
                },
                onDismiss = {
                    expandedMedida = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_rfid_unit_measure")
            )

            FieldSpacer()

            RfidStatusPanel(
                inventoryName = uiState.inventoryName,
                connectionState = uiState.connectionState,
                isReading = uiState.isReading,
                lastEpc = uiState.lastEpc,
                lastGs1Type = uiState.lastGs1Type,
                lastBarcodeSaved = uiState.lastBarcodeSaved,
                totalReadsCount = uiState.totalReadsCount,
                validReadsCount = uiState.validReadsCount,
                duplicatedReadsCount = uiState.duplicatedReadsCount
            )

            FieldSpacer()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                verticalAlignment = Alignment.Top
            ) {
                InventoryMenuButton(
                    text = if (uiState.verifyingConnection) {
                        "Verificando..."
                    } else {
                        "Verificar"
                    },
                    icon = Icons.Default.CheckCircle,
                    enabled = !uiState.verifyingConnection &&
                            !uiState.startingReading &&
                            !uiState.stoppingReading &&
                            !uiState.isReading,
                    loading = uiState.verifyingConnection,
                    onClick = {
                        viewModel.verifyConnection()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_verify_rfid_connection")
                )

                InventoryMenuButton(
                    text = if (uiState.startingReading) {
                        "Iniciando..."
                    } else {
                        "Iniciar"
                    },
                    icon = Icons.Default.PlayArrow,
                    enabled = !uiState.isReading &&
                            !uiState.startingReading &&
                            !uiState.verifyingConnection &&
                            !uiState.stoppingReading,
                    loading = uiState.startingReading,
                    onClick = {
                        viewModel.startReading()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_start_rfid_reading")
                )

                InventoryMenuButton(
                    text = when {
                        uiState.processingCaptures -> "Procesando..."
                        uiState.stoppingReading -> "Deteniendo..."
                        else -> "Detener"
                    },
                    icon = Icons.Default.Stop,
                    enabled = uiState.isReading &&
                            !uiState.stoppingReading,
                    loading = uiState.stoppingReading,
                    onClick = {
                        viewModel.stopReading()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_stop_rfid_reading")
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.space6))

                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = ButtonRedDark,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.space12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                verticalAlignment = Alignment.Top
            ) {
                InventoryMenuButton(
                    text = "Ver",
                    icon = Icons.Default.ListAlt,
                    onClick = onViewList,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_rfid_view_inventory")
                )

                InventoryMenuButton(
                    text = "Pendiente",
                    icon = Icons.Default.PauseCircle,
                    onClick = {
                        viewModel.leavePending(
                            onLeavePending = onLeavePending
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_rfid_leave_pending")
                )

                InventoryMenuButton(
                    text = "Finalizar",
                    icon = Icons.Default.Inventory2,
                    onClick = {
                        viewModel.finalizeInventory(
                            inventoryId = inventoryId,
                            onFinished = onFinishInventory
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_rfid_finish_inventory")
                )
            }
        }

        if (uiState.processingCaptures) {
            ProcessingCapturesOverlay()
        }
    }
}

/**
 * Bloquea la pantalla mientras se persisten las capturas encoladas al detener la
 * lectura, para que el operador no salga creyendo que el proceso ya terminó.
 */
@Composable
private fun ProcessingCapturesOverlay() {
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
                    text = "Procesando capturas...",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(Dimens.space12))

                Text(
                    text = "Guardando las lecturas pendientes.",
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

@Composable
private fun FieldSpacer() {
    Spacer(modifier = Modifier.height(Dimens.space8))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdownField(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedUnitName: String,
    units: List<RfidUnidadMedidaOption>,
    onUnitSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            onExpandedChange(!expanded)
        },
        modifier = modifier
    ) {
        FloatingLabelContainer(
            label = "Unidad medida",
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        ) {
            AppTextField(
                value = selectedUnitName,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.buttonHeight)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            units.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.nombre)
                    },
                    onClick = {
                        onUnitSelected(item.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun RfidStatusPanel(
    inventoryName: String,
    connectionState: RfidConnectionState,
    isReading: Boolean,
    lastEpc: String,
    lastGs1Type: String,
    lastBarcodeSaved: String,
    totalReadsCount: Int,
    validReadsCount: Int,
    duplicatedReadsCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, MaterialTheme.shapes.medium)
            .border(Dimens.borderWidth, BorderGray, MaterialTheme.shapes.medium)
            .padding(horizontal = Dimens.space16, vertical = Dimens.space14),
        verticalArrangement = Arrangement.spacedBy(Dimens.space8)
    ) {
        if (inventoryName.isNotBlank()) {
            Text(
                text = inventoryName,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Estado RFID",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        RfidInfoRow(
            title = "Conexión:",
            value = connectionState.toUserText()
        )

        RfidInfoRow(
            title = "Lectura:",
            value = if (isReading) {
                "Leyendo"
            } else {
                "Detenida"
            }
        )

        RfidInfoRow(
            title = "Último EPC:",
            value = lastEpc.ifBlank { "Sin lecturas" }
        )

        RfidInfoRow(
            title = "Tipo GS1:",
            value = lastGs1Type.ifBlank { "Sin lectura" }
        )

        RfidInfoRow(
            title = "Código:",
            value = lastBarcodeSaved.ifBlank { "Sin lectura" }
        )

        RfidInfoRow(
            title = "Totales:",
            value = totalReadsCount.toString()
        )

        RfidInfoRow(
            title = "Válidas:",
            value = validReadsCount.toString()
        )

        RfidInfoRow(
            title = "Duplicadas:",
            value = duplicatedReadsCount.toString()
        )
    }
}

@Composable
private fun RfidInfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title,
            color = LabelGray,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(Dimens.loginLogoHeight)
        )

        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FloatingLabelContainer(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.space10)
        ) {
            content()
        }

        Text(
            text = label,
            color = LabelGray,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier
                .padding(start = Dimens.space16)
                .background(Background)
                .padding(horizontal = Dimens.space7)
                .zIndex(1f)
        )
    }
}

@Composable
private fun inventoryTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = White,
    unfocusedContainerColor = White,
    disabledContainerColor = White,
    focusedBorderColor = TealPrimary,
    unfocusedBorderColor = BorderGray,
    disabledBorderColor = BorderGray,
    focusedLabelColor = LabelGray,
    unfocusedLabelColor = LabelGray,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = TealPrimary
)

private fun RfidConnectionState.toUserText(): String {
    return when (this) {
        RfidConnectionState.DISCONNECTED -> "Desconectado"
        RfidConnectionState.CONNECTING -> "Conectando"
        RfidConnectionState.CONNECTED -> "Conectado"
        RfidConnectionState.READING -> "Leyendo"
        RfidConnectionState.PAUSED -> "Pausado"
        RfidConnectionState.ERROR -> "Error"
    }
}
