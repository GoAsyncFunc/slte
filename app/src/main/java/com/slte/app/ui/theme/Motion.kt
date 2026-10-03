package com.slte.app.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.PI
import kotlin.math.sin

/**
 * 全应用统一的动效参数。
 *
 * 这组数值对齐 HyperOS（参考 compose-miuix-ui 公开的动画规格），但由我们自己实现，
 * 不引入第三方依赖：改这里就能整体调整"手感"，各组件不要各写一套弹簧。
 */
object SlteMotion {

    /** HyperOS 的变暗/淡入曲线：sin(f × π/2)，起步快、收尾稳。 */
    val sinOut: Easing = Easing { fraction -> sin((fraction * PI / 2).toFloat()) }

    /** 弹窗缩放用的弹簧：轻微回弹（阻尼比 0.82）。 */
    val popupFraction: SpringSpec<Float> =
        spring(
            dampingRatio = 0.82f,
            stiffness = 362.5f,
            visibilityThreshold = 0.0001f,
        )

    /** 按压回馈（Sink）用的弹簧：更快、几乎不回弹。 */
    val pressSink: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 600f)

    val popupAlphaEnter: TweenSpec<Float> = tween(durationMillis = 200)
    val popupAlphaExit: TweenSpec<Float> = tween(durationMillis = 150)
    val popupDimEnter: TweenSpec<Float> = tween(durationMillis = 300, easing = sinOut)
    val popupDimExit: TweenSpec<Float> = tween(durationMillis = 150, easing = sinOut)

    /** 弹窗起手缩放：从锚点角 0.15 长到 1.0（HyperOS 的"弹出"观感）。 */
    const val popupMinScale = 0.15f

    /** 按压时缩到的最小比例。 */
    const val pressSinkScale = 0.94f
}
