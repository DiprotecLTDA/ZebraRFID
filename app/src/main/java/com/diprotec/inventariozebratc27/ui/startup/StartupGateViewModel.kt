package com.diprotec.inventariozebratc27.ui.startup

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.session.SessionManager
import com.diprotec.inventariozebratc27.data.local.dao.LocationDao
import com.diprotec.inventariozebratc27.data.local.dao.RuleDao
import com.diprotec.inventariozebratc27.data.local.dao.UnitMeasureDao
import com.diprotec.inventariozebratc27.data.local.dao.UserDao
import com.diprotec.inventariozebratc27.service.SyncService
import com.diprotec.inventariozebratc27.worker.CatalogSyncWorker
import com.diprotec.inventariozebratc27.worker.PendingInventorySyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StartupGateUiState(
    val loading: Boolean = true,
    val message: String = "Verificando aplicación...",
    val error: String? = null,

    val waitingForUpdate: Boolean = false,
    val updateMandatory: Boolean = false,
    val updateFileName: String = "",
    val updateUrl: String = "",

    val goLogin: Boolean = false,
    val goMainMenu: Boolean = false,
    val goSettings: Boolean = false,
    val canContinueOffline: Boolean = false
)

@HiltViewModel
class StartupGateViewModel @Inject constructor(
    private val settings: SettingsManager,
    private val session: SessionManager,
    private val sync: SyncService,
    private val userDao: UserDao,
    private val locationDao: LocationDao,
    private val ruleDao: RuleDao,
    private val unitMeasureDao: UnitMeasureDao,
    @ApplicationContext context: Context
) : ViewModel() {

    private val appContext = context.applicationContext

    private val _state = mutableStateOf(StartupGateUiState())
    val state: State<StartupGateUiState> = _state

    private var started = false
    private var continuingAfterOptional = false

    fun start() {
        if (started) return
        started = true

        val hasBase = settings.baseUrl.value.trim().isNotBlank()
        val hasEmpresa = settings.empresaRut.value.trim().isNotBlank()
        val hasApiKey = settings.apiKey.value.trim().isNotBlank()
        val hasAuth = settings.authToken.value.trim().isNotBlank()

        if (!hasBase || !hasEmpresa || !hasApiKey || !hasAuth) {
            _state.value = _state.value.copy(
                loading = false,
                message = "Faltan parámetros de configuración.",
                error = "Faltan parámetros (Base URL / Empresa / API Key / Authorization). Configure la app.",
                waitingForUpdate = false,
                updateMandatory = false,
                updateFileName = "",
                updateUrl = "",
                goLogin = false,
                goMainMenu = false,
                goSettings = true,
                canContinueOffline = false
            )
            return
        }

        if (!settings.deviceActivated.value) {
            _state.value = _state.value.copy(
                loading = false,
                message = "Dispositivo no activado.",
                error = "El dispositivo no está activado. Debe activarlo en Configuración.",
                waitingForUpdate = false,
                updateMandatory = false,
                updateFileName = "",
                updateUrl = "",
                goLogin = false,
                goMainMenu = false,
                goSettings = true,
                canContinueOffline = false
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                message = "Preparando sesión del dispositivo...",
                error = null,
                waitingForUpdate = false,
                updateMandatory = false,
                updateFileName = "",
                updateUrl = "",
                goLogin = false,
                goMainMenu = false,
                goSettings = false,
                canContinueOffline = false
            )

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    sync.warmUpDeviceSession()
                }

                _state.value = _state.value.copy(
                    message = "Consultando actualización..."
                )

                withContext(Dispatchers.IO) {
                    sync.checkStartupUpdateAndSavePending()
                }
            }

            result.onFailure { t ->
                Log.e(TAG, "Startup gate failed", t)

                if (canWorkOffline()) {
                    showOfflineOption(
                        message = "Sin conexión a internet.",
                        error = "No se pudo verificar la aplicación. Puede continuar trabajando offline con los datos almacenados."
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = "No se pudo verificar la aplicación.",
                        error = t.message ?: "No se pudo verificar la aplicación.",
                        waitingForUpdate = false,
                        updateMandatory = false,
                        updateFileName = "",
                        updateUrl = "",
                        goLogin = false,
                        goMainMenu = false,
                        goSettings = false,
                        canContinueOffline = false
                    )
                }
            }

            result.onSuccess { update ->
                val hasUpdate =
                    update.hasNewVersion &&
                            !update.apkUrl.isNullOrBlank() &&
                            update.apkFileName.isNotBlank()

                if (hasUpdate) {
                    Log.d(
                        TAG,
                        "Update detected. mandatory=${update.mandatory}, " +
                                "apk=${update.apkFileName}, url=${update.apkUrl}"
                    )

                    _state.value = _state.value.copy(
                        loading = false,
                        message = if (update.mandatory) {
                            "Actualización obligatoria disponible."
                        } else {
                            "Nueva versión disponible."
                        },
                        error = null,
                        waitingForUpdate = true,
                        updateMandatory = update.mandatory,
                        updateFileName = update.apkFileName,
                        updateUrl = update.apkUrl.orEmpty(),
                        goLogin = false,
                        goMainMenu = false,
                        goSettings = false,
                        canContinueOffline = false
                    )
                } else {
                    continueToLogin()
                }
            }
        }
    }

    fun retry() {
        started = false
        continuingAfterOptional = false
        start()
    }

    fun continueOffline() {
        viewModelScope.launch {
            goLoginOffline()
        }
    }

    fun continueAfterOptionalUpdateDismissed() {
        if (continuingAfterOptional) return
        continuingAfterOptional = true

        viewModelScope.launch {
            continueToLogin()
            continuingAfterOptional = false
        }
    }

    fun onUpdateStarted() {
        _state.value = _state.value.copy(
            loading = false,
            waitingForUpdate = true,
            goLogin = false,
            goMainMenu = false,
            goSettings = false,
            canContinueOffline = false,
            message = "Descargando actualización..."
        )
    }

    private suspend fun continueToLogin() {
        _state.value = _state.value.copy(
            loading = true,
            message = "Sincronizando usuarios...",
            error = null,
            waitingForUpdate = false,
            updateMandatory = false,
            updateFileName = "",
            updateUrl = "",
            goLogin = false,
            goMainMenu = false,
            goSettings = false,
            canContinueOffline = false
        )

        val result = withContext(Dispatchers.IO) {
            runCatching {
                sync.syncUsers()
            }
        }

        result.onFailure { t ->
            Log.e(TAG, "syncUsers failed", t)

            if (canWorkOffline()) {
                showOfflineOption(
                    message = "Sin conexión a internet.",
                    error = "No se pudo sincronizar usuarios. Puede continuar trabajando offline con los datos almacenados."
                )
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    message = "No se pudo sincronizar usuarios.",
                    error = t.message ?: "No se pudo sincronizar usuarios.",
                    waitingForUpdate = false,
                    updateMandatory = false,
                    updateFileName = "",
                    updateUrl = "",
                    goLogin = false,
                    goMainMenu = false,
                    goSettings = false,
                    canContinueOffline = false
                )
            }
        }

        result.onSuccess {
            CatalogSyncWorker.schedulePeriodic(appContext)
            PendingInventorySyncWorker.schedulePeriodic(appContext)

            goToSessionDestination()
        }
    }

    private suspend fun canWorkOffline(): Boolean = withContext(Dispatchers.IO) {
        userDao.countUsers() > 0 &&
                locationDao.count() > 0 &&
                ruleDao.count() > 0 &&
                unitMeasureDao.count() > 0
    }

    private fun showOfflineOption(
        message: String,
        error: String
    ) {
        _state.value = _state.value.copy(
            loading = false,
            message = message,
            error = error,
            waitingForUpdate = false,
            updateMandatory = false,
            updateFileName = "",
            updateUrl = "",
            goLogin = false,
            goMainMenu = false,
            goSettings = false,
            canContinueOffline = true
        )
    }

    private suspend fun goLoginOffline() {
        CatalogSyncWorker.schedulePeriodic(appContext)
        PendingInventorySyncWorker.schedulePeriodic(appContext)

        goToSessionDestination(
            offline = true
        )
    }

    private suspend fun goToSessionDestination(
        offline: Boolean = false
    ) {
        val hasValidSession = session.restoreSession()

        _state.value = _state.value.copy(
            loading = false,
            message = if (offline) {
                "Modo offline."
            } else {
                "Aplicación lista."
            },
            error = null,
            waitingForUpdate = false,
            updateMandatory = false,
            updateFileName = "",
            updateUrl = "",
            goLogin = !hasValidSession,
            goMainMenu = hasValidSession,
            goSettings = false,
            canContinueOffline = false
        )
    }

    companion object {
        private const val TAG = "STARTUP_GATE"
    }
}