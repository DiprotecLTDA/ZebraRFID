package com.diprotec.inventariozebratc27.di

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
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
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.repository.InventoryRemoteRepository
import com.diprotec.inventariozebratc27.data.repository.InventoryRemoteRepositoryImpl
import com.diprotec.inventariozebratc27.data.repository.InventoryRepository
import com.diprotec.inventariozebratc27.data.repository.LocationRepository
import com.diprotec.inventariozebratc27.data.repository.LocationRepositoryImpl
import com.diprotec.inventariozebratc27.data.repository.NetworkUsageRepository
import com.diprotec.inventariozebratc27.data.repository.ProductRepository
import com.diprotec.inventariozebratc27.data.repository.ProductRepositoryImpl
import com.diprotec.inventariozebratc27.data.repository.RuleRepository
import com.diprotec.inventariozebratc27.data.repository.RuleRepositoryImpl
import com.diprotec.inventariozebratc27.data.repository.SyncLogRepository
import com.diprotec.inventariozebratc27.data.repository.UnitMeasureRepository
import com.diprotec.inventariozebratc27.data.repository.UnitMeasureRepositoryImpl
import com.diprotec.inventariozebratc27.data.repository.UserRepository
import com.diprotec.inventariozebratc27.data.repository.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        apiService: ApiService,
        settingsManager: SettingsManager,
        protectedHeadersBuilder: ProtectedHeadersBuilder,
        apiCallExecutor: ApiCallExecutor
    ): UserRepository {
        return UserRepositoryImpl(
            userDao = userDao,
            api = apiService,
            settings = settingsManager,
            headersBuilder = protectedHeadersBuilder,
            apiCallExecutor = apiCallExecutor
        )
    }

    @Provides
    @Singleton
    fun provideRuleRepository(
        ruleDao: RuleDao,
        apiService: ApiService,
        settingsManager: SettingsManager,
        protectedHeadersBuilder: ProtectedHeadersBuilder,
        apiCallExecutor: ApiCallExecutor
    ): RuleRepository {
        return RuleRepositoryImpl(
            ruleDao = ruleDao,
            api = apiService,
            settings = settingsManager,
            headersBuilder = protectedHeadersBuilder,
            apiCallExecutor = apiCallExecutor
        )
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        locationDao: LocationDao,
        apiService: ApiService,
        settingsManager: SettingsManager,
        protectedHeadersBuilder: ProtectedHeadersBuilder,
        apiCallExecutor: ApiCallExecutor
    ): LocationRepository {
        return LocationRepositoryImpl(
            locationDao = locationDao,
            api = apiService,
            settings = settingsManager,
            headersBuilder = protectedHeadersBuilder,
            apiCallExecutor = apiCallExecutor
        )
    }

    @Provides
    @Singleton
    fun provideProductRepository(
        productDao: ProductDao,
        apiService: ApiService,
        settingsManager: SettingsManager,
        protectedHeadersBuilder: ProtectedHeadersBuilder,
        apiCallExecutor: ApiCallExecutor
    ): ProductRepository {
        return ProductRepositoryImpl(
            productDao = productDao,
            api = apiService,
            settings = settingsManager,
            headersBuilder = protectedHeadersBuilder,
            apiCallExecutor = apiCallExecutor
        )
    }

    @Provides
    @Singleton
    fun provideUnitMeasureRepository(
        unitMeasureDao: UnitMeasureDao,
        apiService: ApiService,
        settingsManager: SettingsManager,
        protectedHeadersBuilder: ProtectedHeadersBuilder,
        apiCallExecutor: ApiCallExecutor
    ): UnitMeasureRepository {
        return UnitMeasureRepositoryImpl(
            unitMeasureDao = unitMeasureDao,
            api = apiService,
            settings = settingsManager,
            headersBuilder = protectedHeadersBuilder,
            apiCallExecutor = apiCallExecutor
        )
    }

    @Provides
    @Singleton
    fun provideInventoryRemoteRepository(
        inventoryRemoteDao: InventoryRemoteDao,
        apiService: ApiService,
        settingsManager: SettingsManager,
        protectedHeadersBuilder: ProtectedHeadersBuilder,
        apiCallExecutor: ApiCallExecutor
    ): InventoryRemoteRepository {
        return InventoryRemoteRepositoryImpl(
            dao = inventoryRemoteDao,
            api = apiService,
            settings = settingsManager,
            headersBuilder = protectedHeadersBuilder,
            apiCallExecutor = apiCallExecutor
        )
    }

    @Provides
    @Singleton
    fun provideInventoryRepository(
        inventoryDao: InventoryDao,
        inventoryItemDao: InventoryItemDao,
        productDao: ProductDao,
        locationDao: LocationDao,
        ruleDao: RuleDao,
        userDao: UserDao,
        settingsManager: SettingsManager
    ): InventoryRepository {
        return InventoryRepository(
            inventoryDao = inventoryDao,
            inventoryItemDao = inventoryItemDao,
            productDao = productDao,
            locationDao = locationDao,
            ruleDao = ruleDao,
            userDao = userDao,
            settings = settingsManager
        )
    }

    @Provides
    @Singleton
    fun provideSyncLogRepository(
        syncLogDao: SyncLogDao
    ): SyncLogRepository {
        return SyncLogRepository(syncLogDao)
    }

    @Provides
    @Singleton
    fun provideNetworkUsageRepository(
        networkUsageDao: NetworkUsageDao
    ): NetworkUsageRepository {
        return NetworkUsageRepository(networkUsageDao)
    }
}