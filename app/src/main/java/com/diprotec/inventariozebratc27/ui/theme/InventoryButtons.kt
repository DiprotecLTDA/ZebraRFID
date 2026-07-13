package com.diprotec.inventariozebratc27.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

@Composable
fun InventoryMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String = text,
    minHeight: Dp = Dimens.menuButtonMinHeight,
    circleSize: Dp = Dimens.menuButtonCircleSize,
    iconSize: Dp = Dimens.iconLarge,
    textSpacing: Dp = Dimens.space6,
    maxTextLines: Int = 1,
    containerColor: Color = InventoryButtonBackground,
    disabledContainerColor: Color = InventoryButtonBackground,
    borderColor: Color = ButtonRed,
    disabledBorderColor: Color = BorderGray,
    iconColor: Color = ButtonRed,
    disabledIconColor: Color = LabelGray,
    textColor: Color = TextPrimary,
    disabledTextColor: Color = LabelGray,
    loadingColor: Color = ButtonRed
) {
    Column(
        modifier = modifier.heightIn(min = minHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled && !loading,
            shape = CircleShape,
            color = if (enabled) containerColor else disabledContainerColor,
            modifier = Modifier.size(circleSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = Dimens.borderWidthStrong,
                        color = if (enabled) borderColor else disabledBorderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(iconSize * 0.85f),
                        strokeWidth = Dimens.borderWidthStrong,
                        color = loadingColor
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = if (enabled) iconColor else disabledIconColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(textSpacing))

        Text(
            text = text,
            color = if (enabled) textColor else disabledTextColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = maxTextLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
