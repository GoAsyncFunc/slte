package com.slte.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ThemeRepository {
    val dark: StateFlow<Boolean>
    fun setDark(enabled: Boolean)
}
