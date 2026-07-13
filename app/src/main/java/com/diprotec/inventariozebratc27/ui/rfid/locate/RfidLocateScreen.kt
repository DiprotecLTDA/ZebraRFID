package com.diprotec.inventariozebratc27.ui.rfid.locate

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.ButtonRedDark
import com.diprotec.inventariozebratc27.ui.theme.InventoryMenuButton
import com.diprotec.inventariozebratc27.ui.theme.LabelGray
import com.diprotec.inventariozebratc27.ui.theme.TealPrimary
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White

@Composable
fun RfidLocateScreen(
    onBack: () -> Unit,
    viewModel: RfidLocateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        RfidLocateTopBar()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchInput,
                    onValueChange = viewModel::onSearchInputChange,
                    enabled = !uiState.locating &&
                            !uiState.connecting &&
                            !uiState.searchingProduct &&
                            !uiState.checkingReader,
                    singleLine = true,
                    label = {
                        Text("Código producto, secundario o descripción")
                    },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii
                    ),
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
                        disabledTextColor = TextPrimary,
                        cursorColor = TealPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                )

                SelectedProductCard(
                    option = uiState.selectedOption,
                    epcToLocate = uiState.generatedEpcToLocate
                )

                TooManyOptionsCard(
                    visible = uiState.tooManyOptions,
                    totalOptionsFound = uiState.totalOptionsFound
                )

                SearchOptionsList(
                    options = uiState.options,
                    totalOptionsFound = uiState.totalOptionsFound,
                    enabled = !uiState.locating &&
                            !uiState.connecting &&
                            !uiState.searchingProduct &&
                            !uiState.checkingReader,
                    onSelect = viewModel::selectOption
                )

                LocateDistanceCard(
                    distance = uiState.relativeDistance,
                    message = uiState.message,
                    readCount = uiState.locateReadCount,
                    lastLocatedEpc = uiState.lastLocatedEpc
                )

                if (!uiState.error.isNullOrBlank()) {
                    Text(
                        text = uiState.error.orEmpty(),
                        color = ButtonRedDark,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    InventoryMenuButton(
                        text = when {
                            uiState.searchingProduct -> "Buscando..."
                            uiState.connecting -> "Conectando..."
                            uiState.locating -> "Leyendo..."
                            else -> "Buscar"
                        },
                        icon = Icons.Default.PlayArrow,
                        enabled = !uiState.locating &&
                                !uiState.connecting &&
                                !uiState.searchingProduct &&
                                !uiState.checkingReader,
                        loading = uiState.connecting || uiState.searchingProduct,
                        onClick = {
                            viewModel.searchProductAndPrepareLocate()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    InventoryMenuButton(
                        text = "Detener",
                        icon = Icons.Default.Stop,
                        enabled = uiState.locating ||
                                uiState.connecting ||
                                uiState.searchingProduct,
                        onClick = {
                            viewModel.stopLocate()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                InventoryMenuButton(
                    text = when {
                        uiState.checkingReader -> "Verificando..."
                        else -> "Verificar lector"
                    },
                    icon = Icons.Default.BluetoothSearching,
                    enabled = !uiState.connecting &&
                            !uiState.searchingProduct &&
                            !uiState.checkingReader,
                    loading = uiState.checkingReader,
                    onClick = {
                        viewModel.verifyReaderConnection()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RfidLocateTopBar() {
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
            text = "BUSCAR ETIQUETA RFID",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SelectedProductCard(
    option: RfidProductSearchOption?,
    epcToLocate: String?
) {
    if (option == null) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(18.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Producto seleccionado",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Código: ${option.productCode}",
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )

        if (!option.secondaryCode.isNullOrBlank()) {
            Text(
                text = "Código secundario: ${option.secondaryCode}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!option.description.isNullOrBlank()) {
            Text(
                text = option.description,
                color = LabelGray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = "Buscando por ${option.rfidSourceLabel}: ${option.rfidSourceValue}",
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        if (!epcToLocate.isNullOrBlank()) {
            Text(
                text = "EPC enviado al lector: $epcToLocate",
                color = LabelGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TooManyOptionsCard(
    visible: Boolean,
    totalOptionsFound: Int
) {
    if (!visible) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(18.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Demasiadas coincidencias",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Se encontraron $totalOptionsFound opciones posibles.",
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Ingrese un código más específico para evitar una lista demasiado extensa.",
            color = LabelGray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SearchOptionsList(
    options: List<RfidProductSearchOption>,
    totalOptionsFound: Int,
    enabled: Boolean,
    onSelect: (RfidProductSearchOption) -> Unit
) {
    if (options.isEmpty()) {
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Coincidencias encontradas ($totalOptionsFound)",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        options.forEach { option ->
            RfidSearchOptionItem(
                option = option,
                enabled = enabled,
                onClick = {
                    onSelect(option)
                }
            )
        }
    }
}

@Composable
private fun RfidSearchOptionItem(
    option: RfidProductSearchOption,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(16.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = option.rfidSourceLabel,
            color = TealPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = option.rfidSourceValue,
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Producto: ${option.productCode}",
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall
        )

        if (!option.secondaryCode.isNullOrBlank()) {
            Text(
                text = "Secundario: ${option.secondaryCode}",
                color = LabelGray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!option.description.isNullOrBlank()) {
            Text(
                text = option.description,
                color = LabelGray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = "EPC a localizar: ${option.epcToLocate}",
            color = LabelGray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun LocateDistanceCard(
    distance: Int,
    message: String,
    readCount: Int,
    lastLocatedEpc: String?
) {
    val safeDistance = distance.coerceIn(0, 100)

    val animatedProgress by animateFloatAsState(
        targetValue = safeDistance / 100f,
        animationSpec = tween(durationMillis = 180),
        label = "rfid_locate_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(18.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Proximidad",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LinearProgressIndicator(
            progress = {
                animatedProgress
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "$safeDistance%",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = message,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Lecturas recibidas: $readCount",
            color = LabelGray,
            style = MaterialTheme.typography.bodySmall
        )

        if (!lastLocatedEpc.isNullOrBlank()) {
            Text(
                text = "Último EPC localizado: $lastLocatedEpc",
                color = LabelGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}