package com.slte.app.utils

import com.slte.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailInputTest {
    @Test
    fun `白名单下只选了后缀没填邮箱名时算没填`() {
        // 拼出来的 "@gmail.com" 看着非空，但用户其实一个字符都没填
        assertEquals(R.string.error_email_required, EmailInput.errorRes("@gmail.com"))
        assertEquals(R.string.error_email_required, EmailInput.errorRes(""))
        assertEquals(R.string.error_email_required, EmailInput.errorRes("   "))
    }

    @Test
    fun `明显不是邮箱时给格式提示而不是发送失败`() {
        assertEquals(R.string.error_email_invalid, EmailInput.errorRes("abc"))
        assertEquals(R.string.error_email_invalid, EmailInput.errorRes("a@b"))
        assertEquals(R.string.error_email_invalid, EmailInput.errorRes("a b@c.d"))
        assertEquals(R.string.error_email_invalid, EmailInput.errorRes("a@b@c.d"))
        assertEquals(R.string.error_email_invalid, EmailInput.errorRes("a@b." + "c".repeat(300)))
    }

    @Test
    fun `正常邮箱通过校验`() {
        assertNull(EmailInput.errorRes("a@b.c"))
        assertNull(EmailInput.errorRes("first.last+tag@sub.example.com"))
        assertNull(EmailInput.errorRes("  a@b.c  "))
    }

    @Test
    fun `本地部分取 @ 之前那截`() {
        assertEquals("a", EmailInput.localPart("a@b.c"))
        assertEquals("a@b", EmailInput.localPart("a@b@c.d"))
        assertEquals("", EmailInput.localPart("@gmail.com"))
        assertEquals("abc", EmailInput.localPart("abc"))
    }
}
