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
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.coroutines.CoroutineContext

/**
 * BackgroundBufferReader
 */
class BackgroundBufferWriter(
    /** Buffer unit size */
    private val bufferSize: Int = BUFFER_SIZE,
    /** Buffer queue capacity  */
    private val queueCapacity: Int = 5,
    /** New InputStream */
    private val newOutputStream: () -> OutputStream
): CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job



    /** Data buffer queue */
    private val dataBufferQueue = ArrayBlockingQueue<DataBuffer>(queueCapacity)
    /** Current data buffer */
    private var currentByteBuffer: ByteBuffer? = null
//    /** Buffer loading job. */
//    private var writingJob: Job? = null

    private var writingTask: Deferred<Unit>? = null

    private var streamPosition = 0L

    fun writeBuffer(position: Long, size: Int, data: ByteArray): Int {
        if (writingTask == null || position != streamPosition) {
            startBufferingJob(position)
        }

        if (size >= bufferSize) {
            currentByteBuffer?.let {
                val length = it.position()
                dataBufferQueue.put(DataBuffer(position - length, length,  it.array()))
                currentByteBuffer = null
                streamPosition += length
            }
            dataBufferQueue.put(DataBuffer(position, size,  data))
        } else {
            val buffer = currentByteBuffer ?: ByteBuffer.allocate(bufferSize).also { currentByteBuffer = it }
            val remain = buffer.remaining() - size
            if (remain > 0) {
                buffer.put(data, 0, size)
            } else {
                buffer.put(data, 0, remain)
                dataBufferQueue.put(
                    DataBuffer(
                        position + remain - bufferSize,
                        bufferSize,
                        buffer.array()
                    )
                )
                currentByteBuffer = ByteBuffer.allocate(bufferSize).also {
                    if (remain < 0) {
                        it.put(data, -remain, size)
                    }
                }
            }
        }

        streamPosition += size
        return size
    }

    /**
     * Start buffering.
     */
    private fun startBufferingJob(startPosition: Long) {
        logD("startBufferingJob=$startPosition")
        reset()
        writeAsync()
//        writingJob = launch (Dispatchers.IO) {
//            writingTask = writeAsync()
//
////            try {
////                newOutputStream.invoke().use { output ->
////                    while (isActive) {
////                        val dataBuffer = dataBufferQueue.take()
////                        if (dataBuffer == DataBuffer.emptyDataBuffer) {
////                            return@use
////                        } else {
////                            output.write(dataBuffer.data, 0, dataBuffer.length)
////                        }
////                    }
////                }
////            } catch (e: Exception) {
////                logE(e)
////            }
////            writingJob = null
//        }

    }



    /**
     * Read from Samba file.
     */
    private fun writeAsync() {
        writingTask = async (Dispatchers.IO) {
            try {
                newOutputStream.invoke().use { output ->
                    while (isActive) {
                        val dataBuffer = dataBufferQueue.take()
                        if (dataBuffer == DataBuffer.emptyDataBuffer) {
                            currentByteBuffer?.let {
                                output.write(it.array(), 0, it.position())
                            }
                            output.flush()
                            logD("writing task finished")
                            return@use
                        } else {
                            output.write(dataBuffer.data, 0, dataBuffer.length)
                        }
                    }
                }
            } catch (e: Exception) {
                logE(e)
            } finally {
                writingTask = null
            }
        }
    }

//    /**
//     * Close stream
//     */
//    private fun closeStream() {
//        try {
//            outputStream?.flush()
//            outputStream?.close()
//        } catch (e: Exception) {
//            logE(e)
//        }
//        outputStream = null
//    }

    /**
     * Reset
     */
    fun reset() {
        logD("reset")
        writingTask?.cancel()
        writingTask = null
        currentByteBuffer?.clear()
        currentByteBuffer = null
    }

    /**
     * Release
     */
    fun release() {
        logD("release")
        dataBufferQueue.put(DataBuffer.emptyDataBuffer)
        runBlocking { writingTask?.await() }
        reset()
    }

}