package com.diprotec.inventariozebratc27.ui.settings

import com.diprotec.inventariozebratc27.ui.theme.Dimens

import android.app.Activity
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.ui.common.AppFloatingMessage
import com.diprotec.inventariozebratc27.ui.components.AppTextField
import com.diprotec.inventariozebratc27.ui.components.AppTopBar
import com.diprotec.inventariozebratc27.ui.components.AppActionButton
import com.diprotec.inventariozebratc27.ui.components.SectionTitle
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.ButtonRed
import com.diprotec.inventariozebratc27.ui.theme.ButtonRedDark
import com.diprotec.inventariozebratc27.ui.theme.InventoryMenuButton
import com.diprotec.inventariozebratc27.ui.theme.LabelGray
import com.diprotec.inventariozebratc27.ui.theme.SuccessBg
import com.diprotec.inventariozebratc27.ui.theme.SuccessBorder
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onDone: () -> Unit,
    onBack: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val s by vm.state

    val hasCreds = s.credentialsLoaded
    val deviceActivated = vm.isDeviceActivated()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var picking by remember {
        mutableStateOf(false)
    }

    if (s.error != null) {
        ErrorDialog(
            message = s.error ?: "",
            onDismiss = {
                vm.clearError()
            }
        )
    }

    val pickKeyFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        picking = false

        if (res.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val uri = res.data?.data ?: return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        scope.launch {
            val ok = vm.importKeyFromUri(uri.toString())

            if (ok) {
                runCatching {
                    DocumentsContract.deleteDocument(
                        context.contentResolver,
                        uri
                    )
                }
            }
        }
    }

    fun launchPicker() {
        if (picking) return
        picking = true

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        pickKeyFile.launch(intent)
    }

    LaunchedEffect(s.info) {
        s.info?.let { message ->
            AppFloatingMessage.info(message)
            vm.clearInfo()
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar()
        },
        bottomBar = {
            SettingsBottomActions(
                saving = s.saving,
                onPickCredentials = {
                    launchPicker()
                },
                onSave = {
                    vm.onSave(onDone)
                }
            )
        },
        containerColor = Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Dimens.settingsContentWidth)
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = Dimens.space20, vertical = Dimens.space16),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SettingsStatusHeader(
                    hasCreds = hasCreds,
                    deviceActivated = deviceActivated
                )

                Spacer(modifier = Modifier.height(Dimens.space24))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space14)
                ) {
                    SettingsTextField(
                        value = s.baseUrl,
                        onValueChange = { value ->
                            vm.onBaseUrlChange(value)
                        },
                        label = "Base URL (termina con /)",
                        testTag = "input_base_url"
                    )

                    SettingsTextField(
                        value = s.empresaRut,
                        onValueChange = { value ->
                            vm.onEmpresaChange(value)
                        },
                        label = "Empresa RUT",
                        keyboardType = KeyboardType.Ascii,
                        enabled = !deviceActivated,
                        testTag = "input_empresa_rut"
                    )

                    SettingsTextField(
                        value = s.activationCode,
                        onValueChange = { value ->
                            vm.onActivationCodeChange(value)
                        },
                        label = "Código de activación",
                        keyboardType = KeyboardType.Text,
                        enabled = !deviceActivated,
                        testTag = "input_activation_code"
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.space24))
            }
        }
    }
}

@Composable
private fun SettingsStatusHeader(
    hasCreds: Boolean,
    deviceActivated: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.space12)
    ) {
        SectionTitle(text = "Estado del dispositivo")

        if (hasCreds) {
            StatusBanner(
                text = "Credenciales cargadas",
                isSuccess = true
            )
        } else {
            NeutralStatus(
                text = "Aún no se han cargado credenciales."
            )
        }

        if (deviceActivated) {
            StatusBanner(
                text = "Dispositivo activado",
                isSuccess = true
            )
        } else {
            NeutralStatus(
                text = "El dispositivo aún no ha sido activado."
            )
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Error de configuración",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        },
        confirmButton = {
            AppActionButton(
                text = "Cerrar",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun SettingsTopBar() {
    AppTopBar(title = "CONFIGURACIÓN")
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    testTag: String
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = label,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.buttonHeightLarge)
            .testTag(testTag)
    )
}

@Composable
private fun StatusBanner(
    text: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SuccessBg, MaterialTheme.shapes.small)
            .border(Dimens.borderWidth, SuccessBorder, MaterialTheme.shapes.small)
            .padding(horizontal = Dimens.space12, vertical = Dimens.space10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.space18)
        )

        Spacer(modifier = Modifier.width(Dimens.space10))

        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NeutralStatus(
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = LabelGray,
            modifier = Modifier.size(Dimens.space18)
        )

        Spacer(modifier = Modifier.width(Dimens.space10))

        Text(
            text = text,
            color = LabelGray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SettingsBottomActions(
    saving: Boolean,
    onPickCredentials: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        tonalElevation = Dimens.space4,
        shadowElevation = Dimens.space8,
        color = Background
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.space20, vertical = Dimens.space14),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Dimens.settingsContentWidth),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                InventoryMenuButton(
                    text = "Credenciales",
                    icon = Icons.Default.Key,
                    enabled = !saving,
                    onClick = onPickCredentials,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_credenciales")
                )

                InventoryMenuButton(
                    text = "Guardar",
                    icon = Icons.Default.Save,
                    enabled = !saving,
                    loading = saving,
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_guardar")
                )
            }
        }
    }
}
