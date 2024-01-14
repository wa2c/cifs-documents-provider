package com.wa2c.android.cifsdocumentsprovider.common.values

/**
 * UI theme
 */
enum class UiTheme(
    /** Key */
    val key: String
) {
    /** Default */
    DEFAULT("default"),
    /** Light */
    LIGHT("light"),
    /** Dark */
    DARK("dark"),
    ;

    /** Index */
    val index: Int = this.ordinal

    companion object {
        /** Find value or default by key. */
        fun findByKeyOrDefault(key: String?): UiTheme {
            return entries.firstOrNull { it.key == key } ?: DEFAULT
        }

        /** Find value or default by index. */
        fun findByIndexOrDefault(index: Int?): UiTheme {
            return entries.firstOrNull { it.index == index } ?: DEFAULT
        }
    }
}
