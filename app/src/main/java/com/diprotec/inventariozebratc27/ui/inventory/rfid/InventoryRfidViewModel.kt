package com.diprotec.inventariozebratc27.ui.inventory.rfid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.core.session.SessionManager
import com.diprotec.inventariozebratc27.data.local.dao.UnitMeasureDao
import com.diprotec.inventariozebratc27.data.repository.InventoryRepository
import com.diprotec.inventariozebratc27.rfid.RfidConnectionState
import com.diprotec.inventariozebratc27.rfid.ZebraRfidManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RfidUbicacionOption(
    val id: String,
    val nombre: String
)

data class RfidUnidadMedidaOption(
    val id: String,
    val nombre: String
)

data class InventoryRfidUiState(
    val inventoryId: Long = 0L,
    val inventoryName: String = "",

    val ubicaciones: List<RfidUbicacionOption> = emptyList(),
    val selectedUbicacionId: String = "",
    val selectedUbicacionName: String = "",

    val units: List<RfidUnidadMedidaOption> = emptyList(),
    val selectedUnitId: String = "",
    val selectedUnitName: String = "",

    val connectionState: RfidConnectionState = RfidConnectionState.DISCONNECTED,
    val isReading: Boolean = false,

    val lastEpc: String = "",
    val lastGs1Type: String = "",
    val lastBarcodeSaved: String = "",

    val totalReadsCount: Int = 0,
    val validReadsCount: Int = 0,
    val duplicatedReadsCount: Int = 0,

    val verifyingConnection: Boolean = false,
    val startingReading: Boolean = false,
    val stoppingReading: Boolean = false,

    val errorMessage: String? = null
)

