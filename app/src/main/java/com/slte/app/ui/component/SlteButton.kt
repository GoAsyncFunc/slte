package com.slte.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slte.app.ui.theme.SlteAlpha
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

enum class SlteButtonStyle {

    Primary,

    Secondary,

    Neutral,

    Tonal,

    Medium,

    Danger,
}

/**
 * 按钮（HyperOS 规格）：16dp 圆角；全局禁用水波纹（SlteTheme 关闭 ripple），按压反馈为触觉振动。
 */
@Composable
fun SlteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SlteButtonStyle = SlteButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
    height: Dp? = null,
) {
    val haptic = LocalHapticFeedback.current
    val presetHeight =
        when (style) {
            SlteButtonStyle.Medium -> Dimens.size.buttonMd
            SlteButtonStyle.Tonal -> Dimens.size.row
            else -> Dimens.size.button
        }
    val effHeight = height ?: presetHeight
    val effContainer =
        containerColor ?: when (style) {
            // 保持项目原有配色：次级/中性为透明底 + 描边，行内按钮为白底
            SlteButtonStyle.Secondary, SlteButtonStyle.Neutral -> Color.Transparent
            SlteButtonStyle.Tonal -> MaterialTheme.colorScheme.surface
            SlteButtonStyle.Danger -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
    val effContent =
        contentColor ?: when (style) {
            SlteButtonStyle.Secondary, SlteButtonStyle.Tonal -> MaterialTheme.colorScheme.primary
            SlteButtonStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            SlteButtonStyle.Danger -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        }
    val effBorder =
        when (style) {
            SlteButtonStyle.Secondary ->
                BorderStroke(Dimens.strokeMedium, MaterialTheme.colorScheme.primary)
            SlteButtonStyle.Neutral ->
                BorderStroke(Dimens.dividerThickness, MaterialTheme.colorScheme.outline)
            else -> null
        }
    val clickable = enabled && !loading
    val textStyle =
        if (style == SlteButtonStyle.Medium) {
            SlteType.body.copy(fontWeight = FontWeight.Medium)
        } else {
            SlteType.field
        }
    val contentPadding =
        if (style == SlteButtonStyle.Medium) {
            PaddingValues(horizontal = Dimens.gap.lg)
        } else {
            ButtonDefaults.ContentPadding
        }

    Surface(
        modifier =
        modifier
            .height(effHeight)
            .alpha(if (enabled) 1f else SlteAlpha.disabled)
            .semantics(mergeDescendants = true) {
                role = Role.Button
            }.clickable(
                enabled = clickable,
                role = Role.Button,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = SlteShapes.large,
        color = effContainer,
        contentColor = effContent,
        border = effBorder,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier =
            Modifier
                // 只撑满高度、宽度随内容：调用方要整行按钮时自己加 fillMaxWidth
                .fillMaxHeight()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                LottieLoadingIcon(modifier = Modifier.size(Dimens.icon.lg))
            } else {
                Text(text = text, style = textStyle)
            }
        }
    }
}
