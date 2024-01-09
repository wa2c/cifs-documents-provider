package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.utils.processFileIo
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import kotlinx.coroutines.*
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.RandomAccessContent
import org.apache.commons.vfs2.util.RandomAccessMode
import java.io.BufferedOutputStream
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

/**
 * Proxy File Callback for Apache FTP with Random Access
 */
internal class ApacheFtpProxyFileCallbackSafe(
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

    private var _readAccess: RandomAccessContent? = null

    private fun createReadAccess(fp: Long): RandomAccessContent {
        return fileObject.content.getRandomAccessContent(fileAccessMode).also {
            it.seek(fp)
        }.also {
            _readAccess = it
        }
    }

    private fun getReadAccess(fp: Long): RandomAccessContent {
        return _readAccess?.let { access ->
            if (access.filePointer != fp) {
                launch(coroutineContext) {
                    access.close() // close in background
                }
                createReadAccess(fp)
            } else {
                access
            }
        } ?: createReadAccess(fp)
    }


    private var writePointer: Long = 0L

    private var _writeAccess: OutputStream? = null

    private fun getWriteAccess(fp: Long): OutputStream {
        return _writeAccess?.let { access ->
            if (writePointer != fp) {
                launch(coroutineContext) { access.close() }
                throw ErrnoException("Writing failed", OsConstants.EBADF)
            } else {
                access
            }
        } ?: let {
            fileObject.content.outputStream.also {
                _writeAccess = BufferedOutputStream(it, BUFFER_SIZE)
            }
        }
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
            getReadAccess(offset).readFully(data, 0, size)
            size
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        if (accessMode != AccessMode.W) { throw ErrnoException("Writing is not permitted", OsConstants.EBADF) }
        return processFileIo(coroutineContext) {
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
            _readAccess?.close()
            _readAccess = null
            _writeAccess?.close()
            _writeAccess = null
            onFileRelease()
        }
    }


}
