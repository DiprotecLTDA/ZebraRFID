package com.diprotec.inventariozebratc27.di

import com.diprotec.inventariozebratc27.BuildConfig
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.BaseUrlProvider
import com.diprotec.inventariozebratc27.core.network.DynamicBaseUrlInterceptor
import com.diprotec.inventariozebratc27.core.network.NetworkUsageCallFactory
import com.diprotec.inventariozebratc27.core.network.NetworkUsageInterceptor
import com.diprotec.inventariozebratc27.core.network.RawBodyLoggingInterceptor
import com.diprotec.inventariozebratc27.core.network.normalizeBaseUrl
import com.diprotec.inventariozebratc27.data.local.dao.NetworkUsageDao
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DEFAULT_BASE_URL = "https://localhost/"

    @Provides
    @Singleton
    fun provideBaseUrlProvider(
        settingsManager: SettingsManager
    ): BaseUrlProvider {
        return object : BaseUrlProvider {
            override fun getBaseUrl(): String {
                val configuredBaseUrl = settingsManager.baseUrl.value.trim()

                if (configuredBaseUrl.isBlank()) {
                    return DEFAULT_BASE_URL
                }

                return normalizeBaseUrl(configuredBaseUrl)
            }
        }
    }

    @Provides
    @Singleton
    fun provideDynamicBaseUrlInterceptor(
        baseUrlProvider: BaseUrlProvider
    ): DynamicBaseUrlInterceptor {
        return DynamicBaseUrlInterceptor(baseUrlProvider)
    }

    @Provides
    @Singleton
    fun provideNetworkUsageInterceptor(
        networkUsageDao: NetworkUsageDao
    ): NetworkUsageInterceptor {
        return NetworkUsageInterceptor(networkUsageDao)
    }

    @Provides
    @Singleton
    fun provideRawBodyLoggingInterceptor(): RawBodyLoggingInterceptor {
        return RawBodyLoggingInterceptor()
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
        networkUsageInterceptor: NetworkUsageInterceptor,
        rawBodyLoggingInterceptor: RawBodyLoggingInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(networkUsageInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(rawBodyLoggingInterceptor)
            builder.addInterceptor(httpLoggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideCallFactory(
        okHttpClient: OkHttpClient
    ): Call.Factory {
        return NetworkUsageCallFactory(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        callFactory: Call.Factory,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .callFactory(callFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(
        retrofit: Retrofit
    ): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
