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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
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

    private companion object {
        const val FLUSH_INTERVAL_MS = 150L
        const val MAX_BATCH_SIZE = 500
    }

    private val _uiState = MutableStateFlow(InventoryRfidUiState())
    val uiState: StateFlow<InventoryRfidUiState> = _uiState.asStateFlow()

    private val epcMutex = Mutex()
    private val seenEpcs = mutableSetOf<String>()
    private val pendingEpcs = mutableListOf<String>()

    private var loadJob: Job? = null
    private var tagsJob: Job? = null
    private var flusherJob: Job? = null

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
            stopFlusherAndFlushPending()
            clearSeenEpcs()

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
                startFlusher()

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

        if (current.stoppingReading) return

        if (!current.isReading) {
            viewModelScope.launch {
                stopFlusherAndFlushPending()
                clearSeenEpcs()
            }
            return
        }

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

            stopFlusherAndFlushPending()
            clearSeenEpcs()
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

            stopFlusherAndFlushPending()
            clearSeenEpcs()

            onLeavePending()
        }
    }

    fun finalizeInventory(
        inventoryId: Long,
        onFinished: () -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    if (_uiState.value.isReading) {
                        rfidManager.stopInventory()
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isReading = false
                )

                stopFlusherAndFlushPending()

                withContext(Dispatchers.IO) {
                    repository.finalizeInventory(inventoryId)
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isReading = false,
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isReading = false,
                    errorMessage = error.message ?: "No se pudo finalizar el inventario"
                )
            }

            clearSeenEpcs()

            result.onSuccess {
                onFinished()
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
        val rawEpc = epc
            .trim()
            .uppercase()

        if (rawEpc.isBlank()) return

        epcMutex.withLock {
            if (seenEpcs.contains(rawEpc)) {
                return@withLock
            }

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

            seenEpcs.add(rawEpc)
            pendingEpcs.add(rawEpc)
        }
    }

    private fun startFlusher() {
        flusherJob?.cancel()

        flusherJob = viewModelScope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flushPending()
            }
        }
    }

    private suspend fun stopFlusherAndFlushPending() {
        flusherJob?.cancelAndJoin()
        flusherJob = null

        while (flushPending()) {
            // Vacía todos los lotes pendientes antes de limpiar la sesión.
        }
    }

    private suspend fun flushPending(): Boolean = withContext(NonCancellable) {
        val batch = epcMutex.withLock {
            val batchSize = minOf(MAX_BATCH_SIZE, pendingEpcs.size)

            if (batchSize == 0) {
                emptyList()
            } else {
                pendingEpcs
                    .take(batchSize)
                    .also {
                        pendingEpcs.subList(0, batchSize).clear()
                    }
            }
        }

        if (batch.isEmpty()) {
            return@withContext false
        }

        val current = _uiState.value
        val rutUsuario = session.loginRut.value.orEmpty().trim()

        val result = withContext(Dispatchers.IO) {
            runCatching {
                repository.registerRfidInventoryItems(
                    inventoryId = current.inventoryId,
                    ubicacionId = current.selectedUbicacionId,
                    ubicacionNombre = current.selectedUbicacionName,
                    quantity = 1.0,
                    unitMeasure = current.selectedUnitName,
                    unitMeasureId = current.selectedUnitId,
                    rutUsuario = rutUsuario,
                    epcs = batch
                )
            }
        }

        result.onSuccess { savedResults ->
            val lastSaved = savedResults.lastOrNull()
            val validReads = savedResults.count { saved ->
                !saved.isDuplicate
            }
            val duplicatedReads = savedResults.size - validReads
            val latest = _uiState.value

            _uiState.value = latest.copy(
                lastEpc = lastSaved?.let { saved ->
                    saved.epcNormalized.ifBlank { saved.epcRaw }
                } ?: latest.lastEpc,
                lastGs1Type = lastSaved?.gs1Type ?: latest.lastGs1Type,
                lastBarcodeSaved = lastSaved?.barcodeSaved ?: latest.lastBarcodeSaved,
                totalReadsCount = latest.totalReadsCount + validReads,
                validReadsCount = latest.validReadsCount + validReads,
                duplicatedReadsCount = latest.duplicatedReadsCount + duplicatedReads,
                errorMessage = null
            )
        }

        result.onFailure {
            epcMutex.withLock {
                seenEpcs.removeAll(batch.toSet())
            }

            _uiState.value = _uiState.value.copy(
                errorMessage = "No se pudo registrar el lote RFID. Las etiquetas se reintentarán."
            )
        }

        true
    }

    private suspend fun clearSeenEpcs() {
        epcMutex.withLock {
            seenEpcs.clear()
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
        flusherJob?.cancel()
        super.onCleared()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                rfidManager.pauseForBackground()
            }
        }
    }
}
