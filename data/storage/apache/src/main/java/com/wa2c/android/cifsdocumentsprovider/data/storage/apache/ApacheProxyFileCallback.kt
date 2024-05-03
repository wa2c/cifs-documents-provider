package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import com.wa2c.android.cifsdocumentsprovider.common.exception.StorageException
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.checkAccessMode
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.processFileIo
import kotlinx.coroutines.*
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.RandomAccessContent
import org.apache.commons.vfs2.util.RandomAccessMode
import java.io.BufferedOutputStream
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

/**
 * Proxy File Callback for sequential access.
 */
internal class ApacheProxyFileCallback(
    private val fileObject: FileObject,
    private val accessMode: AccessMode,
    private val onFileRelease: suspend () -> Unit,
) : ProxyFileDescriptorCallback(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    /** File size */
    private val fileSize: Long by lazy {
        processFileIo(coroutineContext) { fileObject.content.size }
    }

    private val fileAccessMode = when (accessMode) {
        AccessMode.R -> RandomAccessMode.READ
        AccessMode.W -> RandomAccessMode.READWRITE
    }

    private var writePointer: Long = 0L

    private var _reader: RandomAccessContent? = null

    private var _writer: OutputStream? = null

    private fun createReader(fp: Long): RandomAccessContent {
        return fileObject.content.getRandomAccessContent(fileAccessMode).also {
            it.seek(fp)
        }.also {
            _reader = it
        }
    }

    private fun createWriter(): OutputStream {
        return fileObject.content.outputStream.also {
            _writer = BufferedOutputStream(it, BUFFER_SIZE)
        }
    }


    private fun closeReader() {
        _reader?.let {
            launch {
                // close in background
                try { it.close() } catch (e: Exception) { logE(e) }
                logD("Reader released")
            }
        }
        _reader = null

    }


    private fun closeWriter() {
       _writer?.let {
           launch {
               // close in background
               try { it.close() } catch (e: Exception) { logE(e) }
               logD("Writer released")
           }
       }
        _writer = null
        writePointer = 0L
    }

    private fun getReadAccess(fp: Long): RandomAccessContent {
        closeWriter()
        return _reader?.let { access ->
            if (access.filePointer != fp) {
                closeReader()
                createReader(fp)
            } else {
                access
            }
        } ?: createReader(fp)
    }

    private fun getWriteAccess(fp: Long): OutputStream {
        closeReader()
        return _writer?.let { access ->
            if (fp == 0L) {
                closeWriter()
                null
            } else if (writePointer != fp) {
                closeWriter()
                throw StorageException.Operation.RandomAccessNotPermitted()
            } else {
                access
            }
        } ?: createWriter()
        // NOTE:
        // Apache VFS does not support Random writing
        // https://commons.apache.org/proper/commons-vfs/filesystems.html
    }

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            val maxSize = minOf(size.toLong(), fileSize - offset).toInt()
            getReadAccess(offset).readFully(data, 0, maxSize)
            maxOf(0, maxSize)
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            checkAccessMode(accessMode)
            getWriteAccess(offset).write(data, 0, size)
            writePointer += size.toLong()
            size
        }
    }

    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    @Throws(ErrnoException::class)
    override fun onRelease() {
        processFileIo(coroutineContext) {
            closeReader()
            closeWriter()
            onFileRelease()
        }
    }


}
