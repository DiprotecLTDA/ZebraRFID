package com.diprotec.inventariozebratc27.core.key

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore

class DeviceKeyManager {

    fun ensureKeyPair(alias: String = DeviceKeyConstants.DEVICE_KEY_ALIAS) {
        val keyStore = KeyStore.getInstance(DeviceKeyConstants.ANDROID_KEYSTORE).apply {
            load(null)
        }

        if (keyStore.containsAlias(alias)) return

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            DeviceKeyConstants.ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or
                    KeyProperties.PURPOSE_VERIFY or
                    KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setDigests(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA512
            )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setKeySize(DeviceKeyConstants.KEY_SIZE)
            .build()

        generator.initialize(spec)
        generator.generateKeyPair()
    }

    fun getPublicKeyBase64(alias: String = DeviceKeyConstants.DEVICE_KEY_ALIAS): String {
        ensureKeyPair(alias)

        val keyStore = KeyStore.getInstance(DeviceKeyConstants.ANDROID_KEYSTORE).apply {
            load(null)
        }

        val cert = keyStore.getCertificate(alias)
            ?: throw IllegalStateException("No existe certificado para alias=$alias")

        return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
    }

    fun getAlias(): String = DeviceKeyConstants.DEVICE_KEY_ALIAS
}