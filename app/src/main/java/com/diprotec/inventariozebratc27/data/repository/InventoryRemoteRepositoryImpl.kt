package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.local.dao.InventoryRemoteDao
import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteUserEntity
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.remote.dto.InventarioDto
import kotlinx.coroutines.flow.Flow

class InventoryRemoteRepositoryImpl(
    private val dao: InventoryRemoteDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder,
    private val apiCallExecutor: ApiCallExecutor
) : InventoryRemoteRepository {

    override suspend fun fetchRemoteInventarios(): List<InventarioDto> {
        val empresaRut = settings.empresaRut.value.trim()

        require(empresaRut.isNotBlank()) {
            "Empresa RUT no configurado"
        }

        val relativeUrl =
            "/api/website/v1/inventarios/$empresaRut/GetInventarios"

        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        val response = apiCallExecutor.execute {
            api.getInventarios(
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

    override suspend fun replaceAllInventarios(
        inventarios: List<InventoryRemoteEntity>,
        usuarios: List<InventoryRemoteUserEntity>
    ) {
        dao.replaceAll(
            inventarios = inventarios,
            usuarios = usuarios
        )
    }

    override fun observeInventarios(): Flow<List<InventoryRemoteEntity>> =
        dao.observeAll()

    override fun observeInventariosActivos(): Flow<List<InventoryRemoteEntity>> =
        dao.observeActivos()

    override fun observeInventariosAsignadosActivos(
        rutUsuario: String
    ): Flow<List<InventoryRemoteEntity>> =
        dao.observeAsignadosActivosPorUsuario(rutUsuario)
}