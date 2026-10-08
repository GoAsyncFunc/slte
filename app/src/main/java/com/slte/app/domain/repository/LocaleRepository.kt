package com.slte.app.domain.repository

import java.util.Locale
import kotlinx.coroutines.flow.StateFlow

interface LocaleRepository {
    val locale: StateFlow<Locale?>
    fun setLocale(locale: Locale?)
}
