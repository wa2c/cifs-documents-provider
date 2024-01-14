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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.CoroutineContext

/**
 * BackgroundBufferReader
 */
class BackgroundBufferWriter(
    /** Buffer unit size */
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    /** Buffer queue capacity  */
    private val queueCapacity: Int = DEFAULT_CAPACITY,
    /** Coroutine context */
    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job(),
    /** Background writing */
    private val writeBackground: suspend CoroutineScope.(start: Long, array: ByteArray, off: Int, len: Int) -> Unit
): CoroutineScope, Closeable {
    /** Data buffer queue */
    private val dataBufferQueue = ArrayBlockingQueue<WriteDataBuffer>(queueCapacity)
    /** Current data buffer */
    private var currentBuffer: WriteDataBuffer? = null

    /**
     * Writing cycle task
     */
    private val writingCycleTask = async(coroutineContext) {
        logD("[CYCLE] Begin: bufferSize=$bufferSize")
        while (isActive) {
            try {
                val dataBuffer = dataBufferQueue.take()
                if (dataBuffer.isEndOfData) {
                    break
                } else {
                    logD("[CYCLE] Write: position=${dataBuffer.position}, length=${dataBuffer.length}")
                    writeBackground(dataBuffer.position, dataBuffer.data.array(), 0, dataBuffer.length)
                }
            } catch (e: Exception) {
                logE(e)
            }
        }
        logD("[CYCLE] End")
    }

    /**
     * Write buffer
     * @param writePosition Absolute data position.
     * @param writeSize Required read data size.
     * @param writeData For saving reading data.
     */
    fun writeBuffer(writePosition: Long, writeSize: Int, writeData: ByteArray): Int {
        // Check new position
        currentBuffer?.let {
            if (it.endPosition != writePosition) {
                dataBufferQueue.put(it)
                currentBuffer = null
            }
        }

        if (writeSize > bufferSize) {
            // writeSize lager than bufferSize
            currentBuffer?.let { dataBufferQueue.put(it) }
            dataBufferQueue.put(WriteDataBuffer(writePosition, ByteBuffer.wrap(writeData)))
            currentBuffer = null
        } else {
            currentBuffer?.let { buffer ->
                if (buffer.data.remaining() >= writeSize) {
                    buffer.data.put(writeData, 0, writeSize)
                } else {
                    dataBufferQueue.put(buffer)
                    currentBuffer = null
                }
            }
            if (currentBuffer == null){
                currentBuffer = WriteDataBuffer(writePosition, ByteBuffer.allocate(bufferSize)).also {
                    it.data.put(writeData, 0, writeSize)
                }
            }
        }

        return writeSize
    }

    /**
     * Close
     */
    override fun close() {
        logD("close")
        runBlocking(coroutineContext) {
            currentBuffer?.let {
                // Put current buffer to queue
                currentBuffer = null
                dataBufferQueue.put(it)
            }
            // Put end flag to queue
            dataBufferQueue.put(WriteDataBuffer.endOfData)
            writingCycleTask.await()
            logD("writingCycleTask completed")
        }
    }

    /**
    * Data buffer
    */
    data class WriteDataBuffer(
        /** Data absolute start position */
        val position: Long,
        /** Data buffer */
        val data: ByteBuffer,
    ) {
        /** Data length */
        val length: Int
            get() = data.position()

        /** End position */
        val endPosition: Long
            get() = position + length

        /** True if this is end of data. */
        val isEndOfData: Boolean
            get() = position < 0

        companion object {
            /** End flag data */
            val endOfData = WriteDataBuffer(-1, ByteBuffer.allocate(0))
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 1024 * 1024
        private const val DEFAULT_CAPACITY = 5
    }

}
