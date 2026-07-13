package com.diprotec.inventariozebratc27.data.mappers

import com.diprotec.inventariozebratc27.data.local.entity.RuleEntity
import com.diprotec.inventariozebratc27.data.remote.dto.ReglaDto

fun ReglaDto.toEntity(): RuleEntity =
    RuleEntity(
        id = id,
        nombre = nombre,
        nombreApellido = nombreApellido,
        empresa = empresa,
        patente = patente,
        comentario = comentario,
        fotografia = fotografia,
        entradaSalida = entradaSalida,
        listaBlancaNegra = listaBlancaNegra,
        eliminaEnviados = eliminaEnviados,
        estado = estado.toApiBool(),
        perfil = perfil,
        rutEmpresa = rutEmpresa
    )