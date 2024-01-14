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
package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils

import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

/**
 * BackgroundBufferReader
 */
class BackgroundBufferReader(
    /** Whole data Size */
    private val streamSize: Long,
    /** Buffer unit size */
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    /** Buffer queue capacity  */
    private val queueCapacity: Int = DEFAULT_CAPACITY,
    /** Coroutine context */
    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job(),
    /** Background reading */
    private val readBackgroundAsync: CoroutineScope.(start: Long, array: ByteArray, off: Int, len: Int) -> Int
): Closeable, CoroutineScope {

    /** Dummy queue item. */
    private val dummyQueueItem = async { null }
    /** Data buffer queue */
    private val dataBufferQueue = ArrayBlockingQueue<Deferred<DataBuffer?>>(queueCapacity)
    /** Current data buffer */
    private var currentDataBuffer: DataBuffer? = null

    /** Reset readingCycleTask position */
    private var resetCyclePosition:  Long = 0
        @Synchronized get
        @Synchronized set

    /**
     * Reading cycle task
     */
    private val readingCycleTask = launch(coroutineContext) {
        logD("[CYCLE] Begin: streamSize=$streamSize, bufferSize=$bufferSize")
        var startPosition = 0L
        var currentCyclePosition = 0L

        while (isActive) {
            try {
                // Check new position
                resetCyclePosition.let {
                    if (startPosition != it) {
                        // Reset position
                        logD("[CYCLE] Reset: startPosition=$startPosition, currentPosition=$currentCyclePosition")
                        resetQueue()
                        startPosition = it
                        currentCyclePosition = it
                    }
                }

                // Check size
                val remainStreamSize = streamSize - currentCyclePosition
                if (remainStreamSize <= 0) {
                    logD("[CYCLE] Paused: startPosition=$startPosition, currentPosition=$currentCyclePosition")
                    dataBufferQueue.put( dummyQueueItem )
                    continue
                }

                // Read buffer
                val readSize = (if (remainStreamSize > bufferSize) bufferSize else remainStreamSize).toInt()
                logD("[CYCLE] Read: startPosition=$startPosition, currentPosition=$currentCyclePosition, readSize=$readSize")
                val task = readAsync(currentCyclePosition, readSize)
                currentCyclePosition += readSize
                dataBufferQueue.put(task)
            } catch (e: Exception) {
                logE(e)
            }
        }

        logD("[CYCLE] End: startPosition=$startPosition, currentCyclePosition=$currentCyclePosition")
    }

    /**
     * Read from Samba file.
     * @param streamPosition Absolute data position.
     * @param readSize Read data size.
     */
    private fun readAsync(streamPosition: Long, readSize: Int): Deferred<DataBuffer> {
        return async (coroutineContext) {
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
     * Read buffer.
     * @param readPosition Absolute data position.
     * @param readSize Required read data size.
     * @param readData For saving reading data.
     */
    fun readBuffer(readPosition: Long, readSize: Int, readData: ByteArray): Int {
        // logD("readPosition=$readPosition, readSize=$readSize")
        if (readData.isEmpty()) return 0
        val maxSize: Int = min(readSize, readData.size).let {
            if (readPosition + it > streamSize) {
                (streamSize - readPosition).toInt() // NOTE: readSize may be larger than the file size
            } else {
                it
            }
        }

        var readOffset = 0
        while (true) {
            val streamPosition = readPosition + readOffset
            val c = getNextDataBuffer() ?: let {
                resetCycle(streamPosition)
                getNextDataBuffer()
            } ?: return 0

            val bufferRemainSize = c.getRemainSize(streamPosition)
            val bufferOffset = c.getPositionOffset(streamPosition)
            if (bufferRemainSize < 0 || bufferOffset < 0) {
                // Position reset (not contains)
                logD("[READ] Reset: streamPosition=$streamPosition, bufferRemainSize=$bufferRemainSize, bufferOffset=$bufferOffset")
                resetCycle(streamPosition)
                continue
            }
            val readRemainSize = maxSize - readOffset
            if (bufferRemainSize >= readRemainSize) {
                // In current buffer
                logD("[READ] Read: streamPosition=$streamPosition, bufferRemainSize=$bufferRemainSize, bufferOffset=$bufferOffset")
                c.data.copyInto(readData, readOffset, bufferOffset, bufferOffset + readRemainSize)
                return maxSize
            } else {
                logD("[READ] Read halfway: streamPosition=$streamPosition, bufferRemainSize=$bufferRemainSize, bufferOffset=$bufferOffset")
                c.data.copyInto(readData, readOffset, bufferOffset, c.length)
                currentDataBuffer = null
                val size = c.length - bufferOffset
                readOffset += size
            }
        }
    }

    /**
     * Get next data buffer
     */
    private fun getNextDataBuffer(): DataBuffer? {
        if (currentDataBuffer != null) {
            return currentDataBuffer
        } else {
            for (i in 0..queueCapacity) {
                dataBufferQueue.take()?.takeIf { it != dummyQueueItem }?.let {
                    runBlocking { it.await() }?.let {
                        logD("Next buffer: startPosition=${it.streamPosition}, length=${it.length} ")
                        currentDataBuffer = it
                        return it
                    }
                }
            }
        }
        return null
    }

    /**
     * Reset cycle position
     */
    private fun resetCycle(position: Long) {
        resetCyclePosition = position
        currentDataBuffer = null
    }

    /**
     * Reset queue
     */
    private fun resetQueue() {
        dataBufferQueue.forEach { it.cancel() }
        dataBufferQueue.clear()
    }

    /**
     * Reset
     */
    override fun close() {
        logD("close")
        runBlocking(coroutineContext) {
            readingCycleTask.cancel()
            resetQueue()
            currentDataBuffer = null
        }
    }

    /**
     * Data buffer
     */
    class DataBuffer(
        /** Data absolute start position */
        val streamPosition: Long,
        /** Data length */
        val length: Int,
        /** Data buffer */
        val data: ByteArray,
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

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 1024 * 1024
        private const val DEFAULT_CAPACITY = 5
    }
}
