package com.diprotec.inventariozebratc27.rfid

data class RfidLocateResult(
    val epc: String,
    val relativeDistance: Int
)