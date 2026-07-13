package com.diprotec.inventariozebratc27.ui.about

import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.components.AppActionButton
import com.diprotec.inventariozebratc27.ui.components.AppTopBar
import com.diprotec.inventariozebratc27.ui.components.AppCard

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
                    .padding(horizontal = Dimens.space20, vertical = Dimens.space18),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = White,
                    modifier = Modifier.size(Dimens.progressSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = appName,
                            modifier = Modifier.size(Dimens.buttonHeightExtraLarge)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space22))

                AppActionButton(
                    text = when {
                        !s.canCheckUpdates -> "Sin conexión"
                        else -> "Chequear versión"
                    },
                    onClick = {
                        vm.loadRemoteVersion()
                    },
                    enabled = !s.loading && s.canCheckUpdates,
                    loading = s.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeightMedium)
                )

                Spacer(modifier = Modifier.height(Dimens.space18))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space10)
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

                Spacer(modifier = Modifier.height(Dimens.space24))
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
    AppTopBar(title = "ACERCA DE")
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

        Spacer(modifier = Modifier.height(Dimens.space4))

        AppCard(
            containerColor = White,
            contentPadding = PaddingValues(
                horizontal = Dimens.space16,
                vertical = Dimens.space14
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.cardMinHeight)
                .border(
                    width = Dimens.borderWidth,
                    color = BorderGray,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        }
    }
}
