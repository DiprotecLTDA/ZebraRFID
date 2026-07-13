package com.diprotec.inventariozebratc27.service

import com.diprotec.inventariozebratc27.data.local.entity.BarcodeEntity
import com.diprotec.inventariozebratc27.data.repository.BarcodeRepository
import kotlinx.coroutines.flow.Flow

class BarcodeService(
    private val barcodes: BarcodeRepository
) {

    suspend fun saveScan(
        rut: String,
        nombre: String?,
        empresa: String?,
        patente: String?,
        comentario: String?,
        sentido: Int?,
        evidenceBytes: ByteArray?,
        evidenceMime: String?,
        evidenceSize: Int?
    ) {
        barcodes.save(
            rut = rut,
            nombre = nombre,
            empresa = empresa,
            patente = patente,
            comentario = comentario,
            sentido = sentido,
            evidenceBytes = evidenceBytes,
            evidenceMime = evidenceMime,
            evidenceSize = evidenceSize
        )
    }

    fun observeScans(): Flow<List<BarcodeEntity>> =
        barcodes.observe()

    suspend fun pending(limit: Int = 500): List<BarcodeEntity> =
        barcodes.getPending(limit)

    suspend fun markSynced(ids: List<Long>) {
        barcodes.markSynced(ids, System.currentTimeMillis())
    }

    suspend fun deleteSent(): Int {
        return barcodes.deleteSent()
    }
}