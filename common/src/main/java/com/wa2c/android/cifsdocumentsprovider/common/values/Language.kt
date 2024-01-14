package com.wa2c.android.cifsdocumentsprovider.common.values

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
    ;

    /** Index */
    val index: Int = this.ordinal

    companion object {
        val default: Language
            get() =  Language.findByCodeOrDefault(Locale.getDefault().language)

        /** Find value or default by code */
        fun findByCodeOrDefault(code: String?): Language {
            return entries.firstOrNull { it.code == code } ?: ENGLISH
        }

        /** Find value or default by index. */
        fun findByIndexOrDefault(index: Int?): Language {
            return entries.firstOrNull { it.index == index } ?: ENGLISH
        }
    }
}
