package com.diprotec.inventariozebratc27.rfid

data class RfidTagRead(
    val epc: String,
    val rssi: Short? = null,
    val seenAtMillis: Long = System.currentTimeMillis()
)