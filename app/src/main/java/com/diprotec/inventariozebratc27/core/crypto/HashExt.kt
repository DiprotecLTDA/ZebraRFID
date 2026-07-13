package com.diprotec.inventariozebratc27.core.crypto

import java.security.MessageDigest

fun String.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(toByteArray()).joinToString("") { "%02x".format(it) }
}