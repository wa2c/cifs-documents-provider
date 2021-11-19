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
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import kotlinx.coroutines.*
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
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
    private val queueCapacity: Int = 10,
    /** New InputStream */
    private val newOutputStream: () -> OutputStream
): CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /** Data buffer queue */
    private val dataBufferQueue = ArrayBlockingQueue<DataBuffer>(queueCapacity)

    private var writingTask: Deferred<Unit>? = null

    private var streamPosition = 0L

    fun writeBuffer(position: Long, size: Int, data: ByteArray): Int {
        if (writingTask == null || position != streamPosition) {
            writeAsync(position)
        }
        dataBufferQueue.put(DataBuffer(position, size,  data.copyOf()))
        streamPosition += size
        return size
    }

    /**
     * Read from Samba file.
     */
    private fun writeAsync(startPosition: Long) {
        logD("startBufferingJob=$startPosition")
        reset()
        writingTask = async (Dispatchers.IO) {
            try {
                newOutputStream.invoke().use { output ->
                    var byteBuffer = ByteBuffer.allocate(bufferSize)
                    while (isActive) {
                        val dataBuffer = dataBufferQueue.take()
                        if (dataBuffer == DataBuffer.emptyDataBuffer) {
                            output.write(byteBuffer.array(), 0, byteBuffer.position())
                            output.flush()
                            logD("writing task finished")
                            return@use
                        } else if (bufferSize < dataBuffer.length) {
                            output.write(dataBuffer.data, 0, dataBuffer.length)
                        } else if (byteBuffer.remaining() >= dataBuffer.length) {
                            byteBuffer.put(dataBuffer.data, 0, dataBuffer.length)
                        } else {
                            logD("■■■■■ dataBufferQueue.size = ${dataBufferQueue.size}")
                            output.write(byteBuffer.array(), 0, byteBuffer.position())
                            byteBuffer = ByteBuffer.allocate(bufferSize)
                            byteBuffer.put(dataBuffer.data, 0, dataBuffer.length)
                        }
                    }
                }
            } catch (e: Exception) {
                logE(e)
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
                dataBufferQueue.put(DataBuffer.emptyDataBuffer)
                writingTask?.await()
            }
        }
        writingTask?.cancel()
        writingTask = null
    }

    /**
     * Release
     */
    fun release() {
        logD("release")
//        dataBufferQueue.put(DataBuffer.emptyDataBuffer)
//        runBlocking { writingTask?.await() }
        reset()
    }

}