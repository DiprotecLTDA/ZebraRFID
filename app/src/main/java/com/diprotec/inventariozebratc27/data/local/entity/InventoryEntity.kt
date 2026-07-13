package com.diprotec.inventariozebratc27.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryStatus

@Entity(
    tableName = "inventories",
    indices = [Index(value = ["remoteInventoryId", "rutUsuario"], unique = true)]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val remoteInventoryId: String,
    val rutUsuario: String = "",

    val name: String,
    val areaUbicacion: String = "",

    val fecha: String? = null,
    val hora: String? = null,
    val desde: String? = null,
    val hasta: String? = null,

    val rutAdministrador: String? = null,
    val rutEmpresa: String? = null,

    val status: String = InventoryStatus.PENDING.name,

    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,

    /**
     * Tipo de lectura utilizado para este inventario.
     *
     * 0 = Láser / código de barras
     * 1 = RFID
     */
    val tipoLectura: Int = 0,

    val finishSynced: Boolean = false,
    val finishSyncedAt: Long? = null
)