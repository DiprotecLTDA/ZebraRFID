package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.data.local.dao.SyncLogDao
import com.diprotec.inventariozebratc27.data.local.entity.SyncLogEntity
import kotlinx.coroutines.flow.Flow

class SyncLogRepository(
    private val dao: SyncLogDao
) {

    fun observeAll(): Flow<List<SyncLogEntity>> =
        dao.observeAll()

    suspend fun insert(log: SyncLogEntity) {
        dao.insert(log)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}