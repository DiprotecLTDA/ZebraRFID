package com.diprotec.inventariozebratc27.core.format

import java.util.Locale

object TextFormatter {

    fun toTitleCase(input: String): String {
        if (input.isBlank()) return input

        return input
            .lowercase(Locale("es", "CL"))
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(Locale("es", "CL"))
                    } else {
                        it.toString()
                    }
                }
            }
    }
}