@HiltViewModel
class InventoryRfidViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val unitMeasureDao: UnitMeasureDao,
    private val session: SessionManager,
    private val rfidManager: ZebraRfidManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryRfidUiState())
    val uiState: StateFlow<InventoryRfidUiState> = _uiState.asStateFlow()

    private val epcMutex = Mutex()

    private var loadJob: Job? = null
    private var tagsJob: Job? = null

    init {
        observeRfidState()
        observeRfidTags()
    }

    fun loadInventory(inventoryId: Long) {
        if (inventoryId <= 0L) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Inventario inválido"
            )
            return
        }

        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            val inventory = withContext(Dispatchers.IO) {
                repository.getInventoryById(inventoryId)
            }

            if (inventory == null) {
                _uiState.value = _uiState.value.copy(
                    inventoryId = inventoryId,
                    errorMessage = "No se encontró el inventario"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                inventoryId = inventoryId,
                inventoryName = inventory.name,
                errorMessage = null
            )

            combine(
                repository.observeUbicacionesActivas(),
                unitMeasureDao.observeAll()
            ) { ubicaciones, unidades ->
                val ubicacionOptions: List<RfidUbicacionOption> = ubicaciones
                    .map { ubicacion ->
                        RfidUbicacionOption(
                            id = ubicacion.id.orEmpty(),
                            nombre = ubicacion.nombre.orEmpty()
                        )
                    }
                    .filter { option ->
                        option.id.isNotBlank() && option.nombre.isNotBlank()
                    }

                val unidadOptions: List<RfidUnidadMedidaOption> = unidades
                    .filter { unidad ->
                        !unidad.estado
                    }
                    .map { unidad ->
                        RfidUnidadMedidaOption(
                            id = unidad.id,
                            nombre = unidad.nombre.orEmpty()
                        )
                    }
                    .filter { option ->
                        option.id.isNotBlank() && option.nombre.isNotBlank()
                    }

                Pair(
                    ubicacionOptions,
                    unidadOptions
                )
            }.collectLatest { result ->
                val ubicaciones = result.first
                val unidades = result.second
                val current = _uiState.value

                val selectedUbicacion = ubicaciones.firstOrNull { ubicacion ->
                    ubicacion.id == current.selectedUbicacionId
                } ?: ubicaciones.firstOrNull()

                val selectedUnit = unidades.firstOrNull { unidad ->
                    unidad.id == current.selectedUnitId
                } ?: unidades.firstOrNull()

                _uiState.value = current.copy(
                    ubicaciones = ubicaciones,
                    selectedUbicacionId = selectedUbicacion?.id.orEmpty(),
                    selectedUbicacionName = selectedUbicacion?.nombre.orEmpty(),

                    units = unidades,
                    selectedUnitId = selectedUnit?.id.orEmpty(),
                    selectedUnitName = selectedUnit?.nombre.orEmpty()
                )
            }
        }
    }

    fun onUbicacionSelected(id: String) {
        val selected = _uiState.value.ubicaciones.firstOrNull { ubicacion ->
            ubicacion.id == id
        } ?: return

        _uiState.value = _uiState.value.copy(
            selectedUbicacionId = selected.id,
            selectedUbicacionName = selected.nombre,
            errorMessage = null
        )
    }

    fun onUnitSelected(id: String) {
        val selected = _uiState.value.units.firstOrNull { unidad ->
            unidad.id == id
        } ?: return

        _uiState.value = _uiState.value.copy(
            selectedUnitId = selected.id,
            selectedUnitName = selected.nombre,
            errorMessage = null
        )
    }

    fun verifyConnection() {
        val current = _uiState.value

        if (current.verifyingConnection || current.isReading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                verifyingConnection = true,
                errorMessage = null
            )

            val connected = withContext(Dispatchers.IO) {
                rfidManager.verifyConnectionAndCleanBuffer()
            }

            _uiState.value = if (connected) {
                _uiState.value.copy(
                    verifyingConnection = false,
                    errorMessage = null
                )
            } else {
                _uiState.value.copy(
                    verifyingConnection = false,
                    errorMessage = "No se pudo conectar con el lector RFID"
                )
            }
        }
    }

    fun startReading() {
        val current = _uiState.value

        if (current.startingReading || current.isReading) return

        val validationError = validateBeforeReading(current)

        if (validationError != null) {
            _uiState.value = current.copy(
                errorMessage = validationError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                startingReading = true,
                errorMessage = null
            )

            val started = withContext(Dispatchers.IO) {
                rfidManager.startInventory()
            }

            _uiState.value = if (started) {
                _uiState.value.copy(
                    startingReading = false,
                    isReading = true,
                    errorMessage = null
                )
            } else {
                _uiState.value.copy(
                    startingReading = false,
                    isReading = false,
                    errorMessage = "No se pudo iniciar la lectura RFID"
                )
            }
        }
    }

    fun stopReading() {
        val current = _uiState.value

        if (current.stoppingReading || !current.isReading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                stoppingReading = true,
                errorMessage = null
            )

            withContext(Dispatchers.IO) {
                rfidManager.stopInventory()
            }

            _uiState.value = _uiState.value.copy(
                stoppingReading = false,
                isReading = false,
                errorMessage = null
            )
        }
    }

    fun leavePending(
        onLeavePending: () -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (_uiState.value.isReading) {
                        rfidManager.stopInventory()
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                isReading = false
            )

            onLeavePending()
        }
    }

    fun finalizeInventory(
        inventoryId: Long,
        onFinished: () -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (_uiState.value.isReading) {
                        rfidManager.stopInventory()
                    }

                    repository.finalizeInventory(inventoryId)
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isReading = false,
                    errorMessage = null
                )

                onFinished()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isReading = false,
                    errorMessage = error.message ?: "No se pudo finalizar el inventario"
                )
            }
        }
    }

    private fun observeRfidState() {
        viewModelScope.launch {
            rfidManager.connectionState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    isReading = state == RfidConnectionState.READING
                )
            }
        }
    }

    private fun observeRfidTags() {
        tagsJob?.cancel()

        tagsJob = viewModelScope.launch {
            rfidManager.tagReads.collect { tag ->
                val current = _uiState.value

                if (!current.isReading && !current.startingReading) {
                    return@collect
                }

                registerEpcAsCapture(tag.epc)
            }
        }
    }

    private suspend fun registerEpcAsCapture(epc: String) {
        val rawEpc = epc.trim()

        if (rawEpc.isBlank()) return

        epcMutex.withLock {
            val current = _uiState.value

            val validationError = validateBeforeReading(current)

            if (validationError != null) {
                _uiState.value = current.copy(
                    errorMessage = validationError
                )
                return@withLock
            }

            val rutUsuario = session.loginRut.value.orEmpty().trim()

            if (rutUsuario.isBlank()) {
                _uiState.value = current.copy(
                    errorMessage = "No hay usuario logueado"
                )
                return@withLock
            }

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.registerRfidInventoryItem(
                        inventoryId = current.inventoryId,
                        ubicacionId = current.selectedUbicacionId,
                        ubicacionNombre = current.selectedUbicacionName,
                        epc = rawEpc,
                        quantity = 1.0,
                        unitMeasure = current.selectedUnitName,
                        unitMeasureId = current.selectedUnitId,
                        rutUsuario = rutUsuario
                    )
                }
            }

            result.onSuccess { saved ->
                val latest = _uiState.value

                if (saved.isDuplicate) {
                    _uiState.value = latest.copy(
                        lastEpc = saved.epcNormalized.ifBlank { saved.epcRaw },
                        lastGs1Type = saved.gs1Type,
                        lastBarcodeSaved = saved.barcodeSaved,
                        duplicatedReadsCount = latest.duplicatedReadsCount + 1,
                        errorMessage = "Etiqueta duplicada ignorada."
                    )
                } else {
                    _uiState.value = latest.copy(
                        lastEpc = saved.epcNormalized.ifBlank { saved.epcRaw },
                        lastGs1Type = saved.gs1Type,
                        lastBarcodeSaved = saved.barcodeSaved,
                        totalReadsCount = latest.totalReadsCount + 1,
                        validReadsCount = latest.validReadsCount + 1,
                        errorMessage = null
                    )
                }
            }

            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    lastEpc = rawEpc,
                    errorMessage = error.message ?: "No se pudo registrar la lectura RFID"
                )
            }
        }
    }

    private fun validateBeforeReading(
        state: InventoryRfidUiState
    ): String? {
        if (state.inventoryId <= 0L) {
            return "Inventario inválido"
        }

        if (state.selectedUbicacionId.isBlank()) {
            return "Debe seleccionar una ubicación"
        }

        if (state.selectedUnitId.isBlank()) {
            return "Debe seleccionar una unidad de medida"
        }

        return null
    }

    override fun onCleared() {
        super.onCleared()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                rfidManager.pauseForBackground()
            }
        }
    }
}