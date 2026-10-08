package com.slte.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.slte.app.ui.theme.SlteColors
import com.slte.app.utils.Dimens

/**
 * 顶栏圆形图标按钮：和按钮/行同一套按压回馈（缩到 0.94），不带水波纹。
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val sink = rememberSinkScale(interactionSource)

    Box(
        modifier =
        modifier
            .size(Dimens.size.touchTarget)
            .graphicsLayer {
                scaleX = sink.value
                scaleY = sink.value
            }.clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (showBackground) {
            Box(
                modifier =
                Modifier
                    .size(Dimens.topBarActionBgSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(Dimens.icon.lg),
                    tint = SlteColors.current.accentInteractive,
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(Dimens.icon.lg),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
