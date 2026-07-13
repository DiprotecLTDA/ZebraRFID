package com.diprotec.inventariozebratc27.data.mappers

import com.diprotec.inventariozebratc27.data.local.entity.LocationEntity
import com.diprotec.inventariozebratc27.data.remote.dto.UbicacionDto

fun UbicacionDto.toEntity(): LocationEntity =
    LocationEntity(
        id = id,
        nombre = nombre,
        estado = estado.toApiBool(),
        rutEmpresa = rutEmpresa
    )