package com.diprotec.inventariozebratc27.core.network

import com.diprotec.inventariozebratc27.data.remote.dto.ApiErrorResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCallExecutor @Inject constructor(
    moshi: Moshi
) {

    private val errorAdapter: JsonAdapter<ApiErrorResponse> =
        moshi.adapter(ApiErrorResponse::class.java)

    suspend fun <T : BaseApiResponse> execute(
        request: suspend () -> Response<T>
    ): T {
        try {
            val httpResponse = request()

            /*
             * Caso HTTP 400, 401, 403, 500, etc.
             */
            if (!httpResponse.isSuccessful) {
                throw parseHttpError(httpResponse)
            }

            /*
             * Caso HTTP exitoso, pero sin body.
             */
            val body = httpResponse.body()
                ?: throw ApiException(
                    message = "El servidor respondió sin contenido.",
                    httpCode = httpResponse.code()
                )

            /*
             * Valida el Estado informado dentro del JSON.
             */
            validateApiState(body)

            return body
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            /*
             * Conserva el mensaje Respuesta.
             */
            throw exception
        } catch (exception: UnknownHostException) {
            throw ApiException(
                message = "No fue posible conectarse al servidor. Verifique su conexión a Internet.",
                cause = exception
            )
        } catch (exception: SocketTimeoutException) {
            throw ApiException(
                message = "El servidor tardó demasiado en responder.",
                cause = exception
            )
        } catch (exception: IOException) {
            throw ApiException(
                message = "Ocurrió un problema de comunicación con el servidor.",
                cause = exception
            )
        } catch (exception: Exception) {
            /*
             * No se muestra exception.message porque puede contener
             * información técnica de Retrofit, Moshi o Java.
             */
            throw ApiException(
                message = "No fue posible completar la operación.",
                cause = exception
            )
        }
    }

    private fun validateApiState(
        response: BaseApiResponse
    ) {
        val estado = response.apiEstado

        /*
         * Según lo definido:
         * 0 y 200 representan éxito.
         */
        val successful = estado == 0 || estado == 200

        if (successful) {
            return
        }

        throw ApiException(
            message = response.apiRespuesta
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: defaultMessage(estado),
            estado = estado,
            codigoError = response.apiCodigoError,
            correlationId = response.apiCorrelationId
        )
    }

    private fun parseHttpError(
        response: Response<*>
    ): ApiException {
        val errorJson = runCatching {
            response.errorBody()?.string()
        }.getOrNull()

        val parsedError = errorJson
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { json ->
                runCatching {
                    errorAdapter.fromJson(json)
                }.getOrNull()
            }

        val code = parsedError?.estado ?: response.code()

        return ApiException(
            message = parsedError
                ?.respuesta
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: defaultMessage(code),
            httpCode = response.code(),
            estado = parsedError?.estado,
            codigoError = parsedError?.codigoError,
            correlationId = parsedError?.correlationId
        )
    }

    private fun defaultMessage(
        code: Int?
    ): String {
        return when (code) {
            400 -> "Los datos enviados son inválidos o están incompletos."
            401 -> "No fue posible autorizar la operación."
            403 -> "El dispositivo no tiene autorización para realizar esta operación."
            500 -> "El servidor presentó un error interno."
            in 501..599 -> "El servidor no pudo completar la operación."
            else -> "No fue posible completar la operación."
        }
    }
}