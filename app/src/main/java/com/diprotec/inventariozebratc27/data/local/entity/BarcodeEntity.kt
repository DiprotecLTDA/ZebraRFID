package com.diprotec.inventariozebratc27.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "barcodes",
    indices = [Index("synced"), Index("timestamp")]
)
data class BarcodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val rut: String,
    val timestamp: Long,
    val nombre: String? = null,
    val empresa: String? = null,
    val patente: String? = null,
    val comentario: String? = null,

    /**
     * 0 = entrada, 1 = salida
     */
    val sentido: Int? = null,

    val synced: Boolean = false,
    val syncedAt: Long? = null,

    val evidenceBytes: ByteArray? = null,
    val evidenceMime: String? = null,
    val evidenceSize: Int? = null
)