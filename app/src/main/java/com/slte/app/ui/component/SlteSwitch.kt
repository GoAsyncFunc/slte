package com.slte.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.slte.app.R
import com.slte.app.ui.theme.SlteColors
import com.slte.app.utils.Dimens

/**
 * 开关（HyperOS 规格）：轨道 49×28dp、滑块 20dp，位移 4→25dp 用回弹弹簧，
 * 按住时滑块放大到 1.127，开关色切换用慢一档的弹簧。
 *
 * 开启态是品牌蓝轨道 + 白色滑块；关闭态是浅灰轨道 + 白色滑块。
 * 触控高度固定 48dp（满足无障碍最小触控区），视觉轨道仍保持 28dp。
 *
 * @param interactionSource 传入所在行的交互源时，按住整行也会同步放大滑块；
 *   不传则开关自己接收点击（[onCheckedChange] 不为空时）。
 */
@Composable
fun SlteSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val ownSource = remember { MutableInteractionSource() }
    val source = interactionSource ?: ownSource
    val pressed by source.collectIsPressedAsState()
    val stateDesc = if (checked) stringResource(R.string.switch_state_on) else stringResource(R.string.switch_state_off)

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) Dimens.switchThumbOnOffset else Dimens.switchThumbOffOffset,
        animationSpec = remember { spring<Dp>(dampingRatio = 0.7f, stiffness = 987f) },
        label = "switch_thumb_offset",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (enabled && pressed) Dimens.switchThumbPressedScale else 1f,
        animationSpec = remember { spring<Float>(dampingRatio = 0.6f, stiffness = 987f) },
        label = "switch_thumb_scale",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) SlteColors.current.accentInteractive else scheme.outlineVariant,
        animationSpec = remember { spring<Color>(dampingRatio = 0.99f, stiffness = 438.6f) },
        label = "switch_track_color",
    )

    val interactiveModifier =
        if (onCheckedChange == null) {
            // 开关状态由所在行负责朗读，这里对无障碍隐藏，避免重复
            Modifier.clearAndSetSemantics {}
        } else {
            Modifier
                .semantics {
                    role = Role.Switch
                    stateDescription = stateDesc
                }.toggleable(value = checked, enabled = enabled, role = Role.Switch) { value ->
                    haptic.performHapticFeedback(
                        if (value) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                    )
                    onCheckedChange(value)
                }
        }

    Box(
        modifier =
        modifier
            .size(width = Dimens.switchTrackWidth, height = Dimens.switchTouchHeight)
            .then(interactiveModifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
            Modifier
                .size(width = Dimens.switchTrackWidth, height = Dimens.switchTrackHeight)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        trackColor
                    } else {
                        trackColor.copy(alpha = Dimens.disabledAlpha)
                    },
                ),
        ) {
            Box(
                modifier =
                Modifier
                    .offset(x = thumbOffset)
                    .size(Dimens.switchThumbSize)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        scaleX = thumbScale
                        scaleY = thumbScale
                    }.clip(CircleShape)
                    .background(
                        if (enabled) {
                            scheme.surface
                        } else {
                            scheme.surface.copy(alpha = Dimens.disabledAlpha)
                        },
                    ),
            )
        }
    }
}
