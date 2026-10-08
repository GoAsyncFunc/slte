package com.slte.app.data.local

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import com.slte.app.domain.repository.ThemeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ThemePreference
@Inject
constructor(
    @ApplicationContext context: Context,
) : ThemeRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    private val _dark = MutableStateFlow(prefs.getBoolean(KEY_DARK, false))
    override val dark: StateFlow<Boolean> = _dark.asStateFlow()

    override fun setDark(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DARK, enabled) }
        _dark.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "slte_theme"
        const val KEY_DARK = "dark_mode"
    }
}
