package com.diprotec.inventariozebratc27.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    indices = [
        Index(value = ["codigoSecundario"]),
        Index(value = ["descripcion"]),
        Index(value = ["estado"]),
        Index(value = ["rutEmpresa"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val codigo: String,
    val codigoSecundario: String?,
    val descripcion: String?,
    val estado: Boolean,
    val rutEmpresa: String?
)