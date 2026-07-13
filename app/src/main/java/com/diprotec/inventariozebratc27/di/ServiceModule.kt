package com.diprotec.inventariozebratc27.di

import android.content.Context
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.local.dao.UserDao
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.repository.InventoryRemoteRepository
import com.diprotec.inventariozebratc27.data.repository.InventoryRepository
import com.diprotec.inventariozebratc27.data.repository.LocationRepository
import com.diprotec.inventariozebratc27.data.repository.ProductRepository
import com.diprotec.inventariozebratc27.data.repository.RuleRepository
import com.diprotec.inventariozebratc27.data.repository.SyncLogRepository
import com.diprotec.inventariozebratc27.data.repository.UnitMeasureRepository
import com.diprotec.inventariozebratc27.data.repository.UserRepository
import com.diprotec.inventariozebratc27.service.AuthService
import com.diprotec.inventariozebratc27.service.SyncService
import com.diprotec.inventariozebratc27.service.VersionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideAuthService(
        userDao: UserDao
    ): AuthService {
        return AuthService(
            userDao = userDao
        )
    }

    @Provides
    @Singleton
    fun provideVersionService(
        apiService: ApiService,
        settingsManager: SettingsManager,
        protectedHeadersBuilder: ProtectedHeadersBuilder,
        apiCallExecutor: ApiCallExecutor,
        @ApplicationContext context: Context
    ): VersionService {
        return VersionService(
            api = apiService,
            settings = settingsManager,
            headersBuilder = protectedHeadersBuilder,
            apiCallExecutor = apiCallExecutor,
            context = context
        )
    }

    @Provides
    @Singleton
    fun provideSyncService(
        userRepository: UserRepository,
        ruleRepository: RuleRepository,
        locationRepository: LocationRepository,
        productRepository: ProductRepository,
        unitMeasureRepository: UnitMeasureRepository,
        inventoryRemoteRepository: InventoryRemoteRepository,
        versionService: VersionService,
        apiService: ApiService,
        settingsManager: SettingsManager,
        inventoryRepository: InventoryRepository,
        syncLogRepository: SyncLogRepository,
        protectedHeadersBuilder: ProtectedHeadersBuilder,
        apiCallExecutor: ApiCallExecutor,
        @ApplicationContext context: Context
    ): SyncService {
        return SyncService(
            userRepository = userRepository,
            ruleRepository = ruleRepository,
            locationRepository = locationRepository,
            productRepository = productRepository,
            unitMeasureRepository = unitMeasureRepository,
            inventoryRemoteRepository = inventoryRemoteRepository,
            versionService = versionService,
            api = apiService,
            settings = settingsManager,
            inventoryRepository = inventoryRepository,
            syncLogRepository = syncLogRepository,
            headersBuilder = protectedHeadersBuilder,
            apiCallExecutor = apiCallExecutor,
            context = context
        )
    }
}