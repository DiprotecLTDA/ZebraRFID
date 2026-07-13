package com.diprotec.inventariozebratc27.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["inventoryId"]),
        Index(value = ["inventoryId", "createdAt"]),
        Index(value = ["sincronizado", "remoteInventoryId", "createdAt"]),
        Index(value = ["remoteInventoryId"]),
        Index(value = ["rutUsuario"]),
        Index(value = ["inventoryId", "barcode", "ubicacionId"]),

        Index(value = ["inventoryId", "rfidGs1Key"]),
        Index(value = ["inventoryId", "rfidEpcNormalized"]),
        Index(value = ["sincronizado", "rfidDuplicado", "remoteInventoryId", "createdAt"])
    ]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val inventoryId: Long,
    val remoteInventoryId: String,

    val ubicacionId: String,
    val ubicacionNombre: String,

    val dispositivoId: String,

    val barcode: String,
    val description: String,
    val quantity: Double,

    val unitMeasure: String,
    val unitMeasureId: String = "",

    val fecha: String,
    val hora: String,
    val rutUsuario: String,

    val createdAt: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false,

    val rfidEpcRaw: String? = null,
    val rfidEpcNormalized: String? = null,
    val rfidGs1Type: String? = null,
    val rfidGs1Key: String? = null,
    val rfidGtin: String? = null,
    val rfidSerial: String? = null,
    val rfidDuplicado: Boolean = false
)