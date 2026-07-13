package com.diprotec.inventariozebratc27.core.network

interface BaseApiResponse {
    val apiEstado: Int
    val apiRespuesta: String?
    val apiCodigoError: String?
    val apiCorrelationId: String?
}