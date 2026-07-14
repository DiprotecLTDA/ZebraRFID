package com.diprotec.inventariozebratc27.ui.rfid.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.rfid.RfidBeeperVolume
import com.diprotec.inventariozebratc27.ui.components.AppActionButton
import com.diprotec.inventariozebratc27.ui.components.AppButtonStyle
import com.diprotec.inventariozebratc27.ui.components.AppCard
import com.diprotec.inventariozebratc27.ui.components.AppTopBar
import com.diprotec.inventariozebratc27.ui.components.SectionTitle
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.theme.LabelGray
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary

@Composable
fun RfidSettingsScreen(
    onBack: () -> Unit,
    viewModel: RfidSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.settingsContentWidth)
                .fillMaxSize()
                .testTag("rfid_settings_screen")
        ) {
            AppTopBar(
                title = "CONFIGURACIÓN RFID",
                navigationIcon = Icons.Default.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.screenPadding)
            ) {
                SectionTitle(text = "Potencia de antena")

                Spacer(modifier = Modifier.size(Dimens.space8))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SettingSlider(
                        title = "Inventario",
                        description = "Potencia usada al capturar etiquetas. " +
                                "Bajarla reduce el alcance y puede hacer que no se " +
                                "lean etiquetas lejanas.",
                        value = uiState.inventoryPowerPercent,
                        onValueChange = viewModel::onInventoryPowerChanged,
                        onValueChangeFinished = viewModel::onInventoryPowerCommitted,
                        testTag = "slider_rfid_power_inventory"
                    )

                    Spacer(modifier = Modifier.size(Dimens.space16))

                    SettingSlider(
                        title = "Localización",
                        description = "Potencia usada al buscar una etiqueta. " +
                                "Una potencia alta satura la señal y el porcentaje de " +
                                "proximidad deja de variar al acercarse.",
                        value = uiState.locatePowerPercent,
                        onValueChange = viewModel::onLocatePowerChanged,
                        onValueChangeFinished = viewModel::onLocatePowerCommitted,
                        testTag = "slider_rfid_power_locate"
                    )
                }

                Spacer(modifier = Modifier.size(Dimens.space24))

                SectionTitle(text = "Sonido")

                Spacer(modifier = Modifier.size(Dimens.space8))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Beep del lector",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Pitido físico del lector RFID al leer una etiqueta.",
                        color = LabelGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.size(Dimens.space12))

                    BeeperVolumeSelector(
                        selected = uiState.beeperVolume,
                        onSelected = viewModel::onBeeperVolumeSelected
                    )

                    Spacer(modifier = Modifier.size(Dimens.space16))

                    SettingSlider(
                        title = "Tono de proximidad",
                        description = "Volumen del tono que emite la app al buscar " +
                                "una etiqueta. En 0 % queda en silencio.",
                        value = uiState.locateToneVolumePercent,
                        onValueChange = viewModel::onLocateToneVolumeChanged,
                        onValueChangeFinished = viewModel::onLocateToneVolumeCommitted,
                        testTag = "slider_rfid_locate_tone"
                    )
                }

                Spacer(modifier = Modifier.size(Dimens.space24))

                AppActionButton(
                    text = "Restaurar valores por defecto",
                    onClick = viewModel::restoreDefaults,
                    style = AppButtonStyle.OUTLINE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_rfid_settings_defaults")
                )

                Spacer(modifier = Modifier.size(Dimens.space12))

                Text(
                    text = "Los cambios se aplican la próxima vez que se inicie una " +
                            "lectura o una búsqueda.",
                    color = LabelGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SettingSlider(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "$value %",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = description,
            color = LabelGray,
            style = MaterialTheme.typography.bodySmall
        )

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..100f,
            steps = 19,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun BeeperVolumeSelector(
    selected: RfidBeeperVolume,
    onSelected: (RfidBeeperVolume) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
    ) {
        RfidBeeperVolume.entries.forEach { volume ->
            AppActionButton(
                text = volume.label,
                onClick = { onSelected(volume) },
                style = if (volume == selected) {
                    AppButtonStyle.PRIMARY
                } else {
                    AppButtonStyle.OUTLINE
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_beeper_${volume.name.lowercase()}")
            )
        }
    }
}
