package com.diprotec.inventariozebratc27.di

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.crypto.DeviceCanonicalStringBuilder
import com.diprotec.inventariozebratc27.core.crypto.DeviceSigner
import com.diprotec.inventariozebratc27.core.crypto.DeviceTimestampProvider
import com.diprotec.inventariozebratc27.core.key.DeviceKeyStoreManager
import com.diprotec.inventariozebratc27.core.key.DevicePublicKeyExporter
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.service.ActivateDeviceService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideDeviceKeyStoreManager(): DeviceKeyStoreManager {
        return DeviceKeyStoreManager()
    }

    @Provides
    @Singleton
    fun provideDeviceSigner(
        deviceKeyStoreManager: DeviceKeyStoreManager
    ): DeviceSigner {
        return DeviceSigner(
            deviceKeyStoreManager
        )
    }

    @Provides
    @Singleton
    fun provideDeviceTimestampProvider(): DeviceTimestampProvider {
        return DeviceTimestampProvider()
    }

    @Provides
    @Singleton
    fun provideDeviceCanonicalStringBuilder(): DeviceCanonicalStringBuilder {
        return DeviceCanonicalStringBuilder()
    }

    @Provides
    @Singleton
    fun provideDevicePublicKeyExporter(
        deviceKeyStoreManager: DeviceKeyStoreManager
    ): DevicePublicKeyExporter {
        return DevicePublicKeyExporter(
            deviceKeyStoreManager
        )
    }

    @Provides
    @Singleton
    fun provideProtectedHeadersBuilder(
        settingsManager: SettingsManager,
        timestampProvider: DeviceTimestampProvider,
        canonicalStringBuilder: DeviceCanonicalStringBuilder,
        deviceSigner: DeviceSigner
    ): ProtectedHeadersBuilder {
        return ProtectedHeadersBuilder(
            settings = settingsManager,
            timestampProvider = timestampProvider,
            canonicalStringBuilder = canonicalStringBuilder,
            deviceSigner = deviceSigner
        )
    }

    @Provides
    @Singleton
    fun provideActivateDeviceService(
        apiService: ApiService,
        settingsManager: SettingsManager,
        apiCallExecutor: ApiCallExecutor
    ): ActivateDeviceService {
        return ActivateDeviceService(
            api = apiService,
            settings = settingsManager,
            apiCallExecutor = apiCallExecutor
        )
    }
}