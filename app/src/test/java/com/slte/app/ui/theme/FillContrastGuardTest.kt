package com.slte.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 填色对比守卫：灰底按钮/字段必须和它所在的底色拉开差距，否则会"融进背景"。
 *
 * 之前用 surfaceVariant 做灰面，而它就是页面底色（#F1F1F3），导致按钮/输入框在页面上消失。
 * 现在灰面统一用 surfaceContainerHigh，这里把差距钉住。
 */
class FillContrastGuardTest {

    private fun diff(a: Color, b: Color): Int {
        fun channel(value: Float) = (value * 255f).toInt()
        return listOf(
            kotlin.math.abs(channel(a.red) - channel(b.red)),
            kotlin.math.abs(channel(a.green) - channel(b.green)),
            kotlin.math.abs(channel(a.blue) - channel(b.blue)),
        ).max()
    }

    @Test
    fun `浅色主题灰面与页面底色有肉眼可辨的差距`() {
        val d = diff(md_light_background, md_light_surfaceContainerHigh)
        assertTrue("灰面 $md_light_surfaceContainerHigh 与页面底色 $md_light_background 只差 $d/255，会看不清", d >= 12)
    }

    @Test
    fun `浅色主题灰面与弹层白底有肉眼可辨的差距`() {
        val d = diff(md_light_surface, md_light_surfaceContainerHigh)
        assertTrue("灰面 $md_light_surfaceContainerHigh 与弹层白底 $md_light_surface 只差 $d/255，会看不清", d >= 12)
    }

    @Test
    fun `深色主题灰面与页面底色有肉眼可辨的差距`() {
        val d = diff(md_dark_background, md_dark_surfaceContainerHigh)
        assertTrue("深色灰面 $md_dark_surfaceContainerHigh 与底色 $md_dark_background 只差 $d/255", d >= 12)
    }
}
