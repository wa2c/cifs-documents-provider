/*
 * MIT License
 *
 * Copyright (c) 2021 wa2c
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.wa2c.android.cifsdocumentsprovider.data

import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import kotlinx.coroutines.*
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.CoroutineContext

/**
 * BackgroundBufferReader
 */
class BackgroundBufferWriter(
    /** Data Size */
    private val dataSize: Long,
    /** Buffer unit size */
    private val bufferSize: Int = 1024 * 1024,
    /** Buffer queue capacity  */
    private val queueCapacity: Int = 5,
    /** New InputStream */
    private val newOutputStream: () -> OutputStream
): CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /** Data buffer queue */
    private val dataBufferQueue = ArrayBlockingQueue<Deferred<DataBuffer?>>(queueCapacity)
    /** Current data buffer */
    private var currentDataBuffer: DataBuffer? = null
    /** Buffer loading job. */
    private var writingJob: Job? = null

    //private var outputStream: BufferedOutputStream? = null
    private var outputStream: OutputStream? = null

    private var streamPosition = 0L

    fun writeBuffer(position: Long, size: Int, data: ByteArray): Int {
        if (position != streamPosition) {
            closeStream()
        }

        return (outputStream ?: newOutputStream.invoke().also {
            outputStream = it
            streamPosition = 0
        }).let { stream ->
            stream.write(data, 0, size)
            streamPosition += size
            // End of data
//            if (streamPosition >= dataSize) {
//                closeStream()
//            }
            size
        }
    }

    /**
     * Close stream
     */
    private fun closeStream() {
        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (e: Exception) {
            logE(e)
        }
        outputStream = null
    }

    /**
     * Cancel loading
     */
    fun cancelBuffering() {
        logD("cancelLoading")
        closeStream()
        writingJob?.cancel()
        writingJob = null
        currentDataBuffer = null
        dataBufferQueue.forEach { it.cancel() }
        dataBufferQueue.clear()
    }

    /**
     * Data buffer
     */
    private class DataBuffer(
        /** Data absolute start position */
        val position: Long = 0,
        /** Data length */
        val length: Int = 0,
        /** Data buffer */
        val data: ByteArray = ByteArray(1024 * 1024),
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
                p >= endPosition -> -1
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
                p >= endPosition -> -1
                else -> (endPosition - p).toInt()
            }
        }
    }

}