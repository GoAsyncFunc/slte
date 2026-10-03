package com.slte.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slte.app.ui.theme.SlteAlpha
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.utils.Dimens

/**
 * 分组卡片：把同一类设置/信息装进一张卡片，行与行之间用细分隔线（HyperOS 的分组方式）。
 *
 * 与 [SlteCard] 的分工：一张卡片一个内容块时用 [SlteCard]；
 * 一组同类条目（设置项、信息行）用 [SlteGroup] + [SlteRow] + [SlteGroupDivider]。
 */
@Composable
fun SlteGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

/** 分组内的行分隔线：两端各内缩 20dp，厚度 1.5dp。 */
@Composable
fun SlteGroupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = Dimens.groupDividerInset),
        thickness = Dimens.groupDividerThickness,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SlteAlpha.divider),
    )
}
