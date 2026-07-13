package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.local.dao.LocationDao
import com.diprotec.inventariozebratc27.data.local.entity.LocationEntity
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.remote.dto.UbicacionDto
import kotlinx.coroutines.flow.Flow

class LocationRepositoryImpl(
    private val locationDao: LocationDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder,
    private val apiCallExecutor: ApiCallExecutor
) : LocationRepository {

    override suspend fun fetchRemoteUbicaciones(): List<UbicacionDto> {
        val empresaRut = settings.empresaRut.value.trim()

        require(empresaRut.isNotBlank()) {
            "Empresa RUT no configurado"
        }

        val relativeUrl =
            "/api/website/v1/ubicaciones/$empresaRut/GetUbicaciones"

        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        val response = apiCallExecutor.execute {
            api.getUbicaciones(
                empresaRUT = empresaRut,
                apiKey = headers.apiKey,
                authorization = headers.authorization,
                deviceSession = headers.deviceSession,
                deviceSignature = headers.deviceSignature,
                deviceTimestamp = headers.deviceTimestamp
            )
        }

        return response.data.orEmpty()
    }

    override suspend fun replaceAllUbicaciones(
        list: List<LocationEntity>
    ) {
        locationDao.replaceAll(list)
    }

    override fun observeUbicaciones(): Flow<List<LocationEntity>> =
        locationDao.observeAll()
}