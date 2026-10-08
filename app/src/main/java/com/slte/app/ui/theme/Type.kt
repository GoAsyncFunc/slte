package com.slte.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 应用字号的唯一来源：SlteType（命名字号）与 M3 Typography（槽位）都从这里取值，
// 调整字号只改一处，两个入口保证一致。
private fun style(
    weight: FontWeight,
    fontSize: Int,
    lineHeight: Int,
    letterSpacing: Double,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

object SlteType {
    val caption = style(FontWeight.Medium, 11, 16, 0.5)
    val label = style(FontWeight.Normal, 12, 16, 0.4)
    val bodySmall = style(FontWeight.Normal, 13, 18, 0.3)
    val body = style(FontWeight.Normal, 14, 20, 0.25)
    val field = style(FontWeight.Normal, 16, 24, 0.5)
    val title = style(FontWeight.Medium, 16, 24, 0.5)
    val heading = style(FontWeight.SemiBold, 18, 24, 0.15)
    val display = style(FontWeight.Bold, 24, 32, 0.0)
    val pageTitle = style(FontWeight.SemiBold, 28, 36, 0.0)
}

object TextSizes {
    val flagFontSize = 12.sp
    val htmlBaseFontSize = 14.sp
}

// M3 槽位尽量引用命名字号；没有对应命名字号的槽位（大标题档）单独给出，不与 SlteType 重复。
val SlteTypography =
    Typography(
        displayLarge = style(FontWeight.Normal, 57, 64, -0.25),
        headlineLarge = style(FontWeight.Medium, 32, 40, 0.0),
        headlineMedium = SlteType.pageTitle,
        headlineSmall = SlteType.display,
        titleLarge = style(FontWeight.Medium, 22, 28, 0.0),
        titleMedium = SlteType.title,
        titleSmall = SlteType.title,
        bodyLarge = SlteType.field,
        bodyMedium = SlteType.body,
        bodySmall = SlteType.bodySmall,
        labelLarge = style(FontWeight.Medium, 14, 20, 0.1),
        labelMedium = SlteType.label,
        labelSmall = SlteType.caption,
    )
