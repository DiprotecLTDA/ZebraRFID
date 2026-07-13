package com.diprotec.inventariozebratc27.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.core.validator.RutValidator
import com.diprotec.inventariozebratc27.ui.common.AppFloatingMessage
import com.diprotec.inventariozebratc27.ui.theme.LoginDesignScreen

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoSettings: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val state by vm.state

    val pickKeyFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            vm.onKeyFileSelected(it.toString())
        }
    }

    LaunchedEffect(Unit) {
        vm.warmUp()
    }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            AppFloatingMessage.error(message)
            vm.clearError()
        }
    }

    LaunchedEffect(state.info) {
        state.info?.let { message ->
            AppFloatingMessage.info(message)
            vm.clearInfo()
        }
    }

    LaunchedEffect(state.goToSettings) {
        if (state.goToSettings) {
            vm.clearGoToSettings()
            onGoSettings()
        }
    }

    val rutOk = state.username.isBlank() ||
            RutValidator.validateAndNormalize(state.username) != null

    LoginDesignScreen(
        state = state,
        rutOk = rutOk,
        onUserChange = { value ->
            vm.onUserChange(value)
        },
        onPassChange = { value ->
            vm.onPassChange(value)
        },
        onLoginClick = {
            vm.onLoginClick(
                onLoggedIn = onLoggedIn
            )
        },
        onSyncClick = {
            vm.onSyncClick()
        },
        onSettingsClick = {
            onGoSettings()
        },
        onUserFocusLost = {
            vm.onUserFocusLost()
        },
        onPickFileClick = {
            pickKeyFileLauncher.launch(
                arrayOf(
                    "application/json",
                    "text/plain",
                    "*/*"
                )
            )
        }
    )
}