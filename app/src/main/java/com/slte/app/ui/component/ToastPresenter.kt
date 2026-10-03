package com.slte.app.ui.component

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 全应用唯一的提示气泡出口：当前实现是平台 Toast 的薄封装，
 * 统一入口的意义在于后续换成应用内自绘气泡时只改这一个类。
 */
class ToastHandle
    internal constructor(
        private val context: Context,
    ) {
    fun show(
        message: String,
        centered: Boolean = false,
    ) = display(message, long = false, centered = centered)

    fun showLong(
        message: String,
        centered: Boolean = false,
    ) = display(message, long = true, centered = centered)

    fun show(
        @StringRes messageRes: Int,
        centered: Boolean = false,
    ) = show(context.getString(messageRes), centered)

    fun showLong(
        @StringRes messageRes: Int,
        centered: Boolean = false,
    ) = showLong(context.getString(messageRes), centered)

    private fun display(
        message: String,
        long: Boolean,
        centered: Boolean,
    ) {
        val toast =
            Toast.makeText(
                context,
                message,
                if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            )
        if (centered) {
            toast.setGravity(Gravity.CENTER, 0, 0)
        }
        toast.show()
    }
}

@Composable
fun rememberToast(): ToastHandle {
    val context = LocalContext.current
    return remember(context) { ToastHandle(context) }
}
