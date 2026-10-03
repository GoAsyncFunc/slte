package com.slte.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SlteShapes =
    Shapes(
        // 圆角梯度（HyperOS 规格）：标签 4 / 小元素 8 / 紧凑块 12 / 卡片·按钮·输入框·内嵌块 16 / 弹层 20
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )
