package com.diprotec.inventariozebratc27.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat

class ZebraScannerReceiver(
    private val context: Context,
    private val onScan: (codigo: String, tipo: String?) -> Unit,
    private val onError: (mensaje: String) -> Unit
) {

    companion object {
        private const val TAG = "ZebraScannerReceiver"

        private const val ACTION_SCAN = "com.symbol.RECEIVER"

        private const val EXTRA_DATA_STRING =
            "com.symbol.datawedge.data_string"

        private const val EXTRA_LABEL_TYPE =
            "com.symbol.datawedge.label_type"
    }

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                onError("Intent de escáner vacío")
                return
            }

            if (intent.action != ACTION_SCAN) {
                return
            }

            val codigo = intent.getStringExtra(EXTRA_DATA_STRING)
            val tipo = intent.getStringExtra(EXTRA_LABEL_TYPE)

            if (codigo.isNullOrBlank()) {
                onError("Lectura vacía o no válida")
                return
            }

            val codigoLimpio = codigo.replace("\"", "")

            Log.i(TAG, "Código leído: $codigoLimpio")
            Log.i(TAG, "Tipo código: $tipo")

            onScan(codigoLimpio, tipo)
        }
    }

    fun register() {
        if (registered) return

        val filter = IntentFilter().apply {
            addAction(ACTION_SCAN)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        registered = true
        Log.i(TAG, "Receiver de escáner registrado")
    }

    fun unregister() {
        if (!registered) return

        try {
            context.unregisterReceiver(receiver)
            registered = false
            Log.i(TAG, "Receiver de escáner desregistrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error desregistrando receiver", e)
        }
    }
}