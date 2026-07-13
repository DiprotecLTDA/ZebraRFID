package com.diprotec.inventariozebratc27.di

import android.content.Context
import androidx.room.Room
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
import com.diprotec.inventariozebratc27.data.local.database.AppDatabase
import com.diprotec.inventariozebratc27.data.local.database.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "inventario_zebra.db"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_28_29,
                DatabaseMigrations.MIGRATION_29_30
            )
            .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideInventoryDao(database: AppDatabase): InventoryDao {
        return database.inventoryDao()
    }

    @Provides
    fun provideInventoryItemDao(database: AppDatabase): InventoryItemDao {
        return database.inventoryItemDao()
    }

    @Provides
    fun provideRuleDao(database: AppDatabase): RuleDao {
        return database.reglaDao()
    }

    @Provides
    fun provideLocationDao(database: AppDatabase): LocationDao {
        return database.ubicacionDao()
    }

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao {
        return database.productoDao()
    }

    @Provides
    fun provideUnitMeasureDao(database: AppDatabase): UnitMeasureDao {
        return database.unidadMedidaDao()
    }

    @Provides
    fun provideInventoryRemoteDao(database: AppDatabase): InventoryRemoteDao {
        return database.inventarioRemotoDao()
    }

    @Provides
    fun provideSyncLogDao(database: AppDatabase): SyncLogDao {
        return database.syncLogDao()
    }

    @Provides
    fun provideNetworkUsageDao(database: AppDatabase): NetworkUsageDao {
        return database.networkUsageDao()
    }

    @Provides
    fun provideBarcodeDao(database: AppDatabase): BarcodeDao {
        return database.barcodeDao()
    }
}