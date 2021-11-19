package com.wa2c.android.cifsdocumentsprovider.data.io

import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE

/**
 * Data buffer
 */
class DataBuffer(
    /** Data absolute start position */
    val position: Long = 0,
    /** Data length */
    val length: Int = 0,
    /** Data buffer */
    val data: ByteArray = ByteArray(BUFFER_SIZE),
) {
    /** Data absolute end position */
    val endPosition = position + length

    /**
     * Get offset with pointer and position. -1 if not in data.
     * @param p Absolute position of stream
     * @return Offset with start position and p
     */
    fun getPositionOffset(p: Long): Int {
        return when {
            p < position -> -1
            p > endPosition -> -1
            else -> (p - position).toInt()
        }
    }

    /**
     * Get remain size from pointer. -1 if not in data.
     * @param p Absolute position of stream
     * @return Offset with p and end position
     */
    fun getRemainSize(p: Long): Int {
        return when {
            p < position -> -1
            p > endPosition -> -1
            else -> (endPosition - p).toInt()
        }
    }

    companion object {
        val emptyDataBuffer = DataBuffer(0, 0, byteArrayOf())
    }
}