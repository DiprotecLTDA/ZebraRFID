package com.diprotec.inventariozebratc27.ui.settings

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
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.ButtonRed
import com.diprotec.inventariozebratc27.ui.theme.ButtonRedDark
import com.diprotec.inventariozebratc27.ui.theme.InventoryMenuButton
import com.diprotec.inventariozebratc27.ui.theme.LabelGray
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White
import kotlinx.coroutines.launch

private val SuccessBg = Color(0xFFEFFFFC)
private val SuccessBorder = Color(0xFFBDEBE5)

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
                    .widthIn(max = 460.dp)
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SettingsStatusHeader(
                    hasCreds = hasCreds,
                    deviceActivated = deviceActivated
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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

                Spacer(modifier = Modifier.height(24.dp))
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Estado del dispositivo",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

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
        shape = RoundedCornerShape(18.dp),
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
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = White
                )
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    ButtonRed,
                                    ButtonRedDark
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cerrar",
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

@Composable
private fun SettingsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "CONFIGURACIÓN",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = BorderGray,
            unfocusedBorderColor = BorderGray,
            disabledBorderColor = BorderGray,
            focusedLabelColor = LabelGray,
            unfocusedLabelColor = LabelGray,
            disabledLabelColor = LabelGray,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextPrimary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
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
            .background(SuccessBg, RoundedCornerShape(12.dp))
            .border(1.dp, SuccessBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

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
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

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
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = Background
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
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