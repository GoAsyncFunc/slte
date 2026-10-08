package com.slte.app.domain.service

import android.content.Context

interface SupportChat {
    fun openChat(context: Context, email: String? = null)
}
