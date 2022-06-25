package com.wa2c.android.cifsdocumentsprovider.common.values

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
    /** Slovak */
    SLOVAK("sk"),
    /** Chinese */
    CHINESE("zh"),
    ;

    /** Index */
    val index: Int = this.ordinal


    companion object {
        /** Find value or default by code */
        fun findByCodeOrDefault(code: String?): Language {
            return values().firstOrNull { it.code == code } ?: ENGLISH
        }

        /** Find value or default by index. */
        fun findByIndexOrDefault(index: Int?): Language {
            return values().firstOrNull { it.index == index } ?: ENGLISH
        }
    }
}