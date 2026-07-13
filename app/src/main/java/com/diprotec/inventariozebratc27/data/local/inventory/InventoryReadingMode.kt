package com.diprotec.inventariozebratc27.data.local.inventory

enum class InventoryReadingMode(
    val value: Int
) {
    LASER(0),
    RFID(1);

    companion object {
        fun fromValue(value: Int): InventoryReadingMode {
            return entries.firstOrNull { it.value == value } ?: LASER
        }
    }
}