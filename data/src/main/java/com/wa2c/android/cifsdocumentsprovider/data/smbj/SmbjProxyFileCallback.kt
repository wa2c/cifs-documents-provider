package com.wa2c.android.cifsdocumentsprovider.data.smbj

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.hierynomus.smbj.share.File
import com.wa2c.android.cifsdocumentsprovider.common.processFileIo
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.io.BackgroundBufferReader
import com.wa2c.android.cifsdocumentsprovider.data.io.BackgroundBufferWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Proxy File Callback for SMBJ (Buffering IO)
 */
class SmbjProxyFileCallback(
    private val file: File,
    private val mode: AccessMode,
) : ProxyFileDescriptorCallback(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()

    /** File size */
    private val fileSize: Long by lazy { file.fileInformation.standardInformation.endOfFile }

    private var reader: BackgroundBufferReader? = null

    private var writer: BackgroundBufferWriter? = null

    private fun getReader(): BackgroundBufferReader {
        writer?.let {
            it.close()
            writer = null
            logD("Writer released")
        }
        return reader ?: BackgroundBufferReader(coroutineContext, onGetSize()) { start, array, off, len ->
            file.inputStream.use {
                it.skip(start)
                it.read(array, off, len)
            }
        }.also {
            reader = it
            logD("Reader created")
        }
    }


    private fun getWriter(): BackgroundBufferWriter {
        reader?.let {
            it.close()
            reader = null
            logD("Reader released")
        }

        return writer ?: BackgroundBufferWriter(coroutineContext) { start, array, off, len ->
            file.writeAsync(array, start, off, len)
        }.also {
            writer = it
            logD("Writer created")
        }
    }

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo {
            getReader().readBuffer(offset, size, data)
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        if (mode != AccessMode.W) { throw ErrnoException("Writing is not permitted", OsConstants.EBADF) }
        return processFileIo {
            getWriter().writeBuffer(offset, size, data)
            size
        }
    }
    
    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    @Throws(ErrnoException::class)
    override fun onRelease() {
        processFileIo {
            reader?.close()
            writer?.close()
            file.close()
            file.diskShare.close()
            file.diskShare.treeConnect.session.close()
            0
        }
    }
}