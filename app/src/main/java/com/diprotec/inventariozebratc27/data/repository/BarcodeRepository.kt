package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.data.local.entity.BarcodeEntity
import kotlinx.coroutines.flow.Flow

interface BarcodeRepository {

    suspend fun save(
        rut: String,
        nombre: String?,
        empresa: String?,
        patente: String?,
        comentario: String?,
        sentido: Int?,
        evidenceBytes: ByteArray?,
        evidenceMime: String?,
        evidenceSize: Int?
    )

    fun observe(): Flow<List<BarcodeEntity>>

    suspend fun getPending(limit: Int = 500): List<BarcodeEntity>

    suspend fun markSynced(ids: List<Long>, at: Long)

    suspend fun deleteSent(): Int
}