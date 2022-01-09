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
package com.wa2c.android.cifsdocumentsprovider.data.io

import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import kotlinx.coroutines.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.CoroutineContext

/**
 * BackgroundBufferReader
 */
class BackgroundBufferReader (
    /** Whole data Size */
    private val streamSize: Long,
    /** Buffer unit size */
    private val bufferSize: Int = BUFFER_SIZE,
    /** Buffer queue capacity  */
    private val queueCapacity: Int = 5,
    /** Background reading */
    private val readBackgroundAsync: (start: Long, array: ByteArray, off: Int, len: Int) -> Int
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
     * @param streamPosition Absolute data position.
     * @param readSize Required read data size.
     * @param readData For saving reading data.
     */
    fun readBuffer(streamPosition: Long, readSize: Int, readData: ByteArray): Int {
        var readOffset = 0
        while (true) {
            // Get current buffer
            val c = currentDataBuffer ?: let {
                if (bufferingJob == null && dataBufferQueue.isEmpty()) {
                    logD("Start cycle: position=$streamPosition")
                    startBackgroundCycle(streamPosition)
                }
                dataBufferQueue.take().let {
                    runBlocking { try { it.await() } catch (e: Exception) { null } }
                }
            }.also { currentDataBuffer = it } ?: continue

            val currentStreamPosition = streamPosition + readOffset
            val bufferRemainSize = c.getRemainSize(currentStreamPosition)
            val bufferOffset = c.getPositionOffset(currentStreamPosition)
            if (bufferOffset < 0) {
                // Position reset
                logD("Reset position: currentStreamPosition=$currentStreamPosition, bufferRemain=$bufferRemainSize, bufferOffset=$bufferOffset")
                startBackgroundCycle(currentStreamPosition)
                continue
            }

            val readRemainSize = readSize - readOffset
            if (bufferRemainSize >= readRemainSize) {
                // In current buffer
                c.data.copyInto(readData, readOffset, bufferOffset, bufferOffset + readRemainSize)
                return readSize
            } else {
                // End of current buffer
                val bufferEnd = bufferOffset + bufferRemainSize
                c.data.copyInto(readData, readOffset, bufferOffset, bufferEnd)
                currentDataBuffer = null
                if (c.length < bufferEnd) {
                    // Read next buffer
                    readOffset += bufferRemainSize
                    continue
                } else {
                    return readOffset + bufferRemainSize
                }
            }
        }
    }

    /**
     * Start background reading.
     * @param startStreamPosition Start stream position.
     */
    private fun startBackgroundCycle(startStreamPosition: Long) {
        logD("startBackgroundCycle=$startStreamPosition")

        reset()
        bufferingJob = launch (Dispatchers.IO) {
            try {
                var currentStreamPosition = startStreamPosition
                while (isActive) {
                    val remainStreamSize = streamSize - currentStreamPosition
                    if (remainStreamSize <= 0) {
                        logD("Out of range: streamSize=$streamSize, currentPosition=$currentStreamPosition")
                        break
                    }

                    // Read buffer
                    if (remainStreamSize > bufferSize) {
                        // Read buffer (Normal)
                        val task = readAsync(currentStreamPosition, bufferSize)
                        dataBufferQueue.put(task)
                        currentStreamPosition += bufferSize
                    } else {
                        // Read buffer (End of stream)
                        val task = readAsync(currentStreamPosition, remainStreamSize.toInt())
                        dataBufferQueue.put(task)
                        currentStreamPosition += remainStreamSize
                        logD("End of stream: streamSize=$streamSize, currentPosition=$currentStreamPosition")
                        break
                    }
                }
            } finally {
                bufferingJob = null
            }
        }
    }

    /**
     * Read from Samba file.
     * @param streamPosition Absolute data position.
     * @param readSize Read data size.
     */
    private fun readAsync(streamPosition: Long, readSize: Int): Deferred<DataBuffer> {
        return async (Dispatchers.IO) {
            val data = ByteArray(readSize)
            val size = readBackgroundAsync(streamPosition, data, 0, readSize)
            val remain = readSize - size

            if (size > 0 && remain > 0) {
                val subSize = readBackgroundAsync(streamPosition + size, data, size, remain)
                DataBuffer(streamPosition, size + subSize, data)
            } else {
                DataBuffer(streamPosition, size, data)
            }
        }
    }

    /**
     * Reset
     */
    private fun reset() {
        logD("reset")
        bufferingJob?.cancel()
        bufferingJob = null
        currentDataBuffer = null
        dataBufferQueue.forEach { it.cancel() }
        dataBufferQueue.clear()
    }

    /**
     * Release
     */
    fun release() {
        reset()
    }

    /**
     * Data buffer
     */
    class DataBuffer(
        /** Data absolute start position */
        private val streamPosition: Long = 0,
        /** Data length */
        val length: Int = 0,
        /** Data buffer */
        val data: ByteArray = ByteArray(BUFFER_SIZE),
    ) {
        /** Data absolute end position */
        private val endStreamPosition = streamPosition + length

        private fun isIn(p: Long): Boolean {
            return p in streamPosition..endStreamPosition
        }

        /**
         * Get offset with pointer and position. -1 if not in data.
         * @param p Absolute position of stream
         * @return Offset with start position and p
         */
        fun getPositionOffset(p: Long): Int {
            return if (isIn(p)) (p - streamPosition).toInt()
            else -1
        }

        /**
         * Get remain size from pointer. -1 if not in data.
         * @param p Absolute position of stream
         * @return Offset with p and end position
         */
        fun getRemainSize(p: Long): Int {
            return if (isIn(p)) (endStreamPosition - p).toInt()
            else -1
        }
    }
}