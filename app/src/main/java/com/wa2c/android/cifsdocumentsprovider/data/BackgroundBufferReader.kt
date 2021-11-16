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
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.CoroutineContext

/**
 * BackgroundBufferReader
 */
class BackgroundBufferReader (
    /** Data Size */
    private val dataSize: Long,
    /** Buffer unit size */
    private val bufferSize: Int = BUFFER_SIZE,
    /** Buffer queue capacity  */
    private val queueCapacity: Int = 5,
    /** New InputStream */
    private val newInputStream: () -> InputStream
): CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /** Data buffer queue */
    private val dataBufferQueue = ArrayBlockingQueue<Deferred<DataBuffer?>>(queueCapacity)
    /** Current data buffer */
    private var currentDataBuffer: DataBuffer? = null
    /** Buffer loading job. */
    private var bufferingJob: Job? = null

    /**
     * Read buffer.
     */
    fun readBuffer(position: Long, size: Int, data: ByteArray): Int {
        if (bufferingJob == null) {
            startBufferingJob(position)
        }

        var dataOffset = 0
        while (true) {
            val c = currentDataBuffer ?: runBlocking {
                dataBufferQueue.take().let {
                    try { it.await() } catch (e: Exception) { null }
                }
            }.also { currentDataBuffer = it } ?: continue

            val bufferPosition = position + dataOffset
            val bufferRemain = c.getRemainSize(bufferPosition)
            val bufferOffset = c.getPositionOffset(bufferPosition)
            if (bufferOffset < 0) {
                startBufferingJob(bufferPosition)
                continue
            }

            val remainDataSize = size - dataOffset
            if (bufferRemain >= remainDataSize) {
                // Middle current buffer
                try {
                    c.data.copyInto(data, dataOffset, bufferOffset, bufferOffset + remainDataSize)
                } catch (e: Exception) {
                    logE(e)
                }
                return size
            } else {
                // End of current buffer
                val end = bufferOffset + bufferRemain
                c.data.copyInto(data, dataOffset, bufferOffset, end)
                currentDataBuffer = null
                if (c.length == end) {
                    // End of data
                    return bufferRemain
                } else {
                    dataOffset += bufferRemain
                }
            }

            return 0
        }
    }

    /**
     * Start buffering.
     */
    private fun startBufferingJob(startPosition: Long) {
        logD("startBufferLoading=$startPosition")
        cancelLoading()
        bufferingJob = launch (Dispatchers.IO) {
            try {
                var currentPosition = startPosition
                while (isActive) {
                    val remain = dataSize - currentPosition
                    if (dataSize > 0 && remain <= 0) break

                    if (remain > bufferSize) {
                        // Read buffer
                        val task = readAsync(currentPosition, bufferSize)
                        dataBufferQueue.put(task)
                        currentPosition += bufferSize
                    } else {
                        // End of data
                        val task = readAsync(currentPosition, remain.toInt())
                        dataBufferQueue.put(task)
                        currentPosition += remain
                        break
                    }
                }
            } catch (e: Exception) {
                logE(e)
            }
            bufferingJob = null
        }
    }

    /**
     * Read from Samba file.
     */
    private fun readAsync(position: Long, buffSize: Int): Deferred<DataBuffer?> {
        return async (Dispatchers.IO) {
            try {
                newInputStream.invoke().use {
                    it.skip(position)
                    val data = ByteArray(buffSize)
                    val size = it.read(data, 0, buffSize)
                    val remain = buffSize - size

                    if (size > 0 && remain > 0) {
                        val subSize = it.read(data, size, remain)
                        DataBuffer(position, size + subSize, data)
                    } else {
                        DataBuffer(position, size, data)
                    }
                }
            } catch (e: Exception) {
                logE(e)
                null
            }
        }
    }

    /**
     * Cancel loading
     */
    fun cancelLoading() {
        logD("cancelLoading")
        bufferingJob?.cancel()
        bufferingJob = null
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
    }

}