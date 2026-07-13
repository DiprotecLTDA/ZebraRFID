package com.diprotec.inventariozebratc27.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.diprotec.inventariozebratc27.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SessionLifecycleService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        runBlocking(Dispatchers.IO) {
            SettingsDataStore(applicationContext).clearUserSession()
        }

        stopSelf()

        super.onTaskRemoved(rootIntent)
    }
}