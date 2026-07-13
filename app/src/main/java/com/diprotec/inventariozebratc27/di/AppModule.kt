package com.diprotec.inventariozebratc27.di

import android.content.Context
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.AppConnectionMonitor
import com.diprotec.inventariozebratc27.core.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context
    ): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideSessionManager(
        settingsManager: SettingsManager
    ): SessionManager {
        return SessionManager(settingsManager)
    }

    @Provides
    @Singleton
    fun provideAppConnectionMonitor(
        @ApplicationContext context: Context
    ): AppConnectionMonitor {
        return AppConnectionMonitor(context)
    }
}