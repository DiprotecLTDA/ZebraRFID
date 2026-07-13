package com.diprotec.inventariozebratc27.ui.rfid.locate

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.data.local.entity.ProductEntity
import com.diprotec.inventariozebratc27.data.repository.ProductRepository
import com.diprotec.inventariozebratc27.rfid.ZebraRfidManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class RfidLocateViewModel @Inject constructor(
    private val rfidManager: ZebraRfidManager,
    private val productRepository: ProductRepository
) : ViewModel() {

    companion object {
        private const val TAG = "RfidLocateViewModel"
        private const val MAX_OPTIONS_TO_SHOW = 20
    }

    private val _uiState = MutableStateFlow(RfidLocateUiState())
    val uiState: StateFlow<RfidLocateUiState> = _uiState.asStateFlow()

    private val toneGenerator = ToneGenerator(
        AudioManager.STREAM_MUSIC,
        80
    )

    private var beepJob: Job? = null
    private var locatingEpc: String = ""

    init {
        observeLocateResults()
    }

    fun onSearchInputChange(value: String) {
        _uiState.value = _uiState.value.copy(
            searchInput = value.uppercase(),
            error = null,
            tooManyOptions = false
        )
    }

    fun searchProductAndPrepareLocate() {
        val query = _uiState.value.searchInput
            .trim()
            .uppercase()

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Debe ingresar código de producto, código secundario o descripción."
            )
            return
        }

        viewModelScope.launch {
            stopBeepLoop()

            _uiState.value = _uiState.value.copy(
                searchingProduct = true,
                locating = false,
                connecting = false,
                checkingReader = false,
                relativeDistance = 0,
                locateReadCount = 0,
                lastLocatedEpc = null,
                options = emptyList(),
                totalOptionsFound = 0,
                tooManyOptions = false,
                selectedOption = null,
                generatedEpcToLocate = null,
                message = "Buscando producto...",
                error = null
            )

            val products = withContext(Dispatchers.IO) {
                productRepository.searchProductsForRfidLocation(query)
            }

            val options = products
                .flatMap { product ->
                    product.toRfidSearchOptions()
                }
                .filter { option ->
                    option.epcToLocate.isNotBlank()
                }
                .distinctBy { option ->
                    "${option.productCode}|${option.rfidSourceLabel}|${option.epcToLocate}|${option.mode}"
                }
                .sortedWith(
                    compareBy<RfidProductSearchOption> {
                        if (it.rfidSourceValue.equals(query, ignoreCase = true)) 0 else 1
                    }.thenBy {
                        modePriority(it.mode)
                    }.thenBy {
                        it.productCode
                    }.thenBy {
                        it.rfidSourceLabel
                    }
                )

            when {
                options.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        searchingProduct = false,
                        options = emptyList(),
                        totalOptionsFound = 0,
                        tooManyOptions = false,
                        message = "No se encontraron productos.",
                        error = "No existe un producto asociado a la búsqueda ingresada."
                    )
                }

                options.size == 1 -> {
                    val option = options.first()

                    _uiState.value = _uiState.value.copy(
                        searchingProduct = false,
                        options = emptyList(),
                        totalOptionsFound = 1,
                        tooManyOptions = false,
                        selectedOption = option,
                        generatedEpcToLocate = option.epcToLocate,
                        error = null
                    )

                    startLocateWithOption(option)
                }

                options.size > MAX_OPTIONS_TO_SHOW -> {
                    _uiState.value = _uiState.value.copy(
                        searchingProduct = false,
                        options = emptyList(),
                        totalOptionsFound = options.size,
                        tooManyOptions = true,
                        selectedOption = null,
                        generatedEpcToLocate = null,
                        message = "Se encontraron demasiadas coincidencias.",
                        error = "Se encontraron ${options.size} coincidencias. Ingrese un código más específico para reducir los resultados."
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        searchingProduct = false,
                        options = options,
                        totalOptionsFound = options.size,
                        tooManyOptions = false,
                        selectedOption = null,
                        generatedEpcToLocate = null,
                        message = "Seleccione qué código RFID desea localizar.",
                        error = null
                    )
                }
            }
        }
    }

    fun selectOption(option: RfidProductSearchOption) {
        _uiState.value = _uiState.value.copy(
            options = emptyList(),
            tooManyOptions = false,
            selectedOption = option,
            generatedEpcToLocate = option.epcToLocate,
            relativeDistance = 0,
            locateReadCount = 0,
            lastLocatedEpc = null,
            error = null
        )

        startLocateWithOption(option)
    }

    fun stopLocate() {
        viewModelScope.launch {
            stopBeepLoop()

            withContext(Dispatchers.IO) {
                rfidManager.stopTagLocationing()
            }

            locatingEpc = ""

            _uiState.value = _uiState.value.copy(
                locating = false,
                connecting = false,
                searchingProduct = false,
                checkingReader = false,
                relativeDistance = 0,
                message = "Búsqueda detenida."
            )
        }
    }

    fun verifyReaderConnection() {
        viewModelScope.launch {
            stopBeepLoop()

            locatingEpc = ""

            _uiState.value = _uiState.value.copy(
                checkingReader = true,
                connecting = false,
                searchingProduct = false,
                locating = false,
                relativeDistance = 0,
                locateReadCount = 0,
                lastLocatedEpc = null,
                message = "Verificando lector RFID y limpiando buffer...",
                error = null
            )

            val verified = withContext(Dispatchers.IO) {
                rfidManager.verifyConnectionAndCleanBuffer()
            }

            _uiState.value = if (verified) {
                _uiState.value.copy(
                    checkingReader = false,
                    connecting = false,
                    searchingProduct = false,
                    locating = false,
                    relativeDistance = 0,
                    locateReadCount = 0,
                    lastLocatedEpc = null,
                    message = "Lector RFID conectado y buffer limpiado correctamente.",
                    error = null
                )
            } else {
                _uiState.value.copy(
                    checkingReader = false,
                    connecting = false,
                    searchingProduct = false,
                    locating = false,
                    relativeDistance = 0,
                    message = "No se pudo verificar el lector RFID.",
                    error = "Revise que el lector esté encendido, vinculado por Bluetooth y cerca del dispositivo."
                )
            }
        }
    }

    private fun startLocateWithOption(option: RfidProductSearchOption) {
        val epc = option.epcToLocate
            .trim()
            .uppercase()

        if (epc.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "No se pudo generar el EPC RFID para la búsqueda."
            )
            return
        }

        locatingEpc = epc

        Log.d(
            TAG,
            "Iniciando búsqueda RFID. producto=${option.productCode}, " +
                    "tipo=${option.rfidSourceLabel}, valor=${option.rfidSourceValue}, " +
                    "modo=${option.mode}, epcToLocate=$epc"
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connecting = true,
                locating = false,
                searchingProduct = false,
                checkingReader = false,
                relativeDistance = 0,
                locateReadCount = 0,
                lastLocatedEpc = null,
                selectedOption = option,
                generatedEpcToLocate = epc,
                message = "Conectando lector RFID...",
                error = null
            )

            val started = withContext(Dispatchers.IO) {
                rfidManager.startTagLocationing(epc)
            }

            if (started) {
                _uiState.value = _uiState.value.copy(
                    connecting = false,
                    locating = true,
                    message = "Buscando etiqueta RFID..."
                )

                startBeepLoop()
            } else {
                locatingEpc = ""

                _uiState.value = _uiState.value.copy(
                    connecting = false,
                    locating = false,
                    message = "No se pudo iniciar búsqueda.",
                    error = "Revise que el lector RFID esté vinculado y encendido."
                )
            }
        }
    }

    private fun observeLocateResults() {
        viewModelScope.launch {
            rfidManager.locateResults.collect { result ->
                val target = locatingEpc
                    .trim()
                    .uppercase()

                val received = result.epc
                    .trim()
                    .uppercase()

                if (target.isNotBlank() && received != target) {
                    Log.d(
                        TAG,
                        "Resultado ignorado. target=$target received=$received"
                    )
                    return@collect
                }

                val distance = result.relativeDistance.coerceIn(0, 100)
                val currentState = _uiState.value
                val newReadCount = currentState.locateReadCount + 1

                _uiState.value = currentState.copy(
                    relativeDistance = distance,
                    locateReadCount = newReadCount,
                    lastLocatedEpc = received,
                    message = buildLocateMessage(distance, newReadCount),
                    error = null
                )
            }
        }
    }

    private fun buildLocateMessage(
        distance: Int,
        readCount: Int
    ): String {
        return when {
            distance >= 85 -> "Etiqueta muy cerca. Lecturas: $readCount"
            distance >= 60 -> "Etiqueta cerca. Lecturas: $readCount"
            distance >= 35 -> "Etiqueta a distancia media. Lecturas: $readCount"
            distance > 0 -> "Etiqueta lejos. Lecturas: $readCount"
            else -> "Etiqueta detectada, esperando distancia. Lecturas: $readCount"
        }
    }

    private fun ProductEntity.toRfidSearchOptions(): List<RfidProductSearchOption> {
        val options = mutableListOf<RfidProductSearchOption>()

        val mainCode = codigo
            .trim()
            .uppercase()

        if (mainCode.isNotBlank()) {
            options.addAll(
                buildRfidOptionsFromValue(
                    product = this,
                    value = mainCode,
                    sourceLabel = "Código principal"
                )
            )
        }

        val secondary = codigoSecundario
            ?.trim()
            ?.uppercase()
            .orEmpty()

        if (secondary.isNotBlank() && !secondary.equals(mainCode, ignoreCase = true)) {
            options.addAll(
                buildRfidOptionsFromValue(
                    product = this,
                    value = secondary,
                    sourceLabel = "Código secundario"
                )
            )
        }

        return options.distinctBy {
            "${it.productCode}|${it.rfidSourceLabel}|${it.epcToLocate}|${it.mode}"
        }
    }

    private fun buildRfidOptionsFromValue(
        product: ProductEntity,
        value: String,
        sourceLabel: String
    ): List<RfidProductSearchOption> {
        val cleanValue = value
            .trim()
            .uppercase()

        if (cleanValue.isBlank()) {
            return emptyList()
        }

        val options = mutableListOf<RfidProductSearchOption>()

        options.add(
            RfidProductSearchOption(
                productCode = product.codigo,
                secondaryCode = product.codigoSecundario,
                description = product.descripcion,
                rfidSourceValue = cleanValue,
                rfidSourceLabel = "$sourceLabel - EPC directo",
                epcToLocate = cleanValue,
                mode = RfidSearchMode.DIRECT
            )
        )

        options.add(
            RfidProductSearchOption(
                productCode = product.codigo,
                secondaryCode = product.codigoSecundario,
                description = product.descripcion,
                rfidSourceValue = cleanValue,
                rfidSourceLabel = "$sourceLabel - ASCII HEX",
                epcToLocate = cleanValue.toAsciiHex(),
                mode = RfidSearchMode.ASCII_HEX
            )
        )

        if (cleanValue.all { it.isDigit() } && cleanValue.length < 24) {
            val padded24 = cleanValue.padStart(24, '0')

            options.add(
                RfidProductSearchOption(
                    productCode = product.codigo,
                    secondaryCode = product.codigoSecundario,
                    description = product.descripcion,
                    rfidSourceValue = padded24,
                    rfidSourceLabel = "$sourceLabel - EPC 24 dígitos",
                    epcToLocate = padded24,
                    mode = RfidSearchMode.PADDED_24_DIRECT
                )
            )

            options.add(
                RfidProductSearchOption(
                    productCode = product.codigo,
                    secondaryCode = product.codigoSecundario,
                    description = product.descripcion,
                    rfidSourceValue = padded24,
                    rfidSourceLabel = "$sourceLabel - EPC 24 dígitos ASCII HEX",
                    epcToLocate = padded24.toAsciiHex(),
                    mode = RfidSearchMode.PADDED_24_ASCII_HEX
                )
            )
        }

        return options
    }

    private fun String.toAsciiHex(): String {
        return trim()
            .toByteArray(Charsets.US_ASCII)
            .joinToString(separator = "") { byte ->
                "%02X".format(byte)
            }
    }

    private fun modePriority(mode: RfidSearchMode): Int {
        return when (mode) {
            RfidSearchMode.DIRECT -> 0
            RfidSearchMode.PADDED_24_DIRECT -> 1
            RfidSearchMode.ASCII_HEX -> 2
            RfidSearchMode.PADDED_24_ASCII_HEX -> 3
        }
    }

    private fun startBeepLoop() {
        beepJob?.cancel()

        beepJob = viewModelScope.launch {
            while (_uiState.value.locating) {
                val distance = _uiState.value.relativeDistance

                if (distance > 0) {
                    toneGenerator.startTone(
                        ToneGenerator.TONE_PROP_BEEP,
                        80
                    )
                }

                kotlinx.coroutines.delay(beepDelay(distance))
            }
        }
    }

    private fun stopBeepLoop() {
        beepJob?.cancel()
        beepJob = null
    }

    private fun beepDelay(distance: Int): Long {
        return when {
            distance >= 85 -> 120L
            distance >= 60 -> 250L
            distance >= 35 -> 500L
            distance > 0 -> 900L
            else -> 1_200L
        }
    }

    override fun onCleared() {
        stopBeepLoop()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                rfidManager.stopTagLocationing()
            }
        }

        toneGenerator.release()

        super.onCleared()
    }
}