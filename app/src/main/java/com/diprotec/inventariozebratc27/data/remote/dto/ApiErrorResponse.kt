package com.diprotec.inventariozebratc27.data.remote.dto

import com.squareup.moshi.Json

data class ApiErrorResponse(
    @Json(name = "Estado")
    val estado: Int? = null,

    @Json(name = "Respuesta")
    val respuesta: String? = null,

    @Json(name = "CodigoError")
    val codigoError: String? = null,

    @Json(name = "CorrelationId")
    val correlationId: String? = null
)