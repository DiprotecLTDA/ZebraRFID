package com.diprotec.inventariozebratc27.rfid

enum class RfidConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    READING,
    PAUSED,
    ERROR
}