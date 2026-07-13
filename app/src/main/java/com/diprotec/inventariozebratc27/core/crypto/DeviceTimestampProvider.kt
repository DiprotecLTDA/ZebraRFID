package com.diprotec.inventariozebratc27.core.crypto

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DeviceTimestampProvider {

    private val formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC)

    fun nowUtc(): String {
        return formatter.format(Instant.now())
    }
}