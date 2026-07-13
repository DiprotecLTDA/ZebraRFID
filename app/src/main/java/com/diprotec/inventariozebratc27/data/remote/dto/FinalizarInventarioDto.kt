package com.diprotec.inventariozebratc27.data.remote.dto

import com.diprotec.inventariozebratc27.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class FinalizarInventarioRequest(
    @Json(name = "InventarioId")
    val InventarioId: Long,

    @Json(name = "UsuarioRUT")
    val UsuarioRUT: String
)

data class FinalizarInventarioResponse(
    @Json(name = "Estado")
    val Estado: Int,

    @Json(name = "Respuesta")
    val Respuesta: String?,

    @Json(name = "Data")
    val Data: Map<String, Any?>?,

    @Json(name = "CodigoError")
    val CodigoError: String?,

    @Json(name = "CorrelationId")
    val CorrelationId: String?
) : BaseApiResponse {

    override val apiEstado: Int
        get() = Estado

    override val apiRespuesta: String?
        get() = Respuesta

    override val apiCodigoError: String?
        get() = CodigoError

    override val apiCorrelationId: String?
        get() = CorrelationId
}