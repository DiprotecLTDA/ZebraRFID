package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.gs1.Gs1EpcDecoder
import com.diprotec.inventariozebratc27.data.local.dao.InventoryDao
import com.diprotec.inventariozebratc27.data.local.dao.InventoryItemDao
import com.diprotec.inventariozebratc27.data.local.dao.LocationDao
import com.diprotec.inventariozebratc27.data.local.dao.ProductDao
import com.diprotec.inventariozebratc27.data.local.dao.RuleDao
import com.diprotec.inventariozebratc27.data.local.dao.UserDao
import com.diprotec.inventariozebratc27.data.local.entity.InventoryEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryItemEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventariozebratc27.data.local.entity.LocationEntity
import com.diprotec.inventariozebratc27.data.local.entity.RuleEntity
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryGroupedRow
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryReadingMode
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow

data class RfidInventoryRegisterResult(
    val epcRaw: String,
    val epcNormalized: String,
    val gs1Type: String,
    val gs1Key: String,
    val gtin: String?,
    val serial: String?,
    val barcodeSaved: String,
    val isDuplicate: Boolean
)

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val inventoryItemDao: InventoryItemDao,
    private val productDao: ProductDao,
    private val locationDao: LocationDao,
    private val ruleDao: RuleDao,
    private val userDao: UserDao,
    private val settings: SettingsManager
) {

    fun observeUbicacionesActivas(): Flow<List<LocationEntity>> =
        locationDao.observeActivas()

    suspend fun findDescriptionByBarcode(barcode: String): String {
        return productDao.findByCodigo(barcode.trim())
            ?.descripcion
            ?.takeIf { it.isNotBlank() }
            ?: "Producto no registrado"
    }

    suspend fun createInventoryFromRemote(
        remote: InventoryRemoteEntity,
        rutUsuario: String,
        readingMode: InventoryReadingMode = InventoryReadingMode.LASER
    ): Long {
        val usuario = rutUsuario.trim()
        val remoteId = remote.id.trim()

        require(usuario.isNotBlank()) {
            "No hay usuario logueado para crear inventario"
        }

        require(remoteId.isNotBlank()) {
            "Inventario remoto inválido"
        }

        val existing = inventoryDao.getByRemoteIdAndUsuario(remoteId, usuario)
        if (existing != null) return existing.id

        return inventoryDao.insert(
            InventoryEntity(
                remoteInventoryId = remoteId,
                rutUsuario = usuario,
                name = remote.descripcion.orEmpty(),
                fecha = remote.fecha,
                hora = remote.hora,
                desde = remote.desde,
                hasta = remote.hasta,
                rutAdministrador = remote.rutAdministrador,
                rutEmpresa = remote.rutEmpresa,
                status = InventoryStatus.PENDING.name,
                tipoLectura = readingMode.value
            )
        )
    }

    suspend fun getInventoryById(inventoryId: Long): InventoryEntity? =
        inventoryDao.getById(inventoryId)

    fun observeInventoriesByStatus(status: InventoryStatus): Flow<List<InventoryEntity>> =
        inventoryDao.observeByStatus(status.name)

    fun observeInventoriesByStatusAndUsuario(
        status: InventoryStatus,
        rutUsuario: String
    ): Flow<List<InventoryEntity>> =
        inventoryDao.observeByStatusAndUsuario(status.name, rutUsuario.trim())

    fun observeAllInventories(): Flow<List<InventoryEntity>> =
        inventoryDao.observeAll()

    fun observeAllInventoriesByUsuario(rutUsuario: String): Flow<List<InventoryEntity>> =
        inventoryDao.observeAllByUsuario(rutUsuario.trim())

    fun observePendingInventories(): Flow<List<InventoryEntity>> =
        observeInventoriesByStatus(InventoryStatus.PENDING)

    suspend fun getInventoryByRemoteId(remoteInventoryId: String): InventoryEntity? =
        inventoryDao.getByRemoteId(remoteInventoryId)

    suspend fun getInventoryByRemoteIdAndUsuario(
        remoteInventoryId: String,
        rutUsuario: String
    ): InventoryEntity? =
        inventoryDao.getByRemoteIdAndUsuario(
            remoteInventoryId = remoteInventoryId,
            rutUsuario = rutUsuario.trim()
        )

    suspend fun expirePendingInventories() {
        expirePendingInventories(
            inventoryDao.getByStatus(InventoryStatus.PENDING.name)
        )
    }

    suspend fun expirePendingInventoriesByUsuario(rutUsuario: String) {
        expirePendingInventories(
            inventoryDao.getByStatusAndUsuario(
                status = InventoryStatus.PENDING.name,
                rutUsuario = rutUsuario.trim()
            )
        )
    }

    fun isInventoryVisible(inventory: InventoryEntity): Boolean =
        !isExpired(inventory.hasta)

    suspend fun finalizeInventory(inventoryId: Long) {
        inventoryDao.updateStatus(
            inventoryId = inventoryId,
            status = InventoryStatus.FINISHED.name,
            finishedAt = System.currentTimeMillis()
        )
    }

    suspend fun registerInventoryItem(
        inventoryId: Long,
        ubicacionId: String,
        ubicacionNombre: String,
        barcode: String,
        quantity: Double,
        unitMeasure: String,
        unitMeasureId: String,
        rutUsuario: String
    ) {
        val inventory = inventoryDao.getById(inventoryId) ?: return
        val now = Date()
        val normalizedBarcode = barcode.trim()
        val description = findDescriptionByBarcode(normalizedBarcode)

        val dispositivoId = settings.deviceId.value.trim()
        require(dispositivoId.isNotBlank()) {
            "DispositivoId no configurado. Debe reactivar el dispositivo."
        }

        inventoryItemDao.insert(
            buildInventoryItem(
                inventory = inventory,
                inventoryId = inventoryId,
                ubicacionId = ubicacionId,
                ubicacionNombre = ubicacionNombre,
                dispositivoId = dispositivoId,
                barcode = normalizedBarcode,
                description = description,
                quantity = quantity,
                unitMeasure = unitMeasure,
                unitMeasureId = unitMeasureId,
                fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now),
                hora = SimpleDateFormat("HH:mm:ss", Locale.US).format(now),
                rutUsuario = rutUsuario,
                rfidEpcRaw = null
            )
        )
    }

    suspend fun registerRfidInventoryItem(
        inventoryId: Long,
        ubicacionId: String,
        ubicacionNombre: String,
        epc: String,
        quantity: Double,
        unitMeasure: String,
        unitMeasureId: String,
        rutUsuario: String
    ): RfidInventoryRegisterResult {
        val inventory = inventoryDao.getById(inventoryId)
            ?: throw IllegalStateException("No se encontró el inventario")

        val decoded = Gs1EpcDecoder.decode(epc)

        val uniqueRfidKey = decoded.gs1Key
            .takeIf { it.isNotBlank() }
            ?: decoded.epcNormalized
                .takeIf { it.isNotBlank() }
            ?: decoded.epcRaw
                .trim()
                .uppercase()

        require(uniqueRfidKey.isNotBlank()) {
            "La lectura RFID no contiene un EPC válido"
        }

        val alreadyExists = inventoryItemDao.countValidRfidByGs1Key(
            inventoryId = inventoryId,
            rfidGs1Key = uniqueRfidKey
        ) > 0

        val barcodeToSave = decoded.gtin
            ?.takeIf { it.isNotBlank() }
            ?: decoded.epcNormalized
                .takeIf { it.isNotBlank() }
            ?: decoded.epcRaw
                .trim()
                .uppercase()

        if (alreadyExists) {
            return RfidInventoryRegisterResult(
                epcRaw = decoded.epcRaw,
                epcNormalized = decoded.epcNormalized,
                gs1Type = decoded.gs1Type.ifBlank { "UNKNOWN" },
                gs1Key = uniqueRfidKey,
                gtin = decoded.gtin,
                serial = decoded.serial,
                barcodeSaved = barcodeToSave,
                isDuplicate = true
            )
        }

        val now = Date()
        val description = findDescriptionByBarcode(barcodeToSave)

        val dispositivoId = settings.deviceId.value.trim()
        require(dispositivoId.isNotBlank()) {
            "DispositivoId no configurado. Debe reactivar el dispositivo."
        }

        inventoryItemDao.insert(
            buildInventoryItem(
                inventory = inventory,
                inventoryId = inventoryId,
                ubicacionId = ubicacionId,
                ubicacionNombre = ubicacionNombre,
                dispositivoId = dispositivoId,
                barcode = barcodeToSave,
                description = description,
                quantity = quantity,
                unitMeasure = unitMeasure,
                unitMeasureId = unitMeasureId,
                fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now),
                hora = SimpleDateFormat("HH:mm:ss", Locale.US).format(now),
                rutUsuario = rutUsuario,
                rfidEpcRaw = decoded.epcRaw,
                rfidEpcNormalized = decoded.epcNormalized.ifBlank {
                    decoded.epcRaw.trim().uppercase()
                },
                rfidGs1Type = decoded.gs1Type.ifBlank { "UNKNOWN" },
                rfidGs1Key = uniqueRfidKey,
                rfidGtin = decoded.gtin,
                rfidSerial = decoded.serial
            )
        )

        return RfidInventoryRegisterResult(
            epcRaw = decoded.epcRaw,
            epcNormalized = decoded.epcNormalized.ifBlank {
                decoded.epcRaw.trim().uppercase()
            },
            gs1Type = decoded.gs1Type.ifBlank { "UNKNOWN" },
            gs1Key = uniqueRfidKey,
            gtin = decoded.gtin,
            serial = decoded.serial,
            barcodeSaved = barcodeToSave,
            isDuplicate = false
        )
    }

    fun observeInventoryItems(inventoryId: Long): Flow<List<InventoryItemEntity>> =
        inventoryItemDao.observeInventoryItems(inventoryId)

    fun observeGroupedInventoryItems(inventoryId: Long): Flow<List<InventoryGroupedRow>> =
        inventoryItemDao.observeGroupedInventoryItems(inventoryId)

    suspend fun canDeleteInventoryItems(inventoryId: Long): Boolean {
        val inventory = inventoryDao.getById(inventoryId) ?: return false

        if (inventory.status == InventoryStatus.FINISHED.name) {
            return false
        }

        return canDeleteByRule(inventory)
    }

    suspend fun deleteInventoryItem(
        inventoryId: Long,
        itemId: Long
    ) {
        val inventory = inventoryDao.getById(inventoryId)
            ?: throw IllegalStateException("No se encontró el inventario")

        if (inventory.status == InventoryStatus.FINISHED.name) {
            throw IllegalStateException("No se pueden eliminar capturas de un inventario finalizado")
        }

        if (!canDeleteByRule(inventory)) {
            throw IllegalStateException(
                "La eliminación no está permitida para este inventario o perfil"
            )
        }

        inventoryItemDao.deleteById(itemId)
    }

    suspend fun getCapturasPendientesSincronizar(): List<InventoryItemEntity> =
        inventoryItemDao.getPendientesSincronizar()

    suspend fun markCapturasSincronizadas(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            inventoryItemDao.markSincronizados(ids)
        }
    }

    suspend fun getInventariosPendientesFinishSync(): List<InventoryEntity> =
        inventoryDao.getPendingFinishSync(InventoryStatus.FINISHED.name)

    suspend fun markFinishSynced(inventoryId: Long) {
        inventoryDao.markFinishSynced(
            inventoryId = inventoryId,
            finishSyncedAt = System.currentTimeMillis()
        )
    }

    private suspend fun expirePendingInventories(
        pending: List<InventoryEntity>
    ) {
        pending
            .filter { isExpired(it.hasta) }
            .forEach { inventory ->
                inventoryDao.updateStatus(
                    inventoryId = inventory.id,
                    status = InventoryStatus.EXPIRED.name,
                    finishedAt = System.currentTimeMillis()
                )
            }
    }

    private fun buildInventoryItem(
        inventory: InventoryEntity,
        inventoryId: Long,
        ubicacionId: String,
        ubicacionNombre: String,
        dispositivoId: String,
        barcode: String,
        description: String,
        quantity: Double,
        unitMeasure: String,
        unitMeasureId: String,
        fecha: String,
        hora: String,
        rutUsuario: String,
        rfidEpcRaw: String?,
        rfidEpcNormalized: String? = null,
        rfidGs1Type: String? = null,
        rfidGs1Key: String? = null,
        rfidGtin: String? = null,
        rfidSerial: String? = null
    ): InventoryItemEntity {
        return InventoryItemEntity(
            inventoryId = inventoryId,
            remoteInventoryId = inventory.remoteInventoryId,
            ubicacionId = ubicacionId,
            ubicacionNombre = ubicacionNombre,
            dispositivoId = dispositivoId,
            barcode = barcode,
            description = description,
            quantity = quantity,
            unitMeasure = unitMeasure,
            unitMeasureId = unitMeasureId,
            fecha = fecha,
            hora = hora,
            rutUsuario = rutUsuario,
            sincronizado = false,
            rfidEpcRaw = rfidEpcRaw,
            rfidEpcNormalized = rfidEpcNormalized,
            rfidGs1Type = rfidGs1Type,
            rfidGs1Key = rfidGs1Key,
            rfidGtin = rfidGtin,
            rfidSerial = rfidSerial,
            rfidDuplicado = false
        )
    }

    private suspend fun canDeleteByRule(inventory: InventoryEntity): Boolean {
        val rutEmpresa = inventory.rutEmpresa.orEmpty().trim()
        val rutUsuario = inventory.rutUsuario.trim()

        if (rutEmpresa.isBlank() || rutUsuario.isBlank()) {
            return false
        }

        val user = userDao.findByRut(rutUsuario) ?: return false

        val rule = ruleDao.getByEmpresaAndPerfil(
            rutEmpresa = rutEmpresa,
            perfil = user.perfil.orEmpty().trim(),
            perfilId = user.perfilId?.toString()?.trim().orEmpty()
        ) ?: ruleDao.getByEmpresa(rutEmpresa)

        return rule?.isActiveForApiZero() == true &&
                rule.eliminaEnviados.isEnabledForApiZero()
    }

    private fun RuleEntity.isActiveForApiZero(): Boolean {
        return !estado
    }

    private fun String?.isEnabledForApiZero(): Boolean {
        return this.orEmpty().trim() == "0"
    }

    private fun isExpired(hasta: String?): Boolean {
        if (hasta.isNullOrBlank()) return false

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            isLenient = false
        }

        val limit = runCatching { formatter.parse(hasta) }.getOrNull()
            ?: return false

        val today = formatter.parse(formatter.format(Date())) ?: Date()

        return today.after(limit)
    }
}
