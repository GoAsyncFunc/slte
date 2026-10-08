package com.slte.app.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import com.slte.app.R
import com.slte.app.support.RobolectricTestApplication
import com.slte.app.ui.theme.SlteTheme
import com.slte.app.utils.Dimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 首页底部连接卡的对齐约束：卡片补满「上方按钮 → 底部安全区」之间的空间，
 * 下沿始终落在同一内容边距上——不会探进系统手势条，也不会在下方留出大小不一的空白；
 * 屏幕变高时卡片跟着变高，屏幕变矮时退到最小高度并允许滚动，卡片本身不被压扁。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RobolectricTestApplication::class)
class DashboardConnectCardLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val rootTag = "dashboard_root"

    private fun render() {
        composeRule.setContent {
            SlteTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize().testTag(rootTag)) {
                    DashboardContent(
                        data =
                        DashboardData(
                            usedBytes = 8L * 1024 * 1024 * 1024,
                            totalBytes = 150L * 1024 * 1024 * 1024,
                            isValid = true,
                            hasPlan = true,
                            planName = "入门套餐",
                            daysUntilExpired = 753,
                            serverName = "香港01",
                            proxyMode = "规则",
                            currentIp = "240a:42c8:880::b11c2",
                            ipCountryCode = "HK",
                        ),
                        onToggleConnection = {},
                        onServerClick = {},
                        onProxyClick = {},
                        onUpdateSubscription = {},
                        onInvite = {},
                        onRenew = {},
                    )
                }
            }
        }
    }

    private fun px(value: Dp): Float = with(composeRule.density) { value.toPx() }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun statusText() = RuntimeEnvironment.getApplication().getString(R.string.status_disconnected)

    /**
     * @param compact 页面自带的断点：可视高度不足时使用紧凑间距与更小的卡片最小高度
     */
    private fun assertCardAnchoredToBottomSafeArea(compact: Boolean) {
        render()

        val root = bounds(rootTag)
        val card = bounds(connectToggleCardTag)
        val bottomPadding =
            if (compact) Dimens.dashboardScreenPaddingVCompact else Dimens.dashboardScreenPaddingV
        val minHeight =
            if (compact) Dimens.dashboardToggleCardMinHeightCompact else Dimens.dashboardToggleCardMinHeight

        assertEquals(
            "卡片下沿与屏幕底部的间距应等于内容边距：不得越过底部安全区，也不得留出多余空白",
            root.bottom - px(bottomPadding),
            card.bottom,
            1.5f,
        )
        assertTrue(
            "卡片补满剩余空间后仍不得低于最小高度 $minHeight",
            card.height >= px(minHeight) - 1.5f,
        )
        assertTrue("卡片整体应落在可视区域内", card.top < root.bottom - px(bottomPadding))
        assertTrue("卡片应贴着上方内容块，不能整块悬空", card.top > root.top + px(bottomPadding))

        // 开关与状态文字整体居中于卡片：卡片被撑高时不贴顶，缩到最小高度时不被裁切。
        val toggle = composeRule.onAllNodes(isToggleable()).onFirst().fetchSemanticsNode().boundsInRoot
        val status = composeRule.onNodeWithText(statusText()).fetchSemanticsNode().boundsInRoot
        assertEquals("开关与状态文字应整体垂直居中于卡片", card.center.y, (toggle.top + status.bottom) / 2f, 2f)
        assertEquals("开关应水平居中于卡片", card.center.x, toggle.center.x, 2f)
    }

    @Test
    @Config(qualifiers = "zh-rCN-w411dp-h1000dp-420dpi")
    fun `大屏卡片补满剩余空间并贴在底部安全区`() {
        assertCardAnchoredToBottomSafeArea(compact = false)

        val card = bounds(connectToggleCardTag)
        assertTrue(
            "大屏上卡片应被撑高（而不是保持最小高度、在下方留出空白）",
            card.height > px(Dimens.dashboardToggleCardMinHeight) + 1f,
        )
    }

    @Test
    @Config(qualifiers = "zh-rCN-w411dp-h700dp-420dpi")
    fun `较矮屏卡片同样贴底且保持最小高度`() {
        assertCardAnchoredToBottomSafeArea(compact = true)
    }

    @Test
    @Config(qualifiers = "zh-rCN-w360dp-h780dp-320dpi")
    fun `小屏窄机型卡片贴底且不被压扁`() {
        assertCardAnchoredToBottomSafeArea(compact = true)
    }
}
