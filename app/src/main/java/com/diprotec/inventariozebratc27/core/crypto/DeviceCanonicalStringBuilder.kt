package com.diprotec.inventariozebratc27.core.crypto

class DeviceCanonicalStringBuilder {

    fun build(
        method: String,
        relativeUrl: String,
        timestamp: String
    ): String {
        return "${method.trim().uppercase()}|${relativeUrl.trim()}|${timestamp.trim()}"
    }
}