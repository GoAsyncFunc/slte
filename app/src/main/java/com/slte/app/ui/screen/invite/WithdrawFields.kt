package com.slte.app.ui.screen.invite

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.slte.app.R
import com.slte.app.ui.component.SlteInput
import com.slte.app.ui.component.SlteInputSize
import com.slte.app.ui.component.SlteOptionMenu
import com.slte.app.ui.component.SlteOptionMenuItem
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

@Composable
internal fun WithdrawMethodField(
    methods: List<String>,
    selected: String,
    isLoading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorBounds by remember { mutableStateOf<Rect?>(null) }

    val enabled = !isLoading && (failed || methods.isNotEmpty())
    val hint =
        when {
            isLoading -> stringResource(R.string.invite_withdraw_methods_loading)
            failed -> stringResource(R.string.invite_withdraw_methods_failed)
            methods.isEmpty() -> stringResource(R.string.invite_withdraw_methods_empty)
            else -> stringResource(R.string.invite_withdraw_method_hint)
        }
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (failed) onRetry() else expanded = true
            },
            enabled = enabled,
            modifier =
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { anchorBounds = it.boundsInWindow() },
            shape = SlteShapes.large,
            // 下拉型字段与弹层内其它字段同一种灰底，不再描边
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.size.button)
                    .padding(horizontal = Dimens.gap.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = SlteIcons.WithdrawMethod,
                    contentDescription = stringResource(R.string.invite_withdraw_method),
                    modifier = Modifier.size(Dimens.icon.md),
                    tint = SlteColors.current.accentInteractive,
                )
                Spacer(modifier = Modifier.width(Dimens.gap.sm))
                Text(
                    text = selected.ifBlank { hint },
                    style = SlteType.body,
                    color =
                    if (selected.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) SlteIcons.ExpandLess else SlteIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.icon.md),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 与语言切换同款的小弹窗：独立窗口，所以放在底部弹层里也不会被裁切，
        // 也不会出现"白底弹窗贴白底弹层"看不清的问题（有遮罩分层）
        SlteOptionMenu(
            expanded = expanded,
            anchorBounds = anchorBounds,
            onDismissRequest = { expanded = false },
        ) {
            methods.forEach { method ->
                val isSelected = method == selected
                SlteOptionMenuItem(
                    label = method,
                    selected = isSelected,
                    onClick = {
                        onSelect(method)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun ReadOnlyAmountField(cents: Int) {
    SlteInput(
        value = FormatUtils.balance(cents),
        onValueChange = {},
        placeholder = "",
        icon = SlteIcons.Balance,
        iconDesc = stringResource(R.string.invite_transfer_available),
        readOnly = true,
        enabled = false,
        size = SlteInputSize.Compact,
        onSheet = true,
    )
}
