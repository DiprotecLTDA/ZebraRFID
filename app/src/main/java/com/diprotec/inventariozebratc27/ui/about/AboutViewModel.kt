package com.diprotec.inventariozebratc27.ui.about

import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.AppConnectionMonitor
import com.diprotec.inventariozebratc27.data.remote.dto.VersionCheckDataDto
import com.diprotec.inventariozebratc27.service.VersionService
import com.diprotec.inventariozebratc27.ui.connection.AppConnectionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class AboutUiState(
    val loading: Boolean = false,
    val versionCheck: VersionCheckDataDto? = null,
    val error: String? = null,
    val info: String? = null,
    val hasNewVersion: Boolean = false,
    val isMandatoryUpdate: Boolean = false,
    val canOperate: Boolean = true,
    val connectionMode: AppConnectionMode = AppConnectionMode.CHECKING,
    val canCheckUpdates: Boolean = false,

    val showUpdateDialog: Boolean = false,
    val updateMandatory: Boolean = false,
    val updateFileName: String = "",
    val updateUrl: String = ""
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val versionService: VersionService,
    private val settingsManager: SettingsManager,
    private val monitor: AppConnectionMonitor
) : ViewModel() {

    var state = mutableStateOf(AboutUiState())
        private set

    init {
        viewModelScope.launch {
            monitor.observeMode().collect { mode ->
                state.value = state.value.copy(
                    connectionMode = mode,
                    canCheckUpdates = mode == AppConnectionMode.ONLINE_API
                )
            }
        }
    }

    fun getCurrentVersionCode(): String =
        versionService.getCurrentVersionCode()

    fun getCurrentVersionName(): String =
        versionService.getCurrentVersionName()

    fun getAndroidVersionName(): String =
        Build.VERSION.RELEASE.orEmpty().trim().ifBlank { "-" }

    fun getAndroidSdk(): String =
        Build.VERSION.SDK_INT.toString()

    fun loadRemoteVersion() {
        if (state.value.loading) return

        state.value = state.value.copy(
            loading = true,
            error = null,
            info = null
        )

        viewModelScope.launch {
            val mode = monitor.checkMode()

            if (mode != AppConnectionMode.ONLINE_API) {
                state.value = state.value.copy(
                    loading = false,
                    connectionMode = mode,
                    canCheckUpdates = false,
                    versionCheck = null,
                    error = "Sin conexión a internet. No se puede consultar versión.",
                    info = null,
                    hasNewVersion = false,
                    isMandatoryUpdate = false,
                    canOperate = true,
                    showUpdateDialog = false,
                    updateMandatory = false,
                    updateFileName = "",
                    updateUrl = ""
                )
                return@launch
            }

            runCatching {
                versionService.checkVersion()
            }.onSuccess { data ->
                val version = data?.version

                val apiRequiresUpdate =
                    data?.requiereActualizacionBool() == true

                val hasNewVersion =
                    versionService.hasNewVersion(data) || apiRequiresUpdate

                val isMandatoryUpdate =
                    data?.actualizacionObligatoriaBool() == true

                val canOperate =
                    versionService.canOperate(data)

                val updateUrl = if (hasNewVersion) {
                    versionService.buildApkUrl(version)
                } else {
                    null
                }

                val apkFileName = version?.apkFileName?.trim().orEmpty()

                val canShowUpdateDialog =
                    hasNewVersion &&
                            !updateUrl.isNullOrBlank() &&
                            apkFileName.isNotBlank()

                if (canShowUpdateDialog) {
                    settingsManager.savePendingUpdate(
                        mandatory = isMandatoryUpdate,
                        downloadId = -1L,
                        apkFileName = apkFileName,
                        apkUrl = updateUrl.orEmpty(),
                        apkDownloaded = false
                    )

                    Log.d(
                        TAG,
                        "Update detected from About. mandatory=$isMandatoryUpdate, " +
                                "apk=$apkFileName, url=$updateUrl"
                    )
                } else {
                    settingsManager.clearPendingUpdate()
                }

                state.value = state.value.copy(
                    loading = false,
                    versionCheck = data,
                    error = null,
                    info = when {
                        canShowUpdateDialog && isMandatoryUpdate -> {
                            "Se encontró una actualización obligatoria."
                        }

                        canShowUpdateDialog -> {
                            "Se encontró una actualización opcional."
                        }

                        else -> {
                            "La aplicación está actualizada."
                        }
                    },
                    hasNewVersion = hasNewVersion,
                    isMandatoryUpdate = isMandatoryUpdate,
                    canOperate = canOperate,
                    connectionMode = AppConnectionMode.ONLINE_API,
                    canCheckUpdates = true,
                    showUpdateDialog = canShowUpdateDialog,
                    updateMandatory = isMandatoryUpdate,
                    updateFileName = apkFileName,
                    updateUrl = updateUrl.orEmpty()
                )
            }.onFailure { t ->
                Log.e(TAG, "No se pudo consultar versión desde About", t)

                state.value = state.value.copy(
                    loading = false,
                    versionCheck = null,
                    error = t.message ?: "No se pudo consultar versión",
                    info = null,
                    hasNewVersion = false,
                    isMandatoryUpdate = false,
                    canOperate = true,
                    showUpdateDialog = false,
                    updateMandatory = false,
                    updateFileName = "",
                    updateUrl = ""
                )
            }
        }
    }

    fun dismissOptionalUpdate() {
        viewModelScope.launch {
            settingsManager.clearPendingUpdate()
        }

        state.value = state.value.copy(
            showUpdateDialog = false,
            updateMandatory = false,
            updateFileName = "",
            updateUrl = ""
        )
    }

    fun onUpdateStarted() {
        state.value = state.value.copy(
            info = "Descargando actualización..."
        )
    }

    fun clearInfo() {
        state.value = state.value.copy(
            info = null
        )
    }

    fun clearError() {
        state.value = state.value.copy(
            error = null
        )
    }

    companion object {
        private const val TAG = "ABOUT_VM"
    }
}