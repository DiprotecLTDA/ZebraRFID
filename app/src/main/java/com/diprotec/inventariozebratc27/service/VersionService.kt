package com.diprotec.inventariozebratc27.service

import android.content.Context
import android.os.Build
import android.util.Log
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.network.ApiCallExecutor
import com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder
import com.diprotec.inventariozebratc27.data.remote.api.ApiService
import com.diprotec.inventariozebratc27.data.remote.dto.VersionCheckDataDto
import com.diprotec.inventariozebratc27.data.remote.dto.VersionDataDto
import com.diprotec.inventariozebratc27.data.remote.dto.VersionEntradaRequest

class VersionService(
    private val api: ApiService,
    private val settings: SettingsManager,
    private val headersBuilder: ProtectedHeadersBuilder,
    private val apiCallExecutor: ApiCallExecutor,
    context: Context
) {

    private val appContext: Context = context.applicationContext

    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = appContext.packageManager.getPackageInfo(
                appContext.packageName,
                0
            )

            packageInfo.versionName ?: "0.0.0"
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "No fue posible obtener versionName",
                exception
            )

            "0.0.0"
        }
    }

    fun getCurrentVersionNameForApi(): String {
        return getCurrentVersionName()
            .trim()
            .replace(".", ",")
    }

    fun getCurrentVersionCode(): String {
        return try {
            val packageInfo = appContext.packageManager.getPackageInfo(
                appContext.packageName,
                0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "No fue posible obtener versionCode",
                exception
            )

            "0"
        }
    }

    fun getCurrentVersionCodeInt(): Int {
        return getCurrentVersionCode()
            .trim()
            .toIntOrNull()
            ?: 0
    }

    suspend fun checkVersion(): VersionCheckDataDto? {
        val empresaRut = settings.empresaRut.value.trim()

        require(empresaRut.isNotBlank()) {
            "Empresa RUT no configurado"
        }

        val versionActual = getCurrentVersionCodeInt()
        val versionActualNombre = getCurrentVersionName().trim()
        val fabricante = Build.MANUFACTURER.orEmpty().trim()

        val androidVersion = Build.VERSION.RELEASE
            .orEmpty()
            .trim()
            .substringBefore(".")
            .toIntOrNull()
            ?: 0

        val modelo = Build.MODEL.orEmpty().trim()

        val relativeUrl =
            "/api/website/v1/versiones/$empresaRut/GetVersion"

        Log.d(TAG, "Consultando versión")
        Log.d(TAG, "empresaRut=$empresaRut")
        Log.d(TAG, "versionActual=$versionActual")
        Log.d(TAG, "versionActualNombre=$versionActualNombre")
        Log.d(TAG, "fabricante=$fabricante")
        Log.d(TAG, "androidVersion=$androidVersion")
        Log.d(TAG, "modelo=$modelo")

        val headers = headersBuilder.build(
            method = "POST",
            relativeUrl = relativeUrl
        )

        val request = VersionEntradaRequest(
            versionActual = versionActual,
            versionActualNombre = versionActualNombre,
            fabricante = fabricante,
            androidVersion = androidVersion,
            modelo = modelo
        )

        val response = apiCallExecutor.execute {
            api.getVersion(
                empresaRUT = empresaRut,
                apiKey = headers.apiKey,
                authorization = headers.authorization,
                deviceSession = headers.deviceSession,
                deviceSignature = headers.deviceSignature,
                deviceTimestamp = headers.deviceTimestamp,
                body = request
            )
        }

        val data = response.data
        val version = data?.version

        Log.d(TAG, "puedeOperar=${data?.puedeOperar}")
        Log.d(
            TAG,
            "requiereActualizacion=${data?.requiereActualizacion}"
        )
        Log.d(
            TAG,
            "actualizacionObligatoria=${data?.actualizacionObligatoria}"
        )
        Log.d(TAG, "mensaje=${data?.mensaje}")
        Log.d(TAG, "versionName=${version?.versionName}")
        Log.d(TAG, "versionCode=${version?.versionCode}")
        Log.d(TAG, "forceUpdate=${version?.forceUpdate}")
        Log.d(TAG, "isActive=${version?.isActive}")
        Log.d(TAG, "isPublished=${version?.isPublished}")
        Log.d(TAG, "fabricanteRemoto=${version?.fabricante}")
        Log.d(TAG, "androidVersionRemoto=${version?.androidVersion}")
        Log.d(TAG, "modeloRemoto=${version?.modelo}")
        Log.d(TAG, "apkFileName=${version?.apkFileName}")
        Log.d(TAG, "apkRelativePath=${version?.apkRelativePath}")
        Log.d(TAG, "fileSizeBytes=${version?.fileSizeBytes}")

        return data
    }

    fun hasNewVersion(
        data: VersionCheckDataDto?
    ): Boolean {
        val version = data?.version ?: return false

        if (!version.isActiveBool()) {
            return false
        }

        if (!version.isPublishedBool()) {
            return false
        }

        return version.versionCodeInt() > getCurrentVersionCodeInt()
    }

    fun isUpdateRequired(
        data: VersionCheckDataDto?
    ): Boolean {
        val version = data?.version ?: return false

        val localVersionCode = getCurrentVersionCodeInt()
        val remoteVersionCode = version.versionCodeInt()

        if (remoteVersionCode <= localVersionCode) {
            return false
        }

        if (!version.isActiveBool()) {
            return false
        }

        if (!version.isPublishedBool()) {
            return false
        }

        return data.actualizacionObligatoriaBool() ||
                version.isForceUpdate() ||
                localVersionCode < version.minSupportedVersionCodeInt()
    }

    fun canOperate(
        data: VersionCheckDataDto?
    ): Boolean {
        return data?.puedeOperarBool() ?: true
    }

    fun buildApkUrl(
        version: VersionDataDto?
    ): String? {
        val apkRelativePath =
            version?.apkRelativePath?.trim().orEmpty()

        val apkFileName =
            version?.apkFileName?.trim().orEmpty()

        if (apkRelativePath.isBlank()) {
            return null
        }

        if (apkFileName.isBlank()) {
            return null
        }

        val cleanPath = apkRelativePath.trimEnd('/')
        val cleanFileName = apkFileName.trimStart('/')

        return "$cleanPath/$cleanFileName"
    }

    companion object {
        private const val TAG = "VERSION_SYNC"
    }
}