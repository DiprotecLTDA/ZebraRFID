package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.data.local.entity.UnitMeasureEntity
import com.diprotec.inventariozebratc27.data.remote.dto.UnidadMedidaDto
import kotlinx.coroutines.flow.Flow

interface UnitMeasureRepository {

    suspend fun fetchRemoteUnidadMedidas(): List<UnidadMedidaDto>

    suspend fun replaceAllUnidadMedidas(list: List<UnitMeasureEntity>)

    fun observeUnidadMedidas(): Flow<List<UnitMeasureEntity>>
}