package com.slte.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 把"错误/空态"这类非列表内容放进一个可下拉刷新的滚动容器里。
 *
 * 直接放 Box 的话下拉刷新收不到手势（它需要一个可滚动的子节点），
 * 而整页塞一个 LazyColumn 只为居中一段提示又太重——这里统一这个折中写法。
 */
@Composable
fun PullRefreshScrollable(content: @Composable () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
