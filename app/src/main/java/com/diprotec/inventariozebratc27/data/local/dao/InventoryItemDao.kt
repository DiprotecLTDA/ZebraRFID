package com.diprotec.inventariozebratc27.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diprotec.inventariozebratc27.data.local.entity.InventoryItemEntity
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryGroupedRow
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity)

    @Query(
        """
        SELECT * 
        FROM inventory_items 
        WHERE inventoryId = :inventoryId 
        ORDER BY createdAt DESC
        """
    )
    fun observeInventoryItems(inventoryId: Long): Flow<List<InventoryItemEntity>>

    @Query(
        """
        SELECT 
            TRIM(barcode) AS barcode,
            MAX(description) AS description,
            MAX(unitMeasure) AS unitMeasure,
            ubicacionId AS ubicacionId,
            MAX(ubicacionNombre) AS ubicacionNombre,
            SUM(quantity) AS totalQuantity,
            COUNT(*) AS totalRows
        FROM inventory_items
        WHERE inventoryId = :inventoryId
        AND rfidDuplicado = 0
        GROUP BY 
            TRIM(barcode),
            ubicacionId,
            unitMeasureId
        ORDER BY MAX(description) ASC
        """
    )
    fun observeGroupedInventoryItems(inventoryId: Long): Flow<List<InventoryGroupedRow>>

    @Query("DELETE FROM inventory_items WHERE inventoryId = :inventoryId")
    suspend fun deleteByInventory(inventoryId: Long)

    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query(
        """
        SELECT * 
        FROM inventory_items 
        WHERE sincronizado = 0
        AND rfidDuplicado = 0
        ORDER BY remoteInventoryId, createdAt ASC
        """
    )
    suspend fun getPendientesSincronizar(): List<InventoryItemEntity>

    @Query("UPDATE inventory_items SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun markSincronizados(ids: List<Long>)

    @Query(
        """
        SELECT COUNT(*) 
        FROM inventory_items
        WHERE inventoryId = :inventoryId
        AND rfidGs1Key = :rfidGs1Key
        AND rfidDuplicado = 0
        """
    )
    suspend fun countValidRfidByGs1Key(
        inventoryId: Long,
        rfidGs1Key: String
    ): Int
}