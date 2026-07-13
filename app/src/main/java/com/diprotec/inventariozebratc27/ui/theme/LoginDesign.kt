package com.diprotec.inventariozebratc27.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.diprotec.inventariozebratc27.R
import com.diprotec.inventariozebratc27.ui.login.LoginUiState

@Composable
fun LoginDesignScreen(
    state: LoginUiState,
    rutOk: Boolean,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUserFocusLost: () -> Unit,
    onPickFileClick: () -> Unit
) {
    var passwordVisible by remember {
        mutableStateOf(false)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        val headerHeight = if (maxHeight >= 720.dp) {
            170.dp
        } else {
            140.dp
        }

        val logoHeight = if (maxHeight >= 720.dp) {
            96.dp
        } else {
            82.dp
        }

        val formMaxWidth = if (maxWidth >= 480.dp) {
            420.dp
        } else {
            maxWidth
        }

        val horizontalPadding = if (maxWidth >= 420.dp) {
            28.dp
        } else {
            24.dp
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LoginHeader(
                logoHeight = logoHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = Background,
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = formMaxWidth)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = horizontalPadding,
                                vertical = 18.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.needsPickKeyFile) {
                            Text(
                                text = "Seleccione el archivo 'inventario.key' para cargar credenciales.",
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Top
                            ) {
                                InventoryMenuButton(
                                    text = "Archivo",
                                    icon = Icons.Default.Key,
                                    enabled = !state.loadingLogin && !state.loadingSync,
                                    onClick = onPickFileClick
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        LoginInputField(
                            value = state.username,
                            onValueChange = { input ->
                                onUserChange(input)
                            },
                            label = "Usuario",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            },
                            isError = state.username.isNotBlank() && !rutOk,
                            supportingText = if (state.username.isNotBlank() && !rutOk) {
                                {
                                    Text(
                                        text = "Ej.: 19120735-1",
                                        color = ErrorRed
                                    )
                                }
                            } else {
                                null
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        onUserFocusLost()
                                    }
                                }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LoginInputField(
                            value = state.password,
                            onValueChange = { input ->
                                onPassChange(
                                    input.filter { char ->
                                        char.isLetterOrDigit()
                                    }
                                )
                            },
                            label = "Contraseña",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        passwordVisible = !passwordVisible
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Ocultar contraseña"
                                        } else {
                                            "Mostrar contraseña"
                                        },
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Top
                            ) {
                                InventoryMenuButton(
                                    text = "Ingresar",
                                    icon = Icons.Default.Login,
                                    enabled = rutOk &&
                                            state.password.isNotBlank() &&
                                            !state.loadingLogin &&
                                            !state.loadingSync,
                                    loading = state.loadingLogin,
                                    onClick = onLoginClick
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Top
                            ) {
                                InventoryMenuButton(
                                    text = "Sincronizar",
                                    icon = Icons.Default.Sync,
                                    loading = state.loadingSync,
                                    enabled = !state.loadingLogin && !state.loadingSync,
                                    onClick = onSyncClick,
                                    modifier = Modifier.weight(1f)
                                )

                                InventoryMenuButton(
                                    text = "Configuración",
                                    icon = Icons.Default.Settings,
                                    enabled = !state.loadingLogin && !state.loadingSync,
                                    onClick = onSettingsClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        state.error?.let { message ->
                            Text(
                                text = message,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        state.info?.let { message ->
                            Text(
                                text = message,
                                color = Success,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginHeader(
    logoHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TealPrimary,
                        TealPrimaryDark
                    )
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(logoHeight)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.diprotec),
                contentDescription = "Logo Diprotec",
                modifier = Modifier.height(logoHeight)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.height(logoHeight),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Inventario",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        maxLines = 1,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        label = {
            Text(
                text = label,
                color = LabelGray
            )
        },
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = TextPrimary
        ),
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TextSecondary,
            unfocusedBorderColor = BorderGray,
            disabledBorderColor = BorderGray,
            errorBorderColor = ErrorRed,
            focusedContainerColor = White,
            unfocusedContainerColor = White,
            disabledContainerColor = White,
            errorContainerColor = White,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextPrimary,
            focusedLeadingIconColor = TextSecondary,
            unfocusedLeadingIconColor = TextSecondary,
            disabledLeadingIconColor = TextSecondary,
            focusedTrailingIconColor = TextSecondary,
            unfocusedTrailingIconColor = TextSecondary,
            disabledTrailingIconColor = TextSecondary,
            focusedLabelColor = LabelGray,
            unfocusedLabelColor = LabelGray,
            cursorColor = TextSecondary
        )
    )
}