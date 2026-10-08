package com.slte.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.slte.app.R

/**
 * 开关行：整行可点（点哪都切换），带 HyperOS 的按压回馈；开关本身不接收点击，
 * 只跟着行的交互源做滑块反馈。
 *
 * 无障碍上整行合并成一个 Switch 节点（角色 + 开关状态），不会重复朗读。
 */
@Composable
fun SlteSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val stateDesc = if (checked) stringResource(R.string.switch_state_on) else stringResource(R.string.switch_state_off)
    val interactionSource = remember { MutableInteractionSource() }
    val sink = rememberSinkScale(interactionSource, enabled)

    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = sink.value
                scaleY = sink.value
            }.semantics(mergeDescendants = true) {
                role = Role.Switch
                stateDescription = stateDesc
            }.clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            },
    ) {
        SlteRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            trailing = {
                SlteSwitch(
                    checked = checked,
                    enabled = enabled,
                    interactionSource = interactionSource,
                )
            },
        )
    }
}
