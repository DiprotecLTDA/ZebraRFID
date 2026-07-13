package com.diprotec.inventariozebratc27.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diprotec.inventariozebratc27.data.local.entity.BarcodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BarcodeEntity)

    @Query("SELECT * FROM barcodes ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BarcodeEntity>>

    @Query("SELECT * FROM barcodes WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPending(limit: Int = 500): List<BarcodeEntity>

    @Query("UPDATE barcodes SET synced = 1, syncedAt = :at WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>, at: Long)

    @Query("DELETE FROM barcodes WHERE synced = 1")
    suspend fun deleteSent(): Int
}