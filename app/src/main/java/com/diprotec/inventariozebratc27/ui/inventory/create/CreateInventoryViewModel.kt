package com.diprotec.inventariozebratc27.ui.inventory.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.core.auth.SuperAdminAccess
import com.diprotec.inventariozebratc27.core.session.SessionManager
import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryReadingMode
import com.diprotec.inventariozebratc27.data.repository.InventoryRemoteRepository
import com.diprotec.inventariozebratc27.data.repository.InventoryRepository
import com.diprotec.inventariozebratc27.rfid.ZebraRfidManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InventarioAsignadoOption(
    val id: String,
    val descripcion: String,
    val fecha: String? = null,
    val hora: String? = null,
    val desde: String? = null,
    val hasta: String? = null,
    val rutAdministrador: String? = null,
    val rutEmpresa: String? = null
) {
    val inicioTexto: String
        get() = listOfNotNull(desde, hora)
            .joinToString(" ")
            .ifBlank { "Sin inicio" }

    val terminoTexto: String
        get() = listOfNotNull(hasta, hora)
            .joinToString(" ")
            .ifBlank { "Sin termino" }
}

data class CreateInventoryUiState(
    val inventarios: List<InventarioAsignadoOption> = emptyList(),
    val selectedInventarioId: String = "",
    val selectedInventarioDescripcion: String = "",
    val selectedInicioTexto: String = "",
    val selectedTerminoTexto: String = "",
    val selectedReadingMode: InventoryReadingMode = InventoryReadingMode.LASER,
    val checkingRfidAvailability: Boolean = false,
    val rfidAvailable: Boolean = false,
    val rfidAvailabilityMessage: String? = null,
    val creating: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateInventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val inventoryRemoteRepository: InventoryRemoteRepository,
    private val session: SessionManager,
    private val rfidManager: ZebraRfidManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateInventoryUiState())
    val uiState: StateFlow<CreateInventoryUiState> = _uiState.asStateFlow()

    init {
        observeInventariosAsignados()
        checkRfidAvailability()
    }

    fun checkRfidAvailability() {
        if (_uiState.value.checkingRfidAvailability) return

        _uiState.value = _uiState.value.copy(
            checkingRfidAvailability = true,
            rfidAvailabilityMessage = null
        )

        viewModelScope.launch {
            val available = withContext(Dispatchers.IO) {
                runCatching {
                    rfidManager.hasAvailableRfidReader()
                }.getOrDefault(false)
            }

            val currentState = _uiState.value

            _uiState.value = currentState.copy(
                checkingRfidAvailability = false,
                rfidAvailable = available,
                selectedReadingMode = if (!available &&
                    currentState.selectedReadingMode == InventoryReadingMode.RFID
                ) {
                    InventoryReadingMode.LASER
                } else {
                    currentState.selectedReadingMode
                },
                rfidAvailabilityMessage = if (available) {
                    null
                } else {
                    "RFID no disponible. Vincule o conecte un lector RFID para utilizar esta opción."
                }
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeInventariosAsignados() {
        viewModelScope.launch {
            session.loginRut
                .flatMapLatest { rut ->
                    val rutUsuario = rut.orEmpty().trim()

                    if (rutUsuario.isBlank()) {
                        flowOf(emptyList<InventarioAsignadoOption>())
                    } else {
                        val isSuperAdmin = SuperAdminAccess.isSuperAdmin(rutUsuario)

                        val remotosFlow = if (isSuperAdmin) {
                            inventoryRemoteRepository.observeInventariosActivos()
                        } else {
                            inventoryRemoteRepository.observeInventariosAsignadosActivos(rutUsuario)
                        }

                        val localesFlow = if (isSuperAdmin) {
                            repository.observeAllInventories()
                        } else {
                            repository.observeAllInventoriesByUsuario(rutUsuario)
                        }

                        combine(
                            remotosFlow,
                            localesFlow
                        ) { remotos, locales ->
                            val remoteIdsYaCreados = locales
                                .map { it.remoteInventoryId.trim() }
                                .filter { it.isNotBlank() }
                                .toSet()

                            remotos
                                .filter { remoto ->
                                    val remoteId = remoto.id.trim()

                                    remoteId.isNotBlank() &&
                                            !remoteIdsYaCreados.contains(remoteId) &&
                                            !isExpired(remoto.hasta)
                                }
                                .map {
                                    InventarioAsignadoOption(
                                        id = it.id.trim(),
                                        descripcion = it.descripcion.orEmpty(),
                                        fecha = it.fecha,
                                        hora = it.hora,
                                        desde = it.desde,
                                        hasta = it.hasta,
                                        rutAdministrador = it.rutAdministrador,
                                        rutEmpresa = it.rutEmpresa
                                    )
                                }
                                .filter {
                                    it.id.isNotBlank() && it.descripcion.isNotBlank()
                                }
                        }
                    }
                }
                .flowOn(Dispatchers.Default)
                .collectLatest { options ->
                    val currentSelectedId = _uiState.value.selectedInventarioId
                    val selected = options.firstOrNull { it.id == currentSelectedId }
                        ?: options.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        inventarios = options,
                        selectedInventarioId = selected?.id.orEmpty(),
                        selectedInventarioDescripcion = selected?.descripcion.orEmpty(),
                        selectedInicioTexto = selected?.inicioTexto.orEmpty(),
                        selectedTerminoTexto = selected?.terminoTexto.orEmpty(),
                        errorMessage = if (options.isEmpty()) {
                            "No hay inventarios disponibles para crear"
                        } else {
                            null
                        }
                    )
                }
        }
    }

    fun onInventarioSelected(id: String) {
        val selected = _uiState.value.inventarios.firstOrNull {
            it.id == id.trim()
        } ?: return

        _uiState.value = _uiState.value.copy(
            selectedInventarioId = selected.id,
            selectedInventarioDescripcion = selected.descripcion,
            selectedInicioTexto = selected.inicioTexto,
            selectedTerminoTexto = selected.terminoTexto,
            errorMessage = null
        )
    }

    fun onReadingModeSelected(mode: InventoryReadingMode) {
        if (mode == InventoryReadingMode.RFID && !_uiState.value.rfidAvailable) {
            _uiState.value = _uiState.value.copy(
                selectedReadingMode = InventoryReadingMode.LASER,
                errorMessage = "RFID no disponible. Vincule o conecte un lector RFID para utilizar esta opción."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedReadingMode = mode,
            errorMessage = null
        )
    }

    fun createSelectedInventory(
        onCreated: (Long, InventoryReadingMode) -> Unit
    ) {
        val currentState = _uiState.value

        if (currentState.creating) return

        val rutUsuario = session.loginRut.value.orEmpty().trim()

        if (rutUsuario.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "No hay usuario logueado"
            )
            return
        }

        val selected = currentState.inventarios
            .firstOrNull { it.id == currentState.selectedInventarioId }

        if (selected == null) {
            _uiState.value = currentState.copy(
                errorMessage = "Debe seleccionar un inventario"
            )
            return
        }

        val selectedReadingMode = currentState.selectedReadingMode

        if (selectedReadingMode == InventoryReadingMode.RFID && !currentState.rfidAvailable) {
            _uiState.value = currentState.copy(
                errorMessage = "RFID no disponible. Vincule o conecte un lector RFID para utilizar esta opción."
            )
            return
        }

        _uiState.value = currentState.copy(
            creating = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.createInventoryFromRemote(
                        remote = InventoryRemoteEntity(
                            id = selected.id.trim(),
                            descripcion = selected.descripcion,
                            fecha = selected.fecha,
                            hora = selected.hora,
                            desde = selected.desde,
                            hasta = selected.hasta,
                            rutAdministrador = selected.rutAdministrador,
                            estado = false,
                            rutEmpresa = selected.rutEmpresa
                        ),
                        rutUsuario = rutUsuario,
                        readingMode = selectedReadingMode
                    )
                }
            }

            result.onSuccess { inventoryId ->
                _uiState.value = _uiState.value.copy(
                    creating = false,
                    errorMessage = null
                )

                onCreated(
                    inventoryId,
                    selectedReadingMode
                )
            }

            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    creating = false,
                    errorMessage = error.message ?: "No se pudo crear el inventario"
                )
            }
        }
    }

    private fun isExpired(hasta: String?): Boolean {
        if (hasta.isNullOrBlank()) return false

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            isLenient = false
        }

        val limit = runCatching {
            formatter.parse(hasta)
        }.getOrNull() ?: return false

        val today = formatter.parse(formatter.format(Date())) ?: Date()

        return today.after(limit)
    }
}