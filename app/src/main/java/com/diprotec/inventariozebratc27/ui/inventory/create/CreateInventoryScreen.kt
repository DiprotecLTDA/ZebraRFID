package com.diprotec.inventariozebratc27.ui.inventory.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryReadingMode
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
fun CreateInventoryScreen(
    onCreated: (Long, InventoryReadingMode) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateInventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var expandedInventario by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.checkRfidAvailability()
    }

    val canContinue =
        !uiState.creating &&
                uiState.selectedInventarioId.isNotBlank() &&
                uiState.selectedInventarioDescripcion.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        CreateInventoryTopBar(
            onBack = onBack
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedInventario,
                        onExpandedChange = {
                            expandedInventario = !expandedInventario
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedInventarioDescripcion,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = {
                                FloatingFieldLabel("Seleccione inventario")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expandedInventario
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .heightIn(min = 72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
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
                        )

                        DropdownMenu(
                            expanded = expandedInventario,
                            onDismissRequest = {
                                expandedInventario = false
                            }
                        ) {
                            uiState.inventarios.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(item.descripcion)
                                    },
                                    onClick = {
                                        viewModel.onInventarioSelected(item.id)
                                        expandedInventario = false
                                    }
                                )
                            }
                        }
                    }

                    InventoryDateInfo(
                        title = "Valido desde:",
                        value = uiState.selectedInicioTexto.ifBlank { "Sin inicio" }
                    )

                    InventoryDateInfo(
                        title = "Fecha maxima para realizar:",
                        value = uiState.selectedTerminoTexto.ifBlank { "Sin termino" }
                    )

                    ReadingModeSelector(
                        selectedMode = uiState.selectedReadingMode,
                        checkingRfidAvailability = uiState.checkingRfidAvailability,
                        rfidAvailable = uiState.rfidAvailable,
                        rfidAvailabilityMessage = uiState.rfidAvailabilityMessage,
                        onModeSelected = viewModel::onReadingModeSelected
                    )

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            color = ButtonRedDark,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    InventoryMenuButton(
                        text = if (uiState.creating) {
                            "Creando..."
                        } else {
                            "Aceptar"
                        },
                        icon = Icons.Default.CheckCircle,
                        enabled = canContinue,
                        loading = uiState.creating,
                        onClick = {
                            viewModel.createSelectedInventory(
                                onCreated = onCreated
                            )
                        },
                        modifier = Modifier.fillMaxWidth(0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateInventoryTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "INVENTARIO",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReadingModeSelector(
    selectedMode: InventoryReadingMode,
    checkingRfidAvailability: Boolean,
    rfidAvailable: Boolean,
    rfidAvailabilityMessage: String?,
    onModeSelected: (InventoryReadingMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FloatingFieldLabel("Tipo de lectura")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            InventoryMenuButton(
                text = if (selectedMode == InventoryReadingMode.LASER) {
                    "Láser seleccionado"
                } else {
                    "Láser"
                },
                icon = Icons.Default.QrCodeScanner,
                onClick = {
                    onModeSelected(InventoryReadingMode.LASER)
                },
                enabled = selectedMode != InventoryReadingMode.LASER,
                modifier = Modifier.weight(1f)
            )

            InventoryMenuButton(
                text = when {
                    checkingRfidAvailability -> "Verificando RFID..."
                    selectedMode == InventoryReadingMode.RFID -> "RFID seleccionado"
                    else -> "RFID"
                },
                icon = Icons.Default.QrCodeScanner,
                onClick = {
                    onModeSelected(InventoryReadingMode.RFID)
                },
                enabled = rfidAvailable &&
                        !checkingRfidAvailability &&
                        selectedMode != InventoryReadingMode.RFID,
                modifier = Modifier.weight(1f)
            )
        }

        if (!rfidAvailabilityMessage.isNullOrBlank()) {
            Text(
                text = rfidAvailabilityMessage,
                color = ButtonRedDark,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = when (selectedMode) {
                InventoryReadingMode.LASER ->
                    "Modo seleccionado: lectura con láser / código de barras."

                InventoryReadingMode.RFID ->
                    "Modo seleccionado: lectura RFID con RFD4031."
            },
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InventoryDateInfo(
    title: String,
    value: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(White, RoundedCornerShape(16.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Text(
                text = value,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 28.dp)
                .background(Background)
                .padding(horizontal = 4.dp)
        ) {
            FloatingFieldLabel(title)
        }
    }
}

@Composable
private fun FloatingFieldLabel(
    text: String
) {
    Text(
        text = text,
        color = LabelGray,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(Background)
            .padding(horizontal = 4.dp)
    )
}