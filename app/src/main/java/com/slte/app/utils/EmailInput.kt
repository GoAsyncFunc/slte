package com.slte.app.utils

import com.slte.app.R

/**
 * 邮箱输入的本地校验（注册 / 忘记密码共用）。
 *
 * 白名单启用时输入框只填 `@` 前面的部分，拼出来的是 `@gmail.com` 这种"看着非空"的值，
 * 直接发给后端只会得到"验证码发送失败"——用户根本不知道问题是自己没填邮箱。
 * 所以必填判断一律看 `@` 之前那一截，而不是整串是否为空。
 */
object EmailInput {
    /** 只拦明显不合法的：本地部分非空、恰好一个 `@`、域名带点、无空白。 */
    private val SHAPE = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

    private const val MAX_LENGTH = 254

    /** `@` 之前的部分：白名单模式下它就是用户实际填的内容（粘整串邮箱进来也能取对）。 */
    fun localPart(email: String): String = email.substringBeforeLast('@', email).trim()

    /** 没填邮箱（含白名单下只选了后缀）时返回提示，否则 null。 */
    fun requiredErrorRes(email: String): Int? = if (localPart(email).isBlank()) R.string.error_email_required else null

    /** 明显不是邮箱时返回提示，否则 null。 */
    fun formatErrorRes(email: String): Int? {
        val trimmed = email.trim()
        return if (trimmed.length > MAX_LENGTH || !SHAPE.matches(trimmed)) R.string.error_email_invalid else null
    }

    /** 先必填再格式，返回第一个提示；合法时 null。 */
    fun errorRes(email: String): Int? = requiredErrorRes(email) ?: formatErrorRes(email)
}
