package com.diprotec.inventariozebratc27.ui.startup

import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.components.AppActionButton
import com.diprotec.inventariozebratc27.ui.components.AppButtonStyle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.R
import com.diprotec.inventariozebratc27.ui.common.AppFloatingMessage
import com.diprotec.inventariozebratc27.ui.common.ResponsiveCenteredContent
import com.diprotec.inventariozebratc27.ui.common.ResponsiveScreen
import com.diprotec.inventariozebratc27.ui.update.StartupUpdateDialog

@Composable
fun StartupGateScreen(
    onGoLogin: () -> Unit,
    onGoMainMenu: () -> Unit,
    onGoSettings: () -> Unit,
    vm: StartupGateViewModel = hiltViewModel()
) {
    val s by vm.state

    LaunchedEffect(Unit) {
        vm.start()
    }

    LaunchedEffect(s.error) {
        s.error?.let { message ->
            AppFloatingMessage.error(message)
        }
    }

    LaunchedEffect(s.goLogin) {
        if (s.goLogin) {
            onGoLogin()
        }
    }

    LaunchedEffect(s.goMainMenu) {
        if (s.goMainMenu) {
            onGoMainMenu()
        }
    }

    LaunchedEffect(s.goSettings) {
        if (s.goSettings) {
            onGoSettings()
        }
    }

    ResponsiveScreen {
        StartupGateContent(
            loading = s.loading,
            message = s.message,
            error = s.error,
            canContinueOffline = s.canContinueOffline,
            onRetry = {
                vm.retry()
            },
            onContinueOffline = {
                vm.continueOffline()
            }
        )

        if (s.waitingForUpdate) {
            StartupUpdateDialog(
                mandatory = s.updateMandatory,
                apkFileName = s.updateFileName,
                apkUrl = s.updateUrl,
                onOptionalDismissed = {
                    vm.continueAfterOptionalUpdateDismissed()
                },
                onUpdateStarted = {
                    vm.onUpdateStarted()
                }
            )
        }
    }
}

@Composable
private fun StartupGateContent(
    loading: Boolean,
    message: String,
    error: String?,
    canContinueOffline: Boolean,
    onRetry: () -> Unit,
    onContinueOffline: () -> Unit
) {
    ResponsiveCenteredContent(
        maxWidth = Dimens.contentWidth,
        horizontalPadding = Dimens.space28,
        modifier = Modifier,
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.logo_diprotec
            ),
            contentDescription = "Diprotec",
            modifier = Modifier.size(Dimens.loginCompactHeaderHeight)
        )

        Spacer(modifier = Modifier.height(Dimens.space24))

        if (loading) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(Dimens.space16))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.space10))

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.space12))

                if (canContinueOffline) {
                    AppActionButton(
                        text = "Continuar offline",
                        onClick = onContinueOffline,
                        style = AppButtonStyle.OUTLINE
                    )

                    Spacer(modifier = Modifier.height(Dimens.space4))
                }

                AppActionButton(
                    text = "Reintentar",
                    onClick = onRetry
                )
            }
        }
    }
}
