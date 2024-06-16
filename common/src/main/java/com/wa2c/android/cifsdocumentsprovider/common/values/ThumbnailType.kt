package com.wa2c.android.cifsdocumentsprovider.common.values

/**
 * Thumbnail type
 */
enum class ThumbnailType(
    /** type */
    val type: String
) {
    /** Image */
    IMAGE("image"),
    /** Audio */
    AUDIO("audio"),
    /** Video */
    VIDEO("video"),
    ;

    companion object {
        /** Find value by type. */
        fun findByType(type: String?): ThumbnailType? {
            return entries.firstOrNull { type?.startsWith(it.type) == true }
        }
    }
}
