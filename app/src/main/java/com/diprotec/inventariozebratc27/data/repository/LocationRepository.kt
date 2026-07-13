package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.data.local.entity.LocationEntity
import com.diprotec.inventariozebratc27.data.remote.dto.UbicacionDto
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    suspend fun fetchRemoteUbicaciones(): List<UbicacionDto>

    suspend fun replaceAllUbicaciones(list: List<LocationEntity>)

    fun observeUbicaciones(): Flow<List<LocationEntity>>
}