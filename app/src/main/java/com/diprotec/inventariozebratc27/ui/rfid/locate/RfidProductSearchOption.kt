package com.diprotec.inventariozebratc27.ui.rfid.locate

data class RfidProductSearchOption(
    val productCode: String,
    val secondaryCode: String?,
    val description: String?,
    val rfidSourceValue: String,
    val rfidSourceLabel: String,
    val epcToLocate: String,
    val mode: RfidSearchMode
)

enum class RfidSearchMode {
    DIRECT,
    ASCII_HEX,
    PADDED_24_DIRECT,
    PADDED_24_ASCII_HEX
}