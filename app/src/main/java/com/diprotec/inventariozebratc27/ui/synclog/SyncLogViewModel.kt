package com.diprotec.inventariozebratc27.ui.synclog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.data.local.dao.RuleDao
import com.diprotec.inventariozebratc27.data.local.dao.UserDao
import com.diprotec.inventariozebratc27.data.local.entity.RuleEntity
import com.diprotec.inventariozebratc27.data.local.entity.SyncLogEntity
import com.diprotec.inventariozebratc27.data.repository.SyncLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncLogUiItem(
    val inventoryName: String,
    val captures: String,
    val status: String,
    val result: String,
    val mode: String,
    val eventType: String,
    val sentAt: String,
    val message: String?
)

data class SyncLogUiState(
    val logs: List<SyncLogUiItem> = emptyList(),
    val canClearLogs: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SyncLogViewModel @Inject constructor(
    private val repository: SyncLogRepository,
    private val ruleDao: RuleDao,
    private val userDao: UserDao,
    private val settings: SettingsManager
) : ViewModel() {

    private val localMessage = MutableStateFlow<String?>(null)

    private val canClearLogsFlow = flow {
        emit(canClearByRule())
    }.flowOn(Dispatchers.IO)

    val uiState = combine(
        repository.observeAll()
            .map { items ->
                items.map { it.toUiItem() }
            },
        canClearLogsFlow,
        localMessage
    ) { logs, canClearLogs, message ->
        SyncLogUiState(
            logs = logs,
            canClearLogs = canClearLogs,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncLogUiState()
    )

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!canClearByRule()) {
                localMessage.value = "La regla del perfil no permite eliminar el historial."
                return@launch
            }

            repository.clearAll()
            localMessage.value = null
        }
    }

    private suspend fun canClearByRule(): Boolean {
        val rutEmpresa = settings.empresaRut.value.trim()
        val rutUsuario = settings.sessionRut.value.trim()
        val sessionPerfilId = settings.sessionPerfilId.value

        if (rutEmpresa.isBlank() || rutUsuario.isBlank()) {
            return false
        }

        val user = userDao.findByRut(rutUsuario)

        val perfil = user?.perfil.orEmpty().trim()

        val perfilId = when {
            user?.perfilId != null -> user.perfilId.toString().trim()
            sessionPerfilId > 0 -> sessionPerfilId.toString()
            else -> ""
        }

        if (perfil.isBlank() && perfilId.isBlank()) {
            return false
        }

        val rule = ruleDao.getByEmpresaAndPerfil(
            rutEmpresa = rutEmpresa,
            perfil = perfil,
            perfilId = perfilId
        ) ?: ruleDao.getByEmpresa(rutEmpresa)

        return rule?.isActiveForApiZero() == true &&
                rule.eliminaEnviados.isEnabledForApiZero()
    }

    private fun RuleEntity.isActiveForApiZero(): Boolean {
        return !estado
    }

    private fun String?.isEnabledForApiZero(): Boolean {
        return this.orEmpty().trim() == "0"
    }

    private fun SyncLogEntity.toUiItem(): SyncLogUiItem {
        return SyncLogUiItem(
            inventoryName = inventoryName,
            captures = capturesCount.toString(),
            status = translateStatus(inventoryStatus),
            result = translateResult(result),
            mode = translateMode(connectionMode),
            eventType = translateEvent(eventType),
            sentAt = formatDate(sentAt),
            message = message
        )
    }

    private fun translateStatus(value: String): String {
        return when (value) {
            "PENDING" -> "Pendiente"
            "IN_PROGRESS" -> "En progreso"
            "FINISHED" -> "Finalizado"
            "EXPIRED" -> "Expirado"
            else -> value
        }
    }

    private fun translateResult(value: String): String {
        return when (value) {
            "ENVIADO" -> "Enviado"
            "ERROR" -> "Error"
            else -> value
        }
    }

    private fun translateMode(value: String): String {
        return when (value) {
            "ONLINE_API" -> "Online/API"
            "LOCAL_ROOM" -> "Room local"
            "CHECKING" -> "Verificando"
            else -> value
        }
    }

    private fun translateEvent(value: String): String {
        return when (value) {
            "CAPTURES_SENT" -> "Capturas enviadas"
            "CAPTURES_FAILED" -> "Error al enviar capturas"
            "INVENTORY_FINISHED" -> "Inventario finalizado"
            "FINISH_FAILED" -> "Error al finalizar"
            else -> value
        }
    }

    private fun formatDate(value: Long): String {
        return SimpleDateFormat(
            "dd-MM-yyyy HH:mm:ss",
            Locale("es", "CL")
        ).format(Date(value))
    }
}