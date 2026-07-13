package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.data.local.entity.ProductEntity
import com.diprotec.inventariozebratc27.data.remote.dto.ProductoDto
import kotlinx.coroutines.flow.Flow

interface ProductRepository {

    suspend fun fetchRemoteProductos(): List<ProductoDto>

    suspend fun replaceAllProductos(list: List<ProductEntity>)

    fun observeProductos(): Flow<List<ProductEntity>>

    suspend fun searchProductsForRfidLocation(
        query: String
    ): List<ProductEntity>
}