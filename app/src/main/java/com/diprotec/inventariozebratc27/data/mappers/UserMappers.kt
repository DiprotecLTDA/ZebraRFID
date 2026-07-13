package com.diprotec.inventariozebratc27.data.mappers

import com.diprotec.inventariozebratc27.data.local.entity.UserEntity
import com.diprotec.inventariozebratc27.data.remote.dto.UserDto

internal fun String?.toApiBool(): Boolean {
    return this.equals("true", ignoreCase = true) ||
            this.equals("1", ignoreCase = true) ||
            this.equals("si", ignoreCase = true) ||
            this.equals("sí", ignoreCase = true)
}

fun UserDto.toEntity(): UserEntity =
    UserEntity(
        rut = rut,
        nombre = nombre,
        email = email,
        telefono = telefono,
        perfil = perfil,
        estado = estado.toApiBool(),
        perfilId = perfilId,
        passwordHash = passwordHash,
        passwordSalt = passwordSalt,
        passwordAlgoritmo = passwordAlgoritmo
    )