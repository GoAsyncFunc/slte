package com.slte.app.ui.screen.settings

import com.slte.app.R
import com.slte.app.utils.isTraditionalChinese
import java.util.Locale

/** 界面语言选项。null 表示跟随系统。 */
enum class LanguageMode(
    val locale: Locale?,
    val labelRes: Int,
) {
    FOLLOW_SYSTEM(null, R.string.language_follow_system),
    SIMPLIFIED(Locale.SIMPLIFIED_CHINESE, R.string.language_simplified),
    TRADITIONAL(Locale.TRADITIONAL_CHINESE, R.string.language_traditional),
    ENGLISH(Locale.ENGLISH, R.string.language_english),
    ;

    companion object {

        fun fromLocale(locale: Locale?): LanguageMode = when {
            locale == null -> FOLLOW_SYSTEM
            locale.language == "zh" && isTraditionalChinese(locale) -> TRADITIONAL
            locale.language == "zh" -> SIMPLIFIED
            locale.language == "en" -> ENGLISH
            else -> FOLLOW_SYSTEM
        }
    }
}

/** TUN 堆栈模式。 */
enum class TunStackMode(
    val value: String,
    val labelRes: Int,
    val descRes: Int,
) {
    SYSTEM("system", R.string.settings_tun_stack_system, R.string.settings_tun_stack_system_desc),
    GVISOR("gvisor", R.string.settings_tun_stack_gvisor, R.string.settings_tun_stack_gvisor_desc),
    MIXED("mixed", R.string.settings_tun_stack_mixed, R.string.settings_tun_stack_mixed_desc),
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromValue(value: String): TunStackMode = entries.firstOrNull { it.value == value } ?: DEFAULT
    }
}
