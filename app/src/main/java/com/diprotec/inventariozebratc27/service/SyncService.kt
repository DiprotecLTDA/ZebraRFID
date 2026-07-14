package com.diprotec.inventariozebratc27.service

import android.content.Context
import android.util.Log
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ApiException
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.local.entity.InventoryEntity
import com.diprotec.inventariozebratc27.data.local.entity.SyncLogEntity
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryStatus
import com.diprotec.inventariozebratc27.data.mappers.toEntity
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.remote.dto.FinalizarInventarioRequest
import com.diprotec.inventariozebratc27.data.remote.dto.RegistroInventarioCapturaRequest
import com.diprotec.inventariozebratc27.data.remote.dto.RegistroInventarioRequest
import com.diprotec.inventariozebratc27.data.repository.InventoryRemoteRepository
import com.diprotec.inventariozebratc27.data.repository.InventoryRepository
import com.diprotec.inventariozebratc27.data.repository.LocationRepository
import com.diprotec.inventariozebratc27.data.repository.ProductRepository
import com.diprotec.inventariozebratc27.data.repository.RuleRepository
import com.diprotec.inventariozebratc27.data.repository.SyncLogRepository
import com.diprotec.inventariozebratc27.data.repository.UnitMeasureRepository
import com.diprotec.inventariozebratc27.data.repository.UserRepository
import com.diprotec.inventariozebratc27.serial.ZebraSerialProvider
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class SyncService(
    private val userRepository: UserRepository,
    private val ruleRepository: RuleRepository,
    private val locationRepository: LocationRepository,
    private val productRepository: ProductRepository,
    private val unitMeasureRepository: UnitMeasureRepository,
    private val inventoryRemoteRepository: InventoryRemoteRepository,
    private val versionService: VersionService,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val inventoryRepository: InventoryRepository,
    private val syncLogRepository: SyncLogRepository,
    private val headersBuilder: ProtectedHeadersBuilder,
    private val apiCallExecutor: ApiCallExecutor,
    context: Context
) {

    private val appContext: Context = context.applicationContext

    private val deviceSessionMutex = Mutex()

    private fun authorizationHeader(): String {
        val token = settings.authToken.value.trim()

        require(token.isNotBlank()) {
            "Authorization no configurado"
        }

        return "Bearer $token"
    }

    private fun requireConfigured() {
        val empresaRut = settings.empresaRut.value.trim()
        val apiKey = settings.apiKey.value.trim()
        val authToken = settings.authToken.value.trim()

        require(empresaRut.isNotBlank()) {
            "Empresa RUT no configurado"
        }

        require(apiKey.isNotBlank()) {
            "X-API-KEY no configurada"
        }

        require(authToken.isNotBlank()) {
            "Authorization no configurado"
        }
    }

    private fun requireActivated() {
        require(settings.deviceActivated.value) {
            "El dispositivo no está activado"
        }
    }

    suspend fun warmUpDeviceSession(): String {
        return ensureDeviceSession()
    }

    private suspend fun ensureDeviceSession(): String =
        withContext(Dispatchers.IO) {
            requireConfigured()

            val currentSession = settings.deviceSession.value.trim()

            if (currentSession.isNotBlank()) {
                return@withContext currentSession
            }

            deviceSessionMutex.withLock {
                val storedSession =
                    settings.deviceSession.value.trim()

                if (storedSession.isNotBlank()) {
                    return@withLock storedSession
                }

                val empresaRut =
                    settings.empresaRut.value.trim()

                val apiKey =
                    settings.apiKey.value.trim()

                val serial =
                    ZebraSerialProvider
                        .requireSerial(appContext)
                        .trim()

                val jsonString = "\"$serial\""

                val body = jsonString.toRequestBody(
                    "application/json".toMediaType()
                )

                val response = apiCallExecutor.execute {
                    api.loginDispositivo(
                        empresaRUT = empresaRut,
                        apiKey = apiKey,
                        authorization = authorizationHeader(),
                        serialNumberPlain = body
                    )
                }

                val deviceSession = response.Data
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: throw ApiException(
                        message = response.Respuesta
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: "El servidor no entregó la sesión del dispositivo."
                    )

                settings.saveDeviceSession(deviceSession)

                deviceSession
            }
        }

    suspend fun syncUsers(): Int = syncRemoteEntities(
        fetch = { userRepository.fetchRemoteUsers() },
        map = { it.toEntity() },
        replaceAll = { _, mapped -> userRepository.replaceAllUsers(mapped) },
        name = "Usuarios"
    )

    suspend fun syncReglas(): Int = syncRemoteEntities(
        fetch = { ruleRepository.fetchRemoteReglas() },
        map = { it.toEntity() },
        replaceAll = { _, mapped -> ruleRepository.replaceAllReglas(mapped) },
        name = "Reglas"
    )

    suspend fun syncUbicaciones(): Int = syncRemoteEntities(
        fetch = { locationRepository.fetchRemoteUbicaciones() },
        map = { it.toEntity() },
        replaceAll = { _, mapped -> locationRepository.replaceAllUbicaciones(mapped) },
        name = "Ubicaciones"
    )

    suspend fun syncProductos(): Int = syncRemoteEntities(
        fetch = { productRepository.fetchRemoteProductos() },
        map = { it.toEntity() },
        replaceAll = { _, mapped -> productRepository.replaceAllProductos(mapped) },
        name = "Productos"
    )

    suspend fun syncUnidadMedidas(): Int = syncRemoteEntities(
        fetch = { unitMeasureRepository.fetchRemoteUnidadMedidas() },
        map = { it.toEntity() },
        replaceAll = { _, mapped -> unitMeasureRepository.replaceAllUnidadMedidas(mapped) },
        name = "UnidadMedidas"
    )

    suspend fun syncInventariosRemotos(): Int = syncRemoteEntities(
        fetch = { inventoryRemoteRepository.fetchRemoteInventarios() },
        map = { it.toEntity() },
        replaceAll = { remote, inventarios ->
            val usuarios = remote.flatMap { inventario ->
                inventario.usuarios.orEmpty().map { usuario ->
                    usuario.toEntity()
                }
            }

            inventoryRemoteRepository.replaceAllInventarios(
                inventarios = inventarios,
                usuarios = usuarios
            )
        },
        name = "Inventarios remotos"
    )

    private suspend fun <Remote, Entity> syncRemoteEntities(
        fetch: suspend () -> List<Remote>,
        map: (Remote) -> Entity,
        replaceAll: suspend (List<Remote>, List<Entity>) -> Unit,
        name: String
    ): Int = withContext(Dispatchers.IO) {
        requireConfigured()
        requireActivated()
        ensureDeviceSession()

        val remote = fetch()
        val mapped = remote.map(map)

        replaceAll(remote, mapped)

        Log.d(
            TAG,
            "Sincronizado $name: ${mapped.size}"
        )

        mapped.size
    }

    suspend fun checkStartupUpdateAndSavePending():
            VersionCheckStartupResult =
        withContext(Dispatchers.IO) {
            requireConfigured()
            requireActivated()
            ensureDeviceSession()

            val data = try {
                versionService.checkVersion()
            } catch (throwable: Throwable) {
                if (isNoPublishedVersionError(throwable)) {
                    Log.i(
                        TAG,
                        "No existe una versión vigente publicada. " +
                                "Se continuará con la versión instalada."
                    )

                    settings.clearPendingUpdate()

                    return@withContext VersionCheckStartupResult(
                        versionName = null,
                        hasNewVersion = false,
                        mandatory = false,
                        apkFileName = "",
                        apkUrl = null
                    )
                }

                throw throwable
            }

            val version = data?.version

            val apiRequiresUpdate =
                data?.requiereActualizacionBool() == true

            val hasNewVersion =
                version != null &&
                        (
                                versionService.hasNewVersion(data) ||
                                        apiRequiresUpdate
                                )

            val isMandatory =
                hasNewVersion &&
                        data?.actualizacionObligatoriaBool() == true

            val possibleUpdateUrl = if (hasNewVersion) {
                versionService.buildApkUrl(version)
            } else {
                null
            }

            val possibleApkFileName =
                version?.apkFileName
                    ?.trim()
                    .orEmpty()

            val finalUpdateUrl = possibleUpdateUrl
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            val finalApkFileName = possibleApkFileName
                .takeIf { it.isNotBlank() }

            val validUpdate =
                hasNewVersion &&
                        finalUpdateUrl != null &&
                        finalApkFileName != null

            if (validUpdate) {
                val apkUrl: String =
                    requireNotNull(finalUpdateUrl)

                val apkFileName: String =
                    requireNotNull(finalApkFileName)

                settings.savePendingUpdate(
                    mandatory = isMandatory,
                    downloadId = -1L,
                    apkFileName = apkFileName,
                    apkUrl = apkUrl,
                    apkDownloaded = false
                )

                Log.d(
                    TAG,
                    "Actualización detectada. " +
                            "mandatory=$isMandatory, " +
                            "apk=$apkFileName, " +
                            "url=$apkUrl"
                )

                VersionCheckStartupResult(
                    versionName = version?.versionName,
                    hasNewVersion = true,
                    mandatory = isMandatory,
                    apkFileName = apkFileName,
                    apkUrl = apkUrl
                )
            } else {
                settings.clearPendingUpdate()

                Log.d(
                    TAG,
                    "No se requiere actualización. " +
                            "apiRequiresUpdate=$apiRequiresUpdate, " +
                            "hasNewVersion=$hasNewVersion, " +
                            "apkFileName=$possibleApkFileName, " +
                            "updateUrl=$possibleUpdateUrl"
                )

                VersionCheckStartupResult(
                    versionName = version?.versionName,
                    hasNewVersion = false,
                    mandatory = false,
                    apkFileName = "",
                    apkUrl = null
                )
            }
        }

    suspend fun syncRegistroInventarios(): Int =
        withContext(Dispatchers.IO) {
            requireConfigured()
            requireActivated()
            ensureDeviceSession()

            val empresaRut =
                settings.empresaRut.value.trim()

            val pendientes =
                inventoryRepository
                    .getCapturasPendientesSincronizar()

            if (pendientes.isEmpty()) {
                Log.d(
                    TAG,
                    "No hay capturas pendientes para sincronizar"
                )

                return@withContext 0
            }

            val groupedByInventory = pendientes.groupBy {
                it.remoteInventoryId to it.rutUsuario
            }

            var totalSincronizadas = 0

            groupedByInventory.forEach { (key, capturas) ->
                val inventarioId = key.first
                val rutUsuario = key.second

                val inventory =
                    inventoryRepository
                        .getInventoryByRemoteIdAndUsuario(
                            remoteInventoryId = inventarioId,
                            rutUsuario = rutUsuario
                        )

                val inventoryName =
                    inventory?.name
                        ?.takeIf { it.isNotBlank() }
                        ?: "Inventario $inventarioId"

                val inventoryStatus =
                    inventory?.status
                        ?: InventoryStatus.PENDING.name

                /*
                 * Las capturas se envían troceadas: un inventario RFID puede acumular
                 * miles de lecturas y un único request con todas resultaría en un cuerpo
                 * enorme (riesgo de timeout o rechazo del servidor). Cada bloque se marca
                 * como sincronizado por separado, de modo que un fallo no descarta el
                 * progreso de los bloques ya confirmados.
                 */
                capturas.chunked(CAPTURAS_POR_ENVIO).forEach { bloque ->
                    try {
                        val relativeUrl =
                            "/api/website/v1/inventarios/" +
                                    "$empresaRut/SendRegistroInventario"

                        val headers = headersBuilder.build(
                            method = "POST",
                            relativeUrl = relativeUrl
                        )

                        val request = RegistroInventarioRequest(
                            InventarioId = inventarioId,
                            Capturas = bloque.map { item ->
                                RegistroInventarioCapturaRequest(
                                    UbicacionId = item.ubicacionId,
                                    DispositivoId = item.dispositivoId,
                                    ProductoCodigo = item.barcode,
                                    Cantidad = formatCantidad(
                                        item.quantity
                                    ),
                                    UnidadMedidaId =
                                        item.unitMeasureId.ifBlank {
                                            item.unitMeasure
                                        },
                                    Fecha = item.fecha,
                                    Hora = item.hora,
                                    RutUsuario = item.rutUsuario
                                )
                            }
                        )

                        val response = apiCallExecutor.execute {
                            api.sendRegistroInventario(
                                empresaRUT = empresaRut,
                                apiKey = headers.apiKey,
                                authorization = headers.authorization,
                                deviceSession = headers.deviceSession,
                                deviceSignature =
                                    headers.deviceSignature,
                                deviceTimestamp =
                                    headers.deviceTimestamp,
                                body = request
                            )
                        }

                        val ids = bloque.map { it.id }

                        inventoryRepository
                            .markCapturasSincronizadas(ids)

                        totalSincronizadas += ids.size

                        insertSyncLog(
                            remoteInventoryId = inventarioId,
                            inventoryName = inventoryName,
                            eventType = EVENT_CAPTURES_SENT,
                            capturesCount = ids.size,
                            inventoryStatus = inventoryStatus,
                            result = RESULT_ENVIADO,
                            connectionMode = MODE_ONLINE_API,
                            message = response.Respuesta
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?: "Capturas enviadas correctamente"
                        )

                        Log.d(
                            TAG,
                            "Inventario $inventarioId sincronizado. " +
                                    "RutUsuario=$rutUsuario, " +
                                    "capturas=${ids.size}"
                        )
                    } catch (throwable: Throwable) {
                        insertSyncLog(
                            remoteInventoryId = inventarioId,
                            inventoryName = inventoryName,
                            eventType = EVENT_CAPTURES_FAILED,
                            capturesCount = bloque.size,
                            inventoryStatus = inventoryStatus,
                            result = RESULT_ERROR,
                            connectionMode =
                                connectionModeForError(throwable),
                            message =
                                userSafeErrorMessage(throwable)
                        )

                        throw throwable
                    }
                }
            }

            Log.d(
                TAG,
                "Capturas sincronizadas: $totalSincronizadas"
            )

            totalSincronizadas
        }

    suspend fun syncFinishInventarios(): Int =
        withContext(Dispatchers.IO) {
            requireConfigured()
            requireActivated()
            ensureDeviceSession()

            val empresaRut =
                settings.empresaRut.value.trim()

            val pendientes =
                inventoryRepository
                    .getInventariosPendientesFinishSync()

            if (pendientes.isEmpty()) {
                Log.d(
                    TAG,
                    "No hay inventarios finalizados pendientes"
                )

                return@withContext 0
            }

            var total = 0

            pendientes.forEach { inventory ->
                val inventarioId =
                    inventory.remoteInventoryId.toLongOrNull()

                if (inventarioId == null) {
                    Log.w(
                        TAG,
                        "remoteInventoryId inválido: " +
                                inventory.remoteInventoryId
                    )

                    return@forEach
                }

                val rutUsuario =
                    inventory.rutUsuario.ifBlank {
                        throw IllegalStateException(
                            "El inventario no tiene usuario asociado."
                        )
                    }

                finishInventario(
                    inventory = inventory,
                    inventarioRemotoId = inventarioId,
                    rutUsuario = rutUsuario,
                    empresaRut = empresaRut,
                    onFinishSynced = { total++ }
                )

                Log.d(
                    TAG,
                    "FinishInventario OK. " +
                            "localId=${inventory.id}, " +
                            "remoteId=${inventory.remoteInventoryId}"
                )
            }

            total
        }

    suspend fun finishInventarioRemoto(
        inventoryId: Long,
        usuarioRut: String
    ): Boolean =
        withContext(Dispatchers.IO) {
            requireConfigured()
            requireActivated()
            ensureDeviceSession()

            val inventory =
                inventoryRepository.getInventoryById(inventoryId)
                    ?: throw IllegalStateException(
                        "No se encontró el inventario local."
                    )

            val inventarioRemotoId =
                inventory.remoteInventoryId.toLongOrNull()
                    ?: throw IllegalStateException(
                        "El identificador del inventario no es válido."
                    )

            val rutUsuario = usuarioRut
                .trim()
                .takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    "El usuario no se encuentra definido."
                )

            val empresaRut =
                settings.empresaRut.value.trim()

            finishInventario(
                inventory = inventory,
                inventarioRemotoId = inventarioRemotoId,
                rutUsuario = rutUsuario,
                empresaRut = empresaRut
            )

            true
        }

    suspend fun syncAllInventarioPendiente():
            InventarioSyncSummary =
        withContext(Dispatchers.IO) {
            val capturas = syncRegistroInventarios()
            val finalizados = syncFinishInventarios()

            InventarioSyncSummary(
                capturas = capturas,
                finalizados = finalizados
            )
        }

    suspend fun syncAllCatalogs(): SyncSummary =
        withContext(Dispatchers.IO) {
            requireConfigured()
            requireActivated()
            ensureDeviceSession()

            Log.d(TAG, "==== SYNC REGLAS ====")
            val reglas = syncReglas()

            Log.d(TAG, "==== SYNC USUARIOS ====")
            val users = syncUsers()

            Log.d(TAG, "==== SYNC UBICACIONES ====")
            val ubicaciones = syncUbicaciones()

            Log.d(TAG, "==== SYNC PRODUCTOS ====")
            val productos = syncProductos()

            Log.d(TAG, "==== SYNC UNIDAD MEDIDAS ====")
            val unidadMedidas = syncUnidadMedidas()

            Log.d(TAG, "==== SYNC INVENTARIOS REMOTOS ====")
            val inventarios = syncInventariosRemotos()

            val summary = SyncSummary(
                users = users,
                reglas = reglas,
                ubicaciones = ubicaciones,
                productos = productos,
                unidadMedidas = unidadMedidas,
                inventarios = inventarios,
                version = null
            )

            Log.d(
                TAG,
                "Sincronización completa: $summary"
            )

            summary
        }

    private suspend fun finishInventario(
        inventory: InventoryEntity,
        inventarioRemotoId: Long,
        rutUsuario: String,
        empresaRut: String,
        onFinishSynced: () -> Unit = {}
    ) {
        try {
            val relativeUrl =
                "/api/website/v1/inventarios/" +
                        "$empresaRut/FinishInventario"

            val headers = headersBuilder.build(
                method = "POST",
                relativeUrl = relativeUrl
            )

            val response = apiCallExecutor.execute {
                api.finishInventario(
                    empresaRUT = empresaRut,
                    apiKey = headers.apiKey,
                    authorization = headers.authorization,
                    deviceSession = headers.deviceSession,
                    deviceSignature = headers.deviceSignature,
                    deviceTimestamp = headers.deviceTimestamp,
                    body = FinalizarInventarioRequest(
                        InventarioId = inventarioRemotoId,
                        UsuarioRUT = rutUsuario
                    )
                )
            }

            inventoryRepository.markFinishSynced(inventory.id)
            onFinishSynced()

            insertSyncLog(
                remoteInventoryId = inventory.remoteInventoryId,
                inventoryName = inventory.name.ifBlank {
                    "Inventario ${inventory.remoteInventoryId}"
                },
                eventType = EVENT_INVENTORY_FINISHED,
                capturesCount = 0,
                inventoryStatus = InventoryStatus.FINISHED.name,
                result = RESULT_ENVIADO,
                connectionMode = MODE_ONLINE_API,
                message = response.Respuesta
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Inventario finalizado correctamente"
            )
        } catch (throwable: Throwable) {
            insertSyncLog(
                remoteInventoryId = inventory.remoteInventoryId,
                inventoryName = inventory.name.ifBlank {
                    "Inventario ${inventory.remoteInventoryId}"
                },
                eventType = EVENT_FINISH_FAILED,
                capturesCount = 0,
                inventoryStatus = inventory.status,
                result = RESULT_ERROR,
                connectionMode = connectionModeForError(throwable),
                message = userSafeErrorMessage(throwable)
            )

            throw throwable
        }
    }

    private fun isNoPublishedVersionError(
        throwable: Throwable
    ): Boolean {
        if (throwable !is ApiException) {
            return false
        }

        val httpCode = throwable.httpCode ?: 0

        val codigoError = throwable.codigoError
            ?.trim()
            ?.uppercase(Locale.ROOT)
            .orEmpty()

        val respuesta = throwable.message
            .trim()
            .lowercase(Locale.ROOT)

        if (httpCode != 404) {
            return false
        }

        val matchesErrorCode =
            codigoError == "VERSION_NO_PUBLICADA" ||
                    codigoError == "VERSION_NOT_FOUND" ||
                    codigoError == "ERR_VERSION_NOT_FOUND" ||
                    codigoError == "ERR_VERSION_NO_PUBLICADA"

        val matchesResponse =
            respuesta.contains(
                "no se encontró una versión vigente publicada"
            ) ||
                    respuesta.contains(
                        "no existe una versión vigente publicada"
                    ) ||
                    respuesta.contains(
                        "no hay una versión vigente publicada"
                    )

        return matchesErrorCode || matchesResponse
    }

    private fun formatCantidad(
        value: Double
    ): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(
                Locale.US,
                "%.3f",
                value
            )
                .trimEnd('0')
                .trimEnd('.')
        }
    }

    private fun connectionModeForError(
        throwable: Throwable
    ): String {
        return when {
            throwable is IOException ->
                MODE_LOCAL_ROOM

            throwable is ApiException &&
                    throwable.cause is IOException ->
                MODE_LOCAL_ROOM

            else ->
                MODE_ONLINE_API
        }
    }

    private fun userSafeErrorMessage(
        throwable: Throwable
    ): String {
        return when (throwable) {
            is ApiException -> {
                throwable.message
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?: "No fue posible completar la operación."
            }

            is IOException -> {
                "No fue posible conectarse al servidor."
            }

            else -> {
                "No fue posible completar la operación."
            }
        }
    }

    private suspend fun insertSyncLog(
        remoteInventoryId: String,
        inventoryName: String,
        eventType: String,
        capturesCount: Int,
        inventoryStatus: String,
        result: String,
        connectionMode: String,
        message: String?
    ) {
        runCatching {
            syncLogRepository.insert(
                SyncLogEntity(
                    remoteInventoryId = remoteInventoryId,
                    inventoryName = inventoryName,
                    eventType = eventType,
                    capturesCount = capturesCount,
                    inventoryStatus = inventoryStatus,
                    result = result,
                    connectionMode = connectionMode,
                    sentAt = System.currentTimeMillis(),
                    message = message
                )
            )
        }.onFailure { throwable ->
            Log.e(
                TAG,
                "No se pudo guardar SyncLog",
                throwable
            )
        }
    }

    companion object {
        private const val TAG = "SyncService"

        /**
         * Máximo de capturas por request de SendRegistroInventario.
         */
        private const val CAPTURAS_POR_ENVIO = 500

        private const val EVENT_CAPTURES_SENT =
            "CAPTURES_SENT"

        private const val EVENT_CAPTURES_FAILED =
            "CAPTURES_FAILED"

        private const val EVENT_INVENTORY_FINISHED =
            "INVENTORY_FINISHED"

        private const val EVENT_FINISH_FAILED =
            "FINISH_FAILED"

        private const val RESULT_ENVIADO =
            "ENVIADO"

        private const val RESULT_ERROR =
            "ERROR"

        private const val MODE_ONLINE_API =
            "ONLINE_API"

        private const val MODE_LOCAL_ROOM =
            "LOCAL_ROOM"
    }
}

data class SyncSummary(
    val users: Int,
    val reglas: Int,
    val ubicaciones: Int,
    val productos: Int,
    val unidadMedidas: Int,
    val inventarios: Int,
    val version: String?
)

data class InventarioSyncSummary(
    val capturas: Int,
    val finalizados: Int
)

data class VersionCheckStartupResult(
    val versionName: String?,
    val hasNewVersion: Boolean,
    val mandatory: Boolean,
    val apkFileName: String,
    val apkUrl: String?
)
