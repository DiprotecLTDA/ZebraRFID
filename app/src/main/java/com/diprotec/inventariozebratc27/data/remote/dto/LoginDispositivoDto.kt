package com.diprotec.inventariozebratc27.data.remote.dto

import com.diprotec.inventariozebratc27.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class LoginDispositivoResponse(
    @Json(name = "Estado")
    val Estado: Int,

    @Json(name = "Respuesta")
    val Respuesta: String?,

    @Json(name = "Data")
    val Data: String?,

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