package com.diprotec.inventariozebratc27.ui.rfid.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.data.local.datastore.RfidSettingsDefaults
import com.diprotec.inventariozebratc27.rfid.RfidBeeperVolume
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RfidSettingsUiState(
    val inventoryPowerPercent: Int = RfidSettingsDefaults.POWER_INVENTORY_PERCENT,
    val locatePowerPercent: Int = RfidSettingsDefaults.POWER_LOCATE_PERCENT,
    val beeperVolume: RfidBeeperVolume = RfidBeeperVolume.DEFAULT,
    val locateToneVolumePercent: Int = RfidSettingsDefaults.LOCATE_TONE_VOLUME_PERCENT
)

@HiltViewModel
class RfidSettingsViewModel @Inject constructor(
    private val settings: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RfidSettingsUiState())
    val uiState: StateFlow<RfidSettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = RfidSettingsUiState(
            inventoryPowerPercent = settings.rfidPowerInventoryPercent.value,
            locatePowerPercent = settings.rfidPowerLocatePercent.value,
            beeperVolume = settings.rfidBeeperVolume.value,
            locateToneVolumePercent = settings.rfidLocateToneVolumePercent.value
        )
    }

    /** Actualiza el valor en pantalla mientras se arrastra el control. */
    fun onInventoryPowerChanged(value: Int) {
        _uiState.value = _uiState.value.copy(
            inventoryPowerPercent = value.coerceIn(0, 100)
        )
    }

    /** Persiste al soltar el control, para no escribir en cada paso del arrastre. */
    fun onInventoryPowerCommitted() {
        viewModelScope.launch {
            settings.saveRfidPowerInventoryPercent(
                _uiState.value.inventoryPowerPercent
            )
        }
    }

    fun onLocatePowerChanged(value: Int) {
        _uiState.value = _uiState.value.copy(
            locatePowerPercent = value.coerceIn(0, 100)
        )
    }

    fun onLocatePowerCommitted() {
        viewModelScope.launch {
            settings.saveRfidPowerLocatePercent(
                _uiState.value.locatePowerPercent
            )
        }
    }

    fun onBeeperVolumeSelected(value: RfidBeeperVolume) {
        _uiState.value = _uiState.value.copy(
            beeperVolume = value
        )

        viewModelScope.launch {
            settings.saveRfidBeeperVolume(value)
        }
    }

    fun onLocateToneVolumeChanged(value: Int) {
        _uiState.value = _uiState.value.copy(
            locateToneVolumePercent = value.coerceIn(0, 100)
        )
    }

    fun onLocateToneVolumeCommitted() {
        viewModelScope.launch {
            settings.saveRfidLocateToneVolumePercent(
                _uiState.value.locateToneVolumePercent
            )
        }
    }

    fun restoreDefaults() {
        _uiState.value = RfidSettingsUiState()

        viewModelScope.launch {
            settings.saveRfidPowerInventoryPercent(
                RfidSettingsDefaults.POWER_INVENTORY_PERCENT
            )
            settings.saveRfidPowerLocatePercent(
                RfidSettingsDefaults.POWER_LOCATE_PERCENT
            )
            settings.saveRfidBeeperVolume(RfidBeeperVolume.DEFAULT)
            settings.saveRfidLocateToneVolumePercent(
                RfidSettingsDefaults.LOCATE_TONE_VOLUME_PERCENT
            )
        }
    }
}
