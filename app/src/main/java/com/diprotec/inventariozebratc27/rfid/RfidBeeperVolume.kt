package com.diprotec.inventariozebratc27.rfid

/**
 * Volumen del beeper físico del lector RFID.
 *
 * Se persiste por [name] para no depender del orden de las constantes.
 */
enum class RfidBeeperVolume(val label: String) {
    HIGH("Alto"),
    MEDIUM("Medio"),
    LOW("Bajo"),
    QUIET("Silencio");

    companion object {
        val DEFAULT = HIGH

        fun fromName(value: String?): RfidBeeperVolume {
            return entries.firstOrNull { it.name == value?.trim()?.uppercase() }
                ?: DEFAULT
        }
    }
}
