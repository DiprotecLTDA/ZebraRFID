package com.diprotec.inventariozebratc27.core.network

internal fun normalizeBaseUrl(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}
