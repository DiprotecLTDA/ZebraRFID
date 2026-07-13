package com.diprotec.inventariozebratc27.serial

import android.content.Context
import android.net.Uri
import android.util.Log

object ZebraSerialProvider {

    private const val TAG = "ZebraSerialProvider"

    private const val SERIAL_URI =
        "content://oem_info/oem.zebra.secure/build_serial"

    @Volatile
    private var cachedSerial: String? = null

    fun getSerial(context: Context): String? {
        val existing = cachedSerial
        if (!existing.isNullOrBlank()) {
            return existing
        }

        return synchronized(this) {
            val secondCheck = cachedSerial
            if (!secondCheck.isNullOrBlank()) {
                secondCheck
            } else {
                val serial = readSerialFromZebra(context.applicationContext)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

                cachedSerial = serial
                serial
            }
        }
    }

    fun requireSerial(context: Context): String {
        return getSerial(context)
            ?: throw IllegalStateException("No se pudo obtener serial Zebra")
    }

    fun clearCache() {
        cachedSerial = null
    }

    private fun readSerialFromZebra(context: Context): String? {
        return try {
            context.contentResolver.query(
                Uri.parse(SERIAL_URI),
                null,
                null,
                null,
                null
            )?.use { cursor ->

                if (!cursor.moveToFirst()) {
                    Log.e(TAG, "Cursor vacío. Posible falta de permiso AccessMgr.")
                    return null
                }

                if (cursor.columnCount == 0) {
                    Log.e(TAG, "OEMInfo no devolvió columnas.")
                    return null
                }

                cursor.getString(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo serial Zebra", e)
            null
        }
    }
}