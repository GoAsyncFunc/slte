package com.slte.app.utils

import androidx.compose.ui.unit.dp

object Dimens {

    object gap {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
    }

    object icon {
        val sm = 16.dp
        val md = 20.dp
        val lg = 24.dp
    }

    val iconBadgeSize = 34.dp
    val iconBadgeRadius = 10.dp

    object size {
        val button = 48.dp
        val buttonMd = 40.dp
        val row = 56.dp
        val touchTarget = 48.dp
    }

    val logoSize = 96.dp

    val periodGridItemPaddingV = 20.dp

    val paymentMethodCellHeight = 48.dp

    val strokeMedium = 1.5.dp
    val strokeThick = 2.dp
    val dividerThickness = 1.dp

    val maxContentWidth = 480.dp

    val loadingBoxSize = 76.dp
    val loadingAnimSize = 32.dp
    val loadingTextGap = 5.dp
    val loadingBoxElevation = 8.dp

    val stateStickerSize = 80.dp

    // 开关（HyperOS 规格）：轨道 49×28dp、滑块 20dp，位移 4→25dp，按住时放大到 1.127
    val switchTrackWidth = 49.dp
    val switchTrackHeight = 28.dp
    val switchTouchHeight = 48.dp
    val switchThumbSize = 20.dp
    val switchThumbOffOffset = 4.dp
    val switchThumbOnOffset = 25.dp
    val switchThumbPressedScale = 1.127f

    val flagSize = 40.dp

    val flagCornerRadius = 4.dp

    val sendCodeButtonWidth = 110.dp

    val topBarActionBgSize = 36.dp

    val cardElevation = 0.dp

    val planStatusPaddingV = 4.dp

    // 套餐状态胶囊：RoundedCornerShape 的 percent 参数（50% = 两端全圆）
    val planStatusChipCornerPercent = 50

    // 透明度类令牌已迁至 ui/theme/SlteAlpha，动效时长在 ui/theme/SlteMotion

    val dashboardScreenPaddingH = gap.lg
    val dashboardScreenPaddingV = 10.dp
    val dashboardCardSpacing = 10.dp

    val dashboardCompactBreakpoint = 840.dp
    val dashboardScreenPaddingVCompact = 6.dp
    val dashboardCardSpacingCompact = 6.dp
    val dashboardUsageBarHeight = 6.dp
    val dashboardUsageBarRadius = 3.dp
    val dashboardListValueMaxWidth = 180.dp
    val dashboardChevronGap = 2.dp
    val dashboardToggleWidth = 100.dp
    val dashboardToggleHeight = 52.dp

    val dashboardToggleCardMinHeight = 240.dp

    val dashboardToggleCardMinHeightCompact = 170.dp
    val dashboardToggleThumbSize = 44.dp
    val dashboardToggleThumbOffset = 52.dp
    val dashboardToggleThumbPadding = 4.dp
    val dashboardToggleGap = 10.dp
    val dashboardActionBtnHeight = size.button

    // 与首页卡片的内边距保持一致（12dp），保证页面节奏统一
    val inviteStatCardPaddingV = gap.md
    val inviteStickerSize = 100.dp
    val inviteCodeItemHeight = 52.dp
    val inviteCodeItemPaddingH = 16.dp
    val inviteCodeCopyIconSize = 18.dp
    val inviteCodeCopyBtnSize = 48.dp

    val sheetPaddingH = gap.xl
    val sheetPaddingV = gap.sm

    // 小弹窗：宽度按内容自适应并夹在 200~288dp（HyperOS 规格），行高 56dp，不加投影
    // 选项文字都不长（语言、后缀、TUN 堆栈），不用做成大卡片
    val optionMenuMinWidth = 160.dp
    val optionMenuMaxWidth = 220.dp
    val optionMenuMaxHeight = 320.dp
    val optionMenuAnchorGap = 8.dp
    val optionMenuItemMinHeight = 48.dp
    val optionMenuItemPaddingH = gap.lg
    val optionMenuItemCheckSize = 20.dp
    val optionMenuCheckGap = 12.dp
    val optionMenuBlurRadius = 10.dp

    // 分组卡片（HyperOS 风格：一组一张卡片，行间 1.5dp 分隔线，两端内缩 20dp）
    val groupDividerThickness = 1.5.dp
    val groupDividerInset = 20.dp

    // 圆角统一走 SlteShapes（见 ui/theme/Shape.kt），透明度走 ui/theme/SlteAlpha，这里只留几何

    val radioDotSize = 18.dp
    val radioDotInnerSize = 9.dp
    val radioDotGap = 5.dp
    val paymentMethodDotSize = 14.dp

    val noticeTagPaddingH = 8.dp
    val noticeTagSpacing = 6.dp
}
