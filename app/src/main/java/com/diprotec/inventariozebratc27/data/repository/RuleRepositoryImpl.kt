package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.local.dao.RuleDao
import com.diprotec.inventariozebratc27.data.local.entity.RuleEntity
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.remote.dto.ReglaDto
import kotlinx.coroutines.flow.Flow

class RuleRepositoryImpl(
    private val ruleDao: RuleDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder,
    private val apiCallExecutor: ApiCallExecutor
) : RuleRepository {

    override suspend fun fetchRemoteReglas(): List<ReglaDto> {
        val empresaRut = settings.empresaRut.value.trim()

        require(empresaRut.isNotBlank()) {
            "Empresa RUT no configurado"
        }

        val relativeUrl =
            "/api/website/v1/reglas/$empresaRut/GetReglas"

        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        val response = apiCallExecutor.execute {
            api.getReglas(
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

    override suspend fun replaceAllReglas(
        list: List<RuleEntity>
    ) {
        ruleDao.replaceAll(list)
    }

    override fun observeReglas(): Flow<List<RuleEntity>> =
        ruleDao.observeAll()
}