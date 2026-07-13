package com.diprotec.inventariozebratc27.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextOverflow
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.theme.LabelGray
import com.diprotec.inventariozebratc27.ui.theme.StatusError
import com.diprotec.inventariozebratc27.ui.theme.StatusOnline
import com.diprotec.inventariozebratc27.ui.theme.StatusWarning
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary

enum class AppButtonStyle {
    PRIMARY,
    SECONDARY,
    OUTLINE
}

@Composable
fun AppActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    style: AppButtonStyle = AppButtonStyle.PRIMARY,
    icon: ImageVector? = null
) {
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.iconMedium),
                strokeWidth = Dimens.borderWidthStrong,
                color = if (style == AppButtonStyle.OUTLINE) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
            )
        } else {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconStandard)
                )

                Spacer(modifier = Modifier.width(Dimens.space8))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    val buttonModifier = modifier.heightIn(min = Dimens.buttonHeight)
    val contentPadding = PaddingValues(
        horizontal = Dimens.space16,
        vertical = Dimens.space8
    )

    when (style) {
        AppButtonStyle.PRIMARY -> Button(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.large,
            contentPadding = contentPadding,
            content = content
        )

        AppButtonStyle.SECONDARY -> Button(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            contentPadding = contentPadding,
            content = content
        )

        AppButtonStyle.OUTLINE -> OutlinedButton(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.large,
            contentPadding = contentPadding,
            content = content
        )
    }
}

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .height(Dimens.topBarHeight)
            .padding(horizontal = Dimens.screenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null && onNavigationClick != null) {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = navigationContentDescription,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimens.iconStandard)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.space8))
        }

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        actions()
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(Dimens.space16),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Dimens.elevationLow
        )
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    supportingText: (@Composable (() -> Unit))? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        readOnly = readOnly,
        isError = isError,
        label = label?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        shape = MaterialTheme.shapes.medium,
        colors = appTextFieldColors()
    )
}

@Composable
fun AppTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    supportingText: (@Composable (() -> Unit))? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        readOnly = readOnly,
        isError = isError,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        shape = MaterialTheme.shapes.medium,
        colors = appTextFieldColors()
    )
}

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = BorderGray,
    disabledBorderColor = BorderGray,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = LabelGray,
    disabledLabelColor = LabelGray,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleMedium
    )
}

enum class AppStatus {
    ONLINE,
    WARNING,
    ERROR
}

@Composable
fun StatusDot(
    status: AppStatus,
    modifier: Modifier = Modifier
) {
    val color = statusColor(status)

    Surface(
        modifier = modifier.size(Dimens.iconSmall),
        shape = MaterialTheme.shapes.large,
        color = color,
        content = {}
    )
}

@Composable
fun StatusChip(
    text: String,
    status: AppStatus,
    modifier: Modifier = Modifier
) {
    val color = statusColor(status)

    Surface(
        modifier = modifier.heightIn(min = Dimens.statusHeight),
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.space12,
                vertical = Dimens.space8
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(status = status)

            Spacer(modifier = Modifier.width(Dimens.space8))

            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun statusColor(status: AppStatus): Color = when (status) {
    AppStatus.ONLINE -> StatusOnline
    AppStatus.WARNING -> StatusWarning
    AppStatus.ERROR -> StatusError
}
