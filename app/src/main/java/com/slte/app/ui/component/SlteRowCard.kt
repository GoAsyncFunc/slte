package com.slte.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteMotion
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

/**
 * 行内容（不带卡片底）：图标 + 标题/副标题 + 右侧值 + 尾部内容 + 箭头。
 *
 * 单行成卡时用 [SlteRowCard]；一行属于某个分组时直接放进 [SlteGroup] 里，
 * 行与行之间用 [SlteGroupDivider]。
 */
@Composable
fun SlteRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val sink = rememberSinkScale(interactionSource, enabled)

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = sink.value
                scaleY = sink.value
            }.then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .heightIn(min = Dimens.size.row)
            .padding(horizontal = Dimens.gap.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.icon.lg),
            tint = iconTint,
        )
        Spacer(modifier = Modifier.width(Dimens.gap.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SlteType.title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Dimens.gap.xs))
                Text(
                    text = subtitle,
                    style = SlteType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = SlteType.title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
        if (chevron) {
            Spacer(modifier = Modifier.width(Dimens.gap.xs))
            Icon(
                imageVector = SlteIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Dimens.icon.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 单行卡片：[SlteRow] 外面套一张卡片。可点击时带 HyperOS 的按压回馈（整行缩到 0.94 再弹回），
 * 不显示水波纹——回馈由缩放承担。
 */
@Composable
fun SlteRowCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    if (onClick == null) {
        SlteCard(modifier = modifier.fillMaxWidth()) {
            SlteRow(
                icon = icon,
                title = title,
                subtitle = subtitle,
                value = value,
                iconTint = iconTint,
                chevron = chevron,
                trailing = trailing,
            )
        }
    } else {
        val haptic = LocalHapticFeedback.current
        val interactionSource = remember { MutableInteractionSource() }
        val sink = rememberSinkScale(interactionSource)
        SlteCard(
            modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = sink.value
                    scaleY = sink.value
                }.clickable(interactionSource = interactionSource, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
        ) {
            SlteRow(
                icon = icon,
                title = title,
                subtitle = subtitle,
                value = value,
                iconTint = iconTint,
                chevron = chevron,
                trailing = trailing,
            )
        }
    }
}

/** 按压回馈用的缩放值：按下 → 0.94，松开 → 1，用快弹簧回弹。 */
@Composable
internal fun rememberSinkScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Animatable<Float, *> {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed, enabled) {
        scale.animateTo(
            targetValue = if (enabled && pressed) SlteMotion.pressSinkScale else 1f,
            animationSpec = SlteMotion.pressSink,
        )
    }
    return scale
}
