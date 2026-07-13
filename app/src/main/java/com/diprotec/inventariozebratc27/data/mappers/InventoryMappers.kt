package com.diprotec.inventariozebratc27.data.mappers

import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteEntity
import com.diprotec.inventariozebratc27.data.local.entity.InventoryRemoteUserEntity
import com.diprotec.inventariozebratc27.data.remote.dto.InventarioDto
import com.diprotec.inventariozebratc27.data.remote.dto.InventarioUsuarioDto

fun InventarioDto.toEntity(): InventoryRemoteEntity =
    InventoryRemoteEntity(
        id = id,
        descripcion = descripcion,
        fecha = fecha,
        hora = hora,
        desde = desde,
        hasta = hasta,
        rutAdministrador = rutAdministrador,
        estado = estado.toApiBool(),
        rutEmpresa = rutEmpresa
    )

fun InventarioUsuarioDto.toEntity(): InventoryRemoteUserEntity {
    return InventoryRemoteUserEntity(
        id = "${inventarioId.orEmpty()}_${rutUsuario.orEmpty()}",
        inventarioId = inventarioId.orEmpty(),
        rutUsuario = rutUsuario.orEmpty()
    )
}