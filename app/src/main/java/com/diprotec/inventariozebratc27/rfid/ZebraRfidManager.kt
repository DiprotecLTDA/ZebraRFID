package com.diprotec.inventariozebratc27.rfid

import android.content.Context
import android.util.Log
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.zebra.rfid.api3.BEEPER_VOLUME
import com.zebra.rfid.api3.ENUM_TRANSPORT
import com.zebra.rfid.api3.DYNAMIC_POWER_OPTIMIZATION
import com.zebra.rfid.api3.InvalidUsageException
import com.zebra.rfid.api3.OperationFailureException
import com.zebra.rfid.api3.RFIDReader
import com.zebra.rfid.api3.RfidEventsListener
import com.zebra.rfid.api3.RfidReadEvents
import com.zebra.rfid.api3.RfidStatusEvents
import com.zebra.rfid.api3.Readers
import com.zebra.rfid.api3.SESSION
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

@Singleton
class ZebraRfidManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsManager
) {

    companion object {
        private const val TAG = "ZebraRfidManager"
        private const val TAG_READS_BUFFER = 16384
        private const val TAG_POPULATION = 30
        private const val READ_TAGS_BATCH_SIZE = 100
        private const val VERIFY_STEP_DELAY_MS = 200L
        private const val RECONNECT_DELAY_MS = 500L
        private const val LOCATIONING_STEP_DELAY_MS = 300L
        private const val STOP_LOCATIONING_STEP_DELAY_MS = 250L
        private const val START_INVENTORY_CLEANUP_DELAY_MS = 150L
        private const val INVENTORY_START_DELAY_MS = 100L
    }

    private var readers: Readers? = null
    private var reader: RFIDReader? = null

    private var locatingEpc: String? = null

    private val _connectionState =
        MutableStateFlow(RfidConnectionState.DISCONNECTED)

    val connectionState: StateFlow<RfidConnectionState> =
        _connectionState

    private val _tagReads =
        MutableSharedFlow<RfidTagRead>(
            extraBufferCapacity = TAG_READS_BUFFER,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    val tagReads: SharedFlow<RfidTagRead> =
        _tagReads

    private val _locateResults =
        MutableSharedFlow<RfidLocateResult>(
            extraBufferCapacity = 256,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    val locateResults: SharedFlow<RfidLocateResult> =
        _locateResults

    private val eventListener = object : RfidEventsListener {

        override fun eventReadNotify(readEvents: RfidReadEvents?) {
            val currentReader = reader ?: return

            if (!currentReader.isConnected) {
                Log.w(TAG, "eventReadNotify: reader no está conectado")
                return
            }

            try {
                while (true) {
                    val tags = currentReader.Actions
                        .getReadTags(READ_TAGS_BATCH_SIZE)

                    if (tags.isNullOrEmpty()) {
                        break
                    }

                    tags.forEach { tagData ->
                        val rawEpc = tagData.tagID
                            ?.trim()
                            ?.uppercase()
                            .orEmpty()

                        val target = locatingEpc
                            ?.trim()
                            ?.uppercase()
                            .orEmpty()

                        val isLocating = target.isNotBlank()

                        val resolvedEpc = if (rawEpc.isNotBlank()) {
                            rawEpc
                        } else {
                            target
                        }

                        if (resolvedEpc.isBlank()) {
                            return@forEach
                        }

                        if (isLocating) {
                            if (!resolvedEpc.equals(target, ignoreCase = true)) {
                                return@forEach
                            }

                            if (tagData.isContainsLocationInfo) {
                                val distance = tagData.LocationInfo
                                    ?.getRelativeDistance()
                                    ?.toInt()
                                    ?: 0

                                val safeDistance = distance.coerceIn(0, 100)

                                _locateResults.tryEmit(
                                    RfidLocateResult(
                                        epc = resolvedEpc,
                                        relativeDistance = safeDistance
                                    )
                                )
                            }

                            return@forEach
                        }

                        _tagReads.tryEmit(
                            RfidTagRead(
                                epc = resolvedEpc,
                                rssi = tagData.peakRSSI
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recibiendo tags RFID", e)
            }
        }

        override fun eventStatusNotify(statusEvents: RfidStatusEvents?) {
            val data = statusEvents?.StatusEventData

            Log.d(
                TAG,
                "STATUS DEBUG data=$data, " +
                        "statusEventType=${data?.statusEventType}, " +
                        "locatingEpc=$locatingEpc"
            )
        }
    }

    suspend fun hasAvailableRfidReader(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (reader?.isConnected == true) {
                Log.d(TAG, "Lector RFID ya conectado")
                return@withContext true
            }

            if (readers == null) {
                readers = Readers(
                    context,
                    ENUM_TRANSPORT.BLUETOOTH
                )
            }

            val availableReaders =
                readers?.GetAvailableRFIDReaderList().orEmpty()

            val hasReader = availableReaders.isNotEmpty()

            if (hasReader) {
                Log.d(TAG, "Lector RFID disponible")
            } else {
                Log.d(TAG, "No hay lector RFID disponible")
            }

            hasReader
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando disponibilidad RFID", e)
            false
        }
    }

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        val currentConnectedReader = reader

        if (currentConnectedReader?.isConnected == true) {
            configureReader()
            _connectionState.value = RfidConnectionState.CONNECTED
            Log.i(TAG, "RFID ya estaba conectado, eventos reconfigurados")
            return@withContext true
        }

        _connectionState.value = RfidConnectionState.CONNECTING

        try {
            if (readers == null) {
                readers = Readers(
                    context,
                    ENUM_TRANSPORT.BLUETOOTH
                )
            }

            val availableReaders =
                readers?.GetAvailableRFIDReaderList().orEmpty()

            if (availableReaders.isEmpty()) {
                Log.e(TAG, "No se encontraron lectores RFID Bluetooth")
                _connectionState.value = RfidConnectionState.ERROR
                return@withContext false
            }

            val readerDevice = availableReaders.firstOrNull {
                it.name.contains("RFD", ignoreCase = true)
            } ?: availableReaders.first()

            reader = readerDevice.rfidReader

            reader?.connect()

            configureReader()

            _connectionState.value = RfidConnectionState.CONNECTED

            Log.i(TAG, "RFID conectado: ${readerDevice.name}")

            true
        } catch (e: InvalidUsageException) {
            Log.e(TAG, "Uso inválido del SDK RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: OperationFailureException) {
            Log.e(TAG, "Falla operacional conectando RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error conectando RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        }
    }

    private fun configureReader(forLocationing: Boolean = false) {
        val currentReader = reader ?: return

        if (!currentReader.isConnected) return

        runCatching {
            currentReader.Events.removeEventsListener(eventListener)
        }

        currentReader.Events.addEventsListener(eventListener)
        currentReader.Events.setTagReadEvent(true)
        currentReader.Events.setAttachTagDataWithReadEvent(true)

        applyReadConfiguration(forLocationing = forLocationing)

        runCatching {
            currentReader.Config.setUniqueTagReport(false)
        }.onSuccess {
            if (forLocationing) {
                Log.d(TAG, "UniqueTagReport desactivado para localización")
            } else {
                Log.d(TAG, "UniqueTagReport desactivado")
            }
        }.onFailure {
            if (forLocationing) {
                Log.w(TAG, "No se pudo desactivar UniqueTagReport para localización", it)
            } else {
                Log.w(TAG, "No se pudo desactivar UniqueTagReport", it)
            }
        }

        if (forLocationing) {
            Log.d(TAG, "Eventos RFID configurados para localización")
        } else {
            Log.d(TAG, "Eventos RFID configurados")
        }
    }

    /**
     * Convierte el porcentaje configurado (0-100) al índice de potencia soportado
     * por el lector conectado. Se guarda porcentaje —y no índice— para que el ajuste
     * no dependa del modelo de lector ni de tenerlo conectado al configurarlo.
     */
    private fun powerIndexForPercent(
        powerPercent: Int,
        lastIndex: Int
    ): Int {
        if (lastIndex <= 0) return 0

        val safePercent = powerPercent.coerceIn(0, 100)

        return (lastIndex * safePercent / 100).coerceIn(0, lastIndex)
    }

    private fun beeperVolumeOf(
        volume: RfidBeeperVolume
    ): BEEPER_VOLUME {
        return when (volume) {
            RfidBeeperVolume.HIGH -> BEEPER_VOLUME.HIGH_BEEP
            RfidBeeperVolume.MEDIUM -> BEEPER_VOLUME.MEDIUM_BEEP
            RfidBeeperVolume.LOW -> BEEPER_VOLUME.LOW_BEEP
            RfidBeeperVolume.QUIET -> BEEPER_VOLUME.QUIET_BEEP
        }
    }

    private fun applyReadConfiguration(forLocationing: Boolean = false) {
        val currentReader = reader ?: return

        if (!currentReader.isConnected) return

        runCatching {
            val powerLevels = currentReader.ReaderCapabilities
                .getTransmitPowerLevelValues()

            check(powerLevels.isNotEmpty()) {
                "El lector no reportó niveles de potencia RF"
            }

            val powerPercent = if (forLocationing) {
                settings.rfidPowerLocatePercent.value
            } else {
                settings.rfidPowerInventoryPercent.value
            }

            val transmitPowerIndex = powerIndexForPercent(
                powerPercent = powerPercent,
                lastIndex = powerLevels.lastIndex
            )

            val antennaConfig = currentReader.Config.Antennas
                .getAntennaRfConfig(1)

            antennaConfig.setTransmitPowerIndex(transmitPowerIndex)
            currentReader.Config.Antennas
                .setAntennaRfConfig(1, antennaConfig)

            transmitPowerIndex
        }.onSuccess { index ->
            if (forLocationing) {
                Log.d(TAG, "Potencia RF de localización configurada (índice=$index)")
            } else {
                Log.d(TAG, "Potencia RF de inventario configurada (índice=$index)")
            }
        }.onFailure {
            if (forLocationing) {
                Log.w(TAG, "No se pudo configurar la potencia RF para localización", it)
            } else {
                Log.w(TAG, "No se pudo configurar la potencia RF de inventario", it)
            }
        }

        runCatching {
            currentReader.Config.setBeeperVolume(
                beeperVolumeOf(settings.rfidBeeperVolume.value)
            )
        }.onSuccess {
            Log.d(TAG, "Volumen del beeper configurado: ${settings.rfidBeeperVolume.value.name}")
        }.onFailure {
            Log.w(TAG, "No se pudo configurar el volumen del beeper", it)
        }

        runCatching {
            val singulation = currentReader.Config.Antennas
                .getSingulationControl(1)

            singulation.setSession(SESSION.SESSION_S0)
            singulation.setTagPopulation(TAG_POPULATION.toShort())
            currentReader.Config.Antennas
                .setSingulationControl(1, singulation)
        }.onSuccess {
            Log.d(TAG, "Singulación RFID configurada en sesión S0")
        }.onFailure {
            Log.w(TAG, "No se pudo configurar la singulación RFID", it)
        }

        runCatching {
            currentReader.Config.setDPOState(
                DYNAMIC_POWER_OPTIMIZATION.DISABLE
            )
        }.onSuccess {
            Log.d(TAG, "DPO desactivado")
        }.onFailure {
            Log.w(TAG, "No se pudo desactivar DPO", it)
        }
    }

    suspend fun reconnectIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (reader?.isConnected == true) {
            configureReader()
            _connectionState.value = RfidConnectionState.CONNECTED
            true
        } else {
            connect()
        }
    }

    private suspend fun stopReaderOperationsAndPurge(
        currentReader: RFIDReader,
        stopLocationing: Boolean = true,
        stopInventory: Boolean = true,
        purgeTags: Boolean = true,
        delayAfterLocationing: Long? = null,
        delayAfterInventory: Long? = null,
        delayAfterPurge: Long? = null,
        verifyConnectedBeforeEachAction: Boolean = false,
        onLocationingFailure: ((Throwable) -> Unit)? = null,
        onInventoryFailure: ((Throwable) -> Unit)? = null,
        onPurgeSuccess: (() -> Unit)? = null,
        onPurgeFailure: ((Throwable) -> Unit)? = null
    ) {
        if (stopLocationing) {
            if (!verifyConnectedBeforeEachAction || currentReader.isConnected) {
                runCatching {
                    currentReader.Actions.TagLocationing.Stop()
                }.onFailure {
                    onLocationingFailure?.invoke(it)
                }
            }

            delayAfterLocationing?.let { delay(it) }
        }

        if (stopInventory) {
            if (!verifyConnectedBeforeEachAction || currentReader.isConnected) {
                runCatching {
                    currentReader.Actions.Inventory.stop()
                }.onFailure {
                    onInventoryFailure?.invoke(it)
                }
            }

            delayAfterInventory?.let { delay(it) }
        }

        if (purgeTags) {
            if (!verifyConnectedBeforeEachAction || currentReader.isConnected) {
                runCatching {
                    currentReader.Actions.purgeTags()
                }.onSuccess {
                    onPurgeSuccess?.invoke()
                }.onFailure {
                    onPurgeFailure?.invoke(it)
                }
            }

            delayAfterPurge?.let { delay(it) }
        }
    }

    suspend fun verifyConnectionAndCleanBuffer(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Verificando conexión RFID y limpiando buffer")

            locatingEpc = null

            val currentReader = reader

            if (currentReader?.isConnected == true) {
                stopReaderOperationsAndPurge(
                    currentReader = currentReader,
                    delayAfterLocationing = VERIFY_STEP_DELAY_MS,
                    delayAfterInventory = VERIFY_STEP_DELAY_MS,
                    delayAfterPurge = VERIFY_STEP_DELAY_MS,
                    onLocationingFailure = {
                        Log.w(TAG, "No había localización activa al verificar lector", it)
                    },
                    onInventoryFailure = {
                        Log.w(TAG, "No había inventario activo al verificar lector", it)
                    },
                    onPurgeSuccess = {
                        Log.d(TAG, "Buffer RFID limpiado correctamente")
                    },
                    onPurgeFailure = {
                        Log.w(TAG, "No se pudo limpiar buffer RFID", it)
                    }
                )

                configureReader()

                _connectionState.value = RfidConnectionState.CONNECTED

                Log.i(TAG, "Lector RFID verificado correctamente")

                return@withContext true
            }

            Log.w(TAG, "Reader no conectado. Se intentará reconstruir conexión RFID")

            runCatching {
                reader?.Events?.removeEventsListener(eventListener)
            }

            runCatching {
                if (reader?.isConnected == true) {
                    reader?.disconnect()
                }
            }

            reader = null

            runCatching {
                readers?.Dispose()
            }

            readers = null

            delay(RECONNECT_DELAY_MS)

            val connected = connect()

            if (!connected || reader?.isConnected != true) {
                Log.e(TAG, "No se pudo reconectar lector RFID")
                _connectionState.value = RfidConnectionState.ERROR
                return@withContext false
            }

            val reconnectedReader = reader

            if (reconnectedReader?.isConnected == true) {
                stopReaderOperationsAndPurge(
                    currentReader = reconnectedReader,
                    delayAfterLocationing = VERIFY_STEP_DELAY_MS,
                    delayAfterInventory = VERIFY_STEP_DELAY_MS,
                    onPurgeSuccess = {
                        Log.d(TAG, "Buffer RFID limpiado después de reconexión")
                    },
                    onPurgeFailure = {
                        Log.w(TAG, "No se pudo limpiar buffer después de reconexión", it)
                    }
                )

                configureReader()

                _connectionState.value = RfidConnectionState.CONNECTED

                Log.i(TAG, "Lector RFID reconectado y preparado correctamente")

                true
            } else {
                _connectionState.value = RfidConnectionState.ERROR
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando conexión RFID", e)

            locatingEpc = null

            runCatching {
                reader?.Events?.removeEventsListener(eventListener)
            }

            runCatching {
                if (reader?.isConnected == true) {
                    reader?.disconnect()
                }
            }

            reader = null

            runCatching {
                readers?.Dispose()
            }

            readers = null

            _connectionState.value = RfidConnectionState.ERROR

            false
        }
    }

    suspend fun startTagLocationing(
        epc: String
    ): Boolean = withContext(Dispatchers.IO) {
        val targetEpc = epc
            .trim()
            .uppercase()

        if (targetEpc.isBlank()) {
            Log.e(TAG, "No se pudo iniciar localización: EPC vacío")
            return@withContext false
        }

        try {
            val connected = if (reader?.isConnected == true) {
                _connectionState.value = RfidConnectionState.CONNECTED
                true
            } else {
                connect()
            }

            if (!connected || reader?.isConnected != true) {
                Log.e(TAG, "No se pudo iniciar localización RFID: lector no conectado")
                _connectionState.value = RfidConnectionState.ERROR
                return@withContext false
            }

            val currentReader = reader

            if (currentReader == null || !currentReader.isConnected) {
                Log.e(TAG, "No se pudo iniciar localización RFID: reader nulo o desconectado")
                _connectionState.value = RfidConnectionState.ERROR
                return@withContext false
            }

            stopReaderOperationsAndPurge(
                currentReader = currentReader,
                purgeTags = false,
                delayAfterLocationing = LOCATIONING_STEP_DELAY_MS,
                delayAfterInventory = LOCATIONING_STEP_DELAY_MS,
                onLocationingFailure = {
                    Log.w(TAG, "No había localización previa activa para detener", it)
                },
                onInventoryFailure = {
                    Log.w(TAG, "No había inventario activo para detener", it)
                }
            )

            configureReader(forLocationing = true)

            delay(LOCATIONING_STEP_DELAY_MS)

            locatingEpc = targetEpc

            Log.i(TAG, "Iniciando TagLocationing.Perform para EPC: $targetEpc")

            currentReader.Actions.TagLocationing.Perform(
                targetEpc,
                null,
                null
            )

            _connectionState.value = RfidConnectionState.READING

            Log.i(TAG, "Localización RFID iniciada para EPC: $targetEpc")

            true
        } catch (e: InvalidUsageException) {
            Log.e(TAG, "Uso inválido iniciando localización RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: OperationFailureException) {
            Log.e(TAG, "Falla operacional iniciando localización RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando localización RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        }
    }

    suspend fun stopTagLocationing(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentReader = reader

            if (currentReader?.isConnected == true) {
                stopReaderOperationsAndPurge(
                    currentReader = currentReader,
                    delayAfterLocationing = STOP_LOCATIONING_STEP_DELAY_MS,
                    delayAfterInventory = STOP_LOCATIONING_STEP_DELAY_MS,
                    onLocationingFailure = {
                        Log.w(TAG, "No se pudo detener localización RFID", it)
                    },
                    onInventoryFailure = {
                        Log.w(TAG, "No se pudo detener inventario al cerrar localización", it)
                    },
                    onPurgeSuccess = {
                        Log.d(TAG, "Buffer RFID limpiado al detener localización")
                    },
                    onPurgeFailure = {
                        Log.w(TAG, "No se pudo limpiar buffer al detener localización", it)
                    }
                )

                locatingEpc = null

                delay(STOP_LOCATIONING_STEP_DELAY_MS)

                configureReader()

                _connectionState.value = RfidConnectionState.CONNECTED

                Log.i(TAG, "Localización RFID detenida")

                true
            } else {
                locatingEpc = null

                _connectionState.value = RfidConnectionState.DISCONNECTED

                Log.i(TAG, "Localización RFID no detenida: lector desconectado")

                false
            }
        } catch (e: InvalidUsageException) {
            Log.e(TAG, "Uso inválido deteniendo localización RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: OperationFailureException) {
            Log.e(TAG, "Falla operacional deteniendo localización RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo localización RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        }
    }

    suspend fun startInventory(): Boolean = withContext(Dispatchers.IO) {
        try {
            val connected = verifyConnectionAndCleanBuffer()

            if (!connected || reader?.isConnected != true) {
                Log.e(TAG, "No se pudo iniciar lectura RFID: lector no conectado")
                _connectionState.value = RfidConnectionState.ERROR
                return@withContext false
            }

            val currentReader = reader

            if (currentReader == null || !currentReader.isConnected) {
                Log.e(TAG, "No se pudo iniciar lectura RFID: reader nulo o desconectado")
                _connectionState.value = RfidConnectionState.ERROR
                return@withContext false
            }

            locatingEpc = null

            stopReaderOperationsAndPurge(
                currentReader = currentReader,
                delayAfterLocationing = START_INVENTORY_CLEANUP_DELAY_MS,
                delayAfterInventory = START_INVENTORY_CLEANUP_DELAY_MS,
                delayAfterPurge = START_INVENTORY_CLEANUP_DELAY_MS,
                onLocationingFailure = {
                    Log.w(TAG, "No había localización activa para detener antes de inventario", it)
                },
                onInventoryFailure = {
                    Log.w(TAG, "No había lectura RFID activa para detener antes de iniciar", it)
                },
                onPurgeSuccess = {
                    Log.d(TAG, "Buffer RFID limpiado inmediatamente antes de iniciar lectura")
                },
                onPurgeFailure = {
                    Log.w(TAG, "No se pudo limpiar buffer RFID antes de iniciar", it)
                }
            )

            configureReader()

            delay(INVENTORY_START_DELAY_MS)

            currentReader.Actions.Inventory.perform()

            _connectionState.value = RfidConnectionState.READING

            Log.i(TAG, "Lectura RFID iniciada con buffer limpio")

            true
        } catch (e: InvalidUsageException) {
            Log.e(TAG, "Uso inválido iniciando lectura RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: OperationFailureException) {
            Log.e(TAG, "Falla operacional iniciando lectura RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando lectura RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        }
    }

    suspend fun stopInventory(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentReader = reader

            if (currentReader?.isConnected == true) {
                stopReaderOperationsAndPurge(
                    currentReader = currentReader,
                    stopLocationing = false,
                    onInventoryFailure = {
                        Log.w(TAG, "No se pudo detener lectura RFID", it)
                    },
                    onPurgeSuccess = {
                        Log.d(TAG, "Buffer RFID limpiado al detener lectura")
                    },
                    onPurgeFailure = {
                        Log.w(TAG, "No se pudo limpiar buffer RFID al detener", it)
                    }
                )

                locatingEpc = null

                configureReader()

                _connectionState.value = RfidConnectionState.CONNECTED

                Log.i(TAG, "Lectura RFID detenida")

                true
            } else {
                _connectionState.value = RfidConnectionState.DISCONNECTED

                Log.i(TAG, "Lectura RFID no detenida: lector desconectado")

                false
            }
        } catch (e: InvalidUsageException) {
            Log.e(TAG, "Uso inválido deteniendo lectura RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: OperationFailureException) {
            Log.e(TAG, "Falla operacional deteniendo lectura RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
            false
        }
    }

    suspend fun pauseForBackground() = withContext(Dispatchers.IO) {
        try {
            val currentReader = reader

            if (currentReader?.isConnected == true) {
                stopReaderOperationsAndPurge(
                    currentReader = currentReader,
                    onPurgeSuccess = {
                        Log.d(TAG, "Buffer RFID limpiado al pausar por background")
                    },
                    onPurgeFailure = {
                        Log.w(TAG, "No se pudo limpiar buffer RFID al pausar", it)
                    }
                )
            }

            locatingEpc = null

            _connectionState.value = RfidConnectionState.PAUSED

            Log.i(TAG, "RFID pausado por background")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausando RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            runCatching {
                reader?.Events?.removeEventsListener(eventListener)
            }

            reader?.let { currentReader ->
                stopReaderOperationsAndPurge(
                    currentReader = currentReader,
                    verifyConnectedBeforeEachAction = true
                )
            }

            if (reader?.isConnected == true) {
                reader?.disconnect()
            }

            locatingEpc = null

            reader = null

            readers?.Dispose()
            readers = null

            _connectionState.value = RfidConnectionState.DISCONNECTED

            Log.i(TAG, "RFID desconectado")
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando RFID", e)
            _connectionState.value = RfidConnectionState.ERROR
        }
    }
}
