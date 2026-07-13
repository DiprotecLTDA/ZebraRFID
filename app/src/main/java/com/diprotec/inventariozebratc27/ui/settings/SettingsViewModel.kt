package com.diprotec.inventariozebratc27.ui.settings

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.key.DeviceKeyStoreManager
import com.diprotec.inventariozebratc27.core.key.DevicePublicKeyExporter
import com.diprotec.inventariozebratc27.core.key.KeyFileReader
import com.diprotec.inventariozebratc27.core.validator.RutValidator
import com.diprotec.inventariozebratc27.serial.ZebraSerialProvider
import com.diprotec.inventariozebratc27.service.ActivateDeviceService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val baseUrl: String = "",
    val empresaRut: String = "",
    val activationCode: String = "",
    val authToken: String = "",
    val apiKey: String = "",
    val credentialsLoaded: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsManager,
    private val activateDeviceService: ActivateDeviceService,
    private val keyStoreManager: DeviceKeyStoreManager,
    private val publicKeyExporter: DevicePublicKeyExporter,
    @ApplicationContext context: Context
) : ViewModel() {

    private val appContext = context.applicationContext

    private val _state = mutableStateOf(
        SettingsUiState(
            baseUrl = settings.baseUrl.value,
            empresaRut = settings.empresaRut.value,
            activationCode = settings.activationCode.value,
            authToken = settings.authToken.value,
            apiKey = settings.apiKey.value,
            credentialsLoaded = settings.authToken.value.isNotBlank() &&
                    settings.apiKey.value.isNotBlank()
        )
    )

    val state: State<SettingsUiState> = _state

    fun clearError() {
        _state.value = _state.value.copy(
            error = null
        )
    }

    fun clearInfo() {
        _state.value = _state.value.copy(
            info = null
        )
    }

    fun hasCredentials(): Boolean {
        val currentState = _state.value

        return currentState.credentialsLoaded &&
                currentState.authToken.isNotBlank() &&
                currentState.apiKey.isNotBlank()
    }

    fun isDeviceActivated(): Boolean {
        return settings.deviceActivated.value
    }

    fun onBaseUrlChange(value: String) {
        _state.value = _state.value.copy(
            baseUrl = value,
            error = null,
            info = null
        )
    }

    fun onEmpresaChange(value: String) {
        _state.value = _state.value.copy(
            empresaRut = value,
            error = null,
            info = null
        )
    }

    fun onActivationCodeChange(value: String) {
        _state.value = _state.value.copy(
            activationCode = value,
            error = null,
            info = null
        )
    }

    fun onSave(onDone: () -> Unit) {
        val currentState = _state.value

        val base = currentState.baseUrl.trim()
        val empresaRut = currentState.empresaRut.trim()
        val activationCode = currentState.activationCode.trim()

        val authToken = currentState.authToken.ifBlank {
            settings.authToken.value
        }.trim()

        val apiKey = currentState.apiKey.ifBlank {
            settings.apiKey.value
        }.trim()

        if (base.isBlank()) {
            _state.value = currentState.copy(
                error = "Debes ingresar la Base URL.",
                info = null,
                saving = false
            )
            return
        }

        if (!(base.startsWith("http://") || base.startsWith("https://"))) {
            _state.value = currentState.copy(
                error = "La Base URL debe comenzar con http:// o https://",
                info = null,
                saving = false
            )
            return
        }

        if (!base.endsWith("/")) {
            _state.value = currentState.copy(
                error = "La Base URL debe terminar con /",
                info = null,
                saving = false
            )
            return
        }

        if (empresaRut.isBlank()) {
            _state.value = currentState.copy(
                error = "Debes ingresar el RUT de la empresa.",
                info = null,
                saving = false
            )
            return
        }

        val normalizedRut = RutValidator.validateAndNormalize(empresaRut)

        if (normalizedRut == null) {
            _state.value = currentState.copy(
                error = "RUT de empresa inválido",
                info = null,
                saving = false
            )
            return
        }

        if (authToken.isBlank() || apiKey.isBlank()) {
            _state.value = currentState.copy(
                error = "Debes cargar el archivo de credenciales.",
                info = null,
                saving = false,
                credentialsLoaded = false
            )
            return
        }

        if (!settings.deviceActivated.value && activationCode.isBlank()) {
            _state.value = currentState.copy(
                error = "Debes ingresar el Activation Code.",
                info = null,
                saving = false
            )
            return
        }

        _state.value = currentState.copy(
            empresaRut = normalizedRut,
            authToken = authToken,
            apiKey = apiKey,
            credentialsLoaded = true,
            saving = true,
            error = null,
            info = null
        )

        viewModelScope.launch {
            runCatching {
                if (!settings.deviceActivated.value) {
                    /*
                     * Importante:
                     * Antes de activar, guardamos Base URL + RUT + authToken + apiKey.
                     * ActivateDeviceService lee Authorization y X-API-KEY desde SettingsManager.
                     */
                    settings.save(
                        baseUrl = base,
                        empresaRut = normalizedRut,
                        authToken = authToken,
                        apiKey = apiKey,
                        deviceSession = settings.deviceSession.value,
                        deviceId = settings.deviceId.value,
                        activationCode = activationCode,
                        deviceKeyAlias = settings.deviceKeyAlias.value,
                        deviceActivated = false
                    )

                    val serial = withContext(Dispatchers.IO) {
                        ZebraSerialProvider.getSerial(appContext)
                            .orEmpty()
                            .trim()
                    }

                    if (serial.isBlank()) {
                        throw IllegalStateException(
                            "No se pudo obtener serial Zebra. Revise permisos, CallerSignature y perfil EMDK."
                        )
                    }

                    keyStoreManager.ensureKeyPair()

                    val publicKeyPem = publicKeyExporter.exportPublicKeyPem()
                    val keyAlias = keyStoreManager.getAlias()

                    val deviceId = activateDeviceService.activate(
                        empresaRut = normalizedRut,
                        serialNumber = serial,
                        activationCode = activationCode,
                        publicKey = publicKeyPem
                    )

                    settings.save(
                        baseUrl = base,
                        empresaRut = normalizedRut,
                        authToken = authToken,
                        apiKey = apiKey,
                        deviceSession = settings.deviceSession.value,
                        deviceId = deviceId,
                        activationCode = activationCode,
                        deviceKeyAlias = keyAlias,
                        deviceActivated = true
                    )
                } else {
                    settings.save(
                        baseUrl = base,
                        empresaRut = normalizedRut,
                        authToken = authToken,
                        apiKey = apiKey,
                        deviceSession = settings.deviceSession.value,
                        deviceId = settings.deviceId.value,
                        activationCode = settings.activationCode.value,
                        deviceKeyAlias = settings.deviceKeyAlias.value,
                        deviceActivated = settings.deviceActivated.value
                    )
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    saving = false,
                    error = error.message ?: "Error guardando configuración",
                    info = null
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    saving = false,
                    error = null,
                    info = "Configuración guardada correctamente",
                    credentialsLoaded = true,
                    empresaRut = settings.empresaRut.value,
                    activationCode = settings.activationCode.value,
                    authToken = settings.authToken.value,
                    apiKey = settings.apiKey.value
                )

                onDone()
            }
        }
    }

    suspend fun importKeyFromUri(uriString: String): Boolean {
        val raw = runCatching {
            KeyFileReader.readFromUri(appContext, uriString)
        }.getOrNull()

        if (raw.isNullOrBlank()) {
            _state.value = _state.value.copy(
                error = "No pude leer el archivo seleccionado.",
                info = null,
                credentialsLoaded = false
            )
            return false
        }

        val (tokenFromFile, apiKeyFromFile) =
            KeyFileReader.extractAuthTokenAndApiKey(raw)

        if (tokenFromFile.isNullOrBlank() || apiKeyFromFile.isNullOrBlank()) {
            _state.value = _state.value.copy(
                error = "El archivo no contiene authToken/apiKey en el formato esperado.",
                info = null,
                credentialsLoaded = false
            )
            return false
        }

        if (!KeyFileReader.isInventarioToken(tokenFromFile)) {
            val appClaim = KeyFileReader.extractJwtAppClaim(tokenFromFile)

            _state.value = _state.value.copy(
                error = "El token no corresponde a Inventario. app=${appClaim ?: "desconocida"}",
                info = null,
                credentialsLoaded = false
            )
            return false
        }

        val currentState = _state.value

        val ok = runCatching {
            settings.save(
                baseUrl = currentState.baseUrl.ifBlank {
                    settings.baseUrl.value
                },
                empresaRut = currentState.empresaRut.ifBlank {
                    settings.empresaRut.value
                },
                authToken = tokenFromFile,
                apiKey = apiKeyFromFile,
                deviceSession = settings.deviceSession.value,
                deviceId = settings.deviceId.value,
                activationCode = currentState.activationCode.ifBlank {
                    settings.activationCode.value
                },
                deviceKeyAlias = settings.deviceKeyAlias.value,
                deviceActivated = settings.deviceActivated.value
            )
        }.isSuccess

        _state.value = if (ok) {
            _state.value.copy(
                authToken = tokenFromFile,
                apiKey = apiKeyFromFile,
                credentialsLoaded = true,
                error = null,
                info = "Credenciales cargadas correctamente"
            )
        } else {
            _state.value.copy(
                authToken = "",
                apiKey = "",
                credentialsLoaded = false,
                error = "Error guardando credenciales.",
                info = null
            )
        }

        return ok
    }
}