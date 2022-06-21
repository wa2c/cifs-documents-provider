package com.wa2c.android.cifsdocumentsprovider.common.values

interface A {

}

/**
 * UI theme
 */
enum class UiTheme(val key: String): A {
    /** Default */
    DEFAULT("default"),
    /** Light */
    LIGHT("light"),
    /** Dark */
    DARK("dark"),
    ;

    val index: Int = this.ordinal

    companion object {
        /** Find value or default by key. */
        fun findByKeyOrDefault(key: String?): UiTheme {
            return values().firstOrNull { it.key == key } ?: DEFAULT
        }

        /** Find value or default by index. */
        fun findByIndexOrDefault(index: Int?): UiTheme {
            return values().firstOrNull { it.index == index } ?: DEFAULT
        }
    }
}
