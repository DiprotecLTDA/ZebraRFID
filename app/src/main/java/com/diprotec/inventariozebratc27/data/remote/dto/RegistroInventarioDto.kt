package com.diprotec.inventariozebratc27.data.remote.dto

import com.diprotec.inventariozebratc27.core.network.BaseApiResponse
import com.squareup.moshi.Json

data class RegistroInventarioRequest(
    @Json(name = "InventarioId")
    val InventarioId: String,

    @Json(name = "Capturas")
    val Capturas: List<RegistroInventarioCapturaRequest>
)

data class RegistroInventarioCapturaRequest(
    @Json(name = "UbicacionId")
    val UbicacionId: String,

    @Json(name = "DispositivoId")
    val DispositivoId: String,

    @Json(name = "ProductoCodigo")
    val ProductoCodigo: String,

    @Json(name = "Cantidad")
    val Cantidad: String,

    @Json(name = "UnidadMedidaId")
    val UnidadMedidaId: String,

    @Json(name = "Fecha")
    val Fecha: String,

    @Json(name = "Hora")
    val Hora: String,

    @Json(name = "RutUsuario")
    val RutUsuario: String
)

data class SendRegistroInventarioResponse(
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