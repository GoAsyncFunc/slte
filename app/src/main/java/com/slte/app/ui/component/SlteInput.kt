package com.slte.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

enum class SlteInputSize { Hero, Compact }

@Composable
fun SlteInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconDesc: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    /** 在 [trailing] 前加一条竖分割线：用于"输入 + 后缀选择"这种合体字段。 */
    trailingDivider: Boolean = false,
    bordered: Boolean = true,
    size: SlteInputSize = SlteInputSize.Hero,
    onSheet: Boolean = false,
) {
    val compact = size == SlteInputSize.Compact
    val fieldHeight = if (compact) Dimens.size.button else Dimens.size.row
    val fieldText = if (compact) SlteType.body else SlteType.field
    val fieldIcon = if (compact) Dimens.icon.md else Dimens.icon.lg
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        // 弹层是白底，字段用灰底；页面是灰底，字段一律白底（bordered 只控制要不要描边）
        color =
        if (onSheet) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        border =
        when {
            focused -> BorderStroke(Dimens.strokeMedium, SlteColors.current.accentInteractive)
            !bordered || onSheet -> null
            else -> BorderStroke(Dimens.dividerThickness, MaterialTheme.colorScheme.outline)
        },
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .padding(horizontal = Dimens.gap.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = iconDesc,
                    modifier = Modifier.size(fieldIcon),
                    tint = SlteColors.current.accentInteractive,
                )
                Spacer(modifier = Modifier.width(Dimens.gap.sm))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                interactionSource = interactionSource,
                enabled = enabled,
                readOnly = readOnly,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                visualTransformation = visualTransformation,
                textStyle = fieldText.copy(color = MaterialTheme.colorScheme.onSurface),

                cursorBrush = SolidColor(SlteColors.current.accentInteractive),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = fieldText.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
            )
            trailing?.let {
                if (trailingDivider) {
                    VerticalDivider(
                        modifier =
                        Modifier
                            .padding(horizontal = Dimens.gap.sm)
                            .height(Dimens.icon.lg),
                        thickness = Dimens.dividerThickness,
                        // outlineVariant 在夜间模式下与字段深色底几乎同色，看不见；
                        // outline 与字段描边同色，两种模式下都有足够对比度
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                it()
            }
        }
    }
}
