package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteUserEntity
import com.diprotec.inventariozebratc27.data.remote.dto.InventarioDto
import kotlinx.coroutines.flow.Flow

interface InventoryRemoteRepository {

    suspend fun fetchRemoteInventarios(): List<InventarioDto>

    suspend fun replaceAllInventarios(
        inventarios: List<InventoryRemoteEntity>,
        usuarios: List<InventoryRemoteUserEntity>
    )

    fun observeInventarios(): Flow<List<InventoryRemoteEntity>>

    fun observeInventariosActivos(): Flow<List<InventoryRemoteEntity>>

    fun observeInventariosAsignadosActivos(
        rutUsuario: String
    ): Flow<List<InventoryRemoteEntity>>
}