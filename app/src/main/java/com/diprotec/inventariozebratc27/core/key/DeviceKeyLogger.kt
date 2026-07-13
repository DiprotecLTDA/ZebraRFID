package com.diprotec.inventariozebratc27.core.key

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceKeyLogger @Inject constructor(
    private val keyStoreManager: DeviceKeyStoreManager,
    private val publicKeyExporter: DevicePublicKeyExporter
) {

    fun logKeys(alias: String = DeviceKeyConstants.DEVICE_KEY_ALIAS) {
        val exists = keyStoreManager.exists(alias)
        val publicPem = publicKeyExporter.exportPublicKeyPem(alias)

        Log.d(TAG, "alias=$alias")
        Log.d(TAG, "exists=$exists")
        Log.d(TAG, "publicKeyPem=${maskPem(publicPem)}")
    }

    private fun maskPem(value: String): String {
        val compact = value
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .trim()

        if (compact.length <= 16) return "****"

        return "${compact.take(8)}****${compact.takeLast(8)}"
    }

    companion object {
        private const val TAG = "DeviceKeyLogger"
    }
}