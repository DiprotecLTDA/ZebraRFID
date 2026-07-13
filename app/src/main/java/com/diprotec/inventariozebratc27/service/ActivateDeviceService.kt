package com.diprotec.inventariozebratc27.service

import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ApiException
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.remote.dto.ActivateDispositivoRequest

class ActivateDeviceService(
    private val api: ApiService,
    private val settings: SettingsManager,
    private val apiCallExecutor: ApiCallExecutor
) {

    private fun authorizationHeader(): String {
        val token = settings.authToken.value.trim()

        require(token.isNotBlank()) {
            "Authorization no configurado"
        }

        return "Bearer $token"
    }

    suspend fun activate(
        empresaRut: String,
        serialNumber: String,
        activationCode: String,
        publicKey: String
    ): String {
        require(empresaRut.isNotBlank()) {
            "Empresa RUT no configurado"
        }

        require(serialNumber.isNotBlank()) {
            "Serial del dispositivo no disponible"
        }

        require(activationCode.isNotBlank()) {
            "Código de activación no ingresado"
        }

        require(publicKey.isNotBlank()) {
            "Clave pública no disponible"
        }

        val apiKey = settings.apiKey.value.trim()

        require(apiKey.isNotBlank()) {
            "API Key no configurada"
        }

        val response = apiCallExecutor.execute {
            api.activateDispositivo(
                empresaRUT = empresaRut.trim(),
                apiKey = apiKey,
                authorization = authorizationHeader(),
                body = ActivateDispositivoRequest(
                    SerialNumber = serialNumber.trim(),
                    ActivationCode = activationCode.trim(),
                    PublicKey = publicKey.trim()
                )
            )
        }

        /*
         * ApiCallExecutor ya validó:
         * - código HTTP;
         * - Estado 0 o 200;
         * - Respuesta del servidor.
         *
         * Si llegamos aquí, la activación fue exitosa.
         */
        val deviceIdFromData = extractDeviceId(response.Data)

        /*
         * Algunos servicios confirman la activación, pero no entregan
         * explícitamente el identificador. En ese caso usamos el serial
         * Zebra como identificador local.
         */
        val deviceId = deviceIdFromData.ifBlank {
            serialNumber.trim()
        }

        if (deviceId.isBlank()) {
            throw ApiException(
                message = "La activación fue realizada, pero no fue posible identificar el dispositivo."
            )
        }

        return deviceId
    }

    private fun extractDeviceId(
        data: Any?
    ): String {
        return when (data) {
            null -> ""

            is String -> {
                data.trim()
            }

            is Number -> {
                data.toLong().toString()
            }

            is Map<*, *> -> {
                extractDeviceIdFromMap(data)
            }

            is List<*> -> {
                data.firstNotNullOfOrNull { item ->
                    when (item) {
                        is String -> {
                            item.trim()
                                .takeIf { it.isNotBlank() }
                        }

                        is Number -> {
                            item.toLong().toString()
                        }

                        is Map<*, *> -> {
                            extractDeviceIdFromMap(item)
                                .takeIf { it.isNotBlank() }
                        }

                        else -> {
                            item
                                ?.toString()
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                        }
                    }
                }.orEmpty()
            }

            else -> {
                data.toString().trim()
            }
        }
    }

    private fun extractDeviceIdFromMap(
        data: Map<*, *>
    ): String {
        val possibleKeys = listOf(
            "DispositivoId",
            "dispositivoId",
            "DISPOSITIVOID",
            "DeviceId",
            "deviceId",
            "DEVICEID",
            "Id",
            "id",
            "ID",
            "IdDispositivo",
            "idDispositivo",
            "IDDISPOSITIVO",
            "Codigo",
            "codigo",
            "Code",
            "code"
        )

        return possibleKeys.firstNotNullOfOrNull { key ->
            data[key]
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }
}