package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.local.dao.UnitMeasureDao
import com.diprotec.inventariozebratc27.data.local.entity.UnitMeasureEntity
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.remote.dto.UnidadMedidaDto
import kotlinx.coroutines.flow.Flow

class UnitMeasureRepositoryImpl(
    private val unitMeasureDao: UnitMeasureDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder,
    private val apiCallExecutor: ApiCallExecutor
) : UnitMeasureRepository {

    override suspend fun fetchRemoteUnidadMedidas(): List<UnidadMedidaDto> {
        val empresaRut = settings.empresaRut.value.trim()

        require(empresaRut.isNotBlank()) {
            "Empresa RUT no configurado"
        }

        val relativeUrl =
            "/api/website/v1/unidadmedidas/$empresaRut/GetUnidadMedidas"

        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        val response = apiCallExecutor.execute {
            api.getUnidadMedidas(
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

    override suspend fun replaceAllUnidadMedidas(
        list: List<UnitMeasureEntity>
    ) {
        unitMeasureDao.replaceAll(list)
    }

    override fun observeUnidadMedidas(): Flow<List<UnitMeasureEntity>> =
        unitMeasureDao.observeAll()
}