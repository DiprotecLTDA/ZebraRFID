package com.diprotec.inventariozebratc27.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventarios_remotos",
    indices = [
        Index(value = ["estado"]),
        Index(value = ["fecha", "hora"]),
        Index(value = ["rutAdministrador"]),
        Index(value = ["rutEmpresa"])
    ]
)
data class InventoryRemoteEntity(
    @PrimaryKey
    val id: String,
    val descripcion: String?,
    val fecha: String?,
    val hora: String?,
    val desde: String?,
    val hasta: String?,
    val rutAdministrador: String?,
    val estado: Boolean,
    val rutEmpresa: String?
)