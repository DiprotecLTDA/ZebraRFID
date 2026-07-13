package com.diprotec.inventariozebratc27.data.remote.dto

import com.diprotec.inventariozebratc27.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class ProductosResponse(
    @Json(name = "Estado")
    val estado: Int,

    @Json(name = "Respuesta")
    val respuesta: String?,

    @Json(name = "Data")
    val data: List<ProductoDto>?,

    @Json(name = "CodigoError")
    val codigoError: String?,

    @Json(name = "CorrelationId")
    val correlationId: String?
) : BaseApiResponse {

    override val apiEstado: Int
        get() = estado

    override val apiRespuesta: String?
        get() = respuesta

    override val apiCodigoError: String?
        get() = codigoError

    override val apiCorrelationId: String?
        get() = correlationId
}

data class ProductoDto(
    @Json(name = "Codigo")
    val codigo: String,

    @Json(name = "CodigoSecundario")
    val codigoSecundario: String?,

    @Json(name = "Descripcion")
    val descripcion: String?,

    @Json(name = "estado")
    val estado: String?,

    @Json(name = "RutEmpresa")
    val rutEmpresa: String?
)