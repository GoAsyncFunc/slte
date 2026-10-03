package com.slte.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteMotion
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 轻量选项弹窗：贴着触发它的那一行弹出的小卡片，只承载「互斥选项」这一种内容。
 *
 * 与底部弹层 [SlteSheet] 的分工：
 * - 选项不超过 5 个、每项一行文字时用小弹窗——不打断页面上下文、少一次转场；
 * - 含输入框、价格明细、长文案的弹层继续用 [SlteSheet]。
 *
 * 动效与几何按 HyperOS 规格实现（参数见 [SlteMotion] 与 Dimens 里的 optionMenu*）：
 * 宽度随内容自适应并夹在 200~288dp；贴在锚点行下方 8dp、右边缘与行对齐，空间不够翻到上方；
 * 进场是从锚点角 0.15 缩放到 1 的弹簧（轻微回弹），内容 200ms 淡入、遮罩 300ms 变暗；
 * 退场 150ms 淡出。卡片本身不加投影，靠遮罩分层。
 *
 * 实现为全屏 Dialog（遮罩）+ 挂在其窗口内的卡片 Popup：popup 窗口会被系统钳在
 * "可见显示区"（不含状态栏/导航栏）里，遮罩怎么画都盖不住系统栏；Dialog 窗口可以
 * 边缘到边缘，遮罩才能真正铺满整屏。锚点坐标请传 [androidx.compose.ui.layout.boundsInWindow]。
 */
@Composable
fun SlteOptionMenu(
    expanded: Boolean,
    anchorBounds: Rect?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val anchor = anchorBounds
    val fraction = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val dim = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(expanded, anchor) {
        if (expanded && anchor != null && anchor.width > 0f) {
            visible = true
            coroutineScope {
                launch { fraction.animateTo(1f, SlteMotion.popupFraction) }
                launch { contentAlpha.animateTo(1f, SlteMotion.popupAlphaEnter) }
                launch { dim.animateTo(1f, SlteMotion.popupDimEnter) }
            }
        } else {
            coroutineScope {
                launch { contentAlpha.animateTo(0f, SlteMotion.popupAlphaExit) }
                launch { dim.animateTo(0f, SlteMotion.popupDimExit) }
                launch { fraction.animateTo(0f, tween(durationMillis = 150)) }
            }
            visible = false
        }
    }

    val density = LocalDensity.current
    val safeBottom = WindowInsets.safeDrawing.getBottom(density)
    val screenGapPx = with(density) { Dimens.gap.md.roundToPx() }
    val anchorGapPx = with(density) { Dimens.optionMenuAnchorGap.roundToPx() }

    if (!visible || anchor == null || anchor.width <= 0f) return

    // 卡片窗口：compose-ui 1.8 起 PopupPositionProvider 的入参与返回值均为窗口相对坐标，
    // 返回值直接用作弹窗窗口的 x/y。anchor 用 boundsInWindow() 记录，本来就是应用窗口坐标；
    // 遮罩 Dialog 与应用窗口都是边缘到边缘的全屏窗口，两套坐标一一对应，卡片挂进
    // Dialog 窗口后按这套坐标定位即可落在触发行旁边。
    val cardPositionProvider =
        remember(anchor, anchorGapPx, screenGapPx, safeBottom) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    parentBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    val maxX =
                        (windowSize.width - screenGapPx - popupContentSize.width).coerceAtLeast(screenGapPx)
                    val x = (anchor.right.roundToInt() - popupContentSize.width).coerceIn(screenGapPx, maxX)
                    val below = anchor.bottom.roundToInt() + anchorGapPx
                    val bottomLimit = windowSize.height - safeBottom - screenGapPx
                    val y =
                        if (below + popupContentSize.height <= bottomLimit) {
                            below
                        } else {
                            (anchor.top.roundToInt() - anchorGapPx - popupContentSize.height)
                                .coerceAtLeast(screenGapPx)
                        }
                    return IntOffset(x, y)
                }
            }
        }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
        DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        // 关掉对话框自带的背景压暗：明暗只由遮罩层的动画控制，避免与系统暗层叠加过黑
        val dialogView = LocalView.current
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        }

        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = Dimens.optionMenuScrimAlpha * dim.value))
                .pointerInput(Unit) { detectTapGestures { onDismissRequest() } },
        ) {
            Popup(
                popupPositionProvider = cardPositionProvider,
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true, usePlatformDefaultWidth = false),
            ) {
                Surface(
                    modifier =
                    modifier
                        .widthIn(
                            min = Dimens.optionMenuMinWidth,
                            max = Dimens.optionMenuMaxWidth,
                        )
                        .heightIn(max = Dimens.optionMenuMaxHeight)
                        .graphicsLayer {
                            val scale = SlteMotion.popupMinScale + (1f - SlteMotion.popupMinScale) * fraction.value
                            scaleX = scale
                            scaleY = scale
                            alpha = contentAlpha.value
                            // 卡片右边缘与触发控件对齐，所以从右上角展开
                            transformOrigin = TransformOrigin(1f, 0f)
                        },
                    shape = SlteShapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(vertical = Dimens.gap.xs), content = content)
                }
            }
        }
    }
}

/**
 * 小弹窗里的单个选项：一行标题，选中项用主题色高亮并显示对勾。
 *
 * 行高与内边距按 HyperOS 规格：最小 56dp、左右 20dp。
 */
@Composable
fun SlteOptionMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.optionMenuItemMinHeight)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            ).padding(horizontal = Dimens.optionMenuItemPaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = SlteType.title,
                color =
                if (selected) {
                    SlteColors.current.accentInteractive
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Spacer(modifier = Modifier.width(Dimens.optionMenuCheckGap))
            Icon(
                imageVector = SlteIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(Dimens.optionMenuItemCheckSize),
                tint = SlteColors.current.accentInteractive,
            )
        }
    }
}
