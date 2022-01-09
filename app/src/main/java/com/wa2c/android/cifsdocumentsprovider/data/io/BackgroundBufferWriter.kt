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
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.CoroutineContext

/**
 * BackgroundBufferReader
 */
class BackgroundBufferWriter(
    /** Buffer unit size */
    private val bufferSize: Int = BUFFER_SIZE,
    /** Buffer queue capacity  */
    private val queueCapacity: Int = 5,
    /** Background writing */
    private val writeBackground: (start: Long, array: ByteArray, off: Int, len: Int) -> Unit
): CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /** Data buffer queue */
    private val dataBufferQueue = ArrayBlockingQueue<WriteDataBuffer>(queueCapacity)

    private var writingTask: Deferred<Unit>? = null

    private var currentBuffer: WriteDataBuffer? = null

    private var streamPosition = 0L

    fun writeBuffer(position: Long, size: Int, data: ByteArray): Int {
        if (writingTask == null) {
            startBackgroundCycle()
        }

        // Different position
        currentBuffer?.let {
            if (it.endPosition != position) {
                dataBufferQueue.put(it)
                currentBuffer = null
            }
        }

        if (size > bufferSize) {
            currentBuffer?.let {
                dataBufferQueue.put(it)
                currentBuffer = null
            }
            dataBufferQueue.put(WriteDataBuffer(position, ByteBuffer.wrap(data)))
        } else {
            currentBuffer?.let { buffer ->
                if (buffer.data.remaining() >= size) {
                    buffer.data.put(data, 0, size)
                } else {
                    dataBufferQueue.put(buffer)
                    currentBuffer = WriteDataBuffer(position, ByteBuffer.allocate(bufferSize)).also {
                        it.data.put(data, 0, size)
                    }
                }
            } ?: let {
                currentBuffer = WriteDataBuffer(position, ByteBuffer.allocate(bufferSize)).also {
                    it.data.put(data, 0, size)
                }
            }
        }

        streamPosition += size
        return size
    }

    /**
     * Start background writing
     */
    private fun startBackgroundCycle() {
        reset()
        writingTask = async (Dispatchers.IO) {
            try {
                while (isActive) {
                    val dataBuffer = dataBufferQueue.take()
                    if (dataBuffer.isEndOfData) {
                        logD("End of Data")
                        break
                    } else {
                        logD("dataBufferQueue.size=${dataBufferQueue.size}")
                        writeBackground(dataBuffer.position, dataBuffer.data.array(), 0, dataBuffer.length)
                    }
                }
            } finally {
                writingTask = null
            }
        }
    }

    /**
     * Reset
     */
    fun reset() {
        logD("reset")
        if (writingTask != null) {
            runBlocking {
                currentBuffer?.let {
                    currentBuffer = null
                    dataBufferQueue.put(it)
                }
                dataBufferQueue.put(WriteDataBuffer.endOfData)
                writingTask?.await()
            }
            logD("writingTask completed")
        }
        writingTask = null
    }

    /**
     * Release
     */
    fun release() {
        logD("release")
        reset()
    }


    /**
    * Data buffer
    */
    data class WriteDataBuffer(
        /** Data absolute start position */
        val position: Long = 0,
        /** Data buffer */
        val data: ByteBuffer = ByteBuffer.allocate(0),
    ) {
        /** Data length */
        val length: Int
            get() = data.position()

        /** End position */
        val endPosition: Long
            get() = position + length

        /** True if this is end of data. */
        val isEndOfData: Boolean
            get() = position == -1L

        companion object {
            /** End Data Flag */
            val endOfData = WriteDataBuffer(-1)
        }
    }



}