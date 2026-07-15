package com.diprotec.inventariozebratc27.core.gs1

/**
 * Deriva los posibles códigos de producto a partir de un EPC leído.
 *
 * Es la operación **inversa** de la que hace la pantalla de localización, que genera el EPC
 * a buscar a partir del código del producto (directo, ASCII-HEX, y ambos rellenados con
 * ceros a 24 dígitos). Las etiquetas en terreno vienen de las dos formas: unas codificadas
 * como GS1 SGTIN-96 (con el GTIN embebido) y otras con el código del producto grabado en
 * crudo, por lo que hay que probar varias derivaciones contra el catálogo.
 *
 * Los candidatos salen en orden de prioridad y sin repetidos.
 */
object RfidProductCodeCandidates {

    fun from(decoded: Gs1EpcDecoded): List<String> {
        val candidates = LinkedHashSet<String>()

        // 1. GTIN embebido (EPC GS1 estándar, p. ej. SGTIN-96).
        decoded.gtin
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { candidates.add(it) }

        val epc = decoded.epcNormalized
            .trim()
            .ifBlank { decoded.epcRaw.trim().uppercase() }

        if (epc.isNotBlank()) {
            // 2. El EPC tal cual: el código ya es el EPC.
            candidates.add(epc)

            // 3. Sin ceros a la izquierda: código numérico rellenado a 24 dígitos.
            epc.trimStart('0')
                .takeIf { it.isNotBlank() }
                ?.let { candidates.add(it) }

            // 4. y 5. El hex decodificado como ASCII, y sin su relleno de ceros.
            asciiFromHex(epc)?.let { ascii ->
                candidates.add(ascii)

                ascii.trimStart('0')
                    .takeIf { it.isNotBlank() }
                    ?.let { candidates.add(it) }
            }
        }

        return candidates.toList()
    }

    /**
     * Decodifica un hex como texto ASCII. Devuelve null si no es hex par o si algún byte
     * cae fuera del rango imprimible: sin esa guarda se generarían candidatos basura.
     */
    private fun asciiFromHex(hex: String): String? {
        if (hex.length < 2 || hex.length % 2 != 0) return null

        val builder = StringBuilder(hex.length / 2)

        for (index in hex.indices step 2) {
            val code = hex.substring(index, index + 2)
                .toIntOrNull(16)
                ?: return null

            if (code < 0x20 || code > 0x7E) return null

            builder.append(code.toChar())
        }

        return builder.toString()
            .trim()
            .takeIf { it.isNotBlank() }
    }
}
