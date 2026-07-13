package com.diprotec.inventariozebratc27.data.mappers

import com.diprotec.inventariozebratc27.data.local.entity.UnitMeasureEntity
import com.diprotec.inventariozebratc27.data.remote.dto.UnidadMedidaDto

fun UnidadMedidaDto.toEntity(): UnitMeasureEntity =
    UnitMeasureEntity(
        id = id,
        nombre = nombre,
        valor = valor,
        predeterminado = predeterminado.toApiBool(),
        estado = estado.toApiBool(),
        rutEmpresa = rutEmpresa
    )