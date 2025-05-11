package com.wa2c.android.cifsdocumentsprovider.presentation.ext

import java.util.Locale

/**
 * Language
 */
enum class Language(
    /** Language code */
    val code: String
) {
    /** English */
    ENGLISH("en"),
    /** Japanese */
    JAPANESE("ja"),
    /** Arabic */
    ARABIC("ar"),
    /** Slovak */
    SLOVAK("sk"),
    /** Chinese */
    CHINESE("zh"),
    /** Burmese */
    MYANMAR("my"),
    /** Russian */
    RUSSIAN("ru"),
    ;

    companion object {
        val default: Language
            get() =  findByCodeOrDefault(Locale.getDefault().language)

        /** Find value or default by code */
        fun findByCodeOrDefault(code: String?): Language {
            val locale = Locale.getDefault()
            return entries.firstOrNull { it.code == code }
                ?: entries.firstOrNull { it.code == locale.toLanguageTag() }
                ?: entries.firstOrNull { it.code == locale.language }
                ?: ENGLISH
        }

    }
}
