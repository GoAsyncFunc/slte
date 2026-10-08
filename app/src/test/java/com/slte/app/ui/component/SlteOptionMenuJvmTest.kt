package com.slte.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.slte.app.support.RobolectricTestApplication
import com.slte.app.ui.theme.SlteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rCN-w411dp-h891dp-420dpi", application = RobolectricTestApplication::class)
class SlteOptionMenuJvmTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun renderMenu(
        onPick: (String) -> Unit,
        onDismissRequest: () -> Unit = {},
    ) {
        composeRule.setContent {
            SlteTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SlteOptionMenu(
                        expanded = true,
                        anchorBounds = Rect(left = 0f, top = 0f, right = 700f, bottom = 140f),
                        onDismissRequest = onDismissRequest,
                    ) {
                        SlteOptionMenuItem(
                            label = "简体中文",
                            selected = true,
                            onClick = { onPick("简体中文") },
                        )
                        SlteOptionMenuItem(
                            label = "English",
                            selected = false,
                            onClick = { onPick("English") },
                        )
                        SlteOptionMenuItem(
                            label = "System",
                            selected = false,
                            onClick = { onPick("System") },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `点选选项回调对应取值`() {
        var picked: String? = null
        renderMenu(onPick = { picked = it })

        composeRule.onNodeWithText("English").performClick()

        assertEquals("English", picked)
    }

    @Test
    fun `选项超出卡片高度上限后可以滚动到最后一个`() {
        composeRule.setContent {
            SlteTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SlteOptionMenu(
                        expanded = true,
                        anchorBounds = Rect(left = 0f, top = 0f, right = 700f, bottom = 140f),
                        onDismissRequest = {},
                    ) {
                        // 邮箱后缀白名单可能有十几个，超出高度的部分必须能划到
                        repeat(20) { index ->
                            SlteOptionMenuItem(
                                label = "option-$index@example.com",
                                selected = false,
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("option-19@example.com").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `当前生效的选项带选中状态`() {
        renderMenu(onPick = {})

        composeRule.onNodeWithText("简体中文").assertIsSelected()
        composeRule.onNodeWithText("English").assertIsNotSelected()
    }
}
