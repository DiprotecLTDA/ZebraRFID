package com.diprotec.inventariozebratc27.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.diprotec.inventariozebratc27.data.local.dao.BarcodeDao
import com.diprotec.inventariozebratc27.data.local.dao.InventoryDao
import com.diprotec.inventariozebratc27.data.local.dao.InventoryItemDao
import com.diprotec.inventariozebratc27.data.local.dao.InventoryRemoteDao
import com.diprotec.inventariozebratc27.data.local.dao.LocationDao
import com.diprotec.inventariozebratc27.data.local.dao.NetworkUsageDao
import com.diprotec.inventariozebratc27.data.local.dao.ProductDao
import com.diprotec.inventariozebratc27.data.local.dao.RuleDao
import com.diprotec.inventariozebratc27.data.local.dao.SyncLogDao
import com.diprotec.inventariozebratc27.data.local.dao.UnitMeasureDao
import com.diprotec.inventariozebratc27.data.local.dao.UserDao
import com.diprotec.inventariozebratc27.data.local.entity.BarcodeEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryItemEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteUserEntity
import com.diprotec.inventariozebratc27.data.local.entity.LocationEntity
import com.diprotec.inventariozebratc27.data.local.entity.NetworkUsageEntity
import com.diprotec.inventariozebratc27.data.local.entity.ProductEntity
import com.diprotec.inventariozebratc27.data.local.entity.RuleEntity
import com.diprotec.inventariozebratc27.data.local.entity.SyncLogEntity
import com.diprotec.inventariozebratc27.data.local.entity.UnitMeasureEntity
import com.diprotec.inventariozebratc27.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        InventoryEntity::class,
        InventoryItemEntity::class,
        RuleEntity::class,
        LocationEntity::class,
        ProductEntity::class,
        UnitMeasureEntity::class,
        InventoryRemoteEntity::class,
        InventoryRemoteUserEntity::class,
        SyncLogEntity::class,
        NetworkUsageEntity::class,
        BarcodeEntity::class
    ],
    version = 30,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun inventoryDao(): InventoryDao

    abstract fun inventoryItemDao(): InventoryItemDao

    abstract fun reglaDao(): RuleDao

    abstract fun ubicacionDao(): LocationDao

    abstract fun productoDao(): ProductDao

    abstract fun unidadMedidaDao(): UnitMeasureDao

    abstract fun inventarioRemotoDao(): InventoryRemoteDao

    abstract fun syncLogDao(): SyncLogDao

    abstract fun networkUsageDao(): NetworkUsageDao

    abstract fun barcodeDao(): BarcodeDao
}