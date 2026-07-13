package com.diprotec.inventariozebratc27.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.core.session.SessionManager
import com.diprotec.inventariozebratc27.serial.ZebraSerialProvider
import com.diprotec.inventariozebratc27.service.SessionLifecycleService
import com.diprotec.inventariozebratc27.ui.theme.InventarioTheme
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity(), EMDKManager.EMDKListener {

    @Inject
    lateinit var settings: SettingsManager

    @Inject
    lateinit var session: SessionManager

    private var emdkManager: EMDKManager? = null
    private var profileManager: ProfileManager? = null

    private var versionCodeAtInstallStart: Int = -1

    private val bluetoothPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val deniedPermissions = result.filterValues { granted ->
                !granted
            }.keys

            if (deniedPermissions.isEmpty()) {
                Log.i(TAG, "Permisos Bluetooth RFID concedidos")
            } else {
                Log.w(
                    TAG,
                    "Permisos Bluetooth RFID denegados: $deniedPermissions"
                )
            }
        }

    companion object {
        private const val TAG = "MainActivity"
        private const val PROFILE_NAME = "ProfileInventario"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(
            Intent(this, SessionLifecycleService::class.java)
        )

        window.statusBarColor = Color.rgb(198, 52, 40)

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = false

        versionCodeAtInstallStart = getCurrentVersionCodeInt()

        iniciarEMDK()

        requestBluetoothPermissionsIfNeeded()

        setContent {
            InventarioTheme {
                AppNavHost(
                    session = session
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            session.touchIfValid()

            val hasPendingUpdate =
                settings.pendingUpdateMandatory.value ||
                        settings.pendingDownloadId.value > 0L ||
                        settings.apkDownloaded.value

            if (!hasPendingUpdate) return@launch

            val currentVersionCode = getCurrentVersionCodeInt()

            if (currentVersionCode > versionCodeAtInstallStart) {
                settings.clearPendingUpdate()
                versionCodeAtInstallStart = currentVersionCode
            }
        }
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(
                    Manifest.permission.BLUETOOTH_SCAN
                )
            }
        } else {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            bluetoothPermissionsLauncher.launch(
                permissionsToRequest.toTypedArray()
            )
        } else {
            Log.i(TAG, "Permisos Bluetooth RFID ya concedidos")
        }
    }

    private fun iniciarEMDK() {
        Log.i(TAG, "Iniciando EMDK...")

        val result = EMDKManager.getEMDKManager(
            applicationContext,
            this
        )

        if (result.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            Log.e(
                TAG,
                "No se pudo iniciar EMDK: ${result.statusCode}"
            )
        }
    }

    override fun onOpened(manager: EMDKManager) {
        emdkManager = manager

        profileManager = manager.getInstance(
            EMDKManager.FEATURE_TYPE.PROFILE
        ) as? ProfileManager

        if (profileManager == null) {
            Log.e(TAG, "No se pudo obtener ProfileManager")
            return
        }

        Log.i(TAG, "EMDK listo. Aplicando perfil Zebra...")
        aplicarPerfilZebra()
    }

    private fun aplicarPerfilZebra() {
        val pm = profileManager

        if (pm == null) {
            Log.w(TAG, "ProfileManager aún no está listo")
            return
        }

        try {
            val modifyData = arrayOfNulls<String>(1)

            val result = pm.processProfile(
                PROFILE_NAME,
                ProfileManager.PROFILE_FLAG.SET,
                modifyData
            )

            Log.i(TAG, "processProfile statusCode: ${result.statusCode}")
            Log.i(TAG, "processProfile statusString: ${result.statusString}")

            if (
                result.statusCode == EMDKResults.STATUS_CODE.SUCCESS ||
                result.statusCode == EMDKResults.STATUS_CODE.CHECK_XML
            ) {
                leerSerialZebra()
            } else {
                Log.e(
                    TAG,
                    "Error aplicando perfil Zebra: ${result.statusCode}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error aplicando perfil EMDK Zebra", e)
        }
    }

    private fun leerSerialZebra() {
        val serial = ZebraSerialProvider.getSerial(this)

        if (serial.isNullOrBlank()) {
            Log.e(TAG, "Perfil aplicado, pero no se pudo leer serial Zebra")
        } else {
            Log.i(TAG, "Serial Zebra obtenido correctamente: $serial")
        }
    }

    override fun onClosed() {
        Log.w(TAG, "EMDK cerrado")

        profileManager = null
        emdkManager = null
    }

    override fun onDestroy() {
        profileManager = null

        emdkManager?.release()
        emdkManager = null

        super.onDestroy()
    }

    private fun getCurrentVersionCodeInt(): Int {
        return try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo obtener versionCode", e)
            -1
        }
    }
}