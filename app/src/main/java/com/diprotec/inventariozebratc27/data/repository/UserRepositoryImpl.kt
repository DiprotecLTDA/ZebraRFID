package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.local.dao.UserDao
import com.diprotec.inventariozebratc27.data.local.entity.UserEntity
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder,
    private val apiCallExecutor: ApiCallExecutor
) : UserRepository {

    override suspend fun fetchRemoteUsers(): List<UserDto> {
        val empresaRut = settings.empresaRut.value.trim()

        require(empresaRut.isNotBlank()) {
            "Empresa RUT no configurado"
        }

        val relativeUrl =
            "/api/website/v1/usuarios/$empresaRut/GetUsuarios"

        val headers = headersBuilder.build(
            method = "GET",
            relativeUrl = relativeUrl
        )

        val response = apiCallExecutor.execute {
            api.getUsers(
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

    override suspend fun replaceAllUsers(
        list: List<UserEntity>
    ) {
        userDao.replaceAll(list)
    }

    override fun observeUsers(): Flow<List<UserEntity>> =
        userDao.observeAll()
}
