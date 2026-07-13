package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.data.local.entity.RuleEntity
import com.diprotec.inventariozebratc27.data.remote.dto.ReglaDto
import kotlinx.coroutines.flow.Flow

interface RuleRepository {

    suspend fun fetchRemoteReglas(): List<ReglaDto>

    suspend fun replaceAllReglas(list: List<RuleEntity>)

    fun observeReglas(): Flow<List<RuleEntity>>
}