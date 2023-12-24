package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.utils.processFileIo
import kotlinx.coroutines.*
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.RandomAccessContent
import org.apache.commons.vfs2.util.RandomAccessMode
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

    private val accessLazy: Lazy<RandomAccessContent> = lazy {
        processFileIo(coroutineContext) {
            fileObject.content.getRandomAccessContent(fileAccessMode)
            // NOTE:
            // Apache VFS does not support Random writing
            // https://commons.apache.org/proper/commons-vfs/filesystems.html
        }
    }

    private val access: RandomAccessContent get() = accessLazy.value

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            access.seek(offset)
            access.readFully(data, 0, size)
            size
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        if (accessMode != AccessMode.W) { throw ErrnoException("Writing is not permitted", OsConstants.EBADF) }
        return processFileIo(coroutineContext) {
            access.seek(offset)
            access.write(data, 0, size)
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
            if (accessLazy.isInitialized()) access.close()
            onFileRelease()
        }
    }

}
