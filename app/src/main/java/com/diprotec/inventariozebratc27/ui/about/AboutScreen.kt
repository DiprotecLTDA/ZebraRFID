package com.diprotec.inventariozebratc27.ui.about

import android.os.Build
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.ui.common.AppFloatingMessage
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White
import com.diprotec.inventariozebratc27.ui.update.StartupUpdateDialog

@Composable
fun AboutScreen(
    onBack: () -> Unit = {},
    vm: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val packageName = context.packageName

    val packageInfo = remember {
        pm.getPackageInfo(packageName, 0)
    }

    val appInfo = remember {
        pm.getApplicationInfo(packageName, 0)
    }

    val appName = remember {
        pm.getApplicationLabel(appInfo).toString()
    }

    val versionName = remember {
        packageInfo.versionName ?: "-"
    }

    val appIconBitmap = remember {
        pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
    }

    val model = remember {
        Build.MODEL.orEmpty()
    }

    val s by vm.state

    val remoteVersion = s.versionCheck?.version
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        vm.loadRemoteVersion()
    }

    LaunchedEffect(s.info) {
        s.info?.let {
            AppFloatingMessage.info(it)
            vm.clearInfo()
        }
    }

    LaunchedEffect(s.error) {
        s.error?.let {
            AppFloatingMessage.error(it)
            vm.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            AboutTopBar(
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = White,
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = appName,
                            modifier = Modifier.size(88.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        vm.loadRemoteVersion()
                    },
                    enabled = !s.loading && s.canCheckUpdates,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.70f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (s.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = when {
                                !s.canCheckUpdates -> "Sin conexión"
                                else -> "Chequear versión"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AboutRow(
                        label = "Versión instalada",
                        value = versionName
                    )

                    AboutRow(
                        label = "Versión disponible",
                        value = remoteVersion?.versionName?.trim().orEmpty().ifBlank { "-" }
                    )

                    AboutRow(
                        label = "Tamaño actualización",
                        value = formatBytes(remoteVersion?.fileSizeBytes)
                    )

                    AboutRow(
                        label = "Modelo",
                        value = model.ifBlank { "-" }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (s.showUpdateDialog) {
            StartupUpdateDialog(
                mandatory = s.updateMandatory,
                apkFileName = s.updateFileName,
                apkUrl = s.updateUrl,
                onOptionalDismissed = {
                    vm.dismissOptionalUpdate()
                },
                onUpdateStarted = {
                    vm.onUpdateStarted()
                }
            )
        }
    }
}

@Composable
private fun AboutTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ACERCA DE",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatBytes(
    bytesText: String?
): String {
    val bytes = bytesText?.trim()?.toLongOrNull() ?: return "-"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0

    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = White,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .border(
                    width = 1.dp,
                    color = BorderGray,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}