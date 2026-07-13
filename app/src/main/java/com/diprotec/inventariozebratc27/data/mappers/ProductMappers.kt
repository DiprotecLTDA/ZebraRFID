package com.diprotec.inventariozebratc27.data.mappers

import com.diprotec.inventariozebratc27.data.local.entity.ProductEntity
import com.diprotec.inventariozebratc27.data.remote.dto.ProductoDto

fun ProductoDto.toEntity(): ProductEntity =
    ProductEntity(
        codigo = codigo,
        codigoSecundario = codigoSecundario,
        descripcion = descripcion,
        estado = estado.toApiBool(),
        rutEmpresa = rutEmpresa
    